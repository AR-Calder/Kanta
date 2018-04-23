package uk.arcalder.Kanta;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Objects;

/**
 * Created by Zynch on 14/03/2018.
 */

public class MiniPlayerFragment extends Fragment implements View.OnClickListener {
    View view;
    public static final String TAG = MiniPlayerFragment.class.getSimpleName();

    String songTitle = "", bundleSongTitle = "SONG_TITLE",
            artistName = "", bundleArtistName = "ARTIST_NAME",
            albumArt = "", bundleAlbumArt = "ALBUM_ART";

    boolean isPlaying = false;
    String bundleIsPlaying = "IS_PLAYING";

    private onMiniPlayerPlayPauseClickListener mMiniPlayerPlayPauseClickCallback;

    public interface onMiniPlayerPlayPauseClickListener {
        void clickMiniPlayerPlayPause(boolean state);
    }

    public MiniPlayerFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        try {
            songTitle = args.getString(bundleSongTitle);
            artistName = args.getString(bundleArtistName);
            albumArt = args.getString(bundleAlbumArt);
            isPlaying = args.getBoolean(bundleIsPlaying, true);
        } catch (Exception e) {
            Log.d(TAG, "Failed to set bundle args");
        }
    }

    @Override
    public void onAttach(Context context) {
        try {
            mMiniPlayerPlayPauseClickCallback = (MiniPlayerFragment.onMiniPlayerPlayPauseClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement onMiniPlayerPlayPauseClickListener");
        }

        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        view = inflater.inflate(R.layout.fragment_mini_player, null);

        // Song Title
        TextView songTextView = (TextView) view.findViewById(R.id.song_title_mini);
        if (null != songTextView) {
            songTextView.setText(songTitle);
        }

        // Artist name
        TextView artistTextView = (TextView) view.findViewById(R.id.artist_name_mini);
        if (null != artistTextView) {
            artistTextView.setText(artistName);
        }

        // Album Art Button
        ImageButton albumButton = (ImageButton) view.findViewById(R.id.btn_expand_player);
        albumButton.setOnClickListener(this);

        if (null != albumArt && !Objects.equals("", albumArt)) {
            albumButton.setImageBitmap(BitmapFactory.decodeFile(albumArt));
        }

        // Play / Pause button
        ImageButton playPauseButton = (ImageButton) view.findViewById(R.id.btn_play_pause_mini);
        playPauseButton.setOnClickListener(this);

        if (isPlaying) {
            playPauseButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_pause));
        } else {
            playPauseButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_play));
        }

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick");
        switch (view.getId()) {
            case R.id.btn_expand_player:
                Intent i = new Intent(this.getActivity(), BigPlayerActivity.class);
                startActivity(i);
                break;
            case R.id.btn_play_pause_mini:
                mMiniPlayerPlayPauseClickCallback.clickMiniPlayerPlayPause(isPlaying);
                break;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "clickMiniPlayerPlayPause");
        mMiniPlayerPlayPauseClickCallback = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        mMiniPlayerPlayPauseClickCallback = null;
        super.onDetach();
    }
}
