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
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Zynch on 07/03/2018.
 */

public class SongListFragment extends Fragment {
    // Based on http://www.java2s.com/Open-Source/Android_Free_Code/App/design/com_epam_dziashko_aliaksei_materialdemo_fragmentRecyclerViewFragment_java.htm
    // & https://developer.android.com/samples/RecyclerView/src/com.example.android.recyclerview/RecyclerViewFragment.html
    // & https://stackoverflow.com/questions/24777985/how-to-implement-onfragmentinteractionlistener
    // & https://developer.android.com/guide/topics/ui/layout/recyclerview.html

    // Tag for debug
    private static final String TAG = SongListFragment.class.getSimpleName();

    private onSongListFragmentInteractionListener mSongListFragmentCallback;

    // Interface for onInteraction callback
    public interface onSongListFragmentInteractionListener {
        void playSongFromPlaysetIndex(int position);
        void playSong();
    }

    private boolean swipeToRemove = false;

    // view, adapter & manager
    private RecyclerView mRecyclerView;
    private static SongListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // Song content access
    private static ArrayList<Song> songList = new ArrayList<>();
    MusicLibrary mMusicLibrary;

    // Fragment data trackers
    private String bundleParentType = "PARENT_TYPE";    // Field we look for in bundle,
    private String parentType = "";                     // The value should be used in switch statement
    private String bundleArgsAlbumId = "ALBUM_ID";      // Field we look for in bundle
    private String AlbumId = "";                        // The actual value in said field

