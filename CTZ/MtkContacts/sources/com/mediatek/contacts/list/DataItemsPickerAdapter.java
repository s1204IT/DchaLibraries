package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ListView;
import com.android.contacts.list.ContactListFilter;
import com.mediatek.contacts.util.Log;

public class DataItemsPickerAdapter extends PhoneNumbersPickerAdapter {
    private String mMimeType;
    private long[] mRestrictPhoneIds;
    public static final Uri DATA_OTHERS_URI = Uri.parse("content://com.android.contacts/data/others");
    public static final Uri DATA_OTHERS_FILTER_URI = Uri.withAppendedPath(DATA_OTHERS_URI, "filter");

    public DataItemsPickerAdapter(Context context, ListView listView) {
        super(context, listView);
    }

    @Override
    protected Uri configLoaderUri(long j) {
        boolean zIsSearchMode = isSearchMode();
        Log.i("DataItemsPickerAdapter", "[configLoaderUri]directoryId = " + j + ",isSearchMode = " + zIsSearchMode);
        if (j != 0) {
            Log.w("DataItemsPickerAdapter", "[configLoaderUri] MultiDataItemsPickerAdapter is not ready for non-default directory ID (directoryId: " + j + ")");
        }
        if (zIsSearchMode) {
            String queryString = getQueryString();
            Uri.Builder builderBuildUpon = DATA_OTHERS_FILTER_URI.buildUpon();
            builderBuildUpon.appendQueryParameter("specified_data_mime_type", this.mMimeType);
            if (TextUtils.isEmpty(queryString)) {
                builderBuildUpon.appendPath("");
            } else {
                builderBuildUpon.appendPath(queryString);
            }
            builderBuildUpon.appendQueryParameter("directory", String.valueOf(j));
            builderBuildUpon.appendQueryParameter("checked_ids_arg", DATA_OTHERS_URI.toString());
            return builderBuildUpon.build();
        }
        Uri uriBuild = DATA_OTHERS_URI.buildUpon().appendQueryParameter("directory", String.valueOf(0L)).appendQueryParameter("specified_data_mime_type", this.mMimeType).appendQueryParameter("checked_ids_arg", DATA_OTHERS_URI.toString()).build();
        if (isSectionHeaderDisplayEnabled()) {
            return buildSectionIndexerUri(uriBuild);
        }
        return uriBuild;
    }

    @Override
    protected void configureSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
        super.configureSelection(cursorLoader, j, contactListFilter);
        StringBuilder sb = new StringBuilder();
        if (this.mRestrictPhoneIds != null && this.mRestrictPhoneIds.length > 0) {
            sb.append("( ");
            sb.append("_id IN (");
            for (long j2 : this.mRestrictPhoneIds) {
                sb.append(j2 + ",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(") )");
        } else {
            sb.append("(0)");
        }
        sb.append(cursorLoader.getSelection());
        cursorLoader.setSelection(sb.toString());
    }

    public void setRestrictList(long[] jArr) {
        this.mRestrictPhoneIds = jArr;
    }

    public void setMimetype(String str) {
        this.mMimeType = str;
    }
}
