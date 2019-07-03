package com.ufotosoft.bzmedia.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextResourceReader {
    private static final String TAG = "bz_TextResourceReader";

    public static String readStringFromResource(Context context, int resourceId) {
        StringBuilder body = new StringBuilder();
        try {
            InputStream inputStream =
                    context.getResources().openRawResource(resourceId);
            InputStreamReader inputStreamReader =
                    new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String nextLine;
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
            return body.toString();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        return null;
    }

    public static String readStringFromAssets(Context context, String file) {
        StringBuilder body = new StringBuilder();
        try {
            InputStream inputStream =
                    context.getAssets().open(file);
            InputStreamReader inputStreamReader =
                    new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String nextLine;
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
            return body.toString();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        return null;
    }
}
