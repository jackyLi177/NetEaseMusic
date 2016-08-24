package com.c0ldcat.netease.music;

import com.c0ldcat.netease.music.utils.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;

public class NetEaseMusic {
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

    final static private int HTTP_METHOD_GET = 0;
    final static private int HTTP_METHOD_POST = 1;
    final static private int HTTP_METHOD_LOGIN_POST = 2;

    private String rawHttpRequest(int method, String action) {
        if (method == HTTP_METHOD_GET) {
            return rawHttpRequest(method, action, null);
        } else {
            return null;
        }
    }

    private String rawHttpRequest(int method, String action, String data){
        String resp;

        HttpClient httpClient = HttpClients.createDefault();

        if (method == HTTP_METHOD_GET){
            //new get
            HttpGet httpGet = new HttpGet(action);
            for (String[] header : headers) {
                httpGet.addHeader(header[0], header[1]);
            }

            //set timeout
            final int TIMEOUTMS = 15 * 1000;
            RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUTMS).setConnectTimeout(TIMEOUTMS).setSocketTimeout(TIMEOUTMS).build();
            httpGet.setConfig(requestConfig);

            //go
            try {
                resp = httpClient.execute(httpGet, new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse httpResponse) throws IOException {
                        return Utils.getStringFromInputStream(httpResponse.getEntity().getContent());
                    }
                });
            } catch (Exception e) {
                return null;
            }

            return resp;
        } else if (method == HTTP_METHOD_POST || method == HTTP_METHOD_LOGIN_POST) {
            //new post
            HttpPost httpPost = new HttpPost(action);
            for (String[] header : headers) {
                httpPost.addHeader(header[0], header[1]);
            }

            //set data
            try {
                httpPost.setEntity(new StringEntity(data));
            } catch (UnsupportedEncodingException e){
                return null;
            }

            //set timeout
            final int TIMEOUTMS = 15 * 1000;
            RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUTMS).setConnectTimeout(TIMEOUTMS).setSocketTimeout(TIMEOUTMS).setCookieSpec(CookieSpecs.STANDARD_STRICT).build();

            //set cookie
            requestConfig = RequestConfig.copy(requestConfig).setCookieSpec(CookieSpecs.STANDARD_STRICT).build();
            CookieStore cookieStore = new BasicCookieStore();
            HttpClientContext httpClientContext = HttpClientContext.create();
            httpClientContext.setCookieStore(cookieStore);

            httpPost.setConfig(requestConfig);

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

            if (method == HTTP_METHOD_LOGIN_POST){
                //save cookie
                System.out.println("cookie : " + httpClientContext.getCookieStore().getCookies().toString());
            }

            return resp;
        } else {
            return null;
        }
    }

    private static String encrypted_request(String text) {
        String secKey = createSecretKey(16);
        String encText = aesEncrypt(aesEncrypt(text, nonce), secKey);
        String encSecKey = rsaEncrypt(secKey, pubKey, modulus);
        return "params=" + URLEncoder.encode(encText) + "&encSecKey=" + URLEncoder.encode(encSecKey);
    }

    private static String aesEncrypt(String text, String key) {
        try {
            IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding ");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(text.getBytes());

            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

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

    private static String createSecretKey(int i) {
        return RandomStringUtils.random(i, "0123456789abcde");
    }
}