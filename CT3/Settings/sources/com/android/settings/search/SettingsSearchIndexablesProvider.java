package com.android.settings.search;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesProvider;
import java.util.Collection;

public class SettingsSearchIndexablesProvider extends SearchIndexablesProvider {
    public boolean onCreate() {
        return true;
    }

    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);
        Collection<SearchIndexableResource> values = SearchIndexableResources.values();
        for (SearchIndexableResource val : values) {
            Object[] ref = {Integer.valueOf(val.rank), Integer.valueOf(val.xmlResId), val.className, Integer.valueOf(val.iconResId), null, null, null};
            cursor.addRow(ref);
        }
        return cursor;
    }

    public Cursor queryRawData(String[] projection) {
        MatrixCursor result = new MatrixCursor(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
        return result;
    }

    public Cursor queryNonIndexableKeys(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        return cursor;
    }
}
