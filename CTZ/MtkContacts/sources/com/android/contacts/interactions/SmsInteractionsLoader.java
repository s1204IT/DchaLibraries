package com.android.contacts.interactions;

import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.Telephony;
import com.android.contacts.compat.TelephonyThreadsCompat;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmsInteractionsLoader extends AsyncTaskLoader<List<ContactInteraction>> {
    private static final String TAG = SmsInteractionsLoader.class.getSimpleName();
    private List<ContactInteraction> mData;
    private int mMaxToRetrieve;
    private String[] mPhoneNums;

    public SmsInteractionsLoader(Context context, String[] strArr, int i) {
        super(context);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "SmsInteractionsLoader");
        }
        this.mPhoneNums = strArr;
        this.mMaxToRetrieve = i;
    }

    @Override
    public List<ContactInteraction> loadInBackground() {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "loadInBackground");
        }
        if (!getContext().getPackageManager().hasSystemFeature("android.hardware.telephony") || this.mPhoneNums == null || this.mPhoneNums.length == 0) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        for (String str : this.mPhoneNums) {
            try {
                arrayList.add(String.valueOf(TelephonyThreadsCompat.getOrCreateThreadId(getContext(), str)));
            } catch (Exception e) {
            }
        }
        Cursor smsCursorFromThreads = getSmsCursorFromThreads(arrayList);
        if (smsCursorFromThreads != null) {
            try {
                ArrayList arrayList2 = new ArrayList();
                while (smsCursorFromThreads.moveToNext()) {
                    ContentValues contentValues = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(smsCursorFromThreads, contentValues);
                    arrayList2.add(new SmsInteraction(contentValues));
                }
                return arrayList2;
            } finally {
                smsCursorFromThreads.close();
            }
        }
        return Collections.emptyList();
    }

    private Cursor getSmsCursorFromThreads(List<String> list) {
        if (list.size() == 0) {
            return null;
        }
        String str = "thread_id IN " + ContactInteractionUtil.questionMarks(list.size());
        return getContext().getContentResolver().query(Telephony.Sms.CONTENT_URI, null, str, (String[]) list.toArray(new String[list.size()]), "date DESC LIMIT " + this.mMaxToRetrieve);
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
