package su.thepeople.carstereo.ui;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * This class's job is to listen for relevant Bluetooth events from the Android system,
 * and then report them back to the MainUI.
 */
public class BluetoothInputHandler extends BroadcastReceiver {
    private static final String LOG_ID = "Bluetooth Input Handler";

    private final MainUI mainUI;
    private final IntentFilter filter;

    public BluetoothInputHandler(MainUI mainUI) {
        this.mainUI = mainUI;
        filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    }

    public IntentFilter getIntentFilter() {
        return filter;
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
