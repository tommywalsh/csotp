package su.thepeople.carstereo.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/*
 * Our main UI has a number of sub-activities that it kicks off. Each of them has several things in common with the
 * others. This manager class exists so that the main process can interact with all of them in a uniform way, without
 * needing to have a lot of duplicated code.
 *
 * The idea is that this class manages the interactions between these three concerns:
 *   - Description of the input expected by the subactivity, and the output provided by it.
 *   - Keeping track of which subactivity has returned which data.
 *   - Launching the subactivity, and processing any result.
 */
public class SubActivityManager {

    /**
     * This class is used to describe the IO characteristics of a subactivity. Each subactivity should supply this
     * data for itself.
     */
    public static class ActivityIODefinition <I extends Serializable, O extends Serializable> {

        public ActivityIODefinition(Class<? extends Activity> activityClass, String inputTag, Class<I> inputClass, String outputTag, Class<O> outputClass) {
            this.activityClass = activityClass;
            this.inputTag = inputTag;
            this.inputClass = inputClass;
            this.outputTag = outputTag;
            this.outputClass = outputClass;
        }

        protected final String inputTag;
        protected final Class<? extends Serializable> inputClass;
        protected final String outputTag;
        protected final Class<? extends Serializable> outputClass;
        protected final Class<? extends Activity> activityClass;
    }

    private final List<SubActivityDefinition<? extends Serializable, ? extends Serializable>> subActivities = new ArrayList<>();

    protected static class SubActivityDefinition<I extends Serializable, O extends Serializable> {
        protected final ActivityIODefinition<I, O> ioDef;
        protected final Consumer<O> outputProcessor;

        protected SubActivityDefinition(ActivityIODefinition<I,O> ioDef, Consumer<O> outputProcessor) {
            this.ioDef = ioDef;
            this.outputProcessor = outputProcessor;
        }

    }

    public SubActivityManager(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    private synchronized <O extends Serializable> int addSubActivityDefinition(SubActivityDefinition<? extends Serializable, O> subActivity) {
        int activityKey = subActivities.size();
        subActivities.add(subActivity);
        return activityKey;
    }

    public <I extends Serializable, O extends Serializable> int addSubActivityDefinition(ActivityIODefinition<I,O> ioDef, Consumer<O> outputProcessor) {
        return addSubActivityDefinition(new SubActivityDefinition<>(ioDef, outputProcessor));
    }

    public <I extends Serializable> void runSubActivity(int activityKey, I input) {
        SubActivityDefinition<? extends Serializable, ? extends Serializable> subActivity = subActivities.get(activityKey);
        assert subActivity.ioDef.inputClass.isAssignableFrom(input.getClass());

        Intent intent = new Intent(parentActivity, subActivity.ioDef.activityClass);
        Bundle bundle = new Bundle();
        bundle.putSerializable(subActivity.ioDef.inputTag, input);
        intent.putExtras(bundle);
        parentActivity.startActivityForResult(intent, activityKey);
    }

    private <T extends Serializable> void runUntypedResultProcessor(Consumer<? extends Serializable> processor, Serializable output, Class<T> outputType) {
        assert outputType.isAssignableFrom(output.getClass());
        @SuppressWarnings("unchecked") Consumer<T> typedProcessor = (Consumer<T>) processor;
        @SuppressWarnings("unchecked") T typedOutput = (T) output;
        typedProcessor.accept(typedOutput);
    }

    public void processSubActivityResult(int activityKey, Intent result) {
        SubActivityDefinition<? extends Serializable, ? extends Serializable> subActivity = subActivities.get(activityKey);
        Class<? extends Serializable> outputClass = subActivity.ioDef.outputClass;
        Serializable untypedOutput = result.getSerializableExtra(subActivity.ioDef.outputTag);
        assert untypedOutput != null;
        Consumer<? extends Serializable> untypedProcessor = subActivity.outputProcessor;
        runUntypedResultProcessor(untypedProcessor, untypedOutput, outputClass);
    }
    private final Activity parentActivity;
}
