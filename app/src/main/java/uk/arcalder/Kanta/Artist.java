package uk.arcalder.Kanta;

/**
 * Created by Zynch on 23/03/2018.
 */

public class Artist {
    // DESCRIPTION:
    // Stores individual Artist objects containing all required metadata, other metadata can be fetched from Song
    // Designed to be used with Song

    // MEMBERS:
    private String id = "";
    private String name = "";
    private String numOfTracks = "";
    private String numOfAlbums = "";

    // CONSTRUCTOR
    public Artist(String id1, String name1, String numOfTracks1, String numOfAlbums1) {
        this.id = id1;    // ID associated with album
        this.name = name1;       // Name of album
        this.numOfTracks = numOfTracks1;
        this.numOfAlbums = numOfAlbums1;
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

    public String getNumOfTracks() {
        return numOfTracks;
    }

    public void setNumOfTracks(String numOfTracks) {
        this.numOfTracks = numOfTracks;
    }

    public String getNumOfAlbums() {
        return numOfAlbums;
    }

    public void setNumOfAlbums(String numOfAlbums) {
        this.numOfAlbums = numOfAlbums;
    }
}
