package uk.arcalder.Kanta;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by arcalder on 07/03/2018.
 */


public class AlbumListFragment extends Fragment {
    // Based on http://www.java2s.com/Open-Source/Android_Free_Code/App/design/com_epam_dziashko_aliaksei_materialdemo_fragmentRecyclerViewFragment_java.htm
    // & https://developer.android.com/samples/RecyclerView/src/com.example.android.recyclerview/RecyclerViewFragment.html
    // & https://stackoverflow.com/questions/24777985/how-to-implement-onfragmentinteractionlistener
    // & https://developer.android.com/guide/topics/ui/layout/recyclerview.html

    // Tag for debug
    private static final String TAG = SongListFragment.class.getSimpleName();

    private onAlbumListFragmentInteractionListener mAlbumListFragmentCallback;



    // Interface for onInteraction callback
    public interface onAlbumListFragmentInteractionListener {
        void createTitlebarFragmentFromAlbumName(String name);
        void createAlbumViewFragmentFromAlbumID(String album_id);
    }

    // view, adapter & manager
    private RecyclerView mRecyclerView;
    private static AlbumListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // Album content access
    private MusicProvider mMusicProvider;                   // Class that makes queries easier
    private static ArrayList<Album> albumList = new ArrayList<>();

    // Fragment data trackers
    private String bundleArgsArtistName = "ARTIST_NAME";   // Field we look for in bundle
    private String bundleArtistName = "";                 // The actual value in said field
    private String titlebarTitle    = "ALBUMS";

    public AlbumListFragment(){
        Log.d(TAG, "AlbumListFragment created");
    }


    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        // Overrided method onAttach(Activity activity) is now deprecated in android.app.Fragment,
        // code should be upgraded to onAttach(Context context)
        try {
            mAlbumListFragmentCallback = (AlbumListFragment.onAlbumListFragmentInteractionListener) context;
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
            mAlbumListFragmentCallback = (AlbumListFragment.onAlbumListFragmentInteractionListener) getActivity();
        } catch (ClassCastException e){
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // Check what type of load to do
        Bundle args = getArguments();
        bundleArtistName = args.getString(bundleArgsArtistName);

        if (null == bundleArtistName || bundleArtistName.equals("")){
            getAllAlbums();
        } else {
            getAlbumsByArtistName(bundleArtistName);
            titlebarTitle = bundleArtistName;
        }

        mAdapter = new AlbumListAdapter(albumList);

        //Retain Fragment to prevent unnecessary recreation
        //setRetainInstance(true);
        // ^ This won't work because:
        // "Retaining an instance will not work when added to the backstack"
        //https://stackoverflow.com/questions/11160412/why-use-fragmentsetretaininstanceboolean
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_song, container, false);
        rootView.setTag(TAG);

        Log.d(TAG, "onCreateView");
        // This is all basically from the sample @
        // https://developer.android.com/guide/topics/ui/layout/recyclerview.html#java

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_list_album_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a grid layout
        mLayoutManager = new GridLayoutManager(getActivity(), 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        Log.d(TAG, "onCreateView: setAdapter to albumList");
        // NOTE: mAdapter is initialized in onCreate
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnItemTouchListener(new RecyclerViewOnInteractionListener(getContext(), mRecyclerView, new RecyclerViewOnInteractionListener.OnTouchActionListener() {
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
                Album album = albumList.get(position);
                mAlbumListFragmentCallback.createAlbumViewFragmentFromAlbumID(album.getId());
                mAlbumListFragmentCallback.createTitlebarFragmentFromAlbumName(album.getName());
            }

        }));

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        // Remove listener when activity is destroyed
        mAlbumListFragmentCallback = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Remove listener when activity is detached
        mAlbumListFragmentCallback = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAlbumListFragmentCallback.createTitlebarFragmentFromAlbumName(titlebarTitle);
    }

    // -----------------------------ALL ALBUM QUERY STUFFS---------------------------------

    // COLUMN HELPER
    private String[] albumColumns = {
            // See Song.class for member explanation
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.ALBUM_ART
    };

    private void QueryHelper(String selection){
        Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumColumns, selection, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        asyncAlbumQuery = new AsyncAlbumQuery();
        asyncAlbumQuery.execute(cursor);
    }

    // TODO GET ALL ALBUMS
    public void getAllAlbums() {

        // No conditions, want everything
        String albumSELECTION = "";

        // By setting end to -1 it will loop until all results have been found
        QueryHelper(albumSELECTION);
    }

    // TODO GET ALBUM BY KEY
    public void getAlbumByKey(String key) {

        // Only accept albums that match Album key
        String albumSELECTION = MediaStore.Audio.Albums.ALBUM_KEY + "=" + key;

        // Only interested in 1 result
        QueryHelper(albumSELECTION);
    }


    // TODO GET ALBUMS BY ARTIST NAME
    public void getAlbumsByArtistName(String name) {

        // Only accept albums that match artist key
        String albumSELECTION = MediaStore.Audio.Albums.ARTIST + "=" + name;

        // Only interested in 1 result
        QueryHelper(albumSELECTION);
    }

    // TODO GET ALBUMS BY DECADE
    public void getAlbumByDecade(int year) {

        // Calculate decade range
        int decade_lower = (int) Math.floor(year / 10d) * 10;
        int decade_upper = decade_lower + 9;

        // Only accept albums that match decade
        String albumSELECTION = MediaStore.Audio.Albums.FIRST_YEAR + " >= " + decade_lower + " and " + MediaStore.Audio.Albums.FIRST_YEAR + "<=" + decade_upper;

        // By setting end to -1 it will loop until all results have been found
        QueryHelper(albumSELECTION);
    }



    private static AsyncAlbumQuery asyncAlbumQuery;

    private static class AsyncAlbumQuery extends AsyncTask<Cursor, Album, ArrayList<Album>> {

        @Override
        protected ArrayList<Album> doInBackground(Cursor... cursors) {
            Log.d(TAG, "AsyncAlbumQuery: doInBackground");
            ArrayList<Album> asyncAlbums = new ArrayList<>();

            if(null != cursors[0] && cursors[0].getCount() > 0){
                Cursor songCursor = cursors[0];
                try {
                    for (songCursor.moveToFirst(); !songCursor.isAfterLast(); songCursor.moveToNext()) {
                        String ID           = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                        String ALBUM        = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                        String ARTIST       = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                        String ALBUM_ART    = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                        publishProgress(new Album(ID, ALBUM, ARTIST, ALBUM_ART));
                    }
                    Log.d(TAG, "AsyncAlbumQuery: loaded = true");
                    songCursor.close();

                } catch (Exception e){
                    Log.d(TAG, "AsyncAlbumQuery", e);
                    songCursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Album... values) {
            super.onProgressUpdate(values);
            albumList.add(values[0]);
            mAdapter.notifyItemInserted(mAdapter.addItem());
        }
    }
}

