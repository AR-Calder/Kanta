package uk.arcalder.Kanta;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by arcalder on 20/03/18.
 */

public class MusicProvider {
    private ArrayList<Song> songs;

    private ArrayList<Playlist> playlists;

    private HashMap<String, String> GENRE_ID_TO_GENRE_NAME;

    private HashMap<String, String> SONG_ID_TO_GENRE_NAME;

    // TODO!! ALBUM_ID_TO_SONG_IDS (MULTIMAP)

    private Boolean isSongsImported = false; // have songs been imported
    private Boolean isBusyImporting = false; // are songs currently being imported

    // getters
    public Boolean getBoolSongsImported() {
        return isSongsImported;
    }

    // setters
    private void setBoolSongsImported(boolean status) {
        isSongsImported = status;
    }

    public Boolean getBoolBusyImporting() {
        return isBusyImporting;
    }

    private void setBoolBusyImporting(boolean status) {
        isBusyImporting = status;
    }

    // ~# IMPORT SONGS #~
    public void importSongs(Context context, @NonNull String source) {
        // TODO Import N songs


        // Where to scan for files (for each)
        Uri audioUri;
        Uri genreUri;
        Uri playlistUri;

        // Double check I'm not already trying to import songs
        if (getBoolBusyImporting()) {
            return;
        }
        isBusyImporting = true;

        if (source.equals("INTERNAL")) {
            audioUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
            genreUri = MediaStore.Audio.Genres.INTERNAL_CONTENT_URI;
            playlistUri = MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI;
        } else { // TODO invert this so it defaults to internal if source is invalid
            audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            genreUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
            playlistUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        }

        // Used to make file system queries
        // REFER TO https://www.androiddesignpatterns.com/2012/06/content-resolvers-and-content-providers.html
        ContentResolver contentResolver = context.getContentResolver();

        // Used to provide read-write access to results returned by fs query
        // https://developer.android.com/reference/android/database/Cursor.html
        //Cursor cursor;

        // For getting Metadata from database
        // Song
        String SONG_ID = android.provider.MediaStore.Audio.Media._ID;
        String SONG_TITLE = android.provider.MediaStore.Audio.Media.TITLE;
        String SONG_ARTIST = android.provider.MediaStore.Audio.Media.ARTIST;
        String SONG_ALBUM = android.provider.MediaStore.Audio.Media.ALBUM;
        String SONG_YEAR = android.provider.MediaStore.Audio.Media.YEAR;
        String SONG_URL = android.provider.MediaStore.Audio.Media.DATA;
        String SONG_TRACK_NUM = android.provider.MediaStore.Audio.Media.TRACK;
        String SONG_DURATION = android.provider.MediaStore.Audio.Media.DURATION;
        // Genre
        String GENRE_ID = MediaStore.Audio.Genres._ID;
        String GENRE_NAME = MediaStore.Audio.Genres.NAME;
        // Playlist
        String PLAYLIST_ID = MediaStore.Audio.Playlists._ID;
        String PLAYLIST_NAME = MediaStore.Audio.Playlists.NAME;
        String PLAYLIST_SONG_ID = MediaStore.Audio.Playlists.Members.AUDIO_ID;

        // Create genreId:genreName HashMap
        // Genres (apparently) by default return an ID3v1 value which is pretty worthless unless we know what they are
        // stackoverflow suggestion was to map genre id to genre name
        // https://stackoverflow.com/questions/32337949/how-to-get-songs-and-other-media-from-an-album-id/35746126#35746126

        GENRE_ID_TO_GENRE_NAME = new HashMap<String, String>();
        SONG_ID_TO_GENRE_NAME = new HashMap<String, String>();
        // TODO!! ALBUM_ID_TO_SONG_IDS (MULTIMAP)

        // Columns for genre query
        String[] genreQueryColumns = {GENRE_ID, GENRE_NAME};

        // Actually querying the genres database
        // Iterating through the results and filling the map.
        try (Cursor cursor = contentResolver.query(genreUri, genreQueryColumns, null, null, null)) {
            while (cursor.moveToNext()) {
                GENRE_ID_TO_GENRE_NAME.put(cursor.getString(0), cursor.getString(1));
            }
            // Shouldn't need to close when using the try with resources statement - https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
            //cursor.close();
            // If doesn't work move cursor back up top and split up the query and result extraction
            // Based on https://stackoverflow.com/questions/10723770/whats-the-best-way-to-iterate-an-android-cursor
        } catch (Exception e) {
            Toast.makeText(context, "Error importing genre IDs: " + e, Toast.LENGTH_SHORT).show();
        }

        // Do the same to map songs to genres
        for (String genreID : GENRE_ID_TO_GENRE_NAME.keySet()) {
            Uri SourceUri = MediaStore.Audio.Genres.Members.getContentUri(source, Long.parseLong(genreID));

            try (Cursor cursor = contentResolver.query(SourceUri, new String[]{SONG_ID}, null, null, null)) {
                while (cursor.moveToNext()) {
                    SONG_ID_TO_GENRE_NAME.put(Long.toString(cursor.getLong(cursor.getColumnIndex(SONG_ID))), genreID);
                }
                // Once again shouldn't need to close thx to "try with res"
            } catch (Exception e) {
                Toast.makeText(context, "Error importing genres names: " + e, Toast.LENGTH_SHORT).show();
            }

        }

        // Columns for song query
        String[] songQueryColumns = {SONG_ID, SONG_TITLE, SONG_ARTIST, SONG_ALBUM, SONG_YEAR, SONG_URL, SONG_TRACK_NUM, SONG_DURATION};

        // This took fucking ages to find https://developer.android.com/reference/android/provider/MediaStore.Audio.AudioColumns.html#IS_MUSIC
        // Add this to query to select only music files "MediaStore.Audio.Media.IS_MUSIC = 1"
        try (Cursor cursor = contentResolver.query(audioUri, songQueryColumns, MediaStore.Audio.Media.IS_MUSIC + "=1", null, null)) {
            while (cursor.moveToNext()) {
                // Import song metadata
                Song thisSong = new Song(
                        cursor.getLong(cursor.getColumnIndex(SONG_ID)),
                        cursor.getString(cursor.getColumnIndex(SONG_TITLE)),
                        cursor.getString(cursor.getColumnIndex(SONG_ARTIST)),
                        cursor.getString(cursor.getColumnIndex(SONG_ARTIST)),
                        cursor.getString(cursor.getColumnIndex(SONG_ARTIST)),
                        cursor.getString(cursor.getColumnIndex(SONG_ARTIST)),
                        cursor.getInt(cursor.getColumnIndex(SONG_YEAR)),
                        cursor.getInt(cursor.getColumnIndex(SONG_TRACK_NUM)),
                        cursor.getLong(cursor.getColumnIndex(SONG_DURATION))
                );

                // Have to set genre separately as need to know song ID in advance
                String thisGenreId = SONG_ID_TO_GENRE_NAME.get(thisSong.getId().toString());
                String thisGenreName = GENRE_ID_TO_GENRE_NAME.get(thisGenreId);
                thisSong.setGenre(thisGenreName);

                // Add to song list
                songs.add(thisSong);
            }
            // Once again shouldn't need to close thx to "try with res"
        } catch (Exception e) {
            Toast.makeText(context, "Error importing songs: " + e, Toast.LENGTH_SHORT).show();
        }

        // Columns for playlists query
        String[] playlistQueryColumns = {PLAYLIST_ID, PLAYLIST_NAME};

        try (Cursor playlistsCursor = contentResolver.query(playlistUri, playlistQueryColumns, null, null, null)) {
            while (playlistsCursor.moveToNext()) {
                Playlist thisPlaylist = new Playlist(
                        playlistsCursor.getLong(playlistsCursor.getColumnIndex(PLAYLIST_ID)),
                        playlistsCursor.getString(playlistsCursor.getColumnIndex(PLAYLIST_NAME))
                );

                // https://developer.android.com/reference/android/provider/MediaStore.Audio.Playlists.Members.html
                // playlist members NOT just playlist!

                // get location of playlist members
                Uri thisPlaylistUri = MediaStore.Audio.Playlists.Members.getContentUri(source, thisPlaylist.getId());

                // Add associated song IDs to this playlist
                try (Cursor thisPlaylistCursor = contentResolver.query(thisPlaylistUri, new String[]{PLAYLIST_SONG_ID}, MediaStore.Audio.Media.IS_MUSIC + "=1", null, null)) {
                    while (thisPlaylistCursor.moveToNext()) {
                        thisPlaylist.addSong(thisPlaylistCursor.getLong(thisPlaylistCursor.getColumnIndex(PLAYLIST_SONG_ID)));
                    }
                }

                // add to playlist list
                playlists.add(thisPlaylist);

            }
        }

        // Import is finished (no longer importing and songs should have now been imported)
        setBoolSongsImported(true);
        setBoolBusyImporting(false);

    }

    // ~# GET ALL SONGS #~
    public ArrayList<Song> getAllSongs() {
        ArrayList<Song> songList = new ArrayList<Song>();
        songList.addAll(songs);
        return songList;
    }

    // ~# GET SONGS BY ARTIST #~
    public ArrayList<Song> getSongsByArtist(String artistName) {
        ArrayList<Song> songList = new ArrayList<Song>();
        for (Song song : songs) {
            if (song.getArtist().equals(artistName)) {
                songList.add(song);
            }
        }
        return songList;
    }

    // ~# GET SONGS BY ALBUM #~
    public ArrayList<Song> getSongsByAlbum(String ablumName) {
        ArrayList<Song> songList = new ArrayList<Song>();
        for (Song song : songs) {
            if (song.getAlbum().equals(ablumName)) {
                songList.add(song);
            }
        }
        return songList;
    }

    // ~# GET SONGS BY GENRE #~
    public ArrayList<Song> getSongsByGenre(String genreName) {
        ArrayList<Song> songList = new ArrayList<Song>();
        for (Song song : songs) {
            if (song.getGenre().equals(genreName)) {
                songList.add(song);
            }
        }
        return songList;
    }

    // TODO get albums(by artist, song, genre), genres, artist(by song, album)
}
