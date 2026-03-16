package com.android.contacts.interactions;

import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.Telephony;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmsInteractionsLoader extends AsyncTaskLoader<List<ContactInteraction>> {
    private static final String TAG = SmsInteractionsLoader.class.getSimpleName();
    private List<ContactInteraction> mData;
    private int mMaxToRetrieve;
    private String[] mPhoneNums;

    public SmsInteractionsLoader(Context context, String[] phoneNums, int maxToRetrieve) {
        super(context);
        Log.v(TAG, "SmsInteractionsLoader");
        this.mPhoneNums = phoneNums;
        this.mMaxToRetrieve = maxToRetrieve;
    }

    @Override
    public List<ContactInteraction> loadInBackground() {
        Log.v(TAG, "loadInBackground");
        if (!getContext().getPackageManager().hasSystemFeature("android.hardware.telephony") || this.mPhoneNums == null || this.mPhoneNums.length == 0) {
            return Collections.emptyList();
        }
        List<String> threadIdStrings = new ArrayList<>();
        String[] arr$ = this.mPhoneNums;
        for (String phone : arr$) {
            try {
                threadIdStrings.add(String.valueOf(Telephony.Threads.getOrCreateThreadId(getContext(), phone)));
            } catch (Exception e) {
            }
        }
        Cursor cursor = getSmsCursorFromThreads(threadIdStrings);
        if (cursor != null) {
            try {
                List<ContactInteraction> interactions = new ArrayList<>();
                while (cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, values);
                    interactions.add(new SmsInteraction(values));
                }
                return interactions;
            } finally {
                cursor.close();
            }
        }
        return Collections.emptyList();
    }

    private Cursor getSmsCursorFromThreads(List<String> threadIds) {
        if (threadIds.size() == 0) {
            return null;
        }
        String selection = "thread_id IN " + ContactInteractionUtil.questionMarks(threadIds.size());
        return getContext().getContentResolver().query(Telephony.Sms.CONTENT_URI, null, selection, (String[]) threadIds.toArray(new String[threadIds.size()]), "date DESC LIMIT " + this.mMaxToRetrieve);
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
    public void deliverResult(List<ContactInteraction> data) {
        this.mData = data;
        if (isStarted()) {
            super.deliverResult(data);
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
