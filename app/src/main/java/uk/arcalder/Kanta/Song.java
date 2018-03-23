package uk.arcalder.Kanta;

/**
 * Created by arcalder on 20/03/18.
 */

// Stores song object with unique ID and path
public class Song {

    // Unique path and id
    private Long id;
    private String url;

    // Song metadata
    private String title = "";
    private String artist = "";
    private String album = "";
    private String genre = "";
    private int year = -1;
    private int trackNum = -1; // Initialized to -1 to prevent confusion about track 0
    private long duration_ms = -1;

    // Constructor
    public Song(Long id, String url) {
        this.id = id;
        this.url = url;
    }

    // Alt Constructor
    public Song(Long id, String url, String title, String artist, String Album, String genre, int year, int trackNum, long duration_ms) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.year = year;
        this.trackNum = trackNum;
        this.duration_ms = duration_ms;
    }

    // URL/ID Getters - These should not change during running of application
    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    // Metadata Getters - These may change during running of application
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    // Setters

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTrackNum() {
        return trackNum;
    }

    public void setTrackNum(int trackNum) {
        this.trackNum = trackNum;
    }

    public long getDuration_ms() {
        return duration_ms;
    }

    public void setDuration_ms(long duration_ms) {
        this.duration_ms = duration_ms;
    }

    //Since duration in ms is fucking useless
    public long getDurationSeconds() {
        return duration_ms / 1000;
    }
}
