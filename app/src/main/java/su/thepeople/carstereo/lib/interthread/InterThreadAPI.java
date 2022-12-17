package su.thepeople.carstereo.lib.interthread;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import su.thepeople.carstereo.lib.platform_interface.MessagingSystem;

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
        protected Consumer<T> callback;
    }

    // All of the callback wrappers for this API will be stored in this list.
    private final List<CallbackWrapper<?>> callbackWrappers = new ArrayList<>();

    private MessagingSystem messenger;

    public void initializeMessaging(MessagingSystem messenger) {
        this.messenger = messenger;
    }

    // Called by the Public Layer to associate API method calls with their inter-thread messages.
    protected <T> int registerCallback(Consumer<T> callback) {
        CallbackWrapper<T> wrapper = new CallbackWrapper<>();
        wrapper.callback = callback;

        int id = callbackWrappers.size();
        callbackWrappers.add(wrapper);
        return id;
    }

    // Special-case for API methods that take no input.
    protected int registerCallback(Runnable callback) {
        Consumer<Object> wrapper = o -> callback.run();
        return registerCallback(wrapper);
    }

    private void callInterThreadHelper(int callbackId, Object untypedInput) {
        if (messenger != null) {
            messenger.sendMessage(callbackId, untypedInput);
        }
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

    public void handleMessage(int callbackId, Object untypedCallbackInput) {
        CallbackWrapper<?> wrapper = callbackWrappers.get(callbackId);
        runCallback(wrapper, untypedCallbackInput);
    }
}
