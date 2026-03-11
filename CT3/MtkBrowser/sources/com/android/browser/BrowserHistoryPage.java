package com.android.browser;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.LoaderManager;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.browser.provider.BrowserContract;
import com.mediatek.browser.ext.IBrowserHistoryExt;

public class BrowserHistoryPage extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ExpandableListView.OnChildClickListener {
    HistoryAdapter mAdapter;
    CombinedBookmarksCallbacks mCallback;
    ListView mChildList;
    HistoryChildWrapper mChildWrapper;
    HistoryItem mContextHeader;
    boolean mDisableNewWindow;
    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    ListView mGroupList;
    private ExpandableListView mHistoryList;
    String mMostVisitsLimit;
    private ViewGroup mPrefsContainer;
    private View mRoot;
    private IBrowserHistoryExt mBrowserHistoryExt = null;
    private AdapterView.OnItemClickListener mGroupItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BrowserHistoryPage.this.mChildWrapper.setSelectedGroup(position);
            BrowserHistoryPage.this.mGroupList.setItemChecked(position, true);
        }
    };
    private AdapterView.OnItemClickListener mChildItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BrowserHistoryPage.this.mCallback.openUrl(((HistoryItem) view).getUrl());
        }
    };

    interface HistoryQuery {
        public static final String[] PROJECTION = {"_id", "date", "title", "url", "favicon", "visits", "bookmark"};
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) getActivity().getSystemService("clipboard");
        cm.setText(text);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri.Builder combinedBuilder = BrowserContract.Combined.CONTENT_URI.buildUpon();
        switch (id) {
            case 1:
                CursorLoader loader = new CursorLoader(getActivity(), combinedBuilder.build(), HistoryQuery.PROJECTION, "visits > 0", null, "date DESC");
                return loader;
            case 2:
                Uri uri = combinedBuilder.appendQueryParameter("limit", this.mMostVisitsLimit).build();
                CursorLoader loader2 = new CursorLoader(getActivity(), uri, HistoryQuery.PROJECTION, "visits > 0", null, "visits DESC");
                return loader2;
            default:
                throw new IllegalArgumentException();
        }
    }

    void selectGroup(int position) {
        this.mGroupItemClickListener.onItemClick(null, this.mAdapter.getGroupView(position, false, null, null), position, position);
    }

    void checkIfEmpty() {
        if (this.mAdapter.mMostVisited == null || this.mAdapter.mHistoryCursor == null) {
            return;
        }
        boolean xlarge = BrowserActivity.isTablet(getActivity());
        if (this.mAdapter.isEmpty()) {
            if (xlarge) {
                this.mRoot.findViewById(R.id.tab_history).setVisibility(8);
            } else {
                this.mRoot.findViewById(R.id.history).setVisibility(8);
            }
            this.mRoot.findViewById(android.R.id.empty).setVisibility(0);
            return;
        }
        if (xlarge) {
            this.mRoot.findViewById(R.id.tab_history).setVisibility(0);
        } else {
            this.mRoot.findViewById(R.id.history).setVisibility(0);
        }
        this.mRoot.findViewById(android.R.id.empty).setVisibility(8);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case 1:
                this.mAdapter.changeCursor(data);
                if (!this.mAdapter.isEmpty() && this.mGroupList != null && this.mGroupList.getCheckedItemPosition() == -1) {
                    selectGroup(0);
                }
                checkIfEmpty();
                return;
            case 2:
                this.mAdapter.changeMostVisitedCursor(data);
                checkIfEmpty();
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        this.mDisableNewWindow = args.getBoolean("disable_new_window", false);
        int mvlimit = getResources().getInteger(R.integer.most_visits_limit);
        this.mMostVisitsLimit = Integer.toString(mvlimit);
        this.mCallback = (CombinedBookmarksCallbacks) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mRoot = inflater.inflate(R.layout.history, container, false);
        this.mAdapter = new HistoryAdapter(getActivity());
        ViewStub stub = (ViewStub) this.mRoot.findViewById(R.id.pref_stub);
        if (stub != null) {
            inflateTwoPane(stub);
        } else {
            inflateSinglePane();
        }
        getLoaderManager().restartLoader(1, null, this);
        getLoaderManager().restartLoader(2, null, this);
        return this.mRoot;
    }

    private void inflateSinglePane() {
        this.mHistoryList = (ExpandableListView) this.mRoot.findViewById(R.id.history);
        this.mHistoryList.setAdapter(this.mAdapter);
        this.mHistoryList.setOnChildClickListener(this);
        registerForContextMenu(this.mHistoryList);
    }

    private void inflateTwoPane(ViewStub stub) {
        stub.setLayoutResource(R.layout.preference_list_content);
        stub.inflate();
        this.mGroupList = (ListView) this.mRoot.findViewById(android.R.id.list);
        this.mPrefsContainer = (ViewGroup) this.mRoot.findViewById(R.id.prefs_frame);
        this.mFragmentBreadCrumbs = (FragmentBreadCrumbs) this.mRoot.findViewById(android.R.id.title);
        this.mFragmentBreadCrumbs.setMaxVisible(1);
        this.mFragmentBreadCrumbs.setActivity(getActivity());
        this.mPrefsContainer.setVisibility(0);
        this.mGroupList.setAdapter((ListAdapter) new HistoryGroupWrapper(this.mAdapter));
        this.mGroupList.setOnItemClickListener(this.mGroupItemClickListener);
        this.mGroupList.setChoiceMode(1);
        this.mChildWrapper = new HistoryChildWrapper(this.mAdapter);
        this.mChildList = new ListView(getActivity());
        this.mChildList.setAdapter((ListAdapter) this.mChildWrapper);
        this.mChildList.setOnItemClickListener(this.mChildItemClickListener);
        registerForContextMenu(this.mChildList);
        ViewGroup prefs = (ViewGroup) this.mRoot.findViewById(R.id.prefs);
        prefs.addView(this.mChildList);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        this.mCallback.openUrl(((HistoryItem) view).getUrl());
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(1);
        getLoaderManager().destroyLoader(2);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        this.mBrowserHistoryExt = Extensions.getHistoryPlugin(getActivity());
        this.mBrowserHistoryExt.createHistoryPageOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.mBrowserHistoryExt = Extensions.getHistoryPlugin(getActivity());
        if (this.mBrowserHistoryExt.historyPageOptionsMenuItemSelected(item, getActivity())) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.mBrowserHistoryExt = Extensions.getHistoryPlugin(getActivity());
        this.mBrowserHistoryExt.prepareHistoryPageOptionsMenuItem(menu, this.mAdapter == null, this.mAdapter.isEmpty());
    }

    static class ClearHistoryTask extends Thread {
        ContentResolver mResolver;

        public ClearHistoryTask(ContentResolver resolver) {
            this.mResolver = resolver;
        }

        @Override
        public void run() {
            com.android.browser.provider.Browser.clearHistory(this.mResolver);
            com.android.browser.provider.Browser.clearSearches(this.mResolver);
        }
    }

    View getTargetView(ContextMenu.ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            return ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView;
        }
        if (menuInfo instanceof ExpandableListView.ExpandableListContextMenuInfo) {
            return ((ExpandableListView.ExpandableListContextMenuInfo) menuInfo).targetView;
        }
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        View targetView = getTargetView(menuInfo);
        if (!(targetView instanceof HistoryItem)) {
            return;
        }
        HistoryItem historyItem = (HistoryItem) targetView;
        Activity parent = getActivity();
        MenuInflater inflater = parent.getMenuInflater();
        inflater.inflate(R.menu.historycontext, menu);
        if (this.mContextHeader == null) {
            this.mContextHeader = new HistoryItem(parent, false);
            this.mContextHeader.setEnableScrolling(true);
        } else if (this.mContextHeader.getParent() != null) {
            ((ViewGroup) this.mContextHeader.getParent()).removeView(this.mContextHeader);
        }
        historyItem.copyTo(this.mContextHeader);
        menu.setHeaderView(this.mContextHeader);
        if (this.mDisableNewWindow) {
            menu.findItem(R.id.new_window_context_menu_id).setVisible(false);
        }
        if (historyItem.isBookmark()) {
            MenuItem item = menu.findItem(R.id.save_to_bookmarks_menu_id);
            item.setTitle(R.string.remove_from_bookmarks);
        }
        PackageManager pm = parent.getPackageManager();
        Intent send = new Intent("android.intent.action.SEND");
        send.setType("text/plain");
        ResolveInfo ri = pm.resolveActivity(send, 65536);
        menu.findItem(R.id.share_link_context_menu_id).setVisible(ri != null);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();
        if (menuInfo == null) {
            return false;
        }
        View targetView = getTargetView(menuInfo);
        if (!(targetView instanceof HistoryItem)) {
            return false;
        }
        HistoryItem historyItem = (HistoryItem) targetView;
        String url = historyItem.getUrl();
        String title = historyItem.getName();
        Activity activity = getActivity();
        switch (item.getItemId()) {
            case R.id.open_context_menu_id:
                this.mCallback.openUrl(url);
                break;
            case R.id.new_window_context_menu_id:
                this.mCallback.openInNewTab(url);
                break;
            case R.id.share_link_context_menu_id:
                com.android.browser.provider.Browser.sendString(activity, url, activity.getText(R.string.choosertitle_sharevia).toString());
                break;
            case R.id.copy_url_context_menu_id:
                copy(url);
                break;
            case R.id.delete_context_menu_id:
                com.android.browser.provider.Browser.deleteFromHistory(activity.getContentResolver(), url);
                break;
            case R.id.homepage_context_menu_id:
                BrowserSettings.getInstance().setHomePage(url);
                BrowserSettings.getInstance().setHomePagePicker("other");
                Toast.makeText(activity, R.string.homepage_set, 1).show();
                break;
            case R.id.save_to_bookmarks_menu_id:
                if (historyItem.isBookmark()) {
                    Bookmarks.removeFromBookmarks(activity, activity.getContentResolver(), url, title);
                } else {
                    com.android.browser.provider.Browser.saveBookmark(activity, title, url);
                }
                break;
        }
        return false;
    }

    private static abstract class HistoryWrapper extends BaseAdapter {
        protected HistoryAdapter mAdapter;
        private DataSetObserver mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                HistoryWrapper.this.notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                HistoryWrapper.this.notifyDataSetInvalidated();
            }
        };

        public HistoryWrapper(HistoryAdapter adapter) {
            this.mAdapter = adapter;
            this.mAdapter.registerDataSetObserver(this.mObserver);
        }
    }

    private static class HistoryGroupWrapper extends HistoryWrapper {
        public HistoryGroupWrapper(HistoryAdapter adapter) {
            super(adapter);
        }

        @Override
        public int getCount() {
            return this.mAdapter.getGroupCount();
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
            return this.mAdapter.getGroupView(position, false, convertView, parent);
        }
    }

    private static class HistoryChildWrapper extends HistoryWrapper {
        private int mSelectedGroup;

        public HistoryChildWrapper(HistoryAdapter adapter) {
            super(adapter);
        }

        void setSelectedGroup(int groupPosition) {
            this.mSelectedGroup = groupPosition;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return this.mAdapter.getChildrenCount(this.mSelectedGroup);
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
            return this.mAdapter.getChildView(this.mSelectedGroup, position, false, convertView, parent);
        }
    }

    private class HistoryAdapter extends DateSortedExpandableListAdapter {
        Drawable mFaviconBackground;
        private Cursor mHistoryCursor;
        private Cursor mMostVisited;

        HistoryAdapter(Context context) {
            super(context, 1);
            this.mFaviconBackground = BookmarkUtils.createListFaviconBackground(context);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            this.mHistoryCursor = cursor;
            super.changeCursor(cursor);
        }

        void changeMostVisitedCursor(Cursor cursor) {
            if (this.mMostVisited == cursor) {
                return;
            }
            if (this.mMostVisited != null) {
                this.mMostVisited.unregisterDataSetObserver(this.mDataSetObserver);
                this.mMostVisited.close();
            }
            this.mMostVisited = cursor;
            if (this.mMostVisited != null) {
                this.mMostVisited.registerDataSetObserver(this.mDataSetObserver);
            }
            notifyDataSetChanged();
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            if (moveCursorToChildPosition(groupPosition, childPosition)) {
                Cursor cursor = getCursor(groupPosition);
                return cursor.getLong(0);
            }
            return 0L;
        }

        @Override
        public int getGroupCount() {
            return (!isMostVisitedEmpty() ? 1 : 0) + super.getGroupCount();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (groupPosition >= super.getGroupCount()) {
                if (isMostVisitedEmpty()) {
                    return 0;
                }
                return this.mMostVisited.getCount();
            }
            return super.getChildrenCount(groupPosition);
        }

        @Override
        public boolean isEmpty() {
            if (!super.isEmpty()) {
                return false;
            }
            return isMostVisitedEmpty();
        }

        private boolean isMostVisitedEmpty() {
            return this.mMostVisited == null || this.mMostVisited.isClosed() || this.mMostVisited.getCount() == 0;
        }

        Cursor getCursor(int groupPosition) {
            if (groupPosition >= super.getGroupCount()) {
                return this.mMostVisited;
            }
            return this.mHistoryCursor;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView item;
            if (groupPosition >= super.getGroupCount()) {
                if (this.mMostVisited == null || this.mMostVisited.isClosed()) {
                    throw new IllegalStateException("Data is not valid");
                }
                if (convertView == null || !(convertView instanceof TextView)) {
                    LayoutInflater factory = LayoutInflater.from(getContext());
                    item = (TextView) factory.inflate(R.layout.history_header, (ViewGroup) null);
                } else {
                    item = (TextView) convertView;
                }
                item.setText(R.string.tab_most_visited);
                return item;
            }
            return super.getGroupView(groupPosition, isExpanded, convertView, parent);
        }

        @Override
        boolean moveCursorToChildPosition(int groupPosition, int childPosition) {
            if (groupPosition >= super.getGroupCount()) {
                if (this.mMostVisited != null && !this.mMostVisited.isClosed()) {
                    this.mMostVisited.moveToPosition(childPosition);
                    return true;
                }
                return false;
            }
            return super.moveCursorToChildPosition(groupPosition, childPosition);
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            HistoryItem item;
            if (convertView == null || !(convertView instanceof HistoryItem)) {
                item = new HistoryItem(getContext());
                item.setPadding(item.getPaddingLeft() + 10, item.getPaddingTop(), item.getPaddingRight(), item.getPaddingBottom());
                item.setFaviconBackground(this.mFaviconBackground);
            } else {
                item = (HistoryItem) convertView;
            }
            if (!moveCursorToChildPosition(groupPosition, childPosition)) {
                return item;
            }
            Cursor cursor = getCursor(groupPosition);
            item.setName(cursor.getString(2));
            String url = cursor.getString(3);
            item.setUrl(url);
            byte[] data = cursor.getBlob(4);
            if (data != null) {
                item.setFavicon(BitmapFactory.decodeByteArray(data, 0, data.length));
            } else {
                item.setFavicon(null);
            }
            item.setIsBookmark(cursor.getInt(6) == 1);
            return item;
        }
    }
}
