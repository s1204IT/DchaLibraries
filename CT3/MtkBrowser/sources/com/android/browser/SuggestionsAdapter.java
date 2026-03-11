package com.android.browser;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.browser.provider.BrowserProvider2;
import com.android.browser.search.SearchEngine;
import java.util.ArrayList;
import java.util.List;

public class SuggestionsAdapter extends BaseAdapter implements Filterable, View.OnClickListener {
    private static final String[] COMBINED_PROJECTION = {"_id", "title", "url", "bookmark"};
    final Context mContext;
    List<SuggestItem> mFilterResults;
    boolean mIncognitoMode;
    boolean mLandscapeMode;
    final int mLinesLandscape;
    final int mLinesPortrait;
    final CompletionListener mListener;
    SuggestionResults mMixedResults;
    List<CursorSource> mSources;
    List<SuggestItem> mSuggestResults;
    final Object mResultsLock = new Object();
    BrowserSettings mSettings = BrowserSettings.getInstance();
    final Filter mFilter = new SuggestFilter();

    interface CompletionListener {
        void onSearch(String str);

        void onSelect(String str, int i, String str2);
    }

    public SuggestionsAdapter(Context ctx, CompletionListener listener) {
        this.mContext = ctx;
        this.mListener = listener;
        this.mLinesPortrait = this.mContext.getResources().getInteger(R.integer.max_suggest_lines_portrait);
        this.mLinesLandscape = this.mContext.getResources().getInteger(R.integer.max_suggest_lines_landscape);
        addSource(new CombinedCursor());
    }

    public void setLandscapeMode(boolean mode) {
        this.mLandscapeMode = mode;
        notifyDataSetChanged();
    }

    public void addSource(CursorSource c) {
        if (this.mSources == null) {
            this.mSources = new ArrayList(5);
        }
        this.mSources.add(c);
    }

    @Override
    public void onClick(View v) {
        SuggestItem item = (SuggestItem) ((View) v.getParent()).getTag();
        if (R.id.icon2 == v.getId()) {
            this.mListener.onSearch(getSuggestionUrl(item));
        } else {
            this.mListener.onSelect(getSuggestionUrl(item), item.type, item.extra);
        }
    }

    @Override
    public Filter getFilter() {
        return this.mFilter;
    }

    @Override
    public int getCount() {
        if (this.mMixedResults == null) {
            return 0;
        }
        return this.mMixedResults.getLineCount();
    }

    @Override
    public SuggestItem getItem(int position) {
        if (this.mMixedResults == null) {
            return null;
        }
        return this.mMixedResults.items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        View view = convertView;
        if (convertView == null) {
            view = inflater.inflate(R.layout.suggestion_item, parent, false);
        }
        bindView(view, getItem(position));
        return view;
    }

    private void bindView(View view, SuggestItem item) {
        int id;
        view.setTag(item);
        TextView tv1 = (TextView) view.findViewById(android.R.id.text1);
        TextView tv2 = (TextView) view.findViewById(android.R.id.text2);
        ImageView ic1 = (ImageView) view.findViewById(R.id.icon1);
        View ic2 = view.findViewById(R.id.icon2);
        View div = view.findViewById(R.id.divider);
        tv1.setText(Html.fromHtml(item.title));
        if (TextUtils.isEmpty(item.url)) {
            tv2.setVisibility(8);
            tv1.setMaxLines(2);
        } else {
            tv2.setVisibility(0);
            tv2.setText(item.url);
            tv1.setMaxLines(1);
        }
        switch (item.type) {
            case 0:
                id = R.drawable.ic_search_category_bookmark;
                break;
            case 1:
                id = R.drawable.ic_search_category_history;
                break;
            case 2:
                id = R.drawable.ic_search_category_browser;
                break;
            case 3:
            case 4:
                id = R.drawable.ic_search_category_suggest;
                break;
            default:
                id = -1;
                break;
        }
        if (id != -1) {
            ic1.setImageDrawable(this.mContext.getResources().getDrawable(id));
        }
        ic2.setVisibility((4 == item.type || 3 == item.type) ? 0 : 8);
        div.setVisibility(ic2.getVisibility());
        ic2.setOnClickListener(this);
        view.findViewById(R.id.suggestion).setOnClickListener(this);
    }

    class SlowFilterTask extends AsyncTask<CharSequence, Void, List<SuggestItem>> {
        SlowFilterTask() {
        }

        @Override
        public List<SuggestItem> doInBackground(CharSequence... params) {
            SuggestCursor cursor = SuggestionsAdapter.this.new SuggestCursor();
            cursor.runQuery(params[0]);
            List<SuggestItem> results = new ArrayList<>();
            int count = cursor.getCount();
            for (int i = 0; i < count; i++) {
                results.add(cursor.getItem());
                cursor.moveToNext();
            }
            cursor.close();
            return results;
        }

