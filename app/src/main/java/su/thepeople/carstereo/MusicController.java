package su.thepeople.carstereo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class controls what songs are (or aren't playing). It mainly translates simplified messages from the UI and
 * passes on more detailed instructions to the music player.
 */
class MusicController {
    // Commands that are supported by this controller.
    private static final int TOGGLE_PLAY_PAUSE = 1;
    private static final int SKIP_AHEAD = 2;
    private static final int TOGGLE_BAND_MODE = 3;
    private static final int TOGGLE_ALBUM_MODE = 4;
    private static final int REPLENISH_PLAYLIST = 5;

    // An object that can send messages to the UI.
    private MainActivity.Updater uiUpdater;

    // Helper object which knows how to dump songs into the queued playlist.
    private SongProvider songProvider;

    // Helper objects which actually knows how to play music.
    private MusicPlayer musicPlayer;

    private Context context;
    private Database database;

    MusicController(MainActivity.Updater updater, Context context) {
        uiUpdater = updater;
        this.context = context;
    }

    // These are the three "modes" that control which songs get played in which order.
    private enum PlayMode {
        BAND,
        ALBUM,
        SHUFFLE
    }
    private PlayMode mode = PlayMode.SHUFFLE;

    private void toggleBandMode() {
        if (mode == PlayMode.BAND) {
            mode = PlayMode.SHUFFLE;
            songProvider = new SongProvider.ShuffleProvider(database);
            replenishPlaylist(true);
        } else {
            SongInfo song = musicPlayer.getCurrentSong();
            if (song != null) {
                long bandId = song.band.uid;
                songProvider = new SongProvider.BandProvider(database, bandId);
                mode = PlayMode.BAND;
                replenishPlaylist(false);
            }
        }
        uiUpdater.updatePlayMode(mode == PlayMode.BAND, mode == PlayMode.ALBUM);
    }

    private void toggleAlbumMode() {
        if (mode == PlayMode.ALBUM) {
            mode = PlayMode.SHUFFLE;
            songProvider = new SongProvider.ShuffleProvider(database);
            replenishPlaylist(true);
        } else {
            SongInfo song = musicPlayer.getCurrentSong();
            if (song != null && song.album != null) {
                long albumId = song.album.uid;
                songProvider = new SongProvider.AlbumProvider(database, albumId, song.song.uid);
                mode = PlayMode.ALBUM;
                replenishPlaylist(false);
            }
        }
        uiUpdater.updatePlayMode(mode == PlayMode.BAND, mode == PlayMode.ALBUM);
    }

    // Should the system be playing music right now, or not?
    private enum PlayState {
        PLAYING,
        PAUSED
    }
    private PlayState playState = PlayState.PAUSED;

    private void togglePlayPause() {
        if (playState == PlayState.PAUSED) {
            musicPlayer.play();
            playState = PlayState.PLAYING;
        } else {
            musicPlayer.pause();
            playState = PlayState.PAUSED;
        }
        uiUpdater.updatePlayState(playState == PlayState.PLAYING);
    }

    private void skipAhead() {
        musicPlayer.prepareNextSong();
    }


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
     * Helper class, used by other parts of the system to send requests to this class. These requests will often be
     * made from another thread.
     */
    static class Requester {
        private volatile Handler handler;

        private void sendMessage(int signal) {
            if (handler != null) {
                Message msg = Message.obtain();
                msg.arg1 = signal;
                handler.sendMessage(msg);
            }
        }

        void togglePlayPause() {
            sendMessage(TOGGLE_PLAY_PAUSE);
        }

        void skipAhead() {
            sendMessage(SKIP_AHEAD);
        }

        void toggleBandMode() {
            sendMessage(TOGGLE_BAND_MODE);
        }

        void toggleAlbumMode() {
            sendMessage(TOGGLE_ALBUM_MODE);
        }

        void replenishPlaylist() {
            sendMessage(REPLENISH_PLAYLIST);
        }
    }

    private Requester requester = new Requester();

    Requester getRequester() {
        return requester;
    }

    /**
     * Bottleneck method where we respond to messages sent from other parts of the app (often from a different thread)
     */
    private boolean handleMessage(Message message) {
        switch (message.arg1) {
            case TOGGLE_BAND_MODE:
                toggleBandMode();
                break;
            case TOGGLE_ALBUM_MODE:
                toggleAlbumMode();
                break;
            case TOGGLE_PLAY_PAUSE:
                togglePlayPause();
                break;
            case SKIP_AHEAD:
                skipAhead();
                break;
            case REPLENISH_PLAYLIST:
                replenishPlaylist(false);
                break;
            default:
                // We should never get here, so we should assert, but asserts are apparently unreliable, so do nothing.
                break;
        }

        // "true" means "We've done all required processing of this messages"
        return true;
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

    /**
     * This method will be called just before this object's event loop starts. Here we can set up all of the objects
     * we'll be interacting with on this thread, and set up messaging to/from all objects (whether on this thread or
     * not).
     */
    void setupHandlers(Looper looper) {
        requester.handler = new Handler(looper, this::handleMessage);
        musicPlayer = new MusicPlayer(uiUpdater, requester);
        database = Database.getDatabase(context);
        songProvider = new SongProvider.ShuffleProvider(database);
        replenishPlaylist(true);
    }
}
