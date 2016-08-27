package com.c0ldcat.netease.music.utils;

public class NoLoginException extends Exception {
    @Override
    public String getMessage() {
        return "No login";
    }
}
