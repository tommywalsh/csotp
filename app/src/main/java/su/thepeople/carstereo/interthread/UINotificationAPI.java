package su.thepeople.carstereo.interthread;

import android.os.Looper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import su.thepeople.carstereo.data.BackendStatus;
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
public abstract class UINotificationAPI extends InterThreadAPI {

    public void notifyBackendStatusChange(BackendStatus status) {
        callInterThread(cb_statusChange, status);
    }

    public void fulfillBandListRequest(List<Band> bands) {
        callInterThread(cb_bandList, new BandListWrapper(bands));
    }

    public void fulfillAlbumListRequest(List<Album> albums) {
        callInterThread(cb_albumList, new AlbumListWrapper(albums));
    }

    public void fulfillYearListRequest(List<Integer> albums) {
        callInterThread(cb_yearList, new YearListWrapper(albums));
    }

    public void reportException(BackendException e) {
        callInterThread(cb_exception, e);
    }

    private final int cb_statusChange;
    private final int cb_bandList;
    private final int cb_albumList;
    private final int cb_yearList;
    private final int cb_exception;

    protected UINotificationAPI() {
        cb_statusChange = registerCallback(this::onBackendStatusChange, BackendStatus.class);
        cb_bandList = registerCallback(this::onBandListResponse, BandListWrapper.class);
        cb_albumList = registerCallback(this::onAlbumListResponse, AlbumListWrapper.class);
        cb_yearList = registerCallback(this::onYearListResponse, YearListWrapper.class);
        cb_exception = registerCallback(this::onExceptionReport, BackendException.class);
    }

    protected Looper getLooper() {
        return Looper.getMainLooper();
    }

    protected static class BandListWrapper implements Serializable {
        public final ArrayList<Band> bands;

        BandListWrapper(List<Band> list) {
            bands = new ArrayList<>(list);
        }
    }

    protected static class AlbumListWrapper implements Serializable {
        public final ArrayList<Album> albums;

        AlbumListWrapper(List<Album> list) {
            albums = new ArrayList<>(list);
        }
    }

    protected static class YearListWrapper implements Serializable {
        public final ArrayList<Integer> years;

        YearListWrapper(List<Integer> list) {
            years = new ArrayList<>(list);
        }
    }

    protected abstract void onBackendStatusChange(BackendStatus newStatus);

    protected abstract void onBandListResponse(BandListWrapper bands);

    protected abstract void onAlbumListResponse(AlbumListWrapper albums);

    protected abstract void onYearListResponse(YearListWrapper years);

    protected abstract void onExceptionReport(BackendException exception);
}