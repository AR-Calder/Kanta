package uk.arcalder.Kanta;

/**
 * Created by arcalder on 20/03/18.
 */

public class Playlist {

    // Unique id and name
    private Long id;
    private String name;

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
}
