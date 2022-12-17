package su.thepeople.carstereo.lib.platform_interface;

import su.thepeople.carstereo.lib.interthread.InterThreadAPI;

/**
 * Different platforms may handle interthread messaging differently.  This interface declares all the functionality needed by the backend.
 *
 * This interface assumes that each interthread message has a designated "target thread", on which a messaging loop will be running.
 */
public interface MessagingSystem {

    // This method is called on the target thread, before the messaging loop runs, in order to do any setup required.
    void prepareThreadForMessageReception(InterThreadAPI receiver);

    /*
     * This may be called on the target thread. It starts a loop which will dispatch incoming messages to callbacks provided by the given receiver.
     * This method does not need to be called if the target thread manages its own looping.
     */
    void runMessagingLoop();

    // This method may be called from any thread at all. Its job is to deliver the message to the target thread.
    void sendMessage(int callbackId, Object data);
}
