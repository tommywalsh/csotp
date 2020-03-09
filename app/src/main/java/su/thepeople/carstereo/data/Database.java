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

    public static Database getDatabase(Context context) {
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

                            workerThread = new Thread(() -> {
                                MusicScanner scanner = new MusicScanner(instance);
                                scanner.scan();
                            });
                            workerThread.start();
                        }
                    };

                    instance = Room.databaseBuilder(context.getApplicationContext(), Database.class, "dbotp")
                            .addCallback(callback)
                            .build();
                }
            }
        }
        return instance;
    }
}
