package su.thepeople.carstereo.android.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * CRUD methods for songs in the database.
 */
@Dao
public interface DBSongDAO {

    @Query("SELECT * FROM DBSong WHERE bandId = :bandId ORDER BY random()")
    List<DBSong> getAllForBandShuffled(Long bandId);

    @Query("SELECT * FROM DBSong WHERE bandId = :bandId ORDER BY year, fullPath")
    List<DBSong> getAllForBandOrdered(Long bandId);

    @Query("SELECT * FROM DBSong WHERE bandId = :bandId ORDER BY random() LIMIT :maxSize")
    List<DBSong> getSomeForBand(Long bandId, Integer maxSize);

    @Query("SELECT * FROM DBSong WHERE albumId = :albumId ORDER BY fullPath")
    List<DBSong> getAllForAlbum(Long albumId);

    @Query("SELECT * FROM DBSong ORDER BY random() LIMIT :batchSize")
    List<DBSong> getRandomBatch(int batchSize);

    @Query("SELECT DISTINCT year FROM DBSong WHERE year IS NOT NULL ORDER BY year")
    List<Integer> getYears();

    @Query("SELECT * FROM DBSong WHERE year >= :startYear AND year <= :endYear ORDER BY random() LIMIT :batchSize")
    List<DBSong> getRandomBatchForEra(int startYear, int endYear, int batchSize);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DBSong song);
}
