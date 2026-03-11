package com.android.launcher3.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.launcher3.provider.LauncherDbUtils;
import com.android.launcher3.util.IOUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DbDowngradeHelper {
    private static final String KEY_DOWNGRADE_TO = "downgrade_to_";
    private static final String KEY_VERSION = "version";
    private static final String TAG = "DbDowngradeHelper";
    private final SparseArray<String[]> mStatements = new SparseArray<>();
    public final int version;

    private DbDowngradeHelper(int i) {
        this.version = i;
    }

    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Exception {
        ArrayList arrayList = new ArrayList();
        for (int i3 = i - 1; i3 >= i2; i3--) {
            String[] strArr = this.mStatements.get(i3);
            if (strArr == null) {
                throw new SQLiteException("Downgrade path not supported to version " + i3);
            }
            Collections.addAll(arrayList, strArr);
        }
        LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
        try {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                sQLiteDatabase.execSQL((String) it.next());
            }
            sQLiteTransaction.commit();
        } finally {
            $closeResource(null, sQLiteTransaction);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static DbDowngradeHelper parse(File file) throws JSONException, IOException {
        JSONObject jSONObject = new JSONObject(new String(IOUtils.toByteArray(file)));
        DbDowngradeHelper dbDowngradeHelper = new DbDowngradeHelper(jSONObject.getInt(KEY_VERSION));
        for (int i = dbDowngradeHelper.version - 1; i > 0; i--) {
            if (jSONObject.has(KEY_DOWNGRADE_TO + i)) {
                JSONArray jSONArray = jSONObject.getJSONArray(KEY_DOWNGRADE_TO + i);
                String[] strArr = new String[jSONArray.length()];
                for (int i2 = 0; i2 < strArr.length; i2++) {
                    strArr[i2] = jSONArray.getString(i2);
                }
                dbDowngradeHelper.mStatements.put(i, strArr);
            }
        }
        return dbDowngradeHelper;
    }

    public static void updateSchemaFile(File file, int i, Context context, int i2) throws Exception {
        Throwable th;
        Throwable th2;
        try {
            if (parse(file).version >= i) {
                return;
            }
        } catch (Exception e) {
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                InputStream inputStreamOpenRawResource = context.getResources().openRawResource(i2);
                try {
                    IOUtils.copy(inputStreamOpenRawResource, fileOutputStream);
                    if (inputStreamOpenRawResource != null) {
                        $closeResource(null, inputStreamOpenRawResource);
                    }
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        th = th3;
                        th2 = th4;
                        if (inputStreamOpenRawResource != null) {
                            throw th2;
                        }
                        $closeResource(th, inputStreamOpenRawResource);
                        throw th2;
                    }
                }
            } finally {
                $closeResource(null, fileOutputStream);
            }
        } catch (IOException e2) {
            Log.e(TAG, "Error writing schema file", e2);
        }
    }
}
