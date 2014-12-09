package me.everything.jittlib;

import android.net.Uri;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by adam on 12/9/14.
 */
public class ServerAPI {

    public static class Suggestion {
        String suggestion;
        String user_selected;
        Integer votes;
    }


    public interface ITranslationsReceiver {
        public void onGotTranslations( Map<String,Map<String,Suggestion>> suggestions );
    }

    public interface IActionDoneReceiver {
        public void onDone( boolean success );
    }

    void getTranslations( String deviceId, List<String> keys, List<String> translationLangs,
                          ITranslationsReceiver receiver ) {
        Uri.Builder builder = Uri.parse("http://jitt-server.appspot.com").buildUpon();
        builder.appendQueryParameter("device_id",deviceId);
        for ( String key : keys ) {
            builder.appendQueryParameter("key", key);
        }
        for ( String lang : translationLangs ) {
            builder.appendQueryParameter("locale", lang);
        }
    }

    public void doAction( String deviceId, String action, String key, String lang, String suggestion,
                          IActionDoneReceiver receiver ) {

    }

}
