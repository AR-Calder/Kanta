package uk.arcalder.Kanta;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
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
            SongListFragment.onSongListFragmentInteractionListener{

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private static final String TAG = MainActivity.class.getSimpleName();

    // Music service requirements
    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;
    private MediaControllerCompat.TransportControls mTransportControls;

    // Music Library - tracks songs, artist, albums
    MusicLibrary mMusicLibrary;

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
                    miniPlayerFragment(new MiniPlayerFragment(), true);
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    Log.d(TAG, "onPlaybackStateChanged to: STATE_PLAYING");
                    miniPlayerFragment(new MiniPlayerFragment(), true);
                    break;
                }
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                    Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_NEXT");
                    miniPlayerFragment(new MiniPlayerFragment(), true);
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                    Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_PREVIOUS");
                    miniPlayerFragment(new MiniPlayerFragment(), true);
                    break;
                default:
                    Log.d(TAG, "onPlaybackStateChanged to: *STATE_NOT_CARE_ABOUT");
                    miniPlayerFragment(mMiniPlayerFragment, false);
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



        // --------------------------Load Existing Library if exists--------------------------
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        storageFragment = (VolatileStorageFragment) fm.findFragmentByTag(STORAGE_TAG);

        if (storageFragment == null){
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

        // --------------------------Get Permissions if no permissions--------------------------

        getPermission(Manifest.permission.WAKE_LOCK);
        getPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

        mMusicLibrary.initLibrary(getApplicationContext());

        // --------------------------Load Fragments---------------------------------------------

        // Load default fragments
        loadToolbarFragment(new TitlebarFragment(), "HOME");
        loadSongListFragment(new SongListFragment(), "ALBUM");


        // --------------------------Connect to Music Player Service--------------------------
        mMediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MusicPlayerService.class),
                mMediaBrowserCompatConnectionCallback, getIntent().getExtras());

        Log.d(TAG, "onCreate: connectionCallback supposedly set");

        // Setup bottom navigation view, disable shift mode (which looks crap) and add listener
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigationBarBottom);
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(this);
    }



    public void getPermission(String Permission){

        Log.i(TAG, "getPermission: Check if have permissions: " + "READ_EXTERNAL_STORAGE");
        // Request permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this, Permission)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "getPermission: Permission not granted");
            // Permission is not granted so,
            // request the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Permission},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE); // Once again shite documentation

            // The callback method gets the result of the request.
            Log.i(TAG, "getPermission: Requesting Permission");

        } else {
            // Permission has already been granted
            mMusicLibrary.setHasPermission(true, getApplicationContext());
        }
    }

    // -----------------------------------Life Cycle---------------------------------------------------
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop Called");
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(mMediaControllerCompatCallback);
        }
        mMediaBrowserCompat.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    mMusicLibrary.setHasPermission(true, getApplicationContext());

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), (CharSequence)"PERMISSION DENIED, requires\nREAD_EXTERNAL_STORAGE to continue!", Toast.LENGTH_LONG).show();
                    mMusicLibrary.setHasPermission(false, getApplicationContext());
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // TODO fragment stack stuff here

    }


    //---------------------------Fragment Loaders--------------------------------------------
    private MiniPlayerFragment mMiniPlayerFragment;

    // Mini Player Fragments
    private void miniPlayerFragment(MiniPlayerFragment mp_frag, boolean show) {
        if (mp_frag != null) {
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            if (show) {
                mMiniPlayerFragment = mp_frag;
                fm.replace(R.id.fragment_container_player, mp_frag);
            } else if (mMiniPlayerFragment != null){
                fm.remove(mMiniPlayerFragment);
            }
            fm.commit();
        }
    }

    // List fragments
    private boolean loadSongListFragment(SongListFragment list_frag, String TAG) {
        if (TAG.equals("HOME")){
            mMusicLibrary.setViewSongs(mMusicLibrary.getSongs());
        }
        // If Fragment doesn't exist
        if (list_frag != null && null == getSupportFragmentManager().findFragmentByTag(TAG)) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_main, list_frag, TAG)
                    .commit();
            return true;
        } else {
            // Fragment exists
            //Toast.makeText(getApplicationContext(), TAG, Toast.LENGTH_SHORT).show();
        }
        return false;

    }

    // Toolbar / search bar fragments
    private boolean loadToolbarFragment(Fragment toolbar_frag, String TAG) {
        Bundle bundle = new Bundle();
        bundle.putString("TITLE", TAG);
        toolbar_frag.setArguments(bundle);
        // IF Fragment already exists
        if (toolbar_frag != null && null == getSupportFragmentManager().findFragmentByTag(TAG)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_toolbar, toolbar_frag, TAG)
                    .commit();
            return true;
        } else if (TAG.equals("SEARCH")) {
            // Fragment exists
            //TODO load searchEditText fragment, Set focus to search editText, open keyboard (and do the same onClick of same item)
            Toast.makeText(getApplicationContext(), "load searchEditText fragment, Set focus to search editText, open keyboard", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    // ------------------------------Main Navigation --------------------------------
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        Fragment title_fragment = null;
        SongListFragment list_fragment = null;

        String TITLE_TAG = null;
        String LIST_TAG = null;

        boolean RESULT = false;

        // TODO replace with actual functionality
        // TODO come up with better way to switch toolbar type
        switch (item.getItemId()) {
            case R.id.navigation_home:
                // Home
                TITLE_TAG = "HOME";
                list_fragment = new SongListFragment();
                title_fragment = new TitlebarFragment();
                break;
            case R.id.navigation_dashboard:
                // Browse
                TITLE_TAG = "BROWSE";
                list_fragment = new SongListFragment();
                title_fragment = new TitlebarFragment();
                try {
                    // TODO put this somewhere else.
                    Song thisSong = mMusicLibrary.getSongs().get(2);
                    Log.d(TAG, String.valueOf(thisSong.getTitle()));
                    Log.d(TAG, String.valueOf(thisSong.getId()));
                    mMusicLibrary.setCurrentSong(thisSong);
                    MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(thisSong.getData(), null);
                } catch (Exception e){
                    Log.wtf(TAG, e);
                }
                break;
            case R.id.navigation_search:
                // Search
                TITLE_TAG = "SEARCH";
                list_fragment = new SongListFragment();
                title_fragment = new SearchFragment();
                try {
                    // TODO put this somewhere else.
                    Song thisSong = mMusicLibrary.getSongs().get(0);
                    Log.d(TAG, String.valueOf(thisSong.getTitle()));
                    Log.d(TAG, String.valueOf(thisSong.getId()));
                    mMusicLibrary.setCurrentSong(thisSong);
                    MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(thisSong.getData(), null);
                } catch (Exception e){
                    Log.wtf(TAG, e);
                }
                break;
            case R.id.navigation_notifications:
                // QUEUE
                TITLE_TAG = "QUEUE";
                list_fragment = new SongListFragment();
                title_fragment = new TitlebarFragment();
                break;
            case R.id.navigation_library:
                // LIBRARY
                TITLE_TAG = "LIBRARY";
                list_fragment = new SongListFragment();
                title_fragment = new TitlebarFragment();
                break;
        }
        if (null != title_fragment) {
            RESULT = loadToolbarFragment(title_fragment, TITLE_TAG);
        }
        if (null != list_fragment) {
            RESULT = loadSongListFragment(list_fragment, "ALBUM");
        }

        return RESULT;
    }

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
        Log.d(TAG, "getSongByIndexFromSongs: index: " + position);
        mMusicLibrary.setCurrentSong(mMusicLibrary.getSongByIndexFromSongs(position));
        mMediaControllerCompat.getTransportControls().playFromMediaId(mMusicLibrary.getCurrentSong().getData(), new Bundle());
    }
}
