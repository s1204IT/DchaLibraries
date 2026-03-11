package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BrowserContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.browser.BreadCrumbView;
import com.android.browser.addbookmark.FolderSpinner;
import com.android.browser.addbookmark.FolderSpinnerAdapter;
import java.net.URI;
import java.net.URISyntaxException;

public class AddBookmarkPage extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener, TextView.OnEditorActionListener, BreadCrumbView.Controller, FolderSpinner.OnSetSelectionListener {
    private ArrayAdapter<BookmarkAccount> mAccountAdapter;
    private Spinner mAccountSpinner;
    private FolderAdapter mAdapter;
    private View mAddNewFolder;
    private View mAddSeparator;
    private EditText mAddress;
    private TextView mButton;
    private View mCancelButton;
    private View mCrumbHolder;
    private BreadCrumbView mCrumbs;
    private long mCurrentFolder;
    private View mDefaultView;
    private boolean mEditingExisting;
    private boolean mEditingFolder;
    private TextView mFakeTitle;
    private View mFakeTitleHolder;
    private FolderSpinner mFolder;
    private FolderSpinnerAdapter mFolderAdapter;
    private View mFolderCancel;
    private EditText mFolderNamer;
    private View mFolderNamerHolder;
    private View mFolderSelector;
    private Handler mHandler;
    private Drawable mHeaderIcon;
    private boolean mIsFolderNamerShowing;
    private CustomListView mListView;
    private Bundle mMap;
    private String mOriginalUrl;
    private View mRemoveLink;
    private long mRootFolder;
    private boolean mSaveToHomeScreen;
    private EditText mTitle;
    private TextView mTopLevelLabel;
    private String mTouchIconUrl;
    private final String LOGTAG = "Bookmarks";
    private final int LOADER_ID_ACCOUNTS = 0;
    private final int LOADER_ID_FOLDER_CONTENTS = 1;
    private final int LOADER_ID_EDIT_INFO = 2;
    private LoaderManager.LoaderCallbacks<EditBookmarkInfo> mEditInfoLoaderCallbacks = new LoaderManager.LoaderCallbacks<EditBookmarkInfo>() {
        @Override
        public void onLoaderReset(Loader<EditBookmarkInfo> loader) {
        }

        @Override
        public void onLoadFinished(Loader<EditBookmarkInfo> loader, EditBookmarkInfo info) {
            boolean setAccount = false;
            if (info.id != -1) {
                AddBookmarkPage.this.mEditingExisting = true;
                AddBookmarkPage.this.showRemoveButton();
                AddBookmarkPage.this.mFakeTitle.setText(R.string.edit_bookmark);
                AddBookmarkPage.this.mTitle.setText(info.title);
                AddBookmarkPage.this.mFolderAdapter.setOtherFolderDisplayText(info.parentTitle);
                AddBookmarkPage.this.mMap.putLong("_id", info.id);
                setAccount = true;
                AddBookmarkPage.this.setAccount(info.accountName, info.accountType);
                AddBookmarkPage.this.mCurrentFolder = info.parentId;
                AddBookmarkPage.this.onCurrentFolderFound();
            }
            if (info.lastUsedId != -1 && info.lastUsedId != info.id && !AddBookmarkPage.this.mEditingFolder) {
                if (setAccount && info.lastUsedId != AddBookmarkPage.this.mRootFolder && TextUtils.equals(info.lastUsedAccountName, info.accountName) && TextUtils.equals(info.lastUsedAccountType, info.accountType)) {
                    AddBookmarkPage.this.mFolderAdapter.addRecentFolder(info.lastUsedId, info.lastUsedTitle);
                } else if (!setAccount) {
                    setAccount = true;
                    AddBookmarkPage.this.setAccount(info.lastUsedAccountName, info.lastUsedAccountType);
                    if (info.lastUsedId != AddBookmarkPage.this.mRootFolder) {
                        AddBookmarkPage.this.mFolderAdapter.addRecentFolder(info.lastUsedId, info.lastUsedTitle);
                    }
                }
            }
            if (!setAccount) {
                AddBookmarkPage.this.mAccountSpinner.setSelection(0);
            }
        }

        @Override
        public Loader<EditBookmarkInfo> onCreateLoader(int id, Bundle args) {
            return new EditBookmarkInfoLoader(AddBookmarkPage.this, AddBookmarkPage.this.mMap);
        }
    };

