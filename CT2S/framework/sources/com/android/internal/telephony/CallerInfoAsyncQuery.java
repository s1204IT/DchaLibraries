package com.android.internal.telephony;

import android.app.ActivityManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

public class CallerInfoAsyncQuery {
    private static final boolean DBG = false;
    private static final boolean ENABLE_UNKNOWN_NUMBER_GEO_DESCRIPTION = true;
    private static final int EVENT_ADD_LISTENER = 2;
    private static final int EVENT_EMERGENCY_NUMBER = 4;
    private static final int EVENT_END_OF_QUEUE = 3;
    private static final int EVENT_NEW_QUERY = 1;
    private static final int EVENT_VOICEMAIL_NUMBER = 5;
    private static final String LOG_TAG = "CallerInfoAsyncQuery";
    private CallerInfoAsyncQueryHandler mHandler;

    public interface OnQueryCompleteListener {
        void onQueryComplete(int i, Object obj, CallerInfo callerInfo);
    }

    private static final class CookieWrapper {
        public Object cookie;
        public int event;
        public OnQueryCompleteListener listener;
        public String number;
        public int subId;

        private CookieWrapper() {
        }
    }

    public static class QueryPoolException extends SQLException {
        public QueryPoolException(String error) {
            super(error);
        }
    }

    static ContentResolver getCurrentProfileContentResolver(Context context) {
        int currentUser = ActivityManager.getCurrentUser();
        int myUser = UserManager.get(context).getUserHandle();
        if (myUser != currentUser) {
            try {
                Context otherContext = context.createPackageContextAsUser(context.getPackageName(), 0, new UserHandle(currentUser));
                return otherContext.getContentResolver();
            } catch (PackageManager.NameNotFoundException e) {
                Rlog.e(LOG_TAG, "Can't find self package", e);
            }
        }
        return context.getContentResolver();
    }

    private class CallerInfoAsyncQueryHandler extends AsyncQueryHandler {
        private CallerInfo mCallerInfo;
        private Context mContext;
        private Uri mQueryUri;

        protected class CallerInfoWorkerHandler extends AsyncQueryHandler.WorkerHandler {
            public CallerInfoWorkerHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                AsyncQueryHandler.WorkerArgs args = (AsyncQueryHandler.WorkerArgs) msg.obj;
                CookieWrapper cw = (CookieWrapper) args.cookie;
                if (cw == null) {
                    super.handleMessage(msg);
                }
                switch (cw.event) {
                    case 1:
                        super.handleMessage(msg);
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        Message reply = args.handler.obtainMessage(msg.what);
                        reply.obj = args;
                        reply.arg1 = msg.arg1;
                        reply.sendToTarget();
                        break;
                }
            }
        }

        private CallerInfoAsyncQueryHandler(Context context) {
            super(CallerInfoAsyncQuery.getCurrentProfileContentResolver(context));
            this.mContext = context;
        }

        @Override
        protected Handler createHandler(Looper looper) {
            return new CallerInfoWorkerHandler(looper);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            CookieWrapper cw = (CookieWrapper) cookie;
            if (cw == null) {
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
            if (cw.event == 3) {
                CallerInfoAsyncQuery.this.release();
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
            if (this.mCallerInfo == null) {
                if (this.mContext == null || this.mQueryUri == null) {
                    throw new QueryPoolException("Bad context or query uri, or CallerInfoAsyncQuery already released.");
                }
                if (cw.event == 4) {
                    this.mCallerInfo = new CallerInfo().markAsEmergency(this.mContext);
                } else if (cw.event == 5) {
                    this.mCallerInfo = new CallerInfo().markAsVoiceMail(cw.subId);
                } else {
                    this.mCallerInfo = CallerInfo.getCallerInfo(this.mContext, this.mQueryUri, cursor);
                    CallerInfo newCallerInfo = CallerInfo.doSecondaryLookupIfNecessary(this.mContext, cw.number, this.mCallerInfo);
                    if (newCallerInfo != this.mCallerInfo) {
                        this.mCallerInfo = newCallerInfo;
                    }
                    if (TextUtils.isEmpty(this.mCallerInfo.name)) {
                        this.mCallerInfo.updateGeoDescription(this.mContext, cw.number);
                    }
                    if (!TextUtils.isEmpty(cw.number)) {
                        this.mCallerInfo.phoneNumber = PhoneNumberUtils.formatNumber(cw.number, this.mCallerInfo.normalizedNumber, CallerInfo.getCurrentCountryIso(this.mContext));
                    }
                }
                CookieWrapper endMarker = new CookieWrapper();
                endMarker.event = 3;
                startQuery(token, endMarker, null, null, null, null, null);
            }
            if (cw.listener != null) {
                cw.listener.onQueryComplete(token, cw.cookie, this.mCallerInfo);
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private CallerInfoAsyncQuery() {
    }

    public static CallerInfoAsyncQuery startQuery(int token, Context context, Uri contactRef, OnQueryCompleteListener listener, Object cookie) {
        CallerInfoAsyncQuery c = new CallerInfoAsyncQuery();
        c.allocate(context, contactRef);
        CookieWrapper cw = new CookieWrapper();
        cw.listener = listener;
        cw.cookie = cookie;
        cw.event = 1;
        c.mHandler.startQuery(token, cw, contactRef, null, null, null, null);
        return c;
    }

    public static CallerInfoAsyncQuery startQuery(int token, Context context, String number, OnQueryCompleteListener listener, Object cookie) {
        int subId = SubscriptionManager.getDefaultSubId();
        return startQuery(token, context, number, listener, cookie, subId);
    }

    public static CallerInfoAsyncQuery startQuery(int token, Context context, String number, OnQueryCompleteListener listener, Object cookie, int subId) {
        Uri contactRef = ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI.buildUpon().appendPath(number).appendQueryParameter("sip", String.valueOf(PhoneNumberUtils.isUriNumber(number))).build();
        CallerInfoAsyncQuery c = new CallerInfoAsyncQuery();
        c.allocate(context, contactRef);
        CookieWrapper cw = new CookieWrapper();
        cw.listener = listener;
        cw.cookie = cookie;
        cw.number = number;
        cw.subId = subId;
        if (PhoneNumberUtils.isLocalEmergencyNumber(context, number)) {
            cw.event = 4;
        } else if (PhoneNumberUtils.isVoiceMailNumber(subId, number)) {
            cw.event = 5;
        } else {
            cw.event = 1;
        }
        c.mHandler.startQuery(token, cw, contactRef, null, null, null, null);
        return c;
    }

    public void addQueryListener(int token, OnQueryCompleteListener listener, Object cookie) {
        CookieWrapper cw = new CookieWrapper();
        cw.listener = listener;
        cw.cookie = cookie;
        cw.event = 2;
        this.mHandler.startQuery(token, cw, null, null, null, null, null);
    }

    private void allocate(Context context, Uri contactRef) {
        if (context == null || contactRef == null) {
            throw new QueryPoolException("Bad context or query uri.");
        }
        this.mHandler = new CallerInfoAsyncQueryHandler(context);
        this.mHandler.mQueryUri = contactRef;
    }

    private void release() {
        this.mHandler.mContext = null;
        this.mHandler.mQueryUri = null;
        this.mHandler.mCallerInfo = null;
        this.mHandler = null;
    }

    private static String sanitizeUriToString(Uri uri) {
        if (uri == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        String uriString = uri.toString();
        int indexOfLastSlash = uriString.lastIndexOf(47);
        if (indexOfLastSlash > 0) {
            return uriString.substring(0, indexOfLastSlash) + "/xxxxxxx";
        }
        return uriString;
    }
}
