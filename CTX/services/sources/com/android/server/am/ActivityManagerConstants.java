package com.android.server.am;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Slog;
import com.android.server.backup.BackupAgentTimeoutParameters;
import java.io.PrintWriter;

final class ActivityManagerConstants extends ContentObserver {
    private static final long DEFAULT_BACKGROUND_SETTLE_TIME = 60000;
    private static final long DEFAULT_BG_START_TIMEOUT = 15000;
    private static final int DEFAULT_BOUND_SERVICE_CRASH_MAX_RETRY = 16;
    private static final long DEFAULT_BOUND_SERVICE_CRASH_RESTART_DURATION = 1800000;
    private static final long DEFAULT_CONTENT_PROVIDER_RETAIN_TIME = 20000;
    private static final long DEFAULT_FGSERVICE_MIN_REPORT_TIME = 3000;
    private static final long DEFAULT_FGSERVICE_MIN_SHOWN_TIME = 2000;
    private static final long DEFAULT_FGSERVICE_SCREEN_ON_AFTER_TIME = 5000;
    private static final long DEFAULT_FGSERVICE_SCREEN_ON_BEFORE_TIME = 1000;
    private static final long DEFAULT_FULL_PSS_LOWERED_INTERVAL = 300000;
    private static final long DEFAULT_FULL_PSS_MIN_INTERVAL = 1200000;
    private static final long DEFAULT_GC_MIN_INTERVAL = 60000;
    private static final long DEFAULT_GC_TIMEOUT = 5000;
    private static final int DEFAULT_MAX_CACHED_PROCESSES = 32;
    private static final long DEFAULT_MAX_SERVICE_INACTIVITY = 1800000;
    private static final long DEFAULT_POWER_CHECK_INTERVAL;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_1 = 25;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_2 = 25;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_3 = 10;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_4 = 2;
    private static final boolean DEFAULT_PROCESS_START_ASYNC = true;
    private static final long DEFAULT_SERVICE_MIN_RESTART_TIME_BETWEEN = 10000;
    private static final long DEFAULT_SERVICE_RESET_RUN_DURATION = 60000;
    private static final long DEFAULT_SERVICE_RESTART_DURATION = 1000;
    private static final int DEFAULT_SERVICE_RESTART_DURATION_FACTOR = 4;
    private static final long DEFAULT_SERVICE_USAGE_INTERACTION_TIME = 1800000;
    private static final long DEFAULT_USAGE_STATS_INTERACTION_INTERVAL = 7200000;
    private static final String KEY_BACKGROUND_SETTLE_TIME = "background_settle_time";
    static final String KEY_BG_START_TIMEOUT = "service_bg_start_timeout";
    static final String KEY_BOUND_SERVICE_CRASH_MAX_RETRY = "service_crash_max_retry";
    static final String KEY_BOUND_SERVICE_CRASH_RESTART_DURATION = "service_crash_restart_duration";
    private static final String KEY_CONTENT_PROVIDER_RETAIN_TIME = "content_provider_retain_time";
    private static final String KEY_FGSERVICE_MIN_REPORT_TIME = "fgservice_min_report_time";
    private static final String KEY_FGSERVICE_MIN_SHOWN_TIME = "fgservice_min_shown_time";
    private static final String KEY_FGSERVICE_SCREEN_ON_AFTER_TIME = "fgservice_screen_on_after_time";
    private static final String KEY_FGSERVICE_SCREEN_ON_BEFORE_TIME = "fgservice_screen_on_before_time";
    private static final String KEY_FULL_PSS_LOWERED_INTERVAL = "full_pss_lowered_interval";
    private static final String KEY_FULL_PSS_MIN_INTERVAL = "full_pss_min_interval";
    private static final String KEY_GC_MIN_INTERVAL = "gc_min_interval";
    private static final String KEY_GC_TIMEOUT = "gc_timeout";
    private static final String KEY_MAX_CACHED_PROCESSES = "max_cached_processes";
    static final String KEY_MAX_SERVICE_INACTIVITY = "service_max_inactivity";
    private static final String KEY_POWER_CHECK_INTERVAL = "power_check_interval";
    private static final String KEY_POWER_CHECK_MAX_CPU_1 = "power_check_max_cpu_1";
    private static final String KEY_POWER_CHECK_MAX_CPU_2 = "power_check_max_cpu_2";
    private static final String KEY_POWER_CHECK_MAX_CPU_3 = "power_check_max_cpu_3";
    private static final String KEY_POWER_CHECK_MAX_CPU_4 = "power_check_max_cpu_4";
    static final String KEY_PROCESS_START_ASYNC = "process_start_async";
    static final String KEY_SERVICE_MIN_RESTART_TIME_BETWEEN = "service_min_restart_time_between";
    static final String KEY_SERVICE_RESET_RUN_DURATION = "service_reset_run_duration";
    static final String KEY_SERVICE_RESTART_DURATION = "service_restart_duration";
    static final String KEY_SERVICE_RESTART_DURATION_FACTOR = "service_restart_duration_factor";
    private static final String KEY_SERVICE_USAGE_INTERACTION_TIME = "service_usage_interaction_time";
    private static final String KEY_USAGE_STATS_INTERACTION_INTERVAL = "usage_stats_interaction_interval";
    public long BACKGROUND_SETTLE_TIME;
    public long BG_START_TIMEOUT;
    public long BOUND_SERVICE_CRASH_RESTART_DURATION;
    public long BOUND_SERVICE_MAX_CRASH_RETRY;
    long CONTENT_PROVIDER_RETAIN_TIME;
    public int CUR_MAX_CACHED_PROCESSES;
    public int CUR_MAX_EMPTY_PROCESSES;
    public int CUR_TRIM_CACHED_PROCESSES;
    public int CUR_TRIM_EMPTY_PROCESSES;
    public long FGSERVICE_MIN_REPORT_TIME;
    public long FGSERVICE_MIN_SHOWN_TIME;
    public long FGSERVICE_SCREEN_ON_AFTER_TIME;
    public long FGSERVICE_SCREEN_ON_BEFORE_TIME;
    public boolean FLAG_PROCESS_START_ASYNC;
    long FULL_PSS_LOWERED_INTERVAL;
    long FULL_PSS_MIN_INTERVAL;
    long GC_MIN_INTERVAL;
    long GC_TIMEOUT;
    public int MAX_CACHED_PROCESSES;
    public long MAX_SERVICE_INACTIVITY;
    long POWER_CHECK_INTERVAL;
    int POWER_CHECK_MAX_CPU_1;
    int POWER_CHECK_MAX_CPU_2;
    int POWER_CHECK_MAX_CPU_3;
    int POWER_CHECK_MAX_CPU_4;
    public long SERVICE_MIN_RESTART_TIME_BETWEEN;
    public long SERVICE_RESET_RUN_DURATION;
    public long SERVICE_RESTART_DURATION;
    public int SERVICE_RESTART_DURATION_FACTOR;
    long SERVICE_USAGE_INTERACTION_TIME;
    long USAGE_STATS_INTERACTION_INTERVAL;
    private int mOverrideMaxCachedProcesses;
    private final KeyValueListParser mParser;
    private ContentResolver mResolver;
    private final ActivityManagerService mService;

