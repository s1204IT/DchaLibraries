package com.android.server.notification;

import android.R;
import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.media.AudioManagerInternal;
import android.media.VolumePolicy;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.logging.MetricsLogger;
import com.android.server.LocalServices;
import com.android.server.notification.ManagedServices;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
    private final AppOpsManager mAppOps;
    private AudioManagerInternal mAudioManager;
    private final ZenModeConditions mConditions;
    private ZenModeConfig mConfig;
    private final Context mContext;
    private final ZenModeConfig mDefaultConfig;
    private final ZenModeFiltering mFiltering;
    private final H mHandler;
    private PackageManager mPm;
    private final ManagedServices.Config mServiceConfig;
    private final SettingsObserver mSettingsObserver;
    private long mSuppressedEffects;
    private int mZenMode;
    static final String TAG = "ZenModeHelper";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private final RingerModeDelegate mRingerModeDelegate = new RingerModeDelegate(this, null);
    private final SparseArray<ZenModeConfig> mConfigs = new SparseArray<>();
    private final Metrics mMetrics = new Metrics(this, 0 == true ? 1 : 0);
    private int mUser = 0;
    private final ZenModeConfig.Migration mConfigMigration = new ZenModeConfig.Migration() {
        public ZenModeConfig migrate(ZenModeConfig.XmlV1 v1) {
            if (v1 == null) {
                return null;
            }
            ZenModeConfig rt = new ZenModeConfig();
            rt.allowCalls = v1.allowCalls;
            rt.allowEvents = v1.allowEvents;
            rt.allowCallsFrom = v1.allowFrom;
            rt.allowMessages = v1.allowMessages;
            rt.allowMessagesFrom = v1.allowFrom;
            rt.allowReminders = v1.allowReminders;
            int[] days = ZenModeConfig.XmlV1.tryParseDays(v1.sleepMode);
            if (days != null && days.length > 0) {
                Log.i(ZenModeHelper.TAG, "Migrating existing V1 downtime to single schedule");
                ZenModeConfig.ScheduleInfo schedule = new ZenModeConfig.ScheduleInfo();
                schedule.days = days;
                schedule.startHour = v1.sleepStartHour;
                schedule.startMinute = v1.sleepStartMinute;
                schedule.endHour = v1.sleepEndHour;
                schedule.endMinute = v1.sleepEndMinute;
                ZenModeConfig.ZenRule rule = new ZenModeConfig.ZenRule();
                rule.enabled = true;
                rule.name = ZenModeHelper.this.mContext.getResources().getString(R.string.lockscreen_missing_sim_message_short);
                rule.conditionId = ZenModeConfig.toScheduleConditionId(schedule);
                rule.zenMode = v1.sleepNone ? 2 : 1;
                rule.component = ScheduleConditionProvider.COMPONENT;
                rt.automaticRules.put(ZenModeConfig.newRuleId(), rule);
            } else {
                Log.i(ZenModeHelper.TAG, "No existing V1 downtime found, generating default schedules");
                ZenModeHelper.this.appendDefaultScheduleRules(rt);
            }
            ZenModeHelper.this.appendDefaultEventRules(rt);
            return rt;
        }
    };

    public ZenModeHelper(Context context, Looper looper, ConditionProviders conditionProviders) {
        this.mContext = context;
        this.mHandler = new H(this, looper, 0 == true ? 1 : 0);
        addCallback(this.mMetrics);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mDefaultConfig = readDefaultConfig(context.getResources());
        appendDefaultScheduleRules(this.mDefaultConfig);
        appendDefaultEventRules(this.mDefaultConfig);
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

    public boolean matchesCallFilter(UserHandle userHandle, Bundle extras, ValidateNotificationPeople validator, int contactsTimeoutMs, float timeoutAffinity) {
        boolean zMatchesCallFilter;
        synchronized (this.mConfig) {
            zMatchesCallFilter = ZenModeFiltering.matchesCallFilter(this.mContext, this.mZenMode, this.mConfig, userHandle, extras, validator, contactsTimeoutMs, timeoutAffinity);
        }
        return zMatchesCallFilter;
    }

    public boolean isCall(NotificationRecord record) {
        return this.mFiltering.isCall(record);
    }

    public boolean shouldIntercept(NotificationRecord record) {
        boolean zShouldIntercept;
        synchronized (this.mConfig) {
            zShouldIntercept = this.mFiltering.shouldIntercept(this.mZenMode, this.mConfig, record);
        }
        return zShouldIntercept;
    }

    public boolean shouldSuppressWhenScreenOff() {
        boolean z;
        synchronized (this.mConfig) {
            z = !this.mConfig.allowWhenScreenOff;
        }
        return z;
    }

    public boolean shouldSuppressWhenScreenOn() {
        boolean z;
        synchronized (this.mConfig) {
            z = !this.mConfig.allowWhenScreenOn;
        }
        return z;
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
    }

    public void onUserSwitched(int user) {
        loadConfigForUser(user, "onUserSwitched");
    }

    public void onUserRemoved(int user) {
        if (user < 0) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onUserRemoved u=" + user);
        }
        this.mConfigs.remove(user);
    }

    public void onUserUnlocked(int user) {
        loadConfigForUser(user, "onUserUnlocked");
    }

    private void loadConfigForUser(int user, String reason) {
        if (this.mUser == user || user < 0) {
            return;
        }
        this.mUser = user;
        if (DEBUG) {
            Log.d(TAG, reason + " u=" + user);
        }
        ZenModeConfig config = this.mConfigs.get(user);
        if (config == null) {
            if (DEBUG) {
                Log.d(TAG, reason + " generating default config for user " + user);
            }
            config = this.mDefaultConfig.copy();
            config.user = user;
        }
        synchronized (this.mConfig) {
            setConfigLocked(config, reason);
        }
        cleanUpZenRules();
    }

    public int getZenModeListenerInterruptionFilter() {
        return NotificationManager.zenModeToInterruptionFilter(this.mZenMode);
    }

    public void requestFromListener(ComponentName name, int filter) {
        int newZen = NotificationManager.zenModeFromInterruptionFilter(filter, -1);
        if (newZen == -1) {
            return;
        }
        setManualZenMode(newZen, null, "listener:" + (name != null ? name.flattenToShortString() : null));
    }

    public void setSuppressedEffects(long suppressedEffects) {
        if (this.mSuppressedEffects == suppressedEffects) {
            return;
        }
        this.mSuppressedEffects = suppressedEffects;
        applyRestrictions();
    }

    public long getSuppressedEffects() {
        return this.mSuppressedEffects;
    }

    public int getZenMode() {
        return this.mZenMode;
    }

    public List<ZenModeConfig.ZenRule> getZenRules() {
        List<ZenModeConfig.ZenRule> rules = new ArrayList<>();
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return rules;
            }
            for (ZenModeConfig.ZenRule rule : this.mConfig.automaticRules.values()) {
                if (canManageAutomaticZenRule(rule)) {
                    rules.add(rule);
                }
            }
            return rules;
        }
    }

    public AutomaticZenRule getAutomaticZenRule(String id) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return null;
            }
            ZenModeConfig.ZenRule rule = (ZenModeConfig.ZenRule) this.mConfig.automaticRules.get(id);
            if (rule != null && canManageAutomaticZenRule(rule)) {
                return createAutomaticZenRule(rule);
            }
            return null;
        }
    }

    public String addAutomaticZenRule(AutomaticZenRule automaticZenRule, String reason) {
        String str;
        if (!isSystemRule(automaticZenRule)) {
            ServiceInfo owner = getServiceInfo(automaticZenRule.getOwner());
            if (owner == null) {
                throw new IllegalArgumentException("Owner is not a condition provider service");
            }
            int ruleInstanceLimit = -1;
            if (owner.metaData != null) {
                ruleInstanceLimit = owner.metaData.getInt("android.service.zen.automatic.ruleInstanceLimit", -1);
            }
            if (ruleInstanceLimit > 0 && ruleInstanceLimit < getCurrentInstanceCount(automaticZenRule.getOwner()) + 1) {
                throw new IllegalArgumentException("Rule instance limit exceeded");
            }
        }
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                throw new AndroidRuntimeException("Could not create rule");
            }
            if (DEBUG) {
                Log.d(TAG, "addAutomaticZenRule rule= " + automaticZenRule + " reason=" + reason);
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            ZenModeConfig.ZenRule rule = new ZenModeConfig.ZenRule();
            populateZenRule(automaticZenRule, rule, true);
            newConfig.automaticRules.put(rule.id, rule);
            if (setConfigLocked(newConfig, reason, true)) {
                str = rule.id;
            } else {
                throw new AndroidRuntimeException("Could not create rule");
            }
        }
        return str;
    }

    public boolean updateAutomaticZenRule(String ruleId, AutomaticZenRule automaticZenRule, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            if (DEBUG) {
                Log.d(TAG, "updateAutomaticZenRule zenRule=" + automaticZenRule + " reason=" + reason);
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            if (ruleId == null) {
                throw new IllegalArgumentException("Rule doesn't exist");
            }
            ZenModeConfig.ZenRule rule = (ZenModeConfig.ZenRule) newConfig.automaticRules.get(ruleId);
            if (rule == null || !canManageAutomaticZenRule(rule)) {
                throw new SecurityException("Cannot update rules not owned by your condition provider");
            }
            populateZenRule(automaticZenRule, rule, false);
            newConfig.automaticRules.put(ruleId, rule);
            return setConfigLocked(newConfig, reason, true);
        }
    }

    public boolean removeAutomaticZenRule(String id, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            ZenModeConfig.ZenRule rule = (ZenModeConfig.ZenRule) newConfig.automaticRules.get(id);
            if (rule == null) {
                return false;
            }
            if (canManageAutomaticZenRule(rule)) {
                newConfig.automaticRules.remove(id);
                if (DEBUG) {
                    Log.d(TAG, "removeZenRule zenRule=" + id + " reason=" + reason);
                }
                boolean setConfig = setConfigLocked(newConfig, reason, true);
                if (rule != null && rule.conditionId != null && rule.conditionId.toString() != null && rule.conditionId.toString().equalsIgnoreCase("scheme:/mock_cp?query_item=valueUnsubscribe")) {
                    Log.d(TAG, "foundTarget true: " + rule.conditionId.toString());
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                    }
                }
                return setConfig;
            }
            throw new SecurityException("Cannot delete rules not owned by your condition provider");
        }
    }

    public boolean removeAutomaticZenRules(String packageName, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            for (int i = newConfig.automaticRules.size() - 1; i >= 0; i--) {
                ZenModeConfig.ZenRule rule = (ZenModeConfig.ZenRule) newConfig.automaticRules.get(newConfig.automaticRules.keyAt(i));
                if (rule.component.getPackageName().equals(packageName) && canManageAutomaticZenRule(rule)) {
                    newConfig.automaticRules.removeAt(i);
                }
            }
            return setConfigLocked(newConfig, reason, true);
        }
    }

    public int getCurrentInstanceCount(ComponentName owner) {
        int count = 0;
        synchronized (this.mConfig) {
            for (ZenModeConfig.ZenRule rule : this.mConfig.automaticRules.values()) {
                if (rule.component != null && rule.component.equals(owner)) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean canManageAutomaticZenRule(ZenModeConfig.ZenRule rule) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0 || callingUid == 1000 || this.mContext.checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
            return true;
        }
        String[] packages = this.mPm.getPackagesForUid(Binder.getCallingUid());
        if (packages != null) {
            for (String str : packages) {
                if (str.equals(rule.component.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSystemRule(AutomaticZenRule rule) {
        return "android".equals(rule.getOwner().getPackageName());
    }

    private ServiceInfo getServiceInfo(ComponentName owner) {
        Intent queryIntent = new Intent();
        queryIntent.setComponent(owner);
        List<ResolveInfo> installedServices = this.mPm.queryIntentServicesAsUser(queryIntent, 132, UserHandle.getCallingUserId());
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ResolveInfo resolveInfo = installedServices.get(i);
                ServiceInfo info = resolveInfo.serviceInfo;
                if (this.mServiceConfig.bindPermission.equals(info.permission)) {
                    return info;
                }
            }
        }
        return null;
    }

    private void populateZenRule(AutomaticZenRule automaticZenRule, ZenModeConfig.ZenRule rule, boolean isNew) {
        if (isNew) {
            rule.id = ZenModeConfig.newRuleId();
            rule.creationTime = System.currentTimeMillis();
            rule.component = automaticZenRule.getOwner();
        }
        if (rule.enabled != automaticZenRule.isEnabled()) {
            rule.snoozing = false;
        }
        rule.name = automaticZenRule.getName();
        rule.condition = null;
        rule.conditionId = automaticZenRule.getConditionId();
        rule.enabled = automaticZenRule.isEnabled();
        rule.zenMode = NotificationManager.zenModeFromInterruptionFilter(automaticZenRule.getInterruptionFilter(), 0);
    }

    private AutomaticZenRule createAutomaticZenRule(ZenModeConfig.ZenRule rule) {
        return new AutomaticZenRule(rule.name, rule.component, rule.conditionId, NotificationManager.zenModeToInterruptionFilter(rule.zenMode), rule.enabled, rule.creationTime);
    }

    public void setManualZenMode(int zenMode, Uri conditionId, String reason) {
        setManualZenMode(zenMode, conditionId, reason, true);
    }

    private void setManualZenMode(int zenMode, Uri conditionId, String reason, boolean setRingerMode) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return;
            }
            if (Settings.Global.isValidZenMode(zenMode)) {
                if (DEBUG) {
                    Log.d(TAG, "setManualZenMode " + Settings.Global.zenModeToString(zenMode) + " conditionId=" + conditionId + " reason=" + reason + " setRingerMode=" + setRingerMode);
                }
                ZenModeConfig newConfig = this.mConfig.copy();
                if (zenMode == 0) {
                    newConfig.manualRule = null;
                    for (ZenModeConfig.ZenRule automaticRule : newConfig.automaticRules.values()) {
                        if (automaticRule.isAutomaticActive()) {
                            automaticRule.snoozing = true;
                        }
                    }
                } else {
                    ZenModeConfig.ZenRule newRule = new ZenModeConfig.ZenRule();
                    newRule.enabled = true;
                    newRule.zenMode = zenMode;
                    newRule.conditionId = conditionId;
                    newConfig.manualRule = newRule;
                }
                setConfigLocked(newConfig, reason, setRingerMode);
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mZenMode=");
        pw.println(Settings.Global.zenModeToString(this.mZenMode));
        dump(pw, prefix, "mDefaultConfig", this.mDefaultConfig);
        int N = this.mConfigs.size();
        for (int i = 0; i < N; i++) {
            dump(pw, prefix, "mConfigs[u=" + this.mConfigs.keyAt(i) + "]", this.mConfigs.valueAt(i));
        }
        pw.print(prefix);
        pw.print("mUser=");
        pw.println(this.mUser);
        synchronized (this.mConfig) {
            dump(pw, prefix, "mConfig", this.mConfig);
        }
        pw.print(prefix);
        pw.print("mSuppressedEffects=");
        pw.println(this.mSuppressedEffects);
        this.mFiltering.dump(pw, prefix);
        this.mConditions.dump(pw, prefix);
    }

    private static void dump(PrintWriter pw, String prefix, String var, ZenModeConfig config) {
        pw.print(prefix);
        pw.print(var);
        pw.print('=');
        if (config == null) {
            pw.println(config);
            return;
        }
        pw.printf("allow(calls=%s,callsFrom=%s,repeatCallers=%s,messages=%s,messagesFrom=%s,events=%s,reminders=%s,whenScreenOff,whenScreenOn=%s)\n", Boolean.valueOf(config.allowCalls), ZenModeConfig.sourceToString(config.allowCallsFrom), Boolean.valueOf(config.allowRepeatCallers), Boolean.valueOf(config.allowMessages), ZenModeConfig.sourceToString(config.allowMessagesFrom), Boolean.valueOf(config.allowEvents), Boolean.valueOf(config.allowReminders), Boolean.valueOf(config.allowWhenScreenOff), Boolean.valueOf(config.allowWhenScreenOn));
        pw.print(prefix);
        pw.print("  manualRule=");
        pw.println(config.manualRule);
        if (config.automaticRules.isEmpty()) {
            return;
        }
        int N = config.automaticRules.size();
        int i = 0;
        while (i < N) {
            pw.print(prefix);
            pw.print(i == 0 ? "  automaticRules=" : "                 ");
            pw.println(config.automaticRules.valueAt(i));
            i++;
        }
    }

    public void readXml(XmlPullParser parser, boolean forRestore) throws XmlPullParserException, IOException {
        ZenModeConfig config = ZenModeConfig.readXml(parser, this.mConfigMigration);
        if (config == null) {
            return;
        }
        if (forRestore) {
            if (config.user != 0) {
                return;
            }
            config.manualRule = null;
            long time = System.currentTimeMillis();
            if (config.automaticRules != null) {
                for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
                    automaticRule.snoozing = false;
                    automaticRule.condition = null;
                    automaticRule.creationTime = time;
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "readXml");
        }
        synchronized (this.mConfig) {
            setConfigLocked(config, "readXml");
        }
    }

    public void writeXml(XmlSerializer out, boolean forBackup) throws IOException {
        int N = this.mConfigs.size();
        for (int i = 0; i < N; i++) {
            if (!forBackup || this.mConfigs.keyAt(i) == 0) {
                this.mConfigs.valueAt(i).writeXml(out);
            }
        }
    }

    public NotificationManager.Policy getNotificationPolicy() {
        return getNotificationPolicy(this.mConfig);
    }

    private static NotificationManager.Policy getNotificationPolicy(ZenModeConfig config) {
        if (config == null) {
            return null;
        }
        return config.toNotificationPolicy();
    }

    public void setNotificationPolicy(NotificationManager.Policy policy) {
        if (policy == null || this.mConfig == null) {
            return;
        }
        synchronized (this.mConfig) {
            ZenModeConfig newConfig = this.mConfig.copy();
            newConfig.applyNotificationPolicy(policy);
            setConfigLocked(newConfig, "setNotificationPolicy");
        }
    }

    private void cleanUpZenRules() {
        long currentTime = System.currentTimeMillis();
        synchronized (this.mConfig) {
            ZenModeConfig newConfig = this.mConfig.copy();
            if (newConfig.automaticRules != null) {
                for (int i = newConfig.automaticRules.size() - 1; i >= 0; i--) {
                    ZenModeConfig.ZenRule rule = (ZenModeConfig.ZenRule) newConfig.automaticRules.get(newConfig.automaticRules.keyAt(i));
                    if (259200000 < currentTime - rule.creationTime) {
                        try {
                            this.mPm.getPackageInfo(rule.component.getPackageName(), PackageManagerService.DumpState.DUMP_PREFERRED_XML);
                        } catch (PackageManager.NameNotFoundException e) {
                            newConfig.automaticRules.removeAt(i);
                        }
                    }
                }
                setConfigLocked(newConfig, "cleanUpZenRules");
            } else {
                setConfigLocked(newConfig, "cleanUpZenRules");
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

    public boolean setConfigLocked(ZenModeConfig config, String reason) {
        return setConfigLocked(config, reason, true);
    }

    public void setConfigAsync(ZenModeConfig config, String reason) {
        this.mHandler.postSetConfig(config, reason);
    }

    private boolean setConfigLocked(ZenModeConfig config, String reason, boolean setRingerMode) {
        long identity = Binder.clearCallingIdentity();
        if (config != null) {
            try {
                if (config.isValid()) {
                    if (config.user != this.mUser) {
                        this.mConfigs.put(config.user, config);
                        if (DEBUG) {
                            Log.d(TAG, "setConfigLocked: store config for user " + config.user);
                        }
                        return true;
                    }
                    this.mConditions.evaluateConfig(config, false);
                    this.mConfigs.put(config.user, config);
                    if (DEBUG) {
                        Log.d(TAG, "setConfigLocked reason=" + reason, new Throwable());
                    }
                    ZenLog.traceConfig(reason, this.mConfig, config);
                    boolean policyChanged = !Objects.equals(getNotificationPolicy(this.mConfig), getNotificationPolicy(config));
                    if (!config.equals(this.mConfig)) {
                        dispatchOnConfigChanged();
                    }
                    if (policyChanged) {
                        dispatchOnPolicyChanged();
                    }
                    this.mConfig = config;
                    this.mHandler.postApplyConfig(config, reason, setRingerMode);
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        Log.w(TAG, "Invalid config in setConfigLocked; " + config);
        return false;
    }

    private void applyConfig(ZenModeConfig config, String reason, boolean setRingerMode) {
        String val = Integer.toString(config.hashCode());
        Settings.Global.putString(this.mContext.getContentResolver(), "zen_mode_config_etag", val);
        if (!evaluateZenMode(reason, setRingerMode)) {
            applyRestrictions();
        }
        this.mConditions.evaluateConfig(config, true);
    }

    private int getZenModeSetting() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
    }

    private void setZenModeSetting(int zen) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "zen_mode", zen);
    }

    private int getPreviousRingerModeSetting() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode_ringer_level", 2);
    }

    private void setPreviousRingerModeSetting(Integer previousRingerLevel) {
        Settings.Global.putString(this.mContext.getContentResolver(), "zen_mode_ringer_level", previousRingerLevel != null ? Integer.toString(previousRingerLevel.intValue()) : null);
    }

    private boolean evaluateZenMode(String reason, boolean setRingerMode) {
        if (DEBUG) {
            Log.d(TAG, "evaluateZenMode");
        }
        int zenBefore = this.mZenMode;
        int zen = computeZenMode();
        ZenLog.traceSetZenMode(zen, reason);
        this.mZenMode = zen;
        updateRingerModeAffectedStreams();
        setZenModeSetting(this.mZenMode);
        if (setRingerMode) {
            applyZenToRingerMode();
        }
        applyRestrictions();
        if (zen != zenBefore) {
            this.mHandler.postDispatchOnZenModeChanged();
            return true;
        }
        return true;
    }

    private void updateRingerModeAffectedStreams() {
        if (this.mAudioManager == null) {
            return;
        }
        this.mAudioManager.updateRingerModeAffectedStreamsInternal();
    }

    private int computeZenMode() {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return 0;
            }
            if (this.mConfig.manualRule != null) {
                return this.mConfig.manualRule.zenMode;
            }
            int zen = 0;
            for (ZenModeConfig.ZenRule automaticRule : this.mConfig.automaticRules.values()) {
                if (automaticRule.isAutomaticActive() && zenSeverity(automaticRule.zenMode) > zenSeverity(zen)) {
                    zen = automaticRule.zenMode;
                }
            }
            return zen;
        }
    }

    private void applyRestrictions() {
        boolean zen = this.mZenMode != 0;
        boolean muteNotifications = (this.mSuppressedEffects & 1) != 0;
        boolean muteCalls = ((!zen || this.mConfig.allowCalls || this.mConfig.allowRepeatCallers) && (this.mSuppressedEffects & 2) == 0) ? false : true;
        boolean muteEverything = this.mZenMode == 2;
        for (int i = 0; i <= 15; i++) {
            if (i == 5) {
                applyRestrictions(!muteNotifications ? muteEverything : true, i);
            } else if (i == 6) {
                applyRestrictions(!muteCalls ? muteEverything : true, i);
            } else {
                applyRestrictions(muteEverything, i);
            }
        }
    }

    private void applyRestrictions(boolean mute, int usage) {
        this.mAppOps.setRestriction(3, usage, mute ? 1 : 0, null);
        this.mAppOps.setRestriction(28, usage, mute ? 1 : 0, null);
    }

    private void applyZenToRingerMode() {
        if (this.mAudioManager == null) {
            return;
        }
        int ringerModeInternal = this.mAudioManager.getRingerModeInternal();
        int newRingerModeInternal = ringerModeInternal;
        switch (this.mZenMode) {
            case 0:
            case 1:
                if (ringerModeInternal == 0) {
                    newRingerModeInternal = getPreviousRingerModeSetting();
                    setPreviousRingerModeSetting(null);
                }
                break;
            case 2:
            case 3:
                if (ringerModeInternal != 0) {
                    setPreviousRingerModeSetting(Integer.valueOf(ringerModeInternal));
                    newRingerModeInternal = 0;
                }
                break;
        }
        if (newRingerModeInternal == -1) {
            return;
        }
        this.mAudioManager.setRingerModeInternal(newRingerModeInternal, TAG);
    }

    private void dispatchOnConfigChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onConfigChanged();
        }
    }

    private void dispatchOnPolicyChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onPolicyChanged();
        }
    }

    private void dispatchOnZenModeChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onZenModeChanged();
        }
    }

    private ZenModeConfig readDefaultConfig(Resources resources) {
        XmlResourceParser parser = null;
        try {
            parser = resources.getXml(R.bool.config_assistantOnTopOfDream);
            while (parser.next() != 1) {
                ZenModeConfig config = ZenModeConfig.readXml(parser, this.mConfigMigration);
                if (config != null) {
                    return config;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading default zen mode config from resource", e);
        } finally {
            IoUtils.closeQuietly(parser);
        }
        return new ZenModeConfig();
    }

    private void appendDefaultScheduleRules(ZenModeConfig config) {
        if (config == null) {
            return;
        }
        ZenModeConfig.ScheduleInfo weeknights = new ZenModeConfig.ScheduleInfo();
        weeknights.days = ZenModeConfig.WEEKNIGHT_DAYS;
        weeknights.startHour = 22;
        weeknights.endHour = 7;
        ZenModeConfig.ZenRule rule1 = new ZenModeConfig.ZenRule();
        rule1.enabled = false;
        rule1.name = this.mContext.getResources().getString(R.string.lockscreen_network_locked_message);
        rule1.conditionId = ZenModeConfig.toScheduleConditionId(weeknights);
        rule1.zenMode = 3;
        rule1.component = ScheduleConditionProvider.COMPONENT;
        rule1.id = ZenModeConfig.newRuleId();
        rule1.creationTime = System.currentTimeMillis();
        config.automaticRules.put(rule1.id, rule1);
        ZenModeConfig.ScheduleInfo weekends = new ZenModeConfig.ScheduleInfo();
        weekends.days = ZenModeConfig.WEEKEND_DAYS;
        weekends.startHour = 23;
        weekends.startMinute = 30;
        weekends.endHour = 10;
        ZenModeConfig.ZenRule rule2 = new ZenModeConfig.ZenRule();
        rule2.enabled = false;
        rule2.name = this.mContext.getResources().getString(R.string.lockscreen_password_wrong);
        rule2.conditionId = ZenModeConfig.toScheduleConditionId(weekends);
        rule2.zenMode = 3;
        rule2.component = ScheduleConditionProvider.COMPONENT;
        rule2.id = ZenModeConfig.newRuleId();
        rule2.creationTime = System.currentTimeMillis();
        config.automaticRules.put(rule2.id, rule2);
    }

    private void appendDefaultEventRules(ZenModeConfig config) {
        if (config == null) {
            return;
        }
        ZenModeConfig.EventInfo events = new ZenModeConfig.EventInfo();
        events.calendar = null;
        events.reply = 1;
        ZenModeConfig.ZenRule rule = new ZenModeConfig.ZenRule();
        rule.enabled = false;
        rule.name = this.mContext.getResources().getString(R.string.lockscreen_pattern_correct);
        rule.conditionId = ZenModeConfig.toEventConditionId(events);
        rule.zenMode = 3;
        rule.component = EventConditionProvider.COMPONENT;
        rule.id = ZenModeConfig.newRuleId();
        rule.creationTime = System.currentTimeMillis();
        config.automaticRules.put(rule.id, rule);
    }

    private static int zenSeverity(int zen) {
        switch (zen) {
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

    private final class RingerModeDelegate implements AudioManagerInternal.RingerModeDelegate {
        RingerModeDelegate(ZenModeHelper this$0, RingerModeDelegate ringerModeDelegate) {
            this();
        }

        private RingerModeDelegate() {
        }

        public String toString() {
            return ZenModeHelper.TAG;
        }

        public int onSetRingerModeInternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeExternal, VolumePolicy policy) {
            boolean isChange = ringerModeOld != ringerModeNew;
            int ringerModeExternalOut = ringerModeNew;
            int newZen = -1;
            switch (ringerModeNew) {
                case 0:
                    if (isChange && policy.doNotDisturbWhenSilent) {
                        if (ZenModeHelper.this.mZenMode != 2 && ZenModeHelper.this.mZenMode != 3) {
                            newZen = 3;
                        }
                        ZenModeHelper.this.setPreviousRingerModeSetting(Integer.valueOf(ringerModeOld));
                    }
                    break;
                case 1:
                case 2:
                    if (isChange && ringerModeOld == 0 && (ZenModeHelper.this.mZenMode == 2 || ZenModeHelper.this.mZenMode == 3)) {
                        newZen = 0;
                    } else if (ZenModeHelper.this.mZenMode != 0) {
                        ringerModeExternalOut = 0;
                    }
                    break;
            }
            if (newZen != -1) {
                ZenModeHelper.this.setManualZenMode(newZen, null, "ringerModeInternal", false);
            }
            if (isChange || newZen != -1 || ringerModeExternal != ringerModeExternalOut) {
                ZenLog.traceSetRingerModeInternal(ringerModeOld, ringerModeNew, caller, ringerModeExternal, ringerModeExternalOut);
            }
            return ringerModeExternalOut;
        }

        public int onSetRingerModeExternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeInternal, VolumePolicy policy) {
            int ringerModeInternalOut = ringerModeNew;
            boolean isChange = ringerModeOld != ringerModeNew;
            boolean isVibrate = ringerModeInternal == 1;
            int newZen = -1;
            switch (ringerModeNew) {
                case 0:
                    if (isChange) {
                        if (ZenModeHelper.this.mZenMode == 0) {
                            newZen = 3;
                        }
                        ringerModeInternalOut = !isVibrate ? 0 : 1;
                    } else {
                        ringerModeInternalOut = ringerModeInternal;
                    }
                    break;
                case 1:
                case 2:
                    if (ZenModeHelper.this.mZenMode != 0) {
                        newZen = 0;
                    }
                    break;
            }
            if (newZen != -1) {
                ZenModeHelper.this.setManualZenMode(newZen, null, "ringerModeExternal", false);
            }
            ZenLog.traceSetRingerModeExternal(ringerModeOld, ringerModeNew, caller, ringerModeInternal, ringerModeInternalOut);
            return ringerModeInternalOut;
        }

        public boolean canVolumeDownEnterSilent() {
            return ZenModeHelper.this.mZenMode == 0;
        }

        public int getRingerModeAffectedStreams(int streams) {
            int streams2 = streams | 38;
            if (ZenModeHelper.this.mZenMode == 2) {
                return streams2 | 24;
            }
            return streams2 & (-25);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE;

        public SettingsObserver(Handler handler) {
            super(handler);
            this.ZEN_MODE = Settings.Global.getUriFor("zen_mode");
        }

        public void observe() {
            ContentResolver resolver = ZenModeHelper.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.ZEN_MODE, false, this);
            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (!this.ZEN_MODE.equals(uri) || ZenModeHelper.this.mZenMode == ZenModeHelper.this.getZenModeSetting()) {
                return;
            }
            if (ZenModeHelper.DEBUG) {
                Log.d(ZenModeHelper.TAG, "Fixing zen mode setting");
            }
            ZenModeHelper.this.setZenModeSetting(ZenModeHelper.this.mZenMode);
        }
    }

    private final class Metrics extends Callback {
        private static final String COUNTER_PREFIX = "dnd_mode_";
        private static final long MINIMUM_LOG_PERIOD_MS = 60000;
        private long mBeginningMs;
        private int mPreviousZenMode;

        Metrics(ZenModeHelper this$0, Metrics metrics) {
            this();
        }

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
            long now = SystemClock.elapsedRealtime();
            long since = now - this.mBeginningMs;
            if (this.mPreviousZenMode == ZenModeHelper.this.mZenMode && since <= MINIMUM_LOG_PERIOD_MS) {
                return;
            }
            if (this.mPreviousZenMode != -1) {
                MetricsLogger.count(ZenModeHelper.this.mContext, COUNTER_PREFIX + this.mPreviousZenMode, (int) since);
            }
            this.mPreviousZenMode = ZenModeHelper.this.mZenMode;
            this.mBeginningMs = now;
        }
    }

    private final class H extends Handler {
        private static final long METRICS_PERIOD_MS = 21600000;
        private static final int MSG_APPLY_CONFIG = 4;
        private static final int MSG_DISPATCH = 1;
        private static final int MSG_METRICS = 2;
        private static final int MSG_SET_CONFIG = 3;

        H(ZenModeHelper this$0, Looper looper, H h) {
            this(looper);
        }

        private final class ConfigMessageData {
            public final ZenModeConfig config;
            public final String reason;
            public final boolean setRingerMode;

            ConfigMessageData(ZenModeConfig config, String reason) {
                this.config = config;
                this.reason = reason;
                this.setRingerMode = false;
            }

            ConfigMessageData(ZenModeConfig config, String reason, boolean setRingerMode) {
                this.config = config;
                this.reason = reason;
                this.setRingerMode = setRingerMode;
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

        private void postSetConfig(ZenModeConfig config, String reason) {
            sendMessage(obtainMessage(3, new ConfigMessageData(config, reason)));
        }

        private void postApplyConfig(ZenModeConfig config, String reason, boolean setRingerMode) {
            sendMessage(obtainMessage(4, new ConfigMessageData(config, reason, setRingerMode)));
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ZenModeHelper.this.dispatchOnZenModeChanged();
                    return;
                case 2:
                    ZenModeHelper.this.mMetrics.emit();
                    return;
                case 3:
                    ConfigMessageData configData = (ConfigMessageData) msg.obj;
                    synchronized (ZenModeHelper.this.mConfig) {
                        ZenModeHelper.this.setConfigLocked(configData.config, configData.reason);
                    }
                    return;
                case 4:
                    ConfigMessageData applyConfigData = (ConfigMessageData) msg.obj;
                    ZenModeHelper.this.applyConfig(applyConfigData.config, applyConfigData.reason, applyConfigData.setRingerMode);
                    return;
                default:
                    return;
            }
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
