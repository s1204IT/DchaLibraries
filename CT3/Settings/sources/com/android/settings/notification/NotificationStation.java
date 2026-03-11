package com.android.settings.notification;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
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
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.CopyablePreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
        public void onNotificationPosted(StatusBarNotification sbn, NotificationListenerService.RankingMap ranking) {
            Object[] objArr = new Object[2];
            objArr[0] = sbn.getNotification();
            objArr[1] = Integer.valueOf(ranking != null ? ranking.getOrderedKeys().length : 0);
            NotificationStation.logd("onNotificationPosted: %s, with update for %d", objArr);
            NotificationStation.this.mRanking = ranking;
            NotificationStation.this.scheduleRefreshList();
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification notification, NotificationListenerService.RankingMap ranking) {
            Object[] objArr = new Object[1];
            objArr[0] = Integer.valueOf(ranking == null ? 0 : ranking.getOrderedKeys().length);
            NotificationStation.logd("onNotificationRankingUpdate with update for %d", objArr);
            NotificationStation.this.mRanking = ranking;
            NotificationStation.this.scheduleRefreshList();
        }

        @Override
        public void onNotificationRankingUpdate(NotificationListenerService.RankingMap ranking) {
            Object[] objArr = new Object[1];
            objArr[0] = Integer.valueOf(ranking == null ? 0 : ranking.getOrderedKeys().length);
            NotificationStation.logd("onNotificationRankingUpdate with update for %d", objArr);
            NotificationStation.this.mRanking = ranking;
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
        public int compare(HistoricalNotificationInfo lhs, HistoricalNotificationInfo rhs) {
            return (int) (rhs.timestamp - lhs.timestamp);
        }
    };

    private static class HistoricalNotificationInfo {
        public boolean active;
        public CharSequence extra;
        public Drawable icon;
        public String pkg;
        public Drawable pkgicon;
        public CharSequence pkgname;
        public int priority;
        public long timestamp;
        public CharSequence title;
        public int user;

        HistoricalNotificationInfo(HistoricalNotificationInfo historicalNotificationInfo) {
            this();
        }

        private HistoricalNotificationInfo() {
        }
    }

    public void scheduleRefreshList() {
        if (this.mHandler == null) {
            return;
        }
        this.mHandler.removeCallbacks(this.mRefreshListRunnable);
        this.mHandler.postDelayed(this.mRefreshListRunnable, 100L);
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
    protected int getMetricsCategory() {
        return 75;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logd("onActivityCreated(%s)", savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        RecyclerView listView = getListView();
        Utils.forceCustomPadding(listView, false);
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
        List<HistoricalNotificationInfo> infos = loadNotifications();
        if (infos == null) {
            return;
        }
        int N = infos.size();
        logd("adding %d infos", Integer.valueOf(N));
        Collections.sort(infos, this.mNotificationSorter);
        if (getPreferenceScreen() == null) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        }
        getPreferenceScreen().removeAll();
        for (int i = 0; i < N; i++) {
            getPreferenceScreen().addPreference(new HistoricalNotificationPreference(getPrefContext(), infos.get(i)));
        }
    }

    public static void logd(String msg, Object... args) {
    }

    private static CharSequence bold(CharSequence cs) {
        if (cs.length() == 0) {
            return cs;
        }
        SpannableString ss = new SpannableString(cs);
        ss.setSpan(new StyleSpan(1), 0, cs.length(), 0);
        return ss;
    }

    private static String getTitleString(Notification n) {
        CharSequence title = null;
        if (n.extras != null) {
            title = n.extras.getCharSequence("android.title");
            if (TextUtils.isEmpty(title)) {
                title = n.extras.getCharSequence("android.text");
            }
        }
        if (TextUtils.isEmpty(title) && !TextUtils.isEmpty(n.tickerText)) {
            title = n.tickerText;
        }
        return String.valueOf(title);
    }

    private static String formatPendingIntent(PendingIntent pi) {
        StringBuilder sb = new StringBuilder();
        IntentSender is = pi.getIntentSender();
        sb.append("Intent(pkg=").append(is.getCreatorPackage());
        try {
            boolean isActivity = ActivityManagerNative.getDefault().isIntentSenderAnActivity(is.getTarget());
            if (isActivity) {
                sb.append(" (activity)");
            }
        } catch (RemoteException e) {
        }
        sb.append(")");
        return sb.toString();
    }

    private List<HistoricalNotificationInfo> loadNotifications() {
        int currentUserId = ActivityManager.getCurrentUser();
        try {
            StatusBarNotification[] active = this.mNoMan.getActiveNotifications(this.mContext.getPackageName());
            StatusBarNotification[] dismissed = this.mNoMan.getHistoricalNotifications(this.mContext.getPackageName(), 50);
            List<HistoricalNotificationInfo> list = new ArrayList<>(active.length + dismissed.length);
            NotificationListenerService.Ranking rank = new NotificationListenerService.Ranking();
            StatusBarNotification[][] statusBarNotificationArr = {active, dismissed};
            int i = 0;
            int length = statusBarNotificationArr.length;
            while (true) {
                int i2 = i;
                if (i2 >= length) {
                    return list;
                }
                StatusBarNotification[] resultset = statusBarNotificationArr[i2];
                for (StatusBarNotification sbn : resultset) {
                    if (!((sbn.getUserId() != currentUserId) & (sbn.getUserId() != -1))) {
                        Notification n = sbn.getNotification();
                        HistoricalNotificationInfo info = new HistoricalNotificationInfo(null);
                        info.pkg = sbn.getPackageName();
                        info.user = sbn.getUserId();
                        info.icon = loadIconDrawable(info.pkg, info.user, n.icon);
                        info.pkgicon = loadPackageIconDrawable(info.pkg, info.user);
                        info.pkgname = loadPackageName(info.pkg);
                        info.title = getTitleString(n);
                        if (TextUtils.isEmpty(info.title)) {
                            info.title = getString(R.string.notification_log_no_title);
                        }
                        info.timestamp = sbn.getPostTime();
                        info.priority = n.priority;
                        info.active = resultset == active;
                        SpannableStringBuilder sb = new SpannableStringBuilder();
                        String delim = getString(R.string.notification_log_details_delimiter);
                        sb.append(bold(getString(R.string.notification_log_details_package))).append((CharSequence) delim).append((CharSequence) info.pkg).append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_key))).append((CharSequence) delim).append((CharSequence) sbn.getKey());
                        sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_icon))).append((CharSequence) delim).append((CharSequence) n.getSmallIcon().toString());
                        if (sbn.isGroup()) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_group))).append((CharSequence) delim).append((CharSequence) sbn.getGroupKey());
                            if (n.isGroupSummary()) {
                                sb.append(bold(getString(R.string.notification_log_details_group_summary)));
                            }
                        }
                        sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_sound))).append((CharSequence) delim);
                        if ((n.defaults & 1) != 0) {
                            sb.append((CharSequence) getString(R.string.notification_log_details_default));
                        } else if (n.sound != null) {
                            sb.append((CharSequence) n.sound.toString());
                        } else {
                            sb.append((CharSequence) getString(R.string.notification_log_details_none));
                        }
                        sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_vibrate))).append((CharSequence) delim);
                        if ((n.defaults & 2) != 0) {
                            sb.append((CharSequence) getString(R.string.notification_log_details_default));
                        } else if (n.vibrate != null) {
                            for (int vi = 0; vi < n.vibrate.length; vi++) {
                                if (vi > 0) {
                                    sb.append(',');
                                }
                                sb.append((CharSequence) String.valueOf(n.vibrate[vi]));
                            }
                        } else {
                            sb.append((CharSequence) getString(R.string.notification_log_details_none));
                        }
                        sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_visibility))).append((CharSequence) delim).append((CharSequence) Notification.visibilityToString(n.visibility));
                        if (n.publicVersion != null) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_public_version))).append((CharSequence) delim).append((CharSequence) getTitleString(n.publicVersion));
                        }
                        sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_priority))).append((CharSequence) delim).append((CharSequence) Notification.priorityToString(n.priority));
                        if (resultset == active) {
                            if (this.mRanking != null && this.mRanking.getRanking(sbn.getKey(), rank)) {
                                sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_importance))).append((CharSequence) delim).append((CharSequence) NotificationListenerService.Ranking.importanceToString(rank.getImportance()));
                                if (rank.getImportanceExplanation() != null) {
                                    sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_explanation))).append((CharSequence) delim).append(rank.getImportanceExplanation());
                                }
                            } else if (this.mRanking == null) {
                                sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_ranking_null)));
                            } else {
                                sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_ranking_none)));
                            }
                        }
                        if (n.contentIntent != null) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_content_intent))).append((CharSequence) delim).append((CharSequence) formatPendingIntent(n.contentIntent));
                        }
                        if (n.deleteIntent != null) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_delete_intent))).append((CharSequence) delim).append((CharSequence) formatPendingIntent(n.deleteIntent));
                        }
                        if (n.fullScreenIntent != null) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_full_screen_intent))).append((CharSequence) delim).append((CharSequence) formatPendingIntent(n.fullScreenIntent));
                        }
                        if (n.actions != null && n.actions.length > 0) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_actions)));
                            for (int ai = 0; ai < n.actions.length; ai++) {
                                Notification.Action action = n.actions[ai];
                                sb.append((CharSequence) "\n  ").append((CharSequence) String.valueOf(ai)).append(' ').append(bold(getString(R.string.notification_log_details_title))).append((CharSequence) delim).append(action.title);
                                if (action.actionIntent != null) {
                                    sb.append((CharSequence) "\n    ").append(bold(getString(R.string.notification_log_details_content_intent))).append((CharSequence) delim).append((CharSequence) formatPendingIntent(action.actionIntent));
                                }
                                if (action.getRemoteInputs() != null) {
                                    sb.append((CharSequence) "\n    ").append(bold(getString(R.string.notification_log_details_remoteinput))).append((CharSequence) delim).append((CharSequence) String.valueOf(action.getRemoteInputs().length));
                                }
                            }
                        }
                        if (n.contentView != null) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_content_view))).append((CharSequence) delim).append((CharSequence) n.contentView.toString());
                        }
                        if (n.extras != null && n.extras.size() > 0) {
                            sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_extras)));
                            for (String extraKey : n.extras.keySet()) {
                                String val = String.valueOf(n.extras.get(extraKey));
                                if (val.length() > 100) {
                                    val = val.substring(0, 100) + "...";
                                }
                                sb.append((CharSequence) "\n  ").append((CharSequence) extraKey).append((CharSequence) delim).append((CharSequence) val);
                            }
                        }
                        Parcel p = Parcel.obtain();
                        n.writeToParcel(p, 0);
                        sb.append((CharSequence) "\n").append(bold(getString(R.string.notification_log_details_parcel))).append((CharSequence) delim).append((CharSequence) String.valueOf(p.dataPosition())).append(' ').append(bold(getString(R.string.notification_log_details_ashmem))).append((CharSequence) delim).append((CharSequence) String.valueOf(p.getBlobAshmemSize())).append((CharSequence) "\n");
                        info.extra = sb;
                        logd("   [%d] %s: %s", Long.valueOf(info.timestamp), info.pkg, info.title);
                        list.add(info);
                    }
                }
                i = i2 + 1;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot load Notifications: ", e);
            return null;
        }
    }

    private Resources getResourcesForUserPackage(String pkg, int userId) {
        if (pkg != null) {
            if (userId == -1) {
                userId = 0;
            }
            try {
                Resources r = this.mPm.getResourcesForApplicationAsUser(pkg, userId);
                return r;
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e(TAG, "Icon package not found: " + pkg, ex);
                return null;
            }
        }
        Resources r2 = this.mContext.getResources();
        return r2;
    }

    private Drawable loadPackageIconDrawable(String pkg, int userId) {
        try {
            Drawable icon = this.mPm.getApplicationIcon(pkg);
            return icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot get application icon", e);
            return null;
        }
    }

    private CharSequence loadPackageName(String pkg) {
        try {
            ApplicationInfo info = this.mPm.getApplicationInfo(pkg, 8192);
            if (info != null) {
                return this.mPm.getApplicationLabel(info);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot load package name", e);
        }
        return pkg;
    }

    private Drawable loadIconDrawable(String pkg, int userId, int resId) {
        Resources r = getResourcesForUserPackage(pkg, userId);
        if (resId == 0) {
            return null;
        }
        try {
            return r.getDrawable(resId, null);
        } catch (RuntimeException e) {
            Log.w(TAG, "Icon not found in " + (pkg != null ? Integer.valueOf(resId) : "<system>") + ": " + Integer.toHexString(resId), e);
            return null;
        }
    }

    private static class HistoricalNotificationPreference extends CopyablePreference {
        private final HistoricalNotificationInfo mInfo;

        public HistoricalNotificationPreference(Context context, HistoricalNotificationInfo info) {
            super(context);
            setLayoutResource(R.layout.notification_log_row);
            this.mInfo = info;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder row) {
            super.onBindViewHolder(row);
            if (this.mInfo.icon != null) {
                ((ImageView) row.findViewById(R.id.icon)).setImageDrawable(this.mInfo.icon);
            }
            if (this.mInfo.pkgicon != null) {
                ((ImageView) row.findViewById(R.id.pkgicon)).setImageDrawable(this.mInfo.pkgicon);
            }
            row.findViewById(R.id.timestamp).setTime(this.mInfo.timestamp);
            ((TextView) row.findViewById(R.id.title)).setText(this.mInfo.title);
            ((TextView) row.findViewById(R.id.pkgname)).setText(this.mInfo.pkgname);
            final TextView extra = (TextView) row.findViewById(R.id.extra);
            extra.setText(this.mInfo.extra);
            extra.setVisibility(8);
            row.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    extra.setVisibility(extra.getVisibility() == 0 ? 8 : 0);
                }
            });
            row.itemView.setAlpha(this.mInfo.active ? 1.0f : 0.5f);
        }

        @Override
        public CharSequence getCopyableText() {
            return new SpannableStringBuilder(this.mInfo.title).append((CharSequence) " [").append((CharSequence) new Date(this.mInfo.timestamp).toString()).append((CharSequence) "]\n").append(this.mInfo.pkgname).append((CharSequence) "\n").append(this.mInfo.extra);
        }

        @Override
        public void performClick() {
        }
    }
}
