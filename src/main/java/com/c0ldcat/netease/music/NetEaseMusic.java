package com.c0ldcat.netease.music;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;

public class NetEaseMusic {
    //key
    final static private String modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7" +
            "b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280" +
            "104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932" +
            "575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b" +
            "3ece0462db0a22b8e7";
    final static private String nonce = "0CoJUm6Qyw8W8jud";
    final static private String pubKey = "010001";
    final static private String headers[][] = {{"Accept","*/*"},
            {"Accept-Encoding","deflate,sdch"},
            {"Accept-Language","zh-CN,zh;q=0.8,gl;q=0.6,zh-TW;q=0.4"},
            {"Connection","keep-alive"},
            {"Content-Type","application/x-www-form-urlencoded"},
            {"Host","music.163.com"},
            {"Referer","http://music.163.com/search/"},
            {"User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36"}
    };

    //action
    final static int HTTP_METHOD_GET = 0;
    final static int HTTP_METHOD_POST = 1;

    //state
    private CookieStore cookieStore;
    private RequestConfig requestConfig;
    private HttpClientContext httpClientContext;

    private Config config;
    private int uid;
    private ArrayList<Playlist> playlists;
    private String cacheDir;

    private static Log log = LogFactory.getLog(NetEaseMusic.class);

    public NetEaseMusic(String configFile) {
        config = new Config(this, configFile); //read config file

        //read config
        try {
            uid = config.getId();
            cookieStore = config.getCookieStore();
        } catch (ConfigNoFoundException e) {
            uid = 0;
            cookieStore = new BasicCookieStore();
            config.setCookieStore(cookieStore);
        }

        try {
            playlists = config.getPlaylists();
        } catch (ConfigNoFoundException e) {
            playlists = new ArrayList<>();
        }

        cacheDir = config.getCacheDir();

        //check cache dir
        if (cacheDir != null) {
            File f = new File(cacheDir);

            if (!f.exists()) {
                f.mkdir();
            }
        }

        //set request config
        //set timeout
        final int TIMEOUTMS = 15 * 1000;
        requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUTMS).setConnectTimeout(TIMEOUTMS).setSocketTimeout(TIMEOUTMS).build();

        //set cookie
        requestConfig = RequestConfig.copy(requestConfig).setCookieSpec(CookieSpecs.STANDARD_STRICT).build();

