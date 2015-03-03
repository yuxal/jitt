package me.everything.jittlib;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
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
    private static final String TAG = "JITT.main";
    private SharedPreferences mSP;
    private Locale mCurrentLocale;
    private PrepareTranslationsView mPrepareTranslationsTask = null;
    private View mLoadingScreen;
    private JittService mService;
    private Activity mSavedActivity;
    private boolean mEnabled = false;
    private Handler mHandler;
    private Runnable mDelayedPauseUpdate;
    private Intent mServiceIntent;
    private ServiceConnection mServiceConnection;

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

    public void initialize(Context context, Class<?>... rs) {
        Resources resources = context.getResources();
        mSP = context.getSharedPreferences("JITT", context.MODE_PRIVATE);
        mCurrentLocale = resources.getConfiguration().locale;
        mServerAPI = new ServerAPI(context.getApplicationContext());
        mHandler = new Handler();
        mDelayedPauseUpdate = new Runnable() {
            @Override
            public void run() {
                mService.setActivity(null);
            }
        };
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

        for ( Class<?> r : rs ) {
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
                    } catch (android.content.res.Resources.NotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        mLoadingScreen = LayoutInflater.from(context).inflate(R.layout.loading_screen, null, false);
        mLoadingScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // DO nothing
            }
        });


        ((Application)context.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                mHandler.removeCallbacks(mDelayedPauseUpdate);
                setCurrentActivity(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if ( mService != null ) {
                    // To prevent Icon flicker when changing in app activities
                    mHandler.postDelayed(mDelayedPauseUpdate, 100);
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    public void setCurrentActivity(Activity activity) {
        if ( mService != null ) {
            mService.setActivity(activity);
        } else {
            mSavedActivity = activity;
        }
    }

    private boolean startService(Context context) {
        if (mServiceIntent == null && mServiceConnection == null && mService == null) {
            mServiceIntent = new Intent(context, JittService.class);

            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (service instanceof JittService.JittLocalBinder) {
                        JittService.JittLocalBinder binder = (JittService.JittLocalBinder) service;
                        mService = binder.getService();
                        if (mSavedActivity != null) {
                            mService.setActivity(mSavedActivity);
                            mSavedActivity = null;
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mService = null;
                }
            };

            return context.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }

        return mService != null;
    }

    private boolean stopService(Context context) {
        if (mServiceConnection != null || mService != null || mServiceIntent != null) {
            try {
                context.unbindService(mServiceConnection);
            } catch (IllegalArgumentException e) {
                // Service not registered ?
                context.stopService(mServiceIntent);
            }

            mService = null;
            mServiceConnection = null;
            mServiceIntent = null;
        }
        return true;
    }

    public synchronized boolean setEnabled(Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();
        if (mEnabled != enabled) {
            if (mService == null && enabled) {
                if (startService(appContext)) {
                    mEnabled = true;
                }
            } else if (mService != null && !enabled) {
                if (stopService(appContext)) {
                    mEnabled = false;
                }
            }
        }

        return mEnabled = enabled;
    }

    public void openTranslationWindow(ViewGroup root) {
        if (mPrepareTranslationsTask == null) {
            mPrepareTranslationsTask = new PrepareTranslationsView(root);
            mPrepareTranslationsTask.execute();
        }
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
        }

        if (view instanceof TextView) {
            // We have one!
            TextView textView = (TextView)view;
            String string = textView.getText().toString();
            addViewString(string);

        }

        if (view instanceof EditText) {
            // We have one!
            EditText textView = (EditText)view;
            String string = textView.getHint().toString();
            addViewString(string);

        }
    }

    private void addViewString(String string) {
        if ( string.length() > 0 ) {

            if (mResourcesEntries.containsKey(string)) {
                // This string has a corresponding entry in string XML
                mViewResourcesStrings.add(string);
            } else {
                mViewNoneResourcesStrings.add(string);
            }
        }
    }

    private class PrepareTranslationsView extends AsyncTask<Void, Void, Void> {

        private ViewGroup mRoot;

        public PrepareTranslationsView(ViewGroup root) {
            mRoot = root;
        }

        @Override
        protected void onPreExecute() {
            // We want to disable the button while loading
            mService.setEnabled(false);
            mRoot.addView(mLoadingScreen);
        }

        @Override
        protected Void doInBackground(Void... params) {
            View root = mRoot;
            // Extract Strings from View Hierarchy
            mViewResourcesStrings.clear();
            mViewNoneResourcesStrings.clear();

            extractStringsFromView(root);

            getDataFromServer();

            return null;
        }

        @Override
        protected void onPostExecute(Void view) {
            mRoot.getContext().startActivity(new Intent(mRoot.getContext(), JittMainActivity.class));
            mRoot.removeView(mLoadingScreen);
            mPrepareTranslationsTask = null;
        }
    }

    private void getDataFromServer() {
        // Get data from server
        List<String> translationLangs = getSelectedLocale();
        List<String> keys = getKeysForStrings(mViewResourcesStrings);
        ServerAPI.TranslationResult suggestions = mServerAPI.getTranslations(mDeviceId, keys, translationLangs);
        if ( suggestions != null ) {
            mSuggestions = suggestions;
        }
    }

    Map<String, ArrayList<ServerAPI.Suggestion>> getDataForString(String string) {
        Entry entry = mResourcesEntries.get(string);
        String key = entry.key;

        if ( mSuggestions != null ) {
            return mSuggestions.get(key);
        } else {
            return Collections.emptyMap();
        }
    }

    public List<Map.Entry<String, String>> getAllLocale() {
        Set<Map.Entry<String, String>> keys = mLocaleList.entrySet();

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
                String currentLocale = mCurrentLocale.getLanguage();

                addSelectedLocale("en");
                addSelectedLocale(currentLocale);
            }
        }

        return mSelectedLocale;
    }

    public void addSelectedLocale(String locale) {
        if (locale != null && !locale.isEmpty() && !mSelectedLocale.contains(locale)) {
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


    void updateData(UserActionListener listener) {
        (new UpdateDataTask(listener)).execute();
    }

    private class UpdateDataTask extends AsyncTask<String, Void, Void> {

        private final UserActionListener mListener;

        public UpdateDataTask(UserActionListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mListener.onPreAction();
        }

        @Override
        protected Void doInBackground(String... params) {

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
