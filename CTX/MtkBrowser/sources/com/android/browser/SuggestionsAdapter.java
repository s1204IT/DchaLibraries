package com.android.browser;

import android.content.ContentResolver;
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
import java.util.Iterator;
import java.util.List;

public class SuggestionsAdapter extends BaseAdapter implements View.OnClickListener, Filterable {
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
    final Filter mFilter = new SuggestFilter(this);

    class CombinedCursor extends CursorSource {
        final SuggestionsAdapter this$0;

        CombinedCursor(SuggestionsAdapter suggestionsAdapter) {
            super(suggestionsAdapter);
            this.this$0 = suggestionsAdapter;
        }

        private String getTitle(String str, String str2) {
            return (TextUtils.isEmpty(str) || TextUtils.getTrimmedLength(str) == 0) ? UrlUtils.stripUrl(str2) : str;
        }

        private String getUrl(String str, String str2) {
            if (TextUtils.isEmpty(str) || TextUtils.getTrimmedLength(str) == 0 || str.equals(str2)) {
                return null;
            }
            return UrlUtils.stripUrl(str2);
        }

        @Override
        public SuggestItem getItem() {
            if (this.mCursor == null || this.mCursor.isAfterLast()) {
                return null;
            }
            String string = this.mCursor.getString(1);
            String string2 = this.mCursor.getString(2);
            return new SuggestItem(this.this$0, getTitle(string, string2), getUrl(string, string2), (this.mCursor.getInt(3) != 1 ? 0 : 1) ^ 1);
        }

        @Override
        public void runQuery(CharSequence charSequence) {
            String str;
            String[] strArr;
            if (this.mCursor != null) {
                this.mCursor.close();
            }
            String str2 = ((Object) charSequence) + "%";
            if (str2.startsWith("http") || str2.startsWith("file")) {
                str = "url LIKE ?";
                strArr = new String[]{str2};
            } else {
                str = "(url LIKE ? OR url LIKE ? OR url LIKE ? OR url LIKE ? OR title LIKE ?)";
                strArr = new String[]{"http://" + str2, "http://www." + str2, "https://" + str2, "https://www." + str2, str2};
            }
            Uri.Builder builderBuildUpon = BrowserProvider2.OmniboxSuggestions.CONTENT_URI.buildUpon();
            builderBuildUpon.appendQueryParameter("limit", Integer.toString(Math.max(this.this$0.mLinesLandscape, this.this$0.mLinesPortrait)));
            ContentResolver contentResolver = this.this$0.mContext.getContentResolver();
            Uri uriBuild = builderBuildUpon.build();
            String[] strArr2 = SuggestionsAdapter.COMBINED_PROJECTION;
            if (charSequence == null) {
                strArr = null;
            }
            this.mCursor = contentResolver.query(uriBuild, strArr2, str, strArr, null);
            if (this.mCursor != null) {
                this.mCursor.moveToFirst();
            }
        }
    }

    interface CompletionListener {
        void onSearch(String str);

        void onSelect(String str, int i, String str2);
    }

    abstract class CursorSource {
        Cursor mCursor;
        final SuggestionsAdapter this$0;

        CursorSource(SuggestionsAdapter suggestionsAdapter) {
            this.this$0 = suggestionsAdapter;
        }

        public void close() {
            if (this.mCursor != null) {
                this.mCursor.close();
            }
        }

        public int getCount() {
            if (this.mCursor != null) {
                return this.mCursor.getCount();
            }
            return 0;
        }

        public abstract SuggestItem getItem();

        boolean moveToNext() {
            return this.mCursor.moveToNext();
        }

        public abstract void runQuery(CharSequence charSequence);
    }

    class SlowFilterTask extends AsyncTask<CharSequence, Void, List<SuggestItem>> {
        final SuggestionsAdapter this$0;

        SlowFilterTask(SuggestionsAdapter suggestionsAdapter) {
            this.this$0 = suggestionsAdapter;
        }

        @Override
        public List<SuggestItem> doInBackground(CharSequence... charSequenceArr) {
            SuggestCursor suggestCursor = new SuggestCursor(this.this$0);
            suggestCursor.runQuery(charSequenceArr[0]);
            ArrayList arrayList = new ArrayList();
            int count = suggestCursor.getCount();
            for (int i = 0; i < count; i++) {
                arrayList.add(suggestCursor.getItem());
                suggestCursor.moveToNext();
            }
            suggestCursor.close();
            return arrayList;
        }

        @Override
        public void onPostExecute(List<SuggestItem> list) {
            this.this$0.mSuggestResults = list;
            this.this$0.mMixedResults = this.this$0.buildSuggestionResults();
            this.this$0.notifyDataSetChanged();
        }
    }

    class SuggestCursor extends CursorSource {
        final SuggestionsAdapter this$0;

        SuggestCursor(SuggestionsAdapter suggestionsAdapter) {
            super(suggestionsAdapter);
            this.this$0 = suggestionsAdapter;
        }

