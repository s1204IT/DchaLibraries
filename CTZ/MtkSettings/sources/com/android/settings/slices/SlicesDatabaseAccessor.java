package com.android.settings.slices;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Pair;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SliceData;
import java.util.ArrayList;
import java.util.List;

public class SlicesDatabaseAccessor {
    public static final String[] SELECT_COLUMNS_ALL = {"key", "title", "summary", "screentitle", "keywords", "icon", "fragment", "controller", "platform_slice", "slice_type"};
    private final int TRUE = 1;
    private final Context mContext;
    private final SlicesDatabaseHelper mHelper;

    public SlicesDatabaseAccessor(Context context) {
        this.mContext = context;
        this.mHelper = SlicesDatabaseHelper.getInstance(this.mContext);
    }

    public SliceData getSliceDataFromUri(Uri uri) {
        Pair<Boolean, String> pathData = SliceBuilderUtils.getPathData(uri);
        return buildSliceData(getIndexedSliceData((String) pathData.second), uri, ((Boolean) pathData.first).booleanValue());
    }

    public SliceData getSliceDataFromKey(String str) {
        return buildSliceData(getIndexedSliceData(str), null, false);
    }

    public List<String> getSliceKeys(boolean z) {
        verifyIndexing();
        String str = z ? "platform_slice = 1" : "platform_slice = 0";
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = this.mHelper.getReadableDatabase().query("slices_index", new String[]{"key"}, str, null, null, null, null);
        Throwable th = null;
        try {
            try {
                if (!cursorQuery.moveToFirst()) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return arrayList;
                }
                do {
                    arrayList.add(cursorQuery.getString(0));
                } while (cursorQuery.moveToNext());
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return arrayList;
            } finally {
            }
        } catch (Throwable th2) {
            if (cursorQuery != null) {
            }
            throw th2;
        }
        if (cursorQuery != null) {
            if (th != null) {
                try {
                    cursorQuery.close();
                } catch (Throwable th3) {
                    th.addSuppressed(th3);
                }
            } else {
                cursorQuery.close();
            }
        }
        throw th2;
    }

    private Cursor getIndexedSliceData(String str) {
        verifyIndexing();
        Cursor cursorQuery = this.mHelper.getReadableDatabase().query("slices_index", SELECT_COLUMNS_ALL, buildKeyMatchWhereClause(), new String[]{str}, null, null, null);
        int count = cursorQuery.getCount();
        if (count == 0) {
            throw new IllegalStateException("Invalid Slices key from path: " + str);
        }
        if (count > 1) {
            throw new IllegalStateException("Should not match more than 1 slice with path: " + str);
        }
        cursorQuery.moveToFirst();
        return cursorQuery;
    }

    private String buildKeyMatchWhereClause() {
        return "key = ?";
    }

    private SliceData buildSliceData(Cursor cursor, Uri uri, boolean z) {
        String string = cursor.getString(cursor.getColumnIndex("key"));
        String string2 = cursor.getString(cursor.getColumnIndex("title"));
        String string3 = cursor.getString(cursor.getColumnIndex("summary"));
        String string4 = cursor.getString(cursor.getColumnIndex("screentitle"));
        String string5 = cursor.getString(cursor.getColumnIndex("keywords"));
        int i = cursor.getInt(cursor.getColumnIndex("icon"));
        String string6 = cursor.getString(cursor.getColumnIndex("fragment"));
        String string7 = cursor.getString(cursor.getColumnIndex("controller"));
        boolean z2 = cursor.getInt(cursor.getColumnIndex("platform_slice")) == 1;
        int i2 = cursor.getInt(cursor.getColumnIndex("slice_type"));
        if (z) {
            i2 = 0;
        }
        return new SliceData.Builder().setKey(string).setTitle(string2).setSummary(string3).setScreenTitle(string4).setKeywords(string5).setIcon(i).setFragmentName(string6).setPreferenceControllerClassName(string7).setUri(uri).setPlatformDefined(z2).setSliceType(i2).build();
    }

    private void verifyIndexing() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            FeatureFactory.getFactory(this.mContext).getSlicesFeatureProvider().indexSliceData(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }
}
