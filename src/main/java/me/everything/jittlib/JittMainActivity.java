package me.everything.jittlib;

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

import java.util.List;


public class JittMainActivity extends ActionBarActivity {

    private ListView mStringsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jitt_main);

        mStringsList = (ListView)findViewById(R.id.list);
        mStringsList.setAdapter(new StringsListAdapter());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_jitt_main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class StringsListAdapter extends BaseAdapter {

        private final int VIEW_TYPE_VALID_STRING = 0;
        private final int VIEW_TYPE_NOT_VALID_STRING_TITLE = 1;
        private final int VIEW_TYPE_NOT_VALID_STRING = 2;

        @Override
        public int getCount() {
            List<String> validStrings = Jitt.getInstance().getValidViewStrings();
            List<String> notValidStrings = Jitt.getInstance().getNoneValidViewStrings();
            return validStrings.size()
                   + (notValidStrings.size() > 0 ? 1 + notValidStrings.size(): 0);
        }

        @Override
        public int getItemViewType(int position) {
            List<String> validStrings = Jitt.getInstance().getValidViewStrings();
            if (position < validStrings.size()) {
                return VIEW_TYPE_VALID_STRING;
            } else if (position == validStrings.size()) {
                return VIEW_TYPE_NOT_VALID_STRING_TITLE;
            } else {
                return VIEW_TYPE_NOT_VALID_STRING;
            }
        }

        @Override
        public Object getItem(int position) {
            List<String> validStrings = Jitt.getInstance().getValidViewStrings();
            List<String> notValidStrings = Jitt.getInstance().getNoneValidViewStrings();
            int type = getItemViewType(position);
            switch (type) {
            case VIEW_TYPE_VALID_STRING:
                return validStrings.get(position);
            case VIEW_TYPE_NOT_VALID_STRING:
                return notValidStrings.get(position - 1 - validStrings.size());
            }

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
            case VIEW_TYPE_VALID_STRING:
                return getValidEntryView(position, convertView, parent);
            case VIEW_TYPE_NOT_VALID_STRING:
                return getNotValidEntryView(position, convertView, parent);
            case VIEW_TYPE_NOT_VALID_STRING_TITLE:
                return getTitleView(R.string.entries_not_valid_title, convertView,parent);
            }
            return getValidEntryView(position, convertView, parent);
        }

        private View getValidEntryView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(JittMainActivity.this).inflate(R.layout.translation_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            holder.title.setAlpha(1f);
            holder.title.setTextColor(Color.BLACK);
            convertView.setBackground(null);
            holder.title.setText((String)getItem(position));

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
            convertView.setBackground(null);
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
        }
    }
}
