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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class BigPlayerActivity extends AppCompatActivity implements
    View.OnClickListener,
    SongListFragment.onSongListFragmentInteractionListener{

        private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

        private static final String TAG = BigPlayerActivity.class.getSimpleName();

        // Music service requirements
        private MediaBrowserCompat mMediaBrowserCompat;
        private MediaControllerCompat mMediaControllerCompat;

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
                updatePlayer();
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

        private int currentPlaybackState = PlaybackStateCompat.STATE_NONE;

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
                        bigPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                        currentPlaybackState = PlaybackStateCompat.STATE_PLAYING;
                        break;
                    }
                    case PlaybackStateCompat.STATE_PAUSED: {
                        Log.d(TAG, "onPlaybackStateChanged to: STATE_PAUSED");
                        bigPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        currentPlaybackState = PlaybackStateCompat.STATE_PAUSED;
                        break;
                    }
                    case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                        Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_NEXT");
                        currentPlaybackState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
                        break;
                    case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                        Log.d(TAG, "onPlaybackStateChanged to: STATE_SKIPPING_TO_PREVIOUS");
                        currentPlaybackState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
                        break;
                    default:
                        finish();
                        return;
                }

                updatePlayer();
            }

            @Override
            public void onSessionDestroyed() {
                Log.d(TAG, "onSessionDestroyed");
                super.onSessionDestroyed();
            }
        };

        ImageButton bigPlayPause,
                    bigNext,
                    bigPrev;

        TextView    bigSongTitle,
                    bigSongArtistAlbum;

        ImageView   bigAlbumArt;


        public void updatePlayer(){
            try {
            bigSongTitle.setText(MusicLibrary.getInstance().getCurrentSong().getTitle());
            bigSongArtistAlbum.setText(String.format("%s / %s", MusicLibrary.getInstance().getCurrentSong().getArtist(), MusicLibrary.getInstance().getCurrentSong().getAlbum()));
            } catch (Exception e){}
            try {
                File image = new File(MusicLibrary.getInstance().getCurrentSong().getArt());
                Picasso.get().load(image).fit().centerCrop().into(bigAlbumArt);
            } catch (Exception e){
                Picasso.get().load(R.drawable.default_album).fit().centerCrop().into(bigAlbumArt);
            }

            if (mMediaControllerCompat.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                bigPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }


        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "onCreate Called");

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_big_player);

            bigPlayPause    = (ImageButton) findViewById(R.id.imageButtonBigPlayPause);
            bigNext         = (ImageButton) findViewById(R.id.imageButtonBigNext);
            bigPrev         = (ImageButton) findViewById(R.id.imageButtonBigPrev);

            bigPlayPause.setOnClickListener(this);
            bigNext.setOnClickListener(this);
            bigPrev.setOnClickListener(this);

            bigSongTitle        = (TextView) findViewById(R.id.textViewBigSongTitle);
            bigSongArtistAlbum  = (TextView) findViewById(R.id.textViewBigSongArtistAlbum);

            bigAlbumArt         = (ImageView)findViewById(R.id.imageViewBigAlbumArt);


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

            // --------------------------Get Permissions if no permissions--------------------------

            Log.d(TAG, "onCreate: get Permissions");
            getPermission(Manifest.permission.WAKE_LOCK, 234);
            getPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 123);

            //mMusicLibrary.initLibrary(getApplicationContext());

            // --------------------------Load Fragments---------------------------------------------


            // --------------------------Connect to Music Player Service--------------------------
            mMediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MusicPlayerService.class),
                    mMediaBrowserCompatConnectionCallback, getIntent().getExtras());



            Log.d(TAG, "onCreate: connectionCallback supposedly set");

        }

    // --------------------------Handle Permissions "Elegantly"-------------------------------------

    public void getPermission(String Permission, int requestCode){

        Log.i(TAG, "getPermission: Check if have permissions: " + "READ_EXTERNAL_STORAGE");
        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Permission)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "getPermission: Permission not granted");
            // Permission is not granted so set false for now
            MusicLibrary.getInstance().setHasPermission(false);

            // but request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Permission},
                    requestCode); // Once again shite documentation

            // The callback method gets the result of the request.
            Log.i(TAG, "getPermission: Requesting Permission");

        } else {
            // Permission has already been granted
            MusicLibrary.getInstance().setHasPermission(true);
        }
    }

    public void PermissionBlock(){

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

        } else {

            // --------------------------NO PERMISSIONS------------------------------

            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            MusicLibrary.getInstance().setHasPermission(false);
            finish();
        }


        // other 'case' lines to check for other
        // permissions this app might request.
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
//        initButtons((currentPlaybackState == PlaybackStateCompat.STATE_PLAYING),
//                (currentPlaybackState != PlaybackStateCompat.STATE_NONE &&
//                        currentPlaybackState != PlaybackStateCompat.STATE_STOPPED));
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(mMediaControllerCompatCallback);
        }
        mMediaBrowserCompat.disconnect();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick Called");
        switch (view.getId()){
            case R.id.imageButtonBigPlayPause:
            //has to be dealt with accordingly, based on the current state of mediaplayer
            if( mMediaControllerCompat.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED ) {
                mMediaControllerCompat.getTransportControls().play();
            }
            else if( mMediaControllerCompat.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ) {
                mMediaControllerCompat.getTransportControls().pause();
            }
            break;

            case R.id.imageButtonBigNext:
                mMediaControllerCompat.getTransportControls().skipToNext();
            break;

            case R.id.imageButtonBigPrev:
                mMediaControllerCompat.getTransportControls().skipToPrevious();
            break;

        //space for future cases here
        //
        //
    }
    }

    @Override
    public void PlayQueueSong() {
        Log.d(TAG, "PlayQueueSong Called");
    }

    @Override
    public void playSongFromPlaysetIndex(int position) {
        Log.d(TAG, "playSongFromPlaysetIndex Called");
    }

    @Override
    public void playSong() {
        Log.d(TAG, "playSong Called");
    }
}
