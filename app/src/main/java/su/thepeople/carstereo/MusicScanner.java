package su.thepeople.carstereo;

import android.content.Context;
import android.util.Log;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final String LOG_TAG = "Music Scanner";
    private static final Pattern ALBUM_DIR_REGEX = Pattern.compile("^(\\d\\d\\d\\d) - (.*)$");

    private final Database database;
    private final Context context;

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
                .filter(x -> x != null)
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
                .forEach(f -> {
                    if (f.isDirectory()) {
                        Long albumID = null;  // Using null for dirs that are not real albums
                        String dirName = f.getName();
                        Matcher dirMatcher = ALBUM_DIR_REGEX.matcher(dirName);
                        String albumName = dirMatcher.matches() ? dirMatcher.group(2) : dirName;
                        Integer albumYear = dirMatcher.matches() ? Integer.parseInt(dirMatcher.group(1)) : null;
                        if (!albumName.startsWith("[")) {
                            Log.d(LOG_TAG, String.format("Found album %s", albumName));
                            Album newAlbum = new Album(albumName, bandID, albumYear);
                            albumID = database.albumDAO().insert(newAlbum);
                        }
                        scanAlbumDir(bandID, albumID, f);
                    } else if (f.isFile()) {
                        Log.d(LOG_TAG, String.format("Found loose song %s", f.getName()));
                        Song newSong = new Song(f.getName(), f.getAbsolutePath(), bandID, null);
                        database.songDAO().insert(newSong);
                    }
                });
    }

    private void scanAlbumDir(long bandID, Long albumID, File albumDir) {
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