    static {
        DEFAULT_POWER_CHECK_INTERVAL = (ActivityManagerDebugConfig.DEBUG_POWER_QUICK ? 1 : 5) * 60 * 1000;
    }

    public ActivityManagerConstants(ActivityManagerService activityManagerService, Handler handler) {
        super(handler);
        this.MAX_CACHED_PROCESSES = 32;
        this.BACKGROUND_SETTLE_TIME = 60000L;
        this.FGSERVICE_MIN_SHOWN_TIME = DEFAULT_FGSERVICE_MIN_SHOWN_TIME;
        this.FGSERVICE_MIN_REPORT_TIME = DEFAULT_FGSERVICE_MIN_REPORT_TIME;
        this.FGSERVICE_SCREEN_ON_BEFORE_TIME = 1000L;
        this.FGSERVICE_SCREEN_ON_AFTER_TIME = 5000L;
        this.CONTENT_PROVIDER_RETAIN_TIME = DEFAULT_CONTENT_PROVIDER_RETAIN_TIME;
        this.GC_TIMEOUT = 5000L;
        this.GC_MIN_INTERVAL = 60000L;
        this.FULL_PSS_MIN_INTERVAL = DEFAULT_FULL_PSS_MIN_INTERVAL;
        this.FULL_PSS_LOWERED_INTERVAL = 300000L;
        this.POWER_CHECK_INTERVAL = DEFAULT_POWER_CHECK_INTERVAL;
        this.POWER_CHECK_MAX_CPU_1 = 25;
        this.POWER_CHECK_MAX_CPU_2 = 25;
        this.POWER_CHECK_MAX_CPU_3 = 10;
        this.POWER_CHECK_MAX_CPU_4 = 2;
        this.SERVICE_USAGE_INTERACTION_TIME = BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS;
        this.USAGE_STATS_INTERACTION_INTERVAL = 7200000L;
        this.SERVICE_RESTART_DURATION = 1000L;
        this.SERVICE_RESET_RUN_DURATION = 60000L;
        this.SERVICE_RESTART_DURATION_FACTOR = 4;
        this.SERVICE_MIN_RESTART_TIME_BETWEEN = 10000L;
        this.MAX_SERVICE_INACTIVITY = BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS;
        this.BG_START_TIMEOUT = DEFAULT_BG_START_TIMEOUT;
        this.BOUND_SERVICE_CRASH_RESTART_DURATION = BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS;
        this.BOUND_SERVICE_MAX_CRASH_RETRY = 16L;
        this.FLAG_PROCESS_START_ASYNC = true;
        this.mParser = new KeyValueListParser(',');
        this.mOverrideMaxCachedProcesses = -1;
        this.mService = activityManagerService;
        updateMaxCachedProcesses();
    }

