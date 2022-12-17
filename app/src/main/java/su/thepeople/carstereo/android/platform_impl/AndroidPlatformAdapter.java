package su.thepeople.carstereo.android.platform_impl;

import android.os.Looper;

import su.thepeople.carstereo.android.database.AndroidDatabase;
import su.thepeople.carstereo.lib.backend.MusicControllerThread;
import su.thepeople.carstereo.lib.platform_interface.MessagingSystem;
import su.thepeople.carstereo.lib.platform_interface.LogProvider;
import su.thepeople.carstereo.lib.platform_interface.MusicPlayer;
import su.thepeople.carstereo.lib.platform_interface.AlbumFetcher;
import su.thepeople.carstereo.lib.platform_interface.ObjectCreator;
import su.thepeople.carstereo.lib.platform_interface.PlatformAdapter;
import su.thepeople.carstereo.lib.platform_interface.SongFetcher;
import su.thepeople.carstereo.lib.platform_interface.BandFetcher;

public class AndroidPlatformAdapter implements PlatformAdapter {

    private final AndroidObjectCreator objectCreator;
    private final AndroidBandFetcher bandFetcher;
    private final AndroidAlbumFetcher albumFetcher;
    private final AndroidSongFetcher songFetcher;
    private final AndroidLogProvider logProvider;

    public AndroidPlatformAdapter(AndroidDatabase database) {
        this.objectCreator = new AndroidObjectCreator(database);
        this.bandFetcher = new AndroidBandFetcher(database.bandDAO());
        this.albumFetcher = new AndroidAlbumFetcher(database.albumDAO());
        this.songFetcher = new AndroidSongFetcher(database.songDAO());
        this.logProvider = new AndroidLogProvider();
    }

    @Override public ObjectCreator getObjectCreator() { return objectCreator; }

    @Override public BandFetcher getBandFetcher() {
        return bandFetcher;
    }

    @Override public AlbumFetcher getAlbumFetcher() { return albumFetcher; }

    @Override public SongFetcher getSongFetcher() {
        return songFetcher;
    }

    @Override public MusicPlayer createMusicPlayer(MusicControllerThread controller) {
        return new AndroidMusicPlayer(controller);
    }

    @Override public LogProvider getLogProvider() { return logProvider; }

    @Override public MessagingSystem createMessagingSystemForCurrentThread() {
        Looper myLooper = Looper.myLooper();
        if (myLooper == null) {
            Looper.prepare();
        }
        return new AndroidMessagingSystem(Looper.myLooper());
    }

}
