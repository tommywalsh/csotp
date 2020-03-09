package su.thepeople.carstereo.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * CRUD methods for bands in the database.
 */
@Dao
public interface BandDAO {

    @Query("SELECT * FROM Band ORDER BY name")
    List<Band> getAll();

    @Query("SELECT * FROM Band WHERE uid = :bandId")
    Band lookup(long bandId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Band band);
}
