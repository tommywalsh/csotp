package su.thepeople.carstereo.lib.platform_interface;

import su.thepeople.carstereo.lib.backend.MusicControllerThread;

// Central provider class for all of the various platform-specific objects that the backend needs.
public interface PlatformAdapter {
    ObjectCreator getObjectCreator();

    BandFetcher getBandFetcher();

    AlbumFetcher getAlbumFetcher();

    SongFetcher getSongFetcher();

    MusicPlayer createMusicPlayer(MusicControllerThread controller);

    LogProvider getLogProvider();

    MessagingSystem createMessagingSystemForCurrentThread();
}
