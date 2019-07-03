package com.ufotosoft.bzmedia.utils;

import android.text.TextUtils;

import java.security.MessageDigest;
import java.util.Random;

/**
 * Created by zhandalin on 2018-10-13 13:52.
 * 说明:
 */
public class BZMD5Util {
    public static String md5(String string) {
        Random random = new Random(System.currentTimeMillis());
        if (TextUtils.isEmpty(string)) {
            return random.nextInt(100000) + "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return random.nextInt(100000) + "";
    }
}
