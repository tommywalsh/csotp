package su.thepeople.carstereo.ui;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.util.List;

import su.thepeople.carstereo.data.BackendStatus;
import su.thepeople.carstereo.interthread.BackendException;
import su.thepeople.carstereo.interthread.LooperThread;
import su.thepeople.carstereo.backend.MusicController;
import su.thepeople.carstereo.backend.MusicController.PlayModeEnum;
import su.thepeople.carstereo.interthread.MusicControllerAPI;
import su.thepeople.carstereo.R;
import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

/**
 * A full-screen activity that plays on-disk music files. This is intended to be used as a car stereo.
 *
 * This activity is optimized to:
 *   - Support only simple "modes" of listening (e.g. "entire collection on shuffle", "this one album"), etc.
 *   - Be controllable without concentration or precise finger movements, so that it can be used while driving.
 *      - Buttons are huge.
 *      - On-screen information is limited, and presented in large text.
 *
 * This class is responsible for the "front screen" UI. It is also in charge of setting up all of
 * the various threads and objects needed by the app.
 */
@SuppressWarnings("ALL")
public class MainUI extends AppCompatActivity {

    // UI widgets that we need to update/control.
    private ToggleButton bandWidget;
    private ToggleButton albumWidget;
    private ToggleButton yearWidget;
    private TextView songWidget;
    private ImageButton playPauseWidget;
    private ImageButton nextSongWidget;
    private TextView messageWidget;

    private UserInputHandler userInputHandler;
    private BackendInputHandler backendInputHandler;

    // Other parts of the app that we need to communicate with.
    private MusicControllerAPI controller;
    private LooperThread<MusicControllerAPI> musicThread;
    private ScreenLocker screenLocker;

    // Activity IDs for the sub-activities that we expect to supply us with a result.
    private int albumPickerId;
    private int bandPickerId;
    private int yearPickerId;
    private int songChooserId;

    // Some of our UI widgets are interdependent. This helper lets us avoid callbacks triggering each other.
    private RecursionLock callbackLock = new RecursionLock();

    private static final String LOG_ID = "Main Activity";

