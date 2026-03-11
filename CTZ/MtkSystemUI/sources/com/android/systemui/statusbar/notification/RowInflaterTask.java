package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.support.v4.view.AsyncLayoutInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.InflationTask;
import com.android.systemui.statusbar.NotificationData;

public class RowInflaterTask implements AsyncLayoutInflater.OnInflateFinishedListener, InflationTask {
    private boolean mCancelled;
    private NotificationData.Entry mEntry;
    private Throwable mInflateOrigin;
    private RowInflationFinishedListener mListener;

    public interface RowInflationFinishedListener {
        void onInflationFinished(ExpandableNotificationRow expandableNotificationRow);
    }

    public void inflate(Context context, ViewGroup viewGroup, NotificationData.Entry entry, RowInflationFinishedListener rowInflationFinishedListener) {
        this.mInflateOrigin = new Throwable("inflate requested here");
        this.mListener = rowInflationFinishedListener;
        AsyncLayoutInflater asyncLayoutInflater = new AsyncLayoutInflater(context);
        this.mEntry = entry;
        entry.setInflationTask(this);
        asyncLayoutInflater.inflate(R.layout.status_bar_notification_row, viewGroup, this);
    }

    @Override
    public void abort() {
        this.mCancelled = true;
    }

    @Override
    public void onInflateFinished(View view, int i, ViewGroup viewGroup) {
        if (!this.mCancelled) {
            try {
                this.mEntry.onInflationTaskFinished();
                this.mListener.onInflationFinished((ExpandableNotificationRow) view);
            } catch (Throwable th) {
                if (this.mInflateOrigin != null) {
                    Log.e("RowInflaterTask", "Error in inflation finished listener: " + th, this.mInflateOrigin);
                    th.addSuppressed(this.mInflateOrigin);
                }
                throw th;
            }
        }
    }
}
