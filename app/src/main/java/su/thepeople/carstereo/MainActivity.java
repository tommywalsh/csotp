package su.thepeople.carstereo;

import android.annotation.SuppressLint;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import su.thepeople.carstereo.MusicController.PlayModeEnum;

/**
 * A full-screen activity that plays on-disk music files. This is intended to be used as a car stereo.
 *
 * This activity is optimized to:
 *   - Support only three simple modes of listening
 *      - Random tracks selected from the entire collections
 *      - All songs by the same band in a random order
 *      - All songs on an album in running order.
 *   - Be controllable without concentration or precise finger movements, so that it can be used while driving.
 *      - Buttons are huge.
 *      - On-screen information is limited, and presented in large text.
 *
 *
 *  There is not a lot of logic in this file. This code mainly handles three tasks:
 *     - Passing user requests to the backend
 *     - Starting sub-activities for features that aren't provided by the main UI
 *     - Updating the screen based on backend changes.
 */
@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity {

    // UI widgets that we need to update/control.
    private ToggleButton bandWidget;
    private ToggleButton albumWidget;
    private ToggleButton yearWidget;
    private TextView songWidget;
    private ImageButton playPauseWidget;
    private ImageButton nextSongWidget;
    private TextView messageWidget;

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

    /**
     * This helper class defines what happens when other parts of the app send us a message.
     */
    private class MainActivityAPIImpl extends MainActivityAPI {

        @Override
        protected void onPlayModeChange(PlayModeEnum mode) {
            callbackLock.run(() -> {
                Log.d(LOG_ID, String.format("Reacting to play mode change: band is%s locked, album is%s locked",
                        mode == PlayModeEnum.BAND ? "" : " not",
                        mode == PlayModeEnum.ALBUM ? "" : " not"));
                bandWidget.setChecked(mode == PlayModeEnum.BAND);
                albumWidget.setChecked(mode == PlayModeEnum.ALBUM);
                yearWidget.setChecked(mode == PlayModeEnum.YEAR);
            });
        }

        @Override
        protected void onPlayStateChange(boolean isPlaying) {
            Log.d(LOG_ID, String.format("Reacting to play state change: we are now%s playing", isPlaying ? "" : " not"));
            if (isPlaying) {
                playPauseWidget.setImageResource(R.drawable.ic_pause_button);
            } else {
                playPauseWidget.setImageResource(R.drawable.ic_play_button);
            }
        }

        @Override
        protected void onSubModeChange(int subModeIDString) {
            Log.d(LOG_ID, String.format("Updating to new sub-mode %s", getResources().getString(subModeIDString)));
            messageWidget.setText(subModeIDString);
        }

        @Override
        protected void onCurrentSongChange(SongInfo currentSong) {
            Log.d(LOG_ID, String.format("Reacting to song change: (%s)(%s)(%s)",
                    currentSong.band.name,
                    currentSong.album == null ? "<none>" : currentSong.album.name,
                    currentSong.song.name));
            songWidget.setText(currentSong.song.name);
            bandWidget.setText(currentSong.band.name);
            bandWidget.setTextOn(currentSong.band.name);
            bandWidget.setTextOff(currentSong.band.name);
            albumWidget.setText(currentSong.album == null ? "" : currentSong.album.name);
            albumWidget.setTextOn(currentSong.album == null ? "" : currentSong.album.name);
            albumWidget.setTextOff(currentSong.album == null ? "" : currentSong.album.name);
            String year = "";
            if (currentSong.song.year != null) {
                year = currentSong.song.year.toString();
            }
            yearWidget.setText(year);
            yearWidget.setTextOn(year);
            yearWidget.setTextOff(year);
        }

        @Override
        protected void onBandListResponse(BandListWrapper wrapper) {
            Log.d(LOG_ID, "Received band list from backend");
            subActivityManager.runSubActivity(bandPickerId, wrapper.bands);
        }

        @Override
        protected void onAlbumListResponse(AlbumListWrapper wrapper) {
            Log.d(LOG_ID, "Received album list from backend");
            subActivityManager.runSubActivity(albumPickerId, wrapper.albums);
        }

        @Override
        protected void onYearListResponse(YearListWrapper wrapper) {
            Log.d(LOG_ID, "Received year list from backend");
            subActivityManager.runSubActivity(yearPickerId, wrapper.years);
        }

        @Override
        protected void onExceptionReport(Exception exception) {
            // Currently we only have one type of exception. This code will need improvement if/when that changes.
            Log.e(LOG_ID, "Reporting error to user", exception);
            playPauseWidget.setEnabled(false);
            nextSongWidget.setEnabled(false);
            bandWidget.setEnabled(false);
            albumWidget.setEnabled(false);
            bandWidget.setText(R.string.error);
            songWidget.setText(R.string.no_library);
        }
    }


    /**
     * This handles turning off and on in response to bluetooth connections.
     */
    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected. Keep the screen on
                Log.d(LOG_ID, "Detected bluetooth connection made. Forcing screen on");
                screenLocker.ensureScreenOn();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected. Pause (if running), and allow the screen to turn off
                Log.d(LOG_ID, "Detected bluetooth connection lost. Pausing music and releasing screen lock");
                controller.forcePause();
                screenLocker.allowScreenToShutOff();
            }
        }
    }

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        albumPickerId = subActivityManager.addSubActivityDefinition(ItemChooser.AlbumChooser.getIODefinition(), a -> controller.lockSpecificAlbum(a.uid));
        bandPickerId = subActivityManager.addSubActivityDefinition(ItemChooser.BandChooser.getIODefinition(), b -> controller.lockSpecificBand(b.uid));
        yearPickerId = subActivityManager.addSubActivityDefinition(ItemChooser.YearChooser.getIODefinition(), era -> controller.lockSpecificYear(era));
        songChooserId = subActivityManager.addSubActivityDefinition(SongChooser.getIODefinition(), r -> onSongChooserResponse(r));

        // Set up the main screen
        setContentView(R.layout.activity_fullscreen);

        // Normal click locks/unlocks the current band. Long click allows custom selection of band.
        bandWidget = findViewById(R.id.band);
        bandWidget.setOnCheckedChangeListener((view, checked) -> onBandModeToggle());
        bandWidget.setOnLongClickListener(view -> onBandChooserRequest());

        // Normal click locks/unlocks the current album. Long click allows custom selection of album.
        albumWidget = findViewById(R.id.album);
        albumWidget.setOnCheckedChangeListener((view, checked) -> onAlbumModeToggle());
        albumWidget.setOnLongClickListener(view -> onAlbumChooserRequest());

        // Normal click locks/unlocks the current year. Long click allows custom selection of year.
        yearWidget = findViewById(R.id.year);
        yearWidget.setOnCheckedChangeListener((view, checked) -> onYearModeToggle());
        yearWidget.setOnLongClickListener(view -> onYearChooserRequest());

        // Keep the song widget selected so that marquee scrolling will work.
        songWidget = findViewById(R.id.song);
        songWidget.setSelected(true);

        messageWidget = findViewById(R.id.message);

        // Normal click toggles play/pause. Long click changes sub-mode.
        playPauseWidget = findViewById(R.id.playPause);
        playPauseWidget.setOnClickListener(view -> onPlayPauseToggle());
        playPauseWidget.setOnLongClickListener(view -> onPlaySubModeChangeRequest());

        // Normal click advances to next song. Long click brings up more navigation options.
        nextSongWidget = findViewById(R.id.next);
        nextSongWidget.setOnClickListener(view -> onNextSongRequest());
        nextSongWidget.setOnLongClickListener(view -> onSongChooserRequest());

        // Register to receive Bluetooth events
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(new BluetoothReceiver(), filter);

        // Initiate the other pieces of this app.
        screenLocker = new ScreenLocker(this);
        musicThread = new MusicController(new MainActivityAPIImpl(), getApplicationContext());
        controller = musicThread.startThread();

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


    private void onBandModeToggle() {
        // Use lock to make sure we only do work in response to user presses on the band button
        callbackLock.run(controller::toggleBandMode);
    }

    private void onAlbumModeToggle() {
        // Use lock to make sure we only do work in response to user presses on the album button
        callbackLock.run(controller::toggleAlbumMode);
    }

    private void onYearModeToggle() {
        callbackLock.run(controller::toggleYearMode);
    }

    private void onPlayPauseToggle() {
        controller.togglePlayPause();
    }

    private void onNextSongRequest() {
        controller.nextSong();
    }

    /**
     * This is called when the user asks to pick an album. The data/control flow is a little convoluted....
     *   1) This method is called to notify us of the user's request.
     *   2) We send a message over to the music controller asking for a list of albums appropriate to the current song.
     *   3) The controller will send us back a message containing the album list.
     *   4) The album list is packaged into an Intent, which is used to start up an AlbumPicker
     *   5) (if the user selects an album) The album picker sends back the ID of the album picked.
     *   6) We send a message to the music controller asking it to "lock" on the selected album.
     */
    private boolean onAlbumChooserRequest() {
        Log.d(LOG_ID, "Requesting album list from controller");
        controller.requestAlbumList();
        return true;
    }

    /**
     * This follows the same strategy as onAlbumChooserRequest()
     */
    private boolean onYearChooserRequest() {
        Log.d(LOG_ID, "Requesting year list from controller");
        controller.requestYearList();
        return true;
    }

    /**
     * This follows the same strategy as onAlbumChooserRequest()
     */
    private boolean onBandChooserRequest() {
        Log.d(LOG_ID, "Requesting band list from controller");
        controller.requestBandList();
        return true;
    }

    /**
     * For songs, we use a different strategy than for bands and albums, because we have no need to query the backend.
     * We always provide the same 4 options:
     *    - Restart song
     *    - Next song
     *    - Jump backward
     *    - Jump forward
     * The UI does not care about the exact meaning of the last two -- that's up to the mode/submode backend code.
     */
    private boolean onSongChooserRequest() {
        Log.d(LOG_ID, "Opening song navigation UI");
        subActivityManager.runSubActivity(songChooserId, Boolean.TRUE);
        return true;
    }

    private boolean onPlaySubModeChangeRequest() {
        Log.d(LOG_ID, "User wants to change submode");
        controller.changeSubMode();
        return true;
    }

    private void onSongChooserResponse(int songChooserCode) {
        Log.d(LOG_ID, String.format("Got song navigation response: %s", Integer.toString(songChooserCode)));
        if (songChooserCode == SongChooser.NEXT_SONG) {
            controller.nextSong();
        } else if (songChooserCode == SongChooser.RESTART_SONG) {
            controller.restartCurrentSong();
        } else if (songChooserCode == SongChooser.SKIP_BACK) {
            controller.skipBackward();
        } else if (songChooserCode == SongChooser.SKIP_FORWARD) {
            controller.skipForward();
        }
    }

    /**
     * This method is called when the user has completed a sub-activity (e.g. has chosen a band to lock on)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            subActivityManager.processSubActivityResult(requestCode, result);
        }
    }
}
