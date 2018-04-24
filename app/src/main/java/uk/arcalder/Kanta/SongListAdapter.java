package uk.arcalder.Kanta;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;


/**
 * Created by Zynch on 21/04/2018.
 */

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder>{

    private static final String TAG = SongListAdapter.class.getSimpleName();
    // As per https://developer.android.com/guide/topics/ui/layout/recyclerview.html#java

    public static final int PLAYTYPE_FROM_SONGS = 0;
    public static final int PLAYTYPE_FROM_ALBUM = 1;
    public static final int PLAYTYPE_FROM_ARTIST = 2;

    public int addedItems = 0;

    public int addItem(){
        return addedItems++;
    }

    private ArrayList<Song> adapterSongs;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView     songListTitleView;
        public TextView     songListArtistAlbumView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.songListTitleView = (TextView) itemView.findViewById(R.id.song_item_title);
            this.songListArtistAlbumView = (TextView) itemView.findViewById(R.id.song_item_artist_album);
        }
    }

    public SongListAdapter(ArrayList<Song> songs){
        Log.d(TAG, "SongListAdapter created");
        this.adapterSongs = songs;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SongListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        Log.d(TAG, "onCreateViewHolder");
        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list_item, parent, false);
        return new ViewHolder(mView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song song;
        try {
            song = adapterSongs.get(position);
        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ", e);
            return;
        }

        // Set Title
        holder.songListTitleView.setText(song.getTitle());
        // Set Artist/Album
        holder.songListArtistAlbumView.setText(String.format("%s / %s", song.getArtist(), song.getAlbum()));

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return adapterSongs.size();
    }

}
