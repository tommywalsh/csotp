package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple POJO type representing a Song, as backed by an on-disk file.
 */
@Entity
public class Song {
    @PrimaryKey(autoGenerate = true)
    public long uid;

    @NonNull
    public String name;

    @NonNull
    public String fullPath;

    @NonNull
    public Long bandId;

    public Long albumId;

    private static final Pattern filenameRegex = Pattern.compile("^(\\d*)( - )?(.*)\\.(\\w{3,4})$");

    public Song(@NonNull String name, @NonNull String fullPath, @NonNull Long bandId, Long albumId) {
        Matcher matcher = filenameRegex.matcher(name);
        this.name = name;
        if (matcher.matches()) {
            String songName = matcher.group(3);
            if (songName != null) {
                this.name = songName;
            }
        }
        this.fullPath = fullPath;
        this.bandId = bandId;
        this.albumId = albumId;
    }
}
