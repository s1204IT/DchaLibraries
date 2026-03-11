package com.android.settings.notification;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationStation extends SettingsPreferenceFragment {
    private static final String TAG = NotificationStation.class.getSimpleName();
    private Context mContext;
    private Handler mHandler;
    private INotificationManager mNoMan;
    private PackageManager mPm;
    private NotificationListenerService.RankingMap mRanking;
    private Runnable mRefreshListRunnable = new Runnable() {
        @Override
        public void run() {
            NotificationStation.this.refreshList();
        }
    };
    private final NotificationListenerService mListener = new NotificationListenerService() {
        @Override
        public void onNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
            Object[] objArr = new Object[2];
            objArr[0] = statusBarNotification.getNotification();
            objArr[1] = Integer.valueOf(rankingMap != null ? rankingMap.getOrderedKeys().length : 0);
            NotificationStation.logd("onNotificationPosted: %s, with update for %d", objArr);
            NotificationStation.this.mRanking = rankingMap;
            NotificationStation.this.scheduleRefreshList();
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
            Object[] objArr = new Object[1];
            objArr[0] = Integer.valueOf(rankingMap == null ? 0 : rankingMap.getOrderedKeys().length);
            NotificationStation.logd("onNotificationRankingUpdate with update for %d", objArr);
            NotificationStation.this.mRanking = rankingMap;
            NotificationStation.this.scheduleRefreshList();
        }

        @Override
        public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
            Object[] objArr = new Object[1];
            objArr[0] = Integer.valueOf(rankingMap == null ? 0 : rankingMap.getOrderedKeys().length);
            NotificationStation.logd("onNotificationRankingUpdate with update for %d", objArr);
            NotificationStation.this.mRanking = rankingMap;
            NotificationStation.this.scheduleRefreshList();
        }

        @Override
        public void onListenerConnected() {
            NotificationStation.this.mRanking = getCurrentRanking();
            Object[] objArr = new Object[1];
            objArr[0] = Integer.valueOf(NotificationStation.this.mRanking == null ? 0 : NotificationStation.this.mRanking.getOrderedKeys().length);
            NotificationStation.logd("onListenerConnected with update for %d", objArr);
            NotificationStation.this.scheduleRefreshList();
        }
    };
    private final Comparator<HistoricalNotificationInfo> mNotificationSorter = new Comparator<HistoricalNotificationInfo>() {
        @Override
        public int compare(HistoricalNotificationInfo historicalNotificationInfo, HistoricalNotificationInfo historicalNotificationInfo2) {
            return Long.compare(historicalNotificationInfo2.timestamp, historicalNotificationInfo.timestamp);
        }
    };

    private static class HistoricalNotificationInfo {
        public boolean active;
        public String channel;
        public CharSequence extra;
        public Drawable icon;
        public String key;
        public String pkg;
        public Drawable pkgicon;
        public CharSequence pkgname;
        public int priority;
        public long timestamp;
        public CharSequence title;
        public int user;

        private HistoricalNotificationInfo() {
        }
    }

    public void scheduleRefreshList() {
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(this.mRefreshListRunnable);
            this.mHandler.postDelayed(this.mRefreshListRunnable, 100L);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        this.mHandler = new Handler(activity.getMainLooper());
        this.mContext = activity;
        this.mPm = this.mContext.getPackageManager();
        this.mNoMan = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
    }

    @Override
    public void onDetach() {
        logd("onDetach()", new Object[0]);
        this.mHandler.removeCallbacks(this.mRefreshListRunnable);
        this.mHandler = null;
        super.onDetach();
    }

    @Override
    public void onPause() {
        try {
            this.mListener.unregisterAsSystemService();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot unregister listener", e);
        }
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return 75;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        logd("onActivityCreated(%s)", bundle);
        super.onActivityCreated(bundle);
        Utils.forceCustomPadding(getListView(), false);
    }

    @Override
    public void onResume() {
        logd("onResume()", new Object[0]);
        super.onResume();
        try {
            this.mListener.registerAsSystemService(this.mContext, new ComponentName(this.mContext.getPackageName(), getClass().getCanonicalName()), ActivityManager.getCurrentUser());
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot register listener", e);
        }
        refreshList();
    }

    public void refreshList() {
        List<HistoricalNotificationInfo> listLoadNotifications = loadNotifications();
        if (listLoadNotifications != null) {
            int size = listLoadNotifications.size();
            logd("adding %d infos", Integer.valueOf(size));
            Collections.sort(listLoadNotifications, this.mNotificationSorter);
            if (getPreferenceScreen() == null) {
                setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
            }
            getPreferenceScreen().removeAll();
            for (int i = 0; i < size; i++) {
                getPreferenceScreen().addPreference(new HistoricalNotificationPreference(getPrefContext(), listLoadNotifications.get(i)));
            }
        }
    }

    public static void logd(String str, Object... objArr) {
        String str2 = TAG;
        if (objArr != null && objArr.length != 0) {
            str = String.format(str, objArr);
        }
        Log.d(str2, str);
    }

    private static CharSequence bold(CharSequence charSequence) {
        if (charSequence.length() == 0) {
            return charSequence;
        }
        SpannableString spannableString = new SpannableString(charSequence);
        spannableString.setSpan(new StyleSpan(1), 0, charSequence.length(), 0);
        return spannableString;
    }

    private static String getTitleString(Notification notification) {
        CharSequence charSequence;
        if (notification.extras != null) {
            charSequence = notification.extras.getCharSequence("android.title");
            if (TextUtils.isEmpty(charSequence)) {
                charSequence = notification.extras.getCharSequence("android.text");
            }
        } else {
            charSequence = null;
        }
        if (TextUtils.isEmpty(charSequence) && !TextUtils.isEmpty(notification.tickerText)) {
            charSequence = notification.tickerText;
        }
        return String.valueOf(charSequence);
    }

    private static String formatPendingIntent(PendingIntent pendingIntent) {
        StringBuilder sb = new StringBuilder();
        IntentSender intentSender = pendingIntent.getIntentSender();
        sb.append("Intent(pkg=");
        sb.append(intentSender.getCreatorPackage());
        try {
            if (ActivityManager.getService().isIntentSenderAnActivity(intentSender.getTarget())) {
                sb.append(" (activity)");
            }
        } catch (RemoteException e) {
        }
        sb.append(")");
        return sb.toString();
    }

    private List<HistoricalNotificationInfo> loadNotifications() {
        int i;
        int i2;
        char c;
        int currentUser = ActivityManager.getCurrentUser();
        AnonymousClass1 anonymousClass1 = null;
        try {
            StatusBarNotification[] activeNotifications = this.mNoMan.getActiveNotifications(this.mContext.getPackageName());
            StatusBarNotification[] historicalNotifications = this.mNoMan.getHistoricalNotifications(this.mContext.getPackageName(), 50);
            ArrayList arrayList = new ArrayList(activeNotifications.length + historicalNotifications.length);
            char c2 = 2;
            int i3 = 0;
            int i4 = 1;
            StatusBarNotification[][] statusBarNotificationArr = {activeNotifications, historicalNotifications};
            int length = statusBarNotificationArr.length;
            int i5 = 0;
            while (i5 < length) {
                StatusBarNotification[] statusBarNotificationArr2 = statusBarNotificationArr[i5];
                int length2 = statusBarNotificationArr2.length;
                int i6 = i3;
                while (i6 < length2) {
                    StatusBarNotification statusBarNotification = statusBarNotificationArr2[i6];
                    if (((statusBarNotification.getUserId() != -1 ? i4 : i3) & (statusBarNotification.getUserId() != currentUser ? i4 : i3)) != 0) {
                        i2 = i4;
                        c = 2;
                        i = i3;
                    } else {
                        Notification notification = statusBarNotification.getNotification();
                        HistoricalNotificationInfo historicalNotificationInfo = new HistoricalNotificationInfo();
                        historicalNotificationInfo.pkg = statusBarNotification.getPackageName();
                        historicalNotificationInfo.user = statusBarNotification.getUserId();
                        historicalNotificationInfo.icon = loadIconDrawable(historicalNotificationInfo.pkg, historicalNotificationInfo.user, notification.icon);
                        historicalNotificationInfo.pkgicon = loadPackageIconDrawable(historicalNotificationInfo.pkg, historicalNotificationInfo.user);
                        historicalNotificationInfo.pkgname = loadPackageName(historicalNotificationInfo.pkg);
                        historicalNotificationInfo.title = getTitleString(notification);
                        if (TextUtils.isEmpty(historicalNotificationInfo.title)) {
                            historicalNotificationInfo.title = getString(R.string.notification_log_no_title);
                        }
                        historicalNotificationInfo.timestamp = statusBarNotification.getPostTime();
                        historicalNotificationInfo.priority = notification.priority;
                        historicalNotificationInfo.channel = notification.getChannelId();
                        historicalNotificationInfo.key = statusBarNotification.getKey();
                        historicalNotificationInfo.active = statusBarNotificationArr2 == activeNotifications;
                        historicalNotificationInfo.extra = generateExtraText(statusBarNotification, historicalNotificationInfo);
                        i = 0;
                        i2 = 1;
                        c = 2;
                        logd("   [%d] %s: %s", Long.valueOf(historicalNotificationInfo.timestamp), historicalNotificationInfo.pkg, historicalNotificationInfo.title);
                        arrayList.add(historicalNotificationInfo);
                    }
                    i6++;
                    i3 = i;
                    i4 = i2;
                    c2 = c;
                    anonymousClass1 = null;
                }
                i5++;
                i4 = i4;
                anonymousClass1 = null;
            }
            return arrayList;
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot load Notifications: ", e);
            return null;
        }
    }

    private CharSequence generateExtraText(StatusBarNotification statusBarNotification, HistoricalNotificationInfo historicalNotificationInfo) {
        NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
        Notification notification = statusBarNotification.getNotification();
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String string = getString(R.string.notification_log_details_delimiter);
        spannableStringBuilder.append(bold(getString(R.string.notification_log_details_package))).append((CharSequence) string).append((CharSequence) historicalNotificationInfo.pkg).append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_key))).append((CharSequence) string).append((CharSequence) statusBarNotification.getKey());
        spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_icon))).append((CharSequence) string).append((CharSequence) String.valueOf(notification.getSmallIcon()));
        spannableStringBuilder.append((CharSequence) "\n").append(bold("channelId")).append((CharSequence) string).append((CharSequence) String.valueOf(notification.getChannelId()));
        spannableStringBuilder.append((CharSequence) "\n").append(bold("postTime")).append((CharSequence) string).append((CharSequence) String.valueOf(statusBarNotification.getPostTime()));
        if (notification.getTimeoutAfter() != 0) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold("timeoutAfter")).append((CharSequence) string).append((CharSequence) String.valueOf(notification.getTimeoutAfter()));
        }
        if (statusBarNotification.isGroup()) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_group))).append((CharSequence) string).append((CharSequence) String.valueOf(statusBarNotification.getGroupKey()));
            if (notification.isGroupSummary()) {
                spannableStringBuilder.append(bold(getString(R.string.notification_log_details_group_summary)));
            }
        }
        spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_sound))).append((CharSequence) string);
        if ((notification.defaults & 1) != 0) {
            spannableStringBuilder.append((CharSequence) getString(R.string.notification_log_details_default));
        } else if (notification.sound != null) {
            spannableStringBuilder.append((CharSequence) notification.sound.toString());
        } else {
            spannableStringBuilder.append((CharSequence) getString(R.string.notification_log_details_none));
        }
        spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_vibrate))).append((CharSequence) string);
        if ((notification.defaults & 2) != 0) {
            spannableStringBuilder.append((CharSequence) getString(R.string.notification_log_details_default));
        } else if (notification.vibrate != null) {
            for (int i = 0; i < notification.vibrate.length; i++) {
                if (i > 0) {
                    spannableStringBuilder.append(',');
                }
                spannableStringBuilder.append((CharSequence) String.valueOf(notification.vibrate[i]));
            }
        } else {
            spannableStringBuilder.append((CharSequence) getString(R.string.notification_log_details_none));
        }
        spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_visibility))).append((CharSequence) string).append((CharSequence) Notification.visibilityToString(notification.visibility));
        if (notification.publicVersion != null) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_public_version))).append((CharSequence) string).append((CharSequence) getTitleString(notification.publicVersion));
        }
        spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_priority))).append((CharSequence) string).append((CharSequence) Notification.priorityToString(notification.priority));
        if (historicalNotificationInfo.active) {
            if (this.mRanking != null && this.mRanking.getRanking(statusBarNotification.getKey(), ranking)) {
                spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_importance))).append((CharSequence) string).append((CharSequence) NotificationListenerService.Ranking.importanceToString(ranking.getImportance()));
                if (ranking.getImportanceExplanation() != null) {
                    spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_explanation))).append((CharSequence) string).append(ranking.getImportanceExplanation());
                }
                spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_badge))).append((CharSequence) string).append((CharSequence) Boolean.toString(ranking.canShowBadge()));
            } else if (this.mRanking == null) {
                spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_ranking_null)));
            } else {
                spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_ranking_none)));
            }
        }
        if (notification.contentIntent != null) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_content_intent))).append((CharSequence) string).append((CharSequence) formatPendingIntent(notification.contentIntent));
        }
        if (notification.deleteIntent != null) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_delete_intent))).append((CharSequence) string).append((CharSequence) formatPendingIntent(notification.deleteIntent));
        }
        if (notification.fullScreenIntent != null) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_full_screen_intent))).append((CharSequence) string).append((CharSequence) formatPendingIntent(notification.fullScreenIntent));
        }
        if (notification.actions != null && notification.actions.length > 0) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_actions)));
            for (int i2 = 0; i2 < notification.actions.length; i2++) {
                Notification.Action action = notification.actions[i2];
                spannableStringBuilder.append((CharSequence) "\n  ").append((CharSequence) String.valueOf(i2)).append(' ').append(bold(getString(R.string.notification_log_details_title))).append((CharSequence) string).append(action.title);
                if (action.actionIntent != null) {
                    spannableStringBuilder.append((CharSequence) "\n    ").append(bold(getString(R.string.notification_log_details_content_intent))).append((CharSequence) string).append((CharSequence) formatPendingIntent(action.actionIntent));
                }
                if (action.getRemoteInputs() != null) {
                    spannableStringBuilder.append((CharSequence) "\n    ").append(bold(getString(R.string.notification_log_details_remoteinput))).append((CharSequence) string).append((CharSequence) String.valueOf(action.getRemoteInputs().length));
                }
            }
        }
        if (notification.contentView != null) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_content_view))).append((CharSequence) string).append((CharSequence) notification.contentView.toString());
        }
        if (notification.extras != null && notification.extras.size() > 0) {
            spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_extras)));
            for (String str : notification.extras.keySet()) {
                String strValueOf = String.valueOf(notification.extras.get(str));
                if (strValueOf.length() > 100) {
                    strValueOf = strValueOf.substring(0, 100) + "...";
                }
                spannableStringBuilder.append((CharSequence) "\n  ").append((CharSequence) str).append((CharSequence) string).append((CharSequence) strValueOf);
            }
        }
        Parcel parcelObtain = Parcel.obtain();
        notification.writeToParcel(parcelObtain, 0);
        spannableStringBuilder.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_parcel))).append((CharSequence) string).append((CharSequence) String.valueOf(parcelObtain.dataPosition())).append(' ').append(bold(getString(R.string.notification_log_details_ashmem))).append((CharSequence) string).append((CharSequence) String.valueOf(parcelObtain.getBlobAshmemSize())).append((CharSequence) "\n");
        return spannableStringBuilder;
    }

    private Resources getResourcesForUserPackage(String str, int i) {
        if (str != null) {
            if (i == -1) {
                i = 0;
            }
            try {
                return this.mPm.getResourcesForApplicationAsUser(str, i);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Icon package not found: " + str, e);
                return null;
            }
        }
        return this.mContext.getResources();
    }

    private Drawable loadPackageIconDrawable(String str, int i) {
        try {
            return this.mPm.getApplicationIcon(str);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot get application icon", e);
            return null;
        }
    }

    private CharSequence loadPackageName(String str) {
        try {
            ApplicationInfo applicationInfo = this.mPm.getApplicationInfo(str, 4194304);
            if (applicationInfo != null) {
                return this.mPm.getApplicationLabel(applicationInfo);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot load package name", e);
        }
        return str;
    }

    private Drawable loadIconDrawable(String str, int i, int i2) {
        Resources resourcesForUserPackage = getResourcesForUserPackage(str, i);
        if (i2 == 0) {
            return null;
        }
        try {
            return resourcesForUserPackage.getDrawable(i2, null);
        } catch (RuntimeException e) {
            String str2 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Icon not found in ");
            sb.append(str != null ? Integer.valueOf(i2) : "<system>");
            sb.append(": ");
            sb.append(Integer.toHexString(i2));
            Log.w(str2, sb.toString(), e);
            return null;
        }
    }

    private static class HistoricalNotificationPreference extends Preference {
        private static long sLastExpandedTimestamp;
        private final HistoricalNotificationInfo mInfo;

        public HistoricalNotificationPreference(Context context, HistoricalNotificationInfo historicalNotificationInfo) {
            super(context);
            setLayoutResource(R.layout.notification_log_row);
            this.mInfo = historicalNotificationInfo;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
            super.onBindViewHolder(preferenceViewHolder);
            if (this.mInfo.icon != null) {
                ((ImageView) preferenceViewHolder.findViewById(R.id.icon)).setImageDrawable(this.mInfo.icon);
            }
            if (this.mInfo.pkgicon != null) {
                ((ImageView) preferenceViewHolder.findViewById(R.id.pkgicon)).setImageDrawable(this.mInfo.pkgicon);
            }
            preferenceViewHolder.findViewById(R.id.timestamp).setTime(this.mInfo.timestamp);
            ((TextView) preferenceViewHolder.findViewById(R.id.title)).setText(this.mInfo.title);
            ((TextView) preferenceViewHolder.findViewById(R.id.pkgname)).setText(this.mInfo.pkgname);
            final TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.extra);
            textView.setText(this.mInfo.extra);
            textView.setVisibility(this.mInfo.timestamp == sLastExpandedTimestamp ? 0 : 8);
            preferenceViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    textView.setVisibility(textView.getVisibility() == 0 ? 8 : 0);
                    long unused = HistoricalNotificationPreference.sLastExpandedTimestamp = HistoricalNotificationPreference.this.mInfo.timestamp;
                }
            });
            preferenceViewHolder.itemView.setAlpha(this.mInfo.active ? 1.0f : 0.5f);
        }

        @Override
        public void performClick() {
        }
    }
}
