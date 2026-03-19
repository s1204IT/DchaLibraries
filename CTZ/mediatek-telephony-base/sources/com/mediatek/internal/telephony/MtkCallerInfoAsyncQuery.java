package com.mediatek.internal.telephony;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import java.util.Iterator;

public class MtkCallerInfoAsyncQuery extends CallerInfoAsyncQuery {
    public static CallerInfoAsyncQuery startQuery(int i, Context context, Uri uri, CallerInfoAsyncQuery.OnQueryCompleteListener onQueryCompleteListener, Object obj) throws CallerInfoAsyncQuery.QueryPoolException {
        MtkCallerInfoAsyncQuery mtkCallerInfoAsyncQuery = new MtkCallerInfoAsyncQuery();
        mtkCallerInfoAsyncQuery.allocate(context, uri);
        CallerInfoAsyncQuery.CookieWrapper cookieWrapper = new CallerInfoAsyncQuery.CookieWrapper();
        cookieWrapper.listener = onQueryCompleteListener;
        cookieWrapper.cookie = obj;
        cookieWrapper.event = 1;
        mtkCallerInfoAsyncQuery.mHandler.startQuery(i, cookieWrapper, uri, (String[]) null, (String) null, (String[]) null, (String) null);
        return mtkCallerInfoAsyncQuery;
    }

    public static CallerInfoAsyncQuery startQuery(int i, Context context, String str, CallerInfoAsyncQuery.OnQueryCompleteListener onQueryCompleteListener, Object obj, int i2) throws CallerInfoAsyncQuery.QueryPoolException {
        Uri uriBuild = ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI.buildUpon().appendPath(str).appendQueryParameter("sip", String.valueOf(PhoneNumberUtils.isUriNumber(str))).build();
        MtkCallerInfoAsyncQuery mtkCallerInfoAsyncQuery = new MtkCallerInfoAsyncQuery();
        mtkCallerInfoAsyncQuery.allocate(context, uriBuild);
        CallerInfoAsyncQuery.CookieWrapper cookieWrapper = new CallerInfoAsyncQuery.CookieWrapper();
        cookieWrapper.listener = onQueryCompleteListener;
        cookieWrapper.cookie = obj;
        cookieWrapper.number = str;
        cookieWrapper.subId = i2;
        if (MtkPhoneNumberUtils.isEmergencyNumberExt(str, TelephonyManager.getDefault().getCurrentPhoneType(cookieWrapper.subId))) {
            cookieWrapper.event = 4;
        } else if (PhoneNumberUtils.isVoiceMailNumber(context, i2, str)) {
            cookieWrapper.event = 5;
        } else {
            cookieWrapper.event = 1;
        }
        mtkCallerInfoAsyncQuery.mHandler.startQuery(i, cookieWrapper, uriBuild, (String[]) null, (String) null, (String[]) null, (String) null);
        return mtkCallerInfoAsyncQuery;
    }

    protected void allocate(Context context, Uri uri) throws CallerInfoAsyncQuery.QueryPoolException {
        if (context == null || uri == null) {
            throw new CallerInfoAsyncQuery.QueryPoolException("Bad context or query uri.");
        }
        this.mHandler = new MtkCallerInfoAsyncQueryHandler(context);
        this.mHandler.mQueryUri = uri;
    }

    public class MtkCallerInfoAsyncQueryHandler extends CallerInfoAsyncQuery.CallerInfoAsyncQueryHandler {
        protected MtkCallerInfoAsyncQueryHandler(Context context) {
            super(MtkCallerInfoAsyncQuery.this, context);
        }

        protected void onQueryComplete(final int i, Object obj, Cursor cursor) throws CallerInfoAsyncQuery.QueryPoolException {
            Rlog.d("CallerInfoAsyncQuery", "##### onQueryComplete() #####   query complete for token: " + i);
            final CallerInfoAsyncQuery.CookieWrapper cookieWrapper = (CallerInfoAsyncQuery.CookieWrapper) obj;
            if (cookieWrapper == null) {
                Rlog.i("CallerInfoAsyncQuery", "Cookie is null, ignoring onQueryComplete() request.");
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
            if (cookieWrapper.event == 3) {
                Iterator it = this.mPendingListenerCallbacks.iterator();
                while (it.hasNext()) {
                    ((Runnable) it.next()).run();
                }
                this.mPendingListenerCallbacks.clear();
                MtkCallerInfoAsyncQuery.this.release();
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
            if (cookieWrapper.event == 6) {
                if (this.mCallerInfo != null) {
                    this.mCallerInfo.geoDescription = cookieWrapper.geoDescription;
                }
                CallerInfoAsyncQuery.CookieWrapper cookieWrapper2 = new CallerInfoAsyncQuery.CookieWrapper();
                cookieWrapper2.event = 3;
                startQuery(i, cookieWrapper2, null, null, null, null, null);
            }
            if (this.mCallerInfo == null) {
                if (this.mContext == null || this.mQueryUri == null) {
                    throw new CallerInfoAsyncQuery.QueryPoolException("Bad context or query uri, or CallerInfoAsyncQuery already released.");
                }
                if (cookieWrapper.event == 4) {
                    this.mCallerInfo = new MtkCallerInfo().markAsEmergency(this.mContext);
                } else if (cookieWrapper.event != 5) {
                    this.mCallerInfo = MtkCallerInfo.getCallerInfo(this.mContext, this.mQueryUri, cursor, cookieWrapper.subId);
                    CallerInfo callerInfoDoSecondaryLookupIfNecessary = CallerInfo.doSecondaryLookupIfNecessary(this.mContext, cookieWrapper.number, this.mCallerInfo);
                    if (callerInfoDoSecondaryLookupIfNecessary != this.mCallerInfo) {
                        this.mCallerInfo = callerInfoDoSecondaryLookupIfNecessary;
                    }
                    if (!TextUtils.isEmpty(cookieWrapper.number)) {
                        this.mCallerInfo.phoneNumber = PhoneNumberUtils.formatNumber(cookieWrapper.number, this.mCallerInfo.normalizedNumber, CallerInfo.getCurrentCountryIso(this.mContext));
                    }
                    if (TextUtils.isEmpty(this.mCallerInfo.name)) {
                        cookieWrapper.event = 6;
                        startQuery(i, cookieWrapper, null, null, null, null, null);
                        return;
                    }
                } else {
                    this.mCallerInfo = new MtkCallerInfo().markAsVoiceMail(cookieWrapper.subId);
                }
                CallerInfoAsyncQuery.CookieWrapper cookieWrapper3 = new CallerInfoAsyncQuery.CookieWrapper();
                cookieWrapper3.event = 3;
                startQuery(i, cookieWrapper3, null, null, null, null, null);
            }
            if (cookieWrapper.listener != null) {
                this.mPendingListenerCallbacks.add(new Runnable() {
                    @Override
                    public void run() {
                        cookieWrapper.listener.onQueryComplete(i, cookieWrapper.cookie, MtkCallerInfoAsyncQueryHandler.this.mCallerInfo);
                    }
                });
            } else {
                Rlog.w("CallerInfoAsyncQuery", "There is no listener to notify for this query.");
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
