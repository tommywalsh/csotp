package su.thepeople.carstereo.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import su.thepeople.carstereo.MusicScanner;

/**
 * Room-based interface to an SQL database holding information about on-disk music files.
 *
 * The Android system keeps its own database of all media files on the system, but that database will have lots of
 * inconsistencies unless you very meticulously maintain all of your media file tags. We use our own simplified
 * database to avoid those inconsistencies, and to cut out a lot of code complexity that would otherwise be required.
 */
@androidx.room.Database(entities = {Band.class, Album.class, Song.class}, version = 1)
public abstract class Database extends RoomDatabase {
    public abstract BandDAO bandDAO();
    public abstract AlbumDAO albumDAO();
    public abstract SongDAO songDAO();

    private static volatile Database instance = null;
    private static Thread workerThread = null;

    public static Database getDatabase(Context context) throws NoLibraryException {
        // Use a singleton Database for the entire application.
        if (instance == null) {
            synchronized(Database.class) {
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

                    instance = Room.databaseBuilder(context.getApplicationContext(), Database.class, "dbotp")
                            .addCallback(callback)
                            .build();

                    int bandCount = instance.bandDAO().getAll().size();
                    if (bandCount == 0) {
                        // Database has not been initialized yet!
                        MusicScanner scanner = new MusicScanner(context, instance);
                        scanner.scan();
                    }
                }
            }
        }

        if (instance.bandDAO().getAll().size() == 0) {
            // Although we've already completed the scan, we still have no bands!
            throw new NoLibraryException();
        }

        return instance;
    }
}
