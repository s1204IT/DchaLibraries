package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;
import com.android.vcard.VCardEntry;
import java.util.ArrayList;
import java.util.Iterator;

public class PhonebookPullRequest extends PullRequest {
    private static final int MAX_OPS = 250;
    private static final String TAG = "PbapPhonebookPullRequest";
    private static final boolean VDBG = false;
    public boolean complete = false;
    private final Account mAccount;
    private final Context mContext;

    public PhonebookPullRequest(Context context, Account account) {
        this.mContext = context;
        this.mAccount = account;
        this.path = PbapClientConnectionHandler.PB_PATH;
    }

    @Override
    public void onPullComplete() {
        if (this.mEntries == null) {
            Log.e(TAG, "onPullComplete entries is null.");
            return;
        }
        try {
            try {
                ContentResolver contentResolver = this.mContext.getContentResolver();
                ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
                Iterator<VCardEntry> it = this.mEntries.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    VCardEntry next = it.next();
                    if (Thread.currentThread().isInterrupted()) {
                        Log.e(TAG, "Interrupted durring insert.");
                        break;
                    }
                    int size = arrayList.size();
                    next.constructInsertOperations(contentResolver, arrayList);
                    if (arrayList.size() >= MAX_OPS) {
                        arrayList.subList(size, arrayList.size()).clear();
                        contentResolver.applyBatch("com.android.contacts", arrayList);
                        arrayList = next.constructInsertOperations(contentResolver, null);
                        if (arrayList.size() >= MAX_OPS) {
                            arrayList.clear();
                        }
                    }
                }
                if (arrayList.size() > 0) {
                    contentResolver.applyBatch("com.android.contacts", arrayList);
                    arrayList.clear();
                }
            } catch (OperationApplicationException | RemoteException | NumberFormatException e) {
                Log.e(TAG, "Got exception: ", e);
            }
        } finally {
            this.complete = true;
        }
    }
}
