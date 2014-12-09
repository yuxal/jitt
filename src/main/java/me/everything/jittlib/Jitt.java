package me.everything.jittlib;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
    private static Map<String, Entry> mResourcesEntries = new HashMap<>();
    private static List<String> mViewResourcesStrings = new ArrayList<>();
    private static List<String> mViewNoneResourcesStrings = new ArrayList<>();

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
        mResourcesEntries.clear();

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
        // Extract Strings from View Hierarchy
        // TODO Show loader for this
        mViewResourcesStrings.clear();
        mViewNoneResourcesStrings.clear();
        extractStringsFromView(root);

        root.getContext().startActivity(new Intent(root.getContext(), JittMainActivity.class));
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

}
