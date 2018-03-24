package uk.arcalder.Kanta;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by arcalder on 23/03/2018.
 */

public class AudioProvider {
    /*REQUIRES:
    * SOURCE        STRING{INTERNAL || EXTERNAL}        COMMON{EXTERNAL}        Which type of storage to search for content
    * DEBUG         BOOLEAN{true || false}              COMMON{FALSE}           Enables or disables debugging messages*/

    // DESIGN:
    // All functions should be self contained
    // Limits should be handled by callee, not this class!

    // TODO:
    // Remove TODOs

    private ContentResolver contentResolver;

    private Uri genreUri,
            playlistUri;

    // CONSTRUCTOR ARGS
    private String SOURCE;


    // MEMBERS
    private Boolean DEBUG;


    // CONSTRUCTOR
    public AudioProvider(Context context, String SOURCE, Boolean DEBUG) {
        this.contentResolver = context.getContentResolver();
        this.SOURCE = SOURCE;
        this.DEBUG = DEBUG;
        this.genreUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
        this.playlistUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

    }


    // -----------------------------------------------------------------SONGS-------------------------------------------------------------------------

    /*CONTAINS:

    * SONG CLASS:
    *   SONG_TITLE_KEY
    *   SONG_TITLE
    *   SONG_ALBUM_KEY
    *   SONG_ALBUM_TITLE
    *   SONG_ARTIST_KEY
    *   SONG_ARTIST_TITLE
    *   SONG_GENRE_ID       // Unlike everything else, GENRE only had ID, not KEY
    *   SONG_GENRE_TITLE
    *   SONG_YEAR
    *   SONG_TRACK_NUM
    *   SONG_DURATION
    *   SONG_DATA
    * */

    // COLUMN HELPER
    private String[] songColumns = {
            "1", // TODO Requires further testing
    // See Song.class for member explanation
    MediaStore.Audio.Genres.Members.TITLE_KEY,
    MediaStore.Audio.Genres.Members.TITLE,
    MediaStore.Audio.Genres.Members.ALBUM_KEY,
    MediaStore.Audio.Genres.Members.ALBUM,
    MediaStore.Audio.Genres.Members.ARTIST_KEY,
    MediaStore.Audio.Genres.Members.ARTIST,
    MediaStore.Audio.Genres.Members.GENRE_ID,
    // Can't get genre name this way
    MediaStore.Audio.Genres.Members.YEAR,
    MediaStore.Audio.Genres.Members.TRACK,
    MediaStore.Audio.Genres.Members.DURATION,
    MediaStore.Audio.Genres.Members.DATA
};

