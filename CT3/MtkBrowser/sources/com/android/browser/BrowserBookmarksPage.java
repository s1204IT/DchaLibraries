package com.android.browser;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;
import com.android.browser.BreadCrumbView;
import com.android.browser.provider.BrowserContract;
import com.android.browser.view.BookmarkExpandableView;
import com.mediatek.browser.ext.IBrowserBookmarkExt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public class BrowserBookmarksPage extends Fragment implements View.OnCreateContextMenuListener, LoaderManager.LoaderCallbacks<Cursor>, BreadCrumbView.Controller, ExpandableListView.OnChildClickListener {
    static ThreadLocal<BitmapFactory.Options> sOptions = new ThreadLocal<BitmapFactory.Options>() {
        @Override
        public BitmapFactory.Options initialValue() {
            return new BitmapFactory.Options();
        }
    };
    BookmarksPageCallbacks mCallbacks;
    boolean mDisableNewWindow;
    View mEmptyView;
    BookmarkExpandableView mGrid;
    View mRoot;
    JSONObject mState;
    boolean mEnableContextMenu = true;
    HashMap<Integer, BrowserBookmarksAdapter> mBookmarkAdapters = new HashMap<>();
    long mCurrentFolderId = 1;
    private IBrowserBookmarkExt mBrowserBookmarkExt = null;
    private MenuItem.OnMenuItemClickListener mContextItemClickListener = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return BrowserBookmarksPage.this.onContextItemSelected(item);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == 1) {
            return new AccountsLoader(getActivity());
        }
        if (id >= 100) {
            String accountType = args.getString("account_type");
            String accountName = args.getString("account_name");
            BookmarksLoader bl = new BookmarksLoader(getActivity(), accountType, accountName);
            return bl;
        }
        throw new UnsupportedOperationException("Unknown loader id " + id);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        boolean empty = false;
        if (cursor.getCount() == 0) {
            empty = true;
        }
        if (loader.getId() == 1) {
            LoaderManager lm = getLoaderManager();
            int id = 100;
            while (cursor.moveToNext()) {
                String accountName = cursor.getString(0);
                String accountType = cursor.getString(1);
                Bundle args = new Bundle();
                args.putString("account_name", accountName);
                args.putString("account_type", accountType);
                BrowserBookmarksAdapter adapter = new BrowserBookmarksAdapter(getActivity());
                this.mBookmarkAdapters.put(Integer.valueOf(id), adapter);
                boolean expand = true;
                try {
                    expand = this.mState.getBoolean(accountName != null ? accountName : "local");
                } catch (JSONException e) {
                }
                this.mGrid.addAccount(accountName, adapter, expand);
                lm.restartLoader(id, args, this);
                id++;
            }
            getLoaderManager().destroyLoader(1);
        } else if (loader.getId() >= 100) {
            BrowserBookmarksAdapter adapter2 = this.mBookmarkAdapters.get(Integer.valueOf(loader.getId()));
            adapter2.changeCursor(cursor);
            if (adapter2.getCount() != 0) {
                this.mCurrentFolderId = adapter2.getItem(0).getLong(8);
            }
        }
        this.mEmptyView.setVisibility(empty ? 0 : 8);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        this.mBrowserBookmarkExt = Extensions.getBookmarkPlugin(getActivity());
        this.mBrowserBookmarkExt.createBookmarksPageOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.mBrowserBookmarkExt = Extensions.getBookmarkPlugin(getActivity());
        if (this.mBrowserBookmarkExt.bookmarksPageOptionsMenuItemSelected(item, getActivity(), this.mCurrentFolderId)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        BookmarkExpandableView.BookmarkContextMenuInfo i;
        if (!(item.getMenuInfo() instanceof BookmarkExpandableView.BookmarkContextMenuInfo) || (i = (BookmarkExpandableView.BookmarkContextMenuInfo) item.getMenuInfo()) == null) {
            return false;
        }
        if (handleContextItem(item.getItemId(), i.groupPosition, i.childPosition)) {
            return true;
        }
        return super.onContextItemSelected(item);
    }

    public boolean handleContextItem(int itemId, int groupPosition, int childPosition) {
        Activity activity = getActivity();
        BrowserBookmarksAdapter adapter = getChildAdapter(groupPosition);
        String bookmarkUrl = getUrl(adapter.getItem(childPosition));
        if (bookmarkUrl == null && (itemId == R.id.open_context_menu_id || itemId == R.id.copy_url_context_menu_id || itemId == R.id.share_link_context_menu_id || itemId == R.id.shortcut_context_menu_id)) {
            Toast.makeText(getActivity(), R.string.bookmark_url_not_valid, 1).show();
            return true;
        }
        switch (itemId) {
            case R.id.open_context_menu_id:
                loadUrl(adapter, childPosition);
                return true;
            case R.id.new_window_context_menu_id:
                openInNewWindow(adapter, childPosition);
                return true;
            case R.id.edit_context_menu_id:
                editBookmark(adapter, childPosition);
                return true;
            case R.id.shortcut_context_menu_id:
                Cursor c = adapter.getItem(childPosition);
                activity.sendBroadcast(createShortcutIntent(getActivity(), c));
                return true;
            case R.id.share_link_context_menu_id:
                Cursor cursor = adapter.getItem(childPosition);
                Controller.sharePage(activity, cursor.getString(2), cursor.getString(1), getBitmap(cursor, 3), getBitmap(cursor, 4));
                return true;
            case R.id.copy_url_context_menu_id:
                copy(getUrl(adapter, childPosition));
                return true;
            case R.id.delete_context_menu_id:
                displayRemoveBookmarkDialog(adapter, childPosition);
                return true;
            case R.id.homepage_context_menu_id:
                BrowserSettings.getInstance().setHomePage(getUrl(adapter, childPosition));
                BrowserSettings.getInstance().setHomePagePicker("other");
                Toast.makeText(activity, R.string.homepage_set, 1).show();
                return true;
            case R.id.save_to_bookmarks_menu_id:
                Cursor cursor2 = adapter.getItem(childPosition);
                String name = cursor2.getString(2);
                String url = cursor2.getString(1);
                Bookmarks.removeFromBookmarks(activity, activity.getContentResolver(), url, name);
                return true;
            default:
                return false;
        }
    }

    static Bitmap getBitmap(Cursor cursor, int columnIndex) {
        return getBitmap(cursor, columnIndex, null);
    }

    static Bitmap getBitmap(Cursor cursor, int columnIndex, Bitmap inBitmap) {
        byte[] data = cursor.getBlob(columnIndex);
        if (data == null) {
            return null;
        }
        BitmapFactory.Options opts = sOptions.get();
        opts.inBitmap = inBitmap;
        opts.inSampleSize = 1;
        opts.inScaled = false;
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (IllegalArgumentException e) {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        BookmarkExpandableView.BookmarkContextMenuInfo info = (BookmarkExpandableView.BookmarkContextMenuInfo) menuInfo;
        BrowserBookmarksAdapter adapter = getChildAdapter(info.groupPosition);
        Cursor cursor = adapter.getItem(info.childPosition);
        if (!canEdit(cursor)) {
            return;
        }
        boolean isFolder = cursor.getInt(6) != 0;
        Activity activity = getActivity();
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.bookmarkscontext, menu);
        if (isFolder) {
            menu.setGroupVisible(R.id.FOLDER_CONTEXT_MENU, true);
        } else {
            menu.setGroupVisible(R.id.BOOKMARK_CONTEXT_MENU, true);
            if (this.mDisableNewWindow) {
                menu.findItem(R.id.new_window_context_menu_id).setVisible(false);
            }
        }
        BookmarkItem header = new BookmarkItem(activity);
        header.setEnableScrolling(true);
        populateBookmarkItem(cursor, header, isFolder);
        menu.setHeaderView(header);
        int count = menu.size();
        for (int i = 0; i < count; i++) {
            menu.getItem(i).setOnMenuItemClickListener(this.mContextItemClickListener);
        }
    }

    boolean canEdit(Cursor c) {
        int type = c.getInt(9);
        return type == 1 || type == 2;
    }

    private void populateBookmarkItem(Cursor cursor, BookmarkItem item, boolean isFolder) {
        item.setName(cursor.getString(2));
        if (isFolder) {
            item.setUrl(null);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_folder_holo_dark);
            item.setFavicon(bitmap);
            new LookupBookmarkCount(getActivity(), item).execute(Long.valueOf(cursor.getLong(0)));
            return;
        }
        String url = cursor.getString(1);
        item.setUrl(url);
        Bitmap bitmap2 = getBitmap(cursor, 3);
        item.setFavicon(bitmap2);
    }

    @Override
    public void onCreate(Bundle icicle) {
        CombinedBookmarksCallbackWrapper combinedBookmarksCallbackWrapper = null;
        super.onCreate(icicle);
        SharedPreferences prefs = BrowserSettings.getInstance().getPreferences();
        try {
            this.mState = new JSONObject(prefs.getString("bbp_group_state", "{}"));
        } catch (JSONException e) {
            prefs.edit().remove("bbp_group_state").apply();
            this.mState = new JSONObject();
        }
        Bundle args = getArguments();
        this.mDisableNewWindow = args != null ? args.getBoolean("disable_new_window", false) : false;
        setHasOptionsMenu(true);
        if (this.mCallbacks != null || !(getActivity() instanceof CombinedBookmarksCallbacks)) {
            return;
        }
        this.mCallbacks = new CombinedBookmarksCallbackWrapper((CombinedBookmarksCallbacks) getActivity(), combinedBookmarksCallbackWrapper);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            this.mState = this.mGrid.saveGroupState();
            SharedPreferences prefs = BrowserSettings.getInstance().getPreferences();
            prefs.edit().putString("bbp_group_state", this.mState.toString()).apply();
        } catch (JSONException e) {
        }
    }

    private static class CombinedBookmarksCallbackWrapper implements BookmarksPageCallbacks {
        private CombinedBookmarksCallbacks mCombinedCallback;

        CombinedBookmarksCallbackWrapper(CombinedBookmarksCallbacks cb, CombinedBookmarksCallbackWrapper combinedBookmarksCallbackWrapper) {
            this(cb);
        }

        private CombinedBookmarksCallbackWrapper(CombinedBookmarksCallbacks cb) {
            this.mCombinedCallback = cb;
        }

        @Override
        public boolean onOpenInNewWindow(String... urls) {
            this.mCombinedCallback.openInNewTab(urls);
            return true;
        }

        @Override
        public boolean onBookmarkSelected(Cursor c, boolean isFolder) {
            if (isFolder) {
                return false;
            }
            this.mCombinedCallback.openUrl(BrowserBookmarksPage.getUrl(c));
            return true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mRoot = inflater.inflate(R.layout.bookmarks, container, false);
        this.mEmptyView = this.mRoot.findViewById(android.R.id.empty);
        this.mGrid = (BookmarkExpandableView) this.mRoot.findViewById(R.id.grid);
        this.mGrid.setOnChildClickListener(this);
        this.mGrid.setColumnWidthFromLayout(R.layout.bookmark_thumbnail);
        this.mGrid.setBreadcrumbController(this);
        setEnableContextMenu(this.mEnableContextMenu);
        LoaderManager lm = getLoaderManager();
        lm.restartLoader(1, null, this);
        return this.mRoot;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mGrid.setBreadcrumbController(null);
        this.mGrid.clearAccounts();
        LoaderManager lm = getLoaderManager();
        lm.destroyLoader(1);
        Iterator id$iterator = this.mBookmarkAdapters.keySet().iterator();
        while (id$iterator.hasNext()) {
            int id = ((Integer) id$iterator.next()).intValue();
            BrowserBookmarksAdapter adapter = this.mBookmarkAdapters.get(Integer.valueOf(id));
            adapter.releaseCursor(lm, id);
        }
        this.mBookmarkAdapters.clear();
    }

    private BrowserBookmarksAdapter getChildAdapter(int groupPosition) {
        return this.mGrid.getChildAdapter(groupPosition);
    }

    private BreadCrumbView getBreadCrumbs(int groupPosition) {
        return this.mGrid.getBreadCrumbs(groupPosition);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        BrowserBookmarksAdapter adapter = getChildAdapter(groupPosition);
        Cursor cursor = adapter.getItem(childPosition);
        boolean isFolder = cursor.getInt(6) != 0;
        String url = getUrl(cursor);
        if (url != null && url.startsWith("rtsp://") && this.mCallbacks != null) {
            Intent i = new Intent();
            i.setAction("android.intent.action.VIEW");
            i.setData(Uri.parse(url.replaceAll(" ", "%20")));
            i.addFlags(268435456);
            getActivity().startActivity(i);
            return true;
        }
        if ((this.mCallbacks == null || !this.mCallbacks.onBookmarkSelected(cursor, isFolder)) && isFolder) {
            String title = cursor.getString(2);
            Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, id);
            BreadCrumbView crumbs = getBreadCrumbs(groupPosition);
            if (crumbs != null) {
                crumbs.pushView(title, uri);
                crumbs.setVisibility(0);
                Object data = crumbs.getTopData();
                this.mCurrentFolderId = data != null ? ContentUris.parseId((Uri) data) : -1L;
            }
            loadFolder(groupPosition, uri);
            return true;
        }
        return true;
    }

    static Intent createShortcutIntent(Context context, Cursor cursor) {
        String url = cursor.getString(1);
        String title = cursor.getString(2);
        Bitmap touchIcon = getBitmap(cursor, 5);
        Bitmap favicon = getBitmap(cursor, 3);
        return BookmarkUtils.createAddToHomeIntent(context, url, title, touchIcon, favicon);
    }

    private void loadUrl(BrowserBookmarksAdapter adapter, int position) {
        if (this.mCallbacks == null || adapter == null) {
            return;
        }
        String url = getUrl(adapter.getItem(position));
        if (url.startsWith("rtsp://")) {
            Intent i = new Intent();
            i.setAction("android.intent.action.VIEW");
            i.setData(Uri.parse(url.replaceAll(" ", "%20")));
            i.addFlags(268435456);
            getActivity().startActivity(i);
            return;
        }
        this.mCallbacks.onBookmarkSelected(adapter.getItem(position), false);
    }

    private void openInNewWindow(BrowserBookmarksAdapter adapter, int position) {
        if (this.mCallbacks == null) {
            return;
        }
        Cursor c = adapter.getItem(position);
        boolean isFolder = c.getInt(6) == 1;
        if (isFolder) {
            long id = c.getLong(0);
            new OpenAllInTabsTask(id).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            return;
        }
        String url = getUrl(c);
        if (url == null) {
            Toast.makeText(getActivity(), R.string.bookmark_url_not_valid, 1).show();
            return;
        }
        if (url.startsWith("rtsp://")) {
            Intent i = new Intent();
            i.setAction("android.intent.action.VIEW");
            i.setData(Uri.parse(url.replaceAll(" ", "%20")));
            i.addFlags(268435456);
            getActivity().startActivity(i);
            return;
        }
        this.mCallbacks.onOpenInNewWindow(getUrl(c));
    }

    class OpenAllInTabsTask extends AsyncTask<Void, Void, ArrayList<String>> {
        long mFolderId;
        ArrayList<String> mUrls = new ArrayList<>();

        public OpenAllInTabsTask(long id) {
            this.mFolderId = id;
        }

        private void getChildrenUrls(Context c, long id) {
            Cursor cursor = c.getContentResolver().query(BookmarkUtils.getBookmarksUri(c), BookmarksLoader.PROJECTION, "parent=?", new String[]{Long.toString(id)}, null);
            if (cursor != null && cursor.getCount() == 0) {
                cursor.close();
                return;
            }
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                if (cursor.getInt(6) == 0) {
                    this.mUrls.add(cursor.getString(1));
                } else {
                    getChildrenUrls(c, cursor.getLong(0));
                }
            }
            cursor.close();
        }

        @Override
        public ArrayList<String> doInBackground(Void... params) {
            Context c = BrowserBookmarksPage.this.getActivity();
            if (c == null) {
                return null;
            }
            getChildrenUrls(c, this.mFolderId);
            return this.mUrls;
        }

        @Override
        public void onPostExecute(ArrayList<String> result) {
            if (result != null && result.size() == 0) {
                Context ctx = BrowserBookmarksPage.this.getActivity();
                Toast.makeText(ctx, ctx.getString(R.string.contextheader_folder_empty), 1).show();
            } else {
                if (BrowserBookmarksPage.this.mCallbacks == null || result == null || result.size() <= 0) {
                    return;
                }
                BrowserBookmarksPage.this.mCallbacks.onOpenInNewWindow((String[]) this.mUrls.toArray(new String[0]));
            }
        }
    }

    private void editBookmark(BrowserBookmarksAdapter adapter, int position) {
        Intent intent = new Intent(getActivity(), (Class<?>) AddBookmarkPage.class);
        Cursor cursor = adapter.getItem(position);
        Bundle item = new Bundle();
        item.putString("title", cursor.getString(2));
        item.putString("url", cursor.getString(1));
        byte[] data = cursor.getBlob(3);
        if (data != null) {
            Bitmap icon = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (icon != null && icon.getWidth() > 60) {
                icon = Bitmap.createScaledBitmap(icon, 60, 60, true);
            }
            item.putParcelable("favicon", icon);
        }
        item.putLong("_id", cursor.getLong(0));
        item.putLong("parent", cursor.getLong(8));
        intent.putExtra("bookmark", item);
        intent.putExtra("is_folder", cursor.getInt(6) == 1);
        startActivity(intent);
    }

    private void displayRemoveBookmarkDialog(BrowserBookmarksAdapter adapter, int position) {
        Cursor cursor = adapter.getItem(position);
        long id = cursor.getLong(0);
        String title = cursor.getString(2);
        Context context = getActivity();
        boolean isFolder = cursor.getInt(6) != 0;
        if (!isFolder) {
            BookmarkUtils.displayRemoveBookmarkDialog(id, title, context, null);
        } else {
            BookmarkUtils.displayRemoveFolderDialog(id, title, context, null);
        }
    }

    private String getUrl(BrowserBookmarksAdapter adapter, int position) {
        return getUrl(adapter.getItem(position));
    }

    static String getUrl(Cursor c) {
        return c.getString(1);
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) getActivity().getSystemService("clipboard");
        cm.setPrimaryClip(ClipData.newRawUri(null, Uri.parse(text.toString())));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Resources res = getActivity().getResources();
        this.mGrid.setColumnWidthFromLayout(R.layout.bookmark_thumbnail);
        int paddingTop = (int) res.getDimension(R.dimen.combo_paddingTop);
        this.mRoot.setPadding(0, paddingTop, 0, 0);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onTop(BreadCrumbView view, int level, Object data) {
        int groupPosition = ((Integer) view.getTag(R.id.group_position)).intValue();
        Uri uri = (Uri) data;
        if (uri == null) {
            uri = BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER;
        }
        loadFolder(groupPosition, uri);
        if (level <= 1) {
            view.setVisibility(8);
        } else {
            view.setVisibility(0);
        }
    }

    private void loadFolder(int groupPosition, Uri uri) {
        LoaderManager manager = getLoaderManager();
        BookmarksLoader loader = (BookmarksLoader) manager.getLoader(groupPosition + 100);
        loader.setUri(uri);
        loader.forceLoad();
    }

    public void setCallbackListener(BookmarksPageCallbacks callbackListener) {
        this.mCallbacks = callbackListener;
    }

    public void setEnableContextMenu(boolean enable) {
        this.mEnableContextMenu = enable;
        if (this.mGrid == null) {
            return;
        }
        if (this.mEnableContextMenu) {
            registerForContextMenu(this.mGrid);
        } else {
            unregisterForContextMenu(this.mGrid);
            this.mGrid.setLongClickable(false);
        }
    }

    private static class LookupBookmarkCount extends AsyncTask<Long, Void, Integer> {
        Context mContext;
        BookmarkItem mHeader;

        public LookupBookmarkCount(Context context, BookmarkItem header) {
            this.mContext = context.getApplicationContext();
            this.mHeader = header;
        }

        @Override
        public Integer doInBackground(Long... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("Missing folder id!");
            }
            Uri uri = BookmarkUtils.getBookmarksUri(this.mContext);
            Cursor c = null;
            int count = 0;
            try {
                c = this.mContext.getContentResolver().query(uri, null, "parent=? AND folder ==0", new String[]{params[0].toString()}, null);
                if (c != null) {
                    count = c.getCount();
                }
                return Integer.valueOf(count);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        @Override
        public void onPostExecute(Integer result) {
            if (result.intValue() > 0) {
                this.mHeader.setUrl(this.mContext.getString(R.string.contextheader_folder_bookmarkcount, result));
            } else {
                if (result.intValue() != 0) {
                    return;
                }
                this.mHeader.setUrl(this.mContext.getString(R.string.contextheader_folder_empty));
            }
        }
    }

    static class AccountsLoader extends CursorLoader {
        static String[] ACCOUNTS_PROJECTION = {"account_name", "account_type"};

        public AccountsLoader(Context context) {
            super(context, BrowserContract.Accounts.CONTENT_URI.buildUpon().appendQueryParameter("allowEmptyAccounts", "true").build(), ACCOUNTS_PROJECTION, null, null, null);
        }
    }
}
