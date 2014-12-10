package me.everything.jittlib;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class JittTranslateActivity extends ActionBarActivity {

    public static String EXTRA_STRING_VALUE = "value";

    private enum ColorsList {

        // 50 / 200 / 600
        Indigo(Color.parseColor("#E8EAF6"), Color.parseColor("#9FA8DA"), Color.parseColor("#3949AB")),
        Red(Color.parseColor("#FFEBEE"), Color.parseColor("#EF9A9A"), Color.parseColor("#E53935"));

        int lighter;
        int light;
        int dark;

        ColorsList(int lighter, int light, int dark) {
            this.lighter = lighter;
            this.light = light;
            this.dark = dark;
        }
    }


    HashMap<String, ArrayList<ServerAPI.Suggestion>> mData;
    private List<String> mLangList = new ArrayList<>();
    private List<ColorsList> mLangColorList = new ArrayList<>();

    private List<Object> mItemsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jitt_translate);

        String value = getIntent().getStringExtra(EXTRA_STRING_VALUE);

        getSupportActionBar().setTitle(value);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mData = Jitt.getInstance().getDataForString(value);

        // Create the languages list
        Set<String> langKeySet = mData.keySet();
        mLangList = new ArrayList<>(langKeySet.size());
        Iterator<String> iterator = langKeySet.iterator();
        ColorsList[] colorsList = ColorsList.values();
        while (iterator.hasNext()) {
            String lang = iterator.next();
            mLangColorList.add(colorsList[mLangList.size()%colorsList.length]);
            if ("en".equals(lang)) {
                mLangList.add(0,lang);
            } else {
                mLangList.add(lang);
            }

        }

        // Create items list for easy management
        for (String lang: mLangList) {
            // Add the lang title
            mItemsList.add(lang);
            ArrayList<ServerAPI.Suggestion> suggestions = mData.get(lang);
            for (ServerAPI.Suggestion suggestion : suggestions) {
                // Add the suggestion, TODO is it ordered? if not, put in a different list and reorder
                SuggestionEntry entry = new SuggestionEntry();
                entry.suggestion = suggestion;
                entry.lang = lang;
                mItemsList.add(entry);
            }
        }

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(new SuggestionsListAdapter());

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_jitt_translate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, JittSelectLanguagesActivity.class));
            return true;
        }  else if (id == R.id.home || id == R.id.homeAsUp || id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class SuggestionsListAdapter extends BaseAdapter {

        private final int VIEW_TYPE_LANG_TITLE = 0;
        private final int VIEW_TYPE_SUGGESTION_ENTRY = 1;

        @Override
        public int getCount() {
            return mItemsList.size();
        }

        @Override
        public int getItemViewType(int position) {
            Object object = mItemsList.get(position);
            if (object instanceof String) {
                return VIEW_TYPE_LANG_TITLE;
            } else {
                return VIEW_TYPE_SUGGESTION_ENTRY;
            }
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            switch (viewType) {
                case VIEW_TYPE_SUGGESTION_ENTRY:
                    return getSuggestionEntryView(position, convertView, parent);
                case VIEW_TYPE_LANG_TITLE:
                    return getTitleView(position, convertView,parent);
            }
            return null;
        }

        private View getSuggestionEntryView(int position, View convertView, ViewGroup parent) {
            SuggestionEntryViewHolder holder = null;
            if (convertView == null) {
                holder = new SuggestionEntryViewHolder();
                convertView = LayoutInflater.from(JittTranslateActivity.this).inflate(R.layout.translate_entry_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);
                holder.votes = (TextView)convertView.findViewById(R.id.votes);

                convertView.setTag(holder);
            } else {
                holder = (SuggestionEntryViewHolder)convertView.getTag();
            }

            SuggestionEntry entry = (SuggestionEntry)mItemsList.get(position);

            String lang = entry.lang;
            int langIndex = mLangList.indexOf(lang);
            ColorsList color = mLangColorList.get(langIndex);
            holder.votes.setText(String.valueOf(entry.suggestion.votes));
            holder.votes.setBackgroundColor(color.light);
            holder.title.setText(entry.suggestion.suggested);
            convertView.setBackgroundColor(color.lighter);

            // TODO update user action if any

            return convertView;
        }

        private View getTitleView(int position, View convertView, ViewGroup parent) {
            TitleViewHolder holder = null;
            if (convertView == null) {
                holder = new TitleViewHolder();
                convertView = LayoutInflater.from(JittTranslateActivity.this).inflate(R.layout.translate_lang_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);

                convertView.setTag(holder);
            } else {
                holder = (TitleViewHolder)convertView.getTag();
            }

            String lang = (String)mItemsList.get(position);

            int langIndex = mLangList.indexOf(lang);
            ColorsList color = mLangColorList.get(langIndex);

            holder.title.setText(Jitt.getInstance().getLanguageName(lang));
            convertView.setBackgroundColor(color.dark);

            return convertView;
        }

        private class SuggestionEntryViewHolder {
            public TextView title;
            public TextView votes;
        }

        private class TitleViewHolder {
            public TextView title;
        }
    }

    private class SuggestionEntry {
        public ServerAPI.Suggestion suggestion;
        public String lang;
    }
}
