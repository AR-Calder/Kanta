package uk.arcalder.Kanta;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by arcalder on 23/03/18.
 */

public class MusicQueue {
    private ArrayList<Long> songQueue = new ArrayList<>(); // stores songQueue allSongs by id

    // Constructor
    public MusicQueue(Context context) {

    }

    public int getLength(){
        return songQueue.size();
    }

    // Get List of Song IDs for the Queue
    public ArrayList<Long> getSongsById() {
        ArrayList<Long> thisQueue = new ArrayList<>();
        thisQueue.addAll(songQueue);
        return thisQueue;
    }

    // Add Song by ID to queue
    public void addSong(@NonNull Long songId) {
        songQueue.add(songId);
    }

    // Add multiple allSongs by IDs to queue
    public void addSongs(@NonNull ArrayList<Long> SongIds) {
        songQueue.addAll(SongIds);
    }

    // Remove song by ID from queue
    public boolean removeSong(Long songId) {
        return songQueue.remove(songId);
    }

    // Remove multiple allSongs by ID list from queue
    public boolean removeSongs(ArrayList<Long> songIds) {
        return songQueue.removeAll(songIds);
    }
}
