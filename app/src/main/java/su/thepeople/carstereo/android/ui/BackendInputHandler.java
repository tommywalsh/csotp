package su.thepeople.carstereo.android.ui;


import android.util.Log;

import su.thepeople.carstereo.lib.data.BackendStatus;
import su.thepeople.carstereo.lib.interthread.BackendException;
import su.thepeople.carstereo.lib.platform_interface.UINotificationAPI;

/**
 * This class's job is to receive all messages from the backend, and dispatch them (after any
 * necessary processing/unwrapping) to the MainUI.
 */
public class BackendInputHandler extends UINotificationAPI {
    private final static String LOG_ID = "Backend Input Handler";
    private final MainUI mainUI;

    public BackendInputHandler(MainUI mainUI) {
        this.mainUI = mainUI;
    }

    @Override
    protected void onBackendStatusChange(BackendStatus newStatus) {
        mainUI.updateBackendStatus(newStatus);
    }

    @Override
    protected void onBandListResponse(BandListWrapper wrapper) {
        Log.d(LOG_ID, "Received band list from backend");
        mainUI.openBandPicker(wrapper.bands);
    }

    @Override
    protected void onAlbumListResponse(AlbumListWrapper wrapper) {
        Log.d(LOG_ID, "Received album list from backend");
        mainUI.openAlbumPicker(wrapper.albums);
    }

    @Override
    protected void onYearListResponse(YearListWrapper wrapper) {
        Log.d(LOG_ID, "Received year list from backend");
        mainUI.openYearPicker(wrapper.years);
    }

    @Override
    protected void onExceptionReport(BackendException exception) {
        // Currently we only have one type of exception. This code will need improvement if/when that changes.
        Log.e(LOG_ID, "Received exception report from backend: ", exception);
        mainUI.reportProblem(exception);
    }
}
