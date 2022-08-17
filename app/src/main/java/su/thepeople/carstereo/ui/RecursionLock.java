package su.thepeople.carstereo.ui;

/**
 * This utility class can be used to avoid infinite recursion when UI widget callbacks want to update other widgets.
 *
 * This class should only be used on the UI thread.
 */
class RecursionLock {
    private boolean isLocked = false;

    void run(Runnable r) {
        // We don't need to worry about synchronization or race conditions since this is meant for single-thread use.
        if (!isLocked) {
            isLocked = true;
            r.run();
            isLocked = false;
        }
    }
}
