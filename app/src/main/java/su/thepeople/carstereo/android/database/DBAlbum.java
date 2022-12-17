package su.thepeople.carstereo.android.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;


/**
 * A simple POJO representing an Album
 */
@Entity
public class DBAlbum implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Nullable
    private final Integer year;

    @NonNull
    private final String name;

    private final long bandId;

    public DBAlbum(@NonNull String name, long bandId, @Nullable Integer year) {
        this.name = name;
        this.bandId = bandId;
        this.year = year;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public long getBandId() {
        return bandId;
    }
}
