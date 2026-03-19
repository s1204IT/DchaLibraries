package com.android.vcard;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import com.android.vcard.VCardEntry;
import java.util.ArrayList;
import java.util.List;

public class VCardEntryCommitter implements VCardEntryHandler {
    public static String LOG_TAG = "MTK_vCard";
    private final ContentResolver mContentResolver;
    private int mCounter;
    private ArrayList<ContentProviderOperation> mOperationList;
    private long mTimeToCommit;
    private final ArrayList<Uri> mCreatedUris = new ArrayList<>();
    private int mTotalPhotoSize = 0;

    public VCardEntryCommitter(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onEnd() {
        if (this.mOperationList != null) {
            this.mCreatedUris.add(pushIntoContentResolver(this.mOperationList));
        }
        if (VCardConfig.showPerformanceLog()) {
            Log.d(LOG_TAG, String.format("time to commit entries: %d ms", Long.valueOf(this.mTimeToCommit)));
        }
        this.mTotalPhotoSize = 0;
    }

    @Override
    public void onEntryCreated(VCardEntry vCardEntry) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mOperationList = vCardEntry.constructInsertOperations(this.mContentResolver, this.mOperationList);
        this.mCounter++;
        this.mTotalPhotoSize += getPhotoSize(vCardEntry);
        if (this.mOperationList != null && (this.mOperationList.size() >= 400 || this.mTotalPhotoSize >= 524288)) {
            this.mCreatedUris.add(pushIntoContentResolver(this.mOperationList));
            this.mCounter = 0;
            this.mTotalPhotoSize = 0;
            this.mOperationList = null;
        }
        this.mTimeToCommit += System.currentTimeMillis() - jCurrentTimeMillis;
    }

    private int getPhotoSize(VCardEntry vCardEntry) {
        List<VCardEntry.PhotoData> photoList = vCardEntry.getPhotoList();
        int length = 0;
        if (photoList == null) {
            return 0;
        }
        for (VCardEntry.PhotoData photoData : photoList) {
            if (!photoData.isEmpty()) {
                length += photoData.getBytes().length;
            }
        }
        return length;
    }

    private Uri pushIntoContentResolver(ArrayList<ContentProviderOperation> arrayList) {
        try {
            ContentProviderResult[] contentProviderResultArrApplyBatch = this.mContentResolver.applyBatch("com.android.contacts", arrayList);
            if (contentProviderResultArrApplyBatch != null && contentProviderResultArrApplyBatch.length != 0 && contentProviderResultArrApplyBatch[0] != null) {
                return contentProviderResultArrApplyBatch[0].uri;
            }
            return null;
        } catch (OperationApplicationException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            return null;
        } catch (RemoteException e2) {
            Log.e(LOG_TAG, String.format("%s: %s", e2.toString(), e2.getMessage()));
            return null;
        }
    }

    public ArrayList<Uri> getCreatedUris() {
        return this.mCreatedUris;
    }
}
