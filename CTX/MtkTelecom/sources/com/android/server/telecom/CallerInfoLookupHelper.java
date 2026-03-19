package com.android.server.telecom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Log;
import android.telecom.Logging.Runnable;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.server.telecom.ContactsAsyncHelper;
import com.android.server.telecom.TelecomSystem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CallerInfoLookupHelper {
    private final CallerInfoAsyncQueryFactory mCallerInfoAsyncQueryFactory;
    private final ContactsAsyncHelper mContactsAsyncHelper;
    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private final Map<Uri, CallerInfoQueryInfo> mQueryEntries = new HashMap();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private static class CallerInfoQueryInfo {
        public CallerInfo callerInfo;
        public boolean imageQueryPending = false;
        public List<OnQueryCompleteListener> listeners = new LinkedList();
    }

    public interface OnQueryCompleteListener {
        void onCallerInfoQueryComplete(Uri uri, CallerInfo callerInfo);

        void onContactPhotoQueryComplete(Uri uri, CallerInfo callerInfo);
    }

    public CallerInfoLookupHelper(Context context, CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory, ContactsAsyncHelper contactsAsyncHelper, TelecomSystem.SyncRoot syncRoot) {
        this.mCallerInfoAsyncQueryFactory = callerInfoAsyncQueryFactory;
        this.mContactsAsyncHelper = contactsAsyncHelper;
        this.mContext = context;
        this.mLock = syncRoot;
    }

    public void startLookup(Uri uri, OnQueryCompleteListener onQueryCompleteListener) {
        startLookup(uri, onQueryCompleteListener, -1);
    }

    public void startLookup(final Uri uri, OnQueryCompleteListener onQueryCompleteListener, final int i) {
        if (uri == null) {
            onQueryCompleteListener.onCallerInfoQueryComplete(uri, null);
            return;
        }
        final String schemeSpecificPart = uri.getSchemeSpecificPart();
        if (TextUtils.isEmpty(schemeSpecificPart)) {
            onQueryCompleteListener.onCallerInfoQueryComplete(uri, null);
            return;
        }
        synchronized (this.mLock) {
            if (this.mQueryEntries.containsKey(uri)) {
                CallerInfoQueryInfo callerInfoQueryInfo = this.mQueryEntries.get(uri);
                if (callerInfoQueryInfo.callerInfo != null) {
                    Log.i(this, "Caller info already exists for handle %s; using cached value", new Object[]{Log.piiHandle(uri)});
                    onQueryCompleteListener.onCallerInfoQueryComplete(uri, callerInfoQueryInfo.callerInfo);
                    if (!callerInfoQueryInfo.imageQueryPending && (callerInfoQueryInfo.callerInfo.cachedPhoto != null || callerInfoQueryInfo.callerInfo.cachedPhotoIcon != null)) {
                        onQueryCompleteListener.onContactPhotoQueryComplete(uri, callerInfoQueryInfo.callerInfo);
                    } else if (callerInfoQueryInfo.imageQueryPending) {
                        Log.i(this, "There is a pending photo query for handle %s. Adding to listeners for this query.", new Object[]{Log.piiHandle(uri)});
                        callerInfoQueryInfo.listeners.add(onQueryCompleteListener);
                    }
                } else {
                    Log.i(this, "There is a previously incomplete query for handle %s. Adding to listeners for this query.", new Object[]{Log.piiHandle(uri)});
                    callerInfoQueryInfo.listeners.add(onQueryCompleteListener);
                    return;
                }
            } else {
                CallerInfoQueryInfo callerInfoQueryInfo2 = new CallerInfoQueryInfo();
                callerInfoQueryInfo2.listeners.add(onQueryCompleteListener);
                this.mQueryEntries.put(uri, callerInfoQueryInfo2);
            }
            this.mHandler.post(new Runnable("CILH.sL", this.mLock) {
                public void loggedRun() {
                    Session sessionCreateSubsession = Log.createSubsession();
                    try {
                        if (CallerInfoLookupHelper.this.mCallerInfoAsyncQueryFactory.startQuery(0, CallerInfoLookupHelper.this.mContext, schemeSpecificPart, CallerInfoLookupHelper.this.makeCallerInfoQueryListener(uri), sessionCreateSubsession, i) == null) {
                            Log.w(this, "Lookup failed for %s.", new Object[]{Log.piiHandle(uri)});
                        }
                    } finally {
                        Log.cancelSubsession(sessionCreateSubsession);
                    }
                }
            }.prepare());
        }
    }

    private CallerInfoAsyncQuery.OnQueryCompleteListener makeCallerInfoQueryListener(final Uri uri) {
        return new CallerInfoAsyncQuery.OnQueryCompleteListener() {
            public final void onQueryComplete(int i, Object obj, CallerInfo callerInfo) {
                CallerInfoLookupHelper.lambda$makeCallerInfoQueryListener$0(this.f$0, uri, i, obj, callerInfo);
            }
        };
    }

    public static void lambda$makeCallerInfoQueryListener$0(CallerInfoLookupHelper callerInfoLookupHelper, Uri uri, int i, Object obj, CallerInfo callerInfo) {
        synchronized (callerInfoLookupHelper.mLock) {
            Log.continueSession((Session) obj, "CILH.oQC");
            try {
                if (callerInfoLookupHelper.mQueryEntries.containsKey(uri)) {
                    Log.i(callerInfoLookupHelper, "CI query for handle %s has completed; notifying all listeners.", new Object[]{Log.piiHandle(uri)});
                    CallerInfoQueryInfo callerInfoQueryInfo = callerInfoLookupHelper.mQueryEntries.get(uri);
                    Iterator<OnQueryCompleteListener> it = callerInfoQueryInfo.listeners.iterator();
                    while (it.hasNext()) {
                        it.next().onCallerInfoQueryComplete(uri, callerInfo);
                    }
                    if (callerInfo.contactDisplayPhotoUri == null) {
                        Log.i(callerInfoLookupHelper, "There is no photo for this contact, skipping photo query", new Object[0]);
                        callerInfoLookupHelper.mQueryEntries.remove(uri);
                    } else {
                        callerInfoQueryInfo.callerInfo = callerInfo;
                        callerInfoQueryInfo.imageQueryPending = true;
                        callerInfoLookupHelper.startPhotoLookup(uri, callerInfo.contactDisplayPhotoUri);
                    }
                } else {
                    Log.i(callerInfoLookupHelper, "CI query for handle %s has completed, but there are no listeners left.", new Object[]{Log.piiHandle(uri)});
                }
            } finally {
                Log.endSession();
            }
        }
    }

    private void startPhotoLookup(final Uri uri, final Uri uri2) {
        this.mHandler.post(new Runnable("CILH.sPL", this.mLock) {
            public void loggedRun() {
                Session sessionCreateSubsession = Log.createSubsession();
                try {
                    CallerInfoLookupHelper.this.mContactsAsyncHelper.startObtainPhotoAsync(0, CallerInfoLookupHelper.this.mContext, uri2, CallerInfoLookupHelper.this.makeContactPhotoListener(uri), sessionCreateSubsession);
                } catch (Throwable th) {
                    Log.cancelSubsession(sessionCreateSubsession);
                    throw th;
                }
            }
        }.prepare());
    }

    private ContactsAsyncHelper.OnImageLoadCompleteListener makeContactPhotoListener(final Uri uri) {
        return new ContactsAsyncHelper.OnImageLoadCompleteListener() {
            @Override
            public final void onImageLoadComplete(int i, Drawable drawable, Bitmap bitmap, Object obj) {
                CallerInfoLookupHelper.lambda$makeContactPhotoListener$1(this.f$0, uri, i, drawable, bitmap, obj);
            }
        };
    }

    public static void lambda$makeContactPhotoListener$1(CallerInfoLookupHelper callerInfoLookupHelper, Uri uri, int i, Drawable drawable, Bitmap bitmap, Object obj) {
        synchronized (callerInfoLookupHelper.mLock) {
            Log.continueSession((Session) obj, "CLIH.oILC");
            try {
                if (callerInfoLookupHelper.mQueryEntries.containsKey(uri)) {
                    CallerInfoQueryInfo callerInfoQueryInfo = callerInfoLookupHelper.mQueryEntries.get(uri);
                    if (callerInfoQueryInfo.callerInfo == null) {
                        Log.w(callerInfoLookupHelper, "Photo query finished, but the CallerInfo object previously looked up was not cached.", new Object[0]);
                        callerInfoLookupHelper.mQueryEntries.remove(uri);
                        return;
                    }
                    callerInfoQueryInfo.callerInfo.cachedPhoto = drawable;
                    callerInfoQueryInfo.callerInfo.cachedPhotoIcon = bitmap;
                    Iterator<OnQueryCompleteListener> it = callerInfoQueryInfo.listeners.iterator();
                    while (it.hasNext()) {
                        it.next().onContactPhotoQueryComplete(uri, callerInfoQueryInfo.callerInfo);
                    }
                    callerInfoLookupHelper.mQueryEntries.remove(uri);
                } else {
                    Log.i(callerInfoLookupHelper, "Photo query for handle %s has completed, but there are no listeners left.", new Object[]{Log.piiHandle(uri)});
                }
            } finally {
                Log.endSession();
            }
        }
    }

    @VisibleForTesting
    public Map<Uri, CallerInfoQueryInfo> getCallerInfoEntries() {
        return this.mQueryEntries;
    }

    @VisibleForTesting
    public Handler getHandler() {
        return this.mHandler;
    }
}
