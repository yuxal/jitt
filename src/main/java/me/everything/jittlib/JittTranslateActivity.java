package me.everything.jittlib;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class JittTranslateActivity extends ActionBarActivity implements Jitt.UserActionListener {

    private static final int RC_SELECT_LANGUAGES = 1010;
    public static String EXTRA_STRING_VALUE = "value";
    private String mString;
    private ListView mList;
    private EditText mSuggestionInput;
    private ViewGroup mSuggestion;
    private TextView mSuggestionTitle;
    private Button mSuggestionSend;
    private View mLoadingScreen;
    private SuggestionsListAdapter mAdapter;

    @Override
    public void onPreAction() {
        mLoadingScreen.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostAction() {
        loadData();
        mAdapter.notifyDataSetChanged();
        mLoadingScreen.setVisibility(View.GONE);
        Toast.makeText(this, getString(R.string.changes_saved), Toast.LENGTH_LONG).show();
    }

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


    Map<String, ArrayList<ServerAPI.Suggestion>> mData;
    private List<String> mLangList;

    private List<Object> mItemsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jitt_translate);

        mString = getIntent().getStringExtra(EXTRA_STRING_VALUE);

        getSupportActionBar().setTitle("Translate");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#13b476")));

        // display the original string in Jitt's locale if possible
        // otherwise use device locale
        String originalString = mString;
        int resId = Jitt.getInstance().getResourceIdForString(mString);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && resId > 0) {
            Configuration conf = new Configuration(getResources().getConfiguration());
            conf.locale = Jitt.getInstance().getDisaplyLocale();
            originalString = createConfigurationContext(conf).getResources().getString(resId);
        }

        TextView originalStringView = (TextView)findViewById(R.id.original_string);
        originalStringView.setText(originalString);

        loadData();

        mAdapter = new SuggestionsListAdapter();
        mList = (ListView)findViewById(R.id.list);
        mList.setAdapter(mAdapter);

        mSuggestion = (ViewGroup)findViewById(R.id.suggestion_container);
        mSuggestionTitle = (TextView)findViewById(R.id.suggestion_lang);
        mSuggestionInput = (EditText)findViewById(R.id.suggestion_string);
        mSuggestionSend = (Button)findViewById(R.id.suggestion_send);

        mSuggestionSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lang = (String)mSuggestionTitle.getTag();
                Jitt.getInstance().sendUserAction(JittTranslateActivity.this,mString, lang, mSuggestionInput.getText().toString(), "new");
                closeSuggestionInput();
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

    private void loadData() {
        mData = Jitt.getInstance().getDataForString(mString);
        mLangList = new ArrayList<>();
        mItemsList = new ArrayList<>();
        // Create the languages list
        Set<String> langKeySet = mData.keySet();
        mLangList = new ArrayList<>(langKeySet.size());
        Iterator<String> iterator = langKeySet.iterator();
        ColorsList[] colorsList = ColorsList.values();
        while (iterator.hasNext()) {
            String lang = iterator.next();
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
    }

    @Override
    public void onBackPressed() {
        if (mSuggestion.getVisibility() == View.VISIBLE) {
            // This is the enter suggestion mode, leave it
            closeSuggestionInput();
            return;

        }
        super.onBackPressed();
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

    private void closeSuggestionInput() {
        mSuggestion.setVisibility(View.GONE);
        mList.setVisibility(View.VISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSuggestionInput.getWindowToken(), 0);
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
            startActivityForResult(new Intent(this, JittSelectLanguagesActivity.class), RC_SELECT_LANGUAGES);
            return true;
        }  else if (id == R.id.home || id == R.id.homeAsUp || id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class SuggestionsListAdapter extends BaseAdapter implements View.OnClickListener {

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

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        private View getSuggestionEntryView(int position, View convertView, ViewGroup parent) {
            SuggestionEntryViewHolder holder = null;
            if (convertView == null) {
                holder = new SuggestionEntryViewHolder();
                convertView = LayoutInflater.from(JittTranslateActivity.this).inflate(R.layout.translate_entry_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);
                holder.votes = (TextView)convertView.findViewById(R.id.votes);

                holder.voteUp = (ImageView)convertView.findViewById(R.id.vote_up);
                holder.voteDown = (ImageView)convertView.findViewById(R.id.vote_down);
                holder.voteFlag = (ImageView)convertView.findViewById(R.id.vote_flag);
                holder.deleteBtn = (ImageView)convertView.findViewById(R.id.delete_translation);

                holder.voteUp.setOnClickListener(this);
                holder.voteDown.setOnClickListener(this);
                holder.voteFlag.setOnClickListener(this);
                holder.deleteBtn.setOnClickListener(this);

                convertView.setTag(holder);
            } else {
                holder = (SuggestionEntryViewHolder)convertView.getTag();
            }

            SuggestionEntry entry = (SuggestionEntry)mItemsList.get(position);

            String lang = entry.lang;
            int langIndex = mLangList.indexOf(lang);
            holder.votes.setText(String.valueOf(entry.suggestion.votes));
            holder.title.setText(entry.suggestion.suggested);

            // TODO update user action if any
            String userSelection = entry.suggestion.user_selected;

            // user is creator, so she can delete
            if ("new".equals(userSelection)) {
                holder.voteUp.setVisibility(View.GONE);
                holder.voteDown.setVisibility(View.GONE);
                holder.voteFlag.setVisibility(View.GONE);
                if (entry.suggestion.votes <= ServerAPI.INITIAL_VOTES) {
                    holder.deleteBtn.setVisibility(View.VISIBLE);
                }
            } else {
                holder.voteUp.setSelected("up".equals(userSelection));
                holder.voteDown.setSelected("down".equals(userSelection));
                holder.voteFlag.setSelected("flag".equals(userSelection));
            }

            Integer pos = position;
            holder.voteUp.setTag(pos);
            holder.voteDown.setTag(pos);
            holder.voteFlag.setTag(pos);
            holder.deleteBtn.setTag(pos);

            return convertView;
        }

        private View getTitleView(int position, View convertView, ViewGroup parent) {
            TitleViewHolder holder = null;
            if (convertView == null) {
                holder = new TitleViewHolder();
                convertView = LayoutInflater.from(JittTranslateActivity.this).inflate(R.layout.translate_lang_item, parent, false);

                holder.title = (TextView)convertView.findViewById(R.id.name);
                holder.addButton = (ImageView)convertView.findViewById(R.id.add);
                holder.addButton.setOnClickListener(this);

                convertView.setTag(holder);
            } else {
                holder = (TitleViewHolder)convertView.getTag();
            }

            String lang = (String)mItemsList.get(position);

            int langIndex = mLangList.indexOf(lang);

            holder.title.setText(Jitt.getInstance().getLanguageName(lang));
            holder.addButton.setTag(Integer.valueOf(position));
            return convertView;
        }

        @Override
        public void onClick(View v) {
            int viewId = v.getId();
            int pos = (Integer)v.getTag();

            if (viewId == R.id.add) {
                mSuggestion.setVisibility(View.VISIBLE);
                mList.setVisibility(View.GONE);

                String lang = (String)mItemsList.get(pos);
                mSuggestionTitle.setText(Jitt.getInstance().getLanguageName(lang));
                mSuggestionTitle.setTag(lang);
                mSuggestionInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mSuggestionInput, InputMethodManager.SHOW_IMPLICIT);

                // Animate Entrance
                ObjectAnimator yAnim = ObjectAnimator.ofFloat(mSuggestion,"translationY", 100, 0);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(mSuggestion,"alpha", 0f, 1f);

                AnimatorSet set = new AnimatorSet();
                set.playTogether(yAnim, alpha);
                set.setDuration(300);
                set.setInterpolator(new DecelerateInterpolator());
                set.start();
            } else {
                SuggestionEntry entry = (SuggestionEntry) mItemsList.get(pos);
                String action = "";

                if (viewId == R.id.vote_up) {
                    action = "up";
                } else if (viewId == R.id.vote_down) {
                    action = "down";
                } else if (viewId == R.id.vote_flag) {
                    action = "flag";
                } else if (viewId == R.id.delete_translation) {
                    action = "delete";
                }

                Jitt.getInstance().sendUserAction(JittTranslateActivity.this, mString, entry.lang, entry.suggestion.suggested, action);
            }
        }

        private class SuggestionEntryViewHolder {
            public TextView title;
            public TextView votes;
            public ImageView voteUp;
            public ImageView voteDown;
            public ImageView voteFlag;
            public ImageView deleteBtn;
        }

        private class TitleViewHolder {
            public TextView title;
            public ImageView addButton;
        }
    }

    private class SuggestionEntry {
        public ServerAPI.Suggestion suggestion;
        public String lang;
    }
}
