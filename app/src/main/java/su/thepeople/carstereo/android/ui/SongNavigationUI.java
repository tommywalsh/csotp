package su.thepeople.carstereo.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.function.Supplier;

import su.thepeople.carstereo.R;

/**
 * This is a small pop-up window that displays a few buttons for navigating through songs.
 */
public class SongNavigationUI extends AppCompatActivity {

    private static final String LOG_ID = "Main Activity";

    private static final String RESULT_NAME = "NAVIGATION_REQUEST";

    public enum NavigationRequest {
        GO_BACK,
        RESTART,
        NEXT,
        GO_FORWARD;

        /*
         * Ugly helper method to redirect calls based on enum value. Better to tuck away the ugly code here than have
         * it mixed in with real logic elsewhere.
         */
        public void switchOnResult(Supplier<Boolean> skipBackAction, Supplier<Boolean> restartSongAction, Supplier<Boolean> nextSongAction, Supplier<Boolean> skipForwardAction) {
            switch (this) {
                case GO_BACK:
                    skipBackAction.get();
                    break;
                case RESTART:
                    restartSongAction.get();
                    break;
                case NEXT:
                    nextSongAction.get();
                    break;
                case GO_FORWARD:
                    skipForwardAction.get();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_ID, "Song Chooser created");

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        assert bundle != null;

        setContentView(R.layout.activity_song_chooser);

        ImageButton firstSongButton = findViewById(R.id.first_song);
        firstSongButton.setOnClickListener(v -> navigateRequest(NavigationRequest.GO_BACK));

        ImageButton thisSongButton = findViewById(R.id.this_song);
        thisSongButton.setOnClickListener(v -> navigateRequest(NavigationRequest.RESTART));

        ImageButton nextSongButton = findViewById(R.id.next_song);
        nextSongButton.setOnClickListener(v -> navigateRequest(NavigationRequest.NEXT));

        ImageButton skipForwardButton = findViewById(R.id.skip_forward);
        skipForwardButton.setOnClickListener(v -> navigateRequest(NavigationRequest.GO_FORWARD));
    }

    private void navigateRequest(NavigationRequest request) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_NAME, request);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    // The "boolean" input is just a dummy argument. It is not used.
    public static SubActivityManager.ActivityIODefinition<Boolean, NavigationRequest> getIODefinition() {
        return new SubActivityManager.ActivityIODefinition<>(SongNavigationUI.class, "DUMMY", Boolean.class, RESULT_NAME, NavigationRequest.class);
    }
}
