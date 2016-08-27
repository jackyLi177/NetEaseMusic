package com.c0ldcat.netease.music.utils;

public class ConfigNoFoundException extends Exception{
    private String key;

    public ConfigNoFoundException (String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getMessage() {
        return key + " no found";
    }
}
