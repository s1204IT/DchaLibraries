package com.mediatek.calendarimporter;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import com.android.common.speech.LoggingEvents;
import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.vcalendar.VCalComposer;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ICalendarProvider extends ContentProvider {
    private static final String DEFAULT_FILE_NAME = "vCalendar";
    private static final String FILENAME_REG_EXP = "[/\\\\:*?\"<>|$()~]";
    private static final String TAG = "ICalendarProvider";
    private static final String VCS = ".vcs";
    private static final int VCS_FILENAME_MAX_LENGTH = 100;
    private VCalComposer mComposer;

    @Override
    public String getType(Uri uri) {
        LogUtils.d(TAG, "getType() " + uri.toString());
        return "text/x-vcalendar";
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String strReplaceAll;
        LogUtils.i(TAG, "query()--->> uri=" + uri.toString());
        if (strArr == null) {
            strArr = new String[]{"_display_name", "_size"};
            LogUtils.v(TAG, "query projection is null, create one.");
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        createComposer(uri);
        AssetFileDescriptor accountsMemoryFile = this.mComposer.getAccountsMemoryFile();
        String memoryFileName = this.mComposer.getMemoryFileName();
        if (accountsMemoryFile == null) {
            return matrixCursor;
        }
        if (memoryFileName != null) {
            strReplaceAll = memoryFileName.replaceAll(FILENAME_REG_EXP, LoggingEvents.EXTRA_CALLING_APP_NAME);
            if (strReplaceAll.getBytes().length > VCS_FILENAME_MAX_LENGTH) {
                strReplaceAll = new String(strReplaceAll.getBytes(), 0, VCS_FILENAME_MAX_LENGTH).substring(0, r1.length() - 1);
                LogUtils.d(TAG, "fileName is too long, format it, fileName=" + strReplaceAll);
            }
            if (strReplaceAll.length() <= 0) {
                strReplaceAll = DEFAULT_FILE_NAME;
            }
        }
        LogUtils.i(TAG, "query, title = " + strReplaceAll);
        int length = strArr.length;
        Object[] objArr = new Object[length];
        for (int i = 0; i < length; i++) {
            if (strArr[i].equals("_display_name")) {
                objArr[i] = strReplaceAll + VCS;
            } else if (strArr[i].equals("_size")) {
                objArr[i] = Long.valueOf(accountsMemoryFile.getLength());
            } else {
                objArr[i] = null;
                LogUtils.e(TAG, "can not support column:" + strArr[i]);
            }
        }
        matrixCursor.addRow(objArr);
        try {
            accountsMemoryFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtils.i(TAG, "query(): return the Cursor.count = " + matrixCursor.getCount());
        return matrixCursor;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String str) throws FileNotFoundException {
        AssetFileDescriptor accountsMemoryFile;
        LogUtils.i(TAG, "openAssetFile()--->> uri=" + uri.toString());
        createComposer(uri);
        try {
            accountsMemoryFile = this.mComposer.getAccountsMemoryFile();
        } catch (IllegalArgumentException e) {
            LogUtils.e(TAG, "openAssetFile, getAccountsMemoryFile trrow IllegalArgumentException.");
            accountsMemoryFile = null;
        }
        if (accountsMemoryFile == null) {
            LogUtils.e(TAG, "openAssetFile,trrow FileNotFoundException.");
            throw new FileNotFoundException();
        }
        LogUtils.i(TAG, "openAssetFile(): return the fileName=" + this.mComposer.getMemoryFileName() + "fileLength = " + accountsMemoryFile.getLength());
        return accountsMemoryFile;
    }

    private void createComposer(Uri uri) {
        long id = ContentUris.parseId(uri);
        if (id < 0) {
            LogUtils.e(TAG, "Constructor,The given eventId is inlegal or empty, eventId :" + id);
            throw new IllegalArgumentException(uri.toString());
        }
        String str = "_id=" + String.valueOf(id) + " AND deleted!=1";
        LogUtils.i(TAG, "Constructor: the going query selection = \"" + str + "\"");
        this.mComposer = new VCalComposer(getContext(), str, null);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }
}