    // QUERY HELPER
    private ArrayList<Song> songQuery(int start, int end, @NonNull String songSELECTION) {
        // container
        ArrayList<Song> songList = new ArrayList<>();

        // Query
        try (Cursor songCursor = this.contentResolver.query(genreUri, songColumns, songSELECTION, null, MediaStore.Audio.Genres.Members.TITLE)) {
            for (songCursor.moveToPosition(start); !songCursor.isAfterLast() && start != end; songCursor.moveToNext(), start++) {
                Song song = new Song(
                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.TITLE_KEY)),
                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.TITLE)),
                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_KEY)),
                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)),
                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_KEY)),
                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST)),
                songCursor.getLong(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.GENRE_ID)),
                songCursor.getString(1), // GENRE NAME TODO Requires further testing
                songCursor.getInt(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.YEAR)),
                songCursor.getInt(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.TRACK)),
                songCursor.getLong(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.DURATION)),
                songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Genres.Members.DATA))
                );
                songList.add(song);
            }
        }
        return songList;
    }

    // TODO GET ALL SONGS
    public ArrayList<Song> getAllSongs() {
        ArrayList<Song> songList = new ArrayList<>();

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1";

        // By setting end to -1 it will loop until all songs have been found
        return songQuery(0, -1, songSELECTION);
    }

    // TODO GET N SONGS (FROM INDEX TO INDEX)
    public ArrayList<Song> getNSongs(int start, int end) {
        ArrayList<Song> songList = new ArrayList<>();

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1";

        // By setting end to -1 it will loop until all songs have been found
        return songQuery(start, end, songSELECTION);
    }

    // TODO GET SONG BY KEY TODO
    public Song getSongByKey(String key) {

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1 and " + MediaStore.Audio.Genres.Members.TITLE_KEY + "=" + key;

        // Only interested in 1 result
        return songQuery(0, 1, songSELECTION).get(0);
    }

    // TODO GET SONGS BY ARTIST KEY
    public ArrayList<Song> getSongsByArtistKey(String key) {

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1 and " + MediaStore.Audio.Genres.Members.ARTIST_KEY + "=" + key;

        // By setting end to -1 it will instead loop until all songs have been found
        return songQuery(0, -1, songSELECTION);
    }

    // TODO GET SONGS BY ALBUM KEY
    public ArrayList<Song> getSongsByAlbumKey(String key) {

        // Only accept music files
        String songSELECTION = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1 and " + MediaStore.Audio.Genres.Members.ALBUM_KEY + "=" + key;

        // By setting end to -1 it will instead loop until all songs have been found
        return songQuery(0, -1, songSELECTION);
    }

    // TODO GET SONGS BY GENRE ID
    public ArrayList<Song> getSongsByGenreId(Long id) {

        // Only accept music files and match id
        String songSELECTION = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1 and " + MediaStore.Audio.Genres.Members.GENRE_ID + "=" + id;

        // By setting end to -1 it will instead loop until all songs have been found
        return songQuery(0, -1, songSELECTION);
    }

    // TODO GET SONGS BY PLAYLIST
    public ArrayList<Song> getSongsByPlaylist(Long id) {
        // Slightly different as need to get info from playlist query

        // Match playlist ID and only accept audio files
        String playlistSELECTION = MediaStore.Audio.Playlists._ID + "=" + id;
        String songSELECTION = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1";

        // By setting end to -1 it will instead loop until all songs have been found
        return playlistSongsQuery(0, -1, playlistSELECTION, songSELECTION);


    }

    // TODO GET SONGS BY DECADE
    public ArrayList<Song> getSongsByDecade(int year) {

        int decade_lower = (int) Math.floor(year / 10d) * 10;
        int decade_upper = decade_lower + 9;


        // Only accept music files and match id
        String songSELECTION = MediaStore.Audio.Genres.Members.YEAR + " >= " + decade_lower + " and " + MediaStore.Audio.Genres.Members.YEAR + "<=" + decade_upper +
        " and " + MediaStore.Audio.Genres.Members.IS_MUSIC + "=1";

        // By setting end to -1 it will loop until all songs have been found
        return songQuery(0, -1, songSELECTION);
    }

    // -----------------------------------------------------------------ALBUMS------------------------------------------------------------------------

    /*CONTAINS:

    * ALBUM CLASS:
    *   ALBUM_KEY
    *   ALBUM_TITLE
    *
    * */

    // COLUMN HELPER
    private String[] albumColumns = {
    // See Song.class for member explanation
    MediaStore.Audio.Genres.Members.ALBUM_KEY,
    MediaStore.Audio.Genres.Members.ALBUM,
    MediaStore.Audio.Genres.Members.YEAR, // TODO CHECK THAT HERE below WORKS AS INTENDED
    MediaStore.Audio.Genres.Members.ARTIST_KEY
};

    // QUERY HELPER
    private ArrayList<Album> albumQuery(int start, int end, @NonNull String albumSELECTION) {
        // container
        ArrayList<Album> albumList = new ArrayList<>();

        // Query
        try (Cursor albumCursor = this.contentResolver.query(genreUri, albumColumns, albumSELECTION, null, MediaStore.Audio.Genres.Members.ALBUM)) {
            for (albumCursor.moveToPosition(start); !albumCursor.isAfterLast() && start != end; albumCursor.moveToNext(), start++) {
                Album album = new Album(
                albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_KEY)),
                albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM))
                );
                albumList.add(album);
            }
        }
        return albumList;
    }

    // TODO GET ALL ALBUMS
    public ArrayList<Album> getAllAlbums() {

        // No conditions, want everything
        String albumSELECTION = "";

        // By setting end to -1 it will loop until all results have been found
        return albumQuery(0, -1, albumSELECTION);
    }

    // TODO GET N ALBUMS
    public ArrayList<Album> getNAlbums(int start, int end) {

        // No conditions, want everything
        String albumSELECTION = "";

        // By setting end to -1 it will loop until all results have been found
        return albumQuery(start, -end, albumSELECTION);
    }

    // TODO GET ALBUM BY KEY
    public Album getAlbumByKey(String key) {

        // Only accept albums that match Album key
        String albumSELECTION = MediaStore.Audio.Genres.Members.ALBUM_KEY + "=" + key;

        // Only interested in 1 result
        return albumQuery(0, 1, albumSELECTION).get(0);
    }

    // TODO GET ALBUMS BY ARTIST KEY
    public Album getAlbumByArtistKey(String key) {

        // Only accept albums that match artist key
        String albumSELECTION = MediaStore.Audio.Genres.Members.ARTIST_KEY + "=" + key;

        // Only interested in 1 result
        return albumQuery(0, 1, albumSELECTION).get(0);
    }

    // TODO GET ALBUMS BY DECADE
    public ArrayList<Album> getAlbumByDecade(int year) {

        // Calculate decade range
        int decade_lower = (int) Math.floor(year / 10d) * 10;
        int decade_upper = decade_lower + 9;

        // Only accept albums that match decade
        String albumSELECTION = MediaStore.Audio.Genres.Members.YEAR + " >= " + decade_lower + " and " + MediaStore.Audio.Genres.Members.YEAR + "<=" + decade_upper;

        // By setting end to -1 it will loop until all results have been found
        return albumQuery(0, -1, albumSELECTION);
    }

    // -----------------------------------------------------------------ARTIST-----------------------------------------------------------------------

    /*CONTAINS:

    * ARTIST CLASS:
    *   ARTIST_KEY
    *   ARTIST_TITLE
    *
    * */

    // COLUMN HELPER
    private String[] artistColumns = {
    // See Song.class for member explanation
    MediaStore.Audio.Genres.Members.ARTIST_KEY,
    MediaStore.Audio.Genres.Members.ARTIST,
};

    // QUERY HELPER
    private ArrayList<Artist> artistQuery(int start, int end, @NonNull String albumSELECTION) {
        // container
        ArrayList<Artist> artists = new ArrayList<>();

        // Query
        try (Cursor artistCursor = this.contentResolver.query(genreUri, artistColumns, albumSELECTION, null, MediaStore.Audio.Genres.Members.ALBUM)) {
            for (artistCursor.moveToPosition(start); !artistCursor.isAfterLast() && start != end; artistCursor.moveToNext(), start++) {
                Artist artist = new Artist(
                artistCursor.getString(artistCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_KEY)),
                artistCursor.getString(artistCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST))
                );
                artists.add(artist);
            }
        }
        return artists;
    }

    // TODO GET ALL ARTISTS
    public ArrayList<Artist> getAllArtists() {

        // No conditions, want everything
        String artistSELECTION = "";

        // By setting end to -1 it will loop until all results have been found
        return artistQuery(0, -1, artistSELECTION);
    }

    // TODO GET N ARTISTS
    public ArrayList<Artist> getNArtists(int start, int end) {

        // No conditions, want everything
        String artistSELECTION = "";

        // Fetch end minus start results
        return artistQuery(start, end, artistSELECTION);
    }

    // TODO GET ARTIST BY KEY
    public Artist getArtistByKey(String key) {
        // Only accept albums that match artist key
        String artistSELECTION = MediaStore.Audio.Genres.Members.ARTIST_KEY + "=" + key;

        // Only interested in 1 result
        return artistQuery(0, 1, artistSELECTION).get(0);
    }

    // -----------------------------------------------------------------GENRE-------------------------------------------------------------------------

    /*CONTAINS:

    * GENRE CLASS:
    *   GENRE_ID
    *   GENRE_TITLE
    *
    * */

    // COLUMN HELPER
    private String[] genreColumns = {
    // See Song.class for member explanation
    MediaStore.Audio.Genres.Members.GENRE_ID,
            "1" //GENRE NAME? TODO REQUIRES FURTHER TESTING
};

    // QUERY HELPER
    private ArrayList<Genre> genreQuery(int start, int end, @NonNull String genreSELECTION) {
        // container
        ArrayList<Genre> genres = new ArrayList<>();

        // Query
        try (Cursor genreCursor = this.contentResolver.query(genreUri, genreColumns, genreSELECTION, null, MediaStore.Audio.Genres.Members.GENRE_ID)) {
            for (genreCursor.moveToPosition(start); !genreCursor.isAfterLast() && start != end; genreCursor.moveToNext(), start++) {
                Genre genre = new Genre(
                genreCursor.getLong(genreCursor.getColumnIndex(MediaStore.Audio.Genres.Members.GENRE_ID)),
                genreCursor.getString(1)
                );
                genres.add(genre);
            }
        }
        return genres;
    }

    // TODO GET ALL GENRES
    public ArrayList<Genre> getAllGenres() {

        // No conditions, want everything
        String genreSELECTION = "";

        // By setting end to -1 it will loop until all results have been found
        return genreQuery(0, -1, genreSELECTION);
    }


    // TODO GET GENRE BY ID
    public Genre getGenreById(Long id) {
        // Only accept genres that match genre ID
        String genreSELECTION = MediaStore.Audio.Genres.Members.GENRE_ID + "=" + id;

        // Only interested in 1 result
        return genreQuery(0, 1, genreSELECTION).get(0);
    }


    // ---------------------------------------------------------------PLAYLISTS-----------------------------------------------------------------------


    // COLUMN HELPER
    private String[] playlistColumns = {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
    };

    // QUERY HELPER - FOR PLAYLIST ONLY
    private ArrayList<Playlist> playlistQuery(int start, int end, @NonNull String playlistSELECTION) {
        // Container
        ArrayList<Playlist> playlists = new ArrayList<>();

        //Query
        try (Cursor playlistCursor = this.contentResolver.query(playlistUri, playlistColumns, playlistSELECTION, null, MediaStore.Audio.Genres.Members.TITLE)) {
            for (playlistCursor.moveToPosition(start); !playlistCursor.isAfterLast() && start != end; playlistCursor.moveToNext(), start++) {
                Playlist thisPlaylist = new Playlist(
                        playlistCursor.getLong(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                        playlistCursor.getString(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))
                );


                // add to playlist list
                playlists.add(thisPlaylist);
            }
        }
        // Returns playlists matching query
        return playlists;
    }

    // QUERY HELPER - FOR SONGS IN PLAYLIST
    private ArrayList<Song> playlistSongsQuery(int start, int end, @NonNull String playlistSELECTION, @NonNull String songSELECTION) {
        // containers
        ArrayList<Song> songs = new ArrayList<>();

        // If selection returns multiple items I will still only use first item
        Playlist playlist = playlistQuery(start, end, playlistSELECTION).get(0);

        // QUERY
        // Location of songs within the playlist
        Uri thisPlaylistUri = MediaStore.Audio.Playlists.Members.getContentUri(SOURCE, playlist.getId());
        // I apologize for this block of code but the variables are only being used once; I don't feel the need to split them up.
        try (Cursor thisPlaylistCursor = contentResolver.query(thisPlaylistUri, new String[]{MediaStore.Audio.Playlists.Members.TITLE_KEY}, MediaStore.Audio.Media.IS_MUSIC + "=1", null, MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER)) {
            while (thisPlaylistCursor.moveToNext()) {
                // A true test of stacked queries
                songs.add(getSongByKey(thisPlaylistCursor.getString(thisPlaylistCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE_KEY))));
            }
        }
        // Returns all songs in playlist matching query
        return songs;
    }

// TODO GET ALL PLAYLISTS

// TODO GET N PLAYLISTS

// TODO GET PLAYLIST BY ID

}