        //create http client context
        httpClientContext = HttpClientContext.create();
        httpClientContext.setCookieStore(cookieStore);
    }

    public boolean login(String username, String password) {
        log.info("try to login " + username + " with password " + password);

        //create request data
        JSONObject requestJsonObject = new JSONObject();
        requestJsonObject.put("username", username);
        requestJsonObject.put("password", password);
        requestJsonObject.put("rememberLogin", "true");
        String data = encryptedRequest(requestJsonObject.toString());

        //post
        String response = rawHttpRequest(HTTP_METHOD_POST, "http://music.163.com/weapi/login?csrf_token=", data);
        if (response == null) {
            log.error("login request error, no response");
            return false;
        }

        //analyze response
        JSONObject responseJsonObject = new JSONObject(response);
        if(responseJsonObject.getInt("code") == 200){
            config.setId(uid = responseJsonObject.getJSONObject("account").getInt("id"));
            config.setCookieStore(cookieStore);
            log.info("login success");
            return true;
        } else {
            log.info("login failed");
            return false;
        }
    }

    public boolean isLogin() {
        return uid != 0;
    }

    public void setCacheDir(String s) {
        cacheDir = s;
        config.setCacheDir(s);

        //check cache dir
        if (cacheDir != null) {
            File f = new File(cacheDir);

            if (!f.exists()) {
                f.mkdir();
            }
        }
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void updateUserPlaylist() throws NoLoginException {

        //no login
        if (uid == 0) {
            throw new NoLoginException();
        }

        //send data
        String data = rawHttpRequest(HTTP_METHOD_GET, "http://music.163.com/api/user/playlist/?offset=0&limit=100&uid=" + uid);

        //request error
        if (data == null) {
            return;
        }

        playlists = new ArrayList<>(); //renew playlists
        config.removeAllPlaylists();

        //analyze response
        JSONObject jsonData = new JSONObject(data);
        for ( Object o : jsonData.getJSONArray("playlist")) {
            JSONObject jsonPlayList = (JSONObject) o;

            Playlist playlist = new Playlist(this, jsonPlayList.getString("name"), jsonPlayList.getInt("id"));

            playlists.add(playlist);
        }
    }

    public ArrayList<Playlist> getPlaylists() {
        return playlists;
    }

    Config getConfig() {
        return config;
    }

    public Playlist getPlaylist(int id) throws ConfigNoFoundException {
        return config.getPlaylist(id);
    }

    CookieStore getCookieStore() {
        return cookieStore;
    }

    String rawHttpRequest(int method, String action) {
        if (method == HTTP_METHOD_GET) {
            return rawHttpRequest(method, action, null);
        } else {
            return null;
        }
    }

    String rawHttpRequest(int method, String action, String data){
        String resp;

        HttpClient httpClient = HttpClients.createDefault();

        if (method == HTTP_METHOD_GET){
            //new get
            HttpGet httpGet = new HttpGet(action);
            for (String[] header : headers) {
                httpGet.addHeader(header[0], header[1]);
            }
            httpGet.setConfig(requestConfig);

            //go
            try {
                resp = httpClient.execute(httpGet, new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse httpResponse) throws IOException {
                        return Utils.getStringFromInputStream(httpResponse.getEntity().getContent());
                    }
                }, httpClientContext);
            } catch (Exception e) {
                return null;
            }

            return resp;
        } else if (method == HTTP_METHOD_POST) {
            //new post
            HttpPost httpPost = new HttpPost(action);
            for (String[] header : headers) {
                httpPost.addHeader(header[0], header[1]);
            }
            httpPost.setConfig(requestConfig);

            //set data
            try {
                httpPost.setEntity(new StringEntity(data));
            } catch (UnsupportedEncodingException e){
                return null;
            }

            //go
            try {
                resp = httpClient.execute(httpPost, new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse httpResponse) throws IOException {
                        return Utils.getStringFromInputStream(httpResponse.getEntity().getContent());
                    }
                }, httpClientContext);
            } catch (Exception e) {
                return null;
            }

            return resp;
        } else {
            return null;
        }
    }

    //based on [darknessomi/musicbox](https://github.com/darknessomi/musicbox)
    static String encryptedRequest(String text) {
        String secKey = createSecretKey(16);
        String encText = aesEncrypt(aesEncrypt(text, nonce), secKey);
        String encSecKey = rsaEncrypt(secKey, pubKey, modulus);
        try {
            return "params=" + URLEncoder.encode(encText, "UTF-8") + "&encSecKey=" + URLEncoder.encode(encSecKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //ignore
            return null;
        }
    }

    //based on [darknessomi/musicbox](https://github.com/darknessomi/musicbox)
    private static String aesEncrypt(String text, String key) {
        try {
            IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(text.getBytes());

            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            //ignore
            return null;
        }
    }

    //based on [darknessomi/musicbox](https://github.com/darknessomi/musicbox)
    private static String rsaEncrypt(String text, String pubKey, String modulus) {
        text = new StringBuilder(text).reverse().toString();
        BigInteger rs = new BigInteger(String.format("%x", new BigInteger(1, text.getBytes())), 16)
                .modPow(new BigInteger(pubKey, 16), new BigInteger(modulus, 16));
        String r = rs.toString(16);
        if (r.length() >= 256) {
            return r.substring(r.length() - 256, r.length());
        } else {
            while (r.length() < 256) {
                r = 0 + r;
            }
            return r;
        }
    }

    //based on [darknessomi/musicbox](https://github.com/darknessomi/musicbox)
    private static String createSecretKey(int i) {
        return RandomStringUtils.random(i, "0123456789abcde");
    }
}
