package su.thepeople.carstereo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class SongChooser extends AppCompatActivity {

    private static final String LOG_ID = "Main Activity";

    public static final int SKIP_BACK = 1;
    public static final int RESTART_SONG = 2;
    public static final int NEXT_SONG = 3;
    public static final int SKIP_FORWARD = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_ID, "Song Chooser created");

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        assert bundle != null;

        setContentView(R.layout.activity_song_chooser);

        ImageButton firstSongButton = findViewById(R.id.first_song);
        firstSongButton.setEnabled(bundle.getBoolean("SHOW_FIRST_SONG"));
        firstSongButton.setOnClickListener(v -> chooseSong(SKIP_BACK));

        ImageButton thisSongButton = findViewById(R.id.this_song);
        thisSongButton.setOnClickListener(v -> chooseSong(RESTART_SONG));

        ImageButton nextSongButton = findViewById(R.id.next_song);
        nextSongButton.setOnClickListener(v -> chooseSong(NEXT_SONG));

        ImageButton skipForwardButton = findViewById(R.id.skip_forward);
        skipForwardButton.setOnClickListener(v -> chooseSong(SKIP_FORWARD));
    }

    private void chooseSong(int songSpecifier) {
        Intent intent = new Intent();
        intent.putExtra("SONG_SPECIFIER", songSpecifier);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public static SubActivityManager.ActivityIODefinition<Boolean, Integer> getIODefinition() {
        return new SubActivityManager.ActivityIODefinition<>(SongChooser.class, "SHOW_FIRST_SONG", Boolean.class, "SONG_SPECIFIER", Integer.class);
    }
}
