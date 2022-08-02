package su.thepeople.carstereo;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.NoLibraryException;
import su.thepeople.carstereo.data.Song;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class controls what songs are (or aren't) playing. It mainly translates simplified messages from the UI and
 * passes on more detailed instructions to the music player.
 */
public class MusicController extends LooperThread<MusicControllerAPI> {

    private static final String LOG_ID = "MusicController";

    // This object handles communications from other object in the system (including ones on other threads)
    private MusicControllerAPI api;

    // An object that can send messages to the UI.
    private final MainActivityAPI mainActivity;

    // Helper object which knows how to dump songs into the queued playlist.
    private SongProvider songProvider;

    // Helper objects which actually knows how to play music.
    private MusicPlayer musicPlayer;

    private final Context context;
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


    // These are the "modes" that control which songs get played in which order.
    public enum PlayMode {
        BAND,
        ALBUM,
        YEAR,
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
        List<Song> newBatch = songProvider.getNextBatch();

        // If songprovider does not provide anything for the next batch, then switch to all-shuffle mode
        if (newBatch.isEmpty()) {
            Log.d(LOG_ID, "Song provider returned empty list, changing to shuffle mode");
            transitionToShuffle(replaceCurrentSong);
            newBatch = songProvider.getNextBatch();
        }
        musicPlayer.setPlaylist(getInfoForSongs(newBatch), replaceCurrentSong);
    }

    private void transitionToShuffle(boolean replaceCurrentSong) {
        mode = PlayMode.SHUFFLE;
        songProvider = new SongProvider.ShuffleProvider(database);
        MusicController.this.replenishPlaylist(replaceCurrentSong);
        mainActivity.notifyPlayModeChange(PlayMode.SHUFFLE);
    }

    /**
     * This helper class implements the "public API". All of its methods will be called on the controller's thread,
     * even if the original request came from a different thread. Therefore, it is safe to do things like make expensive
     * calls to the Database, since there is no chance of holding up another thread while we are doing so.
     */
    protected class MusicControllerAPIImpl extends MusicControllerAPI {

        private final Looper looper;

        protected MusicControllerAPIImpl(Looper looper) {
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
            Log.d(LOG_ID, "Restarting current album");
            enterAlbumLock(true);
        }

        @Override
        protected void onChangeSubMode() {
            /*
             * This code could be a lot better. The idea here is that we have different "play modes"
             * (e.g. we could be in album-lock mode or shuffle mode, etc.).  On top of that, each
             * mode might have its own set of defined sub-modes. This method will jump to the next
             * sub-mode.
             *
             * Right now, SHUFFLE is the only mode that has any sub-modes. The three sub-modes are:
             * normal shuffle, double-shot weekend, and block-party weekend.
             *
             * This code here is currently just a quick-and-dirty implementation. If we add more
             * sub-modes, we will likely want to introduce an abstract interface, and have each mode
             * define its own sub-modes. But for now, this works okay.
             */
            if (mode == PlayMode.SHUFFLE) {
                Log.d(LOG_ID, "Shuffle sub-mode toggle");

                if (songProvider instanceof SongProvider.ShuffleProvider) {
                    // If we're in basic shuffle, change to double-shot
                    SongInfo song = musicPlayer.getCurrentSong();
                    Optional<Band> startingBand = (song == null) ? Optional.empty() : Optional.of(song.band);
                    songProvider = new SongProvider.DoubleShotProvider(database, startingBand);
                } else if (songProvider instanceof SongProvider.DoubleShotProvider) {
                    // If we're in double-shot, change to block party
                    songProvider = new SongProvider.BlockPartyProvider(database);
                } else {
                    // Otherwise, we're in block party, so switch back to basic shuffle
                    songProvider = new SongProvider.ShuffleProvider(database);
                }
                MusicController.this.replenishPlaylist(false);
            }
        }

        @Override
        protected void onToggleBandMode() {
            if (mode == PlayMode.BAND) {
                transitionToShuffle(true);
            } else {
                SongInfo song = musicPlayer.getCurrentSong();
                if (song != null) {
                    long bandId = song.band.uid;
                    songProvider = new SongProvider.BandProvider(database, bandId);
                    mode = PlayMode.BAND;
                    MusicController.this.replenishPlaylist(false);
                    mainActivity.notifyPlayModeChange(mode);
                }
            }
        }

        private void enterAlbumLock(boolean forceFromBeginning) {
            SongInfo song = musicPlayer.getCurrentSong();
            Log.d(LOG_ID, String.format("Locking on album () with song ()", song.album, song.toString()));
            if (song != null && song.album != null) {
                long albumId = song.album.uid;
                if (forceFromBeginning) {
                    songProvider = new SongProvider.AlbumProvider(database, albumId);
                } else {
                    songProvider = new SongProvider.AlbumProvider(database, albumId, song.song.uid);
                }
                mode = PlayMode.ALBUM;
                MusicController.this.replenishPlaylist(forceFromBeginning);
                mainActivity.notifyPlayModeChange(PlayMode.ALBUM);
            }
        }

        private void enterYearLock() {
            SongInfo songInfo = musicPlayer.getCurrentSong();
            if (songInfo != null && songInfo.song.year != null) {
                int year = songInfo.song.year;
                songProvider = new SongProvider.EraProvider(database, year, year);
                mode = PlayMode.YEAR;
                MusicController.this.replenishPlaylist(false);
                mainActivity.notifyPlayModeChange(PlayMode.YEAR);
            }
        }

        @Override
        protected void onToggleAlbumMode() {
            if (mode == PlayMode.ALBUM) {
                transitionToShuffle(true);
            } else {
                enterAlbumLock(false);
            }
        }

        @Override
        protected void onToggleYearMode() {
            if (mode == PlayMode.YEAR) {
                transitionToShuffle(true);
            } else {
                enterYearLock();
            }
        }

        @Override
        protected void onReplenishPlaylist() {
            MusicController.this.replenishPlaylist(false);
        }

        @Override
        protected void onLockSpecificBand(long bandId) {
            songProvider = new SongProvider.BandProvider(database, bandId);
            mode = PlayMode.BAND;
            MusicController.this.replenishPlaylist(bandId != musicPlayer.getCurrentSong().band.uid);
            mainActivity.notifyPlayModeChange(mode);
        }

        @Override
        protected void onLockSpecificAlbum(long albumId) {
            songProvider = new SongProvider.AlbumProvider(database, albumId);
            mode = PlayMode.ALBUM;
            MusicController.this.replenishPlaylist(true);
            mainActivity.notifyPlayModeChange(mode);
        }

        @Override
        protected void onLockSpecificEra(MainActivity.Era era) {
            songProvider = new SongProvider.EraProvider(database, era.startYear, era.endYear);
            mode = PlayMode.YEAR;
            MusicController.this.replenishPlaylist(true);
            mainActivity.notifyPlayModeChange(mode);
        }

        @Override
        protected void onRequestBandList() {
            mainActivity.fulfillBandListRequest(database.bandDAO().getAll());
        }

        @Override
        protected void onRequestAlbumList() {
            long bandId = musicPlayer.getCurrentSong().band.uid;
            List<Album> albums = database.albumDAO().getAllForBand(bandId);
            mainActivity.fulfillAlbumListRequest(albums);
        }

        @Override
        protected void onRequestYearList() {
            List<Integer> years = database.songDAO().getYears();
            mainActivity.fulfillYearListRequest(years);
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
