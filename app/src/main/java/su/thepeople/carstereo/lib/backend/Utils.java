package su.thepeople.carstereo.lib.backend;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    /**
     * A stream of all items in the given directory. Stream will be empty if given file is not really a directory.
     */
    public static Stream<File> dirContentsStream(File maybeDir) {
        if (maybeDir != null && maybeDir.isDirectory()) {
            File[] dirContents = maybeDir.listFiles();
            if (dirContents != null) {
                return Stream.of(dirContents);
            }
        }
        return Stream.of();
    }

    private static class FileAncestorIterator implements Iterator<File> {
        File currentItem;

        protected FileAncestorIterator(File child) {
            currentItem = child;
        }

        @Override
        public boolean hasNext() {
            File parent = currentItem.getParentFile();
            return parent != null && parent.canRead();
        }

        @Override
        public File next() {
            File parent = currentItem.getParentFile();
            currentItem = parent;
            return parent;
        }
    }

    private static <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * A stream that traverses each of the directory ancestors of the given file. Basically, this just keeps looking
     * at the next ".." parent.
     */
    public static Stream<File> dirParentStream(File child) {
        return iteratorToStream(new FileAncestorIterator(child));
    }


    /*
       Finds the given item ID in the list, and produces a new list. The new list will begin with
       the item AFTER the one with the matching ID, and will "wrap around" to the beginning, and
       end with the item BEFORE the one with the matching ID. (Thus the matching item is not in the
       returned list)
     */
    public static <T> List<T> splitList(List<T> listToSplit, long itemId, boolean keepItem, Function<T, Long> idGetter) {
        List<T> finalList = new ArrayList<>();
        List<T> tail = new ArrayList<>();
        boolean foundItem = false;
        for (T item : listToSplit) {
            if (foundItem) {
                finalList.add(item);
            } else {
                long thisId = idGetter.apply(item);
                if (thisId == itemId) {
                    foundItem = true;
                    if (keepItem) {
                        finalList.add(item);
                    }
                } else {
                    tail.add(item);
                }
            }
        }
        finalList.addAll(tail);
        return finalList;
    }
}
