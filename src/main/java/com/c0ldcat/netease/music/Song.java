package com.c0ldcat.netease.music;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;

public class Song {
    private String name;
    private int id;
    private NetEaseMusic netEaseMusic;

    private BigInteger bDfsID;
    private BigInteger hDfsID;
    private BigInteger mDfsID;
    private BigInteger lDfsID;

    public static final int B_MUSIC = 0;
    public static final int H_MUSIC = 1;
    public static final int M_MUSIC = 2;
    public static final int L_MUSIC = 3;

    final static private String songKey = "3go8&$8*3*3h0k(2)2";

    public Song(NetEaseMusic netEaseMusic, String name, int id, BigInteger bDfsID, BigInteger hDfsID, BigInteger mDfsID, BigInteger lDfsID) {
        this.name = name;
        this.id = id;
        this.bDfsID = bDfsID;
        this.hDfsID = hDfsID;
        this.mDfsID = mDfsID;
        this.lDfsID = lDfsID;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigInteger getBDfsID() {
        return bDfsID;
    }

    public BigInteger getHDfsID() {
        return hDfsID;
    }

    public BigInteger getMDfsID() {
        return mDfsID;
    }

    public BigInteger getLDfsID() {
        return lDfsID;
    }

    public String getUrl(int m) {
        BigInteger dfs;
        switch (m) {
            case B_MUSIC:
                dfs = bDfsID;
                if (dfs != null) break;
            case H_MUSIC:
                dfs = hDfsID;
                if (dfs != null) break;
            case M_MUSIC:
                dfs = mDfsID;
                if (dfs != null) break;
            case L_MUSIC:
                dfs = lDfsID;
                if (dfs != null) break;
            default:
                return null;
        }
        return "http://m2.music.126.net/" + encryptedId(dfs) + "/" + dfs + ".mp3";
    }

    @Override
    public String toString() {
        return "{" + name + "," + id + "," + bDfsID + "}";
    }


    private static String encryptedId(BigInteger id) {
        byte key[] = songKey.getBytes();
        int keyLen = key.length;
        byte idb[] = id.toString().getBytes();

        for (int i = 0 ; i < idb.length ; i++) {
            idb[i] = (byte) (idb[i] ^ key[i % keyLen]);
        }

        String result = new String(Base64.encodeBase64(DigestUtils.md5(idb)));
        result = result.replace('/', '_').replace('+', '-');

        return result;
    }
}
