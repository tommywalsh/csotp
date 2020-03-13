package su.thepeople.carstereo;

import android.app.Activity;
import android.view.View;

class Utils {

    /**
     * This app is intended to work like a real car stereo. So, it assumes that the device is being exclusively used to
     * run the app. This helper method causes the app to go "full screen", hiding all system controls.
     *
     * The user can, of course, use other apps occasionally, but our UI is not optimized for it.
     */
    static void hideSystemUI(Activity activity, int id) {
        activity.findViewById(id).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
}
