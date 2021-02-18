package su.thepeople.carstereo;

import android.os.Looper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

/**
 * This class defines the MainActivity functionality that is available from outside the UI thread.
 *
 * Any public method may be called on any thread. This will automatically route the request to the UI thread for
 * processing.
 *
 * The abstract methods are implemented by the MainActivity itself, and will be run on the MainActivity thread.
 */
public abstract class MainActivityAPI extends InterThreadAPI {

    public void notifyPlayModeChange(boolean isBandLocked, boolean isAlbumLocked, boolean isYearLocked) {
        PlayMode mode = new PlayMode();
        mode.isBandLocked = isBandLocked;
        mode.isAlbumLocked = isAlbumLocked;
        mode.isYearLocked = isYearLocked;
        callInterThread(cb_playMode, mode);
    }

    public void notifyPlayStateChange(boolean isPlaying) { callInterThread(cb_playState, isPlaying); }

    public void notifyCurrentSongChange(SongInfo currentSong) {
        callInterThread(cb_currentSong, currentSong);
    }

    public void fulfillBandListRequest(List<Band> bands) {
        callInterThread(cb_bandList, new BandListWrapper(bands));
    }

    public void fulfillAlbumListRequest(List<Album> albums) {
        callInterThread(cb_albumList, new AlbumListWrapper(albums));
    }

    public void reportException(Exception e) {
        callInterThread(cb_exception, e);
    }

    private int cb_playMode;
    private int cb_playState;
    private int cb_currentSong;
    private int cb_bandList;
    private int cb_albumList;
    private int cb_exception;

    protected MainActivityAPI() {
        cb_playMode = registerCallback(this::onPlayModeChange, PlayMode.class);
        cb_playState = registerCallback(this::onPlayStateChange, Boolean.class);
        cb_currentSong = registerCallback(this::onCurrentSongChange, SongInfo.class);
        cb_bandList = registerCallback(this::onBandListResponse, BandListWrapper.class);
        cb_albumList = registerCallback(this::onAlbumListResponse, AlbumListWrapper.class);
        cb_exception = registerCallback(this::onExceptionReport, Exception.class);
    }

    protected Looper getLooper() {
        return Looper.getMainLooper();
    }

    protected static class PlayMode {
        boolean isBandLocked;
        boolean isAlbumLocked;
        boolean isYearLocked;
    }

    protected static class BandListWrapper implements Serializable {
        ArrayList<Band> bands;

        BandListWrapper(List<Band> list) {
            bands = new ArrayList<>(list);
        }
    }

    protected static class AlbumListWrapper implements Serializable {
        ArrayList<Album> albums;

        AlbumListWrapper(List<Album> list) {
            albums = new ArrayList<>(list);
        }
    }

    protected abstract void onPlayModeChange(PlayMode newPlayMode);

    protected abstract void onPlayStateChange(boolean isPlaying);

    protected abstract void onCurrentSongChange(SongInfo currentSong);

    protected abstract void onBandListResponse(BandListWrapper bands);

    protected abstract void onAlbumListResponse(AlbumListWrapper albums);

    protected abstract void onExceptionReport(Exception exception);
}