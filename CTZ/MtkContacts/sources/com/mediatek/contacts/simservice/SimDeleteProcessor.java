package com.mediatek.contacts.simservice;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.android.contacts.ContactSaveService;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimProcessorManager;
import com.mediatek.contacts.util.Log;

public class SimDeleteProcessor extends SimProcessorBase {
    private static Listener mListener = null;
    private Context mContext;
    private Intent mIntent;
    private Uri mLocalContactUri;
    private int mSimIndex;
    private Uri mSimUri;
    private int mSubId;

    public interface Listener {
        void onSIMDeleteCompleted();

        void onSIMDeleteFailed();
    }

    public static void registerListener(Listener listener) {
        if (listener instanceof ContactDeletionInteraction) {
            Log.i("SIMDeleteProcessor", "[registerListener]listener added to SIMDeleteProcessor:" + listener);
            mListener = listener;
        }
    }

    public static void unregisterListener(Listener listener) {
        Log.i("SIMDeleteProcessor", "[unregisterListener]removed from SIMDeleteProcessor: " + listener);
        mListener = null;
    }

    public SimDeleteProcessor(Context context, int i, Intent intent, SimProcessorManager.ProcessorCompleteListener processorCompleteListener) {
        super(intent, processorCompleteListener);
        this.mSimUri = null;
        this.mLocalContactUri = null;
        this.mSimIndex = -1;
        this.mSubId = SubInfoUtils.getInvalidSubId();
        Log.i("SIMDeleteProcessor", "[SIMDeleteProcessor]new...");
        this.mContext = context;
        this.mSubId = i;
        this.mIntent = intent;
    }

    @Override
    public int getType() {
        return 2;
    }

    @Override
    public void doWork() {
        if (isCancelled()) {
            Log.w("SIMDeleteProcessor", "[dowork]cancel remove work. Thread id = " + Thread.currentThread().getId());
            return;
        }
        this.mSimUri = this.mIntent.getData();
        this.mSimIndex = this.mIntent.getIntExtra("sim_index", -1);
        this.mLocalContactUri = (Uri) this.mIntent.getParcelableExtra("local_contact_uri");
        if (this.mContext.getContentResolver().delete(this.mSimUri, "index = " + this.mSimIndex, null) <= 0) {
            Log.i("SIMDeleteProcessor", "[doWork] Delete SIM contact failed");
            if (mListener != null) {
                mListener.onSIMDeleteFailed();
                return;
            }
            return;
        }
        Log.i("SIMDeleteProcessor", "[doWork] Delete SIM contact successfully");
        this.mContext.startService(ContactSaveService.createDeleteContactIntent(this.mContext, this.mLocalContactUri));
        if (mListener != null) {
            mListener.onSIMDeleteCompleted();
        }
    }
}
