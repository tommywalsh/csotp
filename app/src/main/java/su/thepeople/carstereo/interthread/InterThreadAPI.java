package su.thepeople.carstereo.interthread;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helper superclass for APIs that can be called from any thread, but which always run on a single "target" thread.
 *
 * This class is designed to be used in a three-level hierarchy:
 *
 * Generic Layer (this class): defines and implements generic inter-thread message passing.
 *    - Defines no public fields/methods, so there is nothing in here of interest to clients.
 *    - Allows public-layer subclasses to specify a list of supported inter-thread messages, and to send these messages
 *      from their public methods.
 *    - Allows concrete-layer subclasses to define what should happen on the target thread when it receives a particular
 *      inter-thread message.
 *
 * Public Layer (e.g. MainActivityAPI): defines the API that is available to other threads.
 *    - Defines the public API and associates each API method with the sending of an inter-thread message.
 *    - Declares various abstract methods for the concrete layer to implement.
 *
 * Concrete Layer (e.g. MainActivityAPI): implements the API with methods that are always run on the correct thread.
 *    - Implements the abstract methods required by the public layer.
 */
public abstract class InterThreadAPI {

    private static class CallbackWrapper<T> {
        protected Class<T> type;
        protected Consumer<T> callback;
    }

    // All of the callback wrappers for this API will be stored in this list.
    private final List<CallbackWrapper<?>> callbackWrappers = new ArrayList<>();

    // The Android Handler which will receive the inter-thread messages.
    private Handler handler;

    /*
     * Returns handle to the "looper" that controls the event loop for the thread on which the Concrete Layer runs.
     * This method will not necessarily be called from the target thread!
     */
    protected abstract Looper getLooper();

    private Handler getHandler() {
        // This is only called from "our" thread, so no need to synchronize.
        if (handler == null) {
            handler = new Handler(getLooper(), this::handleMessage);
        }
        return handler;
    }

    // Called by the Public Layer to associate API method calls with their inter-thread messages.
    protected <T> int registerCallback(Consumer<T> callback, Class<T> type) {
        CallbackWrapper<T> wrapper = new CallbackWrapper<>();
        wrapper.type = type;
        wrapper.callback = callback;

        int id = callbackWrappers.size();
        callbackWrappers.add(wrapper);
        return id;
    }

    // Special-case for API methods that take no input.
    protected int registerCallback(Runnable callback) {
        Consumer<Object> wrapper = o -> callback.run();
        return registerCallback(wrapper, Object.class);
    }

    private void callInterThreadHelper(int callbackId, Object untypedInput) {
        Message msg = Message.obtain();
        msg.arg1 = callbackId;
        msg.obj = untypedInput;
        getHandler().sendMessage(msg);
    }

    // Used by Public Layer to send message to the correct thread.
    protected <T> void callInterThread(int callbackId, T input) {
        callInterThreadHelper(callbackId, input);
    }

    // Special-case for messages with no data (for methods with no input)
    protected void callInterThread(int callbackId) {
        callInterThreadHelper(callbackId, null);
    }

    // This is what actually does the required work on the target thread when an inter-thread request is received.
    private <T> void runCallback(CallbackWrapper<T> wrapper, Object input) {
        Consumer<T> callback = wrapper.callback;
        @SuppressWarnings("unchecked") T typedInput = (T) input;
        callback.accept(typedInput);
    }

    /*
     * This method will be called by the Android system, on the target thread, when a new message is received.
     */
    @SuppressWarnings("SameReturnValue")
    private boolean handleMessage(Message message) {
        // Figure out what we need to do in response to this message.
        int callbackId = message.arg1;
        CallbackWrapper<?> wrapper = callbackWrappers.get(callbackId);
        Object untypedCallbackInput = message.obj;
        runCallback(wrapper, untypedCallbackInput);
        return true;
    }
}
