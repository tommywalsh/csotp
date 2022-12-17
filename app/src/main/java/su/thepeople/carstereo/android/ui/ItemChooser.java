package su.thepeople.carstereo.android.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import su.thepeople.carstereo.R;
import su.thepeople.carstereo.lib.data.Album;
import su.thepeople.carstereo.lib.data.Band;
import su.thepeople.carstereo.android.ui.SubActivityManager.ActivityIODefinition;

/**
 * This is an abstract superclass that defines most of the behavior of a scrollable item picker. Internal classes
 * specialize the behavior for different types.
 *
 * Speaking generally, the ItemChooser is sent a list of objects as input. These objects are shown in a list, and the
 * user may choose one. If they do, then the chosen item is send back as output.
 *
 * The main widget optionally offers coarse/fine scrolling using buttons. This is intended to be relatively easy to
 * use with occasional glances at the screen. Most other scrolling solutions require continued attention on the screen,
 * which is not what you want to have in a car stereo.
 *
 * Each concrete implementation only needs to do minor tweaking to adapt. There are tree implementation, one each for
 * bands, albums, and years.
 */
public abstract class ItemChooser <T extends Serializable> extends AppCompatActivity {

    // Each type of ItemChooser can implement these methods for its own data type.
    protected abstract String getDisplayString(T obj);
    protected String getCoarseAbbreviation(T obj) { return ""; }
    protected String getFineAbbreviation(T obj) { return ""; }

    public static class AlbumChooser extends ItemChooser<Album> {
        @Override protected String getDisplayString(Album a) {
            return a.getName();
        }
        @SuppressWarnings("rawtypes") public static ActivityIODefinition<ArrayList, Album> getIODefinition() {
            return ItemChooser.makeIODefinition(AlbumChooser.class, Album.class);
        }
    }

    public static class BandChooser extends ItemChooser<Band> {
        @Override protected String getDisplayString(Band b) {
            return b.getName();
        }
        @SuppressWarnings("rawtypes")
        public static ActivityIODefinition<ArrayList, Band> getIODefinition() {
            return ItemChooser.makeIODefinition(BandChooser.class, Band.class);
        }
        @Override public String getCoarseAbbreviation(Band b) { return Utils.leadingCharacters(b.getName(), 1); }
        @Override public String getFineAbbreviation(Band b) { return Utils.leadingCharacters(b.getName(), 2); }
    }

    public static class YearChooser extends ItemChooser<Integer> {
        @Override protected String getDisplayString(Integer year) {
            return Integer.toString(year);
        }
        @SuppressWarnings("rawtypes") public static ActivityIODefinition<ArrayList, Integer> getIODefinition() {
            return ItemChooser.makeIODefinition(YearChooser.class, Integer.class);
        }
    }


    /**
     * This is the list of items that is presented by the ItemChooser.
     */
    private List<T> items;

    /**
     * Each ItemChooser screen uses a "recycler view" in order to efficiently handle large lists.
     * Rather than allocate a UI widget for every item in the list, the Recycler view uses a
     * relatively small set of widgets, which are dynamically re-associated with different objects
     * as the list is scrolled through.
     *
     * This requires the use of a couple of custom helper classes, below.
     */
    private RecyclerView listView;

    /**
     * Each slot in the scrollable "recycler" list contains a view that needs to be customized to
     * our needs by providing an implementation of the "ViewHolder" class.
     *
     * In our case, each view is simply a single button. So, our implementation only needs to hold
     * one button. The button's properties are modified whenever this ViewHolder is bound to a new
     * item.
     */
    private class ButtonViewHolder extends RecyclerView.ViewHolder {
        protected final Button button;

        ButtonViewHolder(@NonNull Button button) {
            super(button);
            this.button = button;
        }

        protected void associateWithItem(int itemPosition) {
            // The button's "tag" property is used to store an index into our 'items' list.
            button.setTag(itemPosition);
            button.setText(getDisplayString(items.get(itemPosition)));
        }

        protected int getItemIndex() {
            return (Integer) button.getTag();
        }
    }

    /**
     * The recyclerview needs an adapter to bind to our particular view holder type and data collection.
     */
    private class Adapter extends RecyclerView.Adapter<ButtonViewHolder> {

