package uk.arcalder.Kanta;

import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by arcalder on 20/03/18.
 */

public class Playlist {

    // Unique id and name
    private Long id;
    private String name;

    private ArrayList<Long> songs = new ArrayList<>(); // stores songs by id

    // Constructor
    public Playlist(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Get Playlist ID
    public Long getId() {
        return id;
    }

    // Get Playlist Name
    public String getName() {
        return name;
    }

    // Get List of Song IDs for this playlist
    public ArrayList<Long> getSongsById() {
        // Return a copy of the playlist in case we need to edit it
        ArrayList<Long> thisPlaylist = new ArrayList<>();
        thisPlaylist.addAll(songs);
        return thisPlaylist;
    }

    // Add Song by ID to playlist
    public void addSong(@NonNull Long songId) {
        songs.add(songId);
    }

    // Add multiple songs by IDs to playlist
    public void addSongs(@NonNull ArrayList<Long> SongIds) {
        songs.addAll(SongIds);
    }

    // Remove song by ID from playlist
    public boolean removeSong(Long songId) {
        return songs.remove(id);
    }

    // Remove songs by ID list
    public boolean removeSongs(ArrayList<Long> songIds) {
        return songs.removeAll(songIds);
    }
}
