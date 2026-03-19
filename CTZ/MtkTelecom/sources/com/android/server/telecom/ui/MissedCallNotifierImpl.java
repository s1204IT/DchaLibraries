package com.android.server.telecom.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.AsyncQueryHandler;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.UserHandle;
import android.provider.CallLog;
import android.telecom.Log;
import android.telecom.Logging.Runnable;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.internal.telephony.CallerInfo;
import com.android.server.telecom.CallerInfoLookupHelper;
import com.android.server.telecom.CallsManagerListenerBase;
import com.android.server.telecom.DefaultDialerCache;
import com.android.server.telecom.MissedCallNotifier;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.R;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.components.TelecomBroadcastReceiver;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MissedCallNotifierImpl extends CallsManagerListenerBase implements MissedCallNotifier {
    private static final String[] CALL_LOG_PROJECTION = {"_id", "number", "presentation", "date", "duration", "type"};
    private static final String NOTIFICATION_TAG = MissedCallNotifierImpl.class.getSimpleName();
    private final Context mContext;
    private UserHandle mCurrentUserHandle;
    private final DefaultDialerCache mDefaultDialerCache;
    private ConcurrentMap<UserHandle, AtomicInteger> mMissedCallCounts;
    private final NotificationBuilderFactory mNotificationBuilderFactory;
    private final NotificationManager mNotificationManager;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private List<UserHandle> mUsersToLoadAfterBootComplete;

    public interface MissedCallNotifierImplFactory {
        MissedCallNotifier makeMissedCallNotifierImpl(Context context, PhoneAccountRegistrar phoneAccountRegistrar, DefaultDialerCache defaultDialerCache);
    }

    public interface NotificationBuilderFactory {
        Notification.Builder getBuilder(Context context);
    }

    private static class DefaultNotificationBuilderFactory implements NotificationBuilderFactory {
        @Override
        public Notification.Builder getBuilder(Context context) {
            return new Notification.Builder(context);
        }
    }

    public MissedCallNotifierImpl(Context context, PhoneAccountRegistrar phoneAccountRegistrar, DefaultDialerCache defaultDialerCache) {
        this(context, phoneAccountRegistrar, defaultDialerCache, new DefaultNotificationBuilderFactory());
    }

    public MissedCallNotifierImpl(Context context, PhoneAccountRegistrar phoneAccountRegistrar, DefaultDialerCache defaultDialerCache, NotificationBuilderFactory notificationBuilderFactory) {
        this.mUsersToLoadAfterBootComplete = new ArrayList();
        this.mContext = context;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mDefaultDialerCache = defaultDialerCache;
        this.mNotificationBuilderFactory = notificationBuilderFactory;
        this.mMissedCallCounts = new ConcurrentHashMap();
    }

    @Override
    public void clearMissedCalls(UserHandle userHandle) {
        if (!shouldManageNotificationThroughDefaultDialer(userHandle)) {
            markMissedCallsAsRead(userHandle);
        }
        cancelMissedCallNotification(userHandle);
    }

    private void markMissedCallsAsRead(final UserHandle userHandle) {
        AsyncTask.execute(new Runnable("MCNI.mMCAR", null) {
            public void loggedRun() {
                ContentValues contentValues = new ContentValues();
                contentValues.put("new", (Integer) 0);
                contentValues.put("is_read", (Integer) 1);
                try {
                    MissedCallNotifierImpl.this.mContext.getContentResolver().update(ContentProvider.maybeAddUserId(CallLog.Calls.CONTENT_URI, userHandle.getIdentifier()), contentValues, "new = 1 AND type = ?", new String[]{Integer.toString(3)});
                } catch (IllegalArgumentException e) {
                    Log.w(this, "ContactsProvider update command failed", new Object[]{e});
                }
            }
        }.prepare());
    }

    private Intent getShowMissedCallIntentForDefaultDialer(UserHandle userHandle) {
        String defaultDialerApplication = this.mDefaultDialerCache.getDefaultDialerApplication(userHandle.getIdentifier());
        if (TextUtils.isEmpty(defaultDialerApplication)) {
            return null;
        }
        return new Intent("android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION").setPackage(defaultDialerApplication);
    }

    private boolean shouldManageNotificationThroughDefaultDialer(UserHandle userHandle) {
        Intent showMissedCallIntentForDefaultDialer = getShowMissedCallIntentForDefaultDialer(userHandle);
        return showMissedCallIntentForDefaultDialer != null && this.mContext.getPackageManager().queryBroadcastReceiversAsUser(showMissedCallIntentForDefaultDialer, 0, userHandle.getIdentifier()).size() > 0;
    }

    private void sendNotificationThroughDefaultDialer(MissedCallNotifier.CallInfo callInfo, UserHandle userHandle) {
        String phoneNumber;
        int i = this.mMissedCallCounts.get(userHandle).get();
        Intent intentPutExtra = getShowMissedCallIntentForDefaultDialer(userHandle).setFlags(268435456).putExtra("android.telecom.extra.CLEAR_MISSED_CALLS_INTENT", createClearMissedCallsPendingIntent(userHandle)).putExtra("android.telecom.extra.NOTIFICATION_COUNT", i);
        if (callInfo == null) {
            phoneNumber = null;
        } else {
            phoneNumber = callInfo.getPhoneNumber();
        }
        Intent intentPutExtra2 = intentPutExtra.putExtra("android.telecom.extra.NOTIFICATION_PHONE_NUMBER", phoneNumber);
        if (i == 1 && callInfo != null) {
            Uri handle = callInfo.getHandle();
            String schemeSpecificPart = handle != null ? handle.getSchemeSpecificPart() : null;
            if (!TextUtils.isEmpty(schemeSpecificPart) && !TextUtils.equals(schemeSpecificPart, this.mContext.getString(R.string.handle_restricted))) {
                intentPutExtra2.putExtra("android.telecom.extra.CALL_BACK_INTENT", createCallBackPendingIntent(handle, userHandle));
            }
        }
        Log.w(this, "Showing missed calls through default dialer.", new Object[0]);
        this.mContext.sendBroadcastAsUser(intentPutExtra2, userHandle, "android.permission.READ_PHONE_STATE");
    }

    @Override
    public void showMissedCallNotification(MissedCallNotifier.CallInfo callInfo) {
        UserHandle userHandle;
        PhoneAccountHandle phoneAccountHandle = callInfo.getPhoneAccountHandle();
        PhoneAccount phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle);
        if (phoneAccountUnchecked != null && phoneAccountUnchecked.hasCapabilities(32)) {
            userHandle = this.mCurrentUserHandle;
        } else {
            userHandle = phoneAccountHandle.getUserHandle();
        }
        showMissedCallNotification(callInfo, userHandle);
    }

    private void showMissedCallNotification(MissedCallNotifier.CallInfo callInfo, UserHandle userHandle) {
        int i;
        String string;
        Bitmap bitmap;
        Log.i(this, "showMissedCallNotification: userHandle=%d", new Object[]{Integer.valueOf(userHandle.getIdentifier())});
        this.mMissedCallCounts.putIfAbsent(userHandle, new AtomicInteger(0));
        int iIncrementAndGet = this.mMissedCallCounts.get(userHandle).incrementAndGet();
        if (shouldManageNotificationThroughDefaultDialer(userHandle)) {
            sendNotificationThroughDefaultDialer(callInfo, userHandle);
            return;
        }
        if (iIncrementAndGet == 1) {
            string = getNameForMissedCallNotification(callInfo);
            CallerInfo callerInfo = callInfo.getCallerInfo();
            if (callerInfo != null && callerInfo.userType == 1) {
                i = R.string.notification_missedWorkCallTitle;
            } else {
                i = R.string.notification_missedCallTitle;
            }
        } else {
            i = R.string.notification_missedCallsTitle;
            string = this.mContext.getString(R.string.notification_missedCallsMsg, Integer.valueOf(iIncrementAndGet));
        }
        Context contextForUser = getContextForUser(userHandle);
        Notification.Builder builder = this.mNotificationBuilderFactory.getBuilder(contextForUser);
        builder.setSmallIcon(android.R.drawable.stat_notify_missed_call).setColor(this.mContext.getResources().getColor(R.color.theme_color)).setWhen(callInfo.getCreationTimeMillis()).setShowWhen(true).setContentTitle(this.mContext.getText(R.string.userCallActivityLabel)).setContentText(this.mContext.getText(i)).setContentIntent(createCallLogPendingIntent(userHandle)).setAutoCancel(true).setDeleteIntent(createClearMissedCallsPendingIntent(userHandle));
        Notification.Builder builder2 = this.mNotificationBuilderFactory.getBuilder(contextForUser);
        builder2.setSmallIcon(android.R.drawable.stat_notify_missed_call).setColor(this.mContext.getResources().getColor(R.color.theme_color)).setWhen(callInfo.getCreationTimeMillis()).setShowWhen(true).setContentTitle(this.mContext.getText(i)).setContentText(string).setContentIntent(createCallLogPendingIntent(userHandle)).setAutoCancel(true).setDeleteIntent(createClearMissedCallsPendingIntent(userHandle)).setPublicVersion(builder.build()).setChannelId("TelecomMissedCalls");
        Uri handle = callInfo.getHandle();
        String handleSchemeSpecificPart = callInfo.getHandleSchemeSpecificPart();
        if (iIncrementAndGet == 1) {
            Log.d(this, "Add actions with number %s.", new Object[]{Log.piiHandle(handleSchemeSpecificPart)});
            if (!TextUtils.isEmpty(handleSchemeSpecificPart) && !TextUtils.equals(handleSchemeSpecificPart, this.mContext.getString(R.string.handle_restricted))) {
                builder2.addAction(R.drawable.ic_phone_24dp, this.mContext.getString(R.string.notification_missedCall_call_back), createCallBackPendingIntent(handle, userHandle));
                if (canRespondViaSms(callInfo)) {
                    builder2.addAction(R.drawable.ic_message_24dp, this.mContext.getString(R.string.notification_missedCall_message), createSendSmsFromNotificationPendingIntent(handle, userHandle));
                }
            }
            if (callInfo.getCallerInfo() != null) {
                bitmap = callInfo.getCallerInfo().cachedPhotoIcon;
            } else {
                bitmap = null;
            }
            if (bitmap != null) {
                builder2.setLargeIcon(bitmap);
            } else {
                Drawable drawable = callInfo.getCallerInfo() != null ? callInfo.getCallerInfo().cachedPhoto : null;
                if (drawable != null && (drawable instanceof BitmapDrawable)) {
                    builder2.setLargeIcon(((BitmapDrawable) drawable).getBitmap());
                }
            }
        } else {
            Log.d(this, "Suppress actions. handle: %s, missedCalls: %d.", new Object[]{Log.piiHandle(handleSchemeSpecificPart), Integer.valueOf(iIncrementAndGet)});
        }
        Notification notificationBuild = builder2.build();
        configureLedOnNotification(notificationBuild);
        Log.i(this, "Adding missed call notification for %s.", new Object[]{Log.pii(callInfo.getHandle())});
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationManager.notifyAsUser(NOTIFICATION_TAG, 1, notificationBuild, userHandle);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void cancelMissedCallNotification(UserHandle userHandle) {
        this.mMissedCallCounts.putIfAbsent(userHandle, new AtomicInteger(0));
        this.mMissedCallCounts.get(userHandle).set(0);
        if (shouldManageNotificationThroughDefaultDialer(userHandle)) {
            sendNotificationThroughDefaultDialer(null, userHandle);
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationManager.cancelAsUser(NOTIFICATION_TAG, 1, userHandle);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private String getNameForMissedCallNotification(MissedCallNotifier.CallInfo callInfo) {
        String handleSchemeSpecificPart = callInfo.getHandleSchemeSpecificPart();
        String name = callInfo.getName();
        if (!TextUtils.isEmpty(handleSchemeSpecificPart)) {
            String number = PhoneNumberUtils.formatNumber(handleSchemeSpecificPart, getCurrentCountryIso(this.mContext));
            if (!TextUtils.isEmpty(number)) {
                handleSchemeSpecificPart = number;
            }
        }
        if (!TextUtils.isEmpty(name) && TextUtils.isGraphic(name)) {
            return name;
        }
        if (!TextUtils.isEmpty(handleSchemeSpecificPart)) {
            return BidiFormatter.getInstance().unicodeWrap(handleSchemeSpecificPart, TextDirectionHeuristics.LTR);
        }
        return this.mContext.getString(R.string.unknown);
    }

    private String getCurrentCountryIso(Context context) {
        String upperCase = ((TelephonyManager) context.getSystemService("phone")).getNetworkCountryIso().toUpperCase();
        if (upperCase == null) {
            String country = Locale.getDefault().getCountry();
            Log.w(this, "No CountryDetector; falling back to countryIso based on locale: " + country, new Object[0]);
            return country;
        }
        return upperCase;
    }

    private PendingIntent createCallLogPendingIntent(UserHandle userHandle) {
        Intent intent = new Intent("android.intent.action.VIEW", (Uri) null);
        intent.setType("vnd.android.cursor.dir/calls");
        TaskStackBuilder taskStackBuilderCreate = TaskStackBuilder.create(this.mContext);
        taskStackBuilderCreate.addNextIntent(intent);
        return taskStackBuilderCreate.getPendingIntent(0, 0, null, userHandle);
    }

    private PendingIntent createClearMissedCallsPendingIntent(UserHandle userHandle) {
        return createTelecomPendingIntent("com.android.server.telecom.ACTION_CLEAR_MISSED_CALLS", null, userHandle);
    }

    private PendingIntent createCallBackPendingIntent(Uri uri, UserHandle userHandle) {
        return createTelecomPendingIntent("com.android.server.telecom.ACTION_CALL_BACK_FROM_NOTIFICATION", uri, userHandle);
    }

    private PendingIntent createSendSmsFromNotificationPendingIntent(Uri uri, UserHandle userHandle) {
        return createTelecomPendingIntent("com.android.server.telecom.ACTION_SEND_SMS_FROM_NOTIFICATION", Uri.fromParts("smsto", uri.getSchemeSpecificPart(), null), userHandle);
    }

    private PendingIntent createTelecomPendingIntent(String str, Uri uri, UserHandle userHandle) {
        Intent intent = new Intent(str, uri, this.mContext, TelecomBroadcastReceiver.class);
        intent.putExtra("userhandle", userHandle);
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
    }

    private void configureLedOnNotification(Notification notification) {
        notification.flags |= 1;
        notification.defaults |= 4;
    }

    private boolean canRespondViaSms(MissedCallNotifier.CallInfo callInfo) {
        return callInfo.getHandle() != null && "tel".equals(callInfo.getHandle().getScheme());
    }

    @Override
    public void reloadAfterBootComplete(CallerInfoLookupHelper callerInfoLookupHelper, MissedCallNotifier.CallInfoFactory callInfoFactory) {
        if (!this.mUsersToLoadAfterBootComplete.isEmpty()) {
            for (UserHandle userHandle : this.mUsersToLoadAfterBootComplete) {
                Log.i(this, "reloadAfterBootComplete: user=%d", new Object[]{Integer.valueOf(userHandle.getIdentifier())});
                reloadFromDatabase(callerInfoLookupHelper, callInfoFactory, userHandle);
            }
            this.mUsersToLoadAfterBootComplete.clear();
            return;
        }
        Log.i(this, "reloadAfterBootComplete: no user(s) to check; skipping reload.", new Object[0]);
    }

    @Override
    public void reloadFromDatabase(final CallerInfoLookupHelper callerInfoLookupHelper, final MissedCallNotifier.CallInfoFactory callInfoFactory, final UserHandle userHandle) {
        Log.d(this, "reloadFromDatabase: user=%d", new Object[]{Integer.valueOf(userHandle.getIdentifier())});
        if (TelecomSystem.getInstance() == null || !TelecomSystem.getInstance().isBootComplete()) {
            Log.i(this, "reloadFromDatabase: Boot not yet complete -- call log db may not be available. Deferring loading until boot complete for user %d", new Object[]{Integer.valueOf(userHandle.getIdentifier())});
            this.mUsersToLoadAfterBootComplete.add(userHandle);
        } else {
            new AsyncQueryHandler(this.mContext.getContentResolver()) {
                @Override
                protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                    Log.d(MissedCallNotifierImpl.this, "onQueryComplete()...", new Object[0]);
                    if (cursor != null) {
                        try {
                            MissedCallNotifierImpl.this.mMissedCallCounts.remove(userHandle);
                            while (cursor.moveToNext()) {
                                String string = cursor.getString(1);
                                int i2 = cursor.getInt(2);
                                final long j = cursor.getLong(3);
                                final Uri uriFromParts = null;
                                if (i2 == 1 && !TextUtils.isEmpty(string)) {
                                    uriFromParts = Uri.fromParts(PhoneNumberUtils.isUriNumber(string) ? "sip" : "tel", string, null);
                                }
                                callerInfoLookupHelper.startLookup(uriFromParts, new CallerInfoLookupHelper.OnQueryCompleteListener() {
                                    @Override
                                    public void onCallerInfoQueryComplete(Uri uri, CallerInfo callerInfo) {
                                        if (!Objects.equals(uri, uriFromParts)) {
                                            Log.w(MissedCallNotifierImpl.this, "CallerInfo query returned with different handle.", new Object[0]);
                                        } else if (callerInfo == null || callerInfo.contactDisplayPhotoUri == null) {
                                            MissedCallNotifierImpl.this.showMissedCallNotification(callInfoFactory.makeCallInfo(callerInfo, null, uriFromParts, j), userHandle);
                                        }
                                    }

                                    @Override
                                    public void onContactPhotoQueryComplete(Uri uri, CallerInfo callerInfo) {
                                        if (!Objects.equals(uri, uriFromParts)) {
                                            Log.w(MissedCallNotifierImpl.this, "CallerInfo query for photo returned with different handle.", new Object[0]);
                                        } else {
                                            MissedCallNotifierImpl.this.showMissedCallNotification(callInfoFactory.makeCallInfo(callerInfo, null, uriFromParts, j), userHandle);
                                        }
                                    }
                                });
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                }
            }.startQuery(0, null, ContentProvider.maybeAddUserId(CallLog.Calls.CONTENT_URI, userHandle.getIdentifier()), CALL_LOG_PROJECTION, "type=3 AND new=1 AND is_read=0", null, "date DESC");
        }
    }

    @Override
    public void setCurrentUserHandle(UserHandle userHandle) {
        this.mCurrentUserHandle = userHandle;
    }

    private Context getContextForUser(UserHandle userHandle) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            return this.mContext;
        }
    }
}
