package com.c0ldcat.netease.music;

import com.c0ldcat.netease.music.utils.NoLoginException;
import org.json.JSONObject;
import java.util.ArrayList;

public class Playlist extends ArrayList<Song> {
    private String name;
    private int id;
    private NetEaseMusic netEaseMusic;

    public Playlist(NetEaseMusic netEaseMusic, String name, int id) {
        this.netEaseMusic = netEaseMusic;
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void update() {
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

    public void updateAll() {
        update();
        for (Song s : this) {
            try {
                s.update();
            } catch (NoLoginException e) {
                //ignore
            }
        }
    }

    @Override
    public String toString() {
        return "{" + name + "," + id + "}" + super.toString();
    }
}
