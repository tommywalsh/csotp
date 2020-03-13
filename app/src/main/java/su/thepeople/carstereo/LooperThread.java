package su.thepeople.carstereo;

import android.os.Looper;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Utility class for a thread that needs to accept messages from other threads in the application.
 */
public class LooperThread extends Thread {

    // Code that will set up message handlers before the main loop starts.
    private Consumer<Looper> handlerCreation;

    private final Lock readyMessageLock = new ReentrantLock();
    private final Condition readyCondition = readyMessageLock.newCondition();
    private boolean ready = false;

    /**
     * @param handlerCreation - Code to run when the looper is set up. This code typically sets up any handlers required
     */
    public LooperThread(Consumer<Looper> handlerCreation) {
        this.handlerCreation = handlerCreation;
    }

    @Override
    public synchronized void start() {
        super.start();

        // Don't actually return from this until the thread has started and all setup is done.
        readyMessageLock.lock();
        try {
            while (!ready) {
                try {
                    readyCondition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            readyMessageLock.unlock();
        }
    }

    public void run() {
        // Set up, but do not start, an event loop.
        Looper.prepare();

        // Before the event loop actually starts, allow the task to set up whatever message handlers it needs.
        handlerCreation.accept(Looper.myLooper());

        /*
         * When we get here, it means all the setup work is done, and we are about to enter the event loop. Only once
         * we get here is it safe for clients to use any message handlers setup above. So, send a message that we're
         * ready.
         */
        ready = true;
        readyMessageLock.lock();
        try {
            readyCondition.signal();
        } finally {
            readyMessageLock.unlock();
        }

        // Turn over control to the event loop.
        Looper.loop();
    }
}
