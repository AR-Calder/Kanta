package uk.arcalder.Kanta;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by Zynch on 07/03/2018.
 */

public class ArtistListFragment extends Fragment {
    // Based on http://www.java2s.com/Open-Source/Android_Free_Code/App/design/com_epam_dziashko_aliaksei_materialdemo_fragmentRecyclerViewFragment_java.htm
    // & https://developer.android.com/samples/RecyclerView/src/com.example.android.recyclerview/RecyclerViewFragment.html
    // & https://stackoverflow.com/questions/24777985/how-to-implement-onfragmentinteractionlistener
    // & https://developer.android.com/guide/topics/ui/layout/recyclerview.html

    // Tag for debug
    private static final String TAG = ArtistListFragment.class.getSimpleName();

    private onArtistListFragmentInteractionListener mArtistListFragmentCallback;

    // Interface for onInteraction callback
    public interface onArtistListFragmentInteractionListener {
        void createAlbumListFragmentFromArtistName(String artistName);
        void createTitlebarFragmentFromArtistName(String artistName);
    }

    // view, adapter & manager
    private RecyclerView mRecyclerView;
    private static ArtistListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // Song content access
    private static ArrayList<Artist> artistList = new ArrayList<>();
    MusicLibrary mMusicLibrary;

    // Fragment data trackers
    private String bundleParentType = "PARENT_TYPE";    // Field we look for in bundle,
    private String parentType = "";                     // The value should be used in switch statement
    private String bundleArgsArtistName = "ARTIST_NAME";// Field we look for in bundle
    private String artistName = "";                        // The actual value in said field

    public ArtistListFragment(){
        Log.d(TAG, "new ArtistListFragment");
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        // Overrided method onAttach(Activity activity) is now deprecated in android.app.Fragment,
        // code should be upgraded to onAttach(Context context)
        try {
            mArtistListFragmentCallback = (onArtistListFragmentInteractionListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        try {
            mArtistListFragmentCallback = (onArtistListFragmentInteractionListener) getActivity();
        } catch (ClassCastException e){
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mContentResolver = getActivity().getContentResolver();

        artistList = new ArrayList<>();

        // Get access to song list
        mMusicLibrary = MusicLibrary.getInstance();

        Bundle args = getArguments();
        try {
            parentType = args.getString(bundleParentType);
            artistName = args.getString(bundleArgsArtistName);
        } catch (Exception e) {
            Log.w(TAG, "onCreate: missing bundle args: ");
        }


        Log.d(TAG, "onCreate: getAllArtists");
        getAllArtists();


        mAdapter = new ArtistListAdapter(artistList);

        //Retain Fragment to prevent unnecessary recreation
        //setRetainInstance(true);
        // ^ This won't work because:
        // "Retaining an instance will not work when added to the backstack"
        //https://stackoverflow.com/questions/11160412/why-use-fragmentsetretaininstanceboolean
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_song, container, false);
        rootView.setTag(TAG);

        Log.d(TAG, "onCreateView");
        // This is all basically from the sample @
        // https://developer.android.com/guide/topics/ui/layout/recyclerview.html#java

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_list_song_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        Log.d(TAG, "onCreateView: setAdapter to playSet");
        // NOTE: mAdapter is initialized in onCreate
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnItemTouchListener(new RecyclerViewOnInteractionListener(getContext(), mRecyclerView, new RecyclerViewOnInteractionListener.OnTouchActionListener(){

            @Override
            public void onLeftSwipe(View view, int position) {
                Log.d(TAG, "onLeftSwipe");
            }

            @Override
            public void onRightSwipe(View view, int position) {
                Log.d(TAG, "onRightSwipe");
            }

            @Override
            public void onClick(View view, int position) {
                Log.d(TAG, "onClick");
                String artistName = artistList.get(position).getName();
                mArtistListFragmentCallback.createAlbumListFragmentFromArtistName(artistName);
                mArtistListFragmentCallback.createTitlebarFragmentFromArtistName(artistName);
            }
        }));

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (null != asyncArtistQuery && !asyncArtistQuery.isCancelled()){
            asyncArtistQuery.cancel(true);
        }
        // Remove listener when fragment is destroyed
        mArtistListFragmentCallback = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Remove listener when fragment is not visible
        mArtistListFragmentCallback = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    // -----------------------------ALL SONG QUERY STUFFS---------------------------------
    // COLUMN HELPER
    private String[] artistColumns = {
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
    };

    private ContentResolver mContentResolver;

    private void QueryHelper(String selection){
        Cursor cursor = mContentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, artistColumns, selection, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
        asyncArtistQuery = new AsyncArtistQuery();
        asyncArtistQuery.execute(cursor);
    }


    // TODO GET ALL SONGS
    public void getAllArtists() {
        ArrayList<Song> songList = new ArrayList<>();
        String songSELECTION = "";
        QueryHelper(songSELECTION);
    }


    private static AsyncArtistQuery asyncArtistQuery;

    private class AsyncArtistQuery extends AsyncTask<Cursor, Artist, ArrayList<Artist>> {

        @Override
        protected ArrayList<Artist> doInBackground(Cursor... cursors) {
            Log.d(TAG, "AsyncSongQuery: doInBackground");
            ArrayList<Album> asyncAlbums = new ArrayList<>();

            if(null != cursors[0] && cursors[0].getCount() > 0){
                Cursor artistCursor = cursors[0];
                try {
                    for (artistCursor.moveToFirst(); !artistCursor.isAfterLast(); artistCursor.moveToNext()) {
                        if(isCancelled()){
                            // Prevents tasks continuing after fragment destroy or detach
                            Log.d(TAG, "AsyncSongQuery cancelled");
                            break;
                        }
                        Artist artist = new Artist(
                                artistCursor.getString(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID)),
                                artistCursor.getString(artistCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)),
                                artistCursor.getString(artistCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)), // Not actually using these last two
                                artistCursor.getString(artistCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS))  // That's just, me being lazy 1 week to deadline tho
                        );


                        try (Cursor artCursor = mContentResolver.query(
                                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                new String[]{MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM_ART},
                                MediaStore.Audio.Albums.ARTIST + "="+artist.getName(), // This is pretty shitty but there is no id field here :/
                                null,
                                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)){
                            artCursor.moveToFirst();
                            artist.setArt(artCursor.getString(artCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));
                        } catch (NullPointerException npe){
                            Log.e(TAG, "Could not get art: ");
                        }
                        // Update List as we go
                        publishProgress(artist);
                    }
                    Log.d(TAG, "AsyncSongQuery: loaded = true");
                    artistCursor.close();

                } catch (Exception e){
                    Log.d(TAG, "AsyncSongQuery", e);
                    artistCursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Artist... values) {
            super.onProgressUpdate(values);
            artistList.add(values[0]);
            mAdapter.notifyItemInserted(mAdapter.addItem());
        }
    }

}
