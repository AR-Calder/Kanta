package uk.arcalder.Kanta;

import java.util.ArrayList;

/**
 * Created by arcalder on 23/03/18.
 */

public class Album {

    // Unique id and name
    private Long id;
    private String name;
    private ArrayList<Long> songs = new ArrayList<>(); // stores songs by id

    // Constructors
    public Album(Long id, String name){
        this.id = id;
        this.name = name;
    }

    public void setAlbumSongs(ArrayList<Long> ids){
        this.songs = ids;
    }

    // Get Album ID
    public Long getId() {
        return id;
    }

    // Get Album Name
    public String getName() {
        return name;
    }

    // Get List of Song IDs for this Album
    public ArrayList<Long> getSongsById() {
        // Return a copy of the playlist in case we need to edit it
        ArrayList<Long> thisAlbum = new ArrayList<>();
        thisAlbum.addAll(songs);
        return thisAlbum;
    }
}
