package uk.arcalder.Kanta;

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
 * Created by Zynch on 22/04/2018.
 */

public class ArtistListAdapter extends RecyclerView.Adapter<ArtistListAdapter.ViewHolder> {
    private static final String TAG = ArtistListAdapter.class.getSimpleName();
    // As per https://developer.android.com/guide/topics/ui/layout/recyclerview.html#java

    private ArrayList<Artist> adapterArtists;

    public static int addedItems = 0;

    public int addItem(){
        return addedItems++;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView    artistListArtView;
        public TextView     artistListArtistView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.artistListArtView = (ImageView) itemView.findViewById(R.id.artist_item_art);
            this.artistListArtistView = (TextView) itemView.findViewById(R.id.artist_item_name);
        }
    }

    public ArtistListAdapter(ArrayList<Artist> album){
        Log.d(TAG, "SongListAdapter created");
        this.adapterArtists = album;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ArtistListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        Log.d(TAG, "onCreateViewHolder");
        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_artist, parent, false);
        return new ArtistListAdapter.ViewHolder(mView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Artist artist;
        try {
            artist = adapterArtists.get(position);
        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ", e);
            return;
        }

        // Set art
        if (artist.getArt() != null && !"".equals(artist.getArt())){
            // Picasso doesn't need context, glide does
            // Glide is apparently better, but I can't be bothered adapting
            // all the code just for one lib
            // As per http://square.github.io/picasso/
            try {
                Picasso.get().load(new File(artist.getArt())).fit().centerCrop().into(holder.artistListArtView);
            } catch (IllegalArgumentException iae){
                Log.d(TAG, "Picasso tried to load albums.getArt = " +artist.getArt());
            }
        } else {
            try {
                Picasso.get().load(R.drawable.default_album).fit().centerCrop().into(holder.artistListArtView);
            } catch (IllegalArgumentException iae){
                Log.d(TAG, "Picasso tried and failed to load album art");
            }
        }

        // Set Title
        holder.artistListArtistView.setText(artist.getName());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return adapterArtists.size();
    }

}
