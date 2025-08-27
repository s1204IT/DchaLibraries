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

/* loaded from: classes.dex */
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

    private static /* synthetic */ void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
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

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [101=4, 104=4] */
    /* JADX WARN: Removed duplicated region for block: B:22:0x0031 A[Catch: all -> 0x0035, Throwable -> 0x0037, TRY_ENTER, TryCatch #4 {, blocks: (B:9:0x0011, B:12:0x001f, B:22:0x0031, B:23:0x0034), top: B:33:0x0011, outer: #0 }] */
    /* JADX WARN: Removed duplicated region for block: B:41:? A[Catch: all -> 0x0035, Throwable -> 0x0037, SYNTHETIC, TRY_LEAVE, TryCatch #4 {, blocks: (B:9:0x0011, B:12:0x001f, B:22:0x0031, B:23:0x0034), top: B:33:0x0011, outer: #0 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void updateSchemaFile(File file, int i, Context context, int i2) throws Exception {
        Throwable th;
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
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    if (inputStreamOpenRawResource != null) {
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
