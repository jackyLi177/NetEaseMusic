package com.c0ldcat.netease.music;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Playlist extends ArrayList<Song> {
    private String name;
    private int id;
    private NetEaseMusic netEaseMusic;

    private final static String CONFIG_PLAYLIST_ID = "playlist_id";
    private final static String CONFIG_PLAYLIST_NAME = "playlist_name";
    private final static String CONFIG_PLAYLIST_SONG_LIST = "playlist_song_list";

    Playlist(NetEaseMusic netEaseMusic, Config config, JSONObject target) {
        this.netEaseMusic = netEaseMusic;
        jsonUpdate(target, config);
    }

    Playlist(NetEaseMusic netEaseMusic, String name, int id) {
        this.netEaseMusic = netEaseMusic;
        this.name = name;
        this.id = id;
        netEaseMusic.getConfig().addPlaylist(this);
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean cacheAll() throws NoLoginException {
        update();

        boolean b = true;
        for (Song s : this) {
            if (! s.cache()) {
                b = false;
            }
        }
        return b;
    }

    public void update() {
        Utils.log("update" + name);
        //send data
        String data = netEaseMusic.rawHttpRequest(NetEaseMusic.HTTP_METHOD_GET, "http://music.163.com/api/playlist/detail?id=" + getId());

        //request error
        if (data == null) {
            return;
        }

        this.clear();

        //analyze response
        JSONObject jsonData = new JSONObject(data);
        for ( Object o : jsonData.getJSONObject("result").getJSONArray("tracks")) {
            JSONObject jsonSong = (JSONObject) o;

            String name = jsonSong.getString("name");
            int id = jsonSong.getInt("id");

            this.add(new Song(netEaseMusic, name, id));
        }

        netEaseMusic.getConfig().addPlaylist(this);
    }

    JSONObject toJson() {
        JSONObject jsonPlaylist = new JSONObject();
        jsonPlaylist.put(CONFIG_PLAYLIST_ID, getId());
        jsonPlaylist.put(CONFIG_PLAYLIST_NAME, getName());
        JSONArray jsonSongArray = new JSONArray();
        for (Song s : this) {
            jsonSongArray.put(s.getId());
        }
        jsonPlaylist.put(CONFIG_PLAYLIST_SONG_LIST, jsonSongArray);
        return jsonPlaylist;
    }

    private void jsonUpdate(JSONObject target, Config config) {
        this.name = target.getString(CONFIG_PLAYLIST_NAME);
        this.id = target.getInt(CONFIG_PLAYLIST_ID);

        for (Object s : target.getJSONArray(CONFIG_PLAYLIST_SONG_LIST)) {
            int id = (int) s;
            try {
                add(config.getSong(id));
            } catch (ConfigNoFoundException e) {
                //ignore
            }
        }
    }

    static boolean jsonEqual(JSONObject target, int id) {
        try {
            return target.getInt(CONFIG_PLAYLIST_ID) == id;
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" + name + "," + id + "}" + super.toString();
    }
}
