package me.everything.jittlib;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by eyalbiran on 12/9/14.
 */
public class Jitt {

    public static class Entry {
        public String key;
        public String text;
        public Locale locale;
        public int resId;
    }

    private static Jitt mInstance = new Jitt();
    private ServerAPI mServerAPI;
    private ServerAPI.TranslationResult mSuggestions;

    private String mDeviceId;

    private Map<String, Entry> mResourcesEntries = new HashMap<>();
    private List<String> mViewResourcesStrings = new ArrayList<>();
    private List<String> mViewResourcesKeys = new ArrayList<>();
    private List<String> mViewNoneResourcesStrings = new ArrayList<>();

    public static Jitt getInstance() {
        return mInstance;
    }

    public List<String> getValidViewStrings() {
        return mViewResourcesStrings;
    }

    public List<String> getNoneValidViewStrings() {
        return mViewNoneResourcesStrings;
    }

    public void initialize(Context context, Class<?> r) {
        Resources resources = context.getResources();
        mServerAPI = new ServerAPI();
        mResourcesEntries.clear();
        mDeviceId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);


        // Create the local mapping from Key, Strings, Locale
        Class stringRClass = null;
        Class[] classes = r.getDeclaredClasses();
        for (Class clazz: classes) {
            if (clazz.getName().endsWith("string")) {
                stringRClass = clazz;
                break;
            }
        }

        if (stringRClass != null) {
            Field[] classFields = stringRClass.getDeclaredFields();
            // Get all (Keys Names, Identifiers)
            for (Field field : classFields) {
                try {
                    Entry entry = new Entry();
                    entry.key = field.getName();
                    entry.resId = (int)field.get(stringRClass);
                    entry.text = resources.getString(entry.resId);
                    mResourcesEntries.put(entry.text, entry);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void openTranslationWindow(View root) {
        // TODO add loading screen with cancel option
        (new PrepareTranslationsView()).execute(root);
    }

    private List<String> getKeysForStrings(List<String> strings) {
        List<String> keys = new ArrayList<>();
        for (String string: strings) {
            keys.add(mResourcesEntries.get(string).key);
        }
        return keys;
    }

    private void extractStringsFromView(View view) {
        if (view instanceof ViewGroup) {
            // This is a container of view, go down the View Hierarchy
            ViewGroup viewGroup = (ViewGroup)view;
            int childCount = viewGroup.getChildCount();
            for (int index = 0; index < childCount; index++) {
                View child = viewGroup.getChildAt(index);
                extractStringsFromView(child);
            }
        } else if (view instanceof TextView) {
            // We have one!
            TextView textView = (TextView)view;
            String string = textView.getText().toString();

            if (mResourcesEntries.containsKey(string)) {
                // This string has a corresponding entry in string XML
                mViewResourcesStrings.add(string);
            } else {
                mViewNoneResourcesStrings.add(string);
            }

        }
    }

    private class PrepareTranslationsView extends AsyncTask<View, Void, View> {

        @Override
        protected View doInBackground(View... params) {
            View root = params[0];
            // Extract Strings from View Hierarchy
            mViewResourcesStrings.clear();
            mViewNoneResourcesStrings.clear();
            extractStringsFromView(root);

            // Get data from server
            List<String> translationLangs = new ArrayList<>();
            translationLangs.add("en");
            translationLangs.add("he");
            List<String> keys = getKeysForStrings(mViewResourcesStrings);
            mSuggestions = mServerAPI.getTranslations(mDeviceId, keys, translationLangs);
            return root;
        }

        @Override
        protected void onPostExecute(View view) {
            super.onPostExecute(view);
            view.getContext().startActivity(new Intent(view.getContext(), JittMainActivity.class));
        }
    }

    HashMap<String, ArrayList<ServerAPI.Suggestion>> getDataForString(String string) {
        Entry entry = mResourcesEntries.get(string);
        String key = entry.key;

        return mSuggestions.get(key);
    }

    public String[] getAllLocale() {
        Locale[] locales = Locale.getAvailableLocales();
        ArrayList<String> localeCountries = new ArrayList<String>();
        for(Locale l:locales) {
            localeCountries.add(l.getDisplayLanguage().toString());
        }
        return (String[]) localeCountries.toArray(new String[localeCountries.size()]);
    }

}
