package com.android.contacts.interactions;

import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.util.PermissionsUtil;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CallLogInteractionsLoader extends AsyncTaskLoader<List<ContactInteraction>> {
    private static final String TAG = "CallLogInteractions";
    private List<ContactInteraction> mData;
    private final int mMaxToRetrieve;
    private final String[] mPhoneNumbers;
    private final String[] mSipNumbers;

    public CallLogInteractionsLoader(Context context, String[] strArr, String[] strArr2, int i) {
        super(context);
        this.mPhoneNumbers = strArr;
        this.mSipNumbers = strArr2;
        this.mMaxToRetrieve = i;
    }

    @Override
    public List<ContactInteraction> loadInBackground() {
        Log.d(TAG, "[loadInBackground]");
        boolean z = this.mPhoneNumbers != null && this.mPhoneNumbers.length > 0;
        boolean z2 = this.mSipNumbers != null && this.mSipNumbers.length > 0;
        if (!PermissionsUtil.hasPhonePermissions(getContext()) || !getContext().getPackageManager().hasSystemFeature("android.hardware.telephony") || ((!z && !z2) || this.mMaxToRetrieve <= 0)) {
            Log.d(TAG, "[loadInBackground] return empty list");
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        if (z) {
            for (String str : this.mPhoneNumbers) {
                String strNormalizeNumber = PhoneNumberUtilsCompat.normalizeNumber(str);
                if (!TextUtils.isEmpty(strNormalizeNumber)) {
                    arrayList.addAll(getCallLogInteractions(strNormalizeNumber));
                }
            }
        }
        if (z2) {
            for (String str2 : this.mSipNumbers) {
                arrayList.addAll(getCallLogInteractions(str2));
            }
        }
        Collections.sort(arrayList, new Comparator<ContactInteraction>() {
            @Override
            public int compare(ContactInteraction contactInteraction, ContactInteraction contactInteraction2) {
                if (contactInteraction2.getInteractionDate() - contactInteraction.getInteractionDate() > 0) {
                    return 1;
                }
                if (contactInteraction2.getInteractionDate() == contactInteraction.getInteractionDate()) {
                    return 0;
                }
                return -1;
            }
        });
        if ((z && this.mPhoneNumbers.length == 1 && !z2) || (z2 && this.mSipNumbers.length == 1 && !z)) {
            return arrayList;
        }
        return pruneDuplicateCallLogInteractions(arrayList, this.mMaxToRetrieve);
    }

    static List<ContactInteraction> pruneDuplicateCallLogInteractions(List<ContactInteraction> list, int i) {
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < list.size(); i2++) {
            if (i2 < 1 || list.get(i2).getInteractionDate() != list.get(i2 - 1).getInteractionDate()) {
                arrayList.add(list.get(i2));
                if (arrayList.size() >= i) {
                    break;
                }
            }
        }
        return arrayList;
    }

    private List<ContactInteraction> getCallLogInteractions(String str) {
        Cursor cursorQuery;
        try {
            cursorQuery = getContext().getContentResolver().query(Uri.withAppendedPath(CallLog.Calls.CONTENT_FILTER_URI, Uri.encode(str)), null, null, null, "date DESC LIMIT " + this.mMaxToRetrieve);
        } catch (Exception e) {
            Log.e(TAG, "Can not query calllog", e);
            cursorQuery = null;
        }
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() >= 1) {
                    cursorQuery.moveToPosition(-1);
                    ArrayList arrayList = new ArrayList();
                    while (cursorQuery.moveToNext()) {
                        ContentValues contentValues = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursorQuery, contentValues);
                        arrayList.add(new CallLogInteraction(contentValues));
                    }
                    return arrayList;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        List<ContactInteraction> listEmptyList = Collections.emptyList();
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return listEmptyList;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (this.mData != null) {
            deliverResult(this.mData);
        }
        if (takeContentChanged() || this.mData == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void deliverResult(List<ContactInteraction> list) {
        this.mData = list;
        if (isStarted()) {
            super.deliverResult(list);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (this.mData != null) {
            this.mData.clear();
        }
    }
}
