package uk.arcalder.Kanta;

/**
 * Created by Zynch on 23/03/2018.
 */

public class Artist {
    // DESCRIPTION:
    // Stores individual Artist objects containing all required metadata, other metadata can be fetched from Song
    // Designed to be used with Song

    // MEMBERS:
    private String ARTIST_KEY = "";
    private String ARTIST = "";

    // CONSTRUCTOR
    public Artist(String ARTIST_KEY, String ARTIST) {
        this.ARTIST_KEY = ARTIST_KEY;   // Key associated with artist
        this.ARTIST = ARTIST;       // Name of Artist
    }

    // GETTERS
    public String getARTIST_KEY() {
        return ARTIST_KEY;
    }

    public String getARTIST() {
        return ARTIST;
    }
}
