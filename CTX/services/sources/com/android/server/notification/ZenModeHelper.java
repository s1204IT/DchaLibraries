package com.android.server.notification;

import android.R;
import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManagerInternal;
import android.media.VolumePolicy;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.LocalServices;
import com.android.server.notification.ManagedServices;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeHelper {
    private static final int RULE_INSTANCE_GRACE_PERIOD = 259200000;
    public static final long SUPPRESSED_EFFECT_ALL = 3;
    public static final long SUPPRESSED_EFFECT_CALLS = 2;
    public static final long SUPPRESSED_EFFECT_NOTIFICATIONS = 1;

    @VisibleForTesting
    protected final AppOpsManager mAppOps;

    @VisibleForTesting
    protected AudioManagerInternal mAudioManager;
    private final ZenModeConditions mConditions;

    @VisibleForTesting
    protected ZenModeConfig mConfig;
    private final Context mContext;
    protected ZenModeConfig mDefaultConfig;
    protected String mDefaultRuleEventsName;
    protected String mDefaultRuleEveryNightName;
    private final ZenModeFiltering mFiltering;
    private final H mHandler;

    @VisibleForTesting
    protected boolean mIsBootComplete;
    private final Metrics mMetrics;

    @VisibleForTesting
    protected final NotificationManager mNotificationManager;
    protected PackageManager mPm;
    private final ManagedServices.Config mServiceConfig;
    private final SettingsObserver mSettingsObserver;
    private long mSuppressedEffects;

    @VisibleForTesting
    protected int mZenMode;
    static final String TAG = "ZenModeHelper";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    protected final RingerModeDelegate mRingerModeDelegate = new RingerModeDelegate();
    private final SparseArray<ZenModeConfig> mConfigs = new SparseArray<>();
    private int mUser = 0;

    public ZenModeHelper(Context context, Looper looper, ConditionProviders conditionProviders) {
        this.mMetrics = new Metrics();
        this.mContext = context;
        this.mHandler = new H(looper);
        addCallback(this.mMetrics);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mDefaultConfig = new ZenModeConfig();
        setDefaultZenRules(this.mContext);
        this.mConfig = this.mDefaultConfig;
        this.mConfigs.put(0, this.mConfig);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        this.mFiltering = new ZenModeFiltering(this.mContext);
        this.mConditions = new ZenModeConditions(this, conditionProviders);
        this.mServiceConfig = conditionProviders.getConfig();
    }

    public Looper getLooper() {
        return this.mHandler.getLooper();
    }

    public String toString() {
        return TAG;
    }

    public boolean matchesCallFilter(UserHandle userHandle, Bundle bundle, ValidateNotificationPeople validateNotificationPeople, int i, float f) {
        boolean zMatchesCallFilter;
        synchronized (this.mConfig) {
            zMatchesCallFilter = ZenModeFiltering.matchesCallFilter(this.mContext, this.mZenMode, this.mConfig, userHandle, bundle, validateNotificationPeople, i, f);
        }
        return zMatchesCallFilter;
    }

    public boolean isCall(NotificationRecord notificationRecord) {
        return this.mFiltering.isCall(notificationRecord);
    }

    public void recordCaller(NotificationRecord notificationRecord) {
        this.mFiltering.recordCall(notificationRecord);
    }

    public boolean shouldIntercept(NotificationRecord notificationRecord) {
        boolean zShouldIntercept;
        synchronized (this.mConfig) {
            zShouldIntercept = this.mFiltering.shouldIntercept(this.mZenMode, this.mConfig, notificationRecord);
        }
        return zShouldIntercept;
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public void initZenMode() {
        if (DEBUG) {
            Log.d(TAG, "initZenMode");
        }
        evaluateZenMode("init", true);
    }

    public void onSystemReady() {
        if (DEBUG) {
            Log.d(TAG, "onSystemReady");
        }
        this.mAudioManager = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (this.mAudioManager != null) {
            this.mAudioManager.setRingerModeDelegate(this.mRingerModeDelegate);
        }
        this.mPm = this.mContext.getPackageManager();
        this.mHandler.postMetricsTimer();
        cleanUpZenRules();
        evaluateZenMode("onSystemReady", true);
        this.mIsBootComplete = true;
        showZenUpgradeNotification(this.mZenMode);
    }

    public void onUserSwitched(int i) {
        loadConfigForUser(i, "onUserSwitched");
    }

    public void onUserRemoved(int i) {
        if (i < 0) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onUserRemoved u=" + i);
        }
        this.mConfigs.remove(i);
    }

    public void onUserUnlocked(int i) {
        loadConfigForUser(i, "onUserUnlocked");
    }

    private void loadConfigForUser(int i, String str) {
        if (this.mUser == i || i < 0) {
            return;
        }
        this.mUser = i;
        if (DEBUG) {
            Log.d(TAG, str + " u=" + i);
        }
        ZenModeConfig zenModeConfigCopy = this.mConfigs.get(i);
        if (zenModeConfigCopy == null) {
            if (DEBUG) {
                Log.d(TAG, str + " generating default config for user " + i);
            }
            zenModeConfigCopy = this.mDefaultConfig.copy();
            zenModeConfigCopy.user = i;
        }
        synchronized (this.mConfig) {
            setConfigLocked(zenModeConfigCopy, str);
        }
        cleanUpZenRules();
    }

    public int getZenModeListenerInterruptionFilter() {
        return NotificationManager.zenModeToInterruptionFilter(this.mZenMode);
    }

    public void requestFromListener(ComponentName componentName, int i) {
        int iZenModeFromInterruptionFilter = NotificationManager.zenModeFromInterruptionFilter(i, -1);
        if (iZenModeFromInterruptionFilter != -1) {
            String packageName = componentName != null ? componentName.getPackageName() : null;
            StringBuilder sb = new StringBuilder();
            sb.append("listener:");
            sb.append(componentName != null ? componentName.flattenToShortString() : null);
            setManualZenMode(iZenModeFromInterruptionFilter, null, packageName, sb.toString());
        }
    }

    public void setSuppressedEffects(long j) {
        if (this.mSuppressedEffects == j) {
            return;
        }
        this.mSuppressedEffects = j;
        applyRestrictions();
    }

    public long getSuppressedEffects() {
        return this.mSuppressedEffects;
    }

    public int getZenMode() {
        return this.mZenMode;
    }

    public List<ZenModeConfig.ZenRule> getZenRules() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return arrayList;
            }
            for (ZenModeConfig.ZenRule zenRule : this.mConfig.automaticRules.values()) {
                if (canManageAutomaticZenRule(zenRule)) {
                    arrayList.add(zenRule);
                }
            }
            return arrayList;
        }
    }

    public AutomaticZenRule getAutomaticZenRule(String str) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return null;
            }
            ZenModeConfig.ZenRule zenRule = (ZenModeConfig.ZenRule) this.mConfig.automaticRules.get(str);
            if (zenRule != null && canManageAutomaticZenRule(zenRule)) {
                return createAutomaticZenRule(zenRule);
            }
            return null;
        }
    }

    public String addAutomaticZenRule(AutomaticZenRule automaticZenRule, String str) {
        String str2;
        if (!isSystemRule(automaticZenRule)) {
            ServiceInfo serviceInfo = getServiceInfo(automaticZenRule.getOwner());
            if (serviceInfo == null) {
                throw new IllegalArgumentException("Owner is not a condition provider service");
            }
            int i = serviceInfo.metaData != null ? serviceInfo.metaData.getInt("android.service.zen.automatic.ruleInstanceLimit", -1) : -1;
            if (i > 0 && i < getCurrentInstanceCount(automaticZenRule.getOwner()) + 1) {
                throw new IllegalArgumentException("Rule instance limit exceeded");
            }
        }
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                throw new AndroidRuntimeException("Could not create rule");
            }
            if (DEBUG) {
                Log.d(TAG, "addAutomaticZenRule rule= " + automaticZenRule + " reason=" + str);
            }
            ZenModeConfig zenModeConfigCopy = this.mConfig.copy();
            ZenModeConfig.ZenRule zenRule = new ZenModeConfig.ZenRule();
            populateZenRule(automaticZenRule, zenRule, true);
            zenModeConfigCopy.automaticRules.put(zenRule.id, zenRule);
            if (setConfigLocked(zenModeConfigCopy, str, true)) {
                str2 = zenRule.id;
            } else {
                throw new AndroidRuntimeException("Could not create rule");
            }
        }
        return str2;
    }

    public boolean updateAutomaticZenRule(String str, AutomaticZenRule automaticZenRule, String str2) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            if (DEBUG) {
                Log.d(TAG, "updateAutomaticZenRule zenRule=" + automaticZenRule + " reason=" + str2);
            }
            ZenModeConfig zenModeConfigCopy = this.mConfig.copy();
            if (str == null) {
                throw new IllegalArgumentException("Rule doesn't exist");
            }
            ZenModeConfig.ZenRule zenRule = (ZenModeConfig.ZenRule) zenModeConfigCopy.automaticRules.get(str);
            if (zenRule == null || !canManageAutomaticZenRule(zenRule)) {
                throw new SecurityException("Cannot update rules not owned by your condition provider");
            }
            populateZenRule(automaticZenRule, zenRule, false);
            zenModeConfigCopy.automaticRules.put(str, zenRule);
            return setConfigLocked(zenModeConfigCopy, str2, true);
        }
    }

    public boolean removeAutomaticZenRule(String str, String str2) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig zenModeConfigCopy = this.mConfig.copy();
            ZenModeConfig.ZenRule zenRule = (ZenModeConfig.ZenRule) zenModeConfigCopy.automaticRules.get(str);
            if (zenRule == null) {
                return false;
            }
            if (canManageAutomaticZenRule(zenRule)) {
                zenModeConfigCopy.automaticRules.remove(str);
                if (DEBUG) {
                    Log.d(TAG, "removeZenRule zenRule=" + str + " reason=" + str2);
                }
                return setConfigLocked(zenModeConfigCopy, str2, true);
            }
            throw new SecurityException("Cannot delete rules not owned by your condition provider");
        }
    }

    public boolean removeAutomaticZenRules(String str, String str2) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig zenModeConfigCopy = this.mConfig.copy();
            for (int size = zenModeConfigCopy.automaticRules.size() - 1; size >= 0; size--) {
                ZenModeConfig.ZenRule zenRule = (ZenModeConfig.ZenRule) zenModeConfigCopy.automaticRules.get(zenModeConfigCopy.automaticRules.keyAt(size));
                if (zenRule.component.getPackageName().equals(str) && canManageAutomaticZenRule(zenRule)) {
                    zenModeConfigCopy.automaticRules.removeAt(size);
                }
            }
            return setConfigLocked(zenModeConfigCopy, str2, true);
        }
    }

    public int getCurrentInstanceCount(ComponentName componentName) {
        int i;
        synchronized (this.mConfig) {
            i = 0;
            for (ZenModeConfig.ZenRule zenRule : this.mConfig.automaticRules.values()) {
                if (zenRule.component != null && zenRule.component.equals(componentName)) {
                    i++;
                }
            }
        }
        return i;
    }

    public boolean canManageAutomaticZenRule(ZenModeConfig.ZenRule zenRule) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0 || callingUid == 1000 || this.mContext.checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
            return true;
        }
        String[] packagesForUid = this.mPm.getPackagesForUid(Binder.getCallingUid());
        if (packagesForUid != null) {
            for (String str : packagesForUid) {
                if (str.equals(zenRule.component.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setDefaultZenRules(Context context) {
        this.mDefaultConfig = readDefaultConfig(context.getResources());
        appendDefaultRules(this.mDefaultConfig);
    }

    private void appendDefaultRules(ZenModeConfig zenModeConfig) {
        getDefaultRuleNames();
        appendDefaultEveryNightRule(zenModeConfig);
        appendDefaultEventRules(zenModeConfig);
    }

    private boolean ruleValuesEqual(AutomaticZenRule automaticZenRule, ZenModeConfig.ZenRule zenRule) {
        return automaticZenRule != null && zenRule != null && automaticZenRule.getInterruptionFilter() == NotificationManager.zenModeToInterruptionFilter(zenRule.zenMode) && automaticZenRule.getConditionId().equals(zenRule.conditionId) && automaticZenRule.getOwner().equals(zenRule.component);
    }

    protected void updateDefaultZenRules() {
        ZenModeConfig zenModeConfig = new ZenModeConfig();
        appendDefaultRules(zenModeConfig);
        for (String str : ZenModeConfig.DEFAULT_RULE_IDS) {
            AutomaticZenRule automaticZenRule = getAutomaticZenRule(str);
            ZenModeConfig.ZenRule zenRule = (ZenModeConfig.ZenRule) zenModeConfig.automaticRules.get(str);
            if (ruleValuesEqual(automaticZenRule, zenRule) && !zenRule.name.equals(automaticZenRule.getName()) && canManageAutomaticZenRule(zenRule)) {
                if (DEBUG) {
                    Slog.d(TAG, "Locale change - updating default zen rule name from " + automaticZenRule.getName() + " to " + zenRule.name);
                }
                AutomaticZenRule automaticZenRuleCreateAutomaticZenRule = createAutomaticZenRule(zenRule);
                automaticZenRuleCreateAutomaticZenRule.setEnabled(automaticZenRule.isEnabled());
                updateAutomaticZenRule(str, automaticZenRuleCreateAutomaticZenRule, "locale changed");
            }
        }
    }

    private boolean isSystemRule(AutomaticZenRule automaticZenRule) {
        return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(automaticZenRule.getOwner().getPackageName());
    }

    private ServiceInfo getServiceInfo(ComponentName componentName) {
        Intent intent = new Intent();
        intent.setComponent(componentName);
        List listQueryIntentServicesAsUser = this.mPm.queryIntentServicesAsUser(intent, 132, UserHandle.getCallingUserId());
        if (listQueryIntentServicesAsUser != null) {
            int size = listQueryIntentServicesAsUser.size();
            for (int i = 0; i < size; i++) {
                ServiceInfo serviceInfo = ((ResolveInfo) listQueryIntentServicesAsUser.get(i)).serviceInfo;
                if (this.mServiceConfig.bindPermission.equals(serviceInfo.permission)) {
                    return serviceInfo;
                }
            }
            return null;
        }
        return null;
    }

    private void populateZenRule(AutomaticZenRule automaticZenRule, ZenModeConfig.ZenRule zenRule, boolean z) {
        if (z) {
            zenRule.id = ZenModeConfig.newRuleId();
            zenRule.creationTime = System.currentTimeMillis();
            zenRule.component = automaticZenRule.getOwner();
        }
        if (zenRule.enabled != automaticZenRule.isEnabled()) {
            zenRule.snoozing = false;
        }
        zenRule.name = automaticZenRule.getName();
        zenRule.condition = null;
        zenRule.conditionId = automaticZenRule.getConditionId();
        zenRule.enabled = automaticZenRule.isEnabled();
        zenRule.zenMode = NotificationManager.zenModeFromInterruptionFilter(automaticZenRule.getInterruptionFilter(), 0);
    }

    protected AutomaticZenRule createAutomaticZenRule(ZenModeConfig.ZenRule zenRule) {
        return new AutomaticZenRule(zenRule.name, zenRule.component, zenRule.conditionId, NotificationManager.zenModeToInterruptionFilter(zenRule.zenMode), zenRule.enabled, zenRule.creationTime);
    }

    public void setManualZenMode(int i, Uri uri, String str, String str2) {
        setManualZenMode(i, uri, str2, str, true);
        Settings.Global.putInt(this.mContext.getContentResolver(), "show_zen_settings_suggestion", 0);
    }

    private void setManualZenMode(int i, Uri uri, String str, String str2, boolean z) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return;
            }
            if (Settings.Global.isValidZenMode(i)) {
                if (DEBUG) {
                    Log.d(TAG, "setManualZenMode " + Settings.Global.zenModeToString(i) + " conditionId=" + uri + " reason=" + str + " setRingerMode=" + z);
                }
                ZenModeConfig zenModeConfigCopy = this.mConfig.copy();
                if (i == 0) {
                    zenModeConfigCopy.manualRule = null;
                    for (ZenModeConfig.ZenRule zenRule : zenModeConfigCopy.automaticRules.values()) {
                        if (zenRule.isAutomaticActive()) {
                            zenRule.snoozing = true;
                        }
                    }
                } else {
                    ZenModeConfig.ZenRule zenRule2 = new ZenModeConfig.ZenRule();
                    zenRule2.enabled = true;
                    zenRule2.zenMode = i;
                    zenRule2.conditionId = uri;
                    zenRule2.enabler = str2;
                    zenModeConfigCopy.manualRule = zenRule2;
                }
                setConfigLocked(zenModeConfigCopy, str, z);
            }
        }
    }

    void dump(ProtoOutputStream protoOutputStream) {
        protoOutputStream.write(1159641169921L, this.mZenMode);
        synchronized (this.mConfig) {
            if (this.mConfig.manualRule != null) {
                this.mConfig.manualRule.writeToProto(protoOutputStream, 2246267895810L);
            }
            for (ZenModeConfig.ZenRule zenRule : this.mConfig.automaticRules.values()) {
                if (zenRule.enabled && zenRule.condition.state == 1 && !zenRule.snoozing) {
                    zenRule.writeToProto(protoOutputStream, 2246267895810L);
                }
            }
            this.mConfig.toNotificationPolicy().writeToProto(protoOutputStream, 1146756268037L);
            protoOutputStream.write(1120986464259L, this.mSuppressedEffects);
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mZenMode=");
        printWriter.println(Settings.Global.zenModeToString(this.mZenMode));
        int size = this.mConfigs.size();
        for (int i = 0; i < size; i++) {
            dump(printWriter, str, "mConfigs[u=" + this.mConfigs.keyAt(i) + "]", this.mConfigs.valueAt(i));
        }
        printWriter.print(str);
        printWriter.print("mUser=");
        printWriter.println(this.mUser);
        synchronized (this.mConfig) {
            dump(printWriter, str, "mConfig", this.mConfig);
        }
        printWriter.print(str);
        printWriter.print("mSuppressedEffects=");
        printWriter.println(this.mSuppressedEffects);
        this.mFiltering.dump(printWriter, str);
        this.mConditions.dump(printWriter, str);
    }

    private static void dump(PrintWriter printWriter, String str, String str2, ZenModeConfig zenModeConfig) {
        printWriter.print(str);
        printWriter.print(str2);
        printWriter.print('=');
        if (zenModeConfig == null) {
            printWriter.println(zenModeConfig);
            return;
        }
        int i = 0;
        printWriter.printf("allow(alarms=%b,media=%b,system=%b,calls=%b,callsFrom=%s,repeatCallers=%b,messages=%b,messagesFrom=%s,events=%b,reminders=%b)\n", Boolean.valueOf(zenModeConfig.allowAlarms), Boolean.valueOf(zenModeConfig.allowMedia), Boolean.valueOf(zenModeConfig.allowSystem), Boolean.valueOf(zenModeConfig.allowCalls), ZenModeConfig.sourceToString(zenModeConfig.allowCallsFrom), Boolean.valueOf(zenModeConfig.allowRepeatCallers), Boolean.valueOf(zenModeConfig.allowMessages), ZenModeConfig.sourceToString(zenModeConfig.allowMessagesFrom), Boolean.valueOf(zenModeConfig.allowEvents), Boolean.valueOf(zenModeConfig.allowReminders));
        printWriter.printf(" disallow(visualEffects=%s)\n", Integer.valueOf(zenModeConfig.suppressedVisualEffects));
        printWriter.print(str);
        printWriter.print("  manualRule=");
        printWriter.println(zenModeConfig.manualRule);
        if (zenModeConfig.automaticRules.isEmpty()) {
            return;
        }
        int size = zenModeConfig.automaticRules.size();
        while (i < size) {
            printWriter.print(str);
            printWriter.print(i == 0 ? "  automaticRules=" : "                 ");
            printWriter.println(zenModeConfig.automaticRules.valueAt(i));
            i++;
        }
    }

    public void readXml(XmlPullParser xmlPullParser, boolean z) throws XmlPullParserException, IOException {
        boolean z2;
        ZenModeConfig xml = ZenModeConfig.readXml(xmlPullParser);
        String str = "readXml";
        if (xml != null) {
            if (z) {
                if (xml.user != 0) {
                    return;
                } else {
                    xml.manualRule = null;
                }
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (xml.automaticRules != null && xml.automaticRules.size() > 0) {
                z2 = true;
                for (ZenModeConfig.ZenRule zenRule : xml.automaticRules.values()) {
                    if (z) {
                        zenRule.snoozing = false;
                        zenRule.condition = null;
                        zenRule.creationTime = jCurrentTimeMillis;
                    }
                    z2 &= !zenRule.enabled;
                }
            } else {
                z2 = true;
            }
            if (xml.version < 8 || z) {
                Settings.Global.putInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 1);
                if (z2) {
                    xml.automaticRules = new ArrayMap();
                    appendDefaultRules(xml);
                    str = "readXml, reset to default rules";
                }
            } else {
                Settings.Global.putInt(this.mContext.getContentResolver(), "zen_settings_updated", 1);
            }
            if (DEBUG) {
                Log.d(TAG, str);
            }
            synchronized (this.mConfig) {
                setConfigLocked(xml, str);
            }
        }
    }

    public void writeXml(XmlSerializer xmlSerializer, boolean z, Integer num) throws IOException {
        int size = this.mConfigs.size();
        for (int i = 0; i < size; i++) {
            if (!z || this.mConfigs.keyAt(i) == 0) {
                this.mConfigs.valueAt(i).writeXml(xmlSerializer, num);
            }
        }
    }

    public NotificationManager.Policy getNotificationPolicy() {
        return getNotificationPolicy(this.mConfig);
    }

    private static NotificationManager.Policy getNotificationPolicy(ZenModeConfig zenModeConfig) {
        if (zenModeConfig == null) {
            return null;
        }
        return zenModeConfig.toNotificationPolicy();
    }

    public void setNotificationPolicy(NotificationManager.Policy policy) {
        if (policy == null || this.mConfig == null) {
            return;
        }
        synchronized (this.mConfig) {
            ZenModeConfig zenModeConfigCopy = this.mConfig.copy();
            zenModeConfigCopy.applyNotificationPolicy(policy);
            setConfigLocked(zenModeConfigCopy, "setNotificationPolicy");
        }
    }

    private void cleanUpZenRules() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (this.mConfig) {
            ZenModeConfig zenModeConfigCopy = this.mConfig.copy();
            if (zenModeConfigCopy.automaticRules != null) {
                for (int size = zenModeConfigCopy.automaticRules.size() - 1; size >= 0; size--) {
                    ZenModeConfig.ZenRule zenRule = (ZenModeConfig.ZenRule) zenModeConfigCopy.automaticRules.get(zenModeConfigCopy.automaticRules.keyAt(size));
                    if (259200000 < jCurrentTimeMillis - zenRule.creationTime) {
                        try {
                            this.mPm.getPackageInfo(zenRule.component.getPackageName(), DumpState.DUMP_CHANGES);
                        } catch (PackageManager.NameNotFoundException e) {
                            zenModeConfigCopy.automaticRules.removeAt(size);
                        }
                    }
                }
                setConfigLocked(zenModeConfigCopy, "cleanUpZenRules");
            } else {
                setConfigLocked(zenModeConfigCopy, "cleanUpZenRules");
            }
        }
    }

    public ZenModeConfig getConfig() {
        ZenModeConfig zenModeConfigCopy;
        synchronized (this.mConfig) {
            zenModeConfigCopy = this.mConfig.copy();
        }
        return zenModeConfigCopy;
    }

    public boolean setConfigLocked(ZenModeConfig zenModeConfig, String str) {
        return setConfigLocked(zenModeConfig, str, true);
    }

    public void setConfig(ZenModeConfig zenModeConfig, String str) {
        synchronized (this.mConfig) {
            setConfigLocked(zenModeConfig, str);
        }
    }

    private boolean setConfigLocked(ZenModeConfig zenModeConfig, String str, boolean z) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (zenModeConfig != null) {
                if (zenModeConfig.isValid()) {
                    if (zenModeConfig.user != this.mUser) {
                        this.mConfigs.put(zenModeConfig.user, zenModeConfig);
                        if (DEBUG) {
                            Log.d(TAG, "setConfigLocked: store config for user " + zenModeConfig.user);
                        }
                        return true;
                    }
                    this.mConditions.evaluateConfig(zenModeConfig, false);
                    this.mConfigs.put(zenModeConfig.user, zenModeConfig);
                    if (DEBUG) {
                        Log.d(TAG, "setConfigLocked reason=" + str, new Throwable());
                    }
                    ZenLog.traceConfig(str, this.mConfig, zenModeConfig);
                    boolean z2 = !Objects.equals(getNotificationPolicy(this.mConfig), getNotificationPolicy(zenModeConfig));
                    if (!zenModeConfig.equals(this.mConfig)) {
                        dispatchOnConfigChanged();
                    }
                    if (z2) {
                        dispatchOnPolicyChanged();
                    }
                    this.mConfig = zenModeConfig;
                    this.mHandler.postApplyConfig(zenModeConfig, str, z);
                    return true;
                }
            }
            Log.w(TAG, "Invalid config in setConfigLocked; " + zenModeConfig);
            return false;
        } catch (SecurityException e) {
            Log.wtf(TAG, "Invalid rule in config", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void applyConfig(ZenModeConfig zenModeConfig, String str, boolean z) {
        Settings.Global.putString(this.mContext.getContentResolver(), "zen_mode_config_etag", Integer.toString(zenModeConfig.hashCode()));
        if (!evaluateZenMode(str, z)) {
            applyRestrictions();
        }
        this.mConditions.evaluateConfig(zenModeConfig, true);
    }

    private int getZenModeSetting() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
    }

    @VisibleForTesting
    protected void setZenModeSetting(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "zen_mode", i);
        showZenUpgradeNotification(i);
    }

    private int getPreviousRingerModeSetting() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode_ringer_level", 2);
    }

    private void setPreviousRingerModeSetting(Integer num) {
        Settings.Global.putString(this.mContext.getContentResolver(), "zen_mode_ringer_level", num == null ? null : Integer.toString(num.intValue()));
    }

    @VisibleForTesting
    protected boolean evaluateZenMode(String str, boolean z) {
        if (DEBUG) {
            Log.d(TAG, "evaluateZenMode");
        }
        int i = this.mZenMode;
        int iComputeZenMode = computeZenMode();
        ZenLog.traceSetZenMode(iComputeZenMode, str);
        this.mZenMode = iComputeZenMode;
        setZenModeSetting(this.mZenMode);
        updateRingerModeAffectedStreams();
        if (z && iComputeZenMode != i) {
            applyZenToRingerMode();
        }
        applyRestrictions();
        if (iComputeZenMode == i) {
            return true;
        }
        this.mHandler.postDispatchOnZenModeChanged();
        return true;
    }

    private void updateRingerModeAffectedStreams() {
        if (this.mAudioManager != null) {
            this.mAudioManager.updateRingerModeAffectedStreamsInternal();
        }
    }

    private int computeZenMode() {
        int i = 0;
        if (this.mConfig == null) {
            return 0;
        }
        synchronized (this.mConfig) {
            if (this.mConfig.manualRule != null) {
                return this.mConfig.manualRule.zenMode;
            }
            for (ZenModeConfig.ZenRule zenRule : this.mConfig.automaticRules.values()) {
                if (zenRule.isAutomaticActive() && zenSeverity(zenRule.zenMode) > zenSeverity(i)) {
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "zen_settings_suggestion_viewed", 1) == 0) {
                        Settings.Global.putInt(this.mContext.getContentResolver(), "show_zen_settings_suggestion", 1);
                    }
                    i = zenRule.zenMode;
                }
            }
            return i;
        }
    }

    private void getDefaultRuleNames() {
        this.mDefaultRuleEveryNightName = this.mContext.getResources().getString(R.string.number_picker_increment_button);
        this.mDefaultRuleEventsName = this.mContext.getResources().getString(R.string.number_picker_decrement_button);
    }

    @VisibleForTesting
    protected void applyRestrictions() {
        boolean z = true;
        boolean z2 = this.mZenMode == 1;
        boolean z3 = this.mZenMode == 2;
        boolean z4 = this.mZenMode == 3;
        boolean z5 = (this.mSuppressedEffects & 1) != 0;
        boolean z6 = z4 || !((!z2 || this.mConfig.allowCalls || this.mConfig.allowRepeatCallers) && (this.mSuppressedEffects & 2) == 0);
        boolean z7 = z2 && !this.mConfig.allowAlarms;
        boolean z8 = z2 && !this.mConfig.allowMedia;
        boolean z9 = z4 || (z2 && !this.mConfig.allowSystem);
        boolean z10 = z3 || (z2 && ZenModeConfig.areAllZenBehaviorSoundsMuted(this.mConfig));
        int[] iArr = AudioAttributes.SDK_USAGES;
        int length = iArr.length;
        int i = 0;
        while (i < length) {
            int i2 = iArr[i];
            int i3 = AudioAttributes.SUPPRESSIBLE_USAGES.get(i2);
            if (i3 == 3) {
                applyRestrictions(false, i2);
            } else if (i3 == z) {
                applyRestrictions((z5 || z10) ? z : false, i2);
            } else if (i3 == 2) {
                applyRestrictions((z6 || z10) ? z : false, i2);
            } else if (i3 == 4) {
                applyRestrictions(z7 || z10, i2);
            } else if (i3 == 5) {
                applyRestrictions(z8 || z10, i2);
            } else if (i3 == 6) {
                if (i2 == 13) {
                    applyRestrictions(z9 || z10, i2, 28);
                    applyRestrictions(false, i2, 3);
                } else {
                    applyRestrictions(z9 || z10, i2);
                }
            } else {
                applyRestrictions(z10, i2);
            }
            i++;
            z = true;
        }
    }

    @VisibleForTesting
    protected void applyRestrictions(boolean z, int i, int i2) {
        if (Process.myUid() == 1000) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mAppOps.setRestriction(i2, i, z ? 1 : 0, null);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    @VisibleForTesting
    protected void applyRestrictions(boolean z, int i) {
        applyRestrictions(z, i, 3);
        applyRestrictions(z, i, 28);
    }

    @VisibleForTesting
    protected void applyZenToRingerMode() {
        if (this.mAudioManager == null) {
            return;
        }
        int ringerModeInternal = this.mAudioManager.getRingerModeInternal();
        switch (this.mZenMode) {
            case 0:
                if (ringerModeInternal == 0) {
                    ringerModeInternal = getPreviousRingerModeSetting();
                    setPreviousRingerModeSetting(null);
                }
                break;
            case 2:
            case 3:
                if (ringerModeInternal != 0) {
                    setPreviousRingerModeSetting(Integer.valueOf(ringerModeInternal));
                    ringerModeInternal = 0;
                }
                break;
        }
        if (ringerModeInternal != -1) {
            this.mAudioManager.setRingerModeInternal(ringerModeInternal, TAG);
        }
    }

    private void dispatchOnConfigChanged() {
        Iterator<Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onConfigChanged();
        }
    }

    private void dispatchOnPolicyChanged() {
        Iterator<Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onPolicyChanged();
        }
    }

    private void dispatchOnZenModeChanged() {
        Iterator<Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onZenModeChanged();
        }
    }

    private ZenModeConfig readDefaultConfig(Resources resources) throws Throwable {
        XmlResourceParser xml;
        ZenModeConfig xml2;
        ?? r1 = 0;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                xml = resources.getXml(R.xml.config_webview_packages);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Exception e) {
            e = e;
        }
        do {
            try {
                r1 = 1;
            } catch (Exception e2) {
                e = e2;
                xmlResourceParser = xml;
                Log.w(TAG, "Error reading default zen mode config from resource", e);
                IoUtils.closeQuietly(xmlResourceParser);
                r1 = xmlResourceParser;
            } catch (Throwable th2) {
                th = th2;
                r1 = xml;
                IoUtils.closeQuietly((AutoCloseable) r1);
                throw th;
            }
            if (xml.next() == 1) {
                IoUtils.closeQuietly(xml);
                return new ZenModeConfig();
            }
            xml2 = ZenModeConfig.readXml(xml);
        } while (xml2 == null);
        IoUtils.closeQuietly(xml);
        return xml2;
    }

    private void appendDefaultEveryNightRule(ZenModeConfig zenModeConfig) {
        if (zenModeConfig == null) {
            return;
        }
        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = ZenModeConfig.ALL_DAYS;
        scheduleInfo.startHour = 22;
        scheduleInfo.endHour = 7;
        scheduleInfo.exitAtAlarm = true;
        ZenModeConfig.ZenRule zenRule = new ZenModeConfig.ZenRule();
        zenRule.enabled = false;
        zenRule.name = this.mDefaultRuleEveryNightName;
        zenRule.conditionId = ZenModeConfig.toScheduleConditionId(scheduleInfo);
        zenRule.zenMode = 1;
        zenRule.component = ScheduleConditionProvider.COMPONENT;
        zenRule.id = "EVERY_NIGHT_DEFAULT_RULE";
        zenRule.creationTime = System.currentTimeMillis();
        zenModeConfig.automaticRules.put(zenRule.id, zenRule);
    }

    private void appendDefaultEventRules(ZenModeConfig zenModeConfig) {
        if (zenModeConfig == null) {
            return;
        }
        ZenModeConfig.EventInfo eventInfo = new ZenModeConfig.EventInfo();
        eventInfo.calendar = null;
        eventInfo.reply = 1;
        ZenModeConfig.ZenRule zenRule = new ZenModeConfig.ZenRule();
        zenRule.enabled = false;
        zenRule.name = this.mDefaultRuleEventsName;
        zenRule.conditionId = ZenModeConfig.toEventConditionId(eventInfo);
        zenRule.zenMode = 1;
        zenRule.component = EventConditionProvider.COMPONENT;
        zenRule.id = "EVENTS_DEFAULT_RULE";
        zenRule.creationTime = System.currentTimeMillis();
        zenModeConfig.automaticRules.put(zenRule.id, zenRule);
    }

    private static int zenSeverity(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return 0;
        }
    }

    @VisibleForTesting
    protected final class RingerModeDelegate implements AudioManagerInternal.RingerModeDelegate {
        protected RingerModeDelegate() {
        }

        public String toString() {
            return ZenModeHelper.TAG;
        }

        public int onSetRingerModeInternal(int i, int i2, String str, int i3, VolumePolicy volumePolicy) {
            int i4;
            int i5 = 0;
            boolean z = i != i2;
            if (ZenModeHelper.this.mZenMode == 0 || (ZenModeHelper.this.mZenMode == 1 && !ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeHelper.this.mConfig))) {
                ZenModeHelper.this.setPreviousRingerModeSetting(Integer.valueOf(i2));
            }
            switch (i2) {
                case 0:
                    if (!z || !volumePolicy.doNotDisturbWhenSilent) {
                        i4 = i2;
                        i5 = -1;
                    } else {
                        if (ZenModeHelper.this.mZenMode != 0) {
                            i5 = -1;
                        } else {
                            i5 = 1;
                        }
                        ZenModeHelper.this.setPreviousRingerModeSetting(Integer.valueOf(i));
                        i4 = i2;
                    }
                    break;
                case 1:
                case 2:
                    if (!z || i != 0 || (ZenModeHelper.this.mZenMode != 2 && ZenModeHelper.this.mZenMode != 3 && (ZenModeHelper.this.mZenMode != 1 || !ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeHelper.this.mConfig)))) {
                        i4 = ZenModeHelper.this.mZenMode != 0 ? 0 : i2;
                        i5 = -1;
                    }
                    i4 = i2;
                    break;
                default:
                    i4 = i2;
                    i5 = -1;
                    break;
            }
            if (i5 != -1) {
                ZenModeHelper.this.setManualZenMode(i5, null, "ringerModeInternal", null, false);
            }
            if (z || i5 != -1 || i3 != i4) {
                ZenLog.traceSetRingerModeInternal(i, i2, str, i3, i4);
            }
            return i4;
        }

        public int onSetRingerModeExternal(int i, int i2, String str, int i3, VolumePolicy volumePolicy) {
            int i4;
            int i5;
            boolean z = i != i2;
            boolean z2 = i3 == 1;
            switch (i2) {
                case 0:
                    if (!z) {
                        i4 = i3;
                        i5 = -1;
                    } else {
                        int i6 = ZenModeHelper.this.mZenMode == 0 ? 1 : -1;
                        i4 = z2 ? 1 : 0;
                        i5 = i6;
                    }
                    break;
                case 1:
                case 2:
                    if (ZenModeHelper.this.mZenMode != 0) {
                        i5 = 0;
                        i4 = i2;
                        break;
                    }
                default:
                    i4 = i2;
                    i5 = -1;
                    break;
            }
            if (i5 != -1) {
                ZenModeHelper.this.setManualZenMode(i5, null, "ringerModeExternal", str, false);
            }
            ZenLog.traceSetRingerModeExternal(i, i2, str, i3, i4);
            return i4;
        }

        public boolean canVolumeDownEnterSilent() {
            return ZenModeHelper.this.mZenMode == 0;
        }

        public int getRingerModeAffectedStreams(int i) {
            int i2;
            int i3 = i | 38;
            if (ZenModeHelper.this.mZenMode == 2) {
                i2 = i3 | 24;
            } else {
                i2 = i3 & (-25);
            }
            if (ZenModeHelper.this.mZenMode == 1 && ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeHelper.this.mConfig)) {
                return i2 & (-3);
            }
            return i2 | 2;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE;

        public SettingsObserver(Handler handler) {
            super(handler);
            this.ZEN_MODE = Settings.Global.getUriFor("zen_mode");
        }

        public void observe() {
            ZenModeHelper.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE, false, this);
            update(null);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (this.ZEN_MODE.equals(uri) && ZenModeHelper.this.mZenMode != ZenModeHelper.this.getZenModeSetting()) {
                if (ZenModeHelper.DEBUG) {
                    Log.d(ZenModeHelper.TAG, "Fixing zen mode setting");
                }
                ZenModeHelper.this.setZenModeSetting(ZenModeHelper.this.mZenMode);
            }
        }
    }

    private void showZenUpgradeNotification(int i) {
        if ((!this.mIsBootComplete || i == 0 || Settings.Global.getInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 0) == 0) ? false : true) {
            this.mNotificationManager.notify(TAG, 48, createZenUpgradeNotification());
            Settings.Global.putInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 0);
        }
    }

    @VisibleForTesting
    protected Notification createZenUpgradeNotification() {
        int i;
        int i2;
        int i3;
        Bundle bundle = new Bundle();
        bundle.putString("android.substName", this.mContext.getResources().getString(R.string.config_defaultSearchSelectorPackageName));
        if (NotificationManager.Policy.areAllVisualEffectsSuppressed(getNotificationPolicy().suppressedVisualEffects)) {
            i = R.string.other_networks_no_internet;
            i2 = R.string.org_unit;
            i3 = R.drawable.emo_im_sad;
        } else {
            i = R.string.org_name;
            i2 = R.string.orgTypeWork;
            i3 = R.drawable.ic_media_route_connecting_holo_dark;
        }
        Intent intent = new Intent("android.settings.ZEN_MODE_ONBOARDING");
        intent.addFlags(268468224);
        return new Notification.Builder(this.mContext, SystemNotificationChannels.DO_NOT_DISTURB).setAutoCancel(true).setSmallIcon(R.drawable.ic_media_route_connected_light_17_mtrl).setLargeIcon(Icon.createWithResource(this.mContext, i3)).setContentTitle(this.mContext.getResources().getString(i)).setContentText(this.mContext.getResources().getString(i2)).setContentIntent(BenesseExtension.getDchaState() == 0 ? PendingIntent.getActivity(this.mContext, 0, intent, 134217728) : null).setAutoCancel(true).setLocalOnly(true).addExtras(bundle).setStyle(new Notification.BigTextStyle()).build();
    }

    private final class Metrics extends Callback {
        private static final String COUNTER_PREFIX = "dnd_mode_";
        private static final long MINIMUM_LOG_PERIOD_MS = 60000;
        private long mBeginningMs;
        private int mPreviousZenMode;

        private Metrics() {
            this.mPreviousZenMode = -1;
            this.mBeginningMs = 0L;
        }

        @Override
        void onZenModeChanged() {
            emit();
        }

        private void emit() {
            ZenModeHelper.this.mHandler.postMetricsTimer();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long j = jElapsedRealtime - this.mBeginningMs;
            if (this.mPreviousZenMode != ZenModeHelper.this.mZenMode || j > 60000) {
                if (this.mPreviousZenMode != -1) {
                    MetricsLogger.count(ZenModeHelper.this.mContext, COUNTER_PREFIX + this.mPreviousZenMode, (int) j);
                }
                this.mPreviousZenMode = ZenModeHelper.this.mZenMode;
                this.mBeginningMs = jElapsedRealtime;
            }
        }
    }

    private final class H extends Handler {
        private static final long METRICS_PERIOD_MS = 21600000;
        private static final int MSG_APPLY_CONFIG = 4;
        private static final int MSG_DISPATCH = 1;
        private static final int MSG_METRICS = 2;

        private final class ConfigMessageData {
            public final ZenModeConfig config;
            public final String reason;
            public final boolean setRingerMode;

            ConfigMessageData(ZenModeConfig zenModeConfig, String str, boolean z) {
                this.config = zenModeConfig;
                this.reason = str;
                this.setRingerMode = z;
            }
        }

        private H(Looper looper) {
            super(looper);
        }

        private void postDispatchOnZenModeChanged() {
            removeMessages(1);
            sendEmptyMessage(1);
        }

        private void postMetricsTimer() {
            removeMessages(2);
            sendEmptyMessageDelayed(2, METRICS_PERIOD_MS);
        }

        private void postApplyConfig(ZenModeConfig zenModeConfig, String str, boolean z) {
            sendMessage(obtainMessage(4, new ConfigMessageData(zenModeConfig, str, z)));
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 4) {
                switch (i) {
                    case 1:
                        ZenModeHelper.this.dispatchOnZenModeChanged();
                        break;
                    case 2:
                        ZenModeHelper.this.mMetrics.emit();
                        break;
                }
            }
            ConfigMessageData configMessageData = (ConfigMessageData) message.obj;
            ZenModeHelper.this.applyConfig(configMessageData.config, configMessageData.reason, configMessageData.setRingerMode);
        }
    }

    public static class Callback {
        void onConfigChanged() {
        }

        void onZenModeChanged() {
        }

        void onPolicyChanged() {
        }
    }
}
