package uk.arcalder.Kanta;

/**
 * Created by arcalder on 20/03/18.
 */

public class Song {
    // DESCRIPTION:
    // Stores individual song objects containing all relevant metadata
    // Designed to be used with Album, Artist, Genre & Playlist classes


    // MEMBERS:
    private String TITLE_KEY = "";
    private String TITLE = "";
    private String ALBUM_KEY = "";
    private String ALBUM = "";
    private String ARTIST_KEY = "";
    private String ARTIST = "";
    private Long GENRE_ID = -1L;
    private String GENRE = "";
    private int YEAR = -1;
    private int TRACK = -1;
    private Long DURATION = -1L;
    private String DATA = "";
    private boolean isCurrentSongFromQueue = false;

    // CONSTRUCTOR
    public Song(String TITLE_KEY, String TITLE, String ALBUM_KEY, String ALBUM, String ARTIST_KEY, String ARTIST, Long SONG_GENRE_ID, String SONG_GENRE_TITLE, int SONG_YEAR, int SONG_TRACK_NUM, Long SONG_DURATION, String SONG_DATA) {
        this.TITLE_KEY = TITLE_KEY;        // Key associated with song title
        this.TITLE = TITLE;            // Song Title
        this.ALBUM_KEY = ALBUM_KEY;        // Key associated with Album
        this.ALBUM = ALBUM;            // Album Title
        this.ARTIST_KEY = ARTIST_KEY;       // Key associated with Artist
        this.ARTIST = ARTIST;           // Artist Title
        this.GENRE_ID = SONG_GENRE_ID;    // ID associated with Genre
        this.GENRE = SONG_GENRE_TITLE; // Genre Name
        this.YEAR = SONG_YEAR;        // Release Year
        this.TRACK = SONG_TRACK_NUM;   // Track number (from album)
        this.DURATION = SONG_DURATION;    // Duration of song in ms. NOTE: using getDuration will return duration in seconds
        this.DATA = SONG_DATA;        // Path to song
    }

    public String getTITLE_KEY() {
        return TITLE_KEY;
    }

    public String getTITLE() {
        return TITLE;
    }

    public String getALBUM_KEY() {
        return ALBUM_KEY;
    }

    public String getALBUM() {
        return ALBUM;
    }

    public String getARTIST_KEY() {
        return ARTIST_KEY;
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
