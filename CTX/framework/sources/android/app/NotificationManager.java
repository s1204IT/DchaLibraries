package android.app;

import android.app.INotificationManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotificationManager {
    public static final String ACTION_APP_BLOCK_STATE_CHANGED = "android.app.action.APP_BLOCK_STATE_CHANGED";
    public static final String ACTION_EFFECTS_SUPPRESSOR_CHANGED = "android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED";
    public static final String ACTION_INTERRUPTION_FILTER_CHANGED = "android.app.action.INTERRUPTION_FILTER_CHANGED";
    public static final String ACTION_INTERRUPTION_FILTER_CHANGED_INTERNAL = "android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL";
    public static final String ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED = "android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED";
    public static final String ACTION_NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED = "android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED";
    public static final String ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED = "android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED";
    public static final String ACTION_NOTIFICATION_POLICY_CHANGED = "android.app.action.NOTIFICATION_POLICY_CHANGED";
    public static final String EXTRA_BLOCKED_STATE = "android.app.extra.BLOCKED_STATE";
    public static final String EXTRA_NOTIFICATION_CHANNEL_GROUP_ID = "android.app.extra.NOTIFICATION_CHANNEL_GROUP_ID";
    public static final String EXTRA_NOTIFICATION_CHANNEL_ID = "android.app.extra.NOTIFICATION_CHANNEL_ID";
    public static final int IMPORTANCE_DEFAULT = 3;
    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_LOW = 2;
    public static final int IMPORTANCE_MAX = 5;
    public static final int IMPORTANCE_MIN = 1;
    public static final int IMPORTANCE_NONE = 0;
    public static final int IMPORTANCE_UNSPECIFIED = -1000;
    public static final int INTERRUPTION_FILTER_ALARMS = 4;
    public static final int INTERRUPTION_FILTER_ALL = 1;
    public static final int INTERRUPTION_FILTER_NONE = 3;
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;
    public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
    public static final int VISIBILITY_NO_OVERRIDE = -1000;
    private static INotificationManager sService;
    private Context mContext;
    private static String TAG = "NotificationManager";
    private static boolean localLOGV = false;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Importance {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface InterruptionFilter {
    }

    public static INotificationManager getService() {
        if (sService != null) {
            return sService;
        }
        sService = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        return sService;
    }

    NotificationManager(Context context, Handler handler) {
        this.mContext = context;
    }

    public static NotificationManager from(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void notify(int i, Notification notification) {
        notify(null, i, notification);
    }

    public void notify(String str, int i, Notification notification) {
        notifyAsUser(str, i, notification, this.mContext.getUser());
    }

    public void notifyAsUser(String str, int i, Notification notification, UserHandle userHandle) {
        INotificationManager service = getService();
        String packageName = this.mContext.getPackageName();
        Notification.addFieldsFromContext(this.mContext, notification);
        if (notification.sound != null) {
            notification.sound = notification.sound.getCanonicalUri();
            if (StrictMode.vmFileUriExposureEnabled()) {
                notification.sound.checkFileUriExposed("Notification.sound");
            }
        }
        fixLegacySmallIcon(notification, packageName);
        if (this.mContext.getApplicationInfo().targetSdkVersion > 22 && notification.getSmallIcon() == null) {
            throw new IllegalArgumentException("Invalid notification (no valid small icon): " + notification);
        }
        if (localLOGV) {
            Log.v(TAG, packageName + ": notify(" + i + ", " + notification + ")");
        }
        notification.reduceImageSizes(this.mContext);
        try {
            service.enqueueNotificationWithTag(packageName, this.mContext.getOpPackageName(), str, i, Notification.Builder.maybeCloneStrippedForDelivery(notification, ((ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE)).isLowRamDevice(), this.mContext), userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void fixLegacySmallIcon(Notification notification, String str) {
        if (notification.getSmallIcon() == null && notification.icon != 0) {
            notification.setSmallIcon(Icon.createWithResource(str, notification.icon));
        }
    }

    public void cancel(int i) {
        cancel(null, i);
    }

    public void cancel(String str, int i) {
        cancelAsUser(str, i, this.mContext.getUser());
    }

    public void cancelAsUser(String str, int i, UserHandle userHandle) {
        INotificationManager service = getService();
        String packageName = this.mContext.getPackageName();
        if (localLOGV) {
            Log.v(TAG, packageName + ": cancel(" + i + ")");
        }
        try {
            service.cancelNotificationWithTag(packageName, str, i, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void cancelAll() {
        INotificationManager service = getService();
        String packageName = this.mContext.getPackageName();
        if (localLOGV) {
            Log.v(TAG, packageName + ": cancelAll()");
        }
        try {
            service.cancelAllNotifications(packageName, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void createNotificationChannelGroup(NotificationChannelGroup notificationChannelGroup) {
        createNotificationChannelGroups(Arrays.asList(notificationChannelGroup));
    }

    public void createNotificationChannelGroups(List<NotificationChannelGroup> list) {
        try {
            getService().createNotificationChannelGroups(this.mContext.getPackageName(), new ParceledListSlice(list));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void createNotificationChannel(NotificationChannel notificationChannel) {
        createNotificationChannels(Arrays.asList(notificationChannel));
    }

    public void createNotificationChannels(List<NotificationChannel> list) {
        try {
            getService().createNotificationChannels(this.mContext.getPackageName(), new ParceledListSlice(list));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NotificationChannel getNotificationChannel(String str) {
        try {
            return getService().getNotificationChannel(this.mContext.getPackageName(), str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<NotificationChannel> getNotificationChannels() {
        try {
            return getService().getNotificationChannels(this.mContext.getPackageName()).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void deleteNotificationChannel(String str) {
        try {
            getService().deleteNotificationChannel(this.mContext.getPackageName(), str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NotificationChannelGroup getNotificationChannelGroup(String str) {
        try {
            return getService().getNotificationChannelGroup(this.mContext.getPackageName(), str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<NotificationChannelGroup> getNotificationChannelGroups() {
        try {
            return getService().getNotificationChannelGroups(this.mContext.getPackageName()).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void deleteNotificationChannelGroup(String str) {
        try {
            getService().deleteNotificationChannelGroup(this.mContext.getPackageName(), str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ComponentName getEffectsSuppressor() {
        try {
            return getService().getEffectsSuppressor();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean matchesCallFilter(Bundle bundle) {
        try {
            return getService().matchesCallFilter(bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSystemConditionProviderEnabled(String str) {
        try {
            return getService().isSystemConditionProviderEnabled(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setZenMode(int i, Uri uri, String str) {
        try {
            getService().setZenMode(i, uri, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getZenMode() {
        try {
            return getService().getZenMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ZenModeConfig getZenModeConfig() {
        try {
            return getService().getZenModeConfig();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getRuleInstanceCount(ComponentName componentName) {
        try {
            return getService().getRuleInstanceCount(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Map<String, AutomaticZenRule> getAutomaticZenRules() {
        try {
            List<ZenModeConfig.ZenRule> zenRules = getService().getZenRules();
            HashMap map = new HashMap();
            for (ZenModeConfig.ZenRule zenRule : zenRules) {
                map.put(zenRule.id, new AutomaticZenRule(zenRule.name, zenRule.component, zenRule.conditionId, zenModeToInterruptionFilter(zenRule.zenMode), zenRule.enabled, zenRule.creationTime));
            }
            return map;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AutomaticZenRule getAutomaticZenRule(String str) {
        try {
            return getService().getAutomaticZenRule(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String addAutomaticZenRule(AutomaticZenRule automaticZenRule) {
        try {
            return getService().addAutomaticZenRule(automaticZenRule);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean updateAutomaticZenRule(String str, AutomaticZenRule automaticZenRule) {
        try {
            return getService().updateAutomaticZenRule(str, automaticZenRule);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean removeAutomaticZenRule(String str) {
        try {
            return getService().removeAutomaticZenRule(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean removeAutomaticZenRules(String str) {
        try {
            return getService().removeAutomaticZenRules(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getImportance() {
        try {
            return getService().getPackageImportance(this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean areNotificationsEnabled() {
        try {
            return getService().areNotificationsEnabled(this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isNotificationPolicyAccessGranted() {
        try {
            return getService().isNotificationPolicyAccessGranted(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isNotificationListenerAccessGranted(ComponentName componentName) {
        try {
            return getService().isNotificationListenerAccessGranted(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isNotificationAssistantAccessGranted(ComponentName componentName) {
        try {
            return getService().isNotificationAssistantAccessGranted(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isNotificationPolicyAccessGrantedForPackage(String str) {
        try {
            return getService().isNotificationPolicyAccessGrantedForPackage(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getEnabledNotificationListenerPackages() {
        try {
            return getService().getEnabledNotificationListenerPackages();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Policy getNotificationPolicy() {
        try {
            return getService().getNotificationPolicy(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setNotificationPolicy(Policy policy) {
        checkRequired("policy", policy);
        try {
            getService().setNotificationPolicy(this.mContext.getOpPackageName(), policy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setNotificationPolicyAccessGranted(String str, boolean z) {
        try {
            getService().setNotificationPolicyAccessGranted(str, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setNotificationListenerAccessGranted(ComponentName componentName, boolean z) {
        try {
            getService().setNotificationListenerAccessGranted(componentName, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setNotificationListenerAccessGrantedForUser(ComponentName componentName, int i, boolean z) {
        try {
            getService().setNotificationListenerAccessGrantedForUser(componentName, i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ComponentName> getEnabledNotificationListeners(int i) {
        try {
            return getService().getEnabledNotificationListeners(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkRequired(String str, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(str + " is required");
        }
    }

    public static class Policy implements Parcelable {
        public static final int PRIORITY_CATEGORY_ALARMS = 32;
        public static final int PRIORITY_CATEGORY_CALLS = 8;
        public static final int PRIORITY_CATEGORY_EVENTS = 2;
        public static final int PRIORITY_CATEGORY_MEDIA = 64;
        public static final int PRIORITY_CATEGORY_MESSAGES = 4;
        public static final int PRIORITY_CATEGORY_REMINDERS = 1;
        public static final int PRIORITY_CATEGORY_REPEAT_CALLERS = 16;
        public static final int PRIORITY_CATEGORY_SYSTEM = 128;
        public static final int PRIORITY_SENDERS_ANY = 0;
        public static final int PRIORITY_SENDERS_CONTACTS = 1;
        public static final int PRIORITY_SENDERS_STARRED = 2;
        public static final int STATE_CHANNELS_BYPASSING_DND = 1;
        public static final int STATE_UNSET = -1;
        public static final int SUPPRESSED_EFFECTS_UNSET = -1;
        public static final int SUPPRESSED_EFFECT_AMBIENT = 128;
        public static final int SUPPRESSED_EFFECT_BADGE = 64;
        public static final int SUPPRESSED_EFFECT_FULL_SCREEN_INTENT = 4;
        public static final int SUPPRESSED_EFFECT_LIGHTS = 8;
        public static final int SUPPRESSED_EFFECT_NOTIFICATION_LIST = 256;
        public static final int SUPPRESSED_EFFECT_PEEK = 16;

        @Deprecated
        public static final int SUPPRESSED_EFFECT_SCREEN_OFF = 1;

        @Deprecated
        public static final int SUPPRESSED_EFFECT_SCREEN_ON = 2;
        public static final int SUPPRESSED_EFFECT_STATUS_BAR = 32;
        public final int priorityCallSenders;
        public final int priorityCategories;
        public final int priorityMessageSenders;
        public final int state;
        public final int suppressedVisualEffects;
        public static final int[] ALL_PRIORITY_CATEGORIES = {32, 64, 128, 1, 2, 4, 8, 16};
        private static final int[] ALL_SUPPRESSED_EFFECTS = {1, 2, 4, 8, 16, 32, 64, 128, 256};
        private static final int[] SCREEN_OFF_SUPPRESSED_EFFECTS = {1, 4, 8, 128};
        private static final int[] SCREEN_ON_SUPPRESSED_EFFECTS = {2, 16, 32, 64, 256};
        public static final Parcelable.Creator<Policy> CREATOR = new Parcelable.Creator<Policy>() {
            @Override
            public Policy createFromParcel(Parcel parcel) {
                return new Policy(parcel);
            }

            @Override
            public Policy[] newArray(int i) {
                return new Policy[i];
            }
        };

        public Policy(int i, int i2, int i3) {
            this(i, i2, i3, -1, -1);
        }

        public Policy(int i, int i2, int i3, int i4) {
            this.priorityCategories = i;
            this.priorityCallSenders = i2;
            this.priorityMessageSenders = i3;
            this.suppressedVisualEffects = i4;
            this.state = -1;
        }

        public Policy(int i, int i2, int i3, int i4, int i5) {
            this.priorityCategories = i;
            this.priorityCallSenders = i2;
            this.priorityMessageSenders = i3;
            this.suppressedVisualEffects = i4;
            this.state = i5;
        }

        public Policy(Parcel parcel) {
            this(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.priorityCategories);
            parcel.writeInt(this.priorityCallSenders);
            parcel.writeInt(this.priorityMessageSenders);
            parcel.writeInt(this.suppressedVisualEffects);
            parcel.writeInt(this.state);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.priorityCategories), Integer.valueOf(this.priorityCallSenders), Integer.valueOf(this.priorityMessageSenders), Integer.valueOf(this.suppressedVisualEffects));
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Policy)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            Policy policy = (Policy) obj;
            return policy.priorityCategories == this.priorityCategories && policy.priorityCallSenders == this.priorityCallSenders && policy.priorityMessageSenders == this.priorityMessageSenders && policy.suppressedVisualEffects == this.suppressedVisualEffects;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("NotificationManager.Policy[priorityCategories=");
            sb.append(priorityCategoriesToString(this.priorityCategories));
            sb.append(",priorityCallSenders=");
            sb.append(prioritySendersToString(this.priorityCallSenders));
            sb.append(",priorityMessageSenders=");
            sb.append(prioritySendersToString(this.priorityMessageSenders));
            sb.append(",suppressedVisualEffects=");
            sb.append(suppressedEffectsToString(this.suppressedVisualEffects));
            sb.append(",areChannelsBypassingDnd=");
            sb.append((this.state & 1) != 0 ? "true" : "false");
            sb.append("]");
            return sb.toString();
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            bitwiseToProtoEnum(protoOutputStream, 2259152797697L, this.priorityCategories);
            protoOutputStream.write(1159641169922L, this.priorityCallSenders);
            protoOutputStream.write(1159641169923L, this.priorityMessageSenders);
            bitwiseToProtoEnum(protoOutputStream, 2259152797700L, this.suppressedVisualEffects);
            protoOutputStream.end(jStart);
        }

        private static void bitwiseToProtoEnum(ProtoOutputStream protoOutputStream, long j, int i) {
            int i2 = 1;
            while (i > 0) {
                if ((i & 1) == 1) {
                    protoOutputStream.write(j, i2);
                }
                i2++;
                i >>>= 1;
            }
        }

        public static int getAllSuppressedVisualEffects() {
            int i = 0;
            for (int i2 = 0; i2 < ALL_SUPPRESSED_EFFECTS.length; i2++) {
                i |= ALL_SUPPRESSED_EFFECTS[i2];
            }
            return i;
        }

        public static boolean areAllVisualEffectsSuppressed(int i) {
            for (int i2 = 0; i2 < ALL_SUPPRESSED_EFFECTS.length; i2++) {
                if ((ALL_SUPPRESSED_EFFECTS[i2] & i) == 0) {
                    return false;
                }
            }
            return true;
        }

        public static boolean areAnyScreenOffEffectsSuppressed(int i) {
            for (int i2 = 0; i2 < SCREEN_OFF_SUPPRESSED_EFFECTS.length; i2++) {
                if ((SCREEN_OFF_SUPPRESSED_EFFECTS[i2] & i) != 0) {
                    return true;
                }
            }
            return false;
        }

        public static boolean areAnyScreenOnEffectsSuppressed(int i) {
            for (int i2 = 0; i2 < SCREEN_ON_SUPPRESSED_EFFECTS.length; i2++) {
                if ((SCREEN_ON_SUPPRESSED_EFFECTS[i2] & i) != 0) {
                    return true;
                }
            }
            return false;
        }

        public static int toggleScreenOffEffectsSuppressed(int i, boolean z) {
            return toggleEffects(i, SCREEN_OFF_SUPPRESSED_EFFECTS, z);
        }

        public static int toggleScreenOnEffectsSuppressed(int i, boolean z) {
            return toggleEffects(i, SCREEN_ON_SUPPRESSED_EFFECTS, z);
        }

        private static int toggleEffects(int i, int[] iArr, boolean z) {
            for (int i2 : iArr) {
                if (z) {
                    i |= i2;
                } else {
                    i &= ~i2;
                }
            }
            return i;
        }

        public static String suppressedEffectsToString(int i) {
            if (i <= 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i2 = 0; i2 < ALL_SUPPRESSED_EFFECTS.length; i2++) {
                int i3 = ALL_SUPPRESSED_EFFECTS[i2];
                if ((i & i3) != 0) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(effectToString(i3));
                }
                i &= ~i3;
            }
            if (i != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append("UNKNOWN_");
                sb.append(i);
            }
            return sb.toString();
        }

        public static String priorityCategoriesToString(int i) {
            if (i == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i2 = 0; i2 < ALL_PRIORITY_CATEGORIES.length; i2++) {
                int i3 = ALL_PRIORITY_CATEGORIES[i2];
                if ((i & i3) != 0) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(priorityCategoryToString(i3));
                }
                i &= ~i3;
            }
            if (i != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append("PRIORITY_CATEGORY_UNKNOWN_");
                sb.append(i);
            }
            return sb.toString();
        }

        private static String effectToString(int i) {
            if (i == -1) {
                return "SUPPRESSED_EFFECTS_UNSET";
            }
            if (i == 4) {
                return "SUPPRESSED_EFFECT_FULL_SCREEN_INTENT";
            }
            if (i == 8) {
                return "SUPPRESSED_EFFECT_LIGHTS";
            }
            if (i == 16) {
                return "SUPPRESSED_EFFECT_PEEK";
            }
            if (i == 32) {
                return "SUPPRESSED_EFFECT_STATUS_BAR";
            }
            if (i == 64) {
                return "SUPPRESSED_EFFECT_BADGE";
            }
            if (i == 128) {
                return "SUPPRESSED_EFFECT_AMBIENT";
            }
            if (i == 256) {
                return "SUPPRESSED_EFFECT_NOTIFICATION_LIST";
            }
            switch (i) {
                case 1:
                    return "SUPPRESSED_EFFECT_SCREEN_OFF";
                case 2:
                    return "SUPPRESSED_EFFECT_SCREEN_ON";
                default:
                    return "UNKNOWN_" + i;
            }
        }

        private static String priorityCategoryToString(int i) {
            if (i == 4) {
                return "PRIORITY_CATEGORY_MESSAGES";
            }
            if (i == 8) {
                return "PRIORITY_CATEGORY_CALLS";
            }
            if (i == 16) {
                return "PRIORITY_CATEGORY_REPEAT_CALLERS";
            }
            if (i == 32) {
                return "PRIORITY_CATEGORY_ALARMS";
            }
            if (i == 64) {
                return "PRIORITY_CATEGORY_MEDIA";
            }
            if (i != 128) {
                switch (i) {
                    case 1:
                        return "PRIORITY_CATEGORY_REMINDERS";
                    case 2:
                        return "PRIORITY_CATEGORY_EVENTS";
                    default:
                        return "PRIORITY_CATEGORY_UNKNOWN_" + i;
                }
            }
            return "PRIORITY_CATEGORY_SYSTEM";
        }

        public static String prioritySendersToString(int i) {
            switch (i) {
                case 0:
                    return "PRIORITY_SENDERS_ANY";
                case 1:
                    return "PRIORITY_SENDERS_CONTACTS";
                case 2:
                    return "PRIORITY_SENDERS_STARRED";
                default:
                    return "PRIORITY_SENDERS_UNKNOWN_" + i;
            }
        }
    }

    public StatusBarNotification[] getActiveNotifications() {
        try {
            List list = getService().getAppActiveNotifications(this.mContext.getPackageName(), this.mContext.getUserId()).getList();
            return (StatusBarNotification[]) list.toArray(new StatusBarNotification[list.size()]);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public final int getCurrentInterruptionFilter() {
        try {
            return zenModeToInterruptionFilter(getService().getZenMode());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public final void setInterruptionFilter(int i) {
        try {
            getService().setInterruptionFilter(this.mContext.getOpPackageName(), i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int zenModeToInterruptionFilter(int i) {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return 0;
        }
    }

    public static int zenModeFromInterruptionFilter(int i, int i2) {
        switch (i) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            default:
                return i2;
        }
    }
}
