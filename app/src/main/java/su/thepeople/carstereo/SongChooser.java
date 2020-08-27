package su.thepeople.carstereo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SongChooser extends AppCompatActivity {

    private static final String LOG_ID = "Main Activity";

    public static final int FIRST_SONG = 1;
    public static final int THIS_SONG = 2;
    public static final int NEXT_SONG = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_ID, "Song Chooser created");

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        setContentView(R.layout.activity_song_chooser);

        Button firstSongButton = findViewById(R.id.first_song);
        firstSongButton.setEnabled(bundle.getBoolean("SHOW_FIRST_SONG"));
        firstSongButton.setOnClickListener(v -> chooseSong(FIRST_SONG));

        Button thisSongButton = findViewById(R.id.this_song);
        thisSongButton.setOnClickListener(v -> chooseSong(THIS_SONG));

        Button nextSongButton = findViewById(R.id.next_song);
        nextSongButton.setOnClickListener(v -> chooseSong(NEXT_SONG));
    }

    private void chooseSong(int songSpecifier) {
        Intent intent = new Intent();
        intent.putExtra("SONG_SPECIFIER", songSpecifier);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