    public SongListFragment(){
        Log.d(TAG, "new SongListFragment");
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        // Overrided method onAttach(Activity activity) is now deprecated in android.app.Fragment,
        // code should be upgraded to onAttach(Context context)
        try {
            mSongListFragmentCallback = (onSongListFragmentInteractionListener) context;
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
            mSongListFragmentCallback = (onSongListFragmentInteractionListener) getActivity();
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

        songList = new ArrayList<>();

        // Get access to song list
        mMusicLibrary = MusicLibrary.getInstance();

        Bundle args = getArguments();
        try {
            parentType = args.getString(bundleParentType);
            AlbumId = args.getString(bundleArgsAlbumId);
        } catch (Exception e) {
            Log.w(TAG, "onCreate: missing bundle args: ");
        }

        // get songs by album id or get all songs
        if ("QUEUE".equals(parentType)){
            Log.d(TAG, "onCreate: getSongsFrom Queue");
            swipeToRemove = true;
            getSongsFromQueue();

        } else  if ("PLAYSET".equals(parentType)){
            Log.d(TAG, "onCreate: getSongsFrom PLAYSET");
            getSongsFromPlayset();

        }else if ((null != parentType && !"".equals(parentType) && null != AlbumId && !"".equals(AlbumId))) {
            Log.d(TAG, "onCreate: getSongsFrom " + parentType + " ByAlbumId (" + AlbumId + ")");
            getSongsByAlbumId(AlbumId);

        } else{
            Log.d(TAG, "onCreate: getAllSongs");
            getAllSongs();
        }

        mAdapter = new SongListAdapter(songList);

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
                Log.d(TAG, "onLeftSwipe"); // Swipe to remove from queue
                if (swipeToRemove){
                    Log.d(TAG, "onLeftSwipe: remove song from queue");
                    mMusicLibrary.removeSongFromQueue(position);
                    Toast.makeText(getActivity(), "Removed " + songList.get(position).getTitle() + " from queue", Toast.LENGTH_SHORT).show();
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onRightSwipe(View view, int position) {
                Log.d(TAG, "onRightSwipe"); // Swipe to add to queue
                if (!swipeToRemove){
                    Log.d(TAG, "onRightSwipe: add song to queue");
                    mMusicLibrary.addSongToQueue(songList.get(position));
                    Toast.makeText(getActivity(), "Added " + songList.get(position).getTitle() + " to queue", Toast.LENGTH_SHORT).show();
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onClick(View view, int position) {
                Log.d(TAG, "onClick");
                if (!swipeToRemove) {
                    mMusicLibrary.setCurrent_position(position);
                    mMusicLibrary.setSongs(songList);
                } else {
                    // Can't set playset to queue else bad things happen
                    mMusicLibrary.setCurrentSong(songList.get(position));
                }
                mSongListFragmentCallback.playSong();
            }
        }));

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (null != asyncSongQuery && !asyncSongQuery.isCancelled()){
            asyncSongQuery.cancel(true);
        }
        // Remove listener when fragment is destroyed
        mSongListFragmentCallback = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Remove listener when fragment is not visible
        mSongListFragmentCallback = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    // -----------------------------ALL SONG QUERY STUFFS---------------------------------
    // COLUMN HELPER
    private String[] songColumns = {
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
    };

    private ContentResolver mContentResolver;

    private void QueryHelper(String selection){
        Cursor cursor = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songColumns, selection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        asyncSongQuery = new AsyncSongQuery();
        asyncSongQuery.execute(cursor);
    }


    // TODO GET ALL SONGS
    public void getAllSongs() {
        ArrayList<Song> songList = new ArrayList<>();

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Media.IS_MUSIC + "=1";

        // By setting end to -1 it will loop until all songs have been found
        QueryHelper(songSELECTION);
    }

    // TODO GET SONGS BY ARTIST ID
    public void getSongsByArtistId(String id) {

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Media.IS_MUSIC + "=1 and " + MediaStore.Audio.Media.ARTIST_ID + "=" + id;

        // By setting end to -1 it will instead loop until all songs have been found
        QueryHelper(songSELECTION);
    }

    // TODO GET SONGS BY ALBUM ID
    public void getSongsByAlbumId(String id) {

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Media.IS_MUSIC + "=1 and " + MediaStore.Audio.Media.ALBUM_ID + "=" + id;

        // By setting end to -1 it will instead loop until all songs have been found
        QueryHelper(songSELECTION);
    }

   public void getSongsFromQueue(){
        songList = MusicLibrary.getInstance().getSongQueue();
        mAdapter.notifyDataSetChanged();
   }

   public void getSongsFromPlayset(){
       songList = MusicLibrary.getInstance().getSongs();
       mAdapter.notifyDataSetChanged();
   }

    // TODO GET SONGS BY DECADE
    public void getSongsByDecade(int year) {

        int decade_lower = (int) Math.floor(year / 10d) * 10;
        int decade_upper = decade_lower + 9;


        // Only accept music files and match id
        String songSELECTION =MediaStore.Audio.Media.YEAR + " >= " + decade_lower + " and " + MediaStore.Audio.Media.YEAR + " <= " + decade_upper +
                " and " + MediaStore.Audio.Media.IS_MUSIC + "=1";

        // By setting end to -1 it will loop until all songs have been found
        QueryHelper(songSELECTION);
    }

    private static AsyncSongQuery asyncSongQuery;

    private class AsyncSongQuery extends AsyncTask<Cursor, Song, ArrayList<Song>> {

        @Override
        protected ArrayList<Song> doInBackground(Cursor... cursors) {
            Log.d(TAG, "AsyncSongQuery: doInBackground");
            ArrayList<Song> asyncAlbums = new ArrayList<>();

            if(null != cursors[0] && cursors[0].getCount() > 0){
                Cursor songCursor = cursors[0];
                try {
                    for (songCursor.moveToFirst(); !songCursor.isAfterLast(); songCursor.moveToNext()) {
                        if(isCancelled()){
                            // Prevents tasks continuing after fragment destroy or detach
                            Log.d(TAG, "AsyncSongQuery cancelled");
                            break;
                        }
                        Song song = new Song(
                                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                        );


                        try (Cursor artCursor = mContentResolver.query(
                                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                                MediaStore.Audio.Albums._ID + "="+song.getAlbum_id(),
                                null,
                                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)){
                            artCursor.moveToFirst();
                            song.setArt(artCursor.getString(artCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));
                            // Its Weird I know, but https://stackoverflow.com/questions/33138006/album-info-does-not-contain-album-id-column-error
                            song.setAlbum_id(artCursor.getString(artCursor.getColumnIndex(MediaStore.Audio.Albums._ID)));
                        } catch (NullPointerException npe){
                            Log.e(TAG, "Could not get art: ");
                        }
                        // Update List as we go
                        publishProgress(song);
                    }
                    Log.d(TAG, "AsyncSongQuery: loaded = true");
                    songCursor.close();

                } catch (Exception e){
                    Log.d(TAG, "AsyncSongQuery", e);
                    songCursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Song... values) {
            super.onProgressUpdate(values);
            songList.add(values[0]);
            mAdapter.notifyItemInserted(mAdapter.addItem());
        }
    }

}