        @Override
        public void onPostExecute(List<SuggestItem> items) {
            SuggestionsAdapter.this.mSuggestResults = items;
            SuggestionsAdapter.this.mMixedResults = SuggestionsAdapter.this.buildSuggestionResults();
            SuggestionsAdapter.this.notifyDataSetChanged();
        }
    }

    SuggestionResults buildSuggestionResults() {
        List<SuggestItem> filter;
        List<SuggestItem> suggest;
        SuggestionResults mixed = new SuggestionResults();
        synchronized (this.mResultsLock) {
            filter = this.mFilterResults;
            suggest = this.mSuggestResults;
        }
        if (filter != null) {
            for (SuggestItem item : filter) {
                mixed.addResult(item);
            }
        }
        if (suggest != null) {
            for (SuggestItem item2 : suggest) {
                mixed.addResult(item2);
            }
        }
        return mixed;
    }

    class SuggestFilter extends Filter {
        SuggestFilter() {
        }

        @Override
        public CharSequence convertResultToString(Object item) {
            if (item == null) {
                return "";
            }
            SuggestItem sitem = (SuggestItem) item;
            if (sitem.title != null) {
                return sitem.title;
            }
            return sitem.url;
        }

        void startSuggestionsAsync(CharSequence constraint) {
            if (SuggestionsAdapter.this.mIncognitoMode) {
                return;
            }
            SuggestionsAdapter.this.new SlowFilterTask().execute(constraint);
        }

        private boolean shouldProcessEmptyQuery() {
            SearchEngine searchEngine = SuggestionsAdapter.this.mSettings.getSearchEngine();
            return searchEngine.wantsEmptyQuery();
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            Filter.FilterResults res = new Filter.FilterResults();
            if (TextUtils.isEmpty(constraint) && !shouldProcessEmptyQuery()) {
                res.count = 0;
                res.values = null;
                return res;
            }
            startSuggestionsAsync(constraint);
            List<SuggestItem> filterResults = new ArrayList<>();
            if (constraint != null) {
                for (CursorSource sc : SuggestionsAdapter.this.mSources) {
                    sc.runQuery(constraint);
                }
                mixResults(filterResults);
            }
            synchronized (SuggestionsAdapter.this.mResultsLock) {
                SuggestionsAdapter.this.mFilterResults = filterResults;
            }
            SuggestionResults mixed = SuggestionsAdapter.this.buildSuggestionResults();
            res.count = mixed.getLineCount();
            res.values = mixed;
            return res;
        }

        void mixResults(List<SuggestItem> results) {
            int maxLines = SuggestionsAdapter.this.getMaxLines();
            for (int i = 0; i < SuggestionsAdapter.this.mSources.size(); i++) {
                CursorSource s = SuggestionsAdapter.this.mSources.get(i);
                int n = Math.min(s.getCount(), maxLines);
                maxLines -= n;
                for (int j = 0; j < n; j++) {
                    results.add(s.getItem());
                    s.moveToNext();
                }
                s.close();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults fresults) {
            if (!(fresults.values instanceof SuggestionResults)) {
                return;
            }
            SuggestionsAdapter.this.mMixedResults = (SuggestionResults) fresults.values;
            SuggestionsAdapter.this.notifyDataSetChanged();
        }
    }

    public int getMaxLines() {
        int maxLines = this.mLandscapeMode ? this.mLinesLandscape : this.mLinesPortrait;
        return (int) Math.ceil(((double) maxLines) / 2.0d);
    }

    class SuggestionResults {
        ArrayList<SuggestItem> items = new ArrayList<>(24);
        int[] counts = new int[5];

        SuggestionResults() {
        }

        void addResult(SuggestItem item) {
            int ix = 0;
            while (ix < this.items.size() && item.type >= this.items.get(ix).type) {
                ix++;
            }
            this.items.add(ix, item);
            int[] iArr = this.counts;
            int i = item.type;
            iArr[i] = iArr[i] + 1;
        }

        int getLineCount() {
            return Math.min(SuggestionsAdapter.this.mLandscapeMode ? SuggestionsAdapter.this.mLinesLandscape : SuggestionsAdapter.this.mLinesPortrait, this.items.size());
        }

