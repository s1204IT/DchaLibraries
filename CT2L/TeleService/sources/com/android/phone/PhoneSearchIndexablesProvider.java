package com.android.phone;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesProvider;

public class PhoneSearchIndexablesProvider extends SearchIndexablesProvider {
    private static SearchIndexableResource[] INDEXABLE_RES = {new SearchIndexableResource(1, R.xml.network_setting, MobileNetworkSettings.class.getName(), R.mipmap.ic_launcher_phone)};

    public boolean onCreate() {
        return true;
    }

    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);
        int count = INDEXABLE_RES.length;
        for (int n = 0; n < count; n++) {
            Object[] ref = {Integer.valueOf(INDEXABLE_RES[n].rank), Integer.valueOf(INDEXABLE_RES[n].xmlResId), null, Integer.valueOf(INDEXABLE_RES[n].iconResId), "android.intent.action.MAIN", "com.android.phone", INDEXABLE_RES[n].className};
            cursor.addRow(ref);
        }
        return cursor;
    }

    public Cursor queryRawData(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
        return cursor;
    }

    public Cursor queryNonIndexableKeys(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        return cursor;
    }
}
