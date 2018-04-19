package uk.arcalder.Kanta;

public class Song {

    private long id;
    private String filePath;

    public Song(long id, String filePath) {
        this.id        = id;
        this.filePath  = filePath;
    }


    // optional metadata

    private String title       = "";
    private String artist      = "";
    private String album       = "";
    private int    year        = -1;
    private String genre       = "";
    private int    track_num    = -1;
    private long   duration = -1;

    // ------------------------------Getters--------------------------------
    //Unique Identifier for this song
    public long getId() {
        return id;
    }

    // Full filepath to song
    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public int getYear() {
        return year;
    }

    public String getGenre() {
        return genre;
    }

    public int getTrackNumber() {
        return track_num;
    }

    public long getDuration() {
        return duration;
    }

    // ------------------------------Setters--------------------------------
    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setTrackNumber(int track_no) {
        this.track_num = track_no;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

}