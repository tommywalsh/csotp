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
 * This class controls what songs are (or aren't) playing.
 *
 * This class talks back and forth to the UI. It is in charge of controlling the music player, and
 * changing between play modes.
 */
public class MusicController extends LooperThread<MusicControllerAPI> {

    private static final String LOG_ID = "MusicController";

    // This object handles communications from other object in the system (including ones on other threads)
    private MusicControllerAPI api;

    // An object that can send messages to the UI.
    private final MainActivityAPI mainActivity;

    // Helper object which knows how to dump songs into the queued playlist.
    //private SongProvider songProvider;

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
            //songProvider = new SongProvider.ShuffleProvider(database);
            playMode = new PlayMode.CollectionMode(database);
            replenishPlaylist(true);
        } catch (NoLibraryException e) {
            mainActivity.reportException(e);
        }
    }


    // These are the "modes" that control which songs get played in which order.
    public enum PlayModeEnum {
        BAND,
        ALBUM,
        YEAR,
        SHUFFLE
    }
   // private PlayMode mode = PlayMode.SHUFFLE;

    private PlayMode playMode;

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
        List<Song> newBatch = playMode.getSongProvider().getNextBatch();

        // If songprovider does not provide anything for the next batch, then switch to all-shuffle mode
        if (newBatch.isEmpty()) {
            Log.d(LOG_ID, "Song provider returned empty list, changing to shuffle mode");
            transitionToShuffle(replaceCurrentSong);
            newBatch = playMode.getSongProvider().getNextBatch();
        }
        musicPlayer.setPlaylist(getInfoForSongs(newBatch), replaceCurrentSong);
    }

    private void transitionToShuffle(boolean replaceCurrentSong) {
        playMode = new PlayMode.CollectionMode(database);
        MusicController.this.replenishPlaylist(replaceCurrentSong);
        mainActivity.notifyPlayModeChange(PlayModeEnum.SHUFFLE);
        mainActivity.notifySubModeChange(playMode.getSubModeIDString());
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
        protected void onSkipBackward() {
            Log.d(LOG_ID, "Skipping backwards");
            boolean changed = playMode.resyncBackward(musicPlayer.getCurrentSong());
            if (changed) {
                MusicController.this.replenishPlaylist(true);
            }
        }

        @Override
        protected void onSkipForward() {
            Log.d(LOG_ID, "Skipping forward");
            boolean changed = playMode.resyncForward(musicPlayer.getCurrentSong());
            if (changed) {
                MusicController.this.replenishPlaylist(true);
            }
        }

        @Override
        protected void onChangeSubMode() {
            boolean wasChanged = playMode.changeSubMode(musicPlayer.getCurrentSong());
            if (wasChanged) {
                MusicController.this.replenishPlaylist(false);
                mainActivity.notifySubModeChange(playMode.getSubModeIDString());
            }
        }

        @Override
        protected void onToggleBandMode() {
            if (playMode instanceof PlayMode.BandMode) {
                transitionToShuffle(false);
            } else {
                SongInfo song = musicPlayer.getCurrentSong();
                if (song != null) {
                    long bandId = song.band.uid;
                    playMode = new PlayMode.BandMode(database, bandId);
                    MusicController.this.replenishPlaylist(false);
                    mainActivity.notifyPlayModeChange(PlayModeEnum.BAND);
                    mainActivity.notifySubModeChange(playMode.getSubModeIDString());
                }
            }
        }

        private void enterAlbumLock(boolean forceFromBeginning) {
            SongInfo song = musicPlayer.getCurrentSong();
            Log.d(LOG_ID, String.format("Locking on album %s with song %s", song.album, song));
            if (song.album != null) {
                long albumId = song.album.uid;
                if (forceFromBeginning) {
                    playMode = new PlayMode.AlbumMode(database, albumId, Optional.empty());
                } else {
                    playMode = new PlayMode.AlbumMode(database, albumId, Optional.of(song.song.uid));
                }
                MusicController.this.replenishPlaylist(forceFromBeginning);
                mainActivity.notifyPlayModeChange(PlayModeEnum.ALBUM);
            }
            mainActivity.notifySubModeChange(playMode.getSubModeIDString());
        }

        private void enterYearLock() {
            SongInfo songInfo = musicPlayer.getCurrentSong();
            if (songInfo != null && songInfo.song.year != null) {
                playMode = new PlayMode.YearMode(database, songInfo.song.year);
                MusicController.this.replenishPlaylist(false);
                mainActivity.notifyPlayModeChange(PlayModeEnum.YEAR);
            }
            mainActivity.notifySubModeChange(playMode.getSubModeIDString());
        }

        @Override
        protected void onToggleAlbumMode() {
            if (playMode instanceof PlayMode.AlbumMode) {
                transitionToShuffle(false);
            } else {
                enterAlbumLock(false);
            }
        }

        @Override
        protected void onToggleYearMode() {
            if (playMode instanceof PlayMode.YearMode) {
                transitionToShuffle(false);
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
            playMode = new PlayMode.BandMode(database, bandId);
            MusicController.this.replenishPlaylist(bandId != musicPlayer.getCurrentSong().band.uid);
            mainActivity.notifyPlayModeChange(PlayModeEnum.BAND);
            mainActivity.notifySubModeChange(playMode.getSubModeIDString());
        }

        @Override
        protected void onLockSpecificAlbum(long albumId) {
            playMode = new PlayMode.AlbumMode(database, albumId, Optional.empty());
            MusicController.this.replenishPlaylist(true);
            mainActivity.notifyPlayModeChange(PlayModeEnum.ALBUM);
            mainActivity.notifySubModeChange(playMode.getSubModeIDString());
        }

        @Override
        protected void onLockSpecificYear(int year) {
            playMode = new PlayMode.YearMode(database, year);
            MusicController.this.replenishPlaylist(true);
            mainActivity.notifyPlayModeChange(PlayModeEnum.YEAR);
            mainActivity.notifySubModeChange(playMode.getSubModeIDString());
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
