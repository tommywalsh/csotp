package su.thepeople.carstereo;

import android.os.Looper;

import java.util.function.Consumer;

/**
 * Utility class for a thread that needs to accept messages from other threads in the application.
 */
public class LooperThread extends Thread {
    private Consumer<Looper> handlerCreation;

    /**
     * @param handlerCreation - Code to run when the looper is set up. This code typically sets up any handlers required
     */
    LooperThread(Consumer<Looper> handlerCreation) {
        this.handlerCreation = handlerCreation;
    }

    public void run() {
        // Set up, but do not start, an event loop.
        Looper.prepare();

        // Before the event loop actually starts, allow the task to set up whatever message handlers it needs.
        handlerCreation.accept(Looper.myLooper());

        // Turn over control to the event loop.
        Looper.loop();
    }
}
