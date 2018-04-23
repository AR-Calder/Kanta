package uk.arcalder.Kanta;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    }

    // view, adapter & manager
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // Song List access
    MusicLibrary mMusicLibrary;

    public SongListFragment(){
        Log.d(TAG, "SongListFragment created");
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
        // Get access to song list
        mMusicLibrary = MusicLibrary.getInstance();

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

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_list_song_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        Log.d(TAG, "onCreateView: setAdapter to playSet");
        mAdapter = new SongListAdapter(mMusicLibrary.getViewSongs());
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
                mSongListFragmentCallback.playSongFromPlaysetIndex(position);
                mMusicLibrary.setPlaySet(mMusicLibrary.getViewSongs());
            }
        }));

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        // Remove listener when activity is destroyed
        mSongListFragmentCallback = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
