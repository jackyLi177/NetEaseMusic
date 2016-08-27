package com.c0ldcat.netease.music;

import com.c0ldcat.netease.music.utils.NoLoginException;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;

public class Song {
    private String name;
    private int id;
    private NetEaseMusic netEaseMusic;

    final static private String songKey = "3go8&$8*3*3h0k(2)2";

    private String url;

    public Song(NetEaseMusic netEaseMusic, int id) {
        this(netEaseMusic, null, id);
    }

    public Song(NetEaseMusic netEaseMusic, String name, int id) {
        this(netEaseMusic, name, id, null);
    }

    public Song(NetEaseMusic netEaseMusic, String name, int id, String url) {
        this.netEaseMusic = netEaseMusic;
        this.name = name;
        this.id = id;
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void update() throws NoLoginException{
        //get csrf"
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
            JSONObject jsonSongs = new JSONObject(resp);
            JSONObject jsonSong = (JSONObject) jsonSongs.getJSONArray("data").get(0);
            if (jsonSong.getInt("code") == 200) {
                this.url = jsonSong.getString("url");
            }
        }

        netEaseMusic.getConfig().addSong(this);
    }

    @Override
    public String toString() {
        return "{" + name + "," + id + "}";
    }
}
