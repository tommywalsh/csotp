package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Simple POJO class representing a Band
 */
@Entity
public class Band implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @NonNull
    public String name;

    public Band(@NonNull String name) {
        this.name = name;
    }
}
