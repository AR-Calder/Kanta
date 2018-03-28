package uk.arcalder.Kanta;

/**
 * Created by arcalder on 23/03/18.
 */

public class Album {
    // DESCRIPTION:
    // Stores individual Album objects containing all required metadata, other metadata can be fetched from Song
    // Designed to be used with Song

    // MEMBERS:
    private Long ALBUM_ID = -1L;
    private String ALBUM = "";

    // CONSTRUCTOR
    public Album(Long ALBUM_ID, String ALBUM) {
        this.ALBUM_ID = ALBUM_ID;    // Key associated with album
        this.ALBUM = ALBUM;        // Name of album
    }

    // GETTERS
    public Long getALBUM_ID() {
        return ALBUM_ID;
    }

    public String getALBUM() {
        return ALBUM;
    }
}
