package com.c0ldcat.netease.music.utils;

import com.c0ldcat.netease.music.NetEaseMusic;
import com.c0ldcat.netease.music.Playlist;
import com.c0ldcat.netease.music.Song;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.CookieStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Config {
    private String configFile;
    private JSONObject configJson;
    private NetEaseMusic netEaseMusic;

    public final static String CONFIG_ID = "uid";
    public final static String CONFIG_COOKIE = "cookie";
    public final static String CONFIG_PLAYLIST_LIST = "playlists";
    public final static String CONFIG_PLAYLIST_ID = "playlist_id";
    public final static String CONFIG_PLAYLIST_NAME = "playlist_name";
    public final static String CONFIG_PLAYLIST_SONG_LIST = "playlist_song_list";
    public final static String CONFIG_SONG_LIST = "songs";
    public final static String CONFIG_SONG_ID = "song_id";
    public final static String CONFIG_SONG_URL = "song_url";
    public final static String CONFIG_SONG_NAME = "song_name";

    public Config(NetEaseMusic netEaseMusic, String file) {
        this.netEaseMusic = netEaseMusic;

        configFile = file;
        try {
            configJson = new JSONObject(Utils.getStringFromInputStream(new FileInputStream(configFile)));
        } catch (FileNotFoundException|JSONException e) {
            configJson = new JSONObject();
        }
    }

    public void save() throws IOException{
        Path file = Paths.get(configFile);
        Files.write(file, configJson.toString().getBytes());
    }

    public int getId() throws ConfigNoFoundException {
        try {
            return configJson.getInt(CONFIG_ID);
        } catch (JSONException e) {
            throw new ConfigNoFoundException(CONFIG_ID);
        }
    }

    public void setId(int id) {
        configJson.put(CONFIG_ID, id);
        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    public CookieStore getCookieStore() throws ConfigNoFoundException {
        try {
            byte[] bin = Base64.decodeBase64(configJson.getString(CONFIG_COOKIE));
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bin));
            return (CookieStore) ois.readObject();
        } catch (JSONException|IOException|ClassNotFoundException e) {
            throw new ConfigNoFoundException(CONFIG_COOKIE);
        }
    }

    public void setCookieStore(CookieStore cookieStore) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(cookieStore);
            oos.close();
            String date = new String(Base64.encodeBase64(baos.toByteArray()));
            configJson.put(CONFIG_COOKIE, date);
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    public void addSong(Song song) {
        //read songs
        JSONArray jsonSongs;
        try {
            jsonSongs = configJson.getJSONArray(CONFIG_SONG_LIST);
        } catch (JSONException e) {
            jsonSongs = new JSONArray();
        }

        //remove same object
        for ( int i = 0 ; i < jsonSongs.length() ; i++ ) {
            JSONObject jsonSong = (JSONObject) jsonSongs.get(i);
            if ( jsonSong.getInt(CONFIG_SONG_ID) == song.getId() ) {
                jsonSongs.remove(i);
                break;
            }
        }

        //build
        JSONObject jsonSong = new JSONObject();
        jsonSong.put(CONFIG_SONG_ID, song.getId());
        jsonSong.put(CONFIG_SONG_NAME,  song.getName());
        jsonSong.put(CONFIG_SONG_URL,  song.getUrl());

        //add to config
        jsonSongs.put(jsonSong);
        configJson.put(CONFIG_SONG_LIST, jsonSongs);

        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    public Song getSong(int id) {
        //read songs
        JSONArray jsonSongs;
        try {
            jsonSongs = configJson.getJSONArray(CONFIG_SONG_LIST);
        } catch (JSONException e) {
            //ignore all
            return null;
        }

        for ( int i = 0 ; i < jsonSongs.length() ; i++ ) {
            JSONObject jsonSong = (JSONObject) jsonSongs.get(i);
            if ( jsonSong.getInt(CONFIG_SONG_ID) == id ) {
                String name = jsonSong.getString(CONFIG_SONG_NAME);

                String url = null;
                try {
                    url = jsonSong.getString(CONFIG_SONG_URL);
                } catch (JSONException e) {
                    //ignore
                }

                return new Song(netEaseMusic, name, id, url);
            }
        }

        return null;
    }

    public ArrayList<Song> getAllSong() throws ConfigNoFoundException {
        ArrayList<Song> list = new ArrayList<>();

        JSONArray jsonSongs;
        try {
            jsonSongs = configJson.getJSONArray(CONFIG_SONG_LIST);
        } catch (JSONException e) {
            throw new ConfigNoFoundException(CONFIG_SONG_LIST);
        }

        for (Object o : jsonSongs) {
            JSONObject jsonSong = (JSONObject) o;
            list.add(getSong(jsonSong.getInt(CONFIG_SONG_ID)));
        }

        return list;
    }

    public void addPlaylist(Playlist playlist) {
        //read playlists
        JSONArray jsonPlaylists;
        try {
            jsonPlaylists = configJson.getJSONArray(CONFIG_PLAYLIST_LIST);
        } catch (JSONException e) {
            jsonPlaylists = new JSONArray();
        }

        //remove same object
        for ( int i = 0 ; i < jsonPlaylists.length() ; i++ ) {
            JSONObject jsonPlaylist = (JSONObject) jsonPlaylists.get(i);
            if ( jsonPlaylist.getInt(CONFIG_PLAYLIST_ID) == playlist.getId() ) {
                jsonPlaylists.remove(i);
                break;
            }
        }

        //build
        JSONObject jsonPlaylist = new JSONObject();
        jsonPlaylist.put(CONFIG_PLAYLIST_ID, playlist.getId());
        jsonPlaylist.put(CONFIG_PLAYLIST_NAME, playlist.getName());
        JSONArray jsonSongArray = new JSONArray();
        for (Song s : playlist) {
            jsonSongArray.put(s.getId());
            addSong(s);
        }
        jsonPlaylist.put(CONFIG_PLAYLIST_SONG_LIST, jsonSongArray);

        //add to config
        jsonPlaylists.put(jsonPlaylist);
        configJson.put(CONFIG_PLAYLIST_LIST, jsonPlaylists);

        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    public ArrayList<Playlist> getPlaylists() throws ConfigNoFoundException {
        ArrayList<Playlist> list = new ArrayList<>();

        JSONArray jsonPlaylists;
        try {
            jsonPlaylists = configJson.getJSONArray(CONFIG_PLAYLIST_LIST);
        } catch (JSONException e) {
            throw new ConfigNoFoundException(CONFIG_PLAYLIST_LIST);
        }

        for (Object o : jsonPlaylists) {
            JSONObject jsonPlaylist = (JSONObject) o;
            Playlist playlist = new Playlist(netEaseMusic, jsonPlaylist.getString(CONFIG_PLAYLIST_NAME), jsonPlaylist.getInt(CONFIG_PLAYLIST_ID));
            for (Object s : jsonPlaylist.getJSONArray(CONFIG_PLAYLIST_SONG_LIST)) {
                int id = (int) s;
                playlist.add(getSong(id));
            }
            list.add(playlist);
        }

        return list;
    }
}
