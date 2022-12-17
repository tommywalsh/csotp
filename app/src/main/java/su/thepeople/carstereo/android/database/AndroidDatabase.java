package su.thepeople.carstereo.android.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.util.stream.Stream;

import su.thepeople.carstereo.lib.backend.Backend;
import su.thepeople.carstereo.lib.data.NoLibraryException;

/**
 * Room-based interface to an SQL database holding information about on-disk music files.
 *
 * The Android system keeps its own database of all media files on the system, but that database will have lots of
 * inconsistencies unless you very meticulously maintain all of your media file tags. We use our own simplified
 * database to avoid those inconsistencies, and to cut out a lot of code complexity that would otherwise be required.
 */
@androidx.room.Database(entities = {DBBand.class, DBAlbum.class, DBSong.class}, version = 2)
public abstract class AndroidDatabase extends RoomDatabase {
    public abstract DBBandDAO bandDAO();
    public abstract DBAlbumDAO albumDAO();
    public abstract DBSongDAO songDAO();

    private static volatile AndroidDatabase instance = null;

    public synchronized void initializeIfNecessary(Backend backend, File[] mediaDirs) throws NoLibraryException {
        int bandCount = instance.bandDAO().getAll().size();
        if (bandCount == 0) {
            // Database has not been initialized yet!
            backend.scanCollection(() -> Stream.of(mediaDirs));
        }
        if (instance.bandDAO().getAll().size() == 0) {
            // Although we've already completed the scan, we still have no bands!
            throw new NoLibraryException();
        }
    }

    public static AndroidDatabase getDatabase(Context context) {
        // Use a singleton Database for the entire application.
        if (instance == null) {
            synchronized(AndroidDatabase.class) {
                if (instance == null) {

                    /*
                     * If we don't already have a database, then we need to scan the on-disk collection to build up
                     * the database from scratch.
                     *
                     * TODO: We should also be checking for on-disk updates, in case songs are added/deleted after we've
                     *  already created the database.
                     */
                    RoomDatabase.Callback callback = new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                        }
                    };

                    instance = Room.databaseBuilder(context.getApplicationContext(), AndroidDatabase.class, "dbotp")
                            .addCallback(callback)
                            .build();
                }
            }
        }
        return instance;
    }
}
