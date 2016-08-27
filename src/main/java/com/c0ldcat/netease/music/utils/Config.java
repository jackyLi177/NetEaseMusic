package com.c0ldcat.netease.music.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.CookieStore;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private String configFile;
    private JSONObject configJson;

    public final static String CONFIG_ID = "uid";
    public final static String CONFIG_COOKIE = "cookie";

    public Config(String file) {
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
}
