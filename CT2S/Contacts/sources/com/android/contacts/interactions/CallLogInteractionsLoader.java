package com.android.contacts.interactions;

import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CallLogInteractionsLoader extends AsyncTaskLoader<List<ContactInteraction>> {
    private List<ContactInteraction> mData;
    private final int mMaxToRetrieve;
    private final String[] mPhoneNumbers;

    public CallLogInteractionsLoader(Context context, String[] phoneNumbers, int maxToRetrieve) {
        super(context);
        this.mPhoneNumbers = phoneNumbers;
        this.mMaxToRetrieve = maxToRetrieve;
    }

    @Override
    public List<ContactInteraction> loadInBackground() {
        if (!getContext().getPackageManager().hasSystemFeature("android.hardware.telephony") || this.mPhoneNumbers == null || this.mPhoneNumbers.length <= 0 || this.mMaxToRetrieve <= 0) {
            return Collections.emptyList();
        }
        List<ContactInteraction> interactions = new ArrayList<>();
        String[] arr$ = this.mPhoneNumbers;
        for (String number : arr$) {
            interactions.addAll(getCallLogInteractions(number));
        }
        Collections.sort(interactions, new Comparator<ContactInteraction>() {
            @Override
            public int compare(ContactInteraction i1, ContactInteraction i2) {
                if (i2.getInteractionDate() - i1.getInteractionDate() > 0) {
                    return 1;
                }
                if (i2.getInteractionDate() == i1.getInteractionDate()) {
                    return 0;
                }
                return -1;
            }
        });
        return this.mPhoneNumbers.length != 1 ? pruneDuplicateCallLogInteractions(interactions, this.mMaxToRetrieve) : interactions;
    }

    static List<ContactInteraction> pruneDuplicateCallLogInteractions(List<ContactInteraction> interactions, int maxToRetrieve) {
        List<ContactInteraction> subsetInteractions = new ArrayList<>();
        for (int i = 0; i < interactions.size(); i++) {
            if (i < 1 || interactions.get(i).getInteractionDate() != interactions.get(i - 1).getInteractionDate()) {
                subsetInteractions.add(interactions.get(i));
                if (subsetInteractions.size() >= maxToRetrieve) {
                    break;
                }
            }
        }
        return subsetInteractions;
    }

    private List<ContactInteraction> getCallLogInteractions(String phoneNumber) {
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
        if (TextUtils.isEmpty(normalizedNumber)) {
            return Collections.emptyList();
        }
        Uri uri = Uri.withAppendedPath(CallLog.Calls.CONTENT_FILTER_URI, Uri.encode(normalizedNumber));
        String orderByAndLimit = "date DESC LIMIT " + this.mMaxToRetrieve;
        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, orderByAndLimit);
        if (cursor != null) {
            try {
                if (cursor.getCount() >= 1) {
                    cursor.moveToPosition(-1);
                    List<ContactInteraction> interactions = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor, values);
                        interactions.add(new CallLogInteraction(values));
                    }
                    if (cursor == null) {
                        return interactions;
                    }
                    cursor.close();
                    return interactions;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        List<ContactInteraction> listEmptyList = Collections.emptyList();
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
