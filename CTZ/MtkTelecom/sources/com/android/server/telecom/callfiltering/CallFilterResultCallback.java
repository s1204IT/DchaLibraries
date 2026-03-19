package com.android.server.telecom.callfiltering;

import com.android.server.telecom.Call;

public interface CallFilterResultCallback {
    void onCallFilteringComplete(Call call, CallFilteringResult callFilteringResult);
}
