package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
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
    List<Song> getAllForBand(@NonNull Long bandId);

    @Query("SELECT * FROM Song WHERE bandId = :bandId ORDER BY random() LIMIT :maxSize")
    List<Song> getSomeForBand(@NonNull Long bandId, @NonNull Integer maxSize);

    @Query("SELECT * FROM Song WHERE albumId = :albumId ORDER BY fullPath")
    List<Song> getAllForAlbum(@NonNull Long albumId);

    @Query("SELECT * FROM Song WHERE albumId = :albumId ORDER BY random()")
    List<Song> getRandomFromAlbum(@NonNull Long albumId);

    @Query("SELECT * FROM Song ORDER BY random() LIMIT :batchSize")
    List<Song> getRandomBatch(int batchSize);

    @Query("SELECT * FROM Song WHERE albumId IN (SELECT uid FROM ALBUM WHERE year >= :startYear AND year <= :endYear) ORDER BY random() LIMIT :batchSize")
    List<Song> getRandomBatchForEra(int startYear, int endYear, int batchSize);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Song song);
}
