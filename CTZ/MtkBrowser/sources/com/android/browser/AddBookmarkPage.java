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
    private DialogInterface.OnClickListener mAlertDlgOk = new DialogInterface.OnClickListener(this) {
        final AddBookmarkPage this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (this.this$0.save()) {
                this.this$0.finish();
            }
        }
    };
    private LoaderManager.LoaderCallbacks<EditBookmarkInfo> mEditInfoLoaderCallbacks = new LoaderManager.LoaderCallbacks<EditBookmarkInfo>(this) {
        final AddBookmarkPage this$0;

        {
            this.this$0 = this;
        }

        @Override
        public Loader<EditBookmarkInfo> onCreateLoader(int i, Bundle bundle) {
            return new EditBookmarkInfoLoader(this.this$0, this.this$0.mMap);
        }

        @Override
        public void onLoadFinished(Loader<EditBookmarkInfo> loader, EditBookmarkInfo editBookmarkInfo) {
            boolean z;
            if (editBookmarkInfo.id != -1) {
                this.this$0.mEditingExisting = true;
                this.this$0.showRemoveButton();
                if (this.this$0.mEditingFolder) {
                    this.this$0.mFakeTitle.setText(2131492992);
                } else {
                    this.this$0.mFakeTitle.setText(2131493003);
                }
                this.this$0.mFolderAdapter.setOtherFolderDisplayText(editBookmarkInfo.parentTitle);
                this.this$0.mMap.putLong("_id", editBookmarkInfo.id);
                this.this$0.setAccount(editBookmarkInfo.accountName, editBookmarkInfo.accountType);
                this.this$0.mCurrentFolder = editBookmarkInfo.parentId;
                this.this$0.onCurrentFolderFound();
                if (this.this$0.mRestoreFolder >= 0) {
                    this.this$0.mFolder.setSelectionIgnoringSelectionChange(this.this$0.mRestoreFolder);
                    this.this$0.mRestoreFolder = -2;
                }
                z = true;
            } else {
                z = false;
            }
            if (editBookmarkInfo.lastUsedId != -1 && editBookmarkInfo.lastUsedId != editBookmarkInfo.id && !this.this$0.mEditingFolder) {
                if (z && editBookmarkInfo.lastUsedId != this.this$0.mRootFolder && TextUtils.equals(editBookmarkInfo.lastUsedAccountName, editBookmarkInfo.accountName) && TextUtils.equals(editBookmarkInfo.lastUsedAccountType, editBookmarkInfo.accountType)) {
                    this.this$0.mFolderAdapter.addRecentFolder(editBookmarkInfo.lastUsedId, editBookmarkInfo.lastUsedTitle);
                } else if (!z) {
                    this.this$0.setAccount(editBookmarkInfo.lastUsedAccountName, editBookmarkInfo.lastUsedAccountType);
                    if (editBookmarkInfo.lastUsedId != this.this$0.mRootFolder) {
                        this.this$0.mFolderAdapter.addRecentFolder(editBookmarkInfo.lastUsedId, editBookmarkInfo.lastUsedTitle);
                    }
                    if (this.this$0.mRestoreFolder >= 0) {
                        this.this$0.mFolder.setSelectionIgnoringSelectionChange(this.this$0.mRestoreFolder);
                        this.this$0.mRestoreFolder = -2;
                    }
                    z = true;
                }
            }
            if (z) {
                return;
            }
            this.this$0.mAccountSpinner.setSelection(0);
        }

        @Override
        public void onLoaderReset(Loader<EditBookmarkInfo> loader) {
        }
    };

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
                this.mLabel = context.getString(2131493285);
            }
        }

        public String toString() {
            return this.mLabel;
        }
    }

    public static class CustomListView extends ListView {
        private EditText mEditText;

        public CustomListView(Context context) {
            super(context);
        }

        public CustomListView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public CustomListView(Context context, AttributeSet attributeSet, int i) {
            super(context, attributeSet, i);
        }

        public void addEditText(EditText editText) {
            this.mEditText = editText;
        }

        @Override
        public boolean checkInputConnectionProxy(View view) {
            return view == this.mEditText;
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
        public EditBookmarkInfo loadInBackground() throws Throwable {
            Cursor cursorQuery;
            Cursor cursorQuery2 = null;
            ContentResolver contentResolver = this.mContext.getContentResolver();
            EditBookmarkInfo editBookmarkInfo = new EditBookmarkInfo();
            try {
                String string = this.mMap.getString("url");
                editBookmarkInfo.id = this.mMap.getLong("_id", -1L);
                if (!this.mMap.getBoolean("check_for_dupe") || editBookmarkInfo.id != -1 || TextUtils.isEmpty(string)) {
                    if (editBookmarkInfo.id != -1) {
                        Cursor cursorQuery3 = contentResolver.query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, editBookmarkInfo.id), new String[]{"parent", "account_name", "account_type", "title"}, null, null, null);
                        try {
                            if (cursorQuery3.moveToFirst()) {
                                editBookmarkInfo.parentId = cursorQuery3.getLong(0);
                                editBookmarkInfo.accountName = cursorQuery3.getString(1);
                                editBookmarkInfo.accountType = cursorQuery3.getString(2);
                                editBookmarkInfo.title = cursorQuery3.getString(3);
                            }
                            cursorQuery3.close();
                            Cursor cursorQuery4 = contentResolver.query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, editBookmarkInfo.parentId), new String[]{"title"}, null, null, null);
                            if (cursorQuery4.moveToFirst()) {
                                editBookmarkInfo.parentTitle = cursorQuery4.getString(0);
                            }
                            cursorQuery4.close();
                        } catch (Throwable th) {
                            th = th;
                            cursorQuery = cursorQuery3;
                        }
                    }
                    cursorQuery2 = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"parent"}, null, null, "modified DESC LIMIT 1");
                    if (cursorQuery2.moveToFirst()) {
                        long j = cursorQuery2.getLong(0);
                        cursorQuery2.close();
                        cursorQuery2 = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title", "account_name", "account_type"}, "_id=?", new String[]{Long.toString(j)}, null);
                        if (cursorQuery2.moveToFirst()) {
                            editBookmarkInfo.lastUsedId = j;
                            editBookmarkInfo.lastUsedTitle = cursorQuery2.getString(0);
                            editBookmarkInfo.lastUsedAccountName = cursorQuery2.getString(1);
                            editBookmarkInfo.lastUsedAccountType = cursorQuery2.getString(2);
                        }
                        cursorQuery2.close();
                    }
                    if (cursorQuery2 != null) {
                        cursorQuery2.close();
                    }
                    return editBookmarkInfo;
                }
                cursorQuery = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "url=?", new String[]{string}, null);
                try {
                    if (cursorQuery.getCount() == 1 && cursorQuery.moveToFirst()) {
                        editBookmarkInfo.id = cursorQuery.getLong(0);
                    }
                    cursorQuery.close();
                    if (editBookmarkInfo.id != -1) {
                    }
                    cursorQuery2 = contentResolver.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"parent"}, null, null, "modified DESC LIMIT 1");
                    if (cursorQuery2.moveToFirst()) {
                    }
                    if (cursorQuery2 != null) {
                    }
                    return editBookmarkInfo;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
                cursorQuery = cursorQuery2;
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            throw th;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }

    private static class Folder {
        long Id;
        String Name;

        Folder(String str, long j) {
            this.Name = str;
            this.Id = j;
        }
    }

    private class FolderAdapter extends CursorAdapter {
        final AddBookmarkPage this$0;

        public FolderAdapter(AddBookmarkPage addBookmarkPage, Context context) {
            super(context, null);
            this.this$0 = addBookmarkPage;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty() && !this.this$0.mIsFolderNamerShowing;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(2130968599, (ViewGroup) null);
        }
    }

    private class SaveBookmarkRunnable implements Runnable {
        private Context mContext;
        private Message mMessage;
        final AddBookmarkPage this$0;

        public SaveBookmarkRunnable(AddBookmarkPage addBookmarkPage, Context context, Message message) {
            this.this$0 = addBookmarkPage;
            this.mContext = context.getApplicationContext();
            this.mMessage = message;
        }

        @Override
        public void run() {
            Bundle data = this.mMessage.getData();
            String string = data.getString("title");
            String string2 = data.getString("url");
            Bitmap bitmap = data.getBoolean("remove_thumbnail") ? null : (Bitmap) data.getParcelable("thumbnail");
            String string3 = data.getString("touch_icon_url");
            try {
                ContentResolver contentResolver = this.this$0.getContentResolver();
                Log.i("Bookmarks", "mCurrentFolder: " + this.this$0.mCurrentFolder);
                Bookmarks.addBookmark(this.this$0, false, string2, string, bitmap, this.this$0.mCurrentFolder);
                if (string3 != null) {
                    new DownloadTouchIcon(this.mContext, contentResolver, string2).execute(this.this$0.mTouchIconUrl);
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

        @Override
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

        @Override
        protected void onPostExecute(Void r9) {
            Log.d("browser/AddBookmarkPage", "UpdateBookmarkTask onPostExecute mBookmarkCurrentId:" + this.mBookmarkCurrentId);
            if (this.mBookmarkCurrentId > 0) {
                this.mContext.getContentResolver().delete(BrowserContract.Bookmarks.CONTENT_URI, "_id = ?", new String[]{String.valueOf(this.mBookmarkCurrentId)});
            }
        }
    }

    private long addFolderToCurrent(String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", str);
        contentValues.put("folder", (Integer) 1);
        Object topData = this.mCrumbs != null ? this.mCrumbs.getTopData() : null;
        contentValues.put("parent", Long.valueOf(topData != null ? ((Folder) topData).Id : this.mRootFolder));
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
        return uriInsert != null ? ContentUris.parseId(uriInsert) : getIdFromName(context, str);
    }

    private void completeOrCancelFolderNaming(boolean z) {
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
        if (!z && !TextUtils.isEmpty(this.mFolderNamer.getText()) && !TextUtils.isEmpty(this.mFolderNamer.getText().toString().trim())) {
            descendInto(this.mFolderNamer.getText().toString(), addFolderToCurrent(this.mFolderNamer.getText().toString()));
        }
        setShowFolderNamer(false);
        this.mBookmarkExt.showCustomizedEditFolderNewFolderView(this.mAddNewFolder, this.mAddSeparator, this.mMap);
    }

    private void createHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(this) {
                final AddBookmarkPage this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void handleMessage(Message message) {
                    switch (message.what) {
                        case 100:
                            if (1 != message.arg1) {
                                Toast.makeText(this.this$0, 2131493011, 1).show();
                            } else {
                                Toast.makeText(this.this$0, 2131493010, 1).show();
                            }
                            break;
                        case 101:
                            Bundle data = message.getData();
                            BookmarkUtils.createShortcutToHome(this.this$0, data.getString("url"), data.getString("title"), (Bitmap) data.getParcelable("touch_icon"), (Bitmap) data.getParcelable("favicon"));
                            break;
                        case 102:
                            this.this$0.finish();
                            break;
                    }
                }
            };
        }
    }

    private void descendInto(String str, long j) {
        if (j == -1) {
            Toast.makeText(getApplicationContext(), 2131492888, 1).show();
        } else {
            this.mCrumbs.pushView(str, new Folder(str, j));
            this.mCrumbs.notifyController();
        }
    }

    private void displayAlertDialogForExistingBookmark() {
        new AlertDialog.Builder(this).setTitle(2131492886).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getText(2131492887).toString()).setPositiveButton(2131492964, this.mAlertDlgOk).setNegativeButton(2131492963, (DialogInterface.OnClickListener) null).show();
    }

    private void displayToastForExistingFolder() {
        Toast.makeText(getApplicationContext(), 2131492888, 1).show();
    }

    private InputFilter[] generateInputFilter(int i) {
        return new InputFilter[]{new InputFilter.LengthFilter(this, i, i) {
            final AddBookmarkPage this$0;
            final int val$nLimit;

            {
                this.this$0 = this;
                this.val$nLimit = i;
            }

            @Override
            public CharSequence filter(CharSequence charSequence, int i2, int i3, Spanned spanned, int i4, int i5) {
                int length = this.val$nLimit - (spanned.length() - (i5 - i4));
                if (length <= 0) {
                    this.this$0.showWarningDialog();
                    return "";
                }
                if (length >= i3 - i2) {
                    return null;
                }
                if (length < charSequence.length()) {
                    this.this$0.showWarningDialog();
                }
                return charSequence.subSequence(i2, length + i2);
            }
        }};
    }

    private static long getIdFromName(Context context, String str) throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = context.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"_id"}, "title = ? AND deleted = ? AND folder = ? AND parent = ?", new String[]{str, "0", "1", "1"}, null);
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
                    if (cursorQuery != null) {
                        cursorQuery.close();
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
            cursorQuery = null;
        }
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService("input_method");
    }

    private String getNameFromId(long j) throws Throwable {
        Cursor cursorQuery;
        String string;
        try {
            cursorQuery = getApplicationContext().getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title"}, "_id = ? AND deleted = ? AND folder = ? ", new String[]{String.valueOf(j), "0", "1"}, null);
            if (cursorQuery != null) {
                try {
                    string = cursorQuery.moveToNext() ? cursorQuery.getString(0) : "";
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
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
            cursorQuery = null;
        }
    }

    private String getTitleFromId(long j) throws Throwable {
        Cursor cursorQuery;
        String string = null;
        Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
        try {
            cursorQuery = getApplicationContext().getContentResolver().query(uri, new String[]{"title"}, "_id = ? AND deleted = ? AND folder = ?", new String[]{j + "", "0", "1"}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() != 0) {
                        while (cursorQuery.moveToNext()) {
                            string = cursorQuery.getString(0);
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
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
            cursorQuery = null;
        }
    }

    private Uri getUriForFolder(long j) {
        BookmarkAccount bookmarkAccount = (BookmarkAccount) this.mAccountSpinner.getSelectedItem();
        return (j != this.mRootFolder || bookmarkAccount == null) ? BrowserContract.Bookmarks.buildFolderUri(j) : BookmarksLoader.addAccount(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, bookmarkAccount.accountType, bookmarkAccount.accountName);
    }

    private int haveToOverwriteBookmarkId(String str, String str2, long j) {
        if (this.mSaveToHomeScreen || this.mEditingFolder) {
            return -1;
        }
        Log.d("browser/AddBookmarkPage", "Add bookmark page haveToOverwriteBookmarkId mCurrentId:" + this.mCurrentId);
        return Bookmarks.getIdByNameOrUrl(getContentResolver(), str, str2, j, this.mCurrentId);
    }

    private void hideSoftInput() {
        Log.d("browser/AddBookmarkPage", "hideSoftInput");
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    private boolean isFolderExist(long j, String str) throws Throwable {
        Cursor cursorQuery;
        boolean z;
        Log.e("browser/AddBookmarkPage", "BrowserProvider2.isValidAccountName parentId:" + j + " title:" + str);
        if (j <= 0 || str == null || str.length() == 0) {
            return false;
        }
        Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
        try {
            cursorQuery = getApplicationContext().getContentResolver().query(uri, new String[]{"_id"}, "parent = ? AND deleted = ? AND folder = ? AND title = ?", new String[]{j + "", "0", "1", str}, null);
            if (cursorQuery != null) {
                try {
                    z = cursorQuery.getCount() != 0;
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery == null) {
                return z;
            }
            cursorQuery.close();
            return z;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private void onCurrentFolderFound() {
        LoaderManager loaderManager = getLoaderManager();
        if (!this.mSaveToHomeScreen) {
            if (this.mCurrentFolder == -1 || this.mCurrentFolder == this.mRootFolder) {
                setShowBookmarkIcon(true);
                if (this.mBookmarkExt.shouldSetCustomizedEditFolderSelection(this.mMap, !this.mEditingFolder)) {
                    this.mFolder.setSelectionIgnoringSelectionChange(!this.mEditingFolder ? 1 : 0);
                }
            } else {
                this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 1 : 2);
            }
        }
        loaderManager.restartLoader(1, null, this);
    }

    private void onRootFolderFound(long j) {
        this.mRootFolder = j;
        if (this.mCurrentFolder == -1 || this.mEditingExisting) {
            this.mCurrentFolder = this.mRootFolder;
        }
        setupTopCrumb();
        onCurrentFolderFound();
    }

    private void setShowBookmarkIcon(boolean z) {
        this.mTopLevelLabel.setCompoundDrawablesWithIntrinsicBounds(z ? this.mHeaderIcon : null, (Drawable) null, (Drawable) null, (Drawable) null);
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

    private void setupTopCrumb() {
        this.mCrumbs.clear();
        String string = getString(2131493026);
        this.mTopLevelLabel = (TextView) this.mCrumbs.pushView(string, false, new Folder(string, this.mRootFolder));
        this.mTopLevelLabel.setCompoundDrawablePadding(6);
    }

    private void showRemoveButton() {
        findViewById(2131558450).setVisibility(0);
        this.mRemoveLink = findViewById(2131558451);
        this.mRemoveLink.setVisibility(0);
        this.mRemoveLink.setOnClickListener(this);
    }

    private void showWarningDialog() {
        if (this.mWarningDialog == null || this.mWarningDialog.isShowing()) {
            return;
        }
        this.mWarningDialog.setTitle(2131492913);
        this.mWarningDialog.setMessage(getString(2131492912));
        this.mWarningDialog.setButton(getString(2131492914), new DialogInterface.OnClickListener(this) {
            final AddBookmarkPage this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        this.mWarningDialog.show();
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
                    this.mFolder.setSelectionIgnoringSelectionChange(!this.mEditingFolder ? 1 : 0);
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
            this.mFolder.setSelectionIgnoringSelectionChange(!this.mEditingFolder ? 1 : 0);
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

    private void switchToFolderSelector() {
        this.mListView.setSelection(0);
        this.mDefaultView.setVisibility(8);
        this.mFolderSelector.setVisibility(0);
        this.mCrumbHolder.setVisibility(0);
        this.mFakeTitleHolder.setVisibility(8);
        this.mBookmarkExt.showCustomizedEditFolderNewFolderView(this.mAddNewFolder, this.mAddSeparator, this.mMap);
        getInputMethodManager().hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    @Override
    public void onClick(View view) {
        if (view == this.mButton) {
            if (this.mFolderSelector.getVisibility() != 0) {
                this.mOverwriteBookmarkId = -1L;
                if (save()) {
                    finish();
                    return;
                }
                return;
            }
            if (this.mIsFolderNamerShowing) {
                completeOrCancelFolderNaming(false);
                return;
            } else {
                this.mSaveToHomeScreen = false;
                switchToDefaultView(true);
                return;
            }
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
            this.mFolderNamer.setText(2131492991);
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

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        String str;
        String string;
        super.onCreate(bundle);
        requestWindowFeature(1);
        this.mMap = getIntent().getExtras();
        setContentView(2130968594);
        Window window = getWindow();
        if (BrowserActivity.isTablet(this)) {
            window.setSoftInputMode(16);
        }
        this.mBookmarkExt = Extensions.getBookmarkPlugin(getApplicationContext());
        this.mFakeTitle = (TextView) findViewById(2131558449);
        if (this.mMap != null) {
            Bundle bundle2 = this.mMap.getBundle("bookmark");
            if (bundle2 != null) {
                this.mEditingFolder = this.mMap.getBoolean("is_folder", false);
                this.mMap = bundle2;
                this.mEditingExisting = this.mBookmarkExt.customizeEditExistingFolderState(this.mMap, true);
                this.mCurrentId = this.mMap.getLong("_id", -1L);
                Log.d("browser/AddBookmarkPage", "Add bookmark page onCreate mCurrentId:" + this.mCurrentId);
                if (this.mEditingFolder) {
                    this.mFakeTitle.setText(this.mBookmarkExt.getCustomizedEditFolderFakeTitleString(this.mMap, getString(2131492992)));
                    findViewById(2131558454).setVisibility(8);
                } else {
                    this.mFakeTitle.setText(2131493003);
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
            String string2 = this.mMap.getString("title");
            string = this.mMap.getString("url");
            this.mOriginalUrl = string;
            this.mTouchIconUrl = this.mMap.getString("touch_icon_url");
            this.mCurrentFolder = this.mMap.getLong("parent", -1L);
            Log.i("Bookmarks", "CurrentFolderId: " + this.mCurrentFolder);
            str = string2;
        } else {
            str = null;
            string = null;
        }
        this.mWarningDialog = new AlertDialog.Builder(this).create();
        this.mTitle = (EditText) findViewById(2131558407);
        this.mTitle.setFilters(generateInputFilter(getResources().getInteger(2131623945)));
        this.mTitle.setText(str);
        if (str != null) {
            this.mTitle.setSelection(this.mTitle.getText().length());
        }
        this.mAddress = (EditText) findViewById(2131558456);
        Context applicationContext = getApplicationContext();
        InputFilter[] inputFilterArrCheckUrlLengthLimit = Extensions.getUrlPlugin(applicationContext).checkUrlLengthLimit(applicationContext);
        if (inputFilterArrCheckUrlLengthLimit != null) {
            this.mAddress.setFilters(inputFilterArrCheckUrlLengthLimit);
        }
        this.mAddress.setText(string);
        this.mButton = (TextView) findViewById(2131558463);
        this.mButton.setOnClickListener(this);
        this.mCancelButton = findViewById(2131558462);
        this.mCancelButton.setOnClickListener(this);
        this.mFolder = (FolderSpinner) findViewById(2131558458);
        this.mFolderAdapter = new FolderSpinnerAdapter(this, !this.mEditingFolder);
        this.mFolder.setAdapter((SpinnerAdapter) this.mFolderAdapter);
        this.mFolder.setOnSetSelectionListener(this);
        if (this.mCurrentFolder == -1 || this.mCurrentFolder == 1) {
            this.mFolder.setSelectionIgnoringSelectionChange(!this.mEditingFolder ? 1 : 0);
        } else {
            this.mFolder.setSelectionIgnoringSelectionChange(this.mEditingFolder ? 1 : 2);
            this.mFolderAdapter.setOtherFolderDisplayText(getNameFromId(this.mCurrentFolder));
        }
        this.mDefaultView = findViewById(2131558452);
        this.mFolderSelector = findViewById(2131558459);
        this.mFolderNamerHolder = getLayoutInflater().inflate(2130968611, (ViewGroup) null);
        this.mFolderNamer = (EditText) this.mFolderNamerHolder.findViewById(2131558498);
        this.mFolderNamer.setFilters(generateInputFilter(getResources().getInteger(2131623945)));
        this.mFolderNamer.setOnEditorActionListener(this);
        this.mFolderCancel = this.mFolderNamerHolder.findViewById(2131558499);
        this.mFolderCancel.setOnClickListener(this);
        this.mAddNewFolder = findViewById(2131558447);
        this.mAddNewFolder.setOnClickListener(this);
        this.mAddSeparator = findViewById(2131558446);
        this.mBookmarkExt.showCustomizedEditFolderNewFolderView(this.mAddNewFolder, this.mAddSeparator, this.mMap);
        this.mCrumbs = (BreadCrumbView) findViewById(2131558436);
        this.mCrumbs.setUseBackButton(true);
        this.mCrumbs.setController(this);
        this.mHeaderIcon = getResources().getDrawable(2130837551);
        this.mCrumbHolder = findViewById(2131558422);
        this.mCrumbs.setMaxVisible(2);
        this.mAdapter = new FolderAdapter(this, this);
        this.mListView = (CustomListView) findViewById(2131558460);
        this.mListView.setEmptyView(findViewById(2131558461));
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mListView.addEditText(this.mFolderNamer);
        this.mAccountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        this.mAccountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mAccountSpinner = (Spinner) findViewById(2131558457);
        this.mAccountSpinner.setAdapter((SpinnerAdapter) this.mAccountAdapter);
        this.mAccountSpinner.setOnItemSelectedListener(this);
        this.mFakeTitleHolder = findViewById(2131558448);
        if (!window.getDecorView().isInTouchMode()) {
            this.mButton.requestFocus();
        }
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case 0:
                return new AccountsLoader(this);
            case 1:
                String str = "folder != 0";
                String[] strArr = null;
                if (this.mEditingFolder) {
                    str = "folder != 0 AND _id != ?";
                    strArr = new String[]{Long.toString(this.mMap.getLong("_id"))};
                }
                Object topData = this.mCrumbs.getTopData();
                return new CursorLoader(this, getUriForFolder(topData != null ? ((Folder) topData).Id : this.mRootFolder), new String[]{"_id", "title", "folder"}, str, strArr, "_id ASC");
            default:
                throw new AssertionError("Asking for nonexistant loader!");
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (textView != this.mFolderNamer) {
            return false;
        }
        if (textView.getText().length() > 0 && (i == 6 || i == 0)) {
            hideSoftInput();
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        descendInto(((TextView) view.findViewById(android.R.id.text1)).getText().toString(), j);
    }

    @Override
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
        if (loader.getId() != 1) {
            return;
        }
        this.mAdapter.changeCursor(null);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mRestoreFolder = this.mFolder.getSelectedItemPosition();
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        Resources resources = getResources();
        if (bundle != null && bundle.getBoolean("titleHasError") && this.mTitle != null && this.mTitle.getText().toString().trim().length() == 0) {
            if (this.mEditingFolder) {
                this.mTitle.setError(resources.getText(2131492925));
            } else {
                this.mTitle.setError(resources.getText(2131493013));
            }
        }
        if (bundle == null || !bundle.getBoolean("addrHasError") || this.mAddress == null || this.mAddress.getText().toString().trim().length() != 0) {
            return;
        }
        this.mAddress.setError(resources.getText(2131493014));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mTitle != null && this.mTitle.getError() != null) {
            bundle.putBoolean("titleHasError", true);
        }
        if (this.mAddress == null || this.mAddress.getError() == null) {
            return;
        }
        bundle.putBoolean("addrHasError", true);
    }

    @Override
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

    @Override
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

    boolean save() {
        String str;
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
                    this.mTitle.setError(resources.getText(2131492925));
                } else {
                    this.mTitle.setError(resources.getText(2131493013));
                }
            }
            if (z2) {
                this.mAddress.setError(resources.getText(2131493014));
            }
            return false;
        }
        Boolean boolSaveCustomizedEditFolder = this.mBookmarkExt.saveCustomizedEditFolder(getApplicationContext(), strTrim, this.mCurrentFolder, this.mMap, getString(2131492888));
        if (boolSaveCustomizedEditFolder != null) {
            if (boolSaveCustomizedEditFolder.booleanValue()) {
                setResult(-1);
            }
            return boolSaveCustomizedEditFolder.booleanValue();
        }
        String strTrim2 = strFixUrl.trim();
        if (this.mEditingFolder) {
            str = strTrim2;
        } else {
            try {
                if (!strTrim2.toLowerCase().startsWith("javascript:")) {
                    String strEncode = URLEncoder.encode(strTrim2);
                    String scheme = new URI(strEncode).getScheme();
                    if (!Bookmarks.urlHasAcceptableScheme(strFixUrl.trim())) {
                        if (scheme != null) {
                            this.mAddress.setError(resources.getText(2131493016));
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
                str = strTrim2;
            } catch (IllegalArgumentException e2) {
                this.mAddress.setError(resources.getText(2131493015));
                return false;
            } catch (URISyntaxException e3) {
                this.mAddress.setError(resources.getText(2131493015));
                return false;
            }
        }
        boolean zEquals = str.equals(this.mOriginalUrl);
        if (this.mOverwriteBookmarkId > 0) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("bookmark_current_id", Long.valueOf(this.mCurrentId));
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("title", strTrim);
            contentValues2.put("parent", Long.valueOf(this.mCurrentFolder));
            contentValues2.put("url", str);
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
        this.mOverwriteBookmarkId = haveToOverwriteBookmarkId(strTrim, str, this.mCurrentFolder);
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
            long j4 = this.mMap.getLong("_id");
            ContentValues contentValues3 = new ContentValues();
            contentValues3.put("title", strTrim);
            contentValues3.put("parent", Long.valueOf(this.mCurrentFolder));
            if (!this.mEditingFolder) {
                contentValues3.put("url", str);
                if (!zEquals) {
                    contentValues3.putNull("thumbnail");
                }
            }
            if (contentValues3.size() > 0) {
                new UpdateBookmarkTask(getApplicationContext(), Long.valueOf(j4).longValue()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contentValues3);
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
            bundle.putString("url", str);
            bundle.putParcelable("favicon", bitmap2);
            if (!this.mSaveToHomeScreen) {
                bundle.putParcelable("thumbnail", bitmap);
                bundle.putBoolean("remove_thumbnail", !zEquals);
                bundle.putString("touch_icon_url", this.mTouchIconUrl);
                Message messageObtain = Message.obtain(this.mHandler, 100);
                messageObtain.setData(bundle);
                new Thread(new SaveBookmarkRunnable(this, getApplicationContext(), messageObtain)).start();
            } else if (this.mTouchIconUrl == null || !zEquals) {
                BookmarkUtils.createShortcutToHome(this, str, strTrim, null, bitmap2);
            } else {
                Message messageObtain2 = Message.obtain(this.mHandler, 101);
                messageObtain2.setData(bundle);
                new DownloadTouchIcon(this, messageObtain2, this.mMap.getString("user_agent")).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.mTouchIconUrl);
            }
            setResult(-1);
            LogTag.logBookmarkAdded(str, "bookmarkview");
        }
        return true;
    }

    void setAccount(String str, String str2) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.mAccountAdapter.getCount()) {
                return;
            }
            BookmarkAccount item = this.mAccountAdapter.getItem(i2);
            if (TextUtils.equals(item.accountName, str) && TextUtils.equals(item.accountType, str2)) {
                this.mAccountSpinner.setSelection(i2);
                onRootFolderFound(item.rootFolderId);
                return;
            }
            i = i2 + 1;
        }
    }
}
