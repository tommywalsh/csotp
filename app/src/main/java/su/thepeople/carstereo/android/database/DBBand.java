package su.thepeople.carstereo.android.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Simple POJO class representing a Band
 */
@Entity
public class DBBand implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public long uid;

    @NonNull
    public final String name;

    public DBBand(@NonNull String name) {
        this.name = name;
    }

    public long getUid() {
        return uid;
    }

    @NonNull
    public String getName() {
        return name;
    }
}
