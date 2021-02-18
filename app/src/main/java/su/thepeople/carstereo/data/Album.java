package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * A simple POJO representing an Album
 */
@Entity
public class Album implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public long uid;

    @NonNull
    public String name;

    public Integer year;

    public long bandId;

    public Album(@NonNull String name, long bandId, @Nullable Integer year) {
        this.name = name;
        this.bandId = bandId;
        this.year = year;
    }
}
