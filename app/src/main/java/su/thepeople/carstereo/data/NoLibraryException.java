package su.thepeople.carstereo.data;

import su.thepeople.carstereo.R;
import su.thepeople.carstereo.interthread.BackendException;

public class NoLibraryException extends BackendException {
    @Override
    public int getDescriptionStringID() {
        return R.string.no_library;
    }
}
