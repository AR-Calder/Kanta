package uk.arcalder.Kanta;

/**
 * Created by arcalder on 23/03/18.
 */

public class Album {
    // DESCRIPTION:
    // Stores individual Album objects containing all required metadata, other metadata can be fetched from Song
    // Designed to be used with Song

    // MEMBERS:
    private String ALBUM_KEY = "";
    private String ALBUM = "";

    // CONSTRUCTOR
    public Album(String ALBUM_KEY, String ALBUM) {
        this.ALBUM_KEY = ALBUM_KEY;    // Key associated with album
        this.ALBUM = ALBUM;        // Name of album
    }

    // GETTERS
    public String getALBUM_KEY() {
        return ALBUM_KEY;
    }

    public String getALBUM() {
        return ALBUM;
    }
}
