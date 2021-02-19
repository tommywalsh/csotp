package su.thepeople.carstereo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import su.thepeople.carstereo.SubActivityManager.ActivityIODefinition;
import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

/**
 * This is an abstract superclass that defines most of the behavior of a scrollable item picker. Internal classes
 * specialize the behavior for different types.
 *
 * Speaking generally, the ItemChooser is sent a list of objects as input. These objects are shown in a list, and the
 * user may choose one. If they do, then the chosen item is send back as output.
 *
 * Each concrete implementation only needs to to minor tweaking
 */
public abstract class ItemChooser <T extends Serializable> extends AppCompatActivity {

    protected abstract String getDisplayString(T obj);

    private static final String INPUT_KEY = "INPUT_LIST";
    private static final String OUTPUT_KEY = "OUTPUT_ITEM";

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
    }

    /**
     * A scrollable year picker
     */
    public static class YearChooser extends ItemChooser<MainActivity.Era> {
        @Override protected String getDisplayString(MainActivity.Era era) { return era.label; }
        @SuppressWarnings("rawtypes")
        public static ActivityIODefinition<ArrayList, MainActivity.Era> getIODefinition() {
            return ItemChooser.makeIODefinition(YearChooser.class, MainActivity.Era.class);
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        protected final Button view;

        ViewHolder(Button view) {
            super(view);
            this.view = view;
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<T> items;

        Adapter(List<T> items) {
            this.items = items;
        }

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
    private int listLength;

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
        double course;
        double fine;
    }

    private void courseScroll(double pct) {
        int itemIndex = (int) (pct * listLength);
        listView.scrollToPosition(itemIndex);
    }

    private ScrollPosition getScrollPosition() {
        int offset = listView.computeVerticalScrollOffset();
        int range = listView.computeVerticalScrollRange();
        double pct = range == 0 ? 0.0 : offset / (double) range;

        double coursePosition = Math.floor(pct * 5.0) / 5.0;
        double remainder = pct - coursePosition;
        double finePosition = Math.floor(remainder * 25.0) / 5.0;

        ScrollPosition position = new ScrollPosition();
        position.course = coursePosition;
        position.fine = finePosition;
        return position;
    }

    private void fineScroll(double pct) {
        ScrollPosition currentPosition = getScrollPosition();
        double desiredPosition = currentPosition.course + (pct / 5.0);
        int itemIndex = (int) (desiredPosition * listLength);
        listView.scrollToPosition(itemIndex);
    }

    private void updateScrollers() {
        ScrollPosition currentPosition = getScrollPosition();
        courseButtons.forEach(b -> b.setBackgroundResource(android.R.drawable.btn_default));
        fineButtons.forEach(b -> b.setBackgroundResource(android.R.drawable.btn_default));

        int courseIndex = (int) (currentPosition.course * 5.0);
        int fineIndex = (int) (currentPosition.fine * 5.0);

        courseButtons.get(courseIndex).setBackgroundResource(R.color.colorAccent);
        fineButtons.get(fineIndex).setBackgroundResource(R.color.colorAccent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_chooser);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        assert bundle != null;
        Serializable untypedInput = bundle.getSerializable(INPUT_KEY);
        assert untypedInput != null;
        @SuppressWarnings("unchecked") ArrayList<T> items = (ArrayList<T>) untypedInput;

        listView = findViewById(R.id.itemRecycler);
        listView.setHasFixedSize(true);

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(new Adapter(items));

        findViewById(R.id.courseScroller).setVisibility(items.size() > 8 ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.fineScroller).setVisibility(items.size() >  35 ? View.VISIBLE : View.INVISIBLE);

        if (items.size() > 8) {
            listLength = items.size();

            listView.setOnScrollChangeListener((v, x, y, x1, y1) -> updateScrollers());

            courseButtons =
                    Stream.of(R.id.course10, R.id.course30, R.id.course50, R.id.course70, R.id.course90)
                            .map(id -> (Button) findViewById(id))
                            .collect(Collectors.toList());

            fineButtons =
                    Stream.of(R.id.fine10, R.id.fine30, R.id.fine50, R.id.fine70, R.id.fine90)
                            .map(id -> (Button) findViewById(id))
                            .collect(Collectors.toList());

            for (int i = 0; i < 5; i++) {
                final int count = i;
                courseButtons.get(i).setOnClickListener(view -> courseScroll(0.1 + 0.2 * count));
                fineButtons.get(i).setOnClickListener(view -> fineScroll(0.1 + 0.2 * count));
            }

            updateScrollers();
        }
    }
}
