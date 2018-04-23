package uk.arcalder.Kanta;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * Created by Zynch on 23/04/2018.
 */

public class BigPlayerArtFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_big_player_album, container, false);
        ImageView imageView = (ImageView)rootView.findViewById(R.id.imageViewBigAlbumArt);
        try {
            Picasso.get().load(MusicLibrary.getInstance().getCurrentSong().getArt()).fit().centerCrop().into(imageView);
        } catch (Exception e){
            Picasso.get().load(android.R.drawable.ic_dialog_alert).fit().centerCrop().into(imageView);
        }
        return rootView;
    }
}
