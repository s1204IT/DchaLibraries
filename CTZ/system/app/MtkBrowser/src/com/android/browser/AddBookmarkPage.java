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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/* loaded from: classes.dex */
public class AddBookmarkPage extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener, TextView.OnEditorActionListener, BreadCrumbView.Controller, FolderSpinner.OnSetSelectionListener {
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
    private DialogInterface.OnClickListener mAlertDlgOk = new DialogInterface.OnClickListener() { // from class: com.android.browser.AddBookmarkPage.1
        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            if (AddBookmarkPage.this.save()) {
                AddBookmarkPage.this.finish();
            }
        }
    };
    private LoaderManager.LoaderCallbacks<EditBookmarkInfo> mEditInfoLoaderCallbacks = new LoaderManager.LoaderCallbacks<EditBookmarkInfo>() { // from class: com.android.browser.AddBookmarkPage.2
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<EditBookmarkInfo> loader) {
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<EditBookmarkInfo> loader, EditBookmarkInfo editBookmarkInfo) {
            boolean z;
            if (editBookmarkInfo.id != -1) {
                AddBookmarkPage.this.mEditingExisting = true;
                AddBookmarkPage.this.showRemoveButton();
                if (AddBookmarkPage.this.mEditingFolder) {
                    AddBookmarkPage.this.mFakeTitle.setText(R.string.edit_folder);
                } else {
                    AddBookmarkPage.this.mFakeTitle.setText(R.string.edit_bookmark);
                }
                AddBookmarkPage.this.mFolderAdapter.setOtherFolderDisplayText(editBookmarkInfo.parentTitle);
                AddBookmarkPage.this.mMap.putLong("_id", editBookmarkInfo.id);
                AddBookmarkPage.this.setAccount(editBookmarkInfo.accountName, editBookmarkInfo.accountType);
                AddBookmarkPage.this.mCurrentFolder = editBookmarkInfo.parentId;
                AddBookmarkPage.this.onCurrentFolderFound();
                if (AddBookmarkPage.this.mRestoreFolder >= 0) {
                    AddBookmarkPage.this.mFolder.setSelectionIgnoringSelectionChange(AddBookmarkPage.this.mRestoreFolder);
                    AddBookmarkPage.this.mRestoreFolder = -2;
                }
                z = true;
            } else {
                z = false;
            }
            if (editBookmarkInfo.lastUsedId != -1 && editBookmarkInfo.lastUsedId != editBookmarkInfo.id && !AddBookmarkPage.this.mEditingFolder) {
                if (z && editBookmarkInfo.lastUsedId != AddBookmarkPage.this.mRootFolder && TextUtils.equals(editBookmarkInfo.lastUsedAccountName, editBookmarkInfo.accountName) && TextUtils.equals(editBookmarkInfo.lastUsedAccountType, editBookmarkInfo.accountType)) {
                    AddBookmarkPage.this.mFolderAdapter.addRecentFolder(editBookmarkInfo.lastUsedId, editBookmarkInfo.lastUsedTitle);
                } else if (!z) {
                    AddBookmarkPage.this.setAccount(editBookmarkInfo.lastUsedAccountName, editBookmarkInfo.lastUsedAccountType);
                    if (editBookmarkInfo.lastUsedId != AddBookmarkPage.this.mRootFolder) {
                        AddBookmarkPage.this.mFolderAdapter.addRecentFolder(editBookmarkInfo.lastUsedId, editBookmarkInfo.lastUsedTitle);
                    }
                    if (AddBookmarkPage.this.mRestoreFolder >= 0) {
                        AddBookmarkPage.this.mFolder.setSelectionIgnoringSelectionChange(AddBookmarkPage.this.mRestoreFolder);
                        AddBookmarkPage.this.mRestoreFolder = -2;
                    }
                    z = true;
                }
            }
            if (!z) {
                AddBookmarkPage.this.mAccountSpinner.setSelection(0);
            }
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<EditBookmarkInfo> onCreateLoader(int i, Bundle bundle) {
            return new EditBookmarkInfoLoader(AddBookmarkPage.this, AddBookmarkPage.this.mMap);
        }
    };

    private static class Folder {
        long Id;
        String Name;

        Folder(String str, long j) {
            this.Name = str;
            this.Id = j;
        }
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService("input_method");
    }

    private Uri getUriForFolder(long j) {
        BookmarkAccount bookmarkAccount = (BookmarkAccount) this.mAccountSpinner.getSelectedItem();
        if (j == this.mRootFolder && bookmarkAccount != null) {
            return BookmarksLoader.addAccount(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, bookmarkAccount.accountType, bookmarkAccount.accountName);
        }
        return BrowserContract.Bookmarks.buildFolderUri(j);
    }

    @Override // com.android.browser.BreadCrumbView.Controller
    public void onTop(BreadCrumbView breadCrumbView, int i, Object obj) {
        if (obj == null) {
            return;
        }
        long j = ((Folder) obj).Id;
        CursorLoader cursorLoader = (CursorLoader) getLoaderManager().getLoader(1);
        cursorLoader.setUri(getUriForFolder(j));
        cursorLoader.forceLoad();
        if (this.mIsFolderNamerShowing) {
            completeOrCancelFolderNaming(true);
        }
        setShowBookmarkIcon(i == 1);
    }

    private void setShowBookmarkIcon(boolean z) {
        this.mTopLevelLabel.setCompoundDrawablesWithIntrinsicBounds(z ? this.mHeaderIcon : null, (Drawable) null, (Drawable) null, (Drawable) null);
    }

    @Override // android.widget.TextView.OnEditorActionListener
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (textView == this.mFolderNamer) {
            if (textView.getText().length() > 0) {
                if (i == 6 || i == 0) {
                    hideSoftInput();
                    return true;
                }
                return true;
            }
            return true;
        }
        return false;
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    private void hideSoftInput() {
        Log.d("browser/AddBookmarkPage", "hideSoftInput");
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    private void switchToDefaultView(boolean z) {
        this.mFolderSelector.setVisibility(8);
        this.mDefaultView.setVisibility(0);
        this.mCrumbHolder.setVisibility(8);
        this.mFakeTitleHolder.setVisibility(0);
        if (z) {
            Object topData = this.mCrumbs.getTopData();
            if (topData != null) {
                Folder folder = (Folder) topData;
                this.mCurrentFolder = folder.Id;
                if (this.mCurrentFolder == this.mRootFolder) {
                    this.mFolder.setSelectionIgnoringSelectionChange(1 ^ (this.mEditingFolder ? 1 : 0));
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
            this.mFolder.setSelectionIgnoringSelectionChange(1 ^ (this.mEditingFolder ? 1 : 0));
            return;
        }
        Object topData2 = this.mCrumbs.getTopData();
        if (topData2 != null) {
            Folder folder2 = (Folder) topData2;
            if (folder2.Id == this.mCurrentFolder) {
                this.mFolderAdapter.setOtherFolderDisplayText(folder2.Name);
                return;
            }
        }
        setupTopCrumb();
        getLoaderManager().restartLoader(1, null, this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.mButton) {
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
            if (save()) {
                finish();
                return;
            }
            return;
        }
        if (view == this.mCancelButton) {
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
        if (view == this.mFolderCancel) {
            completeOrCancelFolderNaming(true);
            return;
        }
        if (view == this.mAddNewFolder) {
            setShowFolderNamer(true);
            this.mFolderNamer.setText(R.string.new_folder);
            this.mFolderNamer.setSelection(this.mFolderNamer.length());
            this.mFolderNamer.requestFocus();
            this.mAddNewFolder.setVisibility(8);
            this.mAddSeparator.setVisibility(8);
            InputMethodManager inputMethodManager = getInputMethodManager();
            inputMethodManager.focusIn(this.mListView);
            inputMethodManager.showSoftInput(this.mFolderNamer, 1);
            return;
        }
        if (view == this.mRemoveLink) {
            if (!this.mEditingExisting) {
                throw new AssertionError("Remove button should not be shown for new bookmarks");
            }
            long j = this.mMap.getLong("_id");
            createHandler();
            Message messageObtain = Message.obtain(this.mHandler, 102);
            if (this.mEditingFolder) {
                BookmarkUtils.displayRemoveFolderDialog(j, this.mTitle.getText().toString(), this, messageObtain);
            } else {
                BookmarkUtils.displayRemoveBookmarkDialog(j, this.mTitle.getText().toString(), this, messageObtain);
            }
        }
    }

    private int haveToOverwriteBookmarkId(String str, String str2, long j) {
        if (!this.mSaveToHomeScreen && !this.mEditingFolder) {
            Log.d("browser/AddBookmarkPage", "Add bookmark page haveToOverwriteBookmarkId mCurrentId:" + this.mCurrentId);
            return Bookmarks.getIdByNameOrUrl(getContentResolver(), str, str2, j, this.mCurrentId);
        }
        return -1;
    }

    private void displayToastForExistingFolder() {
        Toast.makeText(getApplicationContext(), R.string.duplicated_folder_warning, 1).show();
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x0076  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean isFolderExist(long j, String str) throws Throwable {
        boolean z;
        Log.e("browser/AddBookmarkPage", "BrowserProvider2.isValidAccountName parentId:" + j + " title:" + str);
        if (j <= 0 || str == null || str.length() == 0) {
            return false;
        }
        Cursor cursor = null;
        try {
            Cursor cursorQuery = getApplicationContext().getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "parent = ? AND deleted = ? AND folder = ? AND title = ?", new String[]{j + "", "0", "1", str}, null);
            if (cursorQuery != null) {
                try {
                    z = cursorQuery.getCount() != 0;
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return z;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void displayAlertDialogForExistingBookmark() {
        new AlertDialog.Builder(this).setTitle(R.string.duplicated_bookmark).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getText(R.string.duplicated_bookmark_warning).toString()).setPositiveButton(R.string.ok, this.mAlertDlgOk).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    @Override // com.android.browser.addbookmark.FolderSpinner.OnSetSelectionListener
    public void onSetSelection(long j) {
        switch ((int) j) {
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
                getLoaderManager().restartLoader(1, null, this);
                break;
        }
    }

    private void completeOrCancelFolderNaming(boolean z) {
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
        if (!z && !TextUtils.isEmpty(this.mFolderNamer.getText()) && !TextUtils.isEmpty(this.mFolderNamer.getText().toString().trim())) {
            descendInto(this.mFolderNamer.getText().toString(), addFolderToCurrent(this.mFolderNamer.getText().toString()));
        }
        setShowFolderNamer(false);
        this.mBookmarkExt.showCustomizedEditFolderNewFolderView(this.mAddNewFolder, this.mAddSeparator, this.mMap);
    }

    private long addFolderToCurrent(String str) {
        Object topData;
        long j;
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", str);
        contentValues.put("folder", (Integer) 1);
        if (this.mCrumbs != null) {
            topData = this.mCrumbs.getTopData();
        } else {
            topData = null;
        }
        if (topData != null) {
            j = ((Folder) topData).Id;
        } else {
            j = this.mRootFolder;
        }
        contentValues.put("parent", Long.valueOf(j));
        Uri uriInsert = getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues);
        if (uriInsert != null) {
            return ContentUris.parseId(uriInsert);
        }
        return -1L;
    }

    public static long addFolderToRoot(Context context, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", str);
        contentValues.put("folder", (Integer) 1);
        contentValues.put("parent", (Long) 1L);
        Uri uriInsert = context.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues);
        if (uriInsert != null) {
            return ContentUris.parseId(uriInsert);
        }
        return getIdFromName(context, str);
    }

    private static long getIdFromName(Context context, String str) throws Throwable {
        Cursor cursor = null;
        try {
            Cursor cursorQuery = context.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "title = ? AND deleted = ? AND folder = ? AND parent = ?", new String[]{str, "0", "1", "1"}, null);
            long j = -1;
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() != 0) {
                        while (cursorQuery.moveToNext()) {
                            j = cursorQuery.getLong(0);
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return j;
        } catch (Throwable th2) {
            th = th2;
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

    private void descendInto(String str, long j) {
        if (j != -1) {
            this.mCrumbs.pushView(str, new Folder(str, j));
            this.mCrumbs.notifyController();
        } else {
            Toast.makeText(getApplicationContext(), R.string.duplicated_folder_warning, 1).show();
        }
    }

    void setAccount(String str, String str2) {
        for (int i = 0; i < this.mAccountAdapter.getCount(); i++) {
            BookmarkAccount item = this.mAccountAdapter.getItem(i);
            if (TextUtils.equals(item.accountName, str) && TextUtils.equals(item.accountType, str2)) {
                this.mAccountSpinner.setSelection(i);
                onRootFolderFound(item.rootFolderId);
                return;
            }
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        long j;
        switch (i) {
            case 0:
                return new AccountsLoader(this);
            case 1:
                String[] strArr = {"_id", "title", "folder"};
                String str = "folder != 0";
                String[] strArr2 = null;
                if (this.mEditingFolder) {
                    str = "folder != 0 AND _id != ?";
                    strArr2 = new String[]{Long.toString(this.mMap.getLong("_id"))};
                }
                String str2 = str;
                String[] strArr3 = strArr2;
                Object topData = this.mCrumbs.getTopData();
                if (topData != null) {
                    j = ((Folder) topData).Id;
                } else {
                    j = this.mRootFolder;
                }
                return new CursorLoader(this, getUriForFolder(j), strArr, str2, strArr3, "_id ASC");
            default:
                throw new AssertionError("Asking for nonexistant loader!");
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
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

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == 1) {
            this.mAdapter.changeCursor(null);
        }
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        descendInto(((TextView) view.findViewById(android.R.id.text1)).getText().toString(), j);
    }

    private void setShowFolderNamer(boolean z) {
        if (z != this.mIsFolderNamerShowing) {
            this.mIsFolderNamerShowing = z;
            if (z) {
                this.mListView.addFooterView(this.mFolderNamerHolder);
            } else {
                this.mListView.removeFooterView(this.mFolderNamerHolder);
            }
            this.mListView.setAdapter((ListAdapter) this.mAdapter);
            if (z) {
                this.mListView.setSelection(this.mListView.getCount() - 1);
            }
        }
    }

    private class FolderAdapter extends CursorAdapter {
        public FolderAdapter(Context context) {
            super(context, null);
        }

        @Override // android.widget.CursorAdapter
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        }

        @Override // android.widget.CursorAdapter
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.folder_list_item, (ViewGroup) null);
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public boolean isEmpty() {
            return super.isEmpty() && !AddBookmarkPage.this.mIsFolderNamerShowing;
        }
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mTitle != null && this.mTitle.getError() != null) {
            bundle.putBoolean("titleHasError", true);
        }
        if (this.mAddress != null && this.mAddress.getError() != null) {
            bundle.putBoolean("addrHasError", true);
        }
    }

    @Override // android.app.Activity
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        Resources resources = getResources();
        if (bundle != null && bundle.getBoolean("titleHasError") && this.mTitle != null && this.mTitle.getText().toString().trim().length() == 0) {
            if (this.mEditingFolder) {
                this.mTitle.setError(resources.getText(R.string.folder_needs_title));
            } else {
                this.mTitle.setError(resources.getText(R.string.bookmark_needs_title));
            }
        }
        if (bundle != null && bundle.getBoolean("addrHasError") && this.mAddress != null && this.mAddress.getText().toString().trim().length() == 0) {
            this.mAddress.setError(resources.getText(R.string.bookmark_needs_url));
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.mRestoreFolder = this.mFolder.getSelectedItemPosition();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
    }

    private InputFilter[] generateInputFilter(final int i) {
        return new InputFilter[]{new InputFilter.LengthFilter(i) { // from class: com.android.browser.AddBookmarkPage.3
            @Override // android.text.InputFilter.LengthFilter, android.text.InputFilter
            public CharSequence filter(CharSequence charSequence, int i2, int i3, Spanned spanned, int i4, int i5) {
                int length = i - (spanned.length() - (i5 - i4));
                if (length <= 0) {
                    AddBookmarkPage.this.showWarningDialog();
                    return "";
                }
                if (length >= i3 - i2) {
                    return null;
                }
                if (length < charSequence.length()) {
                    AddBookmarkPage.this.showWarningDialog();
                }
                return charSequence.subSequence(i2, length + i2);
            }
        }};
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        String string;
        String string2;
        super.onCreate(bundle);
        requestWindowFeature(1);
        this.mMap = getIntent().getExtras();
        setContentView(R.layout.browser_add_bookmark);
        Window window = getWindow();
        if (BrowserActivity.isTablet(this)) {
            window.setSoftInputMode(16);
        }
        this.mBookmarkExt = Extensions.getBookmarkPlugin(getApplicationContext());
        this.mFakeTitle = (TextView) findViewById(R.id.fake_title);
        if (this.mMap != null) {
            Bundle bundle2 = this.mMap.getBundle("bookmark");
            if (bundle2 != null) {
                this.mEditingFolder = this.mMap.getBoolean("is_folder", false);
                this.mMap = bundle2;
                this.mEditingExisting = this.mBookmarkExt.customizeEditExistingFolderState(this.mMap, true);
                this.mCurrentId = this.mMap.getLong("_id", -1L);
                Log.d("browser/AddBookmarkPage", "Add bookmark page onCreate mCurrentId:" + this.mCurrentId);
                if (this.mEditingFolder) {
                    this.mFakeTitle.setText(this.mBookmarkExt.getCustomizedEditFolderFakeTitleString(this.mMap, getString(R.string.edit_folder)));
                    findViewById(R.id.row_address).setVisibility(8);
                } else {
                    this.mFakeTitle.setText(R.string.edit_bookmark);
                    showRemoveButton();
                }
            } else {
                int i = this.mMap.getInt("gravity", -1);
                if (i != -1) {
                    WindowManager.LayoutParams attributes = window.getAttributes();
                    attributes.gravity = i;
                    window.setAttributes(attributes);
                }
            }
            string = this.mMap.getString("title");
            string2 = this.mMap.getString("url");
            this.mOriginalUrl = string2;
            this.mTouchIconUrl = this.mMap.getString("touch_icon_url");
            this.mCurrentFolder = this.mMap.getLong("parent", -1L);
            Log.i("Bookmarks", "CurrentFolderId: " + this.mCurrentFolder);
        } else {
            string = null;
            string2 = null;
        }
        this.mWarningDialog = new AlertDialog.Builder(this).create();
        this.mTitle = (EditText) findViewById(R.id.title);
        this.mTitle.setFilters(generateInputFilter(getResources().getInteger(R.integer.bookmark_title_maxlength)));
        this.mTitle.setText(string);
        if (string != null) {
            this.mTitle.setSelection(this.mTitle.getText().length());
        }
        this.mAddress = (EditText) findViewById(R.id.address);
        Context applicationContext = getApplicationContext();
        InputFilter[] inputFilterArrCheckUrlLengthLimit = Extensions.getUrlPlugin(applicationContext).checkUrlLengthLimit(applicationContext);
        if (inputFilterArrCheckUrlLengthLimit != null) {
            this.mAddress.setFilters(inputFilterArrCheckUrlLengthLimit);
        }
        this.mAddress.setText(string2);
        this.mButton = (TextView) findViewById(R.id.OK);
        this.mButton.setOnClickListener(this);
        this.mCancelButton = findViewById(R.id.cancel);
        this.mCancelButton.setOnClickListener(this);
        this.mFolder = (FolderSpinner) findViewById(R.id.folder);
        this.mFolderAdapter = new FolderSpinnerAdapter(this, !this.mEditingFolder);
        this.mFolder.setAdapter((SpinnerAdapter) this.mFolderAdapter);
        this.mFolder.setOnSetSelectionListener(this);
        if (this.mCurrentFolder == -1 || this.mCurrentFolder == 1) {
            this.mFolder.setSelectionIgnoringSelectionChange(!this.mEditingFolder ? 1 : 0);
        } else {
            this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 1 : 2);
            this.mFolderAdapter.setOtherFolderDisplayText(getNameFromId(this.mCurrentFolder));
        }
        this.mDefaultView = findViewById(R.id.default_view);
        this.mFolderSelector = findViewById(R.id.folder_selector);
        this.mFolderNamerHolder = getLayoutInflater().inflate(R.layout.new_folder_layout, (ViewGroup) null);
        this.mFolderNamer = (EditText) this.mFolderNamerHolder.findViewById(R.id.folder_namer);
        this.mFolderNamer.setFilters(generateInputFilter(getResources().getInteger(R.integer.bookmark_title_maxlength)));
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
        this.mListView.setEmptyView(findViewById(R.id.empty));
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

    private String getNameFromId(long j) throws Throwable {
        String string = "";
        Cursor cursor = null;
        try {
            Cursor cursorQuery = getApplicationContext().getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title"}, "_id = ? AND deleted = ? AND folder = ? ", new String[]{String.valueOf(j), "0", "1"}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        string = cursorQuery.getString(0);
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            Log.d("browser/AddBookmarkPage", "title :" + string);
            return string;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void showWarningDialog() {
        if (this.mWarningDialog != null && !this.mWarningDialog.isShowing()) {
            this.mWarningDialog.setTitle(R.string.max_input_browser_search_title);
            this.mWarningDialog.setMessage(getString(R.string.max_input_browser_search));
            this.mWarningDialog.setButton(getString(R.string.max_input_browser_search_button), new DialogInterface.OnClickListener() { // from class: com.android.browser.AddBookmarkPage.4
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            this.mWarningDialog.show();
        }
    }

    private void showRemoveButton() {
        findViewById(R.id.remove_divider).setVisibility(0);
        this.mRemoveLink = findViewById(R.id.remove);
        this.mRemoveLink.setVisibility(0);
        this.mRemoveLink.setOnClickListener(this);
    }

    private void onRootFolderFound(long j) {
        this.mRootFolder = j;
        if (this.mCurrentFolder == -1 || this.mEditingExisting) {
            this.mCurrentFolder = this.mRootFolder;
        }
        setupTopCrumb();
        onCurrentFolderFound();
    }

    private void setupTopCrumb() {
        this.mCrumbs.clear();
        String string = getString(R.string.bookmarks);
        this.mTopLevelLabel = (TextView) this.mCrumbs.pushView(string, false, new Folder(string, this.mRootFolder));
        this.mTopLevelLabel.setCompoundDrawablePadding(6);
    }

    private void onCurrentFolderFound() {
        LoaderManager loaderManager = getLoaderManager();
        if (!this.mSaveToHomeScreen) {
            if (this.mCurrentFolder != -1 && this.mCurrentFolder != this.mRootFolder) {
                this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 1 : 2);
            } else {
                setShowBookmarkIcon(true);
                if (this.mBookmarkExt.shouldSetCustomizedEditFolderSelection(this.mMap, !this.mEditingFolder)) {
                    this.mFolder.setSelectionIgnoringSelectionChange(!this.mEditingFolder ? 1 : 0);
                }
            }
        }
        loaderManager.restartLoader(1, null, this);
    }

    private class SaveBookmarkRunnable implements Runnable {
        private Context mContext;
        private Message mMessage;

        public SaveBookmarkRunnable(Context context, Message message) {
            this.mContext = context.getApplicationContext();
            this.mMessage = message;
        }

        @Override // java.lang.Runnable
        public void run() {
            Bundle data = this.mMessage.getData();
            String string = data.getString("title");
            String string2 = data.getString("url");
            Bitmap bitmap = data.getBoolean("remove_thumbnail") ? null : (Bitmap) data.getParcelable("thumbnail");
            String string3 = data.getString("touch_icon_url");
            try {
                ContentResolver contentResolver = AddBookmarkPage.this.getContentResolver();
                Log.i("Bookmarks", "mCurrentFolder: " + AddBookmarkPage.this.mCurrentFolder);
                Bookmarks.addBookmark(AddBookmarkPage.this, false, string2, string, bitmap, AddBookmarkPage.this.mCurrentFolder);
                if (string3 != null) {
                    new DownloadTouchIcon(this.mContext, contentResolver, string2).execute(AddBookmarkPage.this.mTouchIconUrl);
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

        public UpdateBookmarkTask(Context context, long j) {
            this.mContext = context.getApplicationContext();
            this.mId = Long.valueOf(j);
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Void doInBackground(ContentValues... contentValuesArr) {
            if (contentValuesArr.length < 1) {
                throw new IllegalArgumentException("No ContentValues provided!");
            }
            this.mContext.getContentResolver().update(ContentUris.withAppendedId(BookmarkUtils.getBookmarksUri(this.mContext), this.mId.longValue()), contentValuesArr[0], null, null);
            Log.d("browser/AddBookmarkPage", "UpdateBookmarkTask doInBackground:");
            if (contentValuesArr.length > 1) {
                this.mBookmarkCurrentId = contentValuesArr[1].getAsLong("bookmark_current_id").longValue();
            } else {
                this.mBookmarkCurrentId = -1L;
            }
            return null;
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(Void r7) {
            Log.d("browser/AddBookmarkPage", "UpdateBookmarkTask onPostExecute mBookmarkCurrentId:" + this.mBookmarkCurrentId);
            if (this.mBookmarkCurrentId > 0) {
                this.mContext.getContentResolver().delete(BrowserContract.Bookmarks.CONTENT_URI, "_id = ?", new String[]{String.valueOf(this.mBookmarkCurrentId)});
            }
        }
    }

    private void createHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler() { // from class: com.android.browser.AddBookmarkPage.5
                @Override // android.os.Handler
                public void handleMessage(Message message) {
                    switch (message.what) {
                        case 100:
                            if (1 == message.arg1) {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_saved, 1).show();
                                break;
                            } else {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_not_saved, 1).show();
                                break;
                            }
                        case 101:
                            Bundle data = message.getData();
                            BookmarkUtils.createShortcutToHome(AddBookmarkPage.this, data.getString("url"), data.getString("title"), (Bitmap) data.getParcelable("touch_icon"), (Bitmap) data.getParcelable("favicon"));
                            break;
                        case 102:
                            AddBookmarkPage.this.finish();
                            break;
                    }
                }
            };
        }
    }

    boolean save() throws URISyntaxException {
        Bitmap bitmap;
        Bitmap bitmap2;
        String titleFromId;
        createHandler();
        String strTrim = this.mTitle.getText().toString().trim();
        String strFixUrl = UrlUtils.fixUrl(this.mAddress.getText().toString());
        boolean z = strTrim.length() == 0;
        boolean z2 = strFixUrl.trim().length() == 0;
        Resources resources = getResources();
        if (z || (z2 && !this.mEditingFolder)) {
            if (z) {
                if (this.mEditingFolder) {
                    this.mTitle.setError(resources.getText(R.string.folder_needs_title));
                } else {
                    this.mTitle.setError(resources.getText(R.string.bookmark_needs_title));
                }
            }
            if (z2) {
                this.mAddress.setError(resources.getText(R.string.bookmark_needs_url));
            }
            return false;
        }
        Boolean boolSaveCustomizedEditFolder = this.mBookmarkExt.saveCustomizedEditFolder(getApplicationContext(), strTrim, this.mCurrentFolder, this.mMap, getString(R.string.duplicated_folder_warning));
        if (boolSaveCustomizedEditFolder != null) {
            if (boolSaveCustomizedEditFolder.booleanValue()) {
                setResult(-1);
            }
            return boolSaveCustomizedEditFolder.booleanValue();
        }
        String strTrim2 = strFixUrl.trim();
        if (!this.mEditingFolder) {
            try {
                if (!strTrim2.toLowerCase().startsWith("javascript:")) {
                    String strEncode = URLEncoder.encode(strTrim2);
                    String scheme = new URI(strEncode).getScheme();
                    if (!Bookmarks.urlHasAcceptableScheme(strFixUrl.trim())) {
                        if (scheme != null) {
                            this.mAddress.setError(resources.getText(R.string.bookmark_cannot_save_url));
                            return false;
                        }
                        try {
                            WebAddress webAddress = new WebAddress(strFixUrl);
                            if (webAddress.getHost().length() == 0) {
                                throw new URISyntaxException("", "");
                            }
                            strEncode = webAddress.toString();
                        } catch (ParseException e) {
                            throw new URISyntaxException("", "");
                        }
                    }
                    strTrim2 = URLDecoder.decode(strEncode);
                }
            } catch (IllegalArgumentException e2) {
                this.mAddress.setError(resources.getText(R.string.bookmark_url_not_valid));
                return false;
            } catch (URISyntaxException e3) {
                this.mAddress.setError(resources.getText(R.string.bookmark_url_not_valid));
                return false;
            }
        }
        boolean zEquals = strTrim2.equals(this.mOriginalUrl);
        if (this.mOverwriteBookmarkId > 0) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("bookmark_current_id", Long.valueOf(this.mCurrentId));
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("title", strTrim);
            contentValues2.put("parent", Long.valueOf(this.mCurrentFolder));
            contentValues2.put("url", strTrim2);
            if (!zEquals) {
                contentValues2.putNull("thumbnail");
            }
            if (contentValues2.size() > 0) {
                new UpdateBookmarkTask(getApplicationContext(), this.mOverwriteBookmarkId).execute(contentValues2, contentValues);
            }
            this.mOverwriteBookmarkId = -1L;
            setResult(-1);
            return true;
        }
        this.mOverwriteBookmarkId = haveToOverwriteBookmarkId(strTrim, strTrim2, this.mCurrentFolder);
        if (this.mOverwriteBookmarkId > 0) {
            displayAlertDialogForExistingBookmark();
            return false;
        }
        if (this.mEditingExisting && this.mEditingFolder) {
            Log.d("browser/AddBookmarkPage", "editing folder save");
            long j = this.mMap.getLong("_id", -1L);
            long j2 = this.mMap.getLong("parent", -1L);
            long j3 = this.mCurrentFolder;
            if (j2 == -1) {
                j2 = this.mRootFolder;
            }
            if (j3 == -1) {
                j3 = this.mRootFolder;
            }
            if (j2 == j3 && (titleFromId = getTitleFromId(j)) != null && titleFromId.equals(strTrim)) {
                Log.d("browser/AddBookmarkPage", "edit folder save, does not change anything");
                return true;
            }
        }
        if (this.mEditingFolder && isFolderExist(this.mCurrentFolder, strTrim)) {
            displayToastForExistingFolder();
            return false;
        }
        if (this.mSaveToHomeScreen) {
            this.mEditingExisting = false;
        }
        if (this.mEditingExisting) {
            Long lValueOf = Long.valueOf(this.mMap.getLong("_id"));
            ContentValues contentValues3 = new ContentValues();
            contentValues3.put("title", strTrim);
            contentValues3.put("parent", Long.valueOf(this.mCurrentFolder));
            if (!this.mEditingFolder) {
                contentValues3.put("url", strTrim2);
                if (!zEquals) {
                    contentValues3.putNull("thumbnail");
                }
            }
            if (contentValues3.size() > 0) {
                new UpdateBookmarkTask(getApplicationContext(), lValueOf.longValue()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contentValues3);
            }
            setResult(-1);
        } else {
            if (zEquals) {
                bitmap = (Bitmap) this.mMap.getParcelable("thumbnail");
                bitmap2 = (Bitmap) this.mMap.getParcelable("favicon");
            } else {
                bitmap = null;
                bitmap2 = null;
            }
            Bundle bundle = new Bundle();
            bundle.putString("title", strTrim);
            bundle.putString("url", strTrim2);
            bundle.putParcelable("favicon", bitmap2);
            if (this.mSaveToHomeScreen) {
                if (this.mTouchIconUrl != null && zEquals) {
                    Message messageObtain = Message.obtain(this.mHandler, 101);
                    messageObtain.setData(bundle);
                    new DownloadTouchIcon(this, messageObtain, this.mMap.getString("user_agent")).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.mTouchIconUrl);
                } else {
                    BookmarkUtils.createShortcutToHome(this, strTrim2, strTrim, null, bitmap2);
                }
            } else {
                bundle.putParcelable("thumbnail", bitmap);
                bundle.putBoolean("remove_thumbnail", !zEquals);
                bundle.putString("touch_icon_url", this.mTouchIconUrl);
                Message messageObtain2 = Message.obtain(this.mHandler, 100);
                messageObtain2.setData(bundle);
                new Thread(new SaveBookmarkRunnable(getApplicationContext(), messageObtain2)).start();
            }
            setResult(-1);
            LogTag.logBookmarkAdded(strTrim2, "bookmarkview");
        }
        return true;
    }

    private String getTitleFromId(long j) throws Throwable {
        Cursor cursor = null;
        string = null;
        string = null;
        String string = null;
        try {
            Cursor cursorQuery = getApplicationContext().getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title"}, "_id = ? AND deleted = ? AND folder = ?", new String[]{j + "", "0", "1"}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() != 0) {
                        while (cursorQuery.moveToNext()) {
                            string = cursorQuery.getString(0);
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return string;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (this.mAccountSpinner == adapterView) {
            long j2 = this.mAccountAdapter.getItem(i).rootFolderId;
            if (j2 != this.mRootFolder) {
                this.mCurrentFolder = -1L;
                onRootFolderFound(j2);
                this.mFolderAdapter.clearRecentFolder();
            }
        }
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public static class CustomListView extends ListView {
        private EditText mEditText;

        public void addEditText(EditText editText) {
            this.mEditText = editText;
        }

        public CustomListView(Context context) {
            super(context);
        }

        public CustomListView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public CustomListView(Context context, AttributeSet attributeSet, int i) {
            super(context, attributeSet, i);
        }

        @Override // android.widget.AbsListView, android.view.View
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

        /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        /* JADX WARN: Removed duplicated region for block: B:21:0x006f A[Catch: all -> 0x0134, TRY_LEAVE, TryCatch #2 {all -> 0x0134, blocks: (B:3:0x000d, B:5:0x002d, B:7:0x0033, B:9:0x0039, B:19:0x0068, B:21:0x006f, B:26:0x00c3, B:28:0x00c9, B:29:0x00cf, B:30:0x00d2, B:34:0x010c, B:36:0x0112, B:37:0x0126), top: B:53:0x000d }] */
        /* JADX WARN: Removed duplicated region for block: B:33:0x00e9 A[Catch: all -> 0x0131, TRY_LEAVE, TryCatch #0 {all -> 0x0131, blocks: (B:22:0x008b, B:24:0x0091, B:25:0x00aa, B:31:0x00e3, B:33:0x00e9), top: B:49:0x008b }] */
        /* JADX WARN: Removed duplicated region for block: B:39:0x012a  */
        /* JADX WARN: Removed duplicated region for block: B:41:0x012d  */
        @Override // android.content.AsyncTaskLoader
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public EditBookmarkInfo loadInBackground() throws Throwable {
            Cursor cursorQuery;
            ContentResolver contentResolver = this.mContext.getContentResolver();
            EditBookmarkInfo editBookmarkInfo = new EditBookmarkInfo();
            Cursor cursorQuery2 = null;
            try {
                String string = this.mMap.getString("url");
                editBookmarkInfo.id = this.mMap.getLong("_id", -1L);
                if (this.mMap.getBoolean("check_for_dupe") && editBookmarkInfo.id == -1 && !TextUtils.isEmpty(string)) {
                    Cursor cursorQuery3 = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "url=?", new String[]{string}, null);
                    try {
                        if (cursorQuery3.getCount() == 1 && cursorQuery3.moveToFirst()) {
                            editBookmarkInfo.id = cursorQuery3.getLong(0);
                        }
                        cursorQuery3.close();
                        if (editBookmarkInfo.id != -1) {
                        }
                        cursorQuery = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"parent"}, null, null, "modified DESC LIMIT 1");
                        if (!cursorQuery.moveToFirst()) {
                        }
                        if (cursorQuery2 != null) {
                        }
                        return editBookmarkInfo;
                    } catch (Throwable th) {
                        th = th;
                        cursorQuery2 = cursorQuery3;
                    }
                } else {
                    if (editBookmarkInfo.id != -1) {
                        cursorQuery = contentResolver.query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, editBookmarkInfo.id), new String[]{"parent", "account_name", "account_type", "title"}, null, null, null);
                        try {
                            if (cursorQuery.moveToFirst()) {
                                editBookmarkInfo.parentId = cursorQuery.getLong(0);
                                editBookmarkInfo.accountName = cursorQuery.getString(1);
                                editBookmarkInfo.accountType = cursorQuery.getString(2);
                                editBookmarkInfo.title = cursorQuery.getString(3);
                            }
                            cursorQuery.close();
                            Cursor cursorQuery4 = contentResolver.query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, editBookmarkInfo.parentId), new String[]{"title"}, null, null, null);
                            if (cursorQuery4.moveToFirst()) {
                                editBookmarkInfo.parentTitle = cursorQuery4.getString(0);
                            }
                            cursorQuery4.close();
                        } catch (Throwable th2) {
                            th = th2;
                            cursorQuery2 = cursorQuery;
                        }
                    }
                    cursorQuery = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"parent"}, null, null, "modified DESC LIMIT 1");
                    if (!cursorQuery.moveToFirst()) {
                        long j = cursorQuery.getLong(0);
                        cursorQuery.close();
                        cursorQuery2 = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title", "account_name", "account_type"}, "_id=?", new String[]{Long.toString(j)}, null);
                        if (cursorQuery2.moveToFirst()) {
                            editBookmarkInfo.lastUsedId = j;
                            editBookmarkInfo.lastUsedTitle = cursorQuery2.getString(0);
                            editBookmarkInfo.lastUsedAccountName = cursorQuery2.getString(1);
                            editBookmarkInfo.lastUsedAccountType = cursorQuery2.getString(2);
                        }
                        cursorQuery2.close();
                    } else {
                        cursorQuery2 = cursorQuery;
                    }
                    if (cursorQuery2 != null) {
                        cursorQuery2.close();
                    }
                    return editBookmarkInfo;
                }
            } catch (Throwable th3) {
                th = th3;
            }
            if (cursorQuery2 != null) {
                cursorQuery2.close();
            }
            throw th;
        }

        @Override // android.content.Loader
        protected void onStartLoading() {
            forceLoad();
        }
    }
}
