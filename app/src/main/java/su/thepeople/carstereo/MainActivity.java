package su.thepeople.carstereo;

import android.annotation.SuppressLint;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import su.thepeople.carstereo.R;

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
 */
public class MainActivity extends AppCompatActivity {

    // UI widgets that we need to update/control.
    private ToggleButton bandWidget;
    private ToggleButton albumWidget;
    private TextView songWidget;
    private ImageButton playPauseWidget;

    // Other parts of the app that we need to communicate with.
    private MusicController.Requester musicRequester;
    private LooperThread musicThread;

    // This object handles incoming messages from other parts of the app.
    private Handler handler;

    // Simple data types for use in incoming messages.
    private static class PlayMode {
        boolean isBandLocked;
        boolean isAlbumLocked;
    }

    /**
     * This method is called to react to any incoming messages from other parts of the app. These messages typically
     * will be coming from a different thread.
     */
    private boolean handleMessage(Message message) {
        Object obj = message.obj;

        if (obj instanceof PlayMode) {
            // The play mode has changed. Update the on-screen toggle buttons to reflect the new mode.
            PlayMode mode = (PlayMode) obj;
            toggleShortCircuit = true;
            bandWidget.setChecked(mode.isBandLocked);
            albumWidget.setChecked(mode.isAlbumLocked);
            toggleShortCircuit = false;
        } else if (obj instanceof Boolean) {
            // Music has started or stopped playing. Update the on-screen play/pause button to match.
            boolean isPlaying = (Boolean) obj;
            if (isPlaying) {
                playPauseWidget.setImageResource(R.drawable.ic_pause_button);
            } else {
                playPauseWidget.setImageResource(R.drawable.ic_play_button);
            }
            // TODO: update button
        } else if (obj instanceof SongInfo) {
            // A new song is being played. Update the text on the screen to match.
            SongInfo info = (SongInfo) obj;
            songWidget.setText(info.song.name);
            bandWidget.setText(info.band.name);
            bandWidget.setTextOn(info.band.name);
            bandWidget.setTextOff(info.band.name);
            albumWidget.setText(info.album == null ? "" : info.album.name);
            albumWidget.setTextOn(info.album == null ? "" : info.album.name);
            albumWidget.setTextOff(info.album == null ? "" : info.album.name);
        }

        // True here means that we have completed all required processing of the message.
        return true;
    }

    /**
     * Helper class used by other parts of the app to send messages to this MainActivity.
     */
    class Updater {

        // Helper method to send one arbitrary object to this class as a message.
        private void sendMessage(Object object) {
            Message msg = Message.obtain();
            msg.obj = object;
            handler.sendMessage(msg);
        }

        // Notify that the play mode has changed.
        void updatePlayMode(boolean isBandLocked, boolean isAlbumLocked) {
            PlayMode mode = new PlayMode();
            mode.isBandLocked = isBandLocked;
            mode.isAlbumLocked = isAlbumLocked;
            sendMessage(mode);
        }

        // Notify that the play state has changed.
        void updatePlayState(boolean isPlaying) {
            sendMessage(isPlaying);
        }

        // Notify that the currently-playing song has changed.
        void updateSongInfo(SongInfo currentSong) {
            sendMessage(currentSong);
        }
    }


    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        // This app is intended to completely take over the devide. So, go full screen, hide OS controls, etc.
        findViewById(R.id.mainTable).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        bandWidget = findViewById(R.id.band);
        albumWidget = findViewById(R.id.album);
        songWidget = findViewById(R.id.song);
        songWidget.setSelected(true);

        bandWidget.setOnCheckedChangeListener((view, checked) -> onBandModeToggle());
        albumWidget.setOnCheckedChangeListener((view, checked) -> onAlbumModeToggle());

        playPauseWidget = findViewById(R.id.playPause);
        playPauseWidget.setOnClickListener(view -> onPlayPauseToggle());
        ImageButton nextSongWidget = findViewById(R.id.next);
        nextSongWidget.setOnClickListener(view -> onNextSongRequest());

        // Set up incoming messages.
        handler = new Handler(Looper.getMainLooper(), this::handleMessage);
        Updater updater = new Updater();

        // Initiate the other pieces of this app.
        MusicController controller = new MusicController(updater, getApplicationContext());
        musicRequester = controller.getRequester();
        musicThread = new LooperThread(controller::setupHandlers);
        musicThread.start();
    }

    /*
     * We want to use ToggleButtons to show whether or not we are "locked" on a band or an album. But, these buttons
     * have interplay (selecting one means deselecting the other). We need to distinguish "the user pressed this button"
     * from "we just programmatically changed the state of this button". This is a bit of a hack, but we use this
     * boolean to control this. When set to true, we simply ignore button update messages.
     */
    private boolean toggleShortCircuit = false;

    public void onBandModeToggle() {
        if (toggleShortCircuit) {
            return;
        }
        musicRequester.toggleBandMode();
    }

    public void onAlbumModeToggle() {
        if (toggleShortCircuit) {
            return;
        }
        musicRequester.toggleAlbumMode();
    }

    public void onPlayPauseToggle() {
        musicRequester.togglePlayPause();
    }

    public void onNextSongRequest() {
        musicRequester.skipAhead();
    }

    @Override
    protected void onDestroy() {
        musicThread.interrupt();
        super.onDestroy();
    }
}
