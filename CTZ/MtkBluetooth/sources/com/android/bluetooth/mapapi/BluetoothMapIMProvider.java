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
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BluetoothMapIMProvider extends ContentProvider {
    private static final boolean D = true;
    private static final int MATCH_ACCOUNT = 1;
    private static final int MATCH_CONVERSATION = 4;
    private static final int MATCH_CONVOCONTACT = 5;
    private static final int MATCH_MESSAGE = 3;
    private static final String TAG = "BluetoothMapIMProvider";
    private Uri CONTENT_URI = null;
    private String mAuthority;
    private UriMatcher mMatcher;
    protected ContentResolver mResolver;

    protected abstract int deleteMessage(String str, String str2);

    protected abstract Uri getContentUri();

    protected abstract String insertMessage(String str, ContentValues contentValues);

    protected abstract Cursor queryAccount(String[] strArr, String str, String[] strArr2, String str2);

    protected abstract Cursor queryConversation(String str, Long l, Boolean bool, Long l2, Long l3, String str2, String[] strArr, String str3);

    protected abstract Cursor queryConvoContact(String str, Long l, String[] strArr, String str2, String[] strArr2, String str3);

    protected abstract Cursor queryMessage(String str, String[] strArr, String str2, String[] strArr2, String str3);

    protected abstract int setBluetoothStatus(boolean z);

    protected abstract int setOwnerStatus(int i, String str, long j, int i2, String str2);

    protected abstract int syncFolder(long j, long j2);

    protected abstract int updateAccount(String str, Integer num);

    protected abstract int updateMessage(String str, Long l, Long l2, Boolean bool);

    @Override
    public void attachInfo(Context context, ProviderInfo providerInfo) {
        this.mAuthority = providerInfo.authority;
        this.mMatcher = new UriMatcher(-1);
        this.mMatcher.addURI(this.mAuthority, BluetoothMapContract.TABLE_ACCOUNT, 1);
        this.mMatcher.addURI(this.mAuthority, "#/Message", 3);
        this.mMatcher.addURI(this.mAuthority, "#/Conversation", 4);
        this.mMatcher.addURI(this.mAuthority, "#/ConvoContact", 5);
        if (!((ComponentInfo) providerInfo).exported) {
            throw new SecurityException("Provider must be exported");
        }
        if (!"android.permission.BLUETOOTH_MAP".equals(providerInfo.writePermission)) {
            throw new SecurityException("Provider must be protected by android.permission.BLUETOOTH_MAP");
        }
        Log.d(TAG, "attachInfo() mAuthority = " + this.mAuthority);
        this.mResolver = context.getContentResolver();
        super.attachInfo(context, providerInfo);
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

    protected void onContactChanged(String str, String str2) {
        Uri uriBuildConvoContactsUriWithId;
        if (this.mAuthority == null) {
            return;
        }
        if (str == null) {
            uriBuildConvoContactsUriWithId = BluetoothMapContract.buildConvoContactsUri(this.mAuthority);
        } else if (str2 == null) {
            uriBuildConvoContactsUriWithId = BluetoothMapContract.buildConvoContactsUri(this.mAuthority, str);
        } else {
            uriBuildConvoContactsUriWithId = BluetoothMapContract.buildConvoContactsUriWithId(this.mAuthority, str, str2);
        }
        Log.d(TAG, "onContactChanged() accountId = " + str + " contactId = " + str2 + " URI: " + uriBuildConvoContactsUriWithId);
        this.mResolver.notifyChange(uriBuildConvoContactsUriWithId, null);
    }

    @Override
    public String getType(Uri uri) {
        return "InstantMessage";
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
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID missing in URI");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Log.d(TAG, "insert(): uri=" + uri.toString() + " - getLastPathSegment() = " + uri.getLastPathSegment());
        try {
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_MESSAGE)) {
                String strInsertMessage = insertMessage(accountId, contentValues);
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
            Log.w(TAG, "query(): uri =" + this.mAuthority + " uri=" + uri.toString());
            int iMatch = this.mMatcher.match(uri);
            if (iMatch == 1) {
                return queryAccount(strArr, str, strArr2, str2);
            }
            switch (iMatch) {
                case 3:
                    return queryMessage(getAccountId(uri), strArr, str, strArr2, str2);
                case 4:
                    String accountId = getAccountId(uri);
                    String queryParameter = uri.getQueryParameter(BluetoothMapContract.FILTER_ORIGINATOR_SUBSTRING);
                    String queryParameter2 = uri.getQueryParameter(BluetoothMapContract.FILTER_PERIOD_BEGIN);
                    Long lValueOf = queryParameter2 != null ? Long.valueOf(Long.parseLong(queryParameter2)) : null;
                    String queryParameter3 = uri.getQueryParameter(BluetoothMapContract.FILTER_PERIOD_END);
                    Long lValueOf2 = queryParameter3 != null ? Long.valueOf(Long.parseLong(queryParameter3)) : null;
                    String queryParameter4 = uri.getQueryParameter(BluetoothMapContract.FILTER_READ_STATUS);
                    Boolean boolValueOf = queryParameter4 != null ? Boolean.valueOf(queryParameter4.equalsIgnoreCase("true")) : null;
                    String queryParameter5 = uri.getQueryParameter("thread_id");
                    return queryConversation(accountId, queryParameter5 != null ? Long.valueOf(Long.parseLong(queryParameter5)) : null, boolValueOf, lValueOf2, lValueOf, queryParameter, strArr, str2);
                case 5:
                    return queryConvoContact(getAccountId(uri), 0L, strArr, str, strArr2, str2);
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
                    return updateAccount(asString, asInteger);
                }
                throw new IllegalArgumentException("Expose flag missing in update values!");
            }
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_FOLDER)) {
                return 0;
            }
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_MESSAGE)) {
                String accountId = getAccountId(uri);
                if (accountId == null) {
                    throw new IllegalArgumentException("Account ID missing in update values!");
                }
                Long asLong = contentValues.getAsLong("_id");
                if (asLong != null) {
                    return updateMessage(accountId, asLong, contentValues.getAsLong(BluetoothMapContract.MessageColumns.FOLDER_ID), contentValues.getAsBoolean(BluetoothMapContract.MessageColumns.FLAG_READ));
                }
                throw new IllegalArgumentException("Message ID missing in update values!");
            }
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_CONVERSATION)) {
                return 0;
            }
            if (lastPathSegment.equals(BluetoothMapContract.TABLE_CONVOCONTACT)) {
                return 0;
            }
            Log.w(TAG, "Unknown table name: " + lastPathSegment);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected ContentValues createContentValues(Set<Map.Entry<String, Object>> set, Map<String, String> map) {
        ContentValues contentValues = new ContentValues(set.size());
        for (Map.Entry<String, Object> entry : set) {
            String str = map.get(entry.getKey());
            Object value = entry.getValue();
            if (value == null) {
                contentValues.putNull(str);
            } else if (entry.getValue() instanceof Boolean) {
                contentValues.put(str, (Boolean) value);
            } else if (entry.getValue() instanceof Byte) {
                contentValues.put(str, (Byte) value);
            } else if (entry.getValue() instanceof byte[]) {
                contentValues.put(str, (byte[]) value);
            } else if (entry.getValue() instanceof Double) {
                contentValues.put(str, (Double) value);
            } else if (entry.getValue() instanceof Float) {
                contentValues.put(str, (Float) value);
            } else if (entry.getValue() instanceof Integer) {
                contentValues.put(str, (Integer) value);
            } else if (entry.getValue() instanceof Long) {
                contentValues.put(str, (Long) value);
            } else if (entry.getValue() instanceof Short) {
                contentValues.put(str, (Short) value);
            } else if (entry.getValue() instanceof String) {
                contentValues.put(str, (String) value);
            } else {
                throw new IllegalArgumentException("Unknown data type in content value");
            }
        }
        return contentValues;
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        int ownerStatus;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Log.w(TAG, "call(): method=" + str + " arg=" + str2 + "ThreadId: " + Thread.currentThread().getId());
        try {
            if (str.equals(BluetoothMapContract.METHOD_UPDATE_FOLDER)) {
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
                ownerStatus = syncFolder(j, j2);
            } else {
                ownerStatus = str.equals(BluetoothMapContract.METHOD_SET_OWNER_STATUS) ? setOwnerStatus(bundle.getInt(BluetoothMapContract.EXTRA_PRESENCE_STATE), bundle.getString(BluetoothMapContract.EXTRA_PRESENCE_STATUS), bundle.getLong(BluetoothMapContract.EXTRA_LAST_ACTIVE), bundle.getInt(BluetoothMapContract.EXTRA_CHAT_STATE), bundle.getString(BluetoothMapContract.EXTRA_CONVERSATION_ID)) : str.equals(BluetoothMapContract.METHOD_SET_BLUETOOTH_STATE) ? setBluetoothStatus(bundle.getBoolean(BluetoothMapContract.EXTRA_BLUETOOTH_STATE)) : -1;
            }
            if (ownerStatus == 0) {
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
