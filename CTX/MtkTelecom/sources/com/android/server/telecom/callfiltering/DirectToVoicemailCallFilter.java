package com.android.server.telecom.callfiltering;

import android.net.Uri;
import android.telecom.Log;
import com.android.internal.telephony.CallerInfo;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallerInfoLookupHelper;
import com.android.server.telecom.callfiltering.IncomingCallFilter;
import java.util.Objects;

public class DirectToVoicemailCallFilter implements IncomingCallFilter.CallFilter {
    private final CallerInfoLookupHelper mCallerInfoLookupHelper;

    public DirectToVoicemailCallFilter(CallerInfoLookupHelper callerInfoLookupHelper) {
        this.mCallerInfoLookupHelper = callerInfoLookupHelper;
    }

    @Override
    public void startFilterLookup(final Call call, final CallFilterResultCallback callFilterResultCallback) {
        Log.addEvent(call, "DIRECT_TO_VM_INITIATED");
        final Uri handle = call.getHandle();
        this.mCallerInfoLookupHelper.startLookup(handle, new CallerInfoLookupHelper.OnQueryCompleteListener() {
            @Override
            public void onCallerInfoQueryComplete(Uri uri, CallerInfo callerInfo) {
                CallFilteringResult callFilteringResult;
                if (uri != null && Objects.equals(handle, uri)) {
                    if (callerInfo != null && callerInfo.shouldSendToVoicemail) {
                        callFilteringResult = new CallFilteringResult(false, true, true, true);
                    } else {
                        callFilteringResult = new CallFilteringResult(true, false, true, true);
                    }
                    Log.addEvent(call, "DIRECT_TO_VM_FINISHED", callFilteringResult);
                    callFilterResultCallback.onCallFilteringComplete(call, callFilteringResult);
                    return;
                }
                CallFilteringResult callFilteringResult2 = new CallFilteringResult(true, false, true, true);
                Log.addEvent(call, "DIRECT_TO_VM_FINISHED", callFilteringResult2);
                Log.w(this, "CallerInfo lookup returned with a different handle than what was passed in. Was %s, should be %s", new Object[]{uri, handle});
                callFilterResultCallback.onCallFilteringComplete(call, callFilteringResult2);
            }

            @Override
            public void onContactPhotoQueryComplete(Uri uri, CallerInfo callerInfo) {
            }
        }, call.getSubsciptionId());
    }
}