        @Override
        public SuggestItem getItem() {
            if (this.mCursor == null || this.mCursor.isClosed()) {
                return null;
            }
            String string = this.mCursor.getString(this.mCursor.getColumnIndex("suggest_text_1"));
            this.mCursor.getString(this.mCursor.getColumnIndex("suggest_text_2"));
            String string2 = this.mCursor.getString(this.mCursor.getColumnIndex("suggest_text_2_url"));
            this.mCursor.getString(this.mCursor.getColumnIndex("suggest_intent_data"));
            SuggestItem suggestItem = new SuggestItem(this.this$0, string, string2, TextUtils.isEmpty(string2) ? 4 : 2);
            suggestItem.extra = this.mCursor.getString(this.mCursor.getColumnIndex("suggest_intent_extra_data"));
            return suggestItem;
        }

        @Override
        public void runQuery(CharSequence charSequence) {
            if (this.mCursor != null) {
                this.mCursor.close();
            }
            SearchEngine searchEngine = this.this$0.mSettings.getSearchEngine();
            if (TextUtils.isEmpty(charSequence)) {
                if (searchEngine.wantsEmptyQuery()) {
                    this.mCursor = searchEngine.getSuggestions(this.this$0.mContext, "");
                }
                this.mCursor = null;
            } else {
                if (searchEngine == null || !searchEngine.supportsSuggestions()) {
                    return;
                }
                this.mCursor = searchEngine.getSuggestions(this.this$0.mContext, charSequence.toString());
                if (this.mCursor != null) {
                    this.mCursor.moveToFirst();
                }
            }
        }
    }

    class SuggestFilter extends Filter {
        final SuggestionsAdapter this$0;

        SuggestFilter(SuggestionsAdapter suggestionsAdapter) {
            this.this$0 = suggestionsAdapter;
        }

        private boolean shouldProcessEmptyQuery() {
            return this.this$0.mSettings.getSearchEngine().wantsEmptyQuery();
        }

        @Override
        public CharSequence convertResultToString(Object obj) {
            if (obj == null) {
                return "";
            }
            SuggestItem suggestItem = (SuggestItem) obj;
            return suggestItem.title != null ? suggestItem.title : suggestItem.url;
        }

        void mixResults(List<SuggestItem> list) {
            int maxLines = this.this$0.getMaxLines();
            for (int i = 0; i < this.this$0.mSources.size(); i++) {
                CursorSource cursorSource = this.this$0.mSources.get(i);
                int iMin = Math.min(cursorSource.getCount(), maxLines);
                maxLines -= iMin;
                for (int i2 = 0; i2 < iMin; i2++) {
                    list.add(cursorSource.getItem());
                    cursorSource.moveToNext();
                }
                cursorSource.close();
            }
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (TextUtils.isEmpty(charSequence) && !shouldProcessEmptyQuery()) {
                filterResults.count = 0;
                filterResults.values = null;
                return filterResults;
            }
            startSuggestionsAsync(charSequence);
            ArrayList arrayList = new ArrayList();
            if (charSequence != null) {
                Iterator<CursorSource> it = this.this$0.mSources.iterator();
                while (it.hasNext()) {
                    it.next().runQuery(charSequence);
                }
                mixResults(arrayList);
            }
            synchronized (this.this$0.mResultsLock) {
                this.this$0.mFilterResults = arrayList;
            }
            SuggestionResults suggestionResultsBuildSuggestionResults = this.this$0.buildSuggestionResults();
            filterResults.count = suggestionResultsBuildSuggestionResults.getLineCount();
            filterResults.values = suggestionResultsBuildSuggestionResults;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            if (filterResults.values instanceof SuggestionResults) {
                this.this$0.mMixedResults = (SuggestionResults) filterResults.values;
                this.this$0.notifyDataSetChanged();
            }
        }

        void startSuggestionsAsync(CharSequence charSequence) {
            if (this.this$0.mIncognitoMode) {
                return;
            }
            new SlowFilterTask(this.this$0).execute(charSequence);
        }
    }

    public class SuggestItem {
        public String extra;
        final SuggestionsAdapter this$0;
        public String title;
        public int type;
        public String url;

        public SuggestItem(SuggestionsAdapter suggestionsAdapter, String str, String str2, int i) {
            this.this$0 = suggestionsAdapter;
            this.title = str;
            this.url = str2;
            this.type = i;
        }
    }

    class SuggestionResults {
        final SuggestionsAdapter this$0;
        ArrayList<SuggestItem> items = new ArrayList<>(24);
        int[] counts = new int[5];

        SuggestionResults(SuggestionsAdapter suggestionsAdapter) {
            this.this$0 = suggestionsAdapter;
        }

        void addResult(SuggestItem suggestItem) {
            int i;
            int i2 = 0;
            while (true) {
                i = i2;
                if (i >= this.items.size() || suggestItem.type < this.items.get(i).type) {
                    break;
                } else {
                    i2 = i + 1;
                }
            }
            this.items.add(i, suggestItem);
            int[] iArr = this.counts;
            int i3 = suggestItem.type;
            iArr[i3] = iArr[i3] + 1;
        }

