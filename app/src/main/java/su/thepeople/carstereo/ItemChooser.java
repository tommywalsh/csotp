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
        @Override protected ArrayList<Album> unbundle(Bundle bundle) {
            return (ArrayList<Album>) bundle.getSerializable("ALBUMS");
        }
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
        @Override protected ArrayList<Band> unbundle(Bundle bundle) {
            return (ArrayList<Band>) bundle.getSerializable("BANDS");
        }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_chooser);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        ArrayList<T> items = unbundle(bundle);

        RecyclerView view = findViewById(R.id.itemRecycler);
        view.setHasFixedSize(true);

        view.setLayoutManager(new LinearLayoutManager(this));
        view.setAdapter(new Adapter(items));
    }
}
