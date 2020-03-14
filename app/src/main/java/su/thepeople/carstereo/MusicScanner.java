package su.thepeople.carstereo;

import android.content.Context;
import android.util.Log;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Class to scan for on-disk music.
 *
 * This class assumes that there will be a directory called "mcotp", in which the entire music library is stored. This
 * mcotp directory should be in the root level of an SD card. Failing that, it might work to put the directory as a
 * sibling to whereever Android will put this application's media directories, or as a sibling to one of the media
 * directories' parents.
 */
public class MusicScanner {

    private static String LOG_TAG = "Music Scanner";

    private Database database;
    private Context context;

    public MusicScanner(Context context, Database database) {
        this.database = database;
        this.context = context;
    }

   private Optional<File> getMcotpSubdir(File maybeDir) {
        Log.d(LOG_TAG, String.format("Looking for collection in %s", maybeDir.toString()));
        Optional<File> maybeMcotp = Utils.dirContentsStream(maybeDir)
                .filter(f -> f.isDirectory() && f.getName().equals("mcotp")).findFirst();
        maybeMcotp.ifPresent(mcotp ->
                Log.d(LOG_TAG, String.format("Found music collection at %s", mcotp.getAbsolutePath())));
        return maybeMcotp;
    }

    private Optional<File> findMcotpRoot() {
        return Stream.of(context.getExternalMediaDirs())
                .flatMap(Utils::dirParentStream)
                .map(this::getMcotpSubdir)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private void scanCollection(File mcotpRoot) {
        Log.d(LOG_TAG, String.format("Scanning collection at %s", mcotpRoot.getAbsolutePath()));
        Utils.dirContentsStream(mcotpRoot)
                .filter(File::isDirectory)
                .filter(d -> !d.getName().startsWith("["))
                .forEach(bandDir -> {
                    Log.d(LOG_TAG, String.format("Found band directory %s", bandDir.getName()));
                    Band newBand = new Band(bandDir.getName());
                    long bandID = database.bandDAO().insert(newBand);
                    scanBandDir(bandID, bandDir);
                });
    }

    private void scanBandDir(long bandID, File bandDir) {
        Log.d(LOG_TAG, String.format("Scanning band directory %s", bandDir.getName()));
        Utils.dirContentsStream(bandDir)
                .filter(f -> !f.getName().startsWith("["))
                .forEach(f -> {
                    if (f.isDirectory()) {
                        Log.d(LOG_TAG, String.format("Found album %s", f.getName()));
                        Album newAlbum = new Album(f.getName(), bandID);
                        long albumID = database.albumDAO().insert(newAlbum);
                        scanAlbumDir(bandID, albumID, f);
                    } else if (f.isFile()) {
                        Log.d(LOG_TAG, String.format("Found loose song %s", f.getName()));
                        Song newSong = new Song(f.getName(), f.getAbsolutePath(), bandID, null);
                        database.songDAO().insert(newSong);
                    }
                });
    }

    private void scanAlbumDir(long bandID, long albumID, File albumDir) {
        Utils.dirContentsStream(albumDir)
                .filter(File::isFile)
                .filter(f -> !f.getName().startsWith("["))
                .forEach(songFile -> {
                    Log.d(LOG_TAG, String.format("Found album song %s", songFile.getName()));
                    Song newSong = new Song(songFile.getName(), songFile.getAbsolutePath(), bandID, albumID);
                    database.songDAO().insert(newSong);
                });
    }

    public void scan() {
        Optional<File> maybeRoot = findMcotpRoot();
        maybeRoot.ifPresent(this::scanCollection);
    }
}
