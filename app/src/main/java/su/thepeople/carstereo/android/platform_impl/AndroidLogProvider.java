package su.thepeople.carstereo.android.platform_impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import su.thepeople.carstereo.lib.platform_interface.LogProvider;

/**
 * This passes along logging requests from the backend to the Android system logger
 */
public class AndroidLogProvider implements LogProvider {

    @Override
    public void debug(@Nullable String tag, @NonNull String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void error(@Nullable String tag, @NonNull String msg, Throwable throwable) {
        Log.e(tag, msg, throwable);
    }

    @Override
    public void verbose(@Nullable String tag, @NonNull String msg) {
        Log.v(tag, msg);
    }

    @Override
    public void warning(@Nullable String tag, @NonNull String msg) {
        Log.w(tag, msg);
    }
}
