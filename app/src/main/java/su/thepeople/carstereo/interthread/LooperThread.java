package su.thepeople.carstereo.interthread;

import android.os.Looper;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import su.thepeople.carstereo.interthread.InterThreadAPI;

/**
 * Utility class for a thread that needs to accept messages from other threads in the application.
 */
public abstract class LooperThread<T extends InterThreadAPI> {

    private final Lock readyMessageLock = new ReentrantLock();
    private final Condition readyCondition = readyMessageLock.newCondition();
    private T api;

    /**
     * This method should set up the inter-thread communication object. This object will be passed back to whoever
     * starts the thread.
     */
    protected abstract T setupCommunications();

    /**
     * This method should do whatever setup is required before the main loop starts. This method can do long-running
     * tasks if it wants. Any incoming interthread messages will be queued until this method finishes.
     */
    protected abstract void beforeMainLoop();

    private Thread helperThread;

    public synchronized T startThread() {
        if (helperThread == null) {

            // Create a new helper thread and set it running.
            helperThread = new Thread(this::run);
            helperThread.start();

            // Wait for the helper thread to set up inter-thread communication
            readyMessageLock.lock();
            try {
                while (api == null) {
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
        return api;
    }

    public synchronized void abandon() {
        if (helperThread != null) {
            helperThread.interrupt();
            helperThread = null;
        }
    }

    private void run() {
        // Set up, but do not start, an event loop.
        Looper.prepare();

        // Before the event loop actually starts, allow the task to set up whatever message handlers it needs.
        api = setupCommunications();

        /*
         * The code that started the thread is now waiting for the API object. Send a signal that it is ready.
         */
        readyMessageLock.lock();
        try {
            readyCondition.signal();
        } finally {
            readyMessageLock.unlock();
        }

        // Before we start the main loop, do any pre-loop setup that is required.
        beforeMainLoop();

        // Turn over control to the event loop.
        Looper.loop();
    }
}
