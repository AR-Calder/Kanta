package uk.arcalder.Kanta;

public class Song{

    // hard reqs
    private String id           = "";
    private String data         = "";

    //  metadata
    private String title        = "";
    private String artist       = "";
    private String artist_id    = "";
    private String album        = "";
    private String album_id     = "";
    private String art          = "";
    private String year         = "";
    private String genre        = "";
    private String genre_id     = "";

    public Song(String id, String data, String title, String artist, String artist_id, String album, String album_id) {
        this.id    = id;
        this.data  = data;
        this.title = title;
        this.artist = artist;
        this.artist_id = artist_id;
        this.album = album;
        this.album_id = album_id;
    }

    // ------------------------------Getters--------------------------------
    //Unique Identifier for this song
    public String getId() {
        return id;
    }
    // Song
    public String getData() {
        return data;
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

    public String getYear() {
        return year;
    }

    public String getGenre() {
        return genre;
    }

    public String getArt() {
        return art;
    }

    public String getArtist_id() {
        return artist_id;
    }

    public String getAlbum_id() {
        return album_id;
    }

    public String getGenre_id() {
        return genre_id;
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

    public void setAlbumId(String album_id) {
        this.album_id = album_id;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setTrackNumber(String art) {
        this.art = art;
    }

    public void setArt(String art) {
        this.art = art;
    }

    public void setArtist_id(String artist_id) {
        this.artist_id = artist_id;
    }

    public void setAlbum_id(String album_id) {
        this.album_id = album_id;
    }

    public void setGenre_id(String genre_id) {
        this.genre_id = genre_id;
    }
}