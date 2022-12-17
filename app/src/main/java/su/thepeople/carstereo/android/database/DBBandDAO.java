package su.thepeople.carstereo.android.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * CRUD methods for bands in the database.
 */
@Dao
public interface DBBandDAO {

    @Query("SELECT * FROM DBBand ORDER BY name")
    List<DBBand> getAll();

    @Query("SELECT * FROM DBBand WHERE uid = :bandId")
    DBBand lookup(long bandId);

    @Query("SELECT * FROM DBBand ORDER BY random() LIMIT 1")
    DBBand getRandom();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DBBand band);
}
