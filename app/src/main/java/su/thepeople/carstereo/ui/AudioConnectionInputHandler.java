package su.thepeople.carstereo.ui;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * This class's job is to listen changes to the system's audio connection, and then report back
 * to the main UI.
 *
 * This app exclusively uses Bluetooth for audio connections.
 */
public class AudioConnectionInputHandler extends BroadcastReceiver {
    private static final String LOG_ID = "Bluetooth Audio Connection Input Handler";

    private final MainUI mainUI;
    private final IntentFilter filter;

    public AudioConnectionInputHandler(MainUI mainUI) {
        this.mainUI = mainUI;
        filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    }

    void registerWithSystem(ContextWrapper context) {
        context.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            Log.d(LOG_ID, "Detected bluetooth connection made");
            mainUI.onAudioConnectionMade();
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            Log.d(LOG_ID, "Detected bluetooth connection lost");
            mainUI.onAudioConnectionLost();
        }
    }
}
