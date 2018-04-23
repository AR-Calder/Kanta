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

import java.util.ArrayList;

public class BigPlayerActivity extends AppCompatActivity implements
    View.OnClickListener,
    SongListFragment.onSongListFragmentInteractionListener{

        private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

        private static final String TAG = BigPlayerActivity.class.getSimpleName();

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
                            new MediaControllerCompat(BigPlayerActivity.this, // context
                                    token);

                    // Save the controller
                    MediaControllerCompat.setMediaController(BigPlayerActivity.this,
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
                        break;
                    }
                    case PlaybackStateCompat.STATE_PAUSED: {
                        Log.d(TAG, "onPlaybackStateChanged to: STATE_PLAYING");
                        break;
                    }
                    case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                        Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_NEXT");
                        break;
                    case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                        Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_PREVIOUS");
                        break;
                    default:
                        Log.d(TAG, "onPlaybackStateChanged to: *STATE_NOT_CARE_ABOUT");
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
            setContentView(R.layout.activity_big_player);



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

            Log.d(TAG, "onCreate: get Permissions");
            getPermission(Manifest.permission.WAKE_LOCK);
            getPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            //mMusicLibrary.initLibrary(getApplicationContext());

            // --------------------------Load Fragments---------------------------------------------

            // Load default fragments
            Log.d(TAG, "onCreate: Load default fragments");
            loadFragments();


            // --------------------------Connect to Music Player Service--------------------------
            Log.d(TAG, "onCreate: connect to service");
            mMediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MusicPlayerService.class),
                    mMediaBrowserCompatConnectionCallback, getIntent().getExtras());

        }



    public void getPermission(String Permission){

        Log.i(TAG, "getPermission: Check if have permissions: " + "READ_EXTERNAL_STORAGE");
        // Request permissions
        if (ContextCompat.checkSelfPermission(BigPlayerActivity.this, Permission)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "getPermission: Permission not granted");
            // Permission is not granted so,
            // request the permission
            ActivityCompat.requestPermissions(BigPlayerActivity.this,
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
        if (MediaControllerCompat.getMediaController(BigPlayerActivity.this) != null) {
            MediaControllerCompat.getMediaController(BigPlayerActivity.this).unregisterCallback(mMediaControllerCompatCallback);
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

    // Toolbar / search bar fragments
    private boolean loadFragments() {
        Log.d(TAG, "loadFragments: Trying to load fragments");
        // IF Fragment already exists
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_big_player_art, new BigPlayerArtFragment())
                .replace(R.id.fragment_container_big_player_controls, new BigPlayerControlsFragment())
                .commit();

        return true;
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "onStop Called");
    }

    @Override
    public void playSongFromPlaysetIndex(int position) {
        Log.d(TAG, "onStop Called");
    }

    @Override
    public void playSong() {
        Log.d(TAG, "onStop Called");
    }
}