    private SubActivityManager subActivityManager = new SubActivityManager(this);

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_ID, "Starting Creation of app");
        backendInputHandler = new BackendInputHandler(this);

        musicThread = new MusicController(backendInputHandler, getApplicationContext());
        controller = musicThread.startThread();

        userInputHandler = new UserInputHandler(controller, this);

        screenLocker = new ScreenLocker(this);

        albumPickerId = subActivityManager.addSubActivityDefinition(ItemChooser.AlbumChooser.getIODefinition(), userInputHandler::specificAlbumRequest);
        bandPickerId = subActivityManager.addSubActivityDefinition(ItemChooser.BandChooser.getIODefinition(), userInputHandler::specificBandRequest);
        yearPickerId = subActivityManager.addSubActivityDefinition(ItemChooser.YearChooser.getIODefinition(), userInputHandler::specificYearRequest);
        songChooserId = subActivityManager.addSubActivityDefinition(SongNavigationUI.getIODefinition(),
                r -> r.switchOnResult(
                        userInputHandler::skipBackwardRequest,
                        userInputHandler::restartSongRequest,
                        userInputHandler::nextSongRequest,
                        userInputHandler::skipForwardRequest
                ));

        // Set up the main screen
        setContentView(R.layout.activity_fullscreen);

        // Normal click locks/unlocks the current band. Long click allows custom selection of band.
        bandWidget = findViewById(R.id.band);
        bandWidget.setOnCheckedChangeListener((view, checked) -> callbackLock.run(userInputHandler::bandModeToggleRequest));
        bandWidget.setOnLongClickListener(view -> userInputHandler.bandChooserRequest());

        // Normal click locks/unlocks the current album. Long click allows custom selection of album.
        albumWidget = findViewById(R.id.album);
        albumWidget.setOnCheckedChangeListener((view, checked) -> callbackLock.run(userInputHandler::albumModeToggleRequest));
        albumWidget.setOnLongClickListener(view -> userInputHandler.albumChooserRequest());

        // Normal click locks/unlocks the current year. Long click allows custom selection of year.
        yearWidget = findViewById(R.id.year);
        yearWidget.setOnCheckedChangeListener((view, checked) -> callbackLock.run(userInputHandler::yearModeToggleRequest));
        yearWidget.setOnLongClickListener(view -> userInputHandler.yearChooserRequest());

        // Keep the song widget selected so that marquee scrolling will work.
        songWidget = findViewById(R.id.song);
        songWidget.setSelected(true);

        messageWidget = findViewById(R.id.message);

        // Normal click toggles play/pause. Long click changes sub-mode.
        playPauseWidget = findViewById(R.id.playPause);
        playPauseWidget.setOnClickListener(view -> userInputHandler.playPauseToggleRequest());
        playPauseWidget.setOnLongClickListener(view -> userInputHandler.changeSubModeRequest());

        // Normal click advances to next song. Long click brings up more navigation options.
        nextSongWidget = findViewById(R.id.next);
        nextSongWidget.setOnClickListener(view -> userInputHandler.nextSongRequest());
        nextSongWidget.setOnLongClickListener(view -> userInputHandler.songNavigationRequest());

        // Handle audio connection inputs
        AudioConnectionInputHandler btHandler = new AudioConnectionInputHandler(this);
        btHandler.registerWithSystem(this);

        Log.d(LOG_ID, "Main Activity created");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_ID, "Main Activity resumed");
        Utils.hideSystemUI(this, R.id.mainTable);
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_ID, "Main activity being destroyed");
        musicThread.abandon();
        super.onDestroy();
    }

    public void onAudioConnectionMade() {
        Log.d(LOG_ID, "Now connected to audio device. Forcing screen to stay on");
        screenLocker.ensureScreenOn();
    }

    public void onAudioConnectionLost() {
        Log.d(LOG_ID, "Audio device disconnected. Releasing screen lock and pausing music");
        controller.forcePause();
        screenLocker.allowScreenToShutOff();
    }

    private static void setButtonText(ToggleButton widget, String text) {
        widget.setText(text);
        widget.setTextOn(text);
        widget.setTextOff(text);
    }

    private static String getYearText(@Nullable Integer year) {
        return (year == null) ? "" : year.toString();
    }

    protected void updateBackendStatus(BackendStatus status) {
        Log.d(LOG_ID, String.format("New status: %s, %splaying, %s %s, (%s)",
                status.mode,
                status.isPlaying ? "" : "not ",
                status.currentSong.band.name,
                status.currentSong.song.name,
                getResources().getString(status.subModeIDString)));

        callbackLock.run(() -> {
            bandWidget.setChecked(status.mode == PlayModeEnum.BAND);
            albumWidget.setChecked(status.mode == PlayModeEnum.ALBUM);
            yearWidget.setChecked(status.mode == PlayModeEnum.YEAR);

            if (status.isPlaying) {
                playPauseWidget.setImageResource(R.drawable.ic_pause_button);
            } else {
                playPauseWidget.setImageResource(R.drawable.ic_play_button);
            }

            messageWidget.setText(status.subModeIDString);

            songWidget.setText(status.currentSong.song.name);
            setButtonText(bandWidget, status.currentSong.band.name);
            setButtonText(albumWidget, status.currentSong.album == null ? "" : status.currentSong.album.name);
            setButtonText(yearWidget, getYearText(status.currentSong.song.year));
        });
    }

    public void openSongNavigationUI() {
        Log.d(LOG_ID, "Opening song navigation UI");
        subActivityManager.runSubActivity(songChooserId, Boolean.TRUE);
    }

    public <L extends List<Band> & Serializable> void openBandPicker(L bands) {
        Log.d(LOG_ID, "Opening band picker");
        subActivityManager.runSubActivity(bandPickerId, bands);
    }

    public <L extends List<Album> & Serializable> void openAlbumPicker(L albums) {
        Log.d(LOG_ID, "Opening album picker");
        subActivityManager.runSubActivity(albumPickerId, albums);
    }

    public <L extends List<Integer> & Serializable> void openYearPicker(L years) {
        Log.d(LOG_ID, "Opening year picker");
        subActivityManager.runSubActivity(yearPickerId, years);
    }

    public void reportProblem(BackendException exception) {
        Log.d(LOG_ID, "Reporting exception to user");
        playPauseWidget.setEnabled(false);
        nextSongWidget.setEnabled(false);
        bandWidget.setEnabled(false);
        albumWidget.setEnabled(false);
        bandWidget.setText(R.string.error);
        songWidget.setText(exception.getDescriptionStringID());
    }

    /**
     * Android will call this method after a sub-activity completes. Pass this off to the helper.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            subActivityManager.processSubActivityResult(requestCode, result);
        }
    }
}
