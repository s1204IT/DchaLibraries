package com.android.server.telecom.callfiltering;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Log;
import android.telecom.Logging.Runnable;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.Call;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.Timeouts;
import java.util.Iterator;
import java.util.List;

public class IncomingCallFilter implements CallFilterResultCallback {
    private final Call mCall;
    private final Context mContext;
    private final List<CallFilter> mFilters;
    private final CallFilterResultCallback mListener;
    private int mNumPendingFilters;
    private final TelecomSystem.SyncRoot mTelecomLock;
    private final Timeouts.Adapter mTimeoutsAdapter;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private CallFilteringResult mResult = new CallFilteringResult(true, false, true, true);
    private boolean mIsPending = true;

    public interface CallFilter {
        void startFilterLookup(Call call, CallFilterResultCallback callFilterResultCallback);
    }

    public IncomingCallFilter(Context context, CallFilterResultCallback callFilterResultCallback, Call call, TelecomSystem.SyncRoot syncRoot, Timeouts.Adapter adapter, List<CallFilter> list) {
        this.mContext = context;
        this.mListener = callFilterResultCallback;
        this.mCall = call;
        this.mTelecomLock = syncRoot;
        this.mFilters = list;
        this.mNumPendingFilters = list.size();
        this.mTimeoutsAdapter = adapter;
    }

    public void performFiltering() {
        Log.addEvent(this.mCall, "FILTERING_INITIATED");
        Iterator<CallFilter> it = this.mFilters.iterator();
        while (it.hasNext()) {
            it.next().startFilterLookup(this.mCall, this);
        }
        this.mHandler.postDelayed(new Runnable("ICF.pFTO", this.mTelecomLock) {
            public void loggedRun() {
                if (IncomingCallFilter.this.mIsPending) {
                    Log.i(IncomingCallFilter.this, "Call filtering has timed out.", new Object[0]);
                    Log.addEvent(IncomingCallFilter.this.mCall, "FILTERING_TIMED_OUT");
                    IncomingCallFilter.this.mListener.onCallFilteringComplete(IncomingCallFilter.this.mCall, IncomingCallFilter.this.mResult);
                    IncomingCallFilter.this.mIsPending = false;
                }
            }
        }.prepare(), this.mTimeoutsAdapter.getCallScreeningTimeoutMillis(this.mContext.getContentResolver()));
    }

    @Override
    public void onCallFilteringComplete(Call call, CallFilteringResult callFilteringResult) {
        synchronized (this.mTelecomLock) {
            this.mNumPendingFilters--;
            this.mResult = callFilteringResult.combine(this.mResult);
            if (this.mNumPendingFilters == 0) {
                this.mHandler.post(new Runnable("ICF.oCFC", this.mTelecomLock) {
                    public void loggedRun() {
                        if (IncomingCallFilter.this.mIsPending) {
                            Log.addEvent(IncomingCallFilter.this.mCall, "FILTERING_COMPLETED", IncomingCallFilter.this.mResult);
                            IncomingCallFilter.this.mListener.onCallFilteringComplete(IncomingCallFilter.this.mCall, IncomingCallFilter.this.mResult);
                            IncomingCallFilter.this.mIsPending = false;
                        }
                    }
                }.prepare());
            }
        }
    }

    @VisibleForTesting
    public Handler getHandler() {
        return this.mHandler;
    }
}
