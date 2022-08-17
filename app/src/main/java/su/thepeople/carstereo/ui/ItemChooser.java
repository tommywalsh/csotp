package su.thepeople.carstereo.ui;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import su.thepeople.carstereo.R;
import su.thepeople.carstereo.ui.SubActivityManager.ActivityIODefinition;
import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

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
 * Each concrete implementation only needs to do minor tweaking to adapt.
 */
public abstract class ItemChooser <T extends Serializable> extends AppCompatActivity {

    private static final String LOG_ID = "Item Chooser";

    protected abstract String getDisplayString(T obj);
    protected abstract String getCoarseAbbreviation(T obj);
    protected abstract String getFineAbbreviation(T obj);

    private static final String INPUT_KEY = "INPUT_LIST";
    private static final String OUTPUT_KEY = "OUTPUT_ITEM";

    private static final int COURSE_SCROLL_MINIMUM = 10;
    private static final int FINE_SCROLL_MINIMUM = 60;

    @SuppressWarnings("rawtypes")
    private static <S extends Serializable> ActivityIODefinition<ArrayList, S> makeIODefinition(Class<? extends ItemChooser<S>> chooserClass, Class<S> baseClass) {
        return new ActivityIODefinition<>(chooserClass, INPUT_KEY, ArrayList.class, OUTPUT_KEY, baseClass);
    }

    /**
     * A scrollable album picker
     */
    public static class AlbumChooser extends ItemChooser<Album> {
        @Override protected String getDisplayString(Album a) {
            return a.name;
        }

        @SuppressWarnings("rawtypes")
        public static ActivityIODefinition<ArrayList, Album> getIODefinition() {
            return ItemChooser.makeIODefinition(AlbumChooser.class, Album.class);
        }
        public String getCoarseAbbreviation(Album b) { return ""; }
        public String getFineAbbreviation(Album b) { return ""; }
    }

    /**
     * A scrollable band picker
     */
    public static class BandChooser extends ItemChooser<Band> {
        @Override protected String getDisplayString(Band b) {
            return b.name;
        }
        @SuppressWarnings("rawtypes")
        public static ActivityIODefinition<ArrayList, Band> getIODefinition() {
            return ItemChooser.makeIODefinition(BandChooser.class, Band.class);
        }

        public String getCoarseAbbreviation(Band b) {
            return b.name.substring(0,1);
        }
        public String getFineAbbreviation(Band b) {
            return b.name.substring(0, 2);
        }

    }

    /**
     * A scrollable year picker
     */
    public static class YearChooser extends ItemChooser<Integer> {
        @Override
        protected String getDisplayString(Integer year) {
            return Integer.toString(year);
        }

        @SuppressWarnings("rawtypes")
        public static ActivityIODefinition<ArrayList, Integer> getIODefinition() {
            return ItemChooser.makeIODefinition(YearChooser.class, Integer.class);
        }

        public String getCoarseAbbreviation(Integer b) {
            return "";
        }
        public String getFineAbbreviation(Integer b) { return ""; }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        protected final Button view;

        ViewHolder(Button view) {
            super(view);
            this.view = view;
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Button view = (Button) LayoutInflater.from(parent.getContext()).inflate(R.layout.chooser_item, parent, false);
            view.setOnClickListener(this::onItemSelect);
            return new ViewHolder(view);
        }

        private void onItemSelect(View view) {
            Integer listPosition = (Integer) view.getTag();
            T item = items.get(listPosition);
            Intent intent = new Intent();
            intent.putExtra(OUTPUT_KEY, item);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.view.setTag(position);
            holder.view.setText(getDisplayString(items.get(position)));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.hideSystemUI(this, R.id.itemPickerLayout);
    }

    private RecyclerView listView;
    private List<Button> courseButtons;
    private List<Button> fineButtons;

    /*
     * We divide the scroll region into 25 roughly-equal parts.
     *
     * The course position will tell which fifth of the entire region we are in. Values are 0.0, 0.2, 0.4, 0.6 and 0.8.
     * The fine position will give the position within that fifth. Again values are 0.0, 0.2, 0.4, 0.6, and 0.8.
     *
     * For example, if we are two-thirds through the entire list (67%), the course position would be 0.6 (because we are
     * between 60% and 80%), and the fine position would be 0.2 (because we are more than 20%, but less than 40%,
     * through the course interval.
     *
     * In other words, in the above example, we are somewhere between 64% and 68% of the entire list.
     */
    private static class ScrollPosition {
        double coarse;
        double fine;
        public ScrollPosition(double coarse, double fine) {
            this.coarse = coarse;
            this.fine = fine;
        }
    }

    private ScrollPosition getScrollPosition() {
        int offset = listView.computeVerticalScrollOffset();
        int range = listView.computeVerticalScrollRange();
        int extent = listView.computeVerticalScrollExtent();
        if (range == 0.0 || extent == 0.0) {
            return new ScrollPosition(0.0, 0.0);
        }
        Log.d(LOG_ID, String.format("After scroll, offset is %s, extent is %s, and range is %s", offset, extent, range));
        int screenCenterPos = offset + (extent / 2);
        double pct = screenCenterPos / (double)range;

        double coursePosition = Math.floor(pct * 5.0) / 5.0;
        double remainder = pct - coursePosition;
        double finePosition = Math.floor(remainder * 25.0) / 5.0;

        return new ScrollPosition(coursePosition, finePosition);
    }

