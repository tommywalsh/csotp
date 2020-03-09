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

import java.util.ArrayList;
import java.util.List;

import su.thepeople.carstereo.data.Album;

public class AlbumChooser extends AppCompatActivity {

    private static class ViewHolder extends RecyclerView.ViewHolder {
        Button view;

        ViewHolder(Button view) {
            super(view);
            this.view = view;
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private List<Album> albums;

        Adapter(List<Album> albums) {
            this.albums = albums;
        }

        @Override
        public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Button view = (Button) LayoutInflater.from(parent.getContext()).inflate(R.layout.album_item, parent, false);
            view.setOnClickListener(this::onAlbumSelect);
            return new ViewHolder(view);
        }

        private void onAlbumSelect(View view) {
            Integer listPosition = (Integer) view.getTag();
            int albumId = albums.get(listPosition).uid;
            Intent intent = new Intent();
            intent.putExtra("ALBUM_ID", albumId);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.view.setTag(position);
            holder.view.setText(albums.get(position).name);
        }

        @Override
        public int getItemCount() {
            return albums.size();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.hideSystemUI(this, R.id.albumPickerLayout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_chooser);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        ArrayList<Album> ALBUMS = (ArrayList<Album>) bundle.getSerializable("ALBUMS");

        RecyclerView view = findViewById(R.id.albumRecycler);
        view.setHasFixedSize(true);

        view.setLayoutManager(new LinearLayoutManager(this));
        view.setAdapter(new Adapter(ALBUMS));
    }
}