    public void start(ContentResolver contentResolver) {
        this.mResolver = contentResolver;
        this.mResolver.registerContentObserver(Settings.Global.getUriFor("activity_manager_constants"), false, this);
        updateConstants();
    }

    public void setOverrideMaxCachedProcesses(int i) {
        this.mOverrideMaxCachedProcesses = i;
        updateMaxCachedProcesses();
    }

    public int getOverrideMaxCachedProcesses() {
        return this.mOverrideMaxCachedProcesses;
    }

    public static int computeEmptyProcessLimit(int i) {
        return i / 2;
    }

    @Override
    public void onChange(boolean z, Uri uri) {
        updateConstants();
    }

    private void updateConstants() {
        String string = Settings.Global.getString(this.mResolver, "activity_manager_constants");
        synchronized (this.mService) {
            try {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mParser.setString(string);
                } catch (IllegalArgumentException e) {
                    Slog.e("ActivityManagerConstants", "Bad activity manager config settings", e);
                }
                this.MAX_CACHED_PROCESSES = this.mParser.getInt(KEY_MAX_CACHED_PROCESSES, 32);
                this.BACKGROUND_SETTLE_TIME = this.mParser.getLong(KEY_BACKGROUND_SETTLE_TIME, 60000L);
                this.FGSERVICE_MIN_SHOWN_TIME = this.mParser.getLong(KEY_FGSERVICE_MIN_SHOWN_TIME, DEFAULT_FGSERVICE_MIN_SHOWN_TIME);
                this.FGSERVICE_MIN_REPORT_TIME = this.mParser.getLong(KEY_FGSERVICE_MIN_REPORT_TIME, DEFAULT_FGSERVICE_MIN_REPORT_TIME);
                this.FGSERVICE_SCREEN_ON_BEFORE_TIME = this.mParser.getLong(KEY_FGSERVICE_SCREEN_ON_BEFORE_TIME, 1000L);
                this.FGSERVICE_SCREEN_ON_AFTER_TIME = this.mParser.getLong(KEY_FGSERVICE_SCREEN_ON_AFTER_TIME, 5000L);
                this.CONTENT_PROVIDER_RETAIN_TIME = this.mParser.getLong(KEY_CONTENT_PROVIDER_RETAIN_TIME, DEFAULT_CONTENT_PROVIDER_RETAIN_TIME);
                this.GC_TIMEOUT = this.mParser.getLong(KEY_GC_TIMEOUT, 5000L);
                this.GC_MIN_INTERVAL = this.mParser.getLong(KEY_GC_MIN_INTERVAL, 60000L);
                this.FULL_PSS_MIN_INTERVAL = this.mParser.getLong(KEY_FULL_PSS_MIN_INTERVAL, DEFAULT_FULL_PSS_MIN_INTERVAL);
                this.FULL_PSS_LOWERED_INTERVAL = this.mParser.getLong(KEY_FULL_PSS_LOWERED_INTERVAL, 300000L);
                this.POWER_CHECK_INTERVAL = this.mParser.getLong(KEY_POWER_CHECK_INTERVAL, DEFAULT_POWER_CHECK_INTERVAL);
                this.POWER_CHECK_MAX_CPU_1 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_1, 25);
                this.POWER_CHECK_MAX_CPU_2 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_2, 25);
                this.POWER_CHECK_MAX_CPU_3 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_3, 10);
                this.POWER_CHECK_MAX_CPU_4 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_4, 2);
                this.SERVICE_USAGE_INTERACTION_TIME = this.mParser.getLong(KEY_SERVICE_USAGE_INTERACTION_TIME, BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.USAGE_STATS_INTERACTION_INTERVAL = this.mParser.getLong(KEY_USAGE_STATS_INTERACTION_INTERVAL, 7200000L);
                this.SERVICE_RESTART_DURATION = this.mParser.getLong(KEY_SERVICE_RESTART_DURATION, 1000L);
                this.SERVICE_RESET_RUN_DURATION = this.mParser.getLong(KEY_SERVICE_RESET_RUN_DURATION, 60000L);
                this.SERVICE_RESTART_DURATION_FACTOR = this.mParser.getInt(KEY_SERVICE_RESTART_DURATION_FACTOR, 4);
                this.SERVICE_MIN_RESTART_TIME_BETWEEN = this.mParser.getLong(KEY_SERVICE_MIN_RESTART_TIME_BETWEEN, 10000L);
                this.MAX_SERVICE_INACTIVITY = this.mParser.getLong(KEY_MAX_SERVICE_INACTIVITY, BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.BG_START_TIMEOUT = this.mParser.getLong(KEY_BG_START_TIMEOUT, DEFAULT_BG_START_TIMEOUT);
                this.BOUND_SERVICE_CRASH_RESTART_DURATION = this.mParser.getLong(KEY_BOUND_SERVICE_CRASH_RESTART_DURATION, BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.BOUND_SERVICE_MAX_CRASH_RETRY = this.mParser.getInt(KEY_BOUND_SERVICE_CRASH_MAX_RETRY, 16);
                this.FLAG_PROCESS_START_ASYNC = this.mParser.getBoolean(KEY_PROCESS_START_ASYNC, true);
                updateMaxCachedProcesses();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void updateMaxCachedProcesses() {
        this.CUR_MAX_CACHED_PROCESSES = this.mOverrideMaxCachedProcesses < 0 ? this.MAX_CACHED_PROCESSES : this.mOverrideMaxCachedProcesses;
        this.CUR_MAX_EMPTY_PROCESSES = computeEmptyProcessLimit(this.CUR_MAX_CACHED_PROCESSES);
        int iComputeEmptyProcessLimit = computeEmptyProcessLimit(this.MAX_CACHED_PROCESSES);
        this.CUR_TRIM_EMPTY_PROCESSES = iComputeEmptyProcessLimit / 2;
        this.CUR_TRIM_CACHED_PROCESSES = (this.MAX_CACHED_PROCESSES - iComputeEmptyProcessLimit) / 3;
    }

    void dump(PrintWriter printWriter) {
        printWriter.println("ACTIVITY MANAGER SETTINGS (dumpsys activity settings) activity_manager_constants:");
        printWriter.print("  ");
        printWriter.print(KEY_MAX_CACHED_PROCESSES);
        printWriter.print("=");
        printWriter.println(this.MAX_CACHED_PROCESSES);
        printWriter.print("  ");
        printWriter.print(KEY_BACKGROUND_SETTLE_TIME);
        printWriter.print("=");
        printWriter.println(this.BACKGROUND_SETTLE_TIME);
        printWriter.print("  ");
        printWriter.print(KEY_FGSERVICE_MIN_SHOWN_TIME);
        printWriter.print("=");
        printWriter.println(this.FGSERVICE_MIN_SHOWN_TIME);
        printWriter.print("  ");
        printWriter.print(KEY_FGSERVICE_MIN_REPORT_TIME);
        printWriter.print("=");
        printWriter.println(this.FGSERVICE_MIN_REPORT_TIME);
        printWriter.print("  ");
        printWriter.print(KEY_FGSERVICE_SCREEN_ON_BEFORE_TIME);
        printWriter.print("=");
        printWriter.println(this.FGSERVICE_SCREEN_ON_BEFORE_TIME);
        printWriter.print("  ");
        printWriter.print(KEY_FGSERVICE_SCREEN_ON_AFTER_TIME);
        printWriter.print("=");
        printWriter.println(this.FGSERVICE_SCREEN_ON_AFTER_TIME);
        printWriter.print("  ");
        printWriter.print(KEY_CONTENT_PROVIDER_RETAIN_TIME);
        printWriter.print("=");
        printWriter.println(this.CONTENT_PROVIDER_RETAIN_TIME);
        printWriter.print("  ");
        printWriter.print(KEY_GC_TIMEOUT);
        printWriter.print("=");
        printWriter.println(this.GC_TIMEOUT);
        printWriter.print("  ");
        printWriter.print(KEY_GC_MIN_INTERVAL);
        printWriter.print("=");
        printWriter.println(this.GC_MIN_INTERVAL);
        printWriter.print("  ");
        printWriter.print(KEY_FULL_PSS_MIN_INTERVAL);
        printWriter.print("=");
        printWriter.println(this.FULL_PSS_MIN_INTERVAL);
        printWriter.print("  ");
        printWriter.print(KEY_FULL_PSS_LOWERED_INTERVAL);
        printWriter.print("=");
        printWriter.println(this.FULL_PSS_LOWERED_INTERVAL);
        printWriter.print("  ");
        printWriter.print(KEY_POWER_CHECK_INTERVAL);
        printWriter.print("=");
        printWriter.println(this.POWER_CHECK_INTERVAL);
        printWriter.print("  ");
        printWriter.print(KEY_POWER_CHECK_MAX_CPU_1);
        printWriter.print("=");
        printWriter.println(this.POWER_CHECK_MAX_CPU_1);
        printWriter.print("  ");
        printWriter.print(KEY_POWER_CHECK_MAX_CPU_2);
        printWriter.print("=");
        printWriter.println(this.POWER_CHECK_MAX_CPU_2);
        printWriter.print("  ");
        printWriter.print(KEY_POWER_CHECK_MAX_CPU_3);
        printWriter.print("=");
        printWriter.println(this.POWER_CHECK_MAX_CPU_3);
        printWriter.print("  ");
        printWriter.print(KEY_POWER_CHECK_MAX_CPU_4);
        printWriter.print("=");
        printWriter.println(this.POWER_CHECK_MAX_CPU_4);
        printWriter.print("  ");
        printWriter.print(KEY_SERVICE_USAGE_INTERACTION_TIME);
        printWriter.print("=");
        printWriter.println(this.SERVICE_USAGE_INTERACTION_TIME);
        printWriter.print("  ");
        printWriter.print(KEY_USAGE_STATS_INTERACTION_INTERVAL);
        printWriter.print("=");
        printWriter.println(this.USAGE_STATS_INTERACTION_INTERVAL);
        printWriter.print("  ");
        printWriter.print(KEY_SERVICE_RESTART_DURATION);
        printWriter.print("=");
        printWriter.println(this.SERVICE_RESTART_DURATION);
        printWriter.print("  ");
        printWriter.print(KEY_SERVICE_RESET_RUN_DURATION);
        printWriter.print("=");
        printWriter.println(this.SERVICE_RESET_RUN_DURATION);
        printWriter.print("  ");
        printWriter.print(KEY_SERVICE_RESTART_DURATION_FACTOR);
        printWriter.print("=");
        printWriter.println(this.SERVICE_RESTART_DURATION_FACTOR);
        printWriter.print("  ");
        printWriter.print(KEY_SERVICE_MIN_RESTART_TIME_BETWEEN);
        printWriter.print("=");
        printWriter.println(this.SERVICE_MIN_RESTART_TIME_BETWEEN);
        printWriter.print("  ");
        printWriter.print(KEY_MAX_SERVICE_INACTIVITY);
        printWriter.print("=");
        printWriter.println(this.MAX_SERVICE_INACTIVITY);
        printWriter.print("  ");
        printWriter.print(KEY_BG_START_TIMEOUT);
        printWriter.print("=");
        printWriter.println(this.BG_START_TIMEOUT);
        printWriter.println();
        if (this.mOverrideMaxCachedProcesses >= 0) {
            printWriter.print("  mOverrideMaxCachedProcesses=");
            printWriter.println(this.mOverrideMaxCachedProcesses);
        }
        printWriter.print("  CUR_MAX_CACHED_PROCESSES=");
        printWriter.println(this.CUR_MAX_CACHED_PROCESSES);
        printWriter.print("  CUR_MAX_EMPTY_PROCESSES=");
        printWriter.println(this.CUR_MAX_EMPTY_PROCESSES);
        printWriter.print("  CUR_TRIM_EMPTY_PROCESSES=");
        printWriter.println(this.CUR_TRIM_EMPTY_PROCESSES);
        printWriter.print("  CUR_TRIM_CACHED_PROCESSES=");
        printWriter.println(this.CUR_TRIM_CACHED_PROCESSES);
    }
}
