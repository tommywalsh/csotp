package su.thepeople.carstereo.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * CRUD methods for songs in the database.
 */
@Dao
public interface SongDAO {

    @Query("SELECT * FROM Song WHERE bandId = :bandId ORDER BY random()")
    List<Song> getAllForBandShuffled(Long bandId);

    @Query("SELECT * FROM Song WHERE bandId = :bandId ORDER BY year, fullPath")
    List<Song> getAllForBandOrdered(Long bandId);

    @Query("SELECT * FROM Song WHERE bandId = :bandId ORDER BY random() LIMIT :maxSize")
    List<Song> getSomeForBand(Long bandId, Integer maxSize);

    @Query("SELECT * FROM Song WHERE albumId = :albumId ORDER BY fullPath")
    List<Song> getAllForAlbum(Long albumId);

    @Query("SELECT * FROM Song ORDER BY random() LIMIT :batchSize")
    List<Song> getRandomBatch(int batchSize);

    @Query("SELECT DISTINCT year FROM Song WHERE year IS NOT NULL ORDER BY year")
    List<Integer> getYears();

    @Query("SELECT * FROM Song WHERE year >= :startYear AND year <= :endYear ORDER BY random() LIMIT :batchSize")
    List<Song> getRandomBatchForEra(int startYear, int endYear, int batchSize);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Song song);
}
