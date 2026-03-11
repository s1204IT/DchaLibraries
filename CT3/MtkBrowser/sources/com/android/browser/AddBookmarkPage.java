package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.res.Configuration;
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
import android.text.InputFilter;
import android.text.Spanned;
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
import com.android.browser.provider.BrowserContract;
import com.mediatek.browser.ext.IBrowserBookmarkExt;
import com.mediatek.browser.ext.IBrowserFeatureIndexExt;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class AddBookmarkPage extends Activity implements View.OnClickListener, TextView.OnEditorActionListener, AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>, BreadCrumbView.Controller, FolderSpinner.OnSetSelectionListener, AdapterView.OnItemSelectedListener {
    private ArrayAdapter<BookmarkAccount> mAccountAdapter;
    private Spinner mAccountSpinner;
    private FolderAdapter mAdapter;
    private View mAddNewFolder;
    private View mAddSeparator;
    private EditText mAddress;
    private IBrowserBookmarkExt mBookmarkExt;
    private TextView mButton;
    private View mCancelButton;
    private View mCrumbHolder;
    private BreadCrumbView mCrumbs;
    private long mCurrentId;
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
    private EditText mTitle;
    private TextView mTopLevelLabel;
    private String mTouchIconUrl;
    private AlertDialog mWarningDialog;
    private final String LOGTAG = "Bookmarks";
    private final int LOADER_ID_ACCOUNTS = 0;
    private final int LOADER_ID_FOLDER_CONTENTS = 1;
    private final int LOADER_ID_EDIT_INFO = 2;
    private long mCurrentFolder = -1;
    private boolean mSaveToHomeScreen = false;
    private long mOverwriteBookmarkId = -1;
    private int mRestoreFolder = -2;
    private DialogInterface.OnClickListener mAlertDlgOk = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface v, int which) {
            if (!AddBookmarkPage.this.save()) {
                return;
            }
            AddBookmarkPage.this.finish();
        }
    };
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
                if (AddBookmarkPage.this.mEditingFolder) {
                    AddBookmarkPage.this.mFakeTitle.setText(R.string.edit_folder);
                } else {
                    AddBookmarkPage.this.mFakeTitle.setText(R.string.edit_bookmark);
                }
                AddBookmarkPage.this.mFolderAdapter.setOtherFolderDisplayText(info.parentTitle);
                AddBookmarkPage.this.mMap.putLong("_id", info.id);
                setAccount = true;
                AddBookmarkPage.this.setAccount(info.accountName, info.accountType);
                AddBookmarkPage.this.mCurrentFolder = info.parentId;
                AddBookmarkPage.this.onCurrentFolderFound();
                if (AddBookmarkPage.this.mRestoreFolder >= 0) {
                    AddBookmarkPage.this.mFolder.setSelectionIgnoringSelectionChange(AddBookmarkPage.this.mRestoreFolder);
                    AddBookmarkPage.this.mRestoreFolder = -2;
                }
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
                    if (AddBookmarkPage.this.mRestoreFolder >= 0) {
                        AddBookmarkPage.this.mFolder.setSelectionIgnoringSelectionChange(AddBookmarkPage.this.mRestoreFolder);
                        AddBookmarkPage.this.mRestoreFolder = -2;
                    }
                }
            }
            if (setAccount) {
                return;
            }
            AddBookmarkPage.this.mAccountSpinner.setSelection(0);
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
        if (folder == this.mRootFolder && account != null) {
            return BookmarksLoader.addAccount(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, account.accountType, account.accountName);
        }
        return BrowserContract.Bookmarks.buildFolderUri(folder);
    }

    @Override
    public void onTop(BreadCrumbView view, int level, Object data) {
        if (data == null) {
            return;
        }
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

    private void setShowBookmarkIcon(boolean show) {
        this.mTopLevelLabel.setCompoundDrawablesWithIntrinsicBounds(show ? this.mHeaderIcon : null, (Drawable) null, (Drawable) null, (Drawable) null);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v != this.mFolderNamer) {
            return false;
        }
        if (v.getText().length() > 0) {
            if (actionId == 6 || actionId == 0) {
                hideSoftInput();
                return true;
            }
            return true;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void hideSoftInput() {
        Log.d("browser/AddBookmarkPage", "hideSoftInput");
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    private void switchToDefaultView(boolean changedFolder) {
        this.mFolderSelector.setVisibility(8);
        this.mDefaultView.setVisibility(0);
        this.mCrumbHolder.setVisibility(8);
        this.mFakeTitleHolder.setVisibility(0);
        if (changedFolder) {
            Object data = this.mCrumbs.getTopData();
            if (data == null) {
                return;
            }
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
            this.mOverwriteBookmarkId = -1L;
            if (!save()) {
                return;
            }
            finish();
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
            this.mFolderNamer.setSelection(this.mFolderNamer.length());
            this.mFolderNamer.requestFocus();
            this.mAddNewFolder.setVisibility(8);
            this.mAddSeparator.setVisibility(8);
            InputMethodManager imm = getInputMethodManager();
            imm.focusIn(this.mListView);
            imm.showSoftInput(this.mFolderNamer, 1);
            return;
        }
        if (v != this.mRemoveLink) {
            return;
        }
        if (!this.mEditingExisting) {
            throw new AssertionError("Remove button should not be shown for new bookmarks");
        }
        long id = this.mMap.getLong("_id");
        createHandler();
        Message msg = Message.obtain(this.mHandler, 102);
        if (this.mEditingFolder) {
            BookmarkUtils.displayRemoveFolderDialog(id, this.mTitle.getText().toString(), this, msg);
        } else {
            BookmarkUtils.displayRemoveBookmarkDialog(id, this.mTitle.getText().toString(), this, msg);
        }
    }

    private int haveToOverwriteBookmarkId(String title, String url, long parent) {
        if (!this.mSaveToHomeScreen && !this.mEditingFolder) {
            Log.d("browser/AddBookmarkPage", "Add bookmark page haveToOverwriteBookmarkId mCurrentId:" + this.mCurrentId);
            return Bookmarks.getIdByNameOrUrl(getContentResolver(), title, url, parent, this.mCurrentId);
        }
        return -1;
    }

    private void displayToastForExistingFolder() {
        Toast.makeText(getApplicationContext(), R.string.duplicated_folder_warning, 1).show();
    }

    private boolean isFolderExist(long parentId, String title) {
        boolean exist;
        Log.e("browser/AddBookmarkPage", "BrowserProvider2.isValidAccountName parentId:" + parentId + " title:" + title);
        if (parentId <= 0 || title == null || title.length() == 0) {
            return false;
        }
        Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
        Cursor cursor = null;
        try {
            cursor = getApplicationContext().getContentResolver().query(uri, new String[]{"_id"}, "parent = ? AND deleted = ? AND folder = ? AND title = ?", new String[]{parentId + "", "0", "1", title}, null);
            if (cursor != null) {
                exist = cursor.getCount() != 0;
            }
            return exist;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void displayAlertDialogForExistingBookmark() {
        new AlertDialog.Builder(this).setTitle(R.string.duplicated_bookmark).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getText(R.string.duplicated_bookmark_warning).toString()).setPositiveButton(R.string.ok, this.mAlertDlgOk).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
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
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
        if (!cancel && !TextUtils.isEmpty(this.mFolderNamer.getText())) {
            String name = this.mFolderNamer.getText().toString();
            long id = addFolderToCurrent(this.mFolderNamer.getText().toString());
            descendInto(name, id);
        }
        setShowFolderNamer(false);
        this.mBookmarkExt.showCustomizedEditFolderNewFolderView(this.mAddNewFolder, this.mAddSeparator, this.mMap);
    }

    private long addFolderToCurrent(String name) {
        long currentFolder;
        ContentValues values = new ContentValues();
        values.put("title", name);
        values.put("folder", (Integer) 1);
        Object data = null;
        if (this.mCrumbs != null) {
            data = this.mCrumbs.getTopData();
        }
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

    public static long addFolderToRoot(Context context, String name) {
        ContentValues values = new ContentValues();
        values.put("title", name);
        values.put("folder", (Integer) 1);
        values.put("parent", (Long) 1L);
        Uri uri = context.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
        if (uri != null) {
            return ContentUris.parseId(uri);
        }
        return getIdFromName(context, name);
    }

    private static long getIdFromName(Context context, String name) {
        long id = -1;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "title = ? AND deleted = ? AND folder = ? AND parent = ?", new String[]{name, "0", "1", "1"}, null);
            if (cursor != null && cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    id = cursor.getLong(0);
                }
            }
            return id;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void switchToFolderSelector() {
        this.mListView.setSelection(0);
        this.mDefaultView.setVisibility(8);
        this.mFolderSelector.setVisibility(0);
        this.mCrumbHolder.setVisibility(0);
        this.mFakeTitleHolder.setVisibility(8);
        this.mBookmarkExt.showCustomizedEditFolderNewFolderView(this.mAddNewFolder, this.mAddSeparator, this.mMap);
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    private void descendInto(String foldername, long id) {
        if (id != -1) {
            this.mCrumbs.pushView(foldername, new Folder(foldername, id));
            this.mCrumbs.notifyController();
        } else {
            Toast.makeText(getApplicationContext(), R.string.duplicated_folder_warning, 1).show();
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
        if (show == this.mIsFolderNamerShowing) {
            return;
        }
        this.mIsFolderNamerShowing = show;
        if (show) {
            this.mListView.addFooterView(this.mFolderNamerHolder);
        } else {
            this.mListView.removeFooterView(this.mFolderNamerHolder);
        }
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        if (!show) {
            return;
        }
        this.mListView.setSelection(this.mListView.getCount() - 1);
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
            return view;
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty() && !AddBookmarkPage.this.mIsFolderNamerShowing;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mTitle != null && this.mTitle.getError() != null) {
            outState.putBoolean("titleHasError", true);
        }
        if (this.mAddress == null || this.mAddress.getError() == null) {
            return;
        }
        outState.putBoolean("addrHasError", true);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
        Resources r = getResources();
        if (inState != null && inState.getBoolean("titleHasError") && this.mTitle != null && this.mTitle.getText().toString().trim().length() == 0) {
            if (this.mEditingFolder) {
                this.mTitle.setError(r.getText(R.string.folder_needs_title));
            } else {
                this.mTitle.setError(r.getText(R.string.bookmark_needs_title));
            }
        }
        if (inState == null || !inState.getBoolean("addrHasError") || this.mAddress == null || this.mAddress.getText().toString().trim().length() != 0) {
            return;
        }
        this.mAddress.setError(r.getText(R.string.bookmark_needs_url));
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mRestoreFolder = this.mFolder.getSelectedItemPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private InputFilter[] generateInputFilter(final int nLimit) {
        InputFilter[] contentFilters = {new InputFilter.LengthFilter(nLimit) {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                int keep = nLimit - (dest.length() - (dend - dstart));
                if (keep <= 0) {
                    AddBookmarkPage.this.showWarningDialog();
                    return "";
                }
                if (keep >= end - start) {
                    return null;
                }
                if (keep < source.length()) {
                    AddBookmarkPage.this.showWarningDialog();
                }
                return source.subSequence(start, start + keep);
            }
        }};
        return contentFilters;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(1);
        this.mMap = getIntent().getExtras();
        setContentView(R.layout.browser_add_bookmark);
        Window window = getWindow();
        if (BrowserActivity.isTablet(this)) {
            window.setSoftInputMode(16);
        }
        this.mBookmarkExt = Extensions.getBookmarkPlugin(getApplicationContext());
        String title = null;
        String url = null;
        this.mFakeTitle = (TextView) findViewById(R.id.fake_title);
        if (this.mMap != null) {
            Bundle b = this.mMap.getBundle("bookmark");
            if (b != null) {
                this.mEditingFolder = this.mMap.getBoolean("is_folder", false);
                this.mMap = b;
                this.mEditingExisting = this.mBookmarkExt.customizeEditExistingFolderState(this.mMap, true);
                this.mCurrentId = this.mMap.getLong("_id", -1L);
                Log.d("browser/AddBookmarkPage", "Add bookmark page onCreate mCurrentId:" + this.mCurrentId);
                if (this.mEditingFolder) {
                    String titleString = this.mBookmarkExt.getCustomizedEditFolderFakeTitleString(this.mMap, getString(R.string.edit_folder));
                    this.mFakeTitle.setText(titleString);
                    findViewById(R.id.row_address).setVisibility(8);
                } else {
                    this.mFakeTitle.setText(R.string.edit_bookmark);
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
            Log.i("Bookmarks", "CurrentFolderId: " + this.mCurrentFolder);
        }
        this.mWarningDialog = new AlertDialog.Builder(this).create();
        this.mTitle = (EditText) findViewById(R.id.title);
        int nLimit = getResources().getInteger(R.integer.bookmark_title_maxlength);
        this.mTitle.setFilters(generateInputFilter(nLimit));
        this.mTitle.setText(title);
        if (title != null) {
            this.mTitle.setSelection(this.mTitle.getText().length());
        }
        this.mAddress = (EditText) findViewById(R.id.address);
        Context context = getApplicationContext();
        InputFilter[] addressFilters = Extensions.getUrlPlugin(context).checkUrlLengthLimit(context);
        if (addressFilters != null) {
            this.mAddress.setFilters(addressFilters);
        }
        this.mAddress.setText(url);
        this.mButton = (TextView) findViewById(R.id.OK);
        this.mButton.setOnClickListener(this);
        this.mCancelButton = findViewById(R.id.cancel);
        this.mCancelButton.setOnClickListener(this);
        this.mFolder = (FolderSpinner) findViewById(R.id.folder);
        this.mFolderAdapter = new FolderSpinnerAdapter(this, !this.mEditingFolder);
        this.mFolder.setAdapter((SpinnerAdapter) this.mFolderAdapter);
        this.mFolder.setOnSetSelectionListener(this);
        if (this.mCurrentFolder != -1 && this.mCurrentFolder != 1) {
            this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 1 : 2);
            this.mFolderAdapter.setOtherFolderDisplayText(getNameFromId(this.mCurrentFolder));
        } else {
            this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 0 : 1);
        }
        this.mDefaultView = findViewById(R.id.default_view);
        this.mFolderSelector = findViewById(R.id.folder_selector);
        this.mFolderNamerHolder = getLayoutInflater().inflate(R.layout.new_folder_layout, (ViewGroup) null);
        this.mFolderNamer = (EditText) this.mFolderNamerHolder.findViewById(R.id.folder_namer);
        int limit = getResources().getInteger(R.integer.bookmark_title_maxlength);
        this.mFolderNamer.setFilters(generateInputFilter(limit));
        this.mFolderNamer.setOnEditorActionListener(this);
        this.mFolderCancel = this.mFolderNamerHolder.findViewById(R.id.close);
        this.mFolderCancel.setOnClickListener(this);
        this.mAddNewFolder = findViewById(R.id.add_new_folder);
        this.mAddNewFolder.setOnClickListener(this);
        this.mAddSeparator = findViewById(R.id.add_divider);
        this.mBookmarkExt.showCustomizedEditFolderNewFolderView(this.mAddNewFolder, this.mAddSeparator, this.mMap);
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

    private String getNameFromId(long mCurrentFolder2) {
        String title = "";
        Cursor cursor = null;
        try {
            cursor = getApplicationContext().getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title"}, "_id = ? AND deleted = ? AND folder = ? ", new String[]{String.valueOf(mCurrentFolder2), "0", "1"}, null);
            if (cursor != null && cursor.moveToNext()) {
                title = cursor.getString(0);
            }
            Log.d("browser/AddBookmarkPage", "title :" + title);
            return title;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void showWarningDialog() {
        if (this.mWarningDialog == null || this.mWarningDialog.isShowing()) {
            return;
        }
        this.mWarningDialog.setTitle(R.string.max_input_browser_search_title);
        this.mWarningDialog.setMessage(getString(R.string.max_input_browser_search));
        this.mWarningDialog.setButton(getString(R.string.max_input_browser_search_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        this.mWarningDialog.show();
    }

    public void showRemoveButton() {
        findViewById(R.id.remove_divider).setVisibility(0);
        this.mRemoveLink = findViewById(R.id.remove);
        this.mRemoveLink.setVisibility(0);
        this.mRemoveLink.setOnClickListener(this);
    }

    private void onRootFolderFound(long root) {
        this.mRootFolder = root;
        if (this.mCurrentFolder == -1 || this.mEditingExisting) {
            this.mCurrentFolder = this.mRootFolder;
        }
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
        if (!this.mSaveToHomeScreen) {
            if (this.mCurrentFolder != -1 && this.mCurrentFolder != this.mRootFolder) {
                this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 1 : 2);
            } else {
                setShowBookmarkIcon(true);
                if (this.mBookmarkExt.shouldSetCustomizedEditFolderSelection(this.mMap, !this.mEditingFolder)) {
                    this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 0 : 1);
                }
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
            Bitmap bitmap = invalidateThumbnail ? null : (Bitmap) bundle.getParcelable("thumbnail");
            String touchIconUrl = bundle.getString("touch_icon_url");
            try {
                ContentResolver cr = AddBookmarkPage.this.getContentResolver();
                Log.i("Bookmarks", "mCurrentFolder: " + AddBookmarkPage.this.mCurrentFolder);
                Bookmarks.addBookmark(AddBookmarkPage.this, false, url, title, bitmap, AddBookmarkPage.this.mCurrentFolder);
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
        long mBookmarkCurrentId;
        Context mContext;
        Long mId;

        public UpdateBookmarkTask(Context context, long id) {
            this.mContext = context.getApplicationContext();
            this.mId = Long.valueOf(id);
        }

        @Override
        public Void doInBackground(ContentValues... params) {
            if (params.length < 1) {
                throw new IllegalArgumentException("No ContentValues provided!");
            }
            Uri uri = ContentUris.withAppendedId(BookmarkUtils.getBookmarksUri(this.mContext), this.mId.longValue());
            this.mContext.getContentResolver().update(uri, params[0], null, null);
            Log.d("browser/AddBookmarkPage", "UpdateBookmarkTask doInBackground:");
            if (params.length > 1) {
                this.mBookmarkCurrentId = params[1].getAsLong("bookmark_current_id").longValue();
            } else {
                this.mBookmarkCurrentId = -1L;
            }
            return null;
        }

        @Override
        public void onPostExecute(Void o) {
            Log.d("browser/AddBookmarkPage", "UpdateBookmarkTask onPostExecute mBookmarkCurrentId:" + this.mBookmarkCurrentId);
            if (this.mBookmarkCurrentId <= 0) {
                return;
            }
            this.mContext.getContentResolver().delete(BrowserContract.Bookmarks.CONTENT_URI, "_id = ?", new String[]{String.valueOf(this.mBookmarkCurrentId)});
        }
    }

    private void createHandler() {
        if (this.mHandler != null) {
            return;
        }
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case IBrowserFeatureIndexExt.CUSTOM_PREFERENCE_LIST:
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

    boolean save() {
        Bitmap thumbnail;
        Bitmap bitmap;
        String beforeEditTitle;
        createHandler();
        String title = this.mTitle.getText().toString().trim();
        String unfilteredUrl = UrlUtils.fixUrl(this.mAddress.getText().toString());
        boolean emptyTitle = title.length() == 0;
        boolean emptyUrl = unfilteredUrl.trim().length() == 0;
        Resources r = getResources();
        if (emptyTitle || (emptyUrl && !this.mEditingFolder)) {
            if (emptyTitle) {
                if (this.mEditingFolder) {
                    this.mTitle.setError(r.getText(R.string.folder_needs_title));
                } else {
                    this.mTitle.setError(r.getText(R.string.bookmark_needs_title));
                }
            }
            if (emptyUrl) {
                this.mAddress.setError(r.getText(R.string.bookmark_needs_url));
                return false;
            }
            return false;
        }
        Boolean result = this.mBookmarkExt.saveCustomizedEditFolder(getApplicationContext(), title, this.mCurrentFolder, this.mMap, getString(R.string.duplicated_folder_warning));
        if (result != null) {
            if (result.booleanValue()) {
                setResult(-1);
            }
            return result.booleanValue();
        }
        String url = unfilteredUrl.trim();
        if (!this.mEditingFolder) {
            try {
                if (!url.toLowerCase().startsWith("javascript:")) {
                    String url2 = URLEncoder.encode(url);
                    URI uriObj = new URI(url2);
                    String scheme = uriObj.getScheme();
                    if (!Bookmarks.urlHasAcceptableScheme(unfilteredUrl.trim())) {
                        if (scheme != null) {
                            this.mAddress.setError(r.getText(R.string.bookmark_cannot_save_url));
                            return false;
                        }
                        try {
                            WebAddress address = new WebAddress(unfilteredUrl);
                            if (address.getHost().length() == 0) {
                                throw new URISyntaxException("", "");
                            }
                            url2 = address.toString();
                        } catch (ParseException e) {
                            throw new URISyntaxException("", "");
                        }
                    }
                    url = URLDecoder.decode(url2);
                }
            } catch (URISyntaxException e2) {
                this.mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
                return false;
            }
        }
        boolean urlUnmodified = url.equals(this.mOriginalUrl);
        if (this.mOverwriteBookmarkId > 0) {
            ContentValues valuesDeleteItem = new ContentValues();
            valuesDeleteItem.put("bookmark_current_id", Long.valueOf(this.mCurrentId));
            ContentValues values = new ContentValues();
            values.put("title", title);
            values.put("parent", Long.valueOf(this.mCurrentFolder));
            values.put("url", url);
            if (!urlUnmodified) {
                values.putNull("thumbnail");
            }
            if (values.size() > 0) {
                new UpdateBookmarkTask(getApplicationContext(), this.mOverwriteBookmarkId).execute(values, valuesDeleteItem);
            }
            this.mOverwriteBookmarkId = -1L;
            setResult(-1);
            return true;
        }
        this.mOverwriteBookmarkId = haveToOverwriteBookmarkId(title, url, this.mCurrentFolder);
        if (this.mOverwriteBookmarkId > 0) {
            displayAlertDialogForExistingBookmark();
            return false;
        }
        if (this.mEditingExisting && this.mEditingFolder) {
            Log.d("browser/AddBookmarkPage", "editing folder save");
            long editId = this.mMap.getLong("_id", -1L);
            long beforeParentId = this.mMap.getLong("parent", -1L);
            long currentParentId = this.mCurrentFolder;
            if (beforeParentId == -1) {
                beforeParentId = this.mRootFolder;
            }
            if (currentParentId == -1) {
                currentParentId = this.mRootFolder;
            }
            if (beforeParentId == currentParentId && (beforeEditTitle = getTitleFromId(editId)) != null && beforeEditTitle.equals(title)) {
                Log.d("browser/AddBookmarkPage", "edit folder save, does not change anything");
                return true;
            }
        }
        if (this.mEditingFolder) {
            boolean isExist = isFolderExist(this.mCurrentFolder, title);
            if (isExist) {
                displayToastForExistingFolder();
                return false;
            }
        }
        if (this.mSaveToHomeScreen) {
            this.mEditingExisting = false;
        }
        if (this.mEditingExisting) {
            Long id = Long.valueOf(this.mMap.getLong("_id"));
            ContentValues values2 = new ContentValues();
            values2.put("title", title);
            values2.put("parent", Long.valueOf(this.mCurrentFolder));
            if (!this.mEditingFolder) {
                values2.put("url", url);
                if (!urlUnmodified) {
                    values2.putNull("thumbnail");
                }
            }
            if (values2.size() > 0) {
                new UpdateBookmarkTask(getApplicationContext(), id.longValue()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, values2);
            }
            setResult(-1);
            return true;
        }
        if (urlUnmodified) {
            thumbnail = (Bitmap) this.mMap.getParcelable("thumbnail");
            bitmap = (Bitmap) this.mMap.getParcelable("favicon");
        } else {
            thumbnail = null;
            bitmap = null;
        }
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("url", url);
        bundle.putParcelable("favicon", bitmap);
        if (this.mSaveToHomeScreen) {
            if (this.mTouchIconUrl != null && urlUnmodified) {
                Message msg = Message.obtain(this.mHandler, 101);
                msg.setData(bundle);
                DownloadTouchIcon icon = new DownloadTouchIcon(this, msg, this.mMap.getString("user_agent"));
                icon.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.mTouchIconUrl);
            } else {
                sendBroadcast(BookmarkUtils.createAddToHomeIntent(this, url, title, null, bitmap));
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
        LogTag.logBookmarkAdded(url, "bookmarkview");
        return true;
    }

    private String getTitleFromId(long editId) {
        Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
        Cursor cursor = null;
        String title = null;
        try {
            cursor = getApplicationContext().getContentResolver().query(uri, new String[]{"title"}, "_id = ? AND deleted = ? AND folder = ?", new String[]{editId + "", "0", "1"}, null);
            if (cursor != null && cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    title = cursor.getString(0);
                }
            }
            return title;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (this.mAccountSpinner != parent) {
            return;
        }
        long root = this.mAccountAdapter.getItem(position).rootFolderId;
        if (root == this.mRootFolder) {
            return;
        }
        this.mCurrentFolder = -1L;
        onRootFolderFound(root);
        this.mFolderAdapter.clearRecentFolder();
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
            if (!TextUtils.isEmpty(this.mLabel)) {
                return;
            }
            this.mLabel = context.getString(R.string.local_bookmarks);
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
