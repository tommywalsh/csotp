package su.thepeople.carstereo.lib.backend;

import su.thepeople.carstereo.lib.data.Album;
import su.thepeople.carstereo.lib.data.Band;
import su.thepeople.carstereo.lib.platform_interface.PlatformAdapter;
import su.thepeople.carstereo.lib.data.Song;
import su.thepeople.carstereo.lib.data.BackendStatus;
import su.thepeople.carstereo.lib.interthread.LooperThread;
import su.thepeople.carstereo.lib.interthread.MusicControllerAPI;
import su.thepeople.carstereo.lib.data.SongInfo;
import su.thepeople.carstereo.lib.platform_interface.UINotificationAPI;
import su.thepeople.carstereo.lib.platform_interface.MusicPlayer;
import su.thepeople.carstereo.lib.util.Log;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class controls what songs are (or aren't) playing.
 *
 * This class talks back and forth to the UI. It is in charge of controlling the music player, and
 * changing between play modes.
 */
public class MusicControllerThread extends LooperThread<MusicControllerAPI> {

    private static final String LOG_ID = "MusicController";

    // An object that can send messages to the UI.
    private final UINotificationAPI uiNotifier;

    // Helper objects which actually knows how to play music.
    private MusicPlayer musicPlayer;

    private final PlatformAdapter platformAdapter;

    public MusicControllerThread(UINotificationAPI uiNotifier, PlatformAdapter platformAdapter) {
        super(platformAdapter);
        this.uiNotifier = uiNotifier;
        this.platformAdapter = platformAdapter;
    }

    @Override
    protected MusicControllerAPI setupCommunications() {
        return new MusicControllerAPIImpl();
    }

    @Override
    protected void beforeMainLoop() {
        musicPlayer = platformAdapter.createMusicPlayer(this);
        musicSelector = new MusicSelector.CollectionMode(platformAdapter);
        replenishPlaylist(true);
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
        List<? extends Song> newBatch = musicSelector.getSongProvider().getNextBatch();

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
        musicSelector = new MusicSelector.CollectionMode(platformAdapter);
        MusicControllerThread.this.replenishPlaylist(replaceCurrentSong);
        sendChangeNotification();
    }

    /**
     * This helper class implements the "public API". All of its methods will be called on the controller's thread,
     * even if the original request came from a different thread. Therefore, it is safe to do things like make expensive
     * calls to the Database, since there is no chance of holding up another thread while we are doing so.
     */
    protected class MusicControllerAPIImpl extends MusicControllerAPI {

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
                MusicControllerThread.this.replenishPlaylist(true);
            }
        }

        @Override
        protected void onSkipForward() {
            Log.d(LOG_ID, "Skipping forward");
            boolean changed = musicSelector.resyncForward(musicPlayer.getCurrentSong());
            if (changed) {
                MusicControllerThread.this.replenishPlaylist(true);
            }
        }

        @Override
        protected void onChangeSubMode() {
            boolean wasChanged = musicSelector.changeSubMode(musicPlayer.getCurrentSong());
            if (wasChanged) {
                MusicControllerThread.this.replenishPlaylist(false);
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
                    long bandId = song.band.getUid();
                    musicSelector = new MusicSelector.BandMode(platformAdapter, bandId);
                    MusicControllerThread.this.replenishPlaylist(false);
                    sendChangeNotification();
                }
            }
        }

        private void enterAlbumLock() {
            SongInfo song = musicPlayer.getCurrentSong();
            Log.d(LOG_ID, String.format("Locking on album %s with song %s", song.album, song));
            if (song.album != null) {
                long albumId = song.album.getUid();
                musicSelector = new MusicSelector.AlbumMode(platformAdapter, albumId, Optional.of(song.song.getUid()));
                MusicControllerThread.this.replenishPlaylist(false);
            }
            sendChangeNotification();
        }

        private void enterYearLock() {
            SongInfo songInfo = musicPlayer.getCurrentSong();
            if (songInfo != null && songInfo.song.getYear() != null) {
                musicSelector = new MusicSelector.YearMode(platformAdapter, songInfo.song.getYear());
                MusicControllerThread.this.replenishPlaylist(false);
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
            musicSelector = new MusicSelector.BandMode(platformAdapter, bandId);
            MusicControllerThread.this.replenishPlaylist(bandId != musicPlayer.getCurrentSong().band.getUid());
            sendChangeNotification();
        }

        @Override
        protected void onLockSpecificAlbum(long albumId) {
            musicSelector = new MusicSelector.AlbumMode(platformAdapter, albumId, Optional.empty());
            MusicControllerThread.this.replenishPlaylist(true);
            sendChangeNotification();
        }

        @Override
        protected void onLockSpecificYear(int year) {
            musicSelector = new MusicSelector.YearMode(platformAdapter, year);
            MusicControllerThread.this.replenishPlaylist(true);
            sendChangeNotification();
        }

        @Override
        protected void onRequestBandList() {
            uiNotifier.fulfillBandListRequest(platformAdapter.getBandFetcher().getAll());
        }

        @Override
        protected void onRequestAlbumList() {
            long bandId = musicPlayer.getCurrentSong().band.getUid();
            List<? extends Album> albums = platformAdapter.getAlbumFetcher().getAllForBand(bandId);
            uiNotifier.fulfillAlbumListRequest(albums);
        }

        @Override
        protected void onRequestYearList() {
            List<Integer> years = platformAdapter.getSongFetcher().getYears();
            uiNotifier.fulfillYearListRequest(years);
        }
    }

    private List<SongInfo> getInfoForSongs(List<? extends Song> songs) {
        return songs.stream().map(song -> {
            Band band = platformAdapter.getBandFetcher().lookup(song.getBandId());
            if (song.getAlbumId() != null) {
                return new SongInfo(band, song, platformAdapter.getAlbumFetcher().lookup(song.getAlbumId()));
            } else {
                return new SongInfo(band, song);
            }
        }).collect(Collectors.toList());
    }
}