    private static class Folder {
        long Id;
        String Name;

        Folder(String name, long id) {
            this.Name = name;
            this.Id = id;
        }
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService("input_method");
    }

    private Uri getUriForFolder(long folder) {
        BookmarkAccount account = (BookmarkAccount) this.mAccountSpinner.getSelectedItem();
        return (folder != this.mRootFolder || account == null) ? BrowserContract.Bookmarks.buildFolderUri(folder) : BookmarksLoader.addAccount(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, account.accountType, account.accountName);
    }

    @Override
    public void onTop(BreadCrumbView view, int level, Object data) {
        if (data != null) {
            Folder folderData = (Folder) data;
            long folder = folderData.Id;
            LoaderManager manager = getLoaderManager();
            CursorLoader loader = (CursorLoader) manager.getLoader(1);
            loader.setUri(getUriForFolder(folder));
            loader.forceLoad();
            if (this.mIsFolderNamerShowing) {
                completeOrCancelFolderNaming(true);
            }
            setShowBookmarkIcon(level == 1);
        }
    }

    private void setShowBookmarkIcon(boolean show) {
        Drawable drawable = show ? this.mHeaderIcon : null;
        this.mTopLevelLabel.setCompoundDrawablesWithIntrinsicBounds(drawable, (Drawable) null, (Drawable) null, (Drawable) null);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v != this.mFolderNamer) {
            return false;
        }
        if (v.getText().length() <= 0 || actionId != 0 || event.getAction() != 1) {
            return true;
        }
        completeOrCancelFolderNaming(false);
        return true;
    }

    private void switchToDefaultView(boolean changedFolder) {
        this.mFolderSelector.setVisibility(8);
        this.mDefaultView.setVisibility(0);
        this.mCrumbHolder.setVisibility(8);
        this.mFakeTitleHolder.setVisibility(0);
        if (changedFolder) {
            Object data = this.mCrumbs.getTopData();
            if (data != null) {
                Folder folder = (Folder) data;
                this.mCurrentFolder = folder.Id;
                if (this.mCurrentFolder == this.mRootFolder) {
                    this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 0 : 1);
                    return;
                } else {
                    this.mFolderAdapter.setOtherFolderDisplayText(folder.Name);
                    return;
                }
            }
            return;
        }
        if (this.mSaveToHomeScreen) {
            this.mFolder.setSelectionIgnoringSelectionChange(0);
            return;
        }
        if (this.mCurrentFolder == this.mRootFolder) {
            this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 0 : 1);
            return;
        }
        Object data2 = this.mCrumbs.getTopData();
        if (data2 != null && ((Folder) data2).Id == this.mCurrentFolder) {
            this.mFolderAdapter.setOtherFolderDisplayText(((Folder) data2).Name);
            return;
        }
        setupTopCrumb();
        LoaderManager manager = getLoaderManager();
        manager.restartLoader(1, null, this);
    }

    @Override
    public void onClick(View v) {
        if (v == this.mButton) {
            if (this.mFolderSelector.getVisibility() == 0) {
                if (this.mIsFolderNamerShowing) {
                    completeOrCancelFolderNaming(false);
                    return;
                } else {
                    this.mSaveToHomeScreen = false;
                    switchToDefaultView(true);
                    return;
                }
            }
            if (save()) {
                finish();
                return;
            }
            return;
        }
        if (v == this.mCancelButton) {
            if (this.mIsFolderNamerShowing) {
                completeOrCancelFolderNaming(true);
                return;
            } else if (this.mFolderSelector.getVisibility() == 0) {
                switchToDefaultView(false);
                return;
            } else {
                finish();
                return;
            }
        }
        if (v == this.mFolderCancel) {
            completeOrCancelFolderNaming(true);
            return;
        }
        if (v == this.mAddNewFolder) {
            setShowFolderNamer(true);
            this.mFolderNamer.setText(R.string.new_folder);
            this.mFolderNamer.requestFocus();
            this.mAddNewFolder.setVisibility(8);
            this.mAddSeparator.setVisibility(8);
            InputMethodManager imm = getInputMethodManager();
            imm.focusIn(this.mListView);
            imm.showSoftInput(this.mFolderNamer, 1);
            return;
        }
        if (v == this.mRemoveLink) {
            if (!this.mEditingExisting) {
                throw new AssertionError("Remove button should not be shown for new bookmarks");
            }
            long id = this.mMap.getLong("_id");
            createHandler();
            Message msg = Message.obtain(this.mHandler, 102);
            BookmarkUtils.displayRemoveBookmarkDialog(id, this.mTitle.getText().toString(), this, msg);
        }
    }

    @Override
    public void onSetSelection(long id) {
        int intId = (int) id;
        switch (intId) {
            case 0:
                this.mSaveToHomeScreen = true;
                break;
            case 1:
                this.mCurrentFolder = this.mRootFolder;
                this.mSaveToHomeScreen = false;
                break;
            case 2:
                switchToFolderSelector();
                break;
            case 3:
                this.mCurrentFolder = this.mFolderAdapter.recentFolderId();
                this.mSaveToHomeScreen = false;
                LoaderManager manager = getLoaderManager();
                manager.restartLoader(1, null, this);
                break;
        }
    }

    private void completeOrCancelFolderNaming(boolean cancel) {
        if (!cancel && !TextUtils.isEmpty(this.mFolderNamer.getText())) {
            String name = this.mFolderNamer.getText().toString();
            long id = addFolderToCurrent(this.mFolderNamer.getText().toString());
            descendInto(name, id);
        }
        setShowFolderNamer(false);
        this.mAddNewFolder.setVisibility(0);
        this.mAddSeparator.setVisibility(0);
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    private long addFolderToCurrent(String name) {
        long currentFolder;
        ContentValues values = new ContentValues();
        values.put("title", name);
        values.put("folder", (Integer) 1);
        Object data = this.mCrumbs.getTopData();
        if (data != null) {
            currentFolder = ((Folder) data).Id;
        } else {
            currentFolder = this.mRootFolder;
        }
        values.put("parent", Long.valueOf(currentFolder));
        Uri uri = getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
        if (uri != null) {
            return ContentUris.parseId(uri);
        }
        return -1L;
    }

    private void switchToFolderSelector() {
        this.mListView.setSelection(0);
        this.mDefaultView.setVisibility(8);
        this.mFolderSelector.setVisibility(0);
        this.mCrumbHolder.setVisibility(0);
        this.mFakeTitleHolder.setVisibility(8);
        this.mAddNewFolder.setVisibility(0);
        this.mAddSeparator.setVisibility(0);
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    private void descendInto(String foldername, long id) {
        if (id != -1) {
            this.mCrumbs.pushView(foldername, new Folder(foldername, id));
            this.mCrumbs.notifyController();
        }
    }

    void setAccount(String accountName, String accountType) {
        for (int i = 0; i < this.mAccountAdapter.getCount(); i++) {
            BookmarkAccount account = this.mAccountAdapter.getItem(i);
            if (TextUtils.equals(account.accountName, accountName) && TextUtils.equals(account.accountType, accountType)) {
                this.mAccountSpinner.setSelection(i);
                onRootFolderFound(account.rootFolderId);
                return;
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long currentFolder;
        switch (id) {
            case 0:
                return new AccountsLoader(this);
            case 1:
                String[] projection = {"_id", "title", "folder"};
                String where = "folder != 0";
                String[] whereArgs = null;
                if (this.mEditingFolder) {
                    where = "folder != 0 AND _id != ?";
                    whereArgs = new String[]{Long.toString(this.mMap.getLong("_id"))};
                }
                Object data = this.mCrumbs.getTopData();
                if (data != null) {
                    currentFolder = ((Folder) data).Id;
                } else {
                    currentFolder = this.mRootFolder;
                }
                return new CursorLoader(this, getUriForFolder(currentFolder), projection, where, whereArgs, "_id ASC");
            default:
                throw new AssertionError("Asking for nonexistant loader!");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case 0:
                this.mAccountAdapter.clear();
                while (cursor.moveToNext()) {
                    this.mAccountAdapter.add(new BookmarkAccount(this, cursor));
                }
                getLoaderManager().destroyLoader(0);
                getLoaderManager().restartLoader(2, null, this.mEditInfoLoaderCallbacks);
                break;
            case 1:
                this.mAdapter.changeCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case 1:
                this.mAdapter.changeCursor(null);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView tv = (TextView) view.findViewById(android.R.id.text1);
        descendInto(tv.getText().toString(), id);
    }

    private void setShowFolderNamer(boolean show) {
        if (show != this.mIsFolderNamerShowing) {
            this.mIsFolderNamerShowing = show;
            if (show) {
                this.mListView.addFooterView(this.mFolderNamerHolder);
            } else {
                this.mListView.removeFooterView(this.mFolderNamerHolder);
            }
            this.mListView.setAdapter((ListAdapter) this.mAdapter);
            if (show) {
                this.mListView.setSelection(this.mListView.getCount() - 1);
            }
        }
    }

    private class FolderAdapter extends CursorAdapter {
        public FolderAdapter(Context context) {
            super(context, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.folder_list_item, (ViewGroup) null);
            view.setBackgroundDrawable(context.getResources().getDrawable(android.R.drawable.list_selector_background));
            return view;
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty() && !AddBookmarkPage.this.mIsFolderNamerShowing;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(1);
        this.mMap = getIntent().getExtras();
        setContentView(R.layout.browser_add_bookmark);
        Window window = getWindow();
        String title = null;
        String url = null;
        this.mFakeTitle = (TextView) findViewById(R.id.fake_title);
        if (this.mMap != null) {
            Bundle b = this.mMap.getBundle("bookmark");
            if (b != null) {
                this.mEditingFolder = this.mMap.getBoolean("is_folder", false);
                this.mMap = b;
                this.mEditingExisting = true;
                this.mFakeTitle.setText(R.string.edit_bookmark);
                if (this.mEditingFolder) {
                    findViewById(R.id.row_address).setVisibility(8);
                } else {
                    showRemoveButton();
                }
            } else {
                int gravity = this.mMap.getInt("gravity", -1);
                if (gravity != -1) {
                    WindowManager.LayoutParams l = window.getAttributes();
                    l.gravity = gravity;
                    window.setAttributes(l);
                }
            }
            title = this.mMap.getString("title");
            url = this.mMap.getString("url");
            this.mOriginalUrl = url;
            this.mTouchIconUrl = this.mMap.getString("touch_icon_url");
            this.mCurrentFolder = this.mMap.getLong("parent", -1L);
        }
        this.mTitle = (EditText) findViewById(R.id.title);
        this.mTitle.setText(title);
        this.mAddress = (EditText) findViewById(R.id.address);
        this.mAddress.setText(url);
        this.mButton = (TextView) findViewById(R.id.OK);
        this.mButton.setOnClickListener(this);
        this.mCancelButton = findViewById(R.id.cancel);
        this.mCancelButton.setOnClickListener(this);
        this.mFolder = (FolderSpinner) findViewById(R.id.folder);
        this.mFolderAdapter = new FolderSpinnerAdapter(this, !this.mEditingFolder);
        this.mFolder.setAdapter((SpinnerAdapter) this.mFolderAdapter);
        this.mFolder.setOnSetSelectionListener(this);
        this.mDefaultView = findViewById(R.id.default_view);
        this.mFolderSelector = findViewById(R.id.folder_selector);
        this.mFolderNamerHolder = getLayoutInflater().inflate(R.layout.new_folder_layout, (ViewGroup) null);
        this.mFolderNamer = (EditText) this.mFolderNamerHolder.findViewById(R.id.folder_namer);
        this.mFolderNamer.setOnEditorActionListener(this);
        this.mFolderCancel = this.mFolderNamerHolder.findViewById(R.id.close);
        this.mFolderCancel.setOnClickListener(this);
        this.mAddNewFolder = findViewById(R.id.add_new_folder);
        this.mAddNewFolder.setOnClickListener(this);
        this.mAddSeparator = findViewById(R.id.add_divider);
        this.mCrumbs = (BreadCrumbView) findViewById(R.id.crumbs);
        this.mCrumbs.setUseBackButton(true);
        this.mCrumbs.setController(this);
        this.mHeaderIcon = getResources().getDrawable(R.drawable.ic_folder_holo_dark);
        this.mCrumbHolder = findViewById(R.id.crumb_holder);
        this.mCrumbs.setMaxVisible(2);
        this.mAdapter = new FolderAdapter(this);
        this.mListView = (CustomListView) findViewById(R.id.list);
        View empty = findViewById(R.id.empty);
        this.mListView.setEmptyView(empty);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mListView.addEditText(this.mFolderNamer);
        this.mAccountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        this.mAccountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mAccountSpinner = (Spinner) findViewById(R.id.accounts);
        this.mAccountSpinner.setAdapter((SpinnerAdapter) this.mAccountAdapter);
        this.mAccountSpinner.setOnItemSelectedListener(this);
        this.mFakeTitleHolder = findViewById(R.id.title_holder);
        if (!window.getDecorView().isInTouchMode()) {
            this.mButton.requestFocus();
        }
        getLoaderManager().restartLoader(0, null, this);
    }

    public void showRemoveButton() {
        findViewById(R.id.remove_divider).setVisibility(0);
        this.mRemoveLink = findViewById(R.id.remove);
        this.mRemoveLink.setVisibility(0);
        this.mRemoveLink.setOnClickListener(this);
    }

    private void onRootFolderFound(long root) {
        this.mRootFolder = root;
        this.mCurrentFolder = this.mRootFolder;
        setupTopCrumb();
        onCurrentFolderFound();
    }

    private void setupTopCrumb() {
        this.mCrumbs.clear();
        String name = getString(R.string.bookmarks);
        this.mTopLevelLabel = (TextView) this.mCrumbs.pushView(name, false, new Folder(name, this.mRootFolder));
        this.mTopLevelLabel.setCompoundDrawablePadding(6);
    }

    public void onCurrentFolderFound() {
        LoaderManager manager = getLoaderManager();
        if (this.mCurrentFolder != this.mRootFolder) {
            this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 1 : 2);
        } else {
            setShowBookmarkIcon(true);
            if (!this.mEditingFolder) {
                this.mFolder.setSelectionIgnoringSelectionChange(1);
            }
        }
        manager.restartLoader(1, null, this);
    }

    private class SaveBookmarkRunnable implements Runnable {
        private Context mContext;
        private Message mMessage;

        public SaveBookmarkRunnable(Context ctx, Message msg) {
            this.mContext = ctx.getApplicationContext();
            this.mMessage = msg;
        }

        @Override
        public void run() {
            Bundle bundle = this.mMessage.getData();
            String title = bundle.getString("title");
            String url = bundle.getString("url");
            boolean invalidateThumbnail = bundle.getBoolean("remove_thumbnail");
            Bitmap thumbnail = invalidateThumbnail ? null : (Bitmap) bundle.getParcelable("thumbnail");
            String touchIconUrl = bundle.getString("touch_icon_url");
            try {
                ContentResolver cr = AddBookmarkPage.this.getContentResolver();
                Bookmarks.addBookmark(AddBookmarkPage.this, false, url, title, thumbnail, AddBookmarkPage.this.mCurrentFolder);
                if (touchIconUrl != null) {
                    new DownloadTouchIcon(this.mContext, cr, url).execute(AddBookmarkPage.this.mTouchIconUrl);
                }
                this.mMessage.arg1 = 1;
            } catch (IllegalStateException e) {
                this.mMessage.arg1 = 0;
            }
            this.mMessage.sendToTarget();
        }
    }

    private static class UpdateBookmarkTask extends AsyncTask<ContentValues, Void, Void> {
        Context mContext;
        Long mId;

        public UpdateBookmarkTask(Context context, long id) {
            this.mContext = context.getApplicationContext();
            this.mId = Long.valueOf(id);
        }

        @Override
        public Void doInBackground(ContentValues... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("No ContentValues provided!");
            }
            Uri uri = ContentUris.withAppendedId(BookmarkUtils.getBookmarksUri(this.mContext), this.mId.longValue());
            Log.d("AddBookmarkPage", "UpdateBookmarkTask, mId:" + this.mId + ", uri:" + uri + ", params[0]:" + params[0]);
            this.mContext.getContentResolver().update(uri, params[0], null, null);
            return null;
        }
    }

    private void createHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 100:
                            if (1 == msg.arg1) {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_saved, 1).show();
                            } else {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_not_saved, 1).show();
                            }
                            break;
                        case 101:
                            Bundle b = msg.getData();
                            AddBookmarkPage.this.sendBroadcast(BookmarkUtils.createAddToHomeIntent(AddBookmarkPage.this, b.getString("url"), b.getString("title"), (Bitmap) b.getParcelable("touch_icon"), (Bitmap) b.getParcelable("favicon")));
                            break;
                        case 102:
                            AddBookmarkPage.this.finish();
                            break;
                    }
                }
            };
        }
    }

    boolean save() {
        Bitmap thumbnail;
        Bitmap favicon;
        createHandler();
        String title = this.mTitle.getText().toString().trim();
        String anchor = "";
        String unfilteredUrl = UrlUtils.fixUrl(this.mAddress.getText().toString());
        int d = unfilteredUrl.indexOf(35);
        if (d != -1) {
            anchor = unfilteredUrl.substring(d);
            unfilteredUrl = unfilteredUrl.substring(0, d);
        }
        boolean emptyTitle = title.length() == 0;
        boolean emptyUrl = unfilteredUrl.trim().length() == 0;
        Resources r = getResources();
        if (emptyTitle || (emptyUrl && !this.mEditingFolder)) {
            if (emptyTitle) {
                this.mTitle.setError(r.getText(R.string.bookmark_needs_title));
            }
            if (emptyUrl) {
                this.mAddress.setError(r.getText(R.string.bookmark_needs_url));
            }
            return false;
        }
        String url = unfilteredUrl.trim();
        if (!this.mEditingFolder) {
            try {
                if (!url.toLowerCase().startsWith("javascript:")) {
                    URI uriObj = new URI(url);
                    String scheme = uriObj.getScheme();
                    if (!Bookmarks.urlHasAcceptableScheme(url)) {
                        if (scheme != null) {
                            this.mAddress.setError(r.getText(R.string.bookmark_cannot_save_url));
                            return false;
                        }
                        try {
                            WebAddress address = new WebAddress(unfilteredUrl);
                            if (address.getHost().length() == 0) {
                                throw new URISyntaxException("", "");
                            }
                            url = address.toString();
                        } catch (ParseException e) {
                            throw new URISyntaxException("", "");
                        }
                    }
                }
            } catch (URISyntaxException e2) {
                this.mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
                return false;
            }
        }
        if (this.mSaveToHomeScreen) {
            this.mEditingExisting = false;
        }
        String url2 = url + anchor;
        boolean urlUnmodified = url2.equals(this.mOriginalUrl);
        Cursor c = null;
        boolean isDupTitleOrAddr = false;
        long bmId = 0L;
        if (!this.mSaveToHomeScreen) {
            try {
                c = getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "(title=? OR url=?) AND parent=? AND folder=?", new String[]{title, url2, String.valueOf(this.mCurrentFolder), "0"}, null);
                if (c != null && c.getCount() == 1 && c.moveToFirst()) {
                    isDupTitleOrAddr = true;
                    bmId = Long.valueOf(c.getLong(c.getColumnIndex("_id")));
                    Log.d("AddBookmarkPage", "isDupTitleOrAddr:true, bmId:" + bmId);
                } else if (c != null && c.getCount() == 2) {
                    this.mTitle.setError(r.getText(R.string.duplicate_title_and_address));
                    this.mAddress.setError(r.getText(R.string.duplicate_title_and_address));
                }
                if (c != null) {
                    c.close();
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        if (this.mEditingExisting || isDupTitleOrAddr) {
            final ContentValues values = new ContentValues();
            values.put("title", title);
            values.put("parent", Long.valueOf(this.mCurrentFolder));
            if (!this.mEditingFolder) {
                values.put("url", url2);
                if (!urlUnmodified) {
                    values.putNull("thumbnail");
                }
            }
            Log.d("AddBookmarkPage", "isDupTitleOrAddr:" + isDupTitleOrAddr);
            if (!isDupTitleOrAddr) {
                Long id = Long.valueOf(this.mMap.getLong("_id"));
                if (values.size() > 0) {
                    new UpdateBookmarkTask(getApplicationContext(), id.longValue()).execute(values);
                }
                setResult(-1);
            } else {
                Log.d("AddBookmarkPage", "create AlertDialog");
                final Long dupId = bmId;
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.duplicate_title_or_address).setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == -1) {
                            if (values.size() > 0) {
                                new UpdateBookmarkTask(AddBookmarkPage.this.getApplicationContext(), dupId.longValue()).execute(values);
                            }
                            AddBookmarkPage.this.finish();
                        }
                    }
                });
                Dialog dialog = builder.create();
                dialog.show();
                return false;
            }
        } else {
            if (urlUnmodified) {
                thumbnail = (Bitmap) this.mMap.getParcelable("thumbnail");
                favicon = (Bitmap) this.mMap.getParcelable("favicon");
            } else {
                thumbnail = null;
                favicon = null;
            }
            Bundle bundle = new Bundle();
            bundle.putString("title", title);
            bundle.putString("url", url2);
            bundle.putParcelable("favicon", favicon);
            if (this.mSaveToHomeScreen) {
                if (this.mTouchIconUrl != null && urlUnmodified) {
                    Message msg = Message.obtain(this.mHandler, 101);
                    msg.setData(bundle);
                    DownloadTouchIcon icon = new DownloadTouchIcon(this, msg, this.mMap.getString("user_agent"));
                    icon.execute(this.mTouchIconUrl);
                } else {
                    sendBroadcast(BookmarkUtils.createAddToHomeIntent(this, url2, title, null, favicon));
                }
            } else {
                bundle.putParcelable("thumbnail", thumbnail);
                bundle.putBoolean("remove_thumbnail", !urlUnmodified);
                bundle.putString("touch_icon_url", this.mTouchIconUrl);
                Message msg2 = Message.obtain(this.mHandler, 100);
                msg2.setData(bundle);
                Thread t = new Thread(new SaveBookmarkRunnable(getApplicationContext(), msg2));
                t.start();
            }
            setResult(-1);
            LogTag.logBookmarkAdded(url2, "bookmarkview");
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (this.mAccountSpinner == parent) {
            long root = this.mAccountAdapter.getItem(position).rootFolderId;
            if (root != this.mRootFolder) {
                onRootFolderFound(root);
                this.mFolderAdapter.clearRecentFolder();
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public static class CustomListView extends ListView {
        private EditText mEditText;

        public void addEditText(EditText editText) {
            this.mEditText = editText;
        }

        public CustomListView(Context context) {
            super(context);
        }

        public CustomListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomListView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public boolean checkInputConnectionProxy(View view) {
            return view == this.mEditText;
        }
    }

    static class AccountsLoader extends CursorLoader {
        static final String[] PROJECTION = {"account_name", "account_type", "root_id"};

        public AccountsLoader(Context context) {
            super(context, BrowserContract.Accounts.CONTENT_URI, PROJECTION, null, null, null);
        }
    }

    public static class BookmarkAccount {
        String accountName;
        String accountType;
        private String mLabel;
        public long rootFolderId;

        public BookmarkAccount(Context context, Cursor cursor) {
            this.accountName = cursor.getString(0);
            this.accountType = cursor.getString(1);
            this.rootFolderId = cursor.getLong(2);
            this.mLabel = this.accountName;
            if (TextUtils.isEmpty(this.mLabel)) {
                this.mLabel = context.getString(R.string.local_bookmarks);
            }
        }

        public String toString() {
            return this.mLabel;
        }
    }

    static class EditBookmarkInfo {
        String accountName;
        String accountType;
        String lastUsedAccountName;
        String lastUsedAccountType;
        String lastUsedTitle;
        String parentTitle;
        String title;
        long id = -1;
        long parentId = -1;
        long lastUsedId = -1;

        EditBookmarkInfo() {
        }
    }

    static class EditBookmarkInfoLoader extends AsyncTaskLoader<EditBookmarkInfo> {
        private Context mContext;
        private Bundle mMap;

        public EditBookmarkInfoLoader(Context context, Bundle bundle) {
            super(context);
            this.mContext = context.getApplicationContext();
            this.mMap = bundle;
        }

        @Override
        public EditBookmarkInfo loadInBackground() {
            ContentResolver cr = this.mContext.getContentResolver();
            EditBookmarkInfo info = new EditBookmarkInfo();
            Cursor c = null;
            try {
                String url = this.mMap.getString("url");
                info.id = this.mMap.getLong("_id", -1L);
                boolean checkForDupe = this.mMap.getBoolean("check_for_dupe");
                if (checkForDupe && info.id == -1 && !TextUtils.isEmpty(url)) {
                    Cursor c2 = cr.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "url=?", new String[]{url}, null);
                    if (c2.getCount() == 1 && c2.moveToFirst()) {
                        info.id = c2.getLong(0);
                    }
                    c2.close();
                }
                if (info.id != -1) {
                    Cursor c3 = cr.query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, info.id), new String[]{"parent", "account_name", "account_type", "title"}, null, null, null);
                    if (c3.moveToFirst()) {
                        info.parentId = c3.getLong(0);
                        info.accountName = c3.getString(1);
                        info.accountType = c3.getString(2);
                        info.title = c3.getString(3);
                    }
                    c3.close();
                    Cursor c4 = cr.query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, info.parentId), new String[]{"title"}, null, null, null);
                    if (c4.moveToFirst()) {
                        info.parentTitle = c4.getString(0);
                    }
                    c4.close();
                }
                c = cr.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"parent"}, null, null, "modified DESC LIMIT 1");
                if (c.moveToFirst()) {
                    long parent = c.getLong(0);
                    c.close();
                    c = cr.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title", "account_name", "account_type"}, "_id=?", new String[]{Long.toString(parent)}, null);
                    if (c.moveToFirst()) {
                        info.lastUsedId = parent;
                        info.lastUsedTitle = c.getString(0);
                        info.lastUsedAccountName = c.getString(1);
                        info.lastUsedAccountType = c.getString(2);
                    }
                    c.close();
                }
                return info;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }
}
