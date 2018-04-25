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
            SongListFragment.onSongListFragmentInteractionListener,
            ArtistListFragment.onArtistListFragmentInteractionListener,
            AlbumListFragment.onAlbumListFragmentInteractionListener,
            MiniPlayerFragment.onMiniPlayerPlayPauseClickListener{

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

        // --------------------------Get Permissions if no permissions------------------------------

        getPermission(Manifest.permission.WAKE_LOCK);
        getPermission(Manifest.permission.READ_EXTERNAL_STORAGE);


        // --------------------------Perform first time setup (if first time)-----------------------

        // DO NOT RELOAD THE FRAGMENTS IF THEY ALREADY EXIST (HOURS WASTED HERE)

        // Load default fragments
        loadSongListFragment(new SongListFragment(), "ALL SONGS");

        FragmentManager fragMan = getSupportFragmentManager();
        FragmentTransaction fragTrans = fragMan.beginTransaction();
        Bundle fragArgs = new Bundle();

        TitlebarFragment titlebarFragment = new TitlebarFragment();
        fragArgs.putString("TITLE", "HOME");

        titlebarFragment.setArguments(fragArgs);
        fragTrans.replace(R.id.fragment_container_toolbar, titlebarFragment);
        fragTrans.addToBackStack(null);
        fragTrans.commit();

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
            MusicLibrary.getInstance().setHasPermission(true, getApplicationContext());
        }
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
        super.onStop();
        Log.d(TAG, "onStop Called");
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(mMediaControllerCompatCallback);
        }
        mMediaBrowserCompat.disconnect();
    }

    @Override
    public void onBackPressed() {
        int levels = getSupportFragmentManager().getBackStackEntryCount();
        if (levels > -1) {
            super.onBackPressed();
        } else{
            this.moveTaskToBack(true);
        }

    }


    //---------------------------Fragment Loaders--------------------------------------------
    private MiniPlayerFragment mMiniPlayerFragment;

    // Mini Player Fragments
    private void miniPlayerFragment(Boolean isPlaying, boolean show) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (show) {
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

    // List fragments
    private boolean loadSongListFragment(SongListFragment list_frag, String TAG) {
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

    // ------------------------------Main Navigation --------------------------------
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        // https://developer.android.com/reference/android/app/FragmentTransaction.html
        //https://developer.android.com/reference/android/app/FragmentManager.BackStackEntry.html
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Bundle fargs = new Bundle();


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
                Bundle tfargs = new Bundle();

                TitlebarFragment titlebarFragment = new TitlebarFragment();
                tfargs.putString("TITLE", "HOME");

                titlebarFragment.setArguments(tfargs);
                ft.replace(R.id.fragment_container_toolbar, titlebarFragment);
                ft.addToBackStack(null);
                ft.commit();
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
                Bundle t1fargs = new Bundle();

                TitlebarFragment titlebar1Fragment = new TitlebarFragment();
                t1fargs.putString("TITLE", "ARTISTS");

                titlebar1Fragment.setArguments(t1fargs);
                ft.replace(R.id.fragment_container_toolbar, titlebar1Fragment);
                ft.addToBackStack(null);
                ft.commit();
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
                Bundle t33fargs = new Bundle();

                TitlebarFragment titlebar33Fragment = new TitlebarFragment();
                t33fargs.putString("TITLE", "PLAYSET");

                titlebar33Fragment.setArguments(t33fargs);
                ft.replace(R.id.fragment_container_toolbar, titlebar33Fragment);
                ft.addToBackStack(null);
                ft.commit();
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
                Bundle t2fargs = new Bundle();

                TitlebarFragment titlebar2Fragment = new TitlebarFragment();
                t2fargs.putString("TITLE", "ALBUMS");

                titlebar2Fragment.setArguments(t2fargs);
                ft.replace(R.id.fragment_container_toolbar, titlebar2Fragment);
                ft.addToBackStack(null);
                ft.commit();
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
                Bundle t3fargs = new Bundle();

                TitlebarFragment titlebar3Fragment = new TitlebarFragment();
                t3fargs.putString("TITLE", "QUEUE");

                titlebar3Fragment.setArguments(t3fargs);
                ft.replace(R.id.fragment_container_toolbar, titlebar3Fragment);
                ft.addToBackStack(null);
                ft.commit();
                break;
        }
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

}
