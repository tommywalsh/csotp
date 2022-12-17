package su.thepeople.carstereo.android.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DBAlbumDAO {

    // TODO: This query not guaranteed to correctly sort multiple albums from the same year
    @Query("SELECT * FROM DBAlbum WHERE bandId = :bandId ORDER BY year, name")
    List<DBAlbum> getAllForBand(long bandId);

    @Query("SELECT * FROM DBAlbum WHERE uid = :albumId")
    DBAlbum lookup(long albumId);

    @Query("SELECT * FROM DBAlbum ORDER BY random() LIMIT 1")
    DBAlbum getRandom();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DBAlbum album);
}
