package su.thepeople.carstereo.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * CRUD methods for albums in the database.
 */
@Dao
public interface AlbumDAO {

    // TODO: This query not guaranteed to work with multiple albums in the same year
    @Query("SELECT * FROM Album WHERE bandId = :bandId ORDER BY year, name")
    List<Album> getAllForBand(long bandId);

    @Query("SELECT * FROM Album WHERE uid = :albumId")
    Album lookup(long albumId);

    @Query("SELECT * FROM Album ORDER BY random() LIMIT 1")
    Album getRandom();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Album album);
}
