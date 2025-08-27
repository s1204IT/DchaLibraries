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

/* loaded from: classes.dex */
public class BrowserBookmarksPage extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnCreateContextMenuListener, ExpandableListView.OnChildClickListener, BreadCrumbView.Controller {
    static ThreadLocal<BitmapFactory.Options> sOptions = new ThreadLocal<BitmapFactory.Options>() { // from class: com.android.browser.BrowserBookmarksPage.1
        AnonymousClass1() {
        }

        /* JADX DEBUG: Method merged with bridge method: initialValue()Ljava/lang/Object; */
        @Override // java.lang.ThreadLocal
        protected BitmapFactory.Options initialValue() {
            return new BitmapFactory.Options();
        }
    };
    BrowserBookmarksPage2 mCallbacks;
    boolean mDisableNewWindow;
    View mEmptyView;
    BookmarkExpandableView mGrid;
    View mRoot;
    JSONObject mState;
    boolean mEnableContextMenu = true;
    HashMap<Integer, BrowserBookmarksAdapter> mBookmarkAdapters = new HashMap<>();
    long mCurrentFolderId = 1;
    private IBrowserBookmarkExt mBrowserBookmarkExt = null;
    private MenuItem.OnMenuItemClickListener mContextItemClickListener = new MenuItem.OnMenuItemClickListener() { // from class: com.android.browser.BrowserBookmarksPage.2
        AnonymousClass2() {
        }

        @Override // android.view.MenuItem.OnMenuItemClickListener
        public boolean onMenuItemClick(MenuItem menuItem) {
            return BrowserBookmarksPage.this.onContextItemSelected(menuItem);
        }
    };

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == 1) {
            return new AccountsLoader(getActivity());
        }
        if (i >= 100) {
            return new BookmarksLoader(getActivity(), bundle.getString("account_type"), bundle.getString("account_name"));
        }
        throw new UnsupportedOperationException("Unknown loader id " + i);
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) throws JSONException {
        boolean z;
        boolean z2 = cursor.getCount() == 0;
        int i = 100;
        if (loader.getId() == 1) {
            LoaderManager loaderManager = getLoaderManager();
            while (cursor.moveToNext()) {
                String string = cursor.getString(0);
                String string2 = cursor.getString(1);
                Bundle bundle = new Bundle();
                bundle.putString("account_name", string);
                bundle.putString("account_type", string2);
                BrowserBookmarksAdapter browserBookmarksAdapter = new BrowserBookmarksAdapter(getActivity());
                this.mBookmarkAdapters.put(Integer.valueOf(i), browserBookmarksAdapter);
                try {
                    z = this.mState.getBoolean(string != null ? string : "local");
                } catch (JSONException e) {
                    z = true;
                }
                this.mGrid.addAccount(string, browserBookmarksAdapter, z);
                loaderManager.restartLoader(i, bundle, this);
                i++;
            }
            getLoaderManager().destroyLoader(1);
        } else if (loader.getId() >= 100) {
            BrowserBookmarksAdapter browserBookmarksAdapter2 = this.mBookmarkAdapters.get(Integer.valueOf(loader.getId()));
            browserBookmarksAdapter2.changeCursor(cursor);
            if (browserBookmarksAdapter2.getCount() != 0) {
                this.mCurrentFolderId = browserBookmarksAdapter2.getItem(0).getLong(8);
            }
        }
        this.mEmptyView.setVisibility(z2 ? 0 : 8);
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        this.mBrowserBookmarkExt = Extensions.getBookmarkPlugin(getActivity());
        this.mBrowserBookmarkExt.createBookmarksPageOptionsMenu(menu, menuInflater);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        this.mBrowserBookmarkExt = Extensions.getBookmarkPlugin(getActivity());
        if (this.mBrowserBookmarkExt.bookmarksPageOptionsMenuItemSelected(menuItem, getActivity(), this.mCurrentFolderId)) {
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // android.app.Fragment
    public boolean onContextItemSelected(MenuItem menuItem) {
        BookmarkExpandableView.BookmarkContextMenuInfo bookmarkContextMenuInfo;
        if (!(menuItem.getMenuInfo() instanceof BookmarkExpandableView.BookmarkContextMenuInfo) || (bookmarkContextMenuInfo = (BookmarkExpandableView.BookmarkContextMenuInfo) menuItem.getMenuInfo()) == null) {
            return false;
        }
        if (handleContextItem(menuItem.getItemId(), bookmarkContextMenuInfo.groupPosition, bookmarkContextMenuInfo.childPosition)) {
            return true;
        }
        return super.onContextItemSelected(menuItem);
    }

    public boolean handleContextItem(int i, int i2, int i3) throws Throwable {
        Activity activity = getActivity();
        BrowserBookmarksAdapter childAdapter = getChildAdapter(i2);
        if (getUrl(childAdapter.getItem(i3)) == null && (i == R.id.open_context_menu_id || i == R.id.copy_url_context_menu_id || i == R.id.share_link_context_menu_id || i == R.id.shortcut_context_menu_id)) {
            Toast.makeText(getActivity(), R.string.bookmark_url_not_valid, 1).show();
            return true;
        }
        if (i != R.id.save_to_bookmarks_menu_id) {
            switch (i) {
                case R.id.open_context_menu_id /* 2131558433 */:
                    loadUrl(childAdapter, i3);
                    break;
                case R.id.new_window_context_menu_id /* 2131558434 */:
                    openInNewWindow(childAdapter, i3);
                    break;
                default:
                    switch (i) {
                        case R.id.edit_context_menu_id /* 2131558563 */:
                            editBookmark(childAdapter, i3);
                            break;
                        case R.id.shortcut_context_menu_id /* 2131558564 */:
                            createShortcut(getActivity(), childAdapter.getItem(i3));
                            break;
                        case R.id.share_link_context_menu_id /* 2131558565 */:
                            Cursor item = childAdapter.getItem(i3);
                            Controller.sharePage(activity, item.getString(2), item.getString(1), getBitmap(item, 3), getBitmap(item, 4));
                            break;
                        case R.id.copy_url_context_menu_id /* 2131558566 */:
                            copy(getUrl(childAdapter, i3));
                            break;
                        case R.id.delete_context_menu_id /* 2131558567 */:
                            displayRemoveBookmarkDialog(childAdapter, i3);
                            break;
                        case R.id.homepage_context_menu_id /* 2131558568 */:
                            BrowserSettings.getInstance().setHomePage(getUrl(childAdapter, i3));
                            BrowserSettings.getInstance().setHomePagePicker("other");
                            Toast.makeText(activity, R.string.homepage_set, 1).show();
                            break;
                        default:
                            return false;
                    }
            }
        } else {
            Cursor item2 = childAdapter.getItem(i3);
            String string = item2.getString(2);
            Bookmarks.removeFromBookmarks(activity, activity.getContentResolver(), item2.getString(1), string);
        }
        return true;
    }

    static Bitmap getBitmap(Cursor cursor, int i) {
        return getBitmap(cursor, i, null);
    }

    /* renamed from: com.android.browser.BrowserBookmarksPage$1 */
    class AnonymousClass1 extends ThreadLocal<BitmapFactory.Options> {
        AnonymousClass1() {
        }

        /* JADX DEBUG: Method merged with bridge method: initialValue()Ljava/lang/Object; */
        @Override // java.lang.ThreadLocal
        protected BitmapFactory.Options initialValue() {
            return new BitmapFactory.Options();
        }
    }

    static Bitmap getBitmap(Cursor cursor, int i, Bitmap bitmap) {
        byte[] blob = cursor.getBlob(i);
        if (blob == null) {
            return null;
        }
        BitmapFactory.Options options = sOptions.get();
        options.inBitmap = bitmap;
        options.inSampleSize = 1;
        options.inScaled = false;
        try {
            return BitmapFactory.decodeByteArray(blob, 0, blob.length, options);
        } catch (IllegalArgumentException e) {
            return BitmapFactory.decodeByteArray(blob, 0, blob.length);
        }
    }

    /* renamed from: com.android.browser.BrowserBookmarksPage$2 */
    class AnonymousClass2 implements MenuItem.OnMenuItemClickListener {
        AnonymousClass2() {
        }

        @Override // android.view.MenuItem.OnMenuItemClickListener
        public boolean onMenuItemClick(MenuItem menuItem) {
            return BrowserBookmarksPage.this.onContextItemSelected(menuItem);
        }
    }

    @Override // android.app.Fragment, android.view.View.OnCreateContextMenuListener
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        BookmarkExpandableView.BookmarkContextMenuInfo bookmarkContextMenuInfo = (BookmarkExpandableView.BookmarkContextMenuInfo) contextMenuInfo;
        Cursor item = getChildAdapter(bookmarkContextMenuInfo.groupPosition).getItem(bookmarkContextMenuInfo.childPosition);
        if (!canEdit(item)) {
            return;
        }
        boolean z = item.getInt(6) != 0;
        Activity activity = getActivity();
        activity.getMenuInflater().inflate(R.menu.bookmarkscontext, contextMenu);
        if (z) {
            contextMenu.setGroupVisible(R.id.FOLDER_CONTEXT_MENU, true);
        } else {
            contextMenu.setGroupVisible(R.id.BOOKMARK_CONTEXT_MENU, true);
            if (this.mDisableNewWindow) {
                contextMenu.findItem(R.id.new_window_context_menu_id).setVisible(false);
            }
        }
        BookmarkItem bookmarkItem = new BookmarkItem(activity);
        bookmarkItem.setEnableScrolling(true);
        populateBookmarkItem(item, bookmarkItem, z);
        contextMenu.setHeaderView(bookmarkItem);
        int size = contextMenu.size();
        for (int i = 0; i < size; i++) {
            contextMenu.getItem(i).setOnMenuItemClickListener(this.mContextItemClickListener);
        }
    }

    boolean canEdit(Cursor cursor) {
        int i = cursor.getInt(9);
        return i == 1 || i == 2;
    }

    private void populateBookmarkItem(Cursor cursor, BookmarkItem bookmarkItem, boolean z) {
        bookmarkItem.setName(cursor.getString(2));
        if (z) {
            bookmarkItem.setUrl(null);
            bookmarkItem.setFavicon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_folder_holo_dark));
            new LookupBookmarkCount(getActivity(), bookmarkItem).execute(Long.valueOf(cursor.getLong(0)));
        } else {
            bookmarkItem.setUrl(cursor.getString(1));
            bookmarkItem.setFavicon(getBitmap(cursor, 3));
        }
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SharedPreferences preferences = BrowserSettings.getInstance().getPreferences();
        try {
            this.mState = new JSONObject(preferences.getString("bbp_group_state", "{}"));
        } catch (JSONException e) {
            preferences.edit().remove("bbp_group_state").apply();
            this.mState = new JSONObject();
        }
        Bundle arguments = getArguments();
        this.mDisableNewWindow = arguments != null ? arguments.getBoolean("disable_new_window", false) : false;
        setHasOptionsMenu(true);
        if (this.mCallbacks == null && (getActivity() instanceof CombinedBookmarksCallbacks)) {
            this.mCallbacks = new CombinedBookmarksCallbackWrapper((CombinedBookmarksCallbacks) getActivity());
        }
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        try {
            this.mState = this.mGrid.saveGroupState();
            BrowserSettings.getInstance().getPreferences().edit().putString("bbp_group_state", this.mState.toString()).apply();
        } catch (JSONException e) {
        }
    }

    private static class CombinedBookmarksCallbackWrapper implements BrowserBookmarksPage2 {
        private CombinedBookmarksCallbacks mCombinedCallback;

        /* synthetic */ CombinedBookmarksCallbackWrapper(CombinedBookmarksCallbacks combinedBookmarksCallbacks, AnonymousClass1 anonymousClass1) {
            this(combinedBookmarksCallbacks);
        }

        private CombinedBookmarksCallbackWrapper(CombinedBookmarksCallbacks combinedBookmarksCallbacks) {
            this.mCombinedCallback = combinedBookmarksCallbacks;
        }

        @Override // com.android.browser.BrowserBookmarksPage2
        public boolean onOpenInNewWindow(String... strArr) {
            this.mCombinedCallback.openInNewTab(strArr);
            return true;
        }

        @Override // com.android.browser.BrowserBookmarksPage2
        public boolean onBookmarkSelected(Cursor cursor, boolean z) {
            if (z) {
                return false;
            }
            this.mCombinedCallback.openUrl(BrowserBookmarksPage.getUrl(cursor));
            return true;
        }
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mRoot = layoutInflater.inflate(R.layout.bookmarks, viewGroup, false);
        this.mEmptyView = this.mRoot.findViewById(android.R.id.empty);
        this.mGrid = (BookmarkExpandableView) this.mRoot.findViewById(R.id.grid);
        this.mGrid.setOnChildClickListener(this);
        this.mGrid.setColumnWidthFromLayout(R.layout.bookmark_thumbnail);
        this.mGrid.setBreadcrumbController(this);
        setEnableContextMenu(this.mEnableContextMenu);
        getLoaderManager().restartLoader(1, null, this);
        return this.mRoot;
    }

    @Override // android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mGrid.setBreadcrumbController(null);
        this.mGrid.clearAccounts();
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.destroyLoader(1);
        Iterator<Integer> it = this.mBookmarkAdapters.keySet().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            this.mBookmarkAdapters.get(Integer.valueOf(iIntValue)).releaseCursor(loaderManager, iIntValue);
        }
        this.mBookmarkAdapters.clear();
    }

    private BrowserBookmarksAdapter getChildAdapter(int i) {
        return this.mGrid.getChildAdapter(i);
    }

    private BreadCrumbView getBreadCrumbs(int i) {
        return this.mGrid.getBreadCrumbs(i);
    }

    @Override // android.widget.ExpandableListView.OnChildClickListener
    public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long j) {
        Cursor item = getChildAdapter(i).getItem(i2);
        boolean z = item.getInt(6) != 0;
        String url = getUrl(item);
        if (url != null && url.startsWith("rtsp://") && this.mCallbacks != null) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse(url.replaceAll(" ", "%20")));
            intent.addFlags(268435456);
            getActivity().startActivity(intent);
            return true;
        }
        if ((this.mCallbacks == null || !this.mCallbacks.onBookmarkSelected(item, z)) && z) {
            String string = item.getString(2);
            Uri uriWithAppendedId = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, j);
            BreadCrumbView breadCrumbs = getBreadCrumbs(i);
            if (breadCrumbs != null) {
                breadCrumbs.pushView(string, uriWithAppendedId);
                breadCrumbs.setVisibility(0);
                Object topData = breadCrumbs.getTopData();
                this.mCurrentFolderId = topData != null ? ContentUris.parseId((Uri) topData) : -1L;
            }
            loadFolder(i, uriWithAppendedId);
        }
        return true;
    }

    static void createShortcut(Context context, Cursor cursor) {
        BookmarkUtils.createShortcutToHome(context, cursor.getString(1), cursor.getString(2), getBitmap(cursor, 5), getBitmap(cursor, 3));
    }

    static Intent createShortcutIntent(Context context, Cursor cursor) {
        return BookmarkUtils.createAddToHomeIntent(context, cursor.getString(1), cursor.getString(2), getBitmap(cursor, 5), getBitmap(cursor, 3));
    }

    private void loadUrl(BrowserBookmarksAdapter browserBookmarksAdapter, int i) {
        if (this.mCallbacks != null && browserBookmarksAdapter != null) {
            String url = getUrl(browserBookmarksAdapter.getItem(i));
            if (url.startsWith("rtsp://")) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                intent.setData(Uri.parse(url.replaceAll(" ", "%20")));
                intent.addFlags(268435456);
                getActivity().startActivity(intent);
                return;
            }
            this.mCallbacks.onBookmarkSelected(browserBookmarksAdapter.getItem(i), false);
        }
    }

    private void openInNewWindow(BrowserBookmarksAdapter browserBookmarksAdapter, int i) {
        if (this.mCallbacks != null) {
            Cursor item = browserBookmarksAdapter.getItem(i);
            if (item.getInt(6) == 1) {
                new OpenAllInTabsTask(item.getLong(0)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                return;
            }
            String url = getUrl(item);
            if (url == null) {
                Toast.makeText(getActivity(), R.string.bookmark_url_not_valid, 1).show();
                return;
            }
            if (url.startsWith("rtsp://")) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                intent.setData(Uri.parse(url.replaceAll(" ", "%20")));
                intent.addFlags(268435456);
                getActivity().startActivity(intent);
                return;
            }
            this.mCallbacks.onOpenInNewWindow(getUrl(item));
        }
    }

    class OpenAllInTabsTask extends AsyncTask<Void, Void, ArrayList<String>> {
        long mFolderId;
        ArrayList<String> mUrls = new ArrayList<>();

        public OpenAllInTabsTask(long j) {
            this.mFolderId = j;
        }

        private void getChildrenUrls(Context context, long j) {
            Cursor cursorQuery = context.getContentResolver().query(BookmarkUtils.getBookmarksUri(context), BookmarksLoader.PROJECTION, "parent=?", new String[]{Long.toString(j)}, null);
            if (cursorQuery != null && cursorQuery.getCount() == 0) {
                cursorQuery.close();
                return;
            }
            if (cursorQuery != null) {
                while (cursorQuery.moveToNext()) {
                    if (cursorQuery.getInt(6) == 0) {
                        this.mUrls.add(cursorQuery.getString(1));
                    } else {
                        getChildrenUrls(context, cursorQuery.getLong(0));
                    }
                }
                cursorQuery.close();
            }
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected ArrayList<String> doInBackground(Void... voidArr) {
            Activity activity = BrowserBookmarksPage.this.getActivity();
            if (activity == null) {
                return null;
            }
            getChildrenUrls(activity, this.mFolderId);
            return this.mUrls;
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(ArrayList<String> arrayList) {
            if (arrayList != null && arrayList.size() == 0) {
                Activity activity = BrowserBookmarksPage.this.getActivity();
                Toast.makeText(activity, activity.getString(R.string.contextheader_folder_empty), 1).show();
            } else if (BrowserBookmarksPage.this.mCallbacks != null && arrayList != null && arrayList.size() > 0) {
                BrowserBookmarksPage.this.mCallbacks.onOpenInNewWindow((String[]) this.mUrls.toArray(new String[0]));
            }
        }
    }

    private void editBookmark(BrowserBookmarksAdapter browserBookmarksAdapter, int i) {
        Intent intent = new Intent(getActivity(), (Class<?>) AddBookmarkPage.class);
        Cursor item = browserBookmarksAdapter.getItem(i);
        Bundle bundle = new Bundle();
        bundle.putString("title", item.getString(2));
        boolean z = true;
        bundle.putString("url", item.getString(1));
        byte[] blob = item.getBlob(3);
        if (blob != null) {
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(blob, 0, blob.length);
            if (bitmapDecodeByteArray != null && bitmapDecodeByteArray.getWidth() > 60) {
                bitmapDecodeByteArray = Bitmap.createScaledBitmap(bitmapDecodeByteArray, 60, 60, true);
            }
            bundle.putParcelable("favicon", bitmapDecodeByteArray);
        }
        bundle.putLong("_id", item.getLong(0));
        bundle.putLong("parent", item.getLong(8));
        intent.putExtra("bookmark", bundle);
        if (item.getInt(6) != 1) {
            z = false;
        }
        intent.putExtra("is_folder", z);
        startActivity(intent);
    }

    private void displayRemoveBookmarkDialog(BrowserBookmarksAdapter browserBookmarksAdapter, int i) {
        Cursor item = browserBookmarksAdapter.getItem(i);
        long j = item.getLong(0);
        String string = item.getString(2);
        Activity activity = getActivity();
        if (!(item.getInt(6) != 0)) {
            BookmarkUtils.displayRemoveBookmarkDialog(j, string, activity, null);
        } else {
            BookmarkUtils.displayRemoveFolderDialog(j, string, activity, null);
        }
    }

    private String getUrl(BrowserBookmarksAdapter browserBookmarksAdapter, int i) {
        return getUrl(browserBookmarksAdapter.getItem(i));
    }

    static String getUrl(Cursor cursor) {
        return cursor.getString(1);
    }

    private void copy(CharSequence charSequence) {
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService("clipboard");
        String string = charSequence.toString();
        if (string.startsWith("content:")) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, string));
        } else {
            clipboardManager.setPrimaryClip(ClipData.newRawUri(null, Uri.parse(string)));
        }
    }

    @Override // android.app.Fragment, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        Resources resources = getActivity().getResources();
        this.mGrid.setColumnWidthFromLayout(R.layout.bookmark_thumbnail);
        this.mRoot.setPadding(0, (int) resources.getDimension(R.dimen.combo_paddingTop), 0, 0);
        getActivity().invalidateOptionsMenu();
    }

    @Override // com.android.browser.BreadCrumbView.Controller
    public void onTop(BreadCrumbView breadCrumbView, int i, Object obj) {
        int iIntValue = ((Integer) breadCrumbView.getTag(R.id.group_position)).intValue();
        Uri uri = (Uri) obj;
        if (uri == null) {
            uri = BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER;
        }
        loadFolder(iIntValue, uri);
        if (i <= 1) {
            breadCrumbView.setVisibility(8);
        } else {
            breadCrumbView.setVisibility(0);
        }
    }

    private void loadFolder(int i, Uri uri) {
        BookmarksLoader bookmarksLoader = (BookmarksLoader) getLoaderManager().getLoader(100 + i);
        bookmarksLoader.setUri(uri);
        bookmarksLoader.forceLoad();
    }

    public void setCallbackListener(BrowserBookmarksPage2 browserBookmarksPage2) {
        this.mCallbacks = browserBookmarksPage2;
    }

    public void setEnableContextMenu(boolean z) {
        this.mEnableContextMenu = z;
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

        public LookupBookmarkCount(Context context, BookmarkItem bookmarkItem) {
            this.mContext = context.getApplicationContext();
            this.mHeader = bookmarkItem;
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Integer doInBackground(Long... lArr) throws Throwable {
            if (lArr.length != 1) {
                throw new IllegalArgumentException("Missing folder id!");
            }
            Cursor cursor = null;
            try {
                int count = 0;
                Cursor cursorQuery = this.mContext.getContentResolver().query(BookmarkUtils.getBookmarksUri(this.mContext), null, "parent=? AND folder ==0", new String[]{lArr[0].toString()}, null);
                if (cursorQuery != null) {
                    try {
                        count = cursorQuery.getCount();
                    } catch (Throwable th) {
                        cursor = cursorQuery;
                        th = th;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return Integer.valueOf(count);
            } catch (Throwable th2) {
                th = th2;
            }
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(Integer num) {
            if (num.intValue() > 0) {
                this.mHeader.setUrl(this.mContext.getString(R.string.contextheader_folder_bookmarkcount, num));
            } else if (num.intValue() == 0) {
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
