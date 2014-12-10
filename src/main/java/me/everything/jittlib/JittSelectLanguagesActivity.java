package me.everything.jittlib;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by eyalbiran on 12/10/14.
 */
public class JittSelectLanguagesActivity extends ActionBarActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_languages);

        getSupportActionBar().setTitle(R.string.action_settings_select_languages);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView mylist = (ListView) findViewById(R.id.list);

        List<Map.Entry<String, String>> allLocale = Jitt.getInstance().getAllLocale();
        Jitt.getInstance().getSelectedLocale();

        SelectLanguagesAdapter adapter = new SelectLanguagesAdapter(allLocale);

        mylist.setAdapter(adapter);
        mylist.setOnItemClickListener(adapter);
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
        } else if (id == R.id.home || id == R.id.homeAsUp || id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class SelectLanguagesAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        List<Map.Entry<String, String>> mAllLocale;

        public SelectLanguagesAdapter(List<Map.Entry<String, String>> allLocale) {
            mAllLocale = allLocale;
        }

        @Override
        public int getCount() {
            return mAllLocale.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(JittSelectLanguagesActivity.this).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);

                holder.text = (CheckedTextView)convertView.findViewById(android.R.id.text1);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            Map.Entry<String, String> item = mAllLocale.get(position);
            holder.text.setText(item.getValue());
            holder.text.setChecked(Jitt.getInstance().getSelectedLocale().contains(item.getKey()));
            return convertView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String localeKey = mAllLocale.get(position).getKey();
            ViewHolder holder = (ViewHolder)view.getTag();

            boolean isChecked = holder.text.isChecked();
            if (isChecked) {
                Jitt.getInstance().removeSelectedLocale(localeKey);
            } else {
                Jitt.getInstance().addSelectedLocale(localeKey);
            }
            holder.text.setChecked(!isChecked);
        }

        private class ViewHolder {
            public CheckedTextView text;
        }
    }

}
