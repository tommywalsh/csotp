package su.thepeople.carstereo.android.platform_impl;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import su.thepeople.carstereo.lib.interthread.InterThreadAPI;
import su.thepeople.carstereo.lib.platform_interface.MessagingSystem;

public class AndroidMessagingSystem implements MessagingSystem {

    private final Looper looper;
    private InterThreadAPI receiver;

    public AndroidMessagingSystem(Looper looper) {
        this.looper = looper;
    }

    @Override
    public void prepareThreadForMessageReception(InterThreadAPI receiver) {
        this.receiver = receiver;
    }

    @Override
    public void runMessagingLoop() {
        Looper.loop();
    }

    private Handler handler;

    private final Handler.Callback handleMessage = (msg) -> {
        assert receiver != null;
        receiver.handleMessage(msg.arg1, msg.obj);
        return true;
    };

    private Handler getHandler() {
        // This is only called from "our" thread, so no need to synchronize.
        if (handler == null) {
            handler = new Handler(looper, handleMessage);
        }
        return handler;
    }

    @Override
    public void sendMessage(int callbackId, Object data) {
        Message msg = Message.obtain();
        msg.arg1 = callbackId;
        msg.obj = data;
        getHandler().sendMessage(msg);
    }
}
