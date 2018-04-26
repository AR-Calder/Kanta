package uk.arcalder.Kanta;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener,
            View.OnClickListener,
            SongListFragment.onSongListFragmentInteractionListener,
            ArtistListFragment.onArtistListFragmentInteractionListener,
            AlbumListFragment.onAlbumListFragmentInteractionListener,
            MiniPlayerFragment.onMiniPlayerPlayPauseClickListener,
            FragmentManager.OnBackStackChangedListener{

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private boolean PERMISSIONS_OK = false;

    private static final String TAG = MainActivity.class.getSimpleName();

    // Music service requirements
    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;
    private MediaControllerCompat.TransportControls mTransportControls;

    // Music Library - tracks songs, artist, albums
    MusicLibrary mMusicLibrary;

    // Tracks playback state
    private static int currentPlaybackState;

    // Store MusicLibrary during rotation/backgrounded events
    private VolatileStorageFragment storageFragment;
    private final String STORAGE_TAG = "STORAGE_FRAGMENT";

    // Service connection callback
    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                Log.d(TAG, "onConnected called");

                // Get session token
                MediaSessionCompat.Token token =  mMediaBrowserCompat.getSessionToken();

                // Create MediaControllerCompat
                mMediaControllerCompat =
                        new MediaControllerCompat(MainActivity.this, // context
                        token);

                // Save the controller
                MediaControllerCompat.setMediaController(MainActivity.this,
                        mMediaControllerCompat);

                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);

            } catch( RemoteException e ) {
                Log.wtf(TAG, e);
            }
        }

        @Override
        public void onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
            Log.d(TAG, "onConnectionSuspended");
        }

        @Override
        public void onConnectionFailed() {
            // The Service has refused our connection
            Log.d(TAG, "onConnectionFailed");
        }
    };

    // Controller callback
    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                Log.d(TAG, "onPlaybackStateChanged(null)");
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    Log.d(TAG, "onPlaybackStateChanged to: STATE_PLAYING");
                    currentPlaybackState = PlaybackStateCompat.STATE_PLAYING;
                    miniPlayerFragment(true, true);
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    Log.d(TAG, "onPlaybackStateChanged to: STATE_PAUSED");
                    currentPlaybackState = PlaybackStateCompat.STATE_PAUSED;
                    miniPlayerFragment(false, true);
                    break;
                }
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                    Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_NEXT");
                    currentPlaybackState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
                    miniPlayerFragment(true, true);
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                    Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_PREVIOUS");
                    currentPlaybackState = PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
                    miniPlayerFragment(true, true);
                    break;
                default:
                    Log.d(TAG, "onPlaybackStateChanged to: *STATE_STOPPED");
                    currentPlaybackState = PlaybackStateCompat.STATE_STOPPED;
                    miniPlayerFragment(false, false);
            }
        }

        @Override
        public void onSessionDestroyed() {
            Log.d(TAG, "onSessionDestroyed");
            super.onSessionDestroyed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate Called");


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --------------------------Load Existing Library if exists--------------------------------
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        storageFragment = (VolatileStorageFragment) fm.findFragmentByTag(STORAGE_TAG);

        if (storageFragment == null) {
            Log.d(TAG, "onCreate: new storageFragment");

            //add the fragment
            storageFragment = new VolatileStorageFragment();
            fm.beginTransaction().add(storageFragment, STORAGE_TAG).commit();
            // set data source
            storageFragment.saveList();
        } else {
            Log.d(TAG, "onCreate: get existing storageFragment");
        }

        mMusicLibrary = storageFragment.getList();



        // --------------------------Perform first time setup (if first time)-----------------------

        // DO NOT ADD THESE FRAGMENTS TO BACKSTACK (HOURS WASTED HERE)

        // --------------------------Get Permissions if no permissions------------------------------

        getPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 123);

        // --------------------------Connect to Music Player Service--------------------------------
        mMediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MusicPlayerService.class),
                mMediaBrowserCompatConnectionCallback, getIntent().getExtras());

        Log.d(TAG, "onCreate: connectionCallback supposedly set");

        // Setup bottom navigation view, disable shift mode (which looks crap) and add listener
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigationBarBottom);
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(this);
    }

    // --------------------------Handle Permissions "Elegantly"-------------------------------------


    public void firstFragments(){

        FragmentManager fragMan = getSupportFragmentManager();
        FragmentTransaction fragTrans = fragMan.beginTransaction();
        Bundle fragArgs = new Bundle();



        if(MusicLibrary.getInstance().hasPermission() && (fragMan.findFragmentByTag("TITLE_HOME") == null || fragMan.findFragmentByTag("CONTAINER_HOME") == null)) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();

            boolean first_run = !preferences.contains("FIRST_RUN");
            if(first_run)
            {
                Toast.makeText(this, "Thanks for trying out Kanta", Toast.LENGTH_SHORT).show();
                editor.putString("FIRST_RUN", "FIRST_RUN").apply();
            }

            TitlebarFragment titlebarFragment = new TitlebarFragment();
            fragArgs.putString("TITLE", "HOME");
            titlebarFragment.setArguments(fragArgs);
            fragTrans.replace(R.id.fragment_container_toolbar, titlebarFragment, "TITLE_HOME");
            fragTrans.replace(R.id.fragment_container_main, new SongListFragment(), "CONTAINER_HOME");
            fragTrans.commit();
        }
    }

    public void getPermission(String Permission, int requestCode){

        Log.i(TAG, "getPermission: Check if have permissions: " + "READ_EXTERNAL_STORAGE");
        // Request permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this, Permission)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "getPermission: Permission not granted");
            // Permission is not granted so set false for now
            MusicLibrary.getInstance().setHasPermission(false);

            // but request the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Permission},
                    requestCode); // Once again shite documentation

            // The callback method gets the result of the request.
            Log.i(TAG, "getPermission: Requesting Permission");

        } else {
            // Permission has already been granted
            firstFragments();
            MusicLibrary.getInstance().setHasPermission(true);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Change fragments based on available options
        FragmentManager fragMan = getSupportFragmentManager();
        FragmentTransaction fragTrans = fragMan.beginTransaction();
        Bundle fragArgs = new Bundle();

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // --------------------------HAS PERMISSIONS------------------------------

            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            MusicLibrary.getInstance().setHasPermission(true);
            firstFragments();

        } else {

            // --------------------------NO PERMISSIONS------------------------------

            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            MusicLibrary.getInstance().setHasPermission(false);

        }


        // other 'case' lines to check for other
        // permissions this app might request.

    }
    // -----------------------------------Life Cycle------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart Called");

        mMediaBrowserCompat.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume Called");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        miniPlayerFragment((currentPlaybackState == PlaybackStateCompat.STATE_PLAYING),
                (currentPlaybackState != PlaybackStateCompat.STATE_NONE &&
                        currentPlaybackState != PlaybackStateCompat.STATE_STOPPED));
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(mMediaControllerCompatCallback);
        }
        mMediaBrowserCompat.disconnect();
        super.onStop();
    }

    private int doubleTapToExit = 2;

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        getSupportFragmentManager().popBackStack();
        if (doubleTapToExit < 1) {
            finishAndRemoveTask();

        } else if (doubleTapToExit < 2){
            Toast.makeText(getApplicationContext(), "Triple Tap to exit", Toast.LENGTH_SHORT).show();
        }


        this.doubleTapToExit--;

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleTapToExit = 2;
            }
        }, 450);


    }


    //---------------------------Fragment Loaders--------------------------------------------
    private MiniPlayerFragment mMiniPlayerFragment;

    // Mini Player Fragments
    private void miniPlayerFragment(Boolean isPlaying, boolean show) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (show && MusicLibrary.getInstance().hasPermission() && null != mMusicLibrary.getCurrentSong()) {
            Log.d(TAG, "miniPlayerFragment: show");
            mMiniPlayerFragment = new MiniPlayerFragment();
            Bundle bundle = new Bundle();
            bundle.putString("ALBUM_ART", mMusicLibrary.getCurrentSong().getArt());
            bundle.putString("SONG_TITLE", mMusicLibrary.getCurrentSong().getTitle());
            bundle.putString("ARTIST_NAME", mMusicLibrary.getCurrentSong().getArtist());
            bundle.putBoolean("IS_PLAYING", isPlaying);
            mMiniPlayerFragment.setArguments(bundle);
            ft.replace(R.id.fragment_container_player, mMiniPlayerFragment);
        } else if (mMiniPlayerFragment != null){
            Log.d(TAG, "miniPlayerFragment: hide");
            ft.remove(mMiniPlayerFragment);
        }
        ft.commit();
    }

    // ------------------------------Main Navigation --------------------------------
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        // https://developer.android.com/reference/android/app/FragmentTransaction.html
        //https://developer.android.com/reference/android/app/FragmentManager.BackStackEntry.html
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        TitlebarFragment titlebarFragment = new TitlebarFragment();
        Bundle fargs = new Bundle();
        Bundle targs = new Bundle();

        if (!MusicLibrary.getInstance().hasPermission()){
            getPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 123);
            return true;
        }

        switch (item.getItemId()) {
            case R.id.navigation_home:
                ft = fm.beginTransaction();
                // Create default song fragment (no args)
                SongListFragment songListFragment = new SongListFragment();
                songListFragment.setArguments(fargs);

                // If set, and the name or ID of a back stack entry has been supplied,
                // then all matching entries will be consumed until one that doesn't match is
                // found or the bottom of the stack is reached.
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // Replace (or create) a fragment where in a container.
                ft.replace(R.id.fragment_container_main, songListFragment);
                // Setup title bar
                targs.putString("TITLE", "HOME");
                break;

            case R.id.navigation_artists: // navigation_artists
                ft = fm.beginTransaction();
                // Create default artist fragment (no args)
                ArtistListFragment artistListFragment = new ArtistListFragment();
                artistListFragment.setArguments(fargs);

                // If set, and the name or ID of a back stack entry has been supplied,
                // then all matching entries will be consumed until one that doesn't match is
                // found or the bottom of the stack is reached.
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // Replace (or create) a fragment where in a container.
                ft.replace(R.id.fragment_container_main, artistListFragment);
                // Setup title bar
                targs.putString("TITLE", "ARTISTS");
                break;

            case R.id.navigation_search:
                // Search
                ft = fm.beginTransaction();
                // Create default song fragment (no args)
                SongListFragment PlaySetSongListFragment = new SongListFragment();
                fargs.putString("PARENT_TYPE", "PLAYSET");
                PlaySetSongListFragment.setArguments(fargs);

                // If set, and the name or ID of a back stack entry has been supplied,
                // then all matching entries will be consumed until one that doesn't match is
                // found or the bottom of the stack is reached.
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // Replace (or create) a fragment where in a container.
                ft.replace(R.id.fragment_container_main, PlaySetSongListFragment);
                // Setup title bar
                targs.putString("TITLE", "PLAYSET");
                break;

            case R.id.navigation_albums:
                ft = fm.beginTransaction();
                // Create default artist fragment (no args)
                AlbumListFragment albumListFragment = new AlbumListFragment();
                albumListFragment.setArguments(fargs);

                // If set, and the name or ID of a back stack entry has been supplied,
                // then all matching entries will be consumed until one that doesn't match is
                // found or the bottom of the stack is reached.
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // Replace (or create) a fragment where in a container.
                ft.replace(R.id.fragment_container_main, albumListFragment);
                // Setup title bar
                targs.putString("TITLE", "ALBUMS");
                break;

            case R.id.navigation_queue:
                ft = fm.beginTransaction();
                // Create default song fragment (no args)
                SongListFragment queueSongListFragment = new SongListFragment();
                fargs.putString("PARENT_TYPE", "QUEUE");
                queueSongListFragment.setArguments(fargs);

                // If set, and the name or ID of a back stack entry has been supplied,
                // then all matching entries will be consumed until one that doesn't match is
                // found or the bottom of the stack is reached.
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // Replace (or create) a fragment where in a container.
                ft.replace(R.id.fragment_container_main, queueSongListFragment);
                // Setup title bar
                targs.putString("TITLE", "QUEUE");
                break;
        }

        // Since this is always updated do it here
        titlebarFragment.setArguments(targs);
        ft.replace(R.id.fragment_container_toolbar, titlebarFragment);
        ft.addToBackStack(null);
        ft.commit();
        return true;
    }

    // --------------------------------------Fragment callbacks---------------------------------------------------------

    @Override
    public void onClick(View view) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO something something parcle-able?
    }

    @Override
    public void playSongFromPlaysetIndex(int position) {
        Log.d(TAG, "playSongFromPlaysetIndex: index: " + position);
        mMusicLibrary.setCurrentSong(mMusicLibrary.getSongByIndexFromSongs(position));
        mMediaControllerCompat.getTransportControls().playFromMediaId(mMusicLibrary.getCurrentSong().getData(), new Bundle());
    }

    @Override
    public void playSong() {    // THIS IS NOT onPlay - That is effectively onResume/onNowPlaying
        Log.d(TAG, "playSong: " + mMusicLibrary.getCurrentSong().getTitle());
        mMediaControllerCompat.getTransportControls().playFromMediaId(mMusicLibrary.getCurrentSong().getData(), new Bundle());
    }

    @Override
    public void clickMiniPlayerPlayPause(boolean state) {
        if (state){
            // is currently playing so pause
            Log.d(TAG, "clickMiniPlayerPlayPause: pause");
            mMediaControllerCompat.getTransportControls().pause();
        } else {
            // isn't currently playing so resume (play)
            Log.d(TAG, "clickMiniPlayerPlayPause: play");
            mMediaControllerCompat.getTransportControls().play();
        }
    }

    @Override
    public void createAlbumViewFragmentFromAlbumID(String album_id, String album_name) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Bundle fargs = new Bundle();

        SongListFragment songListFragment = new SongListFragment();
        fargs.putString("PARENT_TYPE", album_name);
        fargs.putString("ALBUM_ID", album_id);

        songListFragment.setArguments(fargs);
        ft.replace(R.id.fragment_container_main, songListFragment);

        // Setup title bar
        Bundle tfargs = new Bundle();

        TitlebarFragment titlebarFragment = new TitlebarFragment();
        tfargs.putString("TITLE", album_name);

        titlebarFragment.setArguments(tfargs);
        ft.replace(R.id.fragment_container_toolbar, titlebarFragment);
        ft.addToBackStack(null);
        ft.commit();

    }

    @Override
    public void createAlbumListFragmentFromArtistName(String artist_name) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Bundle fargs = new Bundle();

        AlbumListFragment albumListFragment = new AlbumListFragment();
        fargs.putString("PARENT_TYPE", artist_name);
        fargs.putString("ARTIST_NAME", artist_name);

        albumListFragment.setArguments(fargs);
        ft.replace(R.id.fragment_container_main, albumListFragment);

        // Setup title bar
        Bundle tfargs = new Bundle();

        TitlebarFragment titlebarFragment = new TitlebarFragment();
        tfargs.putString("TITLE", artist_name);

        titlebarFragment.setArguments(tfargs);
        ft.replace(R.id.fragment_container_toolbar, titlebarFragment);
        ft.addToBackStack(null);
        ft.commit();
    }



    @Override
    public void onBackStackChanged() {

    }


}
