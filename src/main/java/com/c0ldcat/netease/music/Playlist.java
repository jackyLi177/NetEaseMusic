package com.c0ldcat.netease.music;

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
        this.clear();
        this.addAll(netEaseMusic.digPlayList(this));
    }

    @Override
    public String toString() {
        return "{" + name + "," + id + "}" + super.toString();
    }
}
