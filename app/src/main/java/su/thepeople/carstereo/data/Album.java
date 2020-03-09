package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A simple POJO representing an Album
 */
@Entity
public class Album {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @NonNull
    public String name;

    public long bandId;

    public Album(@NonNull String name, long bandId) {
        this.name = name;
        this.bandId = bandId;
    }
}
