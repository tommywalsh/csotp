package su.thepeople.carstereo.lib.platform_interface;

import su.thepeople.carstereo.lib.util.NonNull;
import su.thepeople.carstereo.lib.util.Nullable;

/**
 * Simple interface for all the logging that the backend requires.
 * The details of logging may vary from platform to platform.
 */
public interface LogProvider {
    void debug(@Nullable java.lang.String tag, @NonNull java.lang.String msg);
    void error(@Nullable java.lang.String tag, @NonNull java.lang.String msg, Throwable throwable);
    void verbose(@Nullable java.lang.String tag, @NonNull java.lang.String msg);
    void warning(@Nullable java.lang.String tag, @NonNull java.lang.String msg);
}
