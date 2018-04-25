package uk.arcalder.Kanta;

/**
 * Created by arcalder on 23/03/18.
 */

public class Album {
    // DESCRIPTION:
    // Stores individual Album objects containing all required metadata, other metadata can be fetched from Song
    // Designed to be used with Song

    // MEMBERS:
    private String id = "";
    private String name = "";
    private String artist = "";
    private String albumArt = "";

    // CONSTRUCTOR
    public Album(String album_id, String name, String artist,  String album_art) {
        this.id = album_id;    // ID associated with album
        this.name = name;       // Name of album
        this.artist = artist;
        this.albumArt = album_art;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
    }
}
