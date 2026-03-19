package com.android.server.telecom;

import android.content.Context;
import com.android.internal.telephony.CallerInfoAsyncQuery;

public interface CallerInfoAsyncQueryFactory {
    CallerInfoAsyncQuery startQuery(int i, Context context, String str, CallerInfoAsyncQuery.OnQueryCompleteListener onQueryCompleteListener, Object obj, int i2);
}
