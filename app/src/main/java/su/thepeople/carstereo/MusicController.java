package su.thepeople.carstereo;

import android.content.Context;
import android.os.Looper;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.NoLibraryException;
import su.thepeople.carstereo.data.Song;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class controls what songs are (or aren't) playing. It mainly translates simplified messages from the UI and
 * passes on more detailed instructions to the music player.
 */
public class MusicController extends LooperThread<MusicControllerAPI> {

    // This object handles communications from other object in the system (including ones on other threads)
    private MusicControllerAPI api;

    // An object that can send messages to the UI.
    private MainActivityAPI mainActivity;

    // Helper object which knows how to dump songs into the queued playlist.
    private SongProvider songProvider;

    // Helper objects which actually knows how to play music.
    private MusicPlayer musicPlayer;

    private Context context;
    private Database database;

    public MusicController(MainActivityAPI mainActivity, Context context) {
        this.mainActivity = mainActivity;
        this.context = context;
    }

    @Override
    protected MusicControllerAPI setupCommunications() {
        api = new MusicControllerAPIImpl(Looper.myLooper());
        return api;
    }

    @Override
    protected void beforeMainLoop() {
        musicPlayer = new MusicPlayer(mainActivity, api);
        try {
            database = Database.getDatabase(context);
            songProvider = new SongProvider.ShuffleProvider(database);
            replenishPlaylist(true);
        } catch (NoLibraryException e) {
            mainActivity.reportException(e);
        }
    }


    // These are the three "modes" that control which songs get played in which order.
    private enum PlayMode {
        BAND,
        ALBUM,
        SHUFFLE
    }
    private PlayMode mode = PlayMode.SHUFFLE;


    // Should the system be playing music right now, or not?
    private enum PlayState {
        PLAYING,
        PAUSED
    }
    private PlayState playState = PlayState.PAUSED;


    /**
     * Helper method to fill up the queue of upcoming songs. This might be called because we are changing modes, or it
     * might be called because the queue is almost empty.
     *
     * @param replaceCurrentSong - If false, the currently-playing song continues playing. If true, we jump to the next
     *                           song.
     */
    private void replenishPlaylist(boolean replaceCurrentSong) {
        musicPlayer.setPlaylist(getInfoForSongs(songProvider.getNextBatch()), replaceCurrentSong);
    }

    /**
     * This helper class implements the "public API". All of its methods will be called on the controller's thread,
     * even if the original request came from a different thread. Therefore, it is safe to do things like make expensive
     * calls to the Database, since there is no chance of holding up another thread while we are doing so.
     */
    private class MusicControllerAPIImpl extends MusicControllerAPI {

        private Looper looper;

        public MusicControllerAPIImpl(Looper looper) {
            this.looper = looper;
        }

        @Override
        protected void onTogglePlayPause() {
            if (playState == PlayState.PAUSED) {
                musicPlayer.play();
                playState = PlayState.PLAYING;
            } else {
                musicPlayer.pause();
                playState = PlayState.PAUSED;
            }
            mainActivity.notifyPlayStateChange(playState == PlayState.PLAYING);
        }

        @Override
        protected void onSkipAhead() {
            musicPlayer.prepareNextSong();
        }

        @Override
        protected void onForcePause() {
            musicPlayer.pause();
            playState = PlayState.PAUSED;
            mainActivity.notifyPlayStateChange(false);
        }

        @Override
        protected void onRestartCurrentSong() {
            musicPlayer.restartCurrent();
        }

        @Override
        protected void onRestartCurrentAlbum() {
            enterAlbumLock();
            MusicController.this.replenishPlaylist(true);
        }

        @Override
        protected void onToggleBandMode() {
            if (mode == PlayMode.BAND) {
                mode = PlayMode.SHUFFLE;
                songProvider = new SongProvider.ShuffleProvider(database);
                MusicController.this.replenishPlaylist(true);
            } else {
                SongInfo song = musicPlayer.getCurrentSong();
                if (song != null) {
                    long bandId = song.band.uid;
                    songProvider = new SongProvider.BandProvider(database, bandId);
                    mode = PlayMode.BAND;
                    MusicController.this.replenishPlaylist(false);
                }
            }
            mainActivity.notifyPlayModeChange(mode == PlayMode.BAND, mode == PlayMode.ALBUM);
        }

        private void enterAlbumLock() {
            SongInfo song = musicPlayer.getCurrentSong();
            if (song != null && song.album != null) {
                long albumId = song.album.uid;
                songProvider = new SongProvider.AlbumProvider(database, albumId, song.song.uid);
                mode = PlayMode.ALBUM;
                MusicController.this.replenishPlaylist(false);
                mainActivity.notifyPlayModeChange(false, true);
            }
        }
        @Override
        protected void onToggleAlbumMode() {
            if (mode == PlayMode.ALBUM) {
                mode = PlayMode.SHUFFLE;
                songProvider = new SongProvider.ShuffleProvider(database);
                MusicController.this.replenishPlaylist(true);
                mainActivity.notifyPlayModeChange(false, false);
            } else {
                enterAlbumLock();
            }
        }

        @Override
        protected void onReplenishPlaylist() {
            MusicController.this.replenishPlaylist(false);
        }

        @Override
        protected void onLockSpecificBand(int bandId) {
            songProvider = new SongProvider.BandProvider(database, bandId);
            mode = PlayMode.BAND;
            MusicController.this.replenishPlaylist(bandId != musicPlayer.getCurrentSong().band.uid);
            mainActivity.notifyPlayModeChange(true, false);
        }

        @Override
        protected void onLockSpecificAlbum(int albumId) {
            songProvider = new SongProvider.AlbumProvider(database, albumId);
            mode = PlayMode.ALBUM;
            MusicController.this.replenishPlaylist(true);
            mainActivity.notifyPlayModeChange(false, true);
        }

        @Override
        protected void onRequestBandList() {
            mainActivity.fulfillBandListRequest(database.bandDAO().getAll());
        }

        @Override
        protected void onRequestAlbumList() {
            int bandId = musicPlayer.getCurrentSong().band.uid;
            List<Album> albums = database.albumDAO().getAllForBand(bandId);
            mainActivity.fulfillAlbumListRequest(albums);
        }

        @Override
        protected Looper getLooper() {
            return looper;
        }
    }

    private List<SongInfo> getInfoForSongs(List<Song> songs) {
        return songs.stream().map(song -> {
            Band band = database.bandDAO().lookup(song.bandId);
            if (song.albumId != null) {
                return new SongInfo(band, song, database.albumDAO().lookup(song.albumId));
            } else {
                return new SongInfo(band, song);
            }
        }).collect(Collectors.toList());
    }
}
