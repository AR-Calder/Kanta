package uk.arcalder.Kanta;

/**
 * Created by arcalder on 20/03/18.
 */

public class Song {
    // DESCRIPTION:
    // Stores individual song objects containing all relevant metadata
    // Designed to be used with Album, Artist, Genre & Playlist classes


    // MEMBERS:
    private Long TITLE_ID = -1L;
    private String TITLE = "";
    private Long ALBUM_ID = -1L;
    private String ALBUM = "";
    private Long ARTIST_ID = -1L;
    private String ARTIST = "";
    private Long GENRE_ID = -1L;
    private String GENRE = "";
    private int YEAR = -1;
    private int TRACK = -1;
    private Long DURATION = -1L;
    private String DATA = "";
    private boolean isCurrentSongFromQueue = false;

    // CONSTRUCTOR
    public Song(Long TITLE_ID, String TITLE, Long ALBUM_ID, String ALBUM, Long ARTIST_ID, String ARTIST, Long SONG_GENRE_ID, String SONG_GENRE_TITLE, int SONG_YEAR, int SONG_TRACK_NUM, Long SONG_DURATION, String SONG_DATA) {
        this.TITLE_ID = TITLE_ID;        // ID associated with song title
        this.TITLE = TITLE;            // Song Title
        this.ALBUM_ID = ALBUM_ID;        // ID associated with Album
        this.ALBUM = ALBUM;            // Album Title
        this.ARTIST_ID = ARTIST_ID;       // ID associated with Artist
        this.ARTIST = ARTIST;           // Artist Title
        this.GENRE_ID = SONG_GENRE_ID;    // ID associated with Genre
        this.GENRE = SONG_GENRE_TITLE; // Genre Name
        this.YEAR = SONG_YEAR;        // Release Year
        this.TRACK = SONG_TRACK_NUM;   // Track number (from album)
        this.DURATION = SONG_DURATION;    // Duration of song in ms. NOTE: using getDuration will return duration in seconds
        this.DATA = SONG_DATA;        // Path to song
    }

    public Long getTITLE_ID() {
        return TITLE_ID;
    }

    public String getTITLE() {
        return TITLE;
    }

    public Long getALBUM_ID() {
        return ALBUM_ID;
    }

    public String getALBUM() {
        return ALBUM;
    }

    public Long getARTIST_ID() {
        return ARTIST_ID;
    }

    public String getARTIST() {
        return ARTIST;
    }

    public Long getGENRE_ID() {
        return GENRE_ID;
    }

    public String getGENRE() {
        return GENRE;
    }

    public int getYEAR() {
        return YEAR;
    }

    public int getTRACK() {
        return TRACK;
    }

    public Long getDURATION() {
        // Return duration in seconds as ms isn't very useful
        return DURATION / 1000;
    }

    public String getDATA() {
        return DATA;
    }

    public boolean getIsCurrentSongFromQueue() {
        return isCurrentSongFromQueue;
    }

    public void setIsCurrentSongFromQueue(boolean currentSongFromQueue) {
        isCurrentSongFromQueue = currentSongFromQueue;
    }
}