        @Override
        public @NonNull ButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // This method must create a brand new view holder, which will be bound to a particular item later.
            Button button = (Button) LayoutInflater.from(parent.getContext()).inflate(R.layout.chooser_item, parent, false);
            ButtonViewHolder holder = new ButtonViewHolder(button);
            button.setOnClickListener(b -> handleUserSelection(holder.getItemIndex()));
            return holder;
        }

        @Override
        public void onBindViewHolder(ButtonViewHolder holder, int position) {
            // This is called to bind (or re-bind) a list slot to a particular item in the list.
            holder.associateWithItem(position);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private void handleUserSelection(int itemIndex) {
        // When an item is selected, we want to close this screen and return the item.
        T item = items.get(itemIndex);
        Intent intent = new Intent();
        intent.putExtra(OUTPUT_KEY, item);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }


    private static final String LOG_ID = "Item Chooser";

    private static final String INPUT_KEY = "INPUT_LIST";
    private static final String OUTPUT_KEY = "OUTPUT_ITEM";

    private static final int COARSE_SCROLL_MINIMUM = 10;
    private static final int FINE_SCROLL_MINIMUM = 60;

    @SuppressWarnings("rawtypes")
    private static <S extends Serializable> ActivityIODefinition<ArrayList, S> makeIODefinition(Class<? extends ItemChooser<S>> chooserClass, Class<S> baseClass) {
        return new ActivityIODefinition<>(chooserClass, INPUT_KEY, ArrayList.class, OUTPUT_KEY, baseClass);
    }


    /*
     * We divide the scroll region into 25 roughly-equal parts, which can be scrolled to using a set
     * of five "coarse scroll" buttons, and another set of five "fine scroll" buttons.
     *
     * The coarse-scroll button will control which fifth of the entire region we are in. That
     * sub-region, in turn, is broken into five parts, and the fine-scroll button controls which of
     * these parts we are in.
     */
    private List<Button> coarseButtons;
    private List<Button> fineButtons;

    /**
     * Helper class to keep track of which coarse/fine scroller position we're at.
     */
    private static class ScrollPosition {
        protected final int coarse;
        protected final int fine;
        protected ScrollPosition(int coarse, int fine) {
            this.coarse = coarse;
            this.fine = fine;
        }
    }

    /**
     * Helper function to determine where we are in the list, according to the coarse and fine
     * scroll buttons.
     */
    private ScrollPosition getScrollPosition() {
        int offset = listView.computeVerticalScrollOffset();
        int range = listView.computeVerticalScrollRange();
        int extent = listView.computeVerticalScrollExtent();
        if (range == 0.0 || extent == 0.0) {
            return new ScrollPosition(0, 0);
        }
        Log.d(LOG_ID, String.format("After scroll, offset is %s, extent is %s, and range is %s", offset, extent, range));
        int screenCenterPos = offset + (extent / 2);
        double pct = screenCenterPos / (double)range;

        int coarseIndex = Math.min((int)(pct * 5.0), 4);

        double coarseStartPct = coarseIndex * 0.2;
        double finePct = (pct - coarseStartPct) / 0.2;
        int fineIndex = Math.min((int)(finePct * 5.0), 4);

        return new ScrollPosition(coarseIndex, fineIndex);
    }

    /**
     * After a scroll event (either a button push, or a manual scroll), we need to make sure that
     * the scroll buttons on-screen really reflect the current displayed position.
     */
    private void updateScrollers() {
        ScrollPosition currentPosition = getScrollPosition();
        updateFineButtons(currentPosition);

        coarseButtons.forEach(b -> b.setBackgroundResource(android.R.drawable.btn_default));
        fineButtons.forEach(b -> b.setBackgroundResource(android.R.drawable.btn_default));

        coarseButtons.get(currentPosition.coarse).setBackgroundResource(R.color.colorAccent);
        fineButtons.get(currentPosition.fine).setBackgroundResource(R.color.colorAccent);
    }

    /**
     * Each scroller button has a target item that it should scroll to. This method produces a
     * lambda which knows how to execute that scroll movement.
     */
    private static View.OnClickListener scrollerCallback(int position, RecyclerView view) {
        return v -> {
            Log.d(LOG_ID, String.format("Scrolling to %s", position));
            view.scrollToPosition(position);
        };
    }

    private void updateButton(Button button, int position, Function<T, String> labelGetter) {
        button.setOnClickListener(scrollerCallback(position, listView));
        button.setText(labelGetter.apply(items.get(position)));
    }

    /**
     * This method sets up the coarse-scroll buttons. This method is called once, at setup time.
     *
     * When you ask the Android scroll widget to move to a particular item, it works like this:
     * - If the item is already on-screen, nothing happens
     * - If the item is "off the top" of the screen, the scroll puts the item in the first position.
     * - If the item is "off the bottom" of the screen, the item will scroll to the last position.
     *
     * This implies that the same scroll button can place you at slightly different places in the
     * list, depending on the situation. This can be slightly unexpected, especially near the
     * beginning and end of the list.
     *
     * So, our scroll button strategy is:
     * - The first button always takes you to the very top of the list
     * - The last button always takes you to the very end of the list
     * - The middle three buttons take you to/near the 30%, 50% and 70% spots in the list.
     */
    private void initializeCoarseButtons(List<T> items) {
        int numItems = items.size();
        updateButton(coarseButtons.get(0),0, this::getCoarseAbbreviation);
        updateButton(coarseButtons.get(1),(3*numItems)/10, this::getCoarseAbbreviation);
        updateButton(coarseButtons.get(2),(5*numItems)/10, this::getCoarseAbbreviation);
        updateButton(coarseButtons.get(3),(7*numItems)/10, this::getCoarseAbbreviation);
        updateButton(coarseButtons.get(4),numItems - 1, this::getCoarseAbbreviation);
    }

    /**
     * This method sets up the fine-scroll buttons. This method may be called many times, as the
     * list is scrolled.
     */
    private void updateFineButtons(ScrollPosition position) {
        int size = items.size();
        int topOfCoarseSpan = (int)(position.coarse * size / 5.0);
        int spanLength = (int)(items.size() * 0.2);
        int lastIndex = Math.min(topOfCoarseSpan + spanLength, size - 1);
        Log.d(LOG_ID, String.format("Resetting fine scroll buttons to cover %s items, starting at %s", spanLength, topOfCoarseSpan));
        updateButton(fineButtons.get(0), topOfCoarseSpan, this::getFineAbbreviation);
        updateButton(fineButtons.get(1), topOfCoarseSpan + (3*spanLength)/10, this::getFineAbbreviation);
        updateButton(fineButtons.get(2), topOfCoarseSpan + (5*spanLength)/10, this::getFineAbbreviation);
        updateButton(fineButtons.get(3), topOfCoarseSpan + (7*spanLength)/10, this::getFineAbbreviation);
        updateButton(fineButtons.get(4), lastIndex, this::getFineAbbreviation);
    }

    /**
     * It's cumbersome to unpack inputs to Android activities. This helper function handles this work.
     */
    private ArrayList<T> unpackageInput() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        assert bundle != null;
        Serializable untypedInput = bundle.getSerializable(INPUT_KEY);
        assert untypedInput != null;
        @SuppressWarnings("unchecked") ArrayList<T> typedInput = (ArrayList<T>)untypedInput;
        return typedInput;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        items = unpackageInput();


        setContentView(R.layout.activity_item_chooser);

        listView = findViewById(R.id.itemRecycler);
        listView.setHasFixedSize(true);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(new Adapter());

        findViewById(R.id.coarseScroller).setVisibility(items.size() > COARSE_SCROLL_MINIMUM ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.fineScroller).setVisibility(items.size() > FINE_SCROLL_MINIMUM ? View.VISIBLE : View.INVISIBLE);

        if (items.size() > COARSE_SCROLL_MINIMUM) {

            listView.setOnScrollChangeListener((v, x, y, x1, y1) -> updateScrollers());

            coarseButtons =
                    Stream.of(R.id.coarse10, R.id.coarse30, R.id.coarse50, R.id.coarse70, R.id.coarse90)
                            .map(id -> (Button) findViewById(id))
                            .collect(Collectors.toList());

            fineButtons =
                    Stream.of(R.id.fine10, R.id.fine30, R.id.fine50, R.id.fine70, R.id.fine90)
                            .map(id -> (Button) findViewById(id))
                            .collect(Collectors.toList());

            initializeCoarseButtons(items);
            updateScrollers();
        }
    }

    /**
     * Here we override the standard system "resume" so that we can try to hide any system UI
     * that would otherwise obscure our UI.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Utils.hideSystemUI(this, R.id.itemPickerLayout);
    }

}
