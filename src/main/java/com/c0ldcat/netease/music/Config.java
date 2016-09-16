package com.c0ldcat.netease.music;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.CookieStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class Config {
    private String configFile;
    private JSONObject configJson;
    private NetEaseMusic netEaseMusic;

    private final static String CONFIG_ID = "uid";
    private final static String CONFIG_COOKIE = "cookie";
    private final static String CONFIG_PLAYLIST_LIST = "playlists";
    private final static String CONFIG_SONG_LIST = "songs";
    private final static String CONFIG_CACHE_DIR = "cache_dir";

    private static Log log = LogFactory.getLog(Config.class);

    Config(NetEaseMusic netEaseMusic, String file) {
        log.debug("loading config from " + file);
        this.netEaseMusic = netEaseMusic;
        configFile = file;

        try {
            configJson = new JSONObject(Utils.getStringFromInputStream(new FileInputStream(configFile)));
        } catch (FileNotFoundException|JSONException e) {
            log.warn("load config failed, create new one");
            configJson = new JSONObject();
        }

        log.debug("loaded config");
    }

    void save() throws IOException{
        log.debug("saving config to " + configFile);
        Path file = Paths.get(configFile);
        Files.write(file, configJson.toString().getBytes());
        log.debug("saved config");
    }

    int getId() throws ConfigNoFoundException {
        try {
            return configJson.getInt(CONFIG_ID);
        } catch (JSONException e) {
            throw new ConfigNoFoundException(CONFIG_ID);
        }
    }

    void setId(int id) {
        configJson.put(CONFIG_ID, id);
        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    CookieStore getCookieStore() throws ConfigNoFoundException {
        try {
            byte[] bin = Base64.decodeBase64(configJson.getString(CONFIG_COOKIE));
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bin));
            return (CookieStore) ois.readObject();
        } catch (JSONException|IOException|ClassNotFoundException e) {
            throw new ConfigNoFoundException(CONFIG_COOKIE);
        }
    }

    void setCookieStore(CookieStore cookieStore) {
        log.debug("set cookie store");
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

    void addSong(Song song) {
        log.debug("add song " + song.getName());

        //read songs
        JSONArray jsonSongs;

        try {
            jsonSongs = getJsonSongs();
        } catch (ConfigNoFoundException e) {
            jsonSongs = new JSONArray();
        }

        //remove same object
        for ( int i = 0 ; i < jsonSongs.length() ; i++ ) {
            JSONObject jsonSong = (JSONObject) jsonSongs.get(i);
            if ( Song.jsonEqual(jsonSong, song.getId()) ) {
                jsonSongs.remove(i);
                break;
            }
        }

        //add to config
        jsonSongs.put(song.toJson());
        configJson.put(CONFIG_SONG_LIST, jsonSongs);

        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    void addSong(List<Song> playlist) {
        for (Song s : playlist) {
            addSong(s);
        }
    }

    Song getSong(int id) throws ConfigNoFoundException{
        JSONArray jsonSongs = getJsonSongs();

        for ( int i = 0 ; i < jsonSongs.length() ; i++ ) {
            JSONObject jsonSong = (JSONObject) jsonSongs.get(i);
            if ( Song.jsonEqual(jsonSong, id) ) {
                return new Song(netEaseMusic, jsonSong);
            }
        }

        throw new ConfigNoFoundException("" + id);
    }

    public ArrayList<Song> getAllSong() throws ConfigNoFoundException {
        ArrayList<Song> list = new ArrayList<>();

        JSONArray jsonSongs = getJsonSongs();

        for (Object o : jsonSongs) {
            list.add(new Song(netEaseMusic, (JSONObject) o));
        }

        return list;
    }

    private JSONArray getJsonSongs() throws ConfigNoFoundException {
        try {
            return configJson.getJSONArray(CONFIG_SONG_LIST);
        } catch (JSONException e) {
            throw new ConfigNoFoundException(CONFIG_SONG_LIST);
        }
    }

    void addPlaylist(Playlist playlist) {
        log.debug("add playlist " + playlist.getName());

        //read playlists
        JSONArray jsonPlaylists;
        try {
            jsonPlaylists = getJsonPlaylists();
        } catch (ConfigNoFoundException e) {
            jsonPlaylists = new JSONArray();
        }

        //remove same object
        for ( int i = 0 ; i < jsonPlaylists.length() ; i++ ) {
            JSONObject jsonPlaylist = (JSONObject) jsonPlaylists.get(i);
            if ( Playlist.jsonEqual(jsonPlaylist, playlist.getId()) ) {
                jsonPlaylists.remove(i);
                break;
            }
        }

        //add to config
        jsonPlaylists.put(playlist.toJson());
        configJson.put(CONFIG_PLAYLIST_LIST, jsonPlaylists);

        //add song to config
        addSong(playlist);

        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    Playlist getPlaylist(int id) throws ConfigNoFoundException {
        JSONArray jsonPlaylists = getJsonPlaylists();

        for ( int i = 0 ; i < jsonPlaylists.length() ; i++ ) {
            JSONObject jsonPlaylist = (JSONObject) jsonPlaylists.get(i);
            if ( Playlist.jsonEqual(jsonPlaylist, id) ) {
                return new Playlist(netEaseMusic, this, jsonPlaylist);
            }
        }

        throw new ConfigNoFoundException("" + id);
    }

    void removeAllPlaylists() {
        log.debug("remove all playlist in config");

        try {
            configJson.remove(CONFIG_PLAYLIST_LIST);
        } catch (JSONException e) {
            //ignore
        }
    }

    ArrayList<Playlist> getPlaylists() throws ConfigNoFoundException {
        ArrayList<Playlist> list = new ArrayList<>();

        JSONArray jsonPlaylists = getJsonPlaylists();

        for (Object o : jsonPlaylists) {
            list.add(new Playlist(netEaseMusic, this, (JSONObject) o));
        }

        return list;
    }

    private JSONArray getJsonPlaylists() throws ConfigNoFoundException {
        try {
            return configJson.getJSONArray(CONFIG_PLAYLIST_LIST);
        } catch (JSONException e) {
            throw new ConfigNoFoundException(CONFIG_PLAYLIST_LIST);
        }
    }

    String getCacheDir() {
        try {
            return configJson.getString(CONFIG_CACHE_DIR);
        } catch (JSONException e) {
            return null;
        }
    }

    void setCacheDir(String s) {
        log.debug("set new cache dir " + s);

        configJson.put(CONFIG_CACHE_DIR, s);

        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }
}
