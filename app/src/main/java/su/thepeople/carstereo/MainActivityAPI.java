package su.thepeople.carstereo;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

/**
 * This class handles cross-thread communications to the MainActivity.
 *
 * The various non-private/protected methods and datatypes are meant to be used by other components of the system that are not
 * running on the same thread as the MainActivity. This class will handle sending a message to the MainActivity on its
 * own thread.
 *
 * The abstract methods are implemented by the MainActivity itself, and will be run on the MainActivity thread.
 *
 * Internally, the class uses a simple strategy: we have a small collection of simple datatypes that are packed into
 * an Android Handler message. We keep a 1:1 correspondence between datatype and message so that we just need to look
 * at the object type to know which message is being sent.
 */
public abstract class MainActivityAPI {

    void notifyPlayModeChange(boolean isBandLocked, boolean isAlbumLocked) {
        PlayMode mode = new PlayMode();
        mode.isBandLocked = isBandLocked;
        mode.isAlbumLocked = isAlbumLocked;
        sendMessage(mode);
    }

    void notifyPlayStateChange(boolean isPlaying) {
        sendMessage(isPlaying);
    }

    void notifyCurrentSongChange(SongInfo currentSong) {
        sendMessage(currentSong);
    }

    void fulfillBandListRequest(List<Band> bands) {
        sendMessage(new BandListWrapper(bands));
    }

    void fulfillAlbumListRequest(List<Album> albums) {
        sendMessage(new AlbumListWrapper(albums));
    }

    void reportException(Exception e) {
        sendMessage(e);
    }

    private Handler handler;

    MainActivityAPI() {
        this.handler = new Handler(Looper.getMainLooper(), this::handleMessage);
    }

    private static class PlayMode {
        boolean isBandLocked;
        boolean isAlbumLocked;
    }

    private static class BandListWrapper implements Serializable {
        ArrayList<Band> bands;

        BandListWrapper(List<Band> list) {
            bands = new ArrayList<>(list);
        }
    }

    private static class AlbumListWrapper implements Serializable {
        ArrayList<Album> albums;

        AlbumListWrapper(List<Album> list) {
            albums = new ArrayList<>(list);
        }
    }

    // Helper method that sends an object of any type over to the MainActivity thread.
    private void sendMessage(Object object) {
        Message msg = Message.obtain();
        msg.obj = object;
        handler.sendMessage(msg);
    }

    protected abstract void onPlayModeChange(boolean isBandLocked, boolean isAlbumLocked);

    protected abstract void onPlayStateChange(boolean isPlaying);

    protected abstract void onCurrentSongChange(SongInfo currentSong);

    protected abstract void onBandListResponse(ArrayList<Band> bands);

    protected abstract void onAlbumListResponse(ArrayList<Album> albums);

    protected abstract void onExceptionReport(Exception exception);

    private boolean handleMessage(Message message) {
        Object obj = message.obj;

        if (obj instanceof PlayMode) {
            // The play mode has changed. Update the on-screen toggle buttons to reflect the new mode.
            PlayMode mode = (PlayMode) obj;
            onPlayModeChange(mode.isBandLocked, mode.isAlbumLocked);
        } else if (obj instanceof Boolean) {
            // Music has started or stopped playing. Update the on-screen play/pause button to match.
            onPlayStateChange((Boolean) obj);
        } else if (obj instanceof SongInfo) {
            // A new song is being played. Update the text on the screen to match.
            SongInfo info = (SongInfo) obj;
            onCurrentSongChange(info);
        } else if (obj instanceof AlbumListWrapper) {
            AlbumListWrapper wrapper = (AlbumListWrapper) obj;
            onAlbumListResponse(wrapper.albums);
        } else if (obj instanceof BandListWrapper) {
            BandListWrapper wrapper = (BandListWrapper) obj;
            onBandListResponse(wrapper.bands);
        } else if (obj instanceof Exception) {
            onExceptionReport((Exception) obj);
        }
        // True here means that we have completed all required processing of the message.
        return true;
    }
}