    private void updateScrollers() {
        ScrollPosition currentPosition = getScrollPosition();
        updateFineButtons();

        courseButtons.forEach(b -> b.setBackgroundResource(android.R.drawable.btn_default));
        fineButtons.forEach(b -> b.setBackgroundResource(android.R.drawable.btn_default));

        int courseIndex = (int) (currentPosition.coarse * 5.0);
        int fineIndex = (int) (currentPosition.fine * 5.0);

        courseButtons.get(courseIndex).setBackgroundResource(R.color.colorAccent);
        fineButtons.get(fineIndex).setBackgroundResource(R.color.colorAccent);

    }

    /*
     * Scroll behavior:
     *   - First scroll position is always first item, last is always last item
     *   - Other positions are in the middle of their ranges.
     *
     * So, for a list of 100 items, the scroll-to positions for the course buttons will be
     * First button for range 0-19, scrolls to 0
     * Second button for range 20-39, scrolls to 30
     * Third button for range 40-59, scrolls to 50
     * Fourth button for range 60-79, scrolls to 70
     * Fifth button for range 80-99, scrolls to 99
     */
    private static View.OnClickListener scrollerCallback(int position, RecyclerView view) {
        return v -> {
            Log.d(LOG_ID, String.format("Scrolling to %s", position));
            view.scrollToPosition(position);
        };
    }

    private void updateSingleCoarseButton(Button button, List<T> items, int topPosition, int scrollToPosition) {
        button.setOnClickListener(scrollerCallback(scrollToPosition, listView));
        button.setText(getCoarseAbbreviation(items.get(topPosition)));
    }

    private void updateSingleFineButton(Button button, List<T> items, int topPosition, int scrollToPosition) {
        button.setOnClickListener(scrollerCallback(scrollToPosition, listView));
        button.setText(getFineAbbreviation(items.get(topPosition)));
    }


    private void initializeCoarseButtons(List<T> items) {
        int numItems = items.size();
        updateSingleCoarseButton(courseButtons.get(0), items, 0, 0);
        updateSingleCoarseButton(courseButtons.get(1), items, (int)(numItems * 0.3), (int)(numItems * 0.3));
        updateSingleCoarseButton(courseButtons.get(2), items, (int)(numItems * 0.5), (int)(numItems * 0.5));
        updateSingleCoarseButton(courseButtons.get(3), items, (int)(numItems * 0.7), (int)(numItems * 0.7));
        updateSingleCoarseButton(courseButtons.get(4), items, numItems - 1, numItems - 1);
    }

    private void updateFineButtons() {
        ScrollPosition position = getScrollPosition();
        int size = items.size();
        int topOfCoarseSpan = (int)(position.coarse * size);
        int spanLength = (int)(items.size() * 0.2);
        int lastIndex = Math.min(topOfCoarseSpan + spanLength, size - 1);
        Log.d(LOG_ID, String.format("%s of %s means span of %s starts at %s", position.coarse, size, spanLength, topOfCoarseSpan));
        updateSingleFineButton(fineButtons.get(0), items, topOfCoarseSpan, topOfCoarseSpan);
        updateSingleFineButton(fineButtons.get(1), items, topOfCoarseSpan + (3*spanLength)/10, topOfCoarseSpan + (3*spanLength)/10);
        updateSingleFineButton(fineButtons.get(2), items, topOfCoarseSpan + (5*spanLength)/10, topOfCoarseSpan + (5*spanLength)/10);
        updateSingleFineButton(fineButtons.get(3), items, topOfCoarseSpan + (7*spanLength)/10, topOfCoarseSpan + (7*spanLength)/10);
        updateSingleFineButton(fineButtons.get(4), items, lastIndex, lastIndex);
    }

    private List<T> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_chooser);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        assert bundle != null;
        Serializable untypedInput = bundle.getSerializable(INPUT_KEY);
        assert untypedInput != null;
        @SuppressWarnings("unchecked") ArrayList<T> typedInput = (ArrayList<T>)untypedInput;
        items = typedInput;

        listView = findViewById(R.id.itemRecycler);
        listView.setHasFixedSize(true);

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(new Adapter());

        findViewById(R.id.courseScroller).setVisibility(items.size() > COURSE_SCROLL_MINIMUM ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.fineScroller).setVisibility(items.size() > FINE_SCROLL_MINIMUM ? View.VISIBLE : View.INVISIBLE);

        if (items.size() > COURSE_SCROLL_MINIMUM) {

            listView.setOnScrollChangeListener((v, x, y, x1, y1) -> updateScrollers());

            courseButtons =
                    Stream.of(R.id.course10, R.id.course30, R.id.course50, R.id.course70, R.id.course90)
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
}
