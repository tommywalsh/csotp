package su.thepeople.carstereo.backend;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import su.thepeople.carstereo.data.BackendStatus;
import su.thepeople.carstereo.interthread.LooperThread;
import su.thepeople.carstereo.interthread.MusicControllerAPI;
import su.thepeople.carstereo.data.SongInfo;
import su.thepeople.carstereo.interthread.UINotificationAPI;
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

    // An object that can send messages to the UI.
    private final UINotificationAPI uiNotifier;

    // Helper objects which actually knows how to play music.
    private MusicPlayer musicPlayer;

    private final Context context;
    private Database database;

    public MusicController(UINotificationAPI uiNotifier, Context context) {
        this.uiNotifier = uiNotifier;
        this.context = context;
    }

    @Override
    protected MusicControllerAPI setupCommunications() {
        return new MusicControllerAPIImpl(Looper.myLooper());
    }

    @Override
    protected void beforeMainLoop() {
        musicPlayer = new MusicPlayer(this);
        try {
            database = Database.getDatabase(context);
            musicSelector = new MusicSelector.CollectionMode(database);
            replenishPlaylist(true);
        } catch (NoLibraryException e) {
            uiNotifier.reportException(e);
        }
    }

    public void onSongAdvance() {
        sendChangeNotification();
    }

    public void onPlayerQueueEmpty() {
        replenishPlaylist(false);
    }

    // These are the "modes" that control which songs get played in which order.
    public enum PlayModeEnum {
        BAND,
        ALBUM,
        YEAR,
        SHUFFLE
    }

    private MusicSelector musicSelector;

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
        List<Song> newBatch = musicSelector.getSongProvider().getNextBatch();

        // If song provider does not provide anything for the next batch, then switch to all-shuffle mode
        if (newBatch.isEmpty()) {
            Log.d(LOG_ID, "Song provider returned empty list, changing to shuffle mode");
            transitionToShuffle(replaceCurrentSong);
            newBatch = musicSelector.getSongProvider().getNextBatch();
        }
        musicPlayer.setPlaylist(getInfoForSongs(newBatch), replaceCurrentSong);
    }

    private void sendChangeNotification() {
        BackendStatus status = new BackendStatus();
        status.currentSong = musicPlayer.getCurrentSong();
        status.isPlaying = (playState == PlayState.PLAYING);
        status.mode = musicSelector.getModeType();
        status.subModeIDString = musicSelector.getSubModeIDString();
        uiNotifier.notifyBackendStatusChange(status);
    }

    private void transitionToShuffle(boolean replaceCurrentSong) {
        musicSelector = new MusicSelector.CollectionMode(database);
        MusicController.this.replenishPlaylist(replaceCurrentSong);
        sendChangeNotification();
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
            sendChangeNotification();
        }

        @Override
        protected void onSkipAhead() {
            musicPlayer.prepareNextSong();
        }

        @Override
        protected void onForcePause() {
            musicPlayer.pause();
            playState = PlayState.PAUSED;
            sendChangeNotification();
        }

        @Override
        protected void onRestartCurrentSong() {
            musicPlayer.restartCurrent();
        }

        @Override
        protected void onSkipBackward() {
            Log.d(LOG_ID, "Skipping backwards");
            boolean changed = musicSelector.resyncBackward(musicPlayer.getCurrentSong());
            if (changed) {
                MusicController.this.replenishPlaylist(true);
            }
        }

        @Override
        protected void onSkipForward() {
            Log.d(LOG_ID, "Skipping forward");
            boolean changed = musicSelector.resyncForward(musicPlayer.getCurrentSong());
            if (changed) {
                MusicController.this.replenishPlaylist(true);
            }
        }

        @Override
        protected void onChangeSubMode() {
            boolean wasChanged = musicSelector.changeSubMode(musicPlayer.getCurrentSong());
            if (wasChanged) {
                MusicController.this.replenishPlaylist(false);
                sendChangeNotification();
            }
        }

        @Override
        protected void onToggleBandMode() {
            if (musicSelector instanceof MusicSelector.BandMode) {
                transitionToShuffle(false);
            } else {
                SongInfo song = musicPlayer.getCurrentSong();
                if (song != null) {
                    long bandId = song.band.uid;
                    musicSelector = new MusicSelector.BandMode(database, bandId);
                    MusicController.this.replenishPlaylist(false);
                    sendChangeNotification();
                }
            }
        }

        private void enterAlbumLock() {
            SongInfo song = musicPlayer.getCurrentSong();
            Log.d(LOG_ID, String.format("Locking on album %s with song %s", song.album, song));
            if (song.album != null) {
                long albumId = song.album.uid;
                musicSelector = new MusicSelector.AlbumMode(database, albumId, Optional.of(song.song.uid));
                MusicController.this.replenishPlaylist(false);
            }
            sendChangeNotification();
        }

        private void enterYearLock() {
            SongInfo songInfo = musicPlayer.getCurrentSong();
            if (songInfo != null && songInfo.song.year != null) {
                musicSelector = new MusicSelector.YearMode(database, songInfo.song.year);
                MusicController.this.replenishPlaylist(false);
            }
            sendChangeNotification();
        }

        @Override
        protected void onToggleAlbumMode() {
            if (musicSelector instanceof MusicSelector.AlbumMode) {
                transitionToShuffle(false);
            } else {
                enterAlbumLock();
            }
        }

        @Override
        protected void onToggleYearMode() {
            if (musicSelector instanceof MusicSelector.YearMode) {
                transitionToShuffle(false);
            } else {
                enterYearLock();
            }
        }

        @Override
        protected void onLockSpecificBand(long bandId) {
            musicSelector = new MusicSelector.BandMode(database, bandId);
            MusicController.this.replenishPlaylist(bandId != musicPlayer.getCurrentSong().band.uid);
            sendChangeNotification();
        }

        @Override
        protected void onLockSpecificAlbum(long albumId) {
            musicSelector = new MusicSelector.AlbumMode(database, albumId, Optional.empty());
            MusicController.this.replenishPlaylist(true);
            sendChangeNotification();
        }

        @Override
        protected void onLockSpecificYear(int year) {
            musicSelector = new MusicSelector.YearMode(database, year);
            MusicController.this.replenishPlaylist(true);
            sendChangeNotification();
        }

        @Override
        protected void onRequestBandList() {
            uiNotifier.fulfillBandListRequest(database.bandDAO().getAll());
        }

        @Override
        protected void onRequestAlbumList() {
            long bandId = musicPlayer.getCurrentSong().band.uid;
            List<Album> albums = database.albumDAO().getAllForBand(bandId);
            uiNotifier.fulfillAlbumListRequest(albums);
        }

        @Override
        protected void onRequestYearList() {
            List<Integer> years = database.songDAO().getYears();
            uiNotifier.fulfillYearListRequest(years);
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
