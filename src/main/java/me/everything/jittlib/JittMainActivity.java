package me.everything.jittlib;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class JittMainActivity extends ActionBarActivity implements Jitt.UserActionListener {

    private static final int RC_SELECT_LANGUAGES = 1010;

    private ListView mStringsList;
    private StringsListAdapter mStringListAdapter;
    private View mLoadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jitt_main);
        getSupportActionBar().setTitle(R.string.title_activity_jitt_main);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#13b476")));

        mStringListAdapter = new StringsListAdapter();
        mStringsList = (ListView)findViewById(R.id.list);
        mStringsList.setAdapter(mStringListAdapter);
        mStringsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(JittMainActivity.this, JittTranslateActivity.class);

                intent.putExtra(JittTranslateActivity.EXTRA_STRING_VALUE, (String)mStringListAdapter.getItem(position));

                startActivity(intent);
            }
        });

        final ViewGroup decorView = (ViewGroup)this.getWindow().getDecorView();

        mLoadingScreen = LayoutInflater.from(this).inflate(R.layout.loading_screen, null, false);
        mLoadingScreen.setVisibility(View.GONE);
        mLoadingScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DO nothing
            }
        });

        decorView.addView(mLoadingScreen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_jitt_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (RC_SELECT_LANGUAGES == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                // There was a change in the languages, we need to reload
                Jitt.getInstance().updateData(this);
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(this, JittSelectLanguagesActivity.class), RC_SELECT_LANGUAGES);
            return true;
        } else if (id == R.id.home || id == R.id.homeAsUp || id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPreAction() {
        mLoadingScreen.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostAction() {
        mStringListAdapter.notifyDataSetChanged();
        mLoadingScreen.setVisibility(View.GONE);
    }

    private class StringsListAdapter extends BaseAdapter {

        private final int VIEW_TYPE_VALID_STRING = 0;
        private final int VIEW_TYPE_NOT_VALID_STRING_TITLE = 1;
        private final int VIEW_TYPE_NOT_VALID_STRING = 2;

        @Override
        public int getCount() {
            List<String> validStrings = Jitt.getInstance().getValidViewStrings();
            return validStrings.size();

        }

        @Override
        public Object getItem(int position) {
            List<String> validStrings = Jitt.getInstance().getValidViewStrings();
            return validStrings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getValidEntryView(position, convertView, parent);
        }

        private View getValidEntryView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(JittMainActivity.this).inflate(R.layout.translation_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);
                holder.icon = (ImageView)convertView.findViewById(R.id.icon);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            String string = (String)getItem(position);
            holder.title.setText(string);

            // Set Icon
            boolean missingTranslation = false;

            HashMap<String, ArrayList<ServerAPI.Suggestion>> data = Jitt.getInstance().getDataForString(string);
            List<String> selectedLocales = Jitt.getInstance().getSelectedLocale();
            for (String locale: selectedLocales) {
                ArrayList<ServerAPI.Suggestion> suggestions = data.get(locale);
                if (suggestions.isEmpty()) {
                    missingTranslation = true;
                    break;
                }
            }

            holder.icon.setImageResource(missingTranslation? R.drawable.icon_warning_yellow : R.drawable.icon_done);

            return convertView;
        }

        private View getNotValidEntryView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(JittMainActivity.this).inflate(R.layout.translation_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            List<String> validStrings = Jitt.getInstance().getValidViewStrings();
            List<String> notValidStrings = Jitt.getInstance().getNoneValidViewStrings();

            holder.title.setAlpha(0.5f);
            holder.title.setTextColor(Color.BLACK);
            convertView.setBackgroundDrawable(null);
            holder.title.setText((String)getItem(position));

            return convertView;
        }

        private View getTitleView(int titleId, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(JittMainActivity.this).inflate(R.layout.translation_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);


                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            holder.title.setText(titleId);
            holder.title.setTextColor(Color.GRAY);
            convertView.setBackgroundColor(Color.BLACK);
            holder.title.setAlpha(1f);

            return convertView;
        }

        private class ViewHolder {
            public TextView title;
            public ImageView icon;
        }
    }
}
