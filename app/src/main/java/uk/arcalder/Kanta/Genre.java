package uk.arcalder.Kanta;

/**
 * Created by Zynch on 23/03/2018.
 */

public class Genre {
    // DESCRIPTION:
    // Stores individual Genre objects containing all required metadata, other metadata can be fetched from Song
    // Designed to be used with Song

    // MEMBERS:
    private Long GENRE_ID = -1L;
    private String GENRE = "";

    // CONSTRUCTORS
    public Genre(Long GENRE_ID, String GENRE) {
        this.GENRE_ID = GENRE_ID;     // ID assigned to Genre
        this.GENRE = GENRE;        // Name associated with ID
    }

    // GETTERS
    public Long getGENRE_ID() {
        return GENRE_ID;
    }

    public String getGENRE() {
        return GENRE;
    }
}
