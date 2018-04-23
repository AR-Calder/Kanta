package uk.arcalder.Kanta;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zynch on 20/04/2018.
 */
// Song List singleton
public class MusicLibrary {
    private static final String TAG = MusicLibrary.class.getSimpleName();

    public static Song currentSong;

    // Song trackers
    public static ArrayList<Song>   allSongs = new ArrayList<>();
    private static ArrayList<Song>  viewSongs = new ArrayList<>();
    private static ArrayList<Song>  songQueue = new ArrayList<>();
    public static ArrayList<Song>   playSet = new ArrayList<>();

    // Artist trackers
    private ArrayList<Artist> viewArtists = new ArrayList<>();
    private ArrayList<Artist> allArtists = new ArrayList<>();

    // Album trackers
    private ArrayList<Album> viewAlbums = new ArrayList<>();
    private ArrayList<Album> allAlbums = new ArrayList<>();

    public Song getCurrentSong() {
        return playSet.get(current_position);
    }

    public void setCurrentSong(Song song) {
        currentSong = song;
    }

    public static boolean loading   = false;
    public static boolean loaded    = false;
    public static boolean hasPermission = true;

    private static int current_position;

    public int getCurrent_position() {
        return current_position;
    }

    public void setCurrent_position(int position) {
        current_position = position;
    }

    // TODO !!-- Move ALL this stuff into individual fragments --!!
    // use the runnable/handler method and just save the PlaySet/Queue/currentSong/CurrentPlaySetIndex here.
    // Thought I could get off without notifysetchanged but no...


    private static AsyncInitSongs initSongs;
    private static class AsyncInitSongs extends AsyncTask<Cursor, Song, ArrayList<Song>>{

        @Override
        protected ArrayList<Song> doInBackground(Cursor... cursors) {
            Log.d(TAG, "AsyncInitSongs: doInBackground");
            ArrayList<Song> asyncsongs = new ArrayList<>();

            if(null != cursors[0]){
                Cursor songCursor = cursors[0];
                try {
                    for (songCursor.moveToFirst(); !songCursor.isAfterLast(); songCursor.moveToNext()) {
                        String data     = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                        String id       = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                        String title    = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        String artist   = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        String artist_id= songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
                        String album    = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        String album_id = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                        asyncsongs.add(new Song(id, data, title, artist, artist_id, album, album_id));
                    }
                    Log.d(TAG, "AsyncInitSongs: loaded = true");
                    songCursor.close();
                    return asyncsongs;

                } catch (Exception e){
                    Log.d(TAG, "AsyncInitSongs", e);
                    songCursor.close();
                    return null;
                }
            }
            return null;
        }
    }

    private static AsyncInitAlbums initAlbums;
    private static class AsyncInitAlbums extends AsyncTask<Cursor, Album, ArrayList<Album>>{

        @Override
        protected ArrayList<Album> doInBackground(Cursor... cursors) {
            Log.d(TAG, "AsyncInitAlbums: doInBackground");
            ArrayList<Album> asyncAlbums = new ArrayList<>();

            if(null != cursors[0]){
                Cursor songCursor = cursors[0];
                try {
                    for (songCursor.moveToFirst(); !songCursor.isAfterLast(); songCursor.moveToNext()) {
                        String ID           = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                        String ALBUM        = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                        String ARTIST       = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                        String ALBUM_ART    = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                        asyncAlbums.add(new Album(ID, ALBUM, ARTIST, ALBUM_ART));
                    }
                    Log.d(TAG, "AsyncInitAlbums: loaded = true");
                    songCursor.close();
                    return asyncAlbums;

                } catch (Exception e){
                    Log.d(TAG, "AsyncInitAlbums", e);
                    songCursor.close();
                    return null;
                }
            }
            return null;
        }
    }

    private static AsyncInitArtists initArtists;
    private static class AsyncInitArtists extends AsyncTask<Cursor, Artist, ArrayList<Artist>>{

        @Override
        protected ArrayList<Artist> doInBackground(Cursor... cursors) {
            Log.d(TAG, "AsyncInitAlbums: doInBackground");
            ArrayList<Artist> asyncArtists = new ArrayList<>();

            if(null != cursors[0]){
                Cursor songCursor = cursors[0];
                try {
                    for (songCursor.moveToFirst(); !songCursor.isAfterLast(); songCursor.moveToNext()) {
//                      MediaStore.Audio.Artists._ID
//                      MediaStore.Audio.Artists.ARTIST
//                      MediaStore.Audio.Artists.NUMBER_OF_TRACKS
//                      MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
                        String id       = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                        String artist   = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                        String tracks   = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
                        String albums   = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
                        asyncArtists.add(new Artist(id, artist, tracks, albums));
                    }
                    Log.d(TAG, "AsyncInitAlbums: loaded = true");
                    songCursor.close();
                    return asyncArtists;

                } catch (Exception e){
                    Log.d(TAG, "AsyncInitAlbums", e);
                    songCursor.close();
                    return null;
                }
            }
            return null;
        }
    }

    //Init big allSongs
    public void initLibrary(Context context){
        Log.d(TAG, "Shouldn't be calling from " +context.toString());
    }

    //Init big allSongs
    public void initLibrary2(Context context){
        Log.d(TAG, "initSongs");
        if (!hasPermission || loaded) return;

        ContentResolver mContentResolver = context.getContentResolver();

        Uri uri                     = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection_is_music   = MediaStore.Audio.Media.IS_MUSIC + "=1";
        String sort_order           = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        // ------------Songs-----------------
        final String[] song_columns       = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        Cursor songCursor = mContentResolver.query(uri,null, selection_is_music, null, sort_order);
        initSongs = new AsyncInitSongs(){
            @Override
            protected void onPostExecute(ArrayList<Song> asyncsongs){
                if (null != asyncsongs) {
                    setSongs(asyncsongs);
                    setPlaySet(asyncsongs);
                    setViewSongs(asyncsongs);
                }
            }
        };
        initSongs.execute(songCursor);

        // ------------Albums-----------------
        final String[] album_columns    = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ART
        };

