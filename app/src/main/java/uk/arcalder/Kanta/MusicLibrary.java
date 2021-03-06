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

    public Song currentSong;

    // Song trackers
    private ArrayList<Song>  songQueue = new ArrayList<>();
    public  ArrayList<Song>   playSet = new ArrayList<>();

    public boolean hasPermission = true;

    private int current_position;

    public int getCurrent_position() {
        return current_position;
    }

    public void setCurrent_position(int position) {
        current_position = position;
        setCurrentSong(getSongByIndexFromSongs(current_position));

    }

    // TODO !!-- Move ALL this stuff into individual fragments --!!
    // use the runnable/handler method and just save the PlaySet/Queue/currentSong/CurrentPlaySetIndex here.
    // Thought I could get off without notifysetchanged but no...


    public void setHasPermission(boolean permissionState){
        hasPermission = permissionState;

        if (!permissionState) {
            Log.d(TAG, "noPermissions: blocking initLibrary");
        } else {
            Log.d(TAG, "hasPermissions:");
        }
    }

    public boolean hasPermission() {
        return hasPermission;
    }

    // -------------------------Songs------------------------------
    // Get all allSongs
    public ArrayList<Song> getSongs(){
        Log.d(TAG, "getSongs");
        return playSet;
    }

    // set all allSongs
    public void setSongs(ArrayList<Song> songs){
        Log.d(TAG, "setSongs");
        this.playSet = songs;
    }

    // get current song
    public Song getCurrentSong() {
        Log.d(TAG, "getCurrentSong from position");
        return currentSong;
        //return (!playSet.isEmpty()) ? playSet.get(current_position) : null;
    }

    // set current song
    public void setCurrentSong(Song song) {
        currentSong = song;
    }

    // get song by index
    public Song getSongByIndexFromSongs(int index){
        return playSet.get(index);
    }

    public int getSizeOfSongs(){
        return playSet.size();
    }

    public Song getPreviousSong(){
        if (!playSet.isEmpty()){
            // There are actually songs to play

            if (0 < current_position && current_position < playSet.size()){
                // There are songs prior to this one
                return playSet.get(--current_position);

            } else {
                // This is the earliest item in the playset
                current_position = 0;
                return playSet.get(current_position);
            }
        }
        // There is nothing to play
        return currentSong;
    }

    public Song getNextSong(){
        Song rtn = null;
        if (!songQueue.isEmpty()){
            // There are actually songs to play in the Queue

            Log.d(TAG, "getNextSong: getting queue song");
            rtn = getNextQueueSong();

        } else if (!playSet.isEmpty()){
            // There are actually songs to play in the playSet

            if (-1 < current_position && current_position + 1 < playSet.size()) {

                // There are songs after this one
                Log.d(TAG, "getNextSong: getting playSet song");
                rtn = playSet.get(++current_position);
            }
            Log.d(TAG, "getNextSong: NO MORE SONGS");
        }

        // TODO TEST 25/04/18
        setCurrentSong(rtn);

        // There is nothing to play
        return rtn;
    }

    // get next queued song
    public Song getNextQueueSong(){
        Log.d(TAG, "getNextQueueSong");
        return songQueue.remove(0);
    }

    public ArrayList<Song> getSongQueue() {
        return songQueue;
    }

    // add a song to the queue
    public void addSongToQueue(Song song){
        Log.d(TAG, "addSongToQueue");
        if (null != song){
            songQueue.add(song);
        }
    }

    // remove a song from the queue
    public void removeSongFromQueue(int position){
        try {
            songQueue.remove(position);
        }catch (IndexOutOfBoundsException iobe){
            Log.e(TAG, "Song at position (" + position +") was out of bounds");
        }
    }

    // clear queue
    public void clearQueue(){
        Log.d(TAG, "clearQueue");
        if (!songQueue.isEmpty()){
            songQueue.clear();
        }
    }

    private static final MusicLibrary M_MUSIC_LIBRARY = new MusicLibrary();

    public static MusicLibrary getInstance() {
        Log.d(TAG, "getInstance");
        return M_MUSIC_LIBRARY;
    }
}
