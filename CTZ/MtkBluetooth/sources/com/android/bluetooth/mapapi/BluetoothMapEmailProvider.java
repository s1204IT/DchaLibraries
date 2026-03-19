package com.android.bluetooth.mapapi;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ComponentInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class BluetoothMapEmailProvider extends ContentProvider {
    private static final boolean D = true;
    private static final int MATCH_ACCOUNT = 1;
    private static final int MATCH_FOLDER = 3;
    private static final int MATCH_MESSAGE = 2;
    private static final String TAG = "BluetoothMapEmailProvider";
    private String mAuthority;
    private UriMatcher mMatcher;
    protected ContentResolver mResolver;
    private Uri CONTENT_URI = null;
    private PipeReader mPipeReader = new PipeReader();
    private PipeWriter mPipeWriter = new PipeWriter();

    public interface PipeDataReader<T> {
        void readDataFromPipe(ParcelFileDescriptor parcelFileDescriptor, Uri uri, String str, Bundle bundle, T t);
    }

    protected abstract void UpdateMimeMessageFromStream(FileInputStream fileInputStream, long j, long j2) throws IOException;

    protected abstract void WriteMessageToStream(long j, long j2, boolean z, boolean z2, FileOutputStream fileOutputStream) throws IOException;

    protected abstract int deleteMessage(String str, String str2);

    protected abstract Uri getContentUri();

    protected abstract String insertMessage(String str, String str2);

    protected abstract Cursor queryAccount(String[] strArr, String str, String[] strArr2, String str2);

    protected abstract Cursor queryFolder(String str, String[] strArr, String str2, String[] strArr2, String str3);

    protected abstract Cursor queryMessage(String str, String[] strArr, String str2, String[] strArr2, String str3);

    protected abstract int syncFolder(long j, long j2);

    protected abstract int updateAccount(String str, int i);

    protected abstract int updateMessage(String str, Long l, Long l2, Boolean bool);

    @Override
    public void attachInfo(Context context, ProviderInfo providerInfo) {
        this.mAuthority = providerInfo.authority;
        this.mMatcher = new UriMatcher(-1);
        this.mMatcher.addURI(this.mAuthority, BluetoothMapContract.TABLE_ACCOUNT, 1);
        this.mMatcher.addURI(this.mAuthority, "#/Folder", 3);
        this.mMatcher.addURI(this.mAuthority, "#/Message", 2);
        if (!((ComponentInfo) providerInfo).exported) {
            throw new SecurityException("Provider must be exported");
        }
        if (!"android.permission.BLUETOOTH_MAP".equals(providerInfo.writePermission)) {
            throw new SecurityException("Provider must be protected by android.permission.BLUETOOTH_MAP");
        }
        this.mResolver = context.getContentResolver();
        super.attachInfo(context, providerInfo);
    }

    public class PipeReader implements PipeDataReader<Cursor> {
        public PipeReader() {
        }

        @Override
        public void readDataFromPipe(ParcelFileDescriptor parcelFileDescriptor, Uri uri, String str, Bundle bundle, Cursor cursor) throws Throwable {
            FileInputStream fileInputStream;
            Log.v(BluetoothMapEmailProvider.TAG, "readDataFromPipe(): uri=" + uri.toString());
            FileInputStream fileInputStream2 = null;
            try {
                try {
                    try {
                        fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                    } catch (IOException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                    fileInputStream = fileInputStream2;
                }
                try {
                    BluetoothMapEmailProvider.this.UpdateMimeMessageFromStream(fileInputStream, Long.valueOf(BluetoothMapEmailProvider.getAccountId(uri)).longValue(), Long.valueOf(uri.getLastPathSegment()).longValue());
                    fileInputStream.close();
                } catch (IOException e2) {
                    e = e2;
                    fileInputStream2 = fileInputStream;
                    Log.w(BluetoothMapEmailProvider.TAG, "IOException: ", e);
                    if (fileInputStream2 != null) {
                        fileInputStream2.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e3) {
                            Log.w(BluetoothMapEmailProvider.TAG, e3);
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
                Log.w(BluetoothMapEmailProvider.TAG, e4);
            }
        }
    }

    public class PipeWriter implements ContentProvider.PipeDataWriter<Cursor> {
        public PipeWriter() {
        }

        @Override
        public void writeDataToPipe(ParcelFileDescriptor parcelFileDescriptor, Uri uri, String str, Bundle bundle, Cursor cursor) throws Throwable {
            ?? fileOutputStream;
            List<String> pathSegments;
            long j;
            long j2;
            ?? r11;
            boolean z;
            boolean z2;
            Log.d(BluetoothMapEmailProvider.TAG, "writeDataToPipe(): uri=" + uri.toString() + " - getLastPathSegment() = " + uri.getLastPathSegment());
            ?? r112 = 0;
            ?? r113 = 0;
            try {
                try {
                    try {
                        fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                    } catch (IOException e) {
                        r112 = "IOException: ";
                        Log.w(BluetoothMapEmailProvider.TAG, "IOException: ", e);
                    }
                } catch (IOException e2) {
                    e = e2;
                }
            } catch (Throwable th) {
                th = th;
                fileOutputStream = r112;
            }
            try {
                pathSegments = uri.getPathSegments();
                j = Long.parseLong(pathSegments.get(2));
                j2 = Long.parseLong(BluetoothMapEmailProvider.getAccountId(uri));
                r11 = 4;
            } catch (IOException e3) {
                e = e3;
                r113 = fileOutputStream;
                Log.w(BluetoothMapEmailProvider.TAG, e);
                try {
                    r113.flush();
                } catch (IOException e4) {
                    Log.w(BluetoothMapEmailProvider.TAG, "IOException: ", e4);
                }
                r113.close();
                r112 = r113;
            } catch (Throwable th2) {
                th = th2;
                try {
                    fileOutputStream.flush();
                } catch (IOException e5) {
                    Log.w(BluetoothMapEmailProvider.TAG, "IOException: ", e5);
                }
                try {
                    fileOutputStream.close();
                    throw th;
                } catch (IOException e6) {
                    Log.w(BluetoothMapEmailProvider.TAG, "IOException: ", e6);
                    throw th;
                }
            }
            if (pathSegments.size() >= 4) {
                String str2 = pathSegments.get(3);
                if (!str2.equalsIgnoreCase(BluetoothMapContract.FILE_MSG_NO_ATTACHMENTS)) {
                    if (!str2.equalsIgnoreCase(BluetoothMapContract.FILE_MSG_DOWNLOAD_NO_ATTACHMENTS)) {
                        if (str2.equalsIgnoreCase(BluetoothMapContract.FILE_MSG_DOWNLOAD)) {
                            z2 = true;
                        }
                        z = false;
                        z2 = true;
                        BluetoothMapEmailProvider.this.WriteMessageToStream(j2, j, z2, z, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        r112 = r11;
                    } else {
                        z2 = false;
                        z = true;
                        BluetoothMapEmailProvider.this.WriteMessageToStream(j2, j, z2, z, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        r112 = r11;
                    }
                } else {
                    z2 = false;
                }
                z = z2;
                BluetoothMapEmailProvider.this.WriteMessageToStream(j2, j, z2, z, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                r112 = r11;
            } else {
                z = false;
                z2 = true;
                BluetoothMapEmailProvider.this.WriteMessageToStream(j2, j, z2, z, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                r112 = r11;
            }
        }
    }

    protected void onAccountChanged(String str) {
        Uri uriBuildAccountUriwithId;
        if (this.mAuthority == null) {
            return;
        }
        if (str == null) {
            uriBuildAccountUriwithId = BluetoothMapContract.buildAccountUri(this.mAuthority);
        } else {
            uriBuildAccountUriwithId = BluetoothMapContract.buildAccountUriwithId(this.mAuthority, str);
        }
        Log.d(TAG, "onAccountChanged() accountId = " + str + " URI: " + uriBuildAccountUriwithId);
        this.mResolver.notifyChange(uriBuildAccountUriwithId, null);
    }

    protected void onMessageChanged(String str, String str2) {
        Uri uriBuildMessageUriWithId;
        if (this.mAuthority == null) {
            return;
        }
        if (str == null) {
            uriBuildMessageUriWithId = BluetoothMapContract.buildMessageUri(this.mAuthority);
        } else if (str2 == null) {
            uriBuildMessageUriWithId = BluetoothMapContract.buildMessageUri(this.mAuthority, str);
        } else {
            uriBuildMessageUriWithId = BluetoothMapContract.buildMessageUriWithId(this.mAuthority, str, str2);
        }
        Log.d(TAG, "onMessageChanged() accountId = " + str + " messageId = " + str2 + " URI: " + uriBuildMessageUriWithId);
        this.mResolver.notifyChange(uriBuildMessageUriWithId, null);
    }

    @Override
    public String getType(Uri uri) {
        return "Email";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Log.d(TAG, "openFile(): uri=" + uri.toString() + " - getLastPathSegment() = " + uri.getLastPathSegment());
        try {
            try {
                return str.equals("w") ? openInversePipeHelper(uri, null, null, null, this.mPipeReader) : openPipeHelper(uri, null, null, null, this.mPipeWriter);
            } catch (IOException e) {
                Log.w(TAG, e);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private <T> ParcelFileDescriptor openInversePipeHelper(final Uri uri, final String str, final Bundle bundle, final T t, final PipeDataReader<T> pipeDataReader) throws FileNotFoundException {
        try {
            final ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... objArr) {
                    pipeDataReader.readDataFromPipe(parcelFileDescriptorArrCreatePipe[0], uri, str, bundle, t);
                    try {
                        parcelFileDescriptorArrCreatePipe[0].close();
                        return null;
                    } catch (IOException e) {
                        Log.w(BluetoothMapEmailProvider.TAG, "Failure closing pipe", e);
                        return null;
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            return parcelFileDescriptorArrCreatePipe[1];
        } catch (IOException e) {
            throw new FileNotFoundException("failure making pipe");
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        Log.d(TAG, "delete(): uri=" + uri.toString());
        String str2 = uri.getPathSegments().get(1);
        if (str2 == null) {
            throw new IllegalArgumentException("Table missing in URI");
        }
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            throw new IllegalArgumentException("Message ID missing in update values!");
        }
        String accountId = getAccountId(uri);
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID missing in update values!");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (str2.equals(BluetoothMapContract.TABLE_MESSAGE)) {
                return deleteMessage(accountId, lastPathSegment);
            }
            Log.w(TAG, "Unknown table name: " + str2);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            throw new IllegalArgumentException("Table missing in URI");
        }
        String accountId = getAccountId(uri);
        Long asLong = contentValues.getAsLong(BluetoothMapContract.MessageColumns.FOLDER_ID);
        if (asLong == null) {
            throw new IllegalArgumentException("FolderId missing in ContentValues");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Log.d(TAG, "insert(): uri=" + uri.toString() + " - getLastPathSegment() = " + uri.getLastPathSegment());
        try {
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_MESSAGE)) {
                String strInsertMessage = insertMessage(accountId, asLong.toString());
                Log.i(TAG, "insert() ID: " + strInsertMessage);
                return Uri.parse(uri.toString() + "/" + strInsertMessage);
            }
            Log.w(TAG, "Unknown table name: " + lastPathSegment);
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected String[] convertProjection(String[] strArr, Map<String, String> map) {
        String[] strArr2 = new String[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            strArr2[i] = map.get(strArr[i]) + " as " + strArr[i];
        }
        return strArr2;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            switch (this.mMatcher.match(uri)) {
                case 1:
                    return queryAccount(strArr, str, strArr2, str2);
                case 2:
                    return queryMessage(getAccountId(uri), strArr, str, strArr2, str2);
                case 3:
                    return queryFolder(getAccountId(uri), strArr, str, strArr2, str2);
                default:
                    throw new UnsupportedOperationException("Unsupported Uri " + uri);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            throw new IllegalArgumentException("Table missing in URI");
        }
        if (str != null) {
            throw new IllegalArgumentException("selection shall not be used, ContentValues shall contain the data");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Log.w(TAG, "update(): uri=" + uri.toString() + " - getLastPathSegment() = " + uri.getLastPathSegment());
        try {
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_ACCOUNT)) {
                String asString = contentValues.getAsString("_id");
                if (asString == null) {
                    throw new IllegalArgumentException("Account ID missing in update values!");
                }
                Integer asInteger = contentValues.getAsInteger(BluetoothMapContract.AccountColumns.FLAG_EXPOSE);
                if (asInteger != null) {
                    return updateAccount(asString, asInteger.intValue());
                }
                throw new IllegalArgumentException("Expose flag missing in update values!");
            }
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_FOLDER)) {
                return 0;
            }
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_MESSAGE)) {
                String accountId = getAccountId(uri);
                Long asLong = contentValues.getAsLong("_id");
                if (asLong != null) {
                    return updateMessage(accountId, asLong, contentValues.getAsLong(BluetoothMapContract.MessageColumns.FOLDER_ID), contentValues.getAsBoolean(BluetoothMapContract.MessageColumns.FLAG_READ));
                }
                throw new IllegalArgumentException("Message ID missing in update values!");
            }
            Log.w(TAG, "Unknown table name: " + lastPathSegment);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Log.d(TAG, "call(): method=" + str + " arg=" + str2 + "ThreadId: " + Thread.currentThread().getId());
        try {
            if (!str.equals(BluetoothMapContract.METHOD_UPDATE_FOLDER)) {
                return null;
            }
            long j = bundle.getLong(BluetoothMapContract.EXTRA_UPDATE_ACCOUNT_ID, -1L);
            if (j == -1) {
                Log.w(TAG, "No account ID in CALL");
                return null;
            }
            long j2 = bundle.getLong(BluetoothMapContract.EXTRA_UPDATE_FOLDER_ID, -1L);
            if (j2 == -1) {
                Log.w(TAG, "No folder ID in CALL");
                return null;
            }
            if (syncFolder(j, j2) == 0) {
                return new Bundle();
            }
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void shutdown() {
    }

    public static String getAccountId(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() < 1) {
            throw new IllegalArgumentException("No AccountId pressent in URI: " + uri);
        }
        return pathSegments.get(0);
    }
}
