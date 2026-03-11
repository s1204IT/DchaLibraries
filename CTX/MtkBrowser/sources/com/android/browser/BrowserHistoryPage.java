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
    private AdapterView.OnItemClickListener mGroupItemClickListener = new AdapterView.OnItemClickListener(this) {
        final BrowserHistoryPage this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            this.this$0.mChildWrapper.setSelectedGroup(i);
            this.this$0.mGroupList.setItemChecked(i, true);
        }
    };
    private AdapterView.OnItemClickListener mChildItemClickListener = new AdapterView.OnItemClickListener(this) {
        final BrowserHistoryPage this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            this.this$0.mCallback.openUrl(((HistoryItem) view).getUrl());
        }
    };

    static class ClearHistoryTask extends Thread {
        ContentResolver mResolver;

        public ClearHistoryTask(ContentResolver contentResolver) {
            this.mResolver = contentResolver;
        }

        @Override
        public void run() throws Throwable {
            com.android.browser.provider.Browser.clearHistory(this.mResolver);
            com.android.browser.provider.Browser.clearSearches(this.mResolver);
        }
    }

    private class HistoryAdapter extends DateSortedExpandableListAdapter {
        Drawable mFaviconBackground;
        private Cursor mHistoryCursor;
        private Cursor mMostVisited;
        final BrowserHistoryPage this$0;

        HistoryAdapter(BrowserHistoryPage browserHistoryPage, Context context) {
            super(context, 1);
            this.this$0 = browserHistoryPage;
            this.mFaviconBackground = BookmarkUtils.createListFaviconBackground(context);
        }

        private boolean isMostVisitedEmpty() {
            return this.mMostVisited == null || this.mMostVisited.isClosed() || this.mMostVisited.getCount() == 0;
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
        public long getChildId(int i, int i2) {
            if (moveCursorToChildPosition(i, i2)) {
                return getCursor(i).getLong(0);
            }
            return 0L;
        }

        @Override
        public View getChildView(int i, int i2, boolean z, View view, ViewGroup viewGroup) {
            HistoryItem historyItem;
            if (view == null || !(view instanceof HistoryItem)) {
                historyItem = new HistoryItem(getContext());
                historyItem.setPadding(historyItem.getPaddingLeft() + 10, historyItem.getPaddingTop(), historyItem.getPaddingRight(), historyItem.getPaddingBottom());
                historyItem.setFaviconBackground(this.mFaviconBackground);
            } else {
                historyItem = (HistoryItem) view;
            }
            if (moveCursorToChildPosition(i, i2)) {
                Cursor cursor = getCursor(i);
                historyItem.setName(cursor.getString(2));
                historyItem.setUrl(cursor.getString(3));
                byte[] blob = cursor.getBlob(4);
                if (blob != null) {
                    historyItem.setFavicon(BitmapFactory.decodeByteArray(blob, 0, blob.length));
                } else {
                    historyItem.setFavicon(null);
                }
                historyItem.setIsBookmark(cursor.getInt(6) == 1);
            }
            return historyItem;
        }

        @Override
        public int getChildrenCount(int i) {
            if (i < super.getGroupCount()) {
                return super.getChildrenCount(i);
            }
            if (isMostVisitedEmpty()) {
                return 0;
            }
            return this.mMostVisited.getCount();
        }

        Cursor getCursor(int i) {
            return i >= super.getGroupCount() ? this.mMostVisited : this.mHistoryCursor;
        }

        @Override
        public int getGroupCount() {
            return super.getGroupCount() + (!isMostVisitedEmpty());
        }

        @Override
        public View getGroupView(int i, boolean z, View view, ViewGroup viewGroup) {
            if (i < super.getGroupCount()) {
                return super.getGroupView(i, z, view, viewGroup);
            }
            if (this.mMostVisited == null || this.mMostVisited.isClosed()) {
                throw new IllegalStateException("Data is not valid");
            }
            TextView textView = (view == null || !(view instanceof TextView)) ? (TextView) LayoutInflater.from(getContext()).inflate(2130968604, (ViewGroup) null) : (TextView) view;
            textView.setText(2131492952);
            return textView;
        }

        @Override
        public boolean isEmpty() {
            if (super.isEmpty()) {
                return isMostVisitedEmpty();
            }
            return false;
        }

        @Override
        boolean moveCursorToChildPosition(int i, int i2) {
            if (i < super.getGroupCount()) {
                return super.moveCursorToChildPosition(i, i2);
            }
            if (this.mMostVisited == null || this.mMostVisited.isClosed()) {
                return false;
            }
            this.mMostVisited.moveToPosition(i2);
            return true;
        }
    }

    private static class HistoryChildWrapper extends HistoryWrapper {
        private int mSelectedGroup;

        public HistoryChildWrapper(HistoryAdapter historyAdapter) {
            super(historyAdapter);
        }

        @Override
        public int getCount() {
            return this.mAdapter.getChildrenCount(this.mSelectedGroup);
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return this.mAdapter.getChildView(this.mSelectedGroup, i, false, view, viewGroup);
        }

        void setSelectedGroup(int i) {
            this.mSelectedGroup = i;
            notifyDataSetChanged();
        }
    }

    private static class HistoryGroupWrapper extends HistoryWrapper {
        public HistoryGroupWrapper(HistoryAdapter historyAdapter) {
            super(historyAdapter);
        }

        @Override
        public int getCount() {
            return this.mAdapter.getGroupCount();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return this.mAdapter.getGroupView(i, false, view, viewGroup);
        }
    }

    interface HistoryQuery {
        public static final String[] PROJECTION = {"_id", "date", "title", "url", "favicon", "visits", "bookmark"};
    }

    private static abstract class HistoryWrapper extends BaseAdapter {
        protected HistoryAdapter mAdapter;
        private DataSetObserver mObserver = new DataSetObserver(this) {
            final HistoryWrapper this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onChanged() {
                super.onChanged();
                this.this$0.notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                this.this$0.notifyDataSetInvalidated();
            }
        };

        public HistoryWrapper(HistoryAdapter historyAdapter) {
            this.mAdapter = historyAdapter;
            this.mAdapter.registerDataSetObserver(this.mObserver);
        }
    }

    private void copy(CharSequence charSequence) {
        ((ClipboardManager) getActivity().getSystemService("clipboard")).setText(charSequence);
    }

    private void inflateSinglePane() {
        this.mHistoryList = (ExpandableListView) this.mRoot.findViewById(2131558484);
        this.mHistoryList.setAdapter(this.mAdapter);
        this.mHistoryList.setOnChildClickListener(this);
        registerForContextMenu(this.mHistoryList);
    }

    private void inflateTwoPane(ViewStub viewStub) {
        viewStub.setLayoutResource(2130968615);
        viewStub.inflate();
        this.mGroupList = (ListView) this.mRoot.findViewById(android.R.id.list);
        this.mPrefsContainer = (ViewGroup) this.mRoot.findViewById(2131558505);
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
        ((ViewGroup) this.mRoot.findViewById(2131558506)).addView(this.mChildList);
    }

    void checkIfEmpty() {
        if (this.mAdapter.mMostVisited == null || this.mAdapter.mHistoryCursor == null) {
            return;
        }
        boolean zIsTablet = BrowserActivity.isTablet(getActivity());
        if (this.mAdapter.isEmpty()) {
            if (zIsTablet) {
                this.mRoot.findViewById(2131558486).setVisibility(8);
            } else {
                this.mRoot.findViewById(2131558484).setVisibility(8);
            }
            this.mRoot.findViewById(android.R.id.empty).setVisibility(0);
            return;
        }
        if (zIsTablet) {
            this.mRoot.findViewById(2131558486).setVisibility(0);
        } else {
            this.mRoot.findViewById(2131558484).setVisibility(0);
        }
        this.mRoot.findViewById(android.R.id.empty).setVisibility(8);
    }

    View getTargetView(ContextMenu.ContextMenuInfo contextMenuInfo) {
        if (contextMenuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            return ((AdapterView.AdapterContextMenuInfo) contextMenuInfo).targetView;
        }
        if (contextMenuInfo instanceof ExpandableListView.ExpandableListContextMenuInfo) {
            return ((ExpandableListView.ExpandableListContextMenuInfo) contextMenuInfo).targetView;
        }
        return null;
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long j) {
        this.mCallback.openUrl(((HistoryItem) view).getUrl());
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) throws Throwable {
        ContextMenu.ContextMenuInfo menuInfo = menuItem.getMenuInfo();
        if (menuInfo == null) {
            return false;
        }
        View targetView = getTargetView(menuInfo);
        if (!(targetView instanceof HistoryItem)) {
            return false;
        }
        HistoryItem historyItem = (HistoryItem) targetView;
        String url = historyItem.getUrl();
        String name = historyItem.getName();
        Activity activity = getActivity();
        int itemId = menuItem.getItemId();
        if (itemId == 2131558641) {
            if (historyItem.isBookmark()) {
                Bookmarks.removeFromBookmarks(activity, activity.getContentResolver(), url, name);
            } else {
                com.android.browser.provider.Browser.saveBookmark(activity, name, url);
            }
            return true;
        }
        switch (itemId) {
            case 2131558433:
                this.mCallback.openUrl(url);
                break;
            case 2131558434:
                this.mCallback.openInNewTab(url);
                break;
            default:
                switch (itemId) {
                    case 2131558565:
                        com.android.browser.provider.Browser.sendString(activity, url, activity.getText(2131493051).toString());
                        break;
                    case 2131558566:
                        copy(url);
                        break;
                    case 2131558567:
                        com.android.browser.provider.Browser.deleteFromHistory(activity.getContentResolver(), url);
                        break;
                    case 2131558568:
                        BrowserSettings.getInstance().setHomePage(url);
                        BrowserSettings.getInstance().setHomePagePicker("other");
                        Toast.makeText(activity, 2131493012, 1).show();
                        break;
                }
                break;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        this.mDisableNewWindow = getArguments().getBoolean("disable_new_window", false);
        this.mMostVisitsLimit = Integer.toString(getResources().getInteger(2131623940));
        this.mCallback = (CombinedBookmarksCallbacks) getActivity();
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        View targetView = getTargetView(contextMenuInfo);
        if (targetView instanceof HistoryItem) {
            HistoryItem historyItem = (HistoryItem) targetView;
            Activity activity = getActivity();
            activity.getMenuInflater().inflate(2131755014, contextMenu);
            if (this.mContextHeader == null) {
                this.mContextHeader = new HistoryItem(activity, false);
                this.mContextHeader.setEnableScrolling(true);
            } else if (this.mContextHeader.getParent() != null) {
                ((ViewGroup) this.mContextHeader.getParent()).removeView(this.mContextHeader);
            }
            historyItem.copyTo(this.mContextHeader);
            contextMenu.setHeaderView(this.mContextHeader);
            if (this.mDisableNewWindow) {
                contextMenu.findItem(2131558434).setVisible(false);
            }
            if (historyItem.isBookmark()) {
                contextMenu.findItem(2131558641).setTitle(2131493007);
            }
            PackageManager packageManager = activity.getPackageManager();
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            contextMenu.findItem(2131558565).setVisible(packageManager.resolveActivity(intent, 65536) != null);
            super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri.Builder builderBuildUpon = BrowserContract.Combined.CONTENT_URI.buildUpon();
        switch (i) {
            case 1:
                return new CursorLoader(getActivity(), builderBuildUpon.build(), HistoryQuery.PROJECTION, "visits > 0", null, "date DESC");
            case 2:
                return new CursorLoader(getActivity(), builderBuildUpon.appendQueryParameter("limit", this.mMostVisitsLimit).build(), HistoryQuery.PROJECTION, "visits > 0", null, "visits DESC");
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        this.mBrowserHistoryExt = Extensions.getHistoryPlugin(getActivity());
        this.mBrowserHistoryExt.createHistoryPageOptionsMenu(menu, menuInflater);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mRoot = layoutInflater.inflate(2130968603, viewGroup, false);
        this.mAdapter = new HistoryAdapter(this, getActivity());
        ViewStub viewStub = (ViewStub) this.mRoot.findViewById(2131558485);
        if (viewStub != null) {
            inflateTwoPane(viewStub);
        } else {
            inflateSinglePane();
        }
        getLoaderManager().restartLoader(1, null, this);
        getLoaderManager().restartLoader(2, null, this);
        return this.mRoot;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(1);
        getLoaderManager().destroyLoader(2);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case 1:
                this.mAdapter.changeCursor(cursor);
                if (!this.mAdapter.isEmpty() && this.mGroupList != null && this.mGroupList.getCheckedItemPosition() == -1) {
                    selectGroup(0);
                }
                checkIfEmpty();
                return;
            case 2:
                this.mAdapter.changeMostVisitedCursor(cursor);
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
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        this.mBrowserHistoryExt = Extensions.getHistoryPlugin(getActivity());
        if (this.mBrowserHistoryExt.historyPageOptionsMenuItemSelected(menuItem, getActivity())) {
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.mBrowserHistoryExt = Extensions.getHistoryPlugin(getActivity());
        this.mBrowserHistoryExt.prepareHistoryPageOptionsMenuItem(menu, this.mAdapter == null, this.mAdapter.isEmpty());
    }

    void selectGroup(int i) {
        this.mGroupItemClickListener.onItemClick(null, this.mAdapter.getGroupView(i, false, null, null), i, i);
    }
}
