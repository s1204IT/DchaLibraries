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
import android.provider.BrowserContract;
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
import com.android.browser.view.BookmarkExpandableView;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public class BrowserBookmarksPage extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnCreateContextMenuListener, ExpandableListView.OnChildClickListener, BreadCrumbView.Controller {
    public static boolean mNeedChangeCursor = false;
    static ThreadLocal<BitmapFactory.Options> sOptions = new ThreadLocal<BitmapFactory.Options>() {
        @Override
        protected BitmapFactory.Options initialValue() {
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
            return new BookmarksLoader(getActivity(), accountType, accountName);
        }
        throw new UnsupportedOperationException("Unknown loader id " + id);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
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
            return;
        }
        if (loader.getId() >= 100) {
            BrowserBookmarksAdapter adapter2 = this.mBookmarkAdapters.get(Integer.valueOf(loader.getId()));
            if (mNeedChangeCursor) {
                adapter2.changeCursor(cursor);
            } else {
                adapter2.simpleChangeCursor(cursor);
            }
            mNeedChangeCursor = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() >= 100) {
            BrowserBookmarksAdapter adapter = this.mBookmarkAdapters.get(Integer.valueOf(loader.getId()));
            adapter.changeCursor(null);
        }
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
        switch (itemId) {
            case R.id.open_context_menu_id:
                loadUrl(adapter, childPosition);
                break;
            case R.id.new_window_context_menu_id:
                openInNewWindow(adapter, childPosition);
                break;
            case R.id.edit_context_menu_id:
                mNeedChangeCursor = true;
                editBookmark(adapter, childPosition);
                break;
            case R.id.shortcut_context_menu_id:
                Cursor c = adapter.getItem(childPosition);
                activity.sendBroadcast(createShortcutIntent(getActivity(), c));
                break;
            case R.id.share_link_context_menu_id:
                Cursor cursor = adapter.getItem(childPosition);
                Controller.sharePage(activity, cursor.getString(2), cursor.getString(1), getBitmap(cursor, 3), getBitmap(cursor, 4));
                break;
            case R.id.copy_url_context_menu_id:
                copy(getUrl(adapter, childPosition));
                break;
            case R.id.delete_context_menu_id:
                mNeedChangeCursor = true;
                displayRemoveBookmarkDialog(adapter, childPosition);
                break;
            case R.id.homepage_context_menu_id:
                BrowserSettings.getInstance().setHomePage(getUrl(adapter, childPosition));
                Toast.makeText(activity, R.string.homepage_set, 1).show();
                break;
            case R.id.save_to_bookmarks_menu_id:
                Cursor cursor2 = adapter.getItem(childPosition);
                String name = cursor2.getString(2);
                String url = cursor2.getString(1);
                Bookmarks.removeFromBookmarks(activity, activity.getContentResolver(), url, name);
                break;
        }
        return true;
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_bookmark_in_bookmarkpage:
                mNeedChangeCursor = true;
                Intent intent = new Intent(getActivity(), (Class<?>) AddBookmarkPage.class);
                intent.putExtra("title", "");
                intent.putExtra("url", "");
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        BookmarkExpandableView.BookmarkContextMenuInfo info = (BookmarkExpandableView.BookmarkContextMenuInfo) menuInfo;
        BrowserBookmarksAdapter adapter = getChildAdapter(info.groupPosition);
        Cursor cursor = adapter.getItem(info.childPosition);
        if (canEdit(cursor)) {
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
        super.onCreate(icicle);
        mNeedChangeCursor = false;
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
        if (this.mCallbacks == null && (getActivity() instanceof CombinedBookmarksCallbacks)) {
            this.mCallbacks = new CombinedBookmarksCallbackWrapper((CombinedBookmarksCallbacks) getActivity());
        }
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
        Iterator<Integer> it = this.mBookmarkAdapters.keySet().iterator();
        while (it.hasNext()) {
            int id = it.next().intValue();
            lm.destroyLoader(id);
            BrowserBookmarksAdapter b = this.mBookmarkAdapters.get(Integer.valueOf(id));
            if (b != null) {
                b.clearThread();
            }
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
        if ((this.mCallbacks == null || !this.mCallbacks.onBookmarkSelected(cursor, isFolder)) && isFolder) {
            mNeedChangeCursor = true;
            String title = cursor.getString(2);
            Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, id);
            BreadCrumbView crumbs = getBreadCrumbs(groupPosition);
            if (crumbs != null) {
                crumbs.pushView(title, uri);
                crumbs.setVisibility(0);
            }
            loadFolder(groupPosition, uri);
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
        if (this.mCallbacks != null && adapter != null) {
            this.mCallbacks.onBookmarkSelected(adapter.getItem(position), false);
        }
    }

    private void openInNewWindow(BrowserBookmarksAdapter adapter, int position) {
        if (this.mCallbacks != null) {
            Cursor c = adapter.getItem(position);
            boolean isFolder = c.getInt(6) == 1;
            if (isFolder) {
                long id = c.getLong(0);
                new OpenAllInTabsTask(id).execute(new Void[0]);
            } else {
                this.mCallbacks.onOpenInNewWindow(getUrl(c));
            }
        }
    }

    class OpenAllInTabsTask extends AsyncTask<Void, Void, Cursor> {
        long mFolderId;

        public OpenAllInTabsTask(long id) {
            this.mFolderId = id;
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            Context c = BrowserBookmarksPage.this.getActivity();
            if (c == null) {
                return null;
            }
            return c.getContentResolver().query(BookmarkUtils.getBookmarksUri(c), BookmarksLoader.PROJECTION, "parent=?", new String[]{Long.toString(this.mFolderId)}, null);
        }

        @Override
        protected void onPostExecute(Cursor result) {
            if (BrowserBookmarksPage.this.mCallbacks != null && result.getCount() > 0) {
                String[] urls = new String[result.getCount()];
                int i = 0;
                while (result.moveToNext()) {
                    urls[i] = BrowserBookmarksPage.getUrl(result);
                    i++;
                }
                BrowserBookmarksPage.this.mCallbacks.onOpenInNewWindow(urls);
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
            item.putParcelable("favicon", BitmapFactory.decodeByteArray(data, 0, data.length));
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
        BookmarkUtils.displayRemoveBookmarkDialog(id, title, context, null);
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
        if (this.mGrid != null) {
            if (this.mEnableContextMenu) {
                registerForContextMenu(this.mGrid);
            } else {
                unregisterForContextMenu(this.mGrid);
                this.mGrid.setLongClickable(false);
            }
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
        protected Integer doInBackground(Long... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("Missing folder id!");
            }
            Uri uri = BookmarkUtils.getBookmarksUri(this.mContext);
            Cursor c = null;
            try {
                c = this.mContext.getContentResolver().query(uri, null, "parent=?", new String[]{params[0].toString()}, null);
                return Integer.valueOf(c.getCount());
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result.intValue() > 0) {
                this.mHeader.setUrl(this.mContext.getString(R.string.contextheader_folder_bookmarkcount, result));
            } else if (result.intValue() == 0) {
                this.mHeader.setUrl(this.mContext.getString(R.string.contextheader_folder_empty));
            }
        }
    }

    static class AccountsLoader extends CursorLoader {
        static String[] ACCOUNTS_PROJECTION = {"account_name", "account_type"};

        public AccountsLoader(Context context) {
            super(context, BrowserContract.Accounts.CONTENT_URI.buildUpon().appendQueryParameter("allowEmptyAccounts", "false").build(), ACCOUNTS_PROJECTION, null, null, null);
        }
    }
}
