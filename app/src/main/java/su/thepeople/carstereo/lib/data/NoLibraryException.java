package su.thepeople.carstereo.lib.data;

import su.thepeople.carstereo.R;
import su.thepeople.carstereo.lib.interthread.BackendException;

public class NoLibraryException extends BackendException {
    @Override
    public int getDescriptionStringID() {
        return R.string.no_library;
    }
}