        public String toString() {
            if (this.items == null) {
                return null;
            }
            if (this.items.size() == 0) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.items.size(); i++) {
                SuggestItem item = this.items.get(i);
                sb.append(item.type).append(": ").append(item.title);
                if (i < this.items.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }

    public class SuggestItem {
        public String extra;
        public String title;
        public int type;
        public String url;

        public SuggestItem(String text, String u, int t) {
            this.title = text;
            this.url = u;
            this.type = t;
        }
    }

    abstract class CursorSource {
        Cursor mCursor;

        public abstract SuggestItem getItem();

        public abstract void runQuery(CharSequence charSequence);

        CursorSource() {
        }

        boolean moveToNext() {
            return this.mCursor.moveToNext();
        }

        public int getCount() {
            if (this.mCursor != null) {
                return this.mCursor.getCount();
            }
            return 0;
        }

        public void close() {
            if (this.mCursor == null) {
                return;
            }
            this.mCursor.close();
        }
    }

    class CombinedCursor extends CursorSource {
        CombinedCursor() {
            super();
        }

        @Override
        public SuggestItem getItem() {
            if (this.mCursor == null || this.mCursor.isAfterLast()) {
                return null;
            }
            String title = this.mCursor.getString(1);
            String url = this.mCursor.getString(2);
            boolean isBookmark = this.mCursor.getInt(3) == 1;
            return SuggestionsAdapter.this.new SuggestItem(getTitle(title, url), getUrl(title, url), isBookmark ? 0 : 1);
        }

        @Override
        public void runQuery(CharSequence constraint) {
            String[] args;
            String selection;
            if (this.mCursor != null) {
                this.mCursor.close();
            }
            String like = constraint + "%";
            if (like.startsWith("http") || like.startsWith("file")) {
                args = new String[]{like};
                selection = "url LIKE ?";
            } else {
                args = new String[]{"http://" + like, "http://www." + like, "https://" + like, "https://www." + like, like};
                selection = "(url LIKE ? OR url LIKE ? OR url LIKE ? OR url LIKE ? OR title LIKE ?)";
            }
            Uri.Builder ub = BrowserProvider2.OmniboxSuggestions.CONTENT_URI.buildUpon();
            ub.appendQueryParameter("limit", Integer.toString(Math.max(SuggestionsAdapter.this.mLinesLandscape, SuggestionsAdapter.this.mLinesPortrait)));
            this.mCursor = SuggestionsAdapter.this.mContext.getContentResolver().query(ub.build(), SuggestionsAdapter.COMBINED_PROJECTION, selection, constraint != null ? args : null, null);
            if (this.mCursor == null) {
                return;
            }
            this.mCursor.moveToFirst();
        }

        private String getTitle(String title, String url) {
            if (TextUtils.isEmpty(title) || TextUtils.getTrimmedLength(title) == 0) {
                return UrlUtils.stripUrl(url);
            }
            return title;
        }

        private String getUrl(String title, String url) {
            if (TextUtils.isEmpty(title) || TextUtils.getTrimmedLength(title) == 0 || title.equals(url)) {
                return null;
            }
            return UrlUtils.stripUrl(url);
        }
    }

    class SuggestCursor extends CursorSource {
        SuggestCursor() {
            super();
        }

        @Override
        public SuggestItem getItem() {
            if (this.mCursor == null) {
                return null;
            }
            String title = this.mCursor.getString(this.mCursor.getColumnIndex("suggest_text_1"));
            this.mCursor.getString(this.mCursor.getColumnIndex("suggest_text_2"));
            String url = this.mCursor.getString(this.mCursor.getColumnIndex("suggest_text_2_url"));
            this.mCursor.getString(this.mCursor.getColumnIndex("suggest_intent_data"));
            int type = TextUtils.isEmpty(url) ? 4 : 2;
            SuggestItem item = SuggestionsAdapter.this.new SuggestItem(title, url, type);
            item.extra = this.mCursor.getString(this.mCursor.getColumnIndex("suggest_intent_extra_data"));
            return item;
        }

        @Override
        public void runQuery(CharSequence constraint) {
            if (this.mCursor != null) {
                this.mCursor.close();
            }
            SearchEngine searchEngine = SuggestionsAdapter.this.mSettings.getSearchEngine();
            if (!TextUtils.isEmpty(constraint)) {
                if (searchEngine == null || !searchEngine.supportsSuggestions()) {
                    return;
                }
                this.mCursor = searchEngine.getSuggestions(SuggestionsAdapter.this.mContext, constraint.toString());
                if (this.mCursor == null) {
                    return;
                }
                this.mCursor.moveToFirst();
                return;
            }
            if (searchEngine.wantsEmptyQuery()) {
                this.mCursor = searchEngine.getSuggestions(SuggestionsAdapter.this.mContext, "");
            }
            this.mCursor = null;
        }
    }

    public void clearCache() {
        this.mFilterResults = null;
        this.mSuggestResults = null;
        notifyDataSetInvalidated();
    }

    public void setIncognitoMode(boolean incognito) {
        this.mIncognitoMode = incognito;
        clearCache();
    }

    static String getSuggestionTitle(SuggestItem item) {
        if (item.title != null) {
            return Html.fromHtml(item.title).toString();
        }
        return null;
    }

    static String getSuggestionUrl(SuggestItem item) {
        String title = getSuggestionTitle(item);
        if (TextUtils.isEmpty(item.url)) {
            return title;
        }
        return item.url;
    }
}
