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

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

/**
 * This is an abstract superclass that defines most of the behavior of a scrollable item picker. Internal classes
 * specialize the behavior for different types.
 *
 * The ItemChooser is sent a list of objects (the object must have an "id" and a "name", and be serializable). If the
 * user selects one of them, then the ID of that item is sent back as a response. If not, then no response is sent.
 */
public abstract class ItemChooser <T extends Serializable> extends AppCompatActivity {

    protected abstract String getName(T obj);
    protected abstract int getId(T obj);
    protected abstract ArrayList<T> unbundle(Bundle bundle);
    protected abstract boolean shouldShowScrollers();

    /**
     * A scrollable album picker
     */
    public static class AlbumChooser extends ItemChooser<Album> {
        @Override protected String getName(Album a) {
            return a.name;
        }
        @Override protected int getId(Album a) {
            return a.uid;
        }
        @Override @SuppressWarnings("unchecked") protected ArrayList<Album> unbundle(Bundle bundle) {
            return (ArrayList<Album>) bundle.getSerializable("ALBUMS");
        }
        @Override protected boolean shouldShowScrollers() { return false; }
    }

    /**
     * A scrollable band picker
     */
    public static class BandChooser extends ItemChooser<Band> {
        @Override protected String getName(Band b) {
            return b.name;
        }
        @Override protected int getId(Band b) {
            return b.uid;
        }
        @Override @SuppressWarnings("unchecked") protected ArrayList<Band> unbundle(Bundle bundle) {
            return (ArrayList<Band>) bundle.getSerializable("BANDS");
        }
        @Override protected boolean shouldShowScrollers() { return true; }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        Button view;

        ViewHolder(Button view) {
            super(view);
            this.view = view;
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private List<T> items;

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
            int id = getId(items.get(listPosition));
            Intent intent = new Intent();
            intent.putExtra("ITEM_ID", id);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.view.setTag(position);
            holder.view.setText(getName(items.get(position)));
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
        ArrayList<T> items = unbundle(bundle);

        listView = findViewById(R.id.itemRecycler);
        listView.setHasFixedSize(true);

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(new Adapter(items));

        findViewById(R.id.courseScroller).setVisibility(shouldShowScrollers() ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.fineScroller).setVisibility(shouldShowScrollers() ? View.VISIBLE : View.INVISIBLE);

        if (shouldShowScrollers()) {
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
