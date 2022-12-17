package su.thepeople.carstereo.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;

/**
 * This class handles all the work associated with turning the screen on and allowing the screen to shut off
 *
 * There are a number of complications around this on the Android platform. They are hidden away in here so that the
 * main activity doesn't get too complicated.
 *
 * This class uses a couple of deprecated and warned-against techniques...
 *   FULL_WAKE_LOCK is deprecated, as they'd like you to use Window flags instead. But, I could not find a way to make
 *     window flags give the desired behavior of forcing the screen on in response to a bluetooth event. I think this is
 *     by design, as ordinarily this would not be desired behavior on a normal phone app. But, this is not meant to be
 *     a well-behaved phone app, so we use the old deprecated flag instead.
 *   It's recommended to use a timeout when acquiring a wake lock so as not to drain a user's battery for long
 *     operations. But, this app is designed to be used on an always-plugged-in Android device, and we really do want
 *     the screen to stay on "forever" (until we explicitly decide to shut it off)
 */
class ScreenLocker {

    private final PowerManager.WakeLock wakeLock;
    private boolean isLocked = false;

    @SuppressWarnings("deprecation")
    ScreenLocker(Activity mainActivity) {
        PowerManager powerManager = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);
        assert powerManager != null;
        wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "csotp:wakelock");
    }

    @SuppressLint("WakelockTimeout")
    void ensureScreenOn() {
        // We might be locked already. If so, the screen is already on, and there's nothing to do.
        if (!isLocked) {
            // This will force the screen to turn on, if it is not on already.
            wakeLock.acquire();
            isLocked = true;
        }
    }

    void allowScreenToShutOff() {
        // We might not be locked, for example, if the app was not running when Bluetooth was connected.
        if (isLocked) {
            // We can't force the screen to shut off, but this will allow it to shut off on its own later.
            wakeLock.release();
            isLocked = false;
        }
    }
}
