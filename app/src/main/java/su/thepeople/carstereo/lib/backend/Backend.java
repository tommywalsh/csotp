package su.thepeople.carstereo.lib.backend;

import java.io.File;
import java.util.function.Supplier;
import java.util.stream.Stream;

import su.thepeople.carstereo.lib.platform_interface.UINotificationAPI;
import su.thepeople.carstereo.lib.platform_interface.PlatformAdapter;
import su.thepeople.carstereo.lib.util.Log;

public class Backend {
    private static volatile Backend instance;

    private final PlatformAdapter adapter;

    private Backend(PlatformAdapter adapter) {
        this.adapter = adapter;
    }

    public static Backend initializePlatform(PlatformAdapter adapter) {
        if (instance == null) {
            synchronized(Backend.class) {
                if (instance == null) {
                    Log.setProvider(adapter.getLogProvider());
                    instance = new Backend(adapter);
                }
            }
        }
        return instance;
    }

    public void scanCollection(Supplier<Stream<File>> collectionSearchDirs) {
        MusicScanner scanner = new MusicScanner(adapter, collectionSearchDirs);
        scanner.scan();
    }

    public MusicControllerThread spawnMusicThread(UINotificationAPI uiNotifier) {
        MusicControllerThread musicThread = new MusicControllerThread(uiNotifier, adapter);
        musicThread.startThread();
        return musicThread;
    }
}
