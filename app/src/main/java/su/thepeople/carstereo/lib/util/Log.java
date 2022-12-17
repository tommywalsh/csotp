package su.thepeople.carstereo.lib.util;

import su.thepeople.carstereo.lib.platform_interface.LogProvider;

public class Log {
    private static LogProvider provider;

    public static void setProvider(LogProvider newProvider) {
        provider = newProvider;
    }

    public static void d(@Nullable java.lang.String tag, @NonNull java.lang.String msg) {
        if (provider != null) {
            provider.debug(tag, msg);
        }
    }

    public static void e(@Nullable java.lang.String tag, @NonNull java.lang.String msg, Throwable throwable) {
        if (provider != null) {
            provider.error(tag, msg, throwable);
        }
    }

    public static void v(@Nullable java.lang.String tag, @NonNull java.lang.String msg) {
        if (provider != null) {
            provider.verbose(tag, msg);
        }
    }

    public static void w(@Nullable java.lang.String tag, @NonNull java.lang.String msg) {
        if (provider != null) {
            provider.warning(tag, msg);
        }
    }
}
