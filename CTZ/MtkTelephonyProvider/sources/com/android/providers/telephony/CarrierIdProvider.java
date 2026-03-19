package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.nano.CarrierIdProto;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import libcore.io.IoUtils;

public class CarrierIdProvider extends ContentProvider {

    @VisibleForTesting
    public static final String AUTHORITY = "carrier_id";
    private final Map<Integer, Pair<Integer, String>> mCurrentSubscriptionMap = new ConcurrentHashMap();
    private CarrierIdDatabaseHelper mDbHelper;
    private static final String TAG = CarrierIdProvider.class.getSimpleName();
    private static final String PREF_FILE = CarrierIdProvider.class.getSimpleName();
    private static final UriMatcher s_urlMatcher = new UriMatcher(-1);
    private static final List<String> CARRIERS_ID_UNIQUE_FIELDS = new ArrayList(Arrays.asList("mccmnc", "gid1", "gid2", "plmn", "imsi_prefix_xpattern", "spn", "apn", "iccid_prefix"));

    @VisibleForTesting
    public static String getStringForCarrierIdTableCreation(String str) {
        return "CREATE TABLE " + str + "(_id INTEGER PRIMARY KEY,mccmnc TEXT NOT NULL,gid1 TEXT,gid2 TEXT,plmn TEXT,imsi_prefix_xpattern TEXT,spn TEXT,apn TEXT,iccid_prefix TEXT,carrier_name TEXT," + AUTHORITY + " INTEGER DEFAULT -1,UNIQUE (" + TextUtils.join(", ", CARRIERS_ID_UNIQUE_FIELDS) + "));";
    }

    @VisibleForTesting
    public static String getStringForIndexCreation(String str) {
        return "CREATE INDEX IF NOT EXISTS mccmncIndex ON " + str + " (mccmnc);";
    }

