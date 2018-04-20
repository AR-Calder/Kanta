package uk.arcalder.Kanta;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.provider.MediaStore;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Zynch on 20/04/2018.
 */
// Song List singleton
public class SongList {
    private static final String TAG = SongList.class.getSimpleName();

    public static ArrayList<Song>   songs;
    private static ArrayList<Song>  songQueue;
    public static ArrayList<Song>   playSet;

    public static boolean loading   = false;
    public static boolean loaded    = false;

    private static int current_position = -1;

    private static class AsyncInitSongs extends AsyncTask<Cursor, Song, ArrayList<Song>>{
        @Override
        protected ArrayList<Song> doInBackground(Cursor... cursors) {

            ArrayList<Song> asyncsongs = new ArrayList<>();

            if(null != cursors[0]){
                Cursor songCursor = cursors[0];
                try {
                    for (songCursor.moveToFirst(); !songCursor.isAfterLast(); songCursor.moveToNext()) {
                        String data     = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                        String id       = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                        String title    = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        String artist   = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        String album    = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        String album_id = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                        asyncsongs.add(new Song(id, data, title, artist, album, album_id));
                    }
                    Log.d(TAG, "AsyncInitSongs: loaded = true");
                    songCursor.close();
                    return asyncsongs;

                } catch (Exception e){
                    Log.d(TAG, "AsyncInitSongs", e);
                    songCursor.close();
                    return null;
                }

                // TODO get ALBUMS, ALBUM ART, GENRES, PLAYLISTS



            }
            return null;
        }
    }

    //Init big songs
    public void initSongs(Context context){
        Log.d(TAG, "initSongs");
        if (loaded || loading) return;

        ContentResolver mContentResolver = context.getContentResolver();

        Uri uri                     = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection_is_music   = MediaStore.Audio.Media.IS_MUSIC + "=1";
        String sort_order           = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        String[] song_columns       = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor songCursor = mContentResolver.query(uri,null, selection_is_music, null, sort_order);
        new AsyncInitSongs(){
            @Override
            protected void onPostExecute(ArrayList<Song> asyncsongs){
                if (null != asyncsongs) {
                    setSongs(asyncsongs);
                    loaded = true;
                    loading = false;
                }
            }
        }.execute(songCursor);
        loading = true;


    }

    // Get all songs
    public ArrayList<Song> getSongs(){
        Log.d(TAG, "getSongs");
        return songs;
    }

    // set all songs
    public void setSongs(ArrayList<Song> songs){
        Log.d(TAG, "setSongs");
        this.songs = songs;
    }

    // get all songs in playSet
    public ArrayList<Song> getplaySet(){
        Log.d(TAG, "getplaySet");
        return playSet;
    }

    // set the playSet
    public void setPlaySet(ArrayList<Song> songs){
        Log.d(TAG, "setPlaySet");
        this.playSet = songs;
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

    public static final SongList mSongList = new SongList();

    public static SongList getInstance() {
        Log.d(TAG, "getInstance");
        return mSongList;
    }
}
