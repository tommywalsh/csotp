package su.thepeople.carstereo;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

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

    static class AlbumListWrapper implements Serializable {
        ArrayList<Album> albums;
        AlbumListWrapper(List<Album> list) {
            albums = new ArrayList<>(list);
        }
    }
    private static final int ALBUM_CHOOSER = 1;

    static class BandListWrapper implements Serializable {
        ArrayList<Band> bands;
        BandListWrapper(List<Band> list) {
            bands = new ArrayList<>(list);
        }
    }
    private static final int BAND_CHOOSER = 2;

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
        } else if (obj instanceof AlbumListWrapper) {
            AlbumListWrapper wrapper = (AlbumListWrapper) obj;
            Intent intent = new Intent(MainActivity.this, ItemChooser.AlbumChooser.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("ALBUMS", wrapper.albums);
            intent.putExtras(bundle);
            MainActivity.this.startActivityForResult(intent, ALBUM_CHOOSER);
        } else if (obj instanceof BandListWrapper) {
            BandListWrapper wrapper = (BandListWrapper) obj;
            Intent intent = new Intent(MainActivity.this, ItemChooser.BandChooser.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("BANDS", wrapper.bands);
            intent.putExtras(bundle);
            MainActivity.this.startActivityForResult(intent, BAND_CHOOSER);
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

        void fulfillAlbumListRequest(List<Album> albums) {
            sendMessage(new AlbumListWrapper(albums));
        }

        void fulfillBandListRequest(List<Band> bands) {
            sendMessage(new BandListWrapper(bands));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.hideSystemUI(this, R.id.mainTable);
    }

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        bandWidget = findViewById(R.id.band);
        albumWidget = findViewById(R.id.album);
        songWidget = findViewById(R.id.song);
        songWidget.setSelected(true);

        // Normal click locks/unlocks the current band. Long click allows custom selection of band.
        bandWidget.setOnCheckedChangeListener((view, checked) -> onBandModeToggle());
        bandWidget.setOnLongClickListener(view -> onBandChooserRequest());

        albumWidget.setOnCheckedChangeListener((view, checked) -> onAlbumModeToggle());
        albumWidget.setOnLongClickListener(view -> onAlbumChooserRequest());

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
        musicRequester.requestAlbumList();
        return true;
    }

    /**
     * This follows the same strategy as onAlbumChooserRequest()
     */
    private boolean onBandChooserRequest() {
        musicRequester.requestBandList();
        return true;
    }

    /**
     * This method is called when the user has chosen an item (album or band) from an item picker. We need to relay
     * the user's choice over to the music controller.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            int itemId = result.getIntExtra("ITEM_ID", -1);
            if (requestCode == ALBUM_CHOOSER) {
                musicRequester.lockExplicitAlbum(itemId);
            } else if (requestCode == BAND_CHOOSER) {
                musicRequester.lockExplicitBand(itemId);
            }
        }
    }

    @Override
    protected void onDestroy() {
        musicThread.interrupt();
        super.onDestroy();
    }
}
