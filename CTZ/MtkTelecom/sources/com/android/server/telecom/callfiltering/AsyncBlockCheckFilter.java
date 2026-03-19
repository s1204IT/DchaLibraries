package com.android.server.telecom.callfiltering;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telecom.Log;
import android.telecom.Logging.Session;
import com.android.internal.telephony.CallerInfo;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallerInfoLookupHelper;
import com.android.server.telecom.callfiltering.IncomingCallFilter;
import com.android.server.telecom.settings.BlockedNumbersUtil;

public class AsyncBlockCheckFilter extends AsyncTask<String, Void, Boolean> implements IncomingCallFilter.CallFilter {
    private Session mBackgroundTaskSubsession;
    private final BlockCheckerAdapter mBlockCheckerAdapter;
    private CallFilterResultCallback mCallback;
    private CallerInfoLookupHelper mCallerInfoLookupHelper;
    private final Context mContext;
    private Call mIncomingCall;
    private Session mPostExecuteSubsession;

    public AsyncBlockCheckFilter(Context context, BlockCheckerAdapter blockCheckerAdapter, CallerInfoLookupHelper callerInfoLookupHelper) {
        this.mContext = context;
        this.mBlockCheckerAdapter = blockCheckerAdapter;
        this.mCallerInfoLookupHelper = callerInfoLookupHelper;
    }

    @Override
    public void startFilterLookup(Call call, CallFilterResultCallback callFilterResultCallback) {
        this.mCallback = callFilterResultCallback;
        this.mIncomingCall = call;
        final String schemeSpecificPart = call.getHandle() == null ? null : call.getHandle().getSchemeSpecificPart();
        if (BlockedNumbersUtil.isEnhancedCallBlockingEnabledByPlatform(this.mContext)) {
            final int handlePresentation = this.mIncomingCall.getHandlePresentation();
            if (handlePresentation == 1) {
                this.mCallerInfoLookupHelper.startLookup(call.getHandle(), new CallerInfoLookupHelper.OnQueryCompleteListener() {
                    @Override
                    public void onCallerInfoQueryComplete(Uri uri, CallerInfo callerInfo) {
                        boolean z;
                        if (callerInfo != null) {
                            z = callerInfo.contactExists;
                        } else {
                            z = false;
                        }
                        AsyncBlockCheckFilter.this.execute(schemeSpecificPart, String.valueOf(handlePresentation), String.valueOf(z));
                    }

                    @Override
                    public void onContactPhotoQueryComplete(Uri uri, CallerInfo callerInfo) {
                    }
                });
                return;
            } else {
                execute(schemeSpecificPart, String.valueOf(handlePresentation));
                return;
            }
        }
        execute(schemeSpecificPart);
    }

    @Override
    protected void onPreExecute() {
        this.mBackgroundTaskSubsession = Log.createSubsession();
        this.mPostExecuteSubsession = Log.createSubsession();
    }

    @Override
    protected Boolean doInBackground(String... strArr) {
        try {
            Log.continueSession(this.mBackgroundTaskSubsession, "ABCF.dIB");
            Log.addEvent(this.mIncomingCall, "BLOCK_CHECK_INITIATED");
            Bundle bundle = new Bundle();
            if (strArr.length > 1) {
                bundle.putInt("extra_call_presentation", Integer.valueOf(strArr[1]).intValue());
            }
            if (strArr.length > 2) {
                bundle.putBoolean("extra_contact_exist", Boolean.valueOf(strArr[2]).booleanValue());
            }
            return Boolean.valueOf(this.mBlockCheckerAdapter.isBlocked(this.mContext, strArr[0], bundle));
        } finally {
            Log.endSession();
        }
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        CallFilteringResult callFilteringResult;
        Log.continueSession(this.mPostExecuteSubsession, "ABCF.oPE");
        try {
            if (bool.booleanValue()) {
                callFilteringResult = new CallFilteringResult(false, true, false, false);
            } else {
                callFilteringResult = new CallFilteringResult(true, false, true, true);
            }
            Log.addEvent(this.mIncomingCall, "BLOCK_CHECK_FINISHED", callFilteringResult);
            this.mCallback.onCallFilteringComplete(this.mIncomingCall, callFilteringResult);
        } finally {
            Log.endSession();
        }
    }
}
