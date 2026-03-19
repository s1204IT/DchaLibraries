package com.android.traceur;

import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesProvider;
import android.provider.Settings;

public class SearchProvider extends SearchIndexablesProvider {
    public boolean onCreate() {
        return true;
    }

    public Cursor queryXmlResources(String[] strArr) {
        return null;
    }

    public Cursor queryRawData(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
        Context context = getContext();
        Object[] objArr = new Object[SearchIndexablesContract.INDEXABLES_RAW_COLUMNS.length];
        objArr[12] = context.getString(R.string.system_tracing);
        objArr[1] = context.getString(R.string.system_tracing);
        objArr[2] = context.getString(R.string.record_system_activity);
        objArr[5] = context.getString(R.string.keywords);
        objArr[9] = "android.intent.action.MAIN";
        objArr[10] = ((PackageItemInfo) getContext().getApplicationInfo()).packageName;
        objArr[11] = MainActivity.class.getName();
        matrixCursor.addRow(objArr);
        return matrixCursor;
    }

    public Cursor queryNonIndexableKeys(String[] strArr) {
        if (!(Settings.Global.getInt(getContext().getContentResolver(), "development_settings_enabled", 0) != 0)) {
            MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
            matrixCursor.addRow(new Object[]{getContext().getString(R.string.system_tracing)});
            return matrixCursor;
        }
        return null;
    }
}
