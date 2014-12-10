package me.everything.jittlib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by eyalbiran on 12/9/14.
 */
public class Jitt {

    private static final String SELECTED_LOCALES = "jitt_selected_locals";
    private SharedPreferences mSP;
    private Locale mCurrentLocle;
    private View mLoadingScreen;

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

    private Map<String, String> mLocaleList;
    private List<String> mSelectedLocale;

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

    public void initialize(Activity context, Class<?> r) {
        Resources resources = context.getResources();
        mSP = context.getSharedPreferences("JITT", context.MODE_PRIVATE);
        mCurrentLocle = resources.getConfiguration().locale;
        mServerAPI = new ServerAPI(context);
        mResourcesEntries.clear();
        mDeviceId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        mLocaleList = new HashMap<>();
        Locale[] locales = Locale.getAvailableLocales();
        ArrayList<String> localeCountries = new ArrayList<String>();
        for (Locale l : locales) {
            String lang = l.getLanguage();
            if (lang.equals("iw")) {
                lang = "he";
            }
            if (!mLocaleList.containsKey(lang)) {
                mLocaleList.put(lang, l.getDisplayLanguage());
                localeCountries.add(l.getLanguage());
            }
        }

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

        final ViewGroup decorView = (ViewGroup)context.getWindow().getDecorView();
        Button button = new Button(context);
        button.setText("Translate!");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTranslationWindow(decorView);
            }
        });
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        decorView.addView(button,layoutParams);


        mLoadingScreen = LayoutInflater.from(context).inflate(R.layout.loading_screen, null, false);
        mLoadingScreen.setVisibility(View.GONE);
        mLoadingScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DO nothing
            }
        });


        decorView.addView(mLoadingScreen);

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
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingScreen.setVisibility(View.VISIBLE);
        }

        @Override
        protected View doInBackground(View... params) {
            View root = params[0];
            // Extract Strings from View Hierarchy
            mViewResourcesStrings.clear();
            mViewNoneResourcesStrings.clear();

            extractStringsFromView(root);

            getDataFromServer();


            return root;
        }

        @Override
        protected void onPostExecute(View view) {
            super.onPostExecute(view);
            view.getContext().startActivity(new Intent(view.getContext(), JittMainActivity.class));
            mLoadingScreen.setVisibility(View.GONE);
        }
    }

    private void getDataFromServer() {
        // Get data from server
        List<String> translationLangs = getSelectedLocale();
        List<String> keys = getKeysForStrings(mViewResourcesStrings);
        mSuggestions = mServerAPI.getTranslations(mDeviceId, keys, translationLangs);
    }

    HashMap<String, ArrayList<ServerAPI.Suggestion>> getDataForString(String string) {
        Entry entry = mResourcesEntries.get(string);
        String key = entry.key;

        return mSuggestions.get(key);
    }

    public List<Map.Entry<String, String>> getAllLocale() {
        Set<Map.Entry<String, String>> keys = mLocaleList.entrySet();

        // TODO ORDER SMART?
        List<Map.Entry<String, String>> allLocale = new ArrayList<>(keys.size());
        allLocale.addAll(keys);
        Collections.sort(allLocale, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> lhs, Map.Entry<String, String> rhs) {
                return lhs.getValue().compareTo(rhs.getValue());
            }
        });

        return allLocale;
    }

    public List<String> getSelectedLocale() {
        if (mSelectedLocale == null) {
            String localesList = mSP.getString(SELECTED_LOCALES, "");
            String[] localesArray = localesList.split(",");
            mSelectedLocale = new ArrayList<>(localesArray.length);
            for (int i = 0; i < localesArray.length; i ++) {
                String locale = localesArray[i];
                if (!locale.isEmpty()) {
                    mSelectedLocale.add(localesArray[i]);
                }
            }
            if (mSelectedLocale.isEmpty()) {
                // Add at least selected locale and ENG?
                addSelectedLocale("en");
                addSelectedLocale(mCurrentLocle.getDisplayName());
            }
        }

        return mSelectedLocale;
    }

    public void addSelectedLocale(String locale) {
        if (!mSelectedLocale.contains(locale)) {
            mSelectedLocale.add(locale);
            saveSelectedLocales();
        }
    }

    public void removeSelectedLocale(String locale) {
        if (mSelectedLocale.contains(locale)) {
            mSelectedLocale.remove(locale);
            saveSelectedLocales();
        }
    }

    private void saveSelectedLocales() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mSelectedLocale.size(); i++) {
            sb.append(mSelectedLocale.get(i)).append(",");
        }
        mSP.edit().putString(SELECTED_LOCALES, sb.toString()).apply();
    }

    public String getLanguageName(String locale) {
        return mLocaleList.get(locale);
    }

    interface  UserActionListener {
        void onPreAction();
        void onPostAction();
    }

    void sendUserAction(UserActionListener listener, String string, String lang, String suggestion, String action) {
        (new SendActionTask(listener)).execute(string, lang, suggestion, action);
    }

    private class SendActionTask extends AsyncTask<String, Void, Void> {

        private final UserActionListener mListener;

        public SendActionTask(UserActionListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mListener.onPreAction();
        }

        @Override
        protected Void doInBackground(String... params) {
            String string = params[0];
            String lang = params[1];
            String suggestion = params[2];
            String action = params[3];
            Entry entry = mResourcesEntries.get(string);
            String key = entry.key;
            mServerAPI.doAction(mDeviceId, key, lang, suggestion, action);

            // Reload data
            getDataFromServer();

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            mListener.onPostAction();
        }
    }

}
