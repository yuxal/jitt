package me.everything.jittlib;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by adam on 12/9/14.
 */
public class ServerAPI {

    private static final String TAG ="JITT.serverapi";
    private final String mAppId;
    private Gson mGson;

    public static class Suggestion {
        String suggested;
        String user_selected;
        Integer votes;

        public String toString() {
            return suggested+" ("+votes+" votes, "+user_selected+")";
        }
    }

    public static class TranslationResult extends HashMap<String,HashMap<String,ArrayList<Suggestion>>> {
    }

    public ServerAPI(Context context) {
        mGson = new Gson();
        mAppId = context.getApplicationContext().getPackageName();
    }

    private String readFromURL(String _url, boolean post) {
        InputStreamReader inStreamReader = null;
        BufferedReader bufferedReader;
        try {
            URL url = new URL(_url);
            if ( post ) {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                inStreamReader = new InputStreamReader(connection.getInputStream());
            } else {
                inStreamReader = new InputStreamReader(url.openStream());
            }
            bufferedReader = new BufferedReader(inStreamReader);
            String data = "";
            while ( true ) {
                String line = bufferedReader.readLine();
                if ( line == null ) {
                    break;
                }
                data += line+"\n";
            }
            return data.trim();

        } catch (IOException e) {
            Log.d(TAG, "Error: " + e);
        }
        return null;
    }

    public TranslationResult getTranslations( String deviceId, List<String> keys, List<String> translationLangs ) {
        Uri.Builder builder = Uri.parse("http://jitt-server.appspot.com/api/translations").buildUpon();
        builder.appendQueryParameter("app_id",mAppId);
        builder.appendQueryParameter("device_id",deviceId);
        for ( String key : keys ) {
            builder.appendQueryParameter("key", key);
        }
        for ( String lang : translationLangs ) {
            builder.appendQueryParameter("locale", lang);
        }
        Log.d(TAG, "URL=" + builder.toString());
        String data = readFromURL(builder.toString(),false);
        Log.v(TAG, "DATA=" + data);
        TranslationResult parsed = mGson.fromJson(data, TranslationResult.class);
        return parsed;
    }

    public boolean doAction( String deviceId, String key, String lang, String suggestion, String action ) {
        Uri.Builder builder = Uri.parse("http://jitt-server.appspot.com/api/action").buildUpon();
        builder.appendQueryParameter("app_id",mAppId);
        builder.appendQueryParameter("device_id",deviceId);
        builder.appendQueryParameter("key", key);
        builder.appendQueryParameter("locale", lang);
        builder.appendQueryParameter("string", suggestion);
        builder.appendQueryParameter("action", action);
        Log.d(TAG, "URL=" + builder.toString());
        String data = readFromURL(builder.toString(),true);
        Log.v(TAG, "DATA=" + data);
        return data.equals("OK");
    }

}