        Cursor albumCursor = mContentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
        );

        initAlbums = new AsyncInitAlbums(){
            @Override
            protected void onPostExecute(ArrayList<Album> asyncAlbums){
                if (null != asyncAlbums) {
                    setAllAlbums(asyncAlbums);
                }
            }
        };
        initAlbums.execute(albumCursor);

        // ------------Artists-----------------
        final String[] artist_columns    = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        };

        Cursor artistCursor = mContentResolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
        );

        initArtists = new AsyncInitArtists(){
            @Override
            protected void onPostExecute(ArrayList<Artist> asyncartists) {
                Log.d(TAG,"AsyncInitArtists: onPostExecute");
                if (null != asyncartists){
                    setAllArtists(asyncartists);
                }
            }
        };
        loading = false;
        loaded = true;
    }

    public void setHasPermission(boolean permissionState, Context context){
        hasPermission = permissionState;

        if (!permissionState) {
            Log.d(TAG, "noPermissions: blocking initLibrary");
        } else {
            Log.d(TAG, "hasPermissions:");
        }
    }

    // ------------------------STATE-------------------------------
    public static class currentState{
        public final int    TYPE_SONGS      = 0,
                            TYPE_ALBUMS     = 1,
                            TYPE_ARTISTS    = 2;

        private int content_type;
    }

    //--------------------------Artists-----------------------------

    public ArrayList<Artist> getViewArtists() {
        Log.d(TAG, "getViewArtists");
        return viewArtists;
    }

    public void setViewArtists(ArrayList<Artist> viewArtists) {
        Log.d(TAG, "setViewArtists");
        this.viewArtists = viewArtists;
    }

    public ArrayList<Artist> getAllArtists() {
        Log.d(TAG, "getAllArtists");
        return allArtists;
    }

    public void setAllArtists(ArrayList<Artist> allArtists) {
        Log.d(TAG, "setAllArtists");
        this.allArtists = allArtists;
    }


    //--------------------------Albums-----------------------------


    public ArrayList<Album> getAllAlbums() {
        Log.d(TAG, "getAllAlbums");
        return allAlbums;
    }

    public void setAllAlbums(ArrayList<Album> setAlbums){
        Log.d(TAG, "setAllAlbums");
        allAlbums = setAlbums;
    }

    public ArrayList<Album> getViewAlbums() {
        Log.d(TAG, "getViewAlbums");
        return viewAlbums;
    }

    public void setViewAlbums(ArrayList<Album> viewAlbums) {
        Log.d(TAG, "setViewAlbums");
        this.viewAlbums = viewAlbums;
    }

    // -------------------------Songs------------------------------
    // Get all allSongs
    public ArrayList<Song> getSongs(){
        Log.d(TAG, "getSongs");
        return allSongs;
    }

    // set all allSongs
    public void setSongs(ArrayList<Song> songs){
        Log.d(TAG, "setSongs");
        this.allSongs = songs;
    }

    public ArrayList<Song> getViewSongs() {
        return viewSongs;
    }

    public void setViewSongs(ArrayList<Song> viewSongs) {
        MusicLibrary.viewSongs = viewSongs;
    }

    // get song by index
    public Song getSongByIndexFromSongs(int index){
        return null;                                    // TODO Remove
    }

    public int getSizeOfSongs(){
        return allSongs.size();
    }

    public Song getNextSongFromAny(){
        if (!songQueue.isEmpty()){
            Log.d(TAG, "getCurrent: getting queue song");
            return getNextQueueSong();
        } else if (!playSet.isEmpty()){
            if (current_position != -1 && current_position < playSet.size()) {
                Log.d(TAG, "getCurrent: getting playSet song");
                return playSet.get(current_position);
            }
            Log.d(TAG, "getCurrent: getting playSet song");

        } else if (!allSongs.isEmpty()){
            // get a random song
            Log.d(TAG, "getCurrent: getting random song");
            allSongs.get(ThreadLocalRandom.current().nextInt(0, allSongs.size()));
        }

        // Literally nothing
        return null;
    }


    // get all allSongs in playSet
    public ArrayList<Song> getplaySet(){
        Log.d(TAG, "getplaySet");
        return playSet;
    }

    // set the playSet
    public void setPlaySet(ArrayList<Song> songs){
        Log.d(TAG, "setPlaySet");
        this.playSet = songs;
    }

    // get song by index
    public Song getSongByIndexFromPlaySet(int index){
            return playSet.get(index);
    }

    public int getSizeOfPlaySet(){
        return playSet.size();
    }

    // get next queued song
    public Song getNextQueueSong(){
        Log.d(TAG, "getNextQueueSong");
        if (!songQueue.isEmpty()) {
            return songQueue.remove(0);
        }
        return null;
    }

    // add a song to the queue
    public void addSongToQueue(Song song){
        Log.d(TAG, "addSongToQueue");
        if (null != song){
            songQueue.add(song);
        }
    }

    // clear queue
    public void clearQueue(){
        Log.d(TAG, "clearQueue");
        if (!songQueue.isEmpty()){
            songQueue.clear();
        }
    }

    public static final MusicLibrary M_MUSIC_LIBRARY = new MusicLibrary();

    public static MusicLibrary getInstance() {
        Log.d(TAG, "getInstance");
        return M_MUSIC_LIBRARY;
    }
}