    @Override
    public boolean onCreate() throws Throwable {
        Log.d(TAG, "onCreate");
        this.mDbHelper = new CarrierIdDatabaseHelper(getContext());
        this.mDbHelper.getReadableDatabase();
        s_urlMatcher.addURI(AUTHORITY, "all", 1);
        s_urlMatcher.addURI(AUTHORITY, "all/update_db", 2);
        s_urlMatcher.addURI(AUTHORITY, "all/get_version", 3);
        updateDatabaseFromPb(this.mDbHelper.getWritableDatabase());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        Log.d(TAG, "getType");
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        int iMatch = s_urlMatcher.match(uri);
        if (iMatch == 1) {
            checkReadPermission();
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            sQLiteQueryBuilder.setTables(AUTHORITY);
            return sQLiteQueryBuilder.query(getReadableDatabase(), strArr, str, strArr2, null, null, str2);
        }
        if (iMatch == 3) {
            checkReadPermission();
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"version"});
            matrixCursor.addRow(new Object[]{Integer.valueOf(getAppliedVersion())});
            return matrixCursor;
        }
        return queryCarrierIdForCurrentSubscription(uri, strArr);
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        checkWritePermission();
        if (s_urlMatcher.match(uri) == 1) {
            long jInsertOrThrow = getWritableDatabase().insertOrThrow(AUTHORITY, null, contentValues);
            if (jInsertOrThrow <= 0) {
                return null;
            }
            Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.CarrierId.All.CONTENT_URI, jInsertOrThrow);
            getContext().getContentResolver().notifyChange(Telephony.CarrierId.All.CONTENT_URI, null);
            return uriWithAppendedId;
        }
        throw new IllegalArgumentException("Cannot insert that URL: " + uri);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        checkWritePermission();
        if (s_urlMatcher.match(uri) == 1) {
            int iDelete = getWritableDatabase().delete(AUTHORITY, str, strArr);
            Log.d(TAG, "  delete.count=" + iDelete);
            if (iDelete > 0) {
                getContext().getContentResolver().notifyChange(Telephony.CarrierId.All.CONTENT_URI, null);
            }
            return iDelete;
        }
        throw new IllegalArgumentException("Cannot delete that URL: " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        checkWritePermission();
        switch (s_urlMatcher.match(uri)) {
            case 1:
                int iUpdate = getWritableDatabase().update(AUTHORITY, contentValues, str, strArr);
                Log.d(TAG, "  update.count=" + iUpdate);
                if (iUpdate > 0) {
                    getContext().getContentResolver().notifyChange(Telephony.CarrierId.All.CONTENT_URI, null);
                }
                return iUpdate;
            case 2:
                return updateDatabaseFromPb(getWritableDatabase());
            default:
                return updateCarrierIdForCurrentSubscription(uri, contentValues);
        }
    }

    SQLiteDatabase getReadableDatabase() {
        return this.mDbHelper.getReadableDatabase();
    }

    SQLiteDatabase getWritableDatabase() {
        return this.mDbHelper.getWritableDatabase();
    }

    private class CarrierIdDatabaseHelper extends SQLiteOpenHelper {
        private final String TAG;

        public CarrierIdDatabaseHelper(Context context) {
            super(context, "carrierIdentification.db", (SQLiteDatabase.CursorFactory) null, 3);
            this.TAG = CarrierIdDatabaseHelper.class.getSimpleName();
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            Log.d(this.TAG, "onCreate");
            sQLiteDatabase.execSQL(CarrierIdProvider.getStringForCarrierIdTableCreation(CarrierIdProvider.AUTHORITY));
            sQLiteDatabase.execSQL(CarrierIdProvider.getStringForIndexCreation(CarrierIdProvider.AUTHORITY));
        }

        public void createCarrierTable(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL(CarrierIdProvider.getStringForCarrierIdTableCreation(CarrierIdProvider.AUTHORITY));
            sQLiteDatabase.execSQL(CarrierIdProvider.getStringForIndexCreation(CarrierIdProvider.AUTHORITY));
        }

        public void dropCarrierTable(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS carrier_id;");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            Log.d(this.TAG, "dbh.onUpgrade:+ db=" + sQLiteDatabase + " oldV=" + i + " newV=" + i2);
            if (i < 3) {
                dropCarrierTable(sQLiteDatabase);
                createCarrierTable(sQLiteDatabase);
            }
        }
    }

    private int updateDatabaseFromPb(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Log.d(TAG, "update database from pb file");
        CarrierIdProto.CarrierList updateCarrierList = getUpdateCarrierList();
        int i = 0;
        if (updateCarrierList == null) {
            return 0;
        }
        try {
            sQLiteDatabase.beginTransaction();
            sQLiteDatabase.delete(AUTHORITY, null, null);
            CarrierIdProto.CarrierId[] carrierIdArr = updateCarrierList.carrierId;
            int length = carrierIdArr.length;
            int i2 = 0;
            int i3 = 0;
            while (i2 < length) {
                CarrierIdProto.CarrierId carrierId = carrierIdArr[i2];
                CarrierIdProto.CarrierAttribute[] carrierAttributeArr = carrierId.carrierAttribute;
                int length2 = carrierAttributeArr.length;
                int i4 = i3;
                int i5 = i;
                while (i5 < length2) {
                    CarrierIdProto.CarrierAttribute carrierAttribute = carrierAttributeArr[i5];
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(AUTHORITY, Integer.valueOf(carrierId.canonicalId));
                    contentValues.put("carrier_name", carrierId.carrierName);
                    ArrayList arrayList = new ArrayList();
                    convertCarrierAttrToContentValues(contentValues, arrayList, carrierAttribute, i);
                    Iterator<ContentValues> it = arrayList.iterator();
                    while (it.hasNext()) {
                        if (sQLiteDatabase.insertWithOnConflict(AUTHORITY, null, it.next(), 4) > 0) {
                            i4++;
                        } else {
                            Log.e(TAG, "updateDatabaseFromPB insertion failure, row: " + i4 + "carrier id: " + carrierId.canonicalId);
                        }
                    }
                    i5++;
                    i = 0;
                }
                i2++;
                i3 = i4;
                i = 0;
            }
            Log.d(TAG, "update database from pb. inserted rows = " + i3);
            if (i3 > 0) {
                getContext().getContentResolver().notifyChange(Telephony.CarrierId.All.CONTENT_URI, null);
            }
            setAppliedVersion(updateCarrierList.version);
            sQLiteDatabase.setTransactionSuccessful();
            return i3;
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    private void convertCarrierAttrToContentValues(ContentValues contentValues, List<ContentValues> list, CarrierIdProto.CarrierAttribute carrierAttribute, int i) {
        boolean z;
        if (i > 7) {
            list.add(new ContentValues(contentValues));
            return;
        }
        int i2 = 0;
        switch (i) {
            case 0:
                String[] strArr = carrierAttribute.mccmncTuple;
                int length = strArr.length;
                z = false;
                while (i2 < length) {
                    contentValues.put("mccmnc", strArr[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("mccmnc");
                    i2++;
                    z = true;
                }
                break;
            case 1:
                String[] strArr2 = carrierAttribute.imsiPrefixXpattern;
                int length2 = strArr2.length;
                z = false;
                while (i2 < length2) {
                    contentValues.put("imsi_prefix_xpattern", strArr2[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("imsi_prefix_xpattern");
                    i2++;
                    z = true;
                }
                break;
            case 2:
                String[] strArr3 = carrierAttribute.gid1;
                int length3 = strArr3.length;
                z = false;
                while (i2 < length3) {
                    contentValues.put("gid1", strArr3[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("gid1");
                    i2++;
                    z = true;
                }
                break;
            case 3:
                String[] strArr4 = carrierAttribute.gid2;
                int length4 = strArr4.length;
                z = false;
                while (i2 < length4) {
                    contentValues.put("gid2", strArr4[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("gid2");
                    i2++;
                    z = true;
                }
                break;
            case 4:
                String[] strArr5 = carrierAttribute.plmn;
                int length5 = strArr5.length;
                z = false;
                while (i2 < length5) {
                    contentValues.put("plmn", strArr5[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("plmn");
                    i2++;
                    z = true;
                }
                break;
            case 5:
                String[] strArr6 = carrierAttribute.spn;
                int length6 = strArr6.length;
                z = false;
                while (i2 < length6) {
                    contentValues.put("spn", strArr6[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("spn");
                    i2++;
                    z = true;
                }
                break;
            case 6:
                String[] strArr7 = carrierAttribute.preferredApn;
                int length7 = strArr7.length;
                z = false;
                while (i2 < length7) {
                    contentValues.put("apn", strArr7[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("apn");
                    i2++;
                    z = true;
                }
                break;
            case 7:
                String[] strArr8 = carrierAttribute.iccidPrefix;
                int length8 = strArr8.length;
                z = false;
                while (i2 < length8) {
                    contentValues.put("iccid_prefix", strArr8[i2]);
                    convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
                    contentValues.remove("iccid_prefix");
                    i2++;
                    z = true;
                }
                break;
            default:
                Log.e(TAG, "unsupported index: " + i);
                z = false;
                break;
        }
        if (!z) {
            convertCarrierAttrToContentValues(contentValues, list, carrierAttribute, i + 1);
        }
    }

    private CarrierIdProto.CarrierList getUpdateCarrierList() throws Throwable {
        InputStream inputStreamOpen;
        CarrierIdProto.CarrierList from;
        CarrierIdProto.CarrierList from2;
        FileInputStream fileInputStream;
        int appliedVersion = getAppliedVersion();
        CarrierIdProto.CarrierList carrierList = null;
        try {
            try {
                inputStreamOpen = getContext().getAssets().open("carrier_list.pb");
                try {
                    from = CarrierIdProto.CarrierList.parseFrom(readInputStreamToByteArray(inputStreamOpen));
                    IoUtils.closeQuietly(inputStreamOpen);
                } catch (IOException e) {
                    e = e;
                    Log.e(TAG, "read carrier list from assets pb failure: " + e);
                    IoUtils.closeQuietly(inputStreamOpen);
                    from = null;
                }
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(inputStreamOpen);
                throw th;
            }
        } catch (IOException e2) {
            e = e2;
            inputStreamOpen = null;
        } catch (Throwable th2) {
            th = th2;
            inputStreamOpen = null;
            IoUtils.closeQuietly(inputStreamOpen);
            throw th;
        }
        try {
            try {
                fileInputStream = new FileInputStream(new File(Environment.getDataDirectory(), "misc/carrierid/carrier_list.pb"));
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (IOException e3) {
            e = e3;
        }
        try {
            from2 = CarrierIdProto.CarrierList.parseFrom(readInputStreamToByteArray(fileInputStream));
            IoUtils.closeQuietly(fileInputStream);
        } catch (IOException e4) {
            e = e4;
            inputStreamOpen = fileInputStream;
            Log.e(TAG, "read carrier list from ota pb failure: " + e);
            IoUtils.closeQuietly(inputStreamOpen);
            from2 = null;
        } catch (Throwable th4) {
            th = th4;
            inputStreamOpen = fileInputStream;
            IoUtils.closeQuietly(inputStreamOpen);
            throw th;
        }
        if (from != null && from.version > appliedVersion) {
            appliedVersion = from.version;
            carrierList = from;
        }
        if (from2 != null && from2.version > appliedVersion) {
            appliedVersion = from2.version;
            carrierList = from2;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("latest version: ");
        sb.append(appliedVersion);
        sb.append(" need update: ");
        sb.append(carrierList != null);
        Log.d(str, sb.toString());
        return carrierList;
    }

    private int getAppliedVersion() {
        return getContext().getSharedPreferences(PREF_FILE, 0).getInt("version", -1);
    }

    private void setAppliedVersion(int i) {
        SharedPreferences.Editor editorEdit = getContext().getSharedPreferences(PREF_FILE, 0).edit();
        editorEdit.putInt("version", i);
        editorEdit.apply();
    }

    private static byte[] readInputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[16384];
        while (true) {
            int i = inputStream.read(bArr, 0, bArr.length);
            if (i != -1) {
                byteArrayOutputStream.write(bArr, 0, i);
            } else {
                byteArrayOutputStream.flush();
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    private int updateCarrierIdForCurrentSubscription(Uri uri, ContentValues contentValues) {
        try {
            int defaultSubId = Integer.parseInt(uri.getLastPathSegment());
            Log.d(TAG, "updateCarrierIdForSubId: " + defaultSubId);
            if (defaultSubId == Integer.MAX_VALUE) {
                defaultSubId = SubscriptionController.getInstance().getDefaultSubId();
            }
            if (!SubscriptionController.getInstance().isActiveSubId(defaultSubId)) {
                int i = 0;
                List listAsList = Arrays.asList(SubscriptionController.getInstance().getActiveSubIdList());
                Iterator<Integer> it = this.mCurrentSubscriptionMap.keySet().iterator();
                while (it.hasNext()) {
                    int iIntValue = it.next().intValue();
                    if (!listAsList.contains(Integer.valueOf(iIntValue))) {
                        i++;
                        Log.d(TAG, "updateCarrierIdForSubId: " + iIntValue);
                        this.mCurrentSubscriptionMap.remove(Integer.valueOf(iIntValue));
                        getContext().getContentResolver().notifyChange(Telephony.CarrierId.CONTENT_URI, null);
                    }
                }
                return i;
            }
            this.mCurrentSubscriptionMap.put(Integer.valueOf(defaultSubId), new Pair<>(contentValues.getAsInteger(AUTHORITY), contentValues.getAsString("carrier_name")));
            getContext().getContentResolver().notifyChange(Telephony.CarrierId.CONTENT_URI, null);
            return 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid subid in provided uri " + uri);
        }
    }

    private Cursor queryCarrierIdForCurrentSubscription(Uri uri, String[] strArr) {
        int defaultSubId = SubscriptionController.getInstance().getDefaultSubId();
        if (!TextUtils.isEmpty(uri.getLastPathSegment())) {
            try {
                defaultSubId = Integer.parseInt(uri.getLastPathSegment());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid subid in provided uri" + uri);
            }
        }
        Log.d(TAG, "queryCarrierIdForSubId: " + defaultSubId);
        if (defaultSubId == Integer.MAX_VALUE) {
            defaultSubId = SubscriptionController.getInstance().getDefaultSubId();
        }
        if (!this.mCurrentSubscriptionMap.containsKey(Integer.valueOf(defaultSubId))) {
            return new MatrixCursor(strArr, 0);
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr, 1);
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        for (int i = 0; i < matrixCursor.getColumnCount(); i++) {
            String columnName = matrixCursor.getColumnName(i);
            if (AUTHORITY.equals(columnName)) {
                rowBuilderNewRow.add(this.mCurrentSubscriptionMap.get(Integer.valueOf(defaultSubId)).first);
            } else if ("carrier_name".equals(columnName)) {
                rowBuilderNewRow.add(this.mCurrentSubscriptionMap.get(Integer.valueOf(defaultSubId)).second);
            } else {
                throw new IllegalArgumentException("Invalid column " + strArr[i]);
            }
        }
        return matrixCursor;
    }

    private void checkReadPermission() {
        if (getContext().checkCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") == 0) {
        } else {
            throw new SecurityException("No permission to read CarrierId provider");
        }
    }

    private void checkWritePermission() {
        if (getContext().checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0) {
        } else {
            throw new SecurityException("No permission to write CarrierId provider");
        }
    }
}