        int getLineCount() {
            return Math.min(this.this$0.mLandscapeMode ? this.this$0.mLinesLandscape : this.this$0.mLinesPortrait, this.items.size());
        }

        public String toString() {
            if (this.items == null) {
                return null;
            }
            if (this.items.size() == 0) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= this.items.size()) {
                    return sb.toString();
                }
                SuggestItem suggestItem = this.items.get(i2);
                sb.append(suggestItem.type + ": " + suggestItem.title);
                if (i2 < this.items.size() - 1) {
                    sb.append(", ");
                }
                i = i2 + 1;
            }
        }
    }

    public SuggestionsAdapter(Context context, CompletionListener completionListener) {
        this.mContext = context;
        this.mListener = completionListener;
        this.mLinesPortrait = this.mContext.getResources().getInteger(2131623937);
        this.mLinesLandscape = this.mContext.getResources().getInteger(2131623936);
        addSource(new CombinedCursor(this));
    }

    private void bindView(View view, SuggestItem suggestItem) {
        int i;
        view.setTag(suggestItem);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
        ImageView imageView = (ImageView) view.findViewById(2131558478);
        View viewFindViewById = view.findViewById(2131558520);
        View viewFindViewById2 = view.findViewById(2131558431);
        textView.setText(Html.fromHtml(suggestItem.title));
        if (TextUtils.isEmpty(suggestItem.url)) {
            textView2.setVisibility(8);
            textView.setMaxLines(2);
        } else {
            textView2.setVisibility(0);
            textView2.setText(suggestItem.url);
            textView.setMaxLines(1);
        }
        switch (suggestItem.type) {
            case 0:
                i = 2130837580;
                break;
            case 1:
                i = 2130837582;
                break;
            case 2:
                i = 2130837581;
                break;
            case 3:
            case 4:
                i = 2130837583;
                break;
            default:
                i = -1;
                break;
        }
        if (i != -1) {
            imageView.setImageDrawable(this.mContext.getResources().getDrawable(i));
        }
        viewFindViewById.setVisibility((4 == suggestItem.type || 3 == suggestItem.type) ? 0 : 8);
        viewFindViewById2.setVisibility(viewFindViewById.getVisibility());
        viewFindViewById.setOnClickListener(this);
        view.findViewById(2131558519).setOnClickListener(this);
    }

    public int getMaxLines() {
        return (int) Math.ceil(((double) (this.mLandscapeMode ? this.mLinesLandscape : this.mLinesPortrait)) / 2.0d);
    }

    static String getSuggestionTitle(SuggestItem suggestItem) {
        if (suggestItem.title != null) {
            return Html.fromHtml(suggestItem.title).toString();
        }
        return null;
    }

    static String getSuggestionUrl(SuggestItem suggestItem) {
        return TextUtils.isEmpty(suggestItem.url) ? getSuggestionTitle(suggestItem) : suggestItem.url;
    }

    public void addSource(CursorSource cursorSource) {
        if (this.mSources == null) {
            this.mSources = new ArrayList(5);
        }
        this.mSources.add(cursorSource);
    }

    SuggestionResults buildSuggestionResults() {
        List<SuggestItem> list;
        List<SuggestItem> list2;
        SuggestionResults suggestionResults = new SuggestionResults(this);
        synchronized (this.mResultsLock) {
            list = this.mFilterResults;
            list2 = this.mSuggestResults;
        }
        if (list != null) {
            Iterator<SuggestItem> it = list.iterator();
            while (it.hasNext()) {
                suggestionResults.addResult(it.next());
            }
        }
        if (list2 != null) {
            Iterator<SuggestItem> it2 = list2.iterator();
            while (it2.hasNext()) {
                suggestionResults.addResult(it2.next());
            }
        }
        return suggestionResults;
    }

    public void clearCache() {
        this.mFilterResults = null;
        this.mSuggestResults = null;
        notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        if (this.mMixedResults == null) {
            return 0;
        }
        return this.mMixedResults.getLineCount();
    }

    @Override
    public Filter getFilter() {
        return this.mFilter;
    }

    @Override
    public SuggestItem getItem(int i) {
        if (this.mMixedResults == null) {
            return null;
        }
        return this.mMixedResults.items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(this.mContext);
        if (view == null) {
            view = layoutInflaterFrom.inflate(2130968626, viewGroup, false);
        }
        bindView(view, getItem(i));
        return view;
    }

    @Override
    public void onClick(View view) {
        SuggestItem suggestItem = (SuggestItem) ((View) view.getParent()).getTag();
        if (2131558520 == view.getId()) {
            this.mListener.onSearch(getSuggestionUrl(suggestItem));
        } else {
            this.mListener.onSelect(getSuggestionUrl(suggestItem), suggestItem.type, suggestItem.extra);
        }
    }

    public void setIncognitoMode(boolean z) {
        this.mIncognitoMode = z;
        clearCache();
    }

    public void setLandscapeMode(boolean z) {
        this.mLandscapeMode = z;
        notifyDataSetChanged();
    }
}
