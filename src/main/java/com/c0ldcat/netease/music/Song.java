package com.c0ldcat.netease.music;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;

public class Song {
    private String name;
    private int id;
    private NetEaseMusic netEaseMusic;

    private String url;

    private final static String CONFIG_SONG_ID = "song_id";
    private final static String CONFIG_SONG_NAME = "song_name";

    private static Log log = LogFactory.getLog(Song.class);

    Song(NetEaseMusic netEaseMusic, JSONObject target) {
        this.netEaseMusic = netEaseMusic;
        jsonUpdate(target);
    }

    Song(NetEaseMusic netEaseMusic, String name, int id) {
        this(netEaseMusic, name, id, null);
    }

    Song(NetEaseMusic netEaseMusic, String name, int id, String url) {
        this.netEaseMusic = netEaseMusic;
        this.name = name;
        this.id = id;
        this.url = url;
        netEaseMusic.getConfig().addSong(this);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isCached() {
        File f = new File(netEaseMusic.getCacheDir().replaceAll("/$", "") + "/" + id + ".mp3");
        return f.exists();
    }

    public boolean cache() throws NoLoginException{
        if (isCached()) return true;

        log.info("try to cache " + name);

        String cacheDir = netEaseMusic.getCacheDir();
        if (cacheDir != null) {
            String url = getUrl();
            File f = new File(netEaseMusic.getCacheDir().replaceAll("/$", "") + "/" + id + ".mp3");
            log.info("downloading");
            try {
                new Downloader().download(url, f);
            } catch (MalformedURLException e) {
                //ignore
            }
            log.info("cached " + name);
            return true;
        } else {
            log.error("no cache dir");
            return false;
        }

    }

    public String getUrl() throws NoLoginException{
        log.debug("try to get url of " + name);

        //get csrf
        String csrf = null;
        for ( Cookie e : netEaseMusic.getCookieStore().getCookies()) {
            if (e.getName().equals("__csrf")) {
                csrf = e.getValue();
            }
        }
        if (csrf == null) throw new NoLoginException();

        String url = "http://music.163.com/weapi/song/enhance/player/url?csrf_token=" + csrf;

        //build data
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonIDs = new JSONArray();
        jsonIDs.put(id);
        jsonObject.put("ids", jsonIDs);
        jsonObject.put("br", 320000);
        jsonObject.put("csrf_token", csrf);
        String data = jsonObject.toString();

        String resp = netEaseMusic.rawHttpRequest(NetEaseMusic.HTTP_METHOD_POST, url, NetEaseMusic.encryptedRequest(data));

        if (resp != null) {
            log.debug("response is " + resp);
            JSONObject jsonSongs = new JSONObject(resp);
            JSONObject jsonSong = (JSONObject) jsonSongs.getJSONArray("data").get(0);
            if (jsonSong.getInt("code") == 200) {
                log.debug("got url");
                return jsonSong.getString("url");
            } else {
                log.debug("got url failed, request failed");
            }
        } else {
            log.error("get url failed, no response");
        }

        return null;
    }

    JSONObject toJson() {
        JSONObject jsonSong = new JSONObject();
        jsonSong.put(CONFIG_SONG_ID, getId());
        jsonSong.put(CONFIG_SONG_NAME, getName());
        return jsonSong;
    }

    private void jsonUpdate(JSONObject target) {
        try {
            name = target.getString(CONFIG_SONG_NAME);
            id = target.getInt(CONFIG_SONG_ID);
        } catch (JSONException e) {
            //ignore
        }
    }

    static boolean jsonEqual(JSONObject target, int id) {
        try {
            return target.getInt(CONFIG_SONG_ID) == id;
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" + name + "," + id + "}";
    }
}
