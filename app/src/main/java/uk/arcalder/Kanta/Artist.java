package uk.arcalder.Kanta;

/**
 * Created by Zynch on 23/03/2018.
 */

public class Artist {
    // DESCRIPTION:
    // Stores individual Artist objects containing all required metadata, other metadata can be fetched from Song
    // Designed to be used with Song

    // MEMBERS:
    private Long ARTIST_ID = -1L;
    private String ARTIST = "";

    // CONSTRUCTOR
    public Artist(Long ARTIST_ID, String ARTIST) {
        this.ARTIST_ID = ARTIST_ID;   // Key associated with artist
        this.ARTIST = ARTIST;       // Name of Artist
    }

    // GETTERS
    public Long getARTIST_ID() {
        return ARTIST_ID;
    }

    public String getARTIST() {
        return ARTIST;
    }
}
