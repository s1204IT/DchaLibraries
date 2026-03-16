package com.android.commands.am;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityContainer;
import android.app.IActivityContainerCallback;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IInstrumentationWatcher;
import android.app.IStopUserCallback;
import android.app.ProfilerInfo;
import android.app.UiAutomationConnection;
import android.app.usage.ConfigurationStats;
import android.app.usage.IUsageStatsManager;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.AndroidException;
import android.util.ArrayMap;
import android.view.IWindowManager;
import com.android.internal.os.BaseCommand;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class Am extends BaseCommand {
    private IActivityManager mAm;
    private boolean mAutoStop;
    private String mProfileFile;
    private String mReceiverPermission;
    private int mSamplingInterval;
    private int mUserId;
    private int mStartFlags = 0;
    private boolean mWaitOption = false;
    private boolean mStopOption = false;
    private int mRepeat = 0;

    public static void main(String[] args) {
        new Am().run(args);
    }

    public void onShowUsage(PrintStream out) {
        out.println("usage: am [subcommand] [options]\nusage: am start [-D] [-W] [-P <FILE>] [--start-profiler <FILE>]\n               [--sampling INTERVAL] [-R COUNT] [-S] [--opengl-trace]\n               [--user <USER_ID> | current] <INTENT>\n       am startservice [--user <USER_ID> | current] <INTENT>\n       am stopservice [--user <USER_ID> | current] <INTENT>\n       am force-stop [--user <USER_ID> | all | current] <PACKAGE>\n       am kill [--user <USER_ID> | all | current] <PACKAGE>\n       am kill-all\n       am broadcast [--user <USER_ID> | all | current] <INTENT>\n       am instrument [-r] [-e <NAME> <VALUE>] [-p <FILE>] [-w]\n               [--user <USER_ID> | current]\n               [--no-window-animation] [--abi <ABI>] <COMPONENT>\n       am profile start [--user <USER_ID> current] <PROCESS> <FILE>\n       am profile stop [--user <USER_ID> current] [<PROCESS>]\n       am dumpheap [--user <USER_ID> current] [-n] <PROCESS> <FILE>\n       am set-debug-app [-w] [--persistent] <PACKAGE>\n       am clear-debug-app\n       am monitor [--gdb <port>]\n       am hang [--allow-restart]\n       am restart\n       am idle-maintenance\n       am screen-compat [on|off] <PACKAGE>\n       am to-uri [INTENT]\n       am to-intent-uri [INTENT]\n       am to-app-uri [INTENT]\n       am switch-user <USER_ID>\n       am start-user <USER_ID>\n       am stop-user <USER_ID>\n       am stack start <DISPLAY_ID> <INTENT>\n       am stack movetask <TASK_ID> <STACK_ID> [true|false]\n       am stack resize <STACK_ID> <LEFT,TOP,RIGHT,BOTTOM>\n       am stack list\n       am stack info <STACK_ID>\n       am lock-task <TASK_ID>\n       am lock-task stop\n       am get-config\n\nam start: start an Activity.  Options are:\n    -D: enable debugging\n    -W: wait for launch to complete\n    --start-profiler <FILE>: start profiler and send results to <FILE>\n    --sampling INTERVAL: use sample profiling with INTERVAL microseconds\n        between samples (use with --start-profiler)\n    -P <FILE>: like above, but profiling stops when app goes idle\n    -R: repeat the activity launch <COUNT> times.  Prior to each repeat,\n        the top activity will be finished.\n    -S: force stop the target app before starting the activity\n    --opengl-trace: enable tracing of OpenGL functions\n    --user <USER_ID> | current: Specify which user to run as; if not\n        specified then run as the current user.\n\nam startservice: start a Service.  Options are:\n    --user <USER_ID> | current: Specify which user to run as; if not\n        specified then run as the current user.\n\nam stopservice: stop a Service.  Options are:\n    --user <USER_ID> | current: Specify which user to run as; if not\n        specified then run as the current user.\n\nam force-stop: force stop everything associated with <PACKAGE>.\n    --user <USER_ID> | all | current: Specify user to force stop;\n        all users if not specified.\n\nam kill: Kill all processes associated with <PACKAGE>.  Only kills.\n  processes that are safe to kill -- that is, will not impact the user\n  experience.\n    --user <USER_ID> | all | current: Specify user whose processes to kill;\n        all users if not specified.\n\nam kill-all: Kill all background processes.\n\nam broadcast: send a broadcast Intent.  Options are:\n    --user <USER_ID> | all | current: Specify which user to send to; if not\n        specified then send to all users.\n    --receiver-permission <PERMISSION>: Require receiver to hold permission.\n\nam instrument: start an Instrumentation.  Typically this target <COMPONENT>\n  is the form <TEST_PACKAGE>/<RUNNER_CLASS>.  Options are:\n    -r: print raw results (otherwise decode REPORT_KEY_STREAMRESULT).  Use with\n        [-e perf true] to generate raw output for performance measurements.\n    -e <NAME> <VALUE>: set argument <NAME> to <VALUE>.  For test runners a\n        common form is [-e <testrunner_flag> <value>[,<value>...]].\n    -p <FILE>: write profiling data to <FILE>\n    -w: wait for instrumentation to finish before returning.  Required for\n        test runners.\n    --user <USER_ID> | current: Specify user instrumentation runs in;\n        current user if not specified.\n    --no-window-animation: turn off window animations while running.\n    --abi <ABI>: Launch the instrumented process with the selected ABI.\n        This assumes that the process supports the selected ABI.\n\nam profile: start and stop profiler on a process.  The given <PROCESS> argument\n  may be either a process name or pid.  Options are:\n    --user <USER_ID> | current: When supplying a process name,\n        specify user of process to profile; uses current user if not specified.\n\nam dumpheap: dump the heap of a process.  The given <PROCESS> argument may\n  be either a process name or pid.  Options are:\n    -n: dump native heap instead of managed heap\n    --user <USER_ID> | current: When supplying a process name,\n        specify user of process to dump; uses current user if not specified.\n\nam set-debug-app: set application <PACKAGE> to debug.  Options are:\n    -w: wait for debugger when application starts\n    --persistent: retain this value\n\nam clear-debug-app: clear the previously set-debug-app.\n\nam bug-report: request bug report generation; will launch UI\n    when done to select where it should be delivered.\n\nam monitor: start monitoring for crashes or ANRs.\n    --gdb: start gdbserv on the given port at crash/ANR\n\nam hang: hang the system.\n    --allow-restart: allow watchdog to perform normal system restart\n\nam restart: restart the user-space system.\n\nam idle-maintenance: perform idle maintenance now.\n\nam screen-compat: control screen compatibility mode of <PACKAGE>.\n\nam to-uri: print the given Intent specification as a URI.\n\nam to-intent-uri: print the given Intent specification as an intent: URI.\n\nam to-app-uri: print the given Intent specification as an android-app: URI.\n\nam switch-user: switch to put USER_ID in the foreground, starting\n  execution of that user if it is currently stopped.\n\nam start-user: start USER_ID in background if it is currently stopped,\n  use switch-user if you want to start the user in foreground.\n\nam stop-user: stop execution of USER_ID, not allowing it to run any\n  code until a later explicit start or switch to it.\n\nam stack start: start a new activity on <DISPLAY_ID> using <INTENT>.\n\nam stack movetask: move <TASK_ID> from its current stack to the top (true) or   bottom (false) of <STACK_ID>.\n\nam stack resize: change <STACK_ID> size and position to <LEFT,TOP,RIGHT,BOTTOM>.\n\nam stack list: list all of the activity stacks and their sizes.\n\nam stack info: display the information about activity stack <STACK_ID>.\n\nam lock-task: bring <TASK_ID> to the front and don't allow other tasks to run\n\nam get-config: retrieve the configuration and any recent configurations\n  of the device\n\n<INTENT> specifications include these flags and arguments:\n    [-a <ACTION>] [-d <DATA_URI>] [-t <MIME_TYPE>]\n    [-c <CATEGORY> [-c <CATEGORY>] ...]\n    [-e|--es <EXTRA_KEY> <EXTRA_STRING_VALUE> ...]\n    [--esn <EXTRA_KEY> ...]\n    [--ez <EXTRA_KEY> <EXTRA_BOOLEAN_VALUE> ...]\n    [--ei <EXTRA_KEY> <EXTRA_INT_VALUE> ...]\n    [--el <EXTRA_KEY> <EXTRA_LONG_VALUE> ...]\n    [--ef <EXTRA_KEY> <EXTRA_FLOAT_VALUE> ...]\n    [--eu <EXTRA_KEY> <EXTRA_URI_VALUE> ...]\n    [--ecn <EXTRA_KEY> <EXTRA_COMPONENT_NAME_VALUE>]\n    [--eia <EXTRA_KEY> <EXTRA_INT_VALUE>[,<EXTRA_INT_VALUE...]]\n    [--ela <EXTRA_KEY> <EXTRA_LONG_VALUE>[,<EXTRA_LONG_VALUE...]]\n    [--efa <EXTRA_KEY> <EXTRA_FLOAT_VALUE>[,<EXTRA_FLOAT_VALUE...]]\n    [--esa <EXTRA_KEY> <EXTRA_STRING_VALUE>[,<EXTRA_STRING_VALUE...]]\n        (to embed a comma into a string escape it using \"\\,\")\n    [-n <COMPONENT>] [-p <PACKAGE>] [-f <FLAGS>]\n    [--grant-read-uri-permission] [--grant-write-uri-permission]\n    [--grant-persistable-uri-permission] [--grant-prefix-uri-permission]\n    [--debug-log-resolution] [--exclude-stopped-packages]\n    [--include-stopped-packages]\n    [--activity-brought-to-front] [--activity-clear-top]\n    [--activity-clear-when-task-reset] [--activity-exclude-from-recents]\n    [--activity-launched-from-history] [--activity-multiple-task]\n    [--activity-no-animation] [--activity-no-history]\n    [--activity-no-user-action] [--activity-previous-is-top]\n    [--activity-reorder-to-front] [--activity-reset-task-if-needed]\n    [--activity-single-top] [--activity-clear-task]\n    [--activity-task-on-home]\n    [--receiver-registered-only] [--receiver-replace-pending]\n    [--selector]\n    [<URI> | <PACKAGE> | <COMPONENT>]\n");
    }

    public void onRun() throws Exception {
        this.mAm = ActivityManagerNative.getDefault();
        if (this.mAm == null) {
            System.err.println("Error type 2");
            throw new AndroidException("Can't connect to activity manager; is the system running?");
        }
        String op = nextArgRequired();
        if (op.equals("start")) {
            runStart();
            return;
        }
        if (op.equals("startservice")) {
            runStartService();
            return;
        }
        if (op.equals("stopservice")) {
            runStopService();
            return;
        }
        if (op.equals("force-stop")) {
            runForceStop();
            return;
        }
        if (op.equals("kill")) {
            runKill();
            return;
        }
        if (op.equals("kill-all")) {
            runKillAll();
            return;
        }
        if (op.equals("instrument")) {
            runInstrument();
            return;
        }
        if (op.equals("broadcast")) {
            sendBroadcast();
            return;
        }
        if (op.equals("profile")) {
            runProfile();
            return;
        }
        if (op.equals("dumpheap")) {
            runDumpHeap();
            return;
        }
        if (op.equals("set-debug-app")) {
            runSetDebugApp();
            return;
        }
        if (op.equals("clear-debug-app")) {
            runClearDebugApp();
            return;
        }
        if (op.equals("bug-report")) {
            runBugReport();
            return;
        }
        if (op.equals("monitor")) {
            runMonitor();
            return;
        }
        if (op.equals("hang")) {
            runHang();
            return;
        }
        if (op.equals("restart")) {
            runRestart();
            return;
        }
        if (op.equals("idle-maintenance")) {
            runIdleMaintenance();
            return;
        }
        if (op.equals("screen-compat")) {
            runScreenCompat();
            return;
        }
        if (op.equals("to-uri")) {
            runToUri(0);
            return;
        }
        if (op.equals("to-intent-uri")) {
            runToUri(1);
            return;
        }
        if (op.equals("to-app-uri")) {
            runToUri(2);
            return;
        }
        if (op.equals("switch-user")) {
            runSwitchUser();
            return;
        }
        if (op.equals("start-user")) {
            runStartUserInBackground();
            return;
        }
        if (op.equals("stop-user")) {
            runStopUser();
            return;
        }
        if (op.equals("stack")) {
            runStack();
            return;
        }
        if (op.equals("lock-task")) {
            runLockTask();
        } else if (op.equals("get-config")) {
            runGetConfig();
        } else {
            showError("Error: unknown command '" + op + "'");
        }
    }

    int parseUserArg(String arg) {
        if ("all".equals(arg)) {
            return -1;
        }
        if ("current".equals(arg) || "cur".equals(arg)) {
            return -2;
        }
        int userId = Integer.parseInt(arg);
        return userId;
    }

    private Intent makeIntent(int defUser) throws URISyntaxException {
        boolean arg;
        Intent intent = new Intent();
        boolean hasIntentInfo = false;
        this.mStartFlags = 0;
        this.mWaitOption = false;
        this.mStopOption = false;
        this.mRepeat = 0;
        this.mProfileFile = null;
        this.mSamplingInterval = 0;
        this.mAutoStop = false;
        this.mUserId = defUser;
        Uri data = null;
        String type = null;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-a")) {
                    intent.setAction(nextArgRequired());
                    if (intent == intent) {
                        hasIntentInfo = true;
                    }
                } else if (opt.equals("-d")) {
                    data = Uri.parse(nextArgRequired());
                    if (intent == intent) {
                        hasIntentInfo = true;
                    }
                } else if (opt.equals("-t")) {
                    type = nextArgRequired();
                    if (intent == intent) {
                        hasIntentInfo = true;
                    }
                } else if (opt.equals("-c")) {
                    intent.addCategory(nextArgRequired());
                    if (intent == intent) {
                        hasIntentInfo = true;
                    }
                } else if (opt.equals("-e") || opt.equals("--es")) {
                    String key = nextArgRequired();
                    String value = nextArgRequired();
                    intent.putExtra(key, value);
                } else if (opt.equals("--esn")) {
                    String key2 = nextArgRequired();
                    intent.putExtra(key2, (String) null);
                } else if (opt.equals("--ei")) {
                    String key3 = nextArgRequired();
                    String value2 = nextArgRequired();
                    intent.putExtra(key3, Integer.decode(value2));
                } else if (opt.equals("--eu")) {
                    String key4 = nextArgRequired();
                    String value3 = nextArgRequired();
                    intent.putExtra(key4, Uri.parse(value3));
                } else if (opt.equals("--ecn")) {
                    String key5 = nextArgRequired();
                    String value4 = nextArgRequired();
                    ComponentName cn = ComponentName.unflattenFromString(value4);
                    if (cn == null) {
                        throw new IllegalArgumentException("Bad component name: " + value4);
                    }
                    intent.putExtra(key5, cn);
                } else if (opt.equals("--eia")) {
                    String key6 = nextArgRequired();
                    String value5 = nextArgRequired();
                    String[] strings = value5.split(",");
                    int[] list = new int[strings.length];
                    for (int i = 0; i < strings.length; i++) {
                        list[i] = Integer.decode(strings[i]).intValue();
                    }
                    intent.putExtra(key6, list);
                } else if (opt.equals("--el")) {
                    String key7 = nextArgRequired();
                    String value6 = nextArgRequired();
                    intent.putExtra(key7, Long.valueOf(value6));
                } else if (opt.equals("--ela")) {
                    String key8 = nextArgRequired();
                    String value7 = nextArgRequired();
                    String[] strings2 = value7.split(",");
                    long[] list2 = new long[strings2.length];
                    for (int i2 = 0; i2 < strings2.length; i2++) {
                        list2[i2] = Long.valueOf(strings2[i2]).longValue();
                    }
                    intent.putExtra(key8, list2);
                    hasIntentInfo = true;
                } else if (opt.equals("--ef")) {
                    String key9 = nextArgRequired();
                    String value8 = nextArgRequired();
                    intent.putExtra(key9, Float.valueOf(value8));
                    hasIntentInfo = true;
                } else if (opt.equals("--efa")) {
                    String key10 = nextArgRequired();
                    String value9 = nextArgRequired();
                    String[] strings3 = value9.split(",");
                    float[] list3 = new float[strings3.length];
                    for (int i3 = 0; i3 < strings3.length; i3++) {
                        list3[i3] = Float.valueOf(strings3[i3]).floatValue();
                    }
                    intent.putExtra(key10, list3);
                    hasIntentInfo = true;
                } else if (opt.equals("--esa")) {
                    String key11 = nextArgRequired();
                    String value10 = nextArgRequired();
                    intent.putExtra(key11, value10.split("(?<!\\\\),"));
                    hasIntentInfo = true;
                } else if (opt.equals("--ez")) {
                    String key12 = nextArgRequired();
                    String value11 = nextArgRequired().toLowerCase();
                    if ("true".equals(value11) || "t".equals(value11)) {
                        arg = true;
                    } else if ("false".equals(value11) || "f".equals(value11)) {
                        arg = false;
                    } else {
                        try {
                            arg = Integer.decode(value11).intValue() != 0;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid boolean value: " + value11);
                        }
                    }
                    intent.putExtra(key12, arg);
                } else if (opt.equals("-n")) {
                    String str = nextArgRequired();
                    ComponentName cn2 = ComponentName.unflattenFromString(str);
                    if (cn2 == null) {
                        throw new IllegalArgumentException("Bad component name: " + str);
                    }
                    intent.setComponent(cn2);
                    if (intent == intent) {
                        hasIntentInfo = true;
                    }
                } else if (opt.equals("-p")) {
                    intent.setPackage(nextArgRequired());
                    if (intent == intent) {
                        hasIntentInfo = true;
                    }
                } else if (opt.equals("-f")) {
                    intent.setFlags(Integer.decode(nextArgRequired()).intValue());
                } else if (opt.equals("--grant-read-uri-permission")) {
                    intent.addFlags(1);
                } else if (opt.equals("--grant-write-uri-permission")) {
                    intent.addFlags(2);
                } else if (opt.equals("--grant-persistable-uri-permission")) {
                    intent.addFlags(64);
                } else if (opt.equals("--grant-prefix-uri-permission")) {
                    intent.addFlags(128);
                } else if (opt.equals("--exclude-stopped-packages")) {
                    intent.addFlags(16);
                } else if (opt.equals("--include-stopped-packages")) {
                    intent.addFlags(32);
                } else if (opt.equals("--debug-log-resolution")) {
                    intent.addFlags(8);
                } else if (opt.equals("--activity-brought-to-front")) {
                    intent.addFlags(4194304);
                } else if (opt.equals("--activity-clear-top")) {
                    intent.addFlags(67108864);
                } else if (opt.equals("--activity-clear-when-task-reset")) {
                    intent.addFlags(524288);
                } else if (opt.equals("--activity-exclude-from-recents")) {
                    intent.addFlags(8388608);
                } else if (opt.equals("--activity-launched-from-history")) {
                    intent.addFlags(1048576);
                } else if (opt.equals("--activity-multiple-task")) {
                    intent.addFlags(134217728);
                } else if (opt.equals("--activity-no-animation")) {
                    intent.addFlags(65536);
                } else if (opt.equals("--activity-no-history")) {
                    intent.addFlags(1073741824);
                } else if (opt.equals("--activity-no-user-action")) {
                    intent.addFlags(262144);
                } else if (opt.equals("--activity-previous-is-top")) {
                    intent.addFlags(16777216);
                } else if (opt.equals("--activity-reorder-to-front")) {
                    intent.addFlags(131072);
                } else if (opt.equals("--activity-reset-task-if-needed")) {
                    intent.addFlags(2097152);
                } else if (opt.equals("--activity-single-top")) {
                    intent.addFlags(536870912);
                } else if (opt.equals("--activity-clear-task")) {
                    intent.addFlags(32768);
                } else if (opt.equals("--activity-task-on-home")) {
                    intent.addFlags(16384);
                } else if (opt.equals("--receiver-registered-only")) {
                    intent.addFlags(1073741824);
                } else if (opt.equals("--receiver-replace-pending")) {
                    intent.addFlags(536870912);
                } else if (opt.equals("--selector")) {
                    intent.setDataAndType(data, type);
                    intent = new Intent();
                } else if (opt.equals("-D")) {
                    this.mStartFlags |= 2;
                } else if (opt.equals("-W")) {
                    this.mWaitOption = true;
                } else if (opt.equals("-P")) {
                    this.mProfileFile = nextArgRequired();
                    this.mAutoStop = true;
                } else if (opt.equals("--start-profiler")) {
                    this.mProfileFile = nextArgRequired();
                    this.mAutoStop = false;
                } else if (opt.equals("--sampling")) {
                    this.mSamplingInterval = Integer.parseInt(nextArgRequired());
                } else if (opt.equals("-R")) {
                    this.mRepeat = Integer.parseInt(nextArgRequired());
                } else if (opt.equals("-S")) {
                    this.mStopOption = true;
                } else if (opt.equals("--opengl-trace")) {
                    this.mStartFlags |= 4;
                } else if (opt.equals("--user")) {
                    this.mUserId = parseUserArg(nextArgRequired());
                } else if (opt.equals("--receiver-permission")) {
                    this.mReceiverPermission = nextArgRequired();
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return null;
                }
            } else {
                intent.setDataAndType(data, type);
                boolean hasSelector = intent != intent;
                if (hasSelector) {
                    intent.setSelector(intent);
                    intent = intent;
                }
                String arg2 = nextArg();
                Intent baseIntent = null;
                if (arg2 == null) {
                    if (hasSelector) {
                        baseIntent = new Intent("android.intent.action.MAIN");
                        baseIntent.addCategory("android.intent.category.LAUNCHER");
                    }
                } else if (arg2.indexOf(58) >= 0) {
                    baseIntent = Intent.parseUri(arg2, 7);
                } else if (arg2.indexOf(47) >= 0) {
                    baseIntent = new Intent("android.intent.action.MAIN");
                    baseIntent.addCategory("android.intent.category.LAUNCHER");
                    baseIntent.setComponent(ComponentName.unflattenFromString(arg2));
                } else {
                    baseIntent = new Intent("android.intent.action.MAIN");
                    baseIntent.addCategory("android.intent.category.LAUNCHER");
                    baseIntent.setPackage(arg2);
                }
                if (baseIntent != null) {
                    Bundle extras = intent.getExtras();
                    intent.replaceExtras((Bundle) null);
                    Bundle uriExtras = baseIntent.getExtras();
                    baseIntent.replaceExtras((Bundle) null);
                    if (intent.getAction() != null && baseIntent.getCategories() != null) {
                        HashSet<String> cats = new HashSet<>(baseIntent.getCategories());
                        for (String c : cats) {
                            baseIntent.removeCategory(c);
                        }
                    }
                    intent.fillIn(baseIntent, 72);
                    if (extras == null) {
                        extras = uriExtras;
                    } else if (uriExtras != null) {
                        uriExtras.putAll(extras);
                        extras = uriExtras;
                    }
                    intent.replaceExtras(extras);
                    hasIntentInfo = true;
                }
                if (hasIntentInfo) {
                    return intent;
                }
                throw new IllegalArgumentException("No intent supplied");
            }
        }
    }

    private void runStartService() throws Exception {
        Intent intent = makeIntent(-2);
        if (this.mUserId == -1) {
            System.err.println("Error: Can't start activity with user 'all'");
            return;
        }
        System.out.println("Starting service: " + intent);
        ComponentName cn = this.mAm.startService((IApplicationThread) null, intent, intent.getType(), this.mUserId);
        if (cn == null) {
            System.err.println("Error: Not found; no service started.");
        } else if (cn.getPackageName().equals("!")) {
            System.err.println("Error: Requires permission " + cn.getClassName());
        } else if (cn.getPackageName().equals("!!")) {
            System.err.println("Error: " + cn.getClassName());
        }
    }

    private void runStopService() throws Exception {
        Intent intent = makeIntent(-2);
        if (this.mUserId == -1) {
            System.err.println("Error: Can't stop activity with user 'all'");
            return;
        }
        System.out.println("Stopping service: " + intent);
        int result = this.mAm.stopService((IApplicationThread) null, intent, intent.getType(), this.mUserId);
        if (result == 0) {
            System.err.println("Service not stopped: was not running.");
        } else if (result == 1) {
            System.err.println("Service stopped");
        } else if (result == -1) {
            System.err.println("Error stopping service");
        }
    }

    private void runStart() throws Exception {
        int res;
        String packageName;
        Intent intent = makeIntent(-2);
        if (this.mUserId == -1) {
            System.err.println("Error: Can't start service with user 'all'");
            return;
        }
        String mimeType = intent.getType();
        if (mimeType == null && intent.getData() != null && "content".equals(intent.getData().getScheme())) {
            mimeType = this.mAm.getProviderMimeType(intent.getData(), this.mUserId);
        }
        do {
            if (this.mStopOption) {
                if (intent.getComponent() != null) {
                    packageName = intent.getComponent().getPackageName();
                } else {
                    IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                    if (pm == null) {
                        System.err.println("Error: Package manager not running; aborting");
                        return;
                    }
                    List<ResolveInfo> activities = pm.queryIntentActivities(intent, mimeType, 0, this.mUserId);
                    if (activities == null || activities.size() <= 0) {
                        System.err.println("Error: Intent does not match any activities: " + intent);
                        return;
                    } else {
                        if (activities.size() > 1) {
                            System.err.println("Error: Intent matches multiple activities; can't stop: " + intent);
                            return;
                        }
                        packageName = activities.get(0).activityInfo.packageName;
                    }
                }
                System.out.println("Stopping: " + packageName);
                this.mAm.forceStopPackage(packageName, this.mUserId);
                Thread.sleep(250L);
            }
            System.out.println("Starting: " + intent);
            intent.addFlags(268435456);
            ProfilerInfo profilerInfo = null;
            if (this.mProfileFile != null) {
                try {
                    ParcelFileDescriptor fd = openForSystemServer(new File(this.mProfileFile), 1006632960);
                    profilerInfo = new ProfilerInfo(this.mProfileFile, fd, this.mSamplingInterval, this.mAutoStop);
                } catch (FileNotFoundException e) {
                    System.err.println("Error: Unable to open file: " + this.mProfileFile);
                    System.err.println("Consider using a file under /data/local/tmp/");
                    return;
                }
            }
            IActivityManager.WaitResult result = null;
            long startTime = SystemClock.uptimeMillis();
            if (this.mWaitOption) {
                result = this.mAm.startActivityAndWait((IApplicationThread) null, (String) null, intent, mimeType, (IBinder) null, (String) null, 0, this.mStartFlags, profilerInfo, (Bundle) null, this.mUserId);
                res = result.result;
            } else {
                res = this.mAm.startActivityAsUser((IApplicationThread) null, (String) null, intent, mimeType, (IBinder) null, (String) null, 0, this.mStartFlags, profilerInfo, (Bundle) null, this.mUserId);
            }
            long endTime = SystemClock.uptimeMillis();
            PrintStream out = this.mWaitOption ? System.out : System.err;
            boolean launched = false;
            switch (res) {
                case -7:
                    out.println("Error: Activity not started, voice control not allowed for: " + intent);
                    break;
                case -6:
                case -5:
                default:
                    out.println("Error: Activity not started, unknown error code " + res);
                    break;
                case -4:
                    out.println("Error: Activity not started, you do not have permission to access it.");
                    break;
                case -3:
                    out.println("Error: Activity not started, you requested to both forward and receive its result");
                    break;
                case -2:
                    out.println("Error type 3");
                    out.println("Error: Activity class " + intent.getComponent().toShortString() + " does not exist.");
                    break;
                case -1:
                    out.println("Error: Activity not started, unable to resolve " + intent.toString());
                    break;
                case 0:
                    launched = true;
                    break;
                case 1:
                    launched = true;
                    out.println("Warning: Activity not started because intent should be handled by the caller");
                    break;
                case 2:
                    launched = true;
                    out.println("Warning: Activity not started, its current task has been brought to the front");
                    break;
                case 3:
                    launched = true;
                    out.println("Warning: Activity not started, intent has been delivered to currently running top-most instance.");
                    break;
                case 4:
                    launched = true;
                    out.println("Warning: Activity not started because the  current activity is being kept for the user.");
                    break;
            }
            if (this.mWaitOption && launched) {
                if (result == null) {
                    result = new IActivityManager.WaitResult();
                    result.who = intent.getComponent();
                }
                System.out.println("Status: " + (result.timeout ? "timeout" : "ok"));
                if (result.who != null) {
                    System.out.println("Activity: " + result.who.flattenToShortString());
                }
                if (result.thisTime >= 0) {
                    System.out.println("ThisTime: " + result.thisTime);
                }
                if (result.totalTime >= 0) {
                    System.out.println("TotalTime: " + result.totalTime);
                }
                System.out.println("WaitTime: " + (endTime - startTime));
                System.out.println("Complete");
            }
            this.mRepeat--;
            if (this.mRepeat > 1) {
                this.mAm.unhandledBack();
            }
        } while (this.mRepeat > 1);
    }

    private void runForceStop() throws Exception {
        int userId = -1;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("--user")) {
                    userId = parseUserArg(nextArgRequired());
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return;
                }
            } else {
                this.mAm.forceStopPackage(nextArgRequired(), userId);
                return;
            }
        }
    }

    private void runKill() throws Exception {
        int userId = -1;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("--user")) {
                    userId = parseUserArg(nextArgRequired());
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return;
                }
            } else {
                this.mAm.killBackgroundProcesses(nextArgRequired(), userId);
                return;
            }
        }
    }

    private void runKillAll() throws Exception {
        this.mAm.killAllBackgroundProcesses();
    }

    private void sendBroadcast() throws Exception {
        Intent intent = makeIntent(-2);
        IntentReceiver receiver = new IntentReceiver();
        System.out.println("Broadcasting: " + intent);
        this.mAm.broadcastIntent((IApplicationThread) null, intent, (String) null, receiver, 0, (String) null, (Bundle) null, this.mReceiverPermission, -1, true, false, this.mUserId);
        receiver.waitForFinish();
    }

    private void runInstrument() throws Exception {
        String profileFile = null;
        boolean wait = false;
        boolean rawMode = false;
        boolean no_window_animation = false;
        int userId = -2;
        Bundle args = new Bundle();
        IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        String abi = null;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-p")) {
                    profileFile = nextArgRequired();
                } else if (opt.equals("-w")) {
                    wait = true;
                } else if (opt.equals("-r")) {
                    rawMode = true;
                } else if (opt.equals("-e")) {
                    String argKey = nextArgRequired();
                    String argValue = nextArgRequired();
                    args.putString(argKey, argValue);
                } else if (opt.equals("--no_window_animation") || opt.equals("--no-window-animation")) {
                    no_window_animation = true;
                } else if (opt.equals("--user")) {
                    userId = parseUserArg(nextArgRequired());
                } else if (opt.equals("--abi")) {
                    abi = nextArgRequired();
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return;
                }
            } else {
                if (userId == -1) {
                    System.err.println("Error: Can't start instrumentation with user 'all'");
                    return;
                }
                String cnArg = nextArgRequired();
                ComponentName cn = ComponentName.unflattenFromString(cnArg);
                if (cn == null) {
                    throw new IllegalArgumentException("Bad component name: " + cnArg);
                }
                InstrumentationWatcher watcher = null;
                UiAutomationConnection connection = null;
                if (wait) {
                    watcher = new InstrumentationWatcher();
                    watcher.setRawOutput(rawMode);
                    connection = new UiAutomationConnection();
                }
                float[] oldAnims = null;
                if (no_window_animation) {
                    oldAnims = wm.getAnimationScales();
                    wm.setAnimationScale(0, 0.0f);
                    wm.setAnimationScale(1, 0.0f);
                }
                if (abi != null) {
                    String[] supportedAbis = Build.SUPPORTED_ABIS;
                    boolean matched = false;
                    int len$ = supportedAbis.length;
                    int i$ = 0;
                    while (true) {
                        if (i$ >= len$) {
                            break;
                        }
                        String supportedAbi = supportedAbis[i$];
                        if (!supportedAbi.equals(abi)) {
                            i$++;
                        } else {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        throw new AndroidException("INSTRUMENTATION_FAILED: Unsupported instruction set " + abi);
                    }
                }
                if (!this.mAm.startInstrumentation(cn, profileFile, 0, args, watcher, connection, userId, abi)) {
                    throw new AndroidException("INSTRUMENTATION_FAILED: " + cn.flattenToString());
                }
                if (watcher != null && !watcher.waitForFinish()) {
                    System.out.println("INSTRUMENTATION_ABORTED: System has crashed.");
                }
                if (oldAnims != null) {
                    wm.setAnimationScales(oldAnims);
                    return;
                }
                return;
            }
        }
    }

    static void removeWallOption() {
        String props = SystemProperties.get("dalvik.vm.extra-opts");
        if (props != null && props.contains("-Xprofile:wallclock")) {
            SystemProperties.set("dalvik.vm.extra-opts", props.replace("-Xprofile:wallclock", "").trim());
        }
    }

    private void runProfile() throws Exception {
        String process;
        boolean start = false;
        boolean wall = false;
        int userId = -2;
        String cmd = nextArgRequired();
        if ("start".equals(cmd)) {
            start = true;
            while (true) {
                String opt = nextOption();
                if (opt != null) {
                    if (opt.equals("--user")) {
                        userId = parseUserArg(nextArgRequired());
                    } else if (opt.equals("--wall")) {
                        wall = true;
                    } else {
                        System.err.println("Error: Unknown option: " + opt);
                        return;
                    }
                } else {
                    process = nextArgRequired();
                    break;
                }
            }
        } else if ("stop".equals(cmd)) {
            while (true) {
                String opt2 = nextOption();
                if (opt2 != null) {
                    if (opt2.equals("--user")) {
                        userId = parseUserArg(nextArgRequired());
                    } else {
                        System.err.println("Error: Unknown option: " + opt2);
                        return;
                    }
                } else {
                    process = nextArg();
                    break;
                }
            }
        } else {
            process = cmd;
            String cmd2 = nextArgRequired();
            if ("start".equals(cmd2)) {
                start = true;
            } else if (!"stop".equals(cmd2)) {
                throw new IllegalArgumentException("Profile command " + process + " not valid");
            }
        }
        if (userId == -1) {
            System.err.println("Error: Can't profile with user 'all'");
            return;
        }
        ProfilerInfo profilerInfo = null;
        if (start) {
            String profileFile = nextArgRequired();
            try {
                ParcelFileDescriptor fd = openForSystemServer(new File(profileFile), 1006632960);
                profilerInfo = new ProfilerInfo(profileFile, fd, 0, false);
            } catch (FileNotFoundException e) {
                System.err.println("Error: Unable to open file: " + profileFile);
                System.err.println("Consider using a file under /data/local/tmp/");
                return;
            }
        }
        if (wall) {
            try {
                String props = SystemProperties.get("dalvik.vm.extra-opts");
                if (props == null || !props.contains("-Xprofile:wallclock")) {
                    String str = props + " -Xprofile:wallclock";
                }
            } catch (Throwable th) {
                if (!wall) {
                }
                throw th;
            }
        } else if (start) {
        }
        if (!this.mAm.profileControl(process, userId, start, profilerInfo, 0)) {
            wall = false;
            throw new AndroidException("PROFILE FAILED on process " + process);
        }
        if (!wall) {
        }
    }

    private void runDumpHeap() throws Exception {
        boolean managed = true;
        int userId = -2;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("--user")) {
                    userId = parseUserArg(nextArgRequired());
                    if (userId == -1) {
                        System.err.println("Error: Can't dump heap with user 'all'");
                        return;
                    }
                } else if (opt.equals("-n")) {
                    managed = false;
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return;
                }
            } else {
                String process = nextArgRequired();
                String heapFile = nextArgRequired();
                try {
                    File file = new File(heapFile);
                    file.delete();
                    ParcelFileDescriptor fd = openForSystemServer(file, 1006632960);
                    if (!this.mAm.dumpHeap(process, userId, managed, heapFile, fd)) {
                        throw new AndroidException("HEAP DUMP FAILED on process " + process);
                    }
                    return;
                } catch (FileNotFoundException e) {
                    System.err.println("Error: Unable to open file: " + heapFile);
                    System.err.println("Consider using a file under /data/local/tmp/");
                    return;
                }
            }
        }
    }

    private void runSetDebugApp() throws Exception {
        boolean wait = false;
        boolean persistent = false;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-w")) {
                    wait = true;
                } else if (opt.equals("--persistent")) {
                    persistent = true;
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return;
                }
            } else {
                String pkg = nextArgRequired();
                this.mAm.setDebugApp(pkg, wait, persistent);
                return;
            }
        }
    }

    private void runClearDebugApp() throws Exception {
        this.mAm.setDebugApp((String) null, false, true);
    }

    private void runBugReport() throws Exception {
        this.mAm.requestBugReport();
        System.out.println("Your lovely bug report is being created; please be patient.");
    }

    private void runSwitchUser() throws Exception {
        String user = nextArgRequired();
        this.mAm.switchUser(Integer.parseInt(user));
    }

    private void runStartUserInBackground() throws Exception {
        String user = nextArgRequired();
        boolean success = this.mAm.startUserInBackground(Integer.parseInt(user));
        if (success) {
            System.out.println("Success: user started");
        } else {
            System.err.println("Error: could not start user");
        }
    }

    private void runStopUser() throws Exception {
        String user = nextArgRequired();
        int res = this.mAm.stopUser(Integer.parseInt(user), (IStopUserCallback) null);
        if (res != 0) {
            String txt = "";
            switch (res) {
                case -2:
                    txt = " (Can't stop current user)";
                    break;
                case -1:
                    txt = " (Unknown user " + user + ")";
                    break;
            }
            System.err.println("Switch failed: " + res + txt);
        }
    }

    class MyActivityController extends IActivityController.Stub {
        static final int RESULT_ANR_DIALOG = 0;
        static final int RESULT_ANR_KILL = 1;
        static final int RESULT_ANR_WAIT = 1;
        static final int RESULT_CRASH_DIALOG = 0;
        static final int RESULT_CRASH_KILL = 1;
        static final int RESULT_DEFAULT = 0;
        static final int RESULT_EARLY_ANR_CONTINUE = 0;
        static final int RESULT_EARLY_ANR_KILL = 1;
        static final int STATE_ANR = 3;
        static final int STATE_CRASHED = 1;
        static final int STATE_EARLY_ANR = 2;
        static final int STATE_NORMAL = 0;
        final String mGdbPort;
        Process mGdbProcess;
        Thread mGdbThread;
        boolean mGotGdbPrint;
        int mResult;
        int mState;

        MyActivityController(String gdbPort) {
            this.mGdbPort = gdbPort;
        }

        public boolean activityResuming(String pkg) {
            synchronized (this) {
                System.out.println("** Activity resuming: " + pkg);
            }
            return true;
        }

        public boolean activityStarting(Intent intent, String pkg) {
            synchronized (this) {
                System.out.println("** Activity starting: " + pkg);
            }
            return true;
        }

        public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) {
            boolean z;
            synchronized (this) {
                System.out.println("** ERROR: PROCESS CRASHED");
                System.out.println("processName: " + processName);
                System.out.println("processPid: " + pid);
                System.out.println("shortMsg: " + shortMsg);
                System.out.println("longMsg: " + longMsg);
                System.out.println("timeMillis: " + timeMillis);
                System.out.println("stack:");
                System.out.print(stackTrace);
                System.out.println("#");
                int result = waitControllerLocked(pid, 1);
                z = result != 1;
            }
            return z;
        }

        public int appEarlyNotResponding(String processName, int pid, String annotation) {
            int i;
            synchronized (this) {
                System.out.println("** ERROR: EARLY PROCESS NOT RESPONDING");
                System.out.println("processName: " + processName);
                System.out.println("processPid: " + pid);
                System.out.println("annotation: " + annotation);
                int result = waitControllerLocked(pid, STATE_EARLY_ANR);
                i = result == 1 ? -1 : 0;
            }
            return i;
        }

        public int appNotResponding(String processName, int pid, String processStats) {
            int i = 1;
            synchronized (this) {
                System.out.println("** ERROR: PROCESS NOT RESPONDING");
                System.out.println("processName: " + processName);
                System.out.println("processPid: " + pid);
                System.out.println("processStats:");
                System.out.print(processStats);
                System.out.println("#");
                int result = waitControllerLocked(pid, STATE_ANR);
                if (result == 1) {
                    i = -1;
                } else if (result != 1) {
                    i = 0;
                }
            }
            return i;
        }

        public int systemNotResponding(String message) {
            synchronized (this) {
                System.out.println("** ERROR: PROCESS NOT RESPONDING");
                System.out.println("message: " + message);
                System.out.println("#");
                System.out.println("Allowing system to die.");
            }
            return -1;
        }

        void killGdbLocked() {
            this.mGotGdbPrint = false;
            if (this.mGdbProcess != null) {
                System.out.println("Stopping gdbserver");
                this.mGdbProcess.destroy();
                this.mGdbProcess = null;
            }
            if (this.mGdbThread != null) {
                this.mGdbThread.interrupt();
                this.mGdbThread = null;
            }
        }

        int waitControllerLocked(int pid, int state) {
            if (this.mGdbPort != null) {
                killGdbLocked();
                try {
                    System.out.println("Starting gdbserver on port " + this.mGdbPort);
                    System.out.println("Do the following:");
                    System.out.println("  adb forward tcp:" + this.mGdbPort + " tcp:" + this.mGdbPort);
                    System.out.println("  gdbclient app_process :" + this.mGdbPort);
                    this.mGdbProcess = Runtime.getRuntime().exec(new String[]{"gdbserver", ":" + this.mGdbPort, "--attach", Integer.toString(pid)});
                    final InputStreamReader converter = new InputStreamReader(this.mGdbProcess.getInputStream());
                    this.mGdbThread = new Thread() {
                        @Override
                        public void run() {
                            BufferedReader in = new BufferedReader(converter);
                            int count = 0;
                            while (true) {
                                synchronized (MyActivityController.this) {
                                    if (MyActivityController.this.mGdbThread != null) {
                                        if (count == MyActivityController.STATE_EARLY_ANR) {
                                            MyActivityController.this.mGotGdbPrint = true;
                                            MyActivityController.this.notifyAll();
                                        }
                                        try {
                                            String line = in.readLine();
                                            if (line != null) {
                                                System.out.println("GDB: " + line);
                                                count++;
                                            } else {
                                                return;
                                            }
                                        } catch (IOException e) {
                                            return;
                                        }
                                    } else {
                                        return;
                                    }
                                }
                            }
                        }
                    };
                    this.mGdbThread.start();
                    try {
                        wait(500L);
                    } catch (InterruptedException e) {
                    }
                } catch (IOException e2) {
                    System.err.println("Failure starting gdbserver: " + e2);
                    killGdbLocked();
                }
            }
            this.mState = state;
            System.out.println("");
            printMessageForState();
            while (this.mState != 0) {
                try {
                    wait();
                } catch (InterruptedException e3) {
                }
            }
            killGdbLocked();
            return this.mResult;
        }

        void resumeController(int result) {
            synchronized (this) {
                this.mState = 0;
                this.mResult = result;
                notifyAll();
            }
        }

        void printMessageForState() {
            switch (this.mState) {
                case 0:
                    System.out.println("Monitoring activity manager...  available commands:");
                    break;
                case 1:
                    System.out.println("Waiting after crash...  available commands:");
                    System.out.println("(c)ontinue: show crash dialog");
                    System.out.println("(k)ill: immediately kill app");
                    break;
                case STATE_EARLY_ANR:
                    System.out.println("Waiting after early ANR...  available commands:");
                    System.out.println("(c)ontinue: standard ANR processing");
                    System.out.println("(k)ill: immediately kill app");
                    break;
                case STATE_ANR:
                    System.out.println("Waiting after ANR...  available commands:");
                    System.out.println("(c)ontinue: show ANR dialog");
                    System.out.println("(k)ill: immediately kill app");
                    System.out.println("(w)ait: wait some more");
                    break;
            }
            System.out.println("(q)uit: finish monitoring");
        }

        void run() throws RemoteException {
            try {
                printMessageForState();
                Am.this.mAm.setActivityController(this);
                this.mState = 0;
                InputStreamReader converter = new InputStreamReader(System.in);
                BufferedReader in = new BufferedReader(converter);
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    boolean addNewline = true;
                    if (line.length() <= 0) {
                        addNewline = false;
                    } else {
                        if ("q".equals(line) || "quit".equals(line)) {
                            break;
                        }
                        if (this.mState == 1) {
                            if ("c".equals(line) || "continue".equals(line)) {
                                resumeController(0);
                            } else if ("k".equals(line) || "kill".equals(line)) {
                                resumeController(1);
                            } else {
                                System.out.println("Invalid command: " + line);
                            }
                        } else if (this.mState == STATE_ANR) {
                            if ("c".equals(line) || "continue".equals(line)) {
                                resumeController(0);
                            } else if ("k".equals(line) || "kill".equals(line) || "w".equals(line) || "wait".equals(line)) {
                                resumeController(1);
                            } else {
                                System.out.println("Invalid command: " + line);
                            }
                        } else if (this.mState == STATE_EARLY_ANR) {
                            if ("c".equals(line) || "continue".equals(line)) {
                                resumeController(0);
                            } else if ("k".equals(line) || "kill".equals(line)) {
                                resumeController(1);
                            } else {
                                System.out.println("Invalid command: " + line);
                            }
                        } else {
                            System.out.println("Invalid command: " + line);
                        }
                    }
                    synchronized (this) {
                        if (addNewline) {
                            System.out.println("");
                            printMessageForState();
                        } else {
                            printMessageForState();
                        }
                    }
                }
                resumeController(0);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Am.this.mAm.setActivityController((IActivityController) null);
            }
        }
    }

    private void runMonitor() throws Exception {
        String gdbPort = null;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("--gdb")) {
                    gdbPort = nextArgRequired();
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return;
                }
            } else {
                MyActivityController controller = new MyActivityController(gdbPort);
                controller.run();
                return;
            }
        }
    }

    private void runHang() throws Exception {
        boolean allowRestart = false;
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("--allow-restart")) {
                    allowRestart = true;
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    return;
                }
            } else {
                System.out.println("Hanging the system...");
                this.mAm.hang(new Binder(), allowRestart);
                return;
            }
        }
    }

    private void runRestart() throws Exception {
        String opt = nextOption();
        if (opt != null) {
            System.err.println("Error: Unknown option: " + opt);
        } else {
            System.out.println("Restart the system...");
            this.mAm.restart();
        }
    }

    private void runIdleMaintenance() throws Exception {
        String opt = nextOption();
        if (opt != null) {
            System.err.println("Error: Unknown option: " + opt);
            return;
        }
        System.out.println("Performing idle maintenance...");
        Intent intent = new Intent("com.android.server.task.controllers.IdleController.ACTION_TRIGGER_IDLE");
        this.mAm.broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String) null, -1, true, false, -1);
    }

    private void runScreenCompat() throws Exception {
        boolean enabled;
        String mode = nextArgRequired();
        if ("on".equals(mode)) {
            enabled = true;
        } else if ("off".equals(mode)) {
            enabled = false;
        } else {
            System.err.println("Error: enabled mode must be 'on' or 'off' at " + mode);
            return;
        }
        String packageName = nextArgRequired();
        do {
            try {
                this.mAm.setPackageScreenCompatMode(packageName, enabled ? 1 : 0);
            } catch (RemoteException e) {
            }
            packageName = nextArg();
        } while (packageName != null);
    }

    private void runToUri(int flags) throws Exception {
        Intent intent = makeIntent(-2);
        System.out.println(intent.toUri(flags));
    }

    private class IntentReceiver extends IIntentReceiver.Stub {
        private boolean mFinished;

        private IntentReceiver() {
            this.mFinished = false;
        }

        public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
            String line = "Broadcast completed: result=" + resultCode;
            if (data != null) {
                line = line + ", data=\"" + data + "\"";
            }
            if (extras != null) {
                line = line + ", extras: " + extras;
            }
            System.out.println(line);
            synchronized (this) {
                this.mFinished = true;
                notifyAll();
            }
        }

        public synchronized void waitForFinish() {
            while (!this.mFinished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private class InstrumentationWatcher extends IInstrumentationWatcher.Stub {
        private boolean mFinished;
        private boolean mRawMode;

        private InstrumentationWatcher() {
            this.mFinished = false;
            this.mRawMode = false;
        }

        public void setRawOutput(boolean rawMode) {
            this.mRawMode = rawMode;
        }

        public void instrumentationStatus(ComponentName name, int resultCode, Bundle results) {
            synchronized (this) {
                String pretty = null;
                if (!this.mRawMode && results != null) {
                    pretty = results.getString("stream");
                }
                if (pretty != null) {
                    System.out.print(pretty);
                } else {
                    if (results != null) {
                        for (String key : results.keySet()) {
                            System.out.println("INSTRUMENTATION_STATUS: " + key + "=" + results.get(key));
                        }
                    }
                    System.out.println("INSTRUMENTATION_STATUS_CODE: " + resultCode);
                }
                notifyAll();
            }
        }

        public void instrumentationFinished(ComponentName name, int resultCode, Bundle results) {
            synchronized (this) {
                String pretty = null;
                if (!this.mRawMode && results != null) {
                    pretty = results.getString("stream");
                }
                if (pretty != null) {
                    System.out.println(pretty);
                } else {
                    if (results != null) {
                        for (String key : results.keySet()) {
                            System.out.println("INSTRUMENTATION_RESULT: " + key + "=" + results.get(key));
                        }
                    }
                    System.out.println("INSTRUMENTATION_CODE: " + resultCode);
                }
                this.mFinished = true;
                notifyAll();
            }
        }

        public boolean waitForFinish() {
            synchronized (this) {
                while (!this.mFinished) {
                    try {
                        if (!Am.this.mAm.asBinder().pingBinder()) {
                            return false;
                        }
                        wait(1000L);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
                return true;
            }
        }
    }

    private void runStack() throws Exception {
        String op = nextArgRequired();
        if (op.equals("start")) {
            runStackStart();
            return;
        }
        if (op.equals("movetask")) {
            runStackMoveTask();
            return;
        }
        if (op.equals("resize")) {
            runStackResize();
            return;
        }
        if (op.equals("list")) {
            runStackList();
        } else if (op.equals("info")) {
            runStackInfo();
        } else {
            showError("Error: unknown command '" + op + "'");
        }
    }

    private void runStackStart() throws Exception {
        String displayIdStr = nextArgRequired();
        int displayId = Integer.valueOf(displayIdStr).intValue();
        Intent intent = makeIntent(-2);
        try {
            IBinder homeActivityToken = this.mAm.getHomeActivityToken();
            IActivityContainer container = this.mAm.createActivityContainer(homeActivityToken, (IActivityContainerCallback) null);
            container.attachToDisplay(displayId);
            container.startActivity(intent);
        } catch (RemoteException e) {
        }
    }

    private void runStackMoveTask() throws Exception {
        boolean toTop;
        String taskIdStr = nextArgRequired();
        int taskId = Integer.valueOf(taskIdStr).intValue();
        String stackIdStr = nextArgRequired();
        int stackId = Integer.valueOf(stackIdStr).intValue();
        String toTopStr = nextArgRequired();
        if ("true".equals(toTopStr)) {
            toTop = true;
        } else if ("false".equals(toTopStr)) {
            toTop = false;
        } else {
            System.err.println("Error: bad toTop arg: " + toTopStr);
            return;
        }
        try {
            this.mAm.moveTaskToStack(taskId, stackId, toTop);
        } catch (RemoteException e) {
        }
    }

    private void runStackResize() throws Exception {
        String stackIdStr = nextArgRequired();
        int stackId = Integer.valueOf(stackIdStr).intValue();
        String leftStr = nextArgRequired();
        int left = Integer.valueOf(leftStr).intValue();
        String topStr = nextArgRequired();
        int top = Integer.valueOf(topStr).intValue();
        String rightStr = nextArgRequired();
        int right = Integer.valueOf(rightStr).intValue();
        String bottomStr = nextArgRequired();
        int bottom = Integer.valueOf(bottomStr).intValue();
        try {
            this.mAm.resizeStack(stackId, new Rect(left, top, right, bottom));
        } catch (RemoteException e) {
        }
    }

    private void runStackList() throws Exception {
        try {
            List<ActivityManager.StackInfo> stacks = this.mAm.getAllStackInfos();
            for (ActivityManager.StackInfo info : stacks) {
                System.out.println(info);
            }
        } catch (RemoteException e) {
        }
    }

    private void runStackInfo() throws Exception {
        try {
            String stackIdStr = nextArgRequired();
            int stackId = Integer.valueOf(stackIdStr).intValue();
            ActivityManager.StackInfo info = this.mAm.getStackInfo(stackId);
            System.out.println(info);
        } catch (RemoteException e) {
        }
    }

    private void runLockTask() throws Exception {
        String taskIdStr = nextArgRequired();
        try {
            if (taskIdStr.equals("stop")) {
                this.mAm.stopLockTaskMode();
            } else {
                int taskId = Integer.valueOf(taskIdStr).intValue();
                this.mAm.startLockTaskMode(taskId);
            }
            System.err.println("Activity manager is " + (this.mAm.isInLockTaskMode() ? "" : "not ") + "in lockTaskMode");
        } catch (RemoteException e) {
        }
    }

    private List<Configuration> getRecentConfigurations(int days) {
        IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
        long now = System.currentTimeMillis();
        long nDaysAgo = now - ((long) ((((days * 24) * 60) * 60) * 1000));
        try {
            ParceledListSlice<ConfigurationStats> configStatsSlice = usm.queryConfigurationStats(4, nDaysAgo, now, "com.android.shell");
            if (configStatsSlice == null) {
                return Collections.emptyList();
            }
            final ArrayMap<Configuration, Integer> recentConfigs = new ArrayMap<>();
            List<ConfigurationStats> configStatsList = configStatsSlice.getList();
            int configStatsListSize = configStatsList.size();
            for (int i = 0; i < configStatsListSize; i++) {
                ConfigurationStats stats = configStatsList.get(i);
                int indexOfKey = recentConfigs.indexOfKey(stats.getConfiguration());
                if (indexOfKey < 0) {
                    recentConfigs.put(stats.getConfiguration(), Integer.valueOf(stats.getActivationCount()));
                } else {
                    recentConfigs.setValueAt(indexOfKey, Integer.valueOf(recentConfigs.valueAt(indexOfKey).intValue() + stats.getActivationCount()));
                }
            }
            Comparator<Configuration> comparator = new Comparator<Configuration>() {
                @Override
                public int compare(Configuration a, Configuration b) {
                    return ((Integer) recentConfigs.get(b)).compareTo((Integer) recentConfigs.get(a));
                }
            };
            ArrayList<Configuration> configs = new ArrayList<>(recentConfigs.size());
            configs.addAll(recentConfigs.keySet());
            Collections.sort(configs, comparator);
            return configs;
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    private void runGetConfig() throws Exception {
        int days = 14;
        String option = nextOption();
        if (option != null) {
            if (!option.equals("--days")) {
                throw new IllegalArgumentException("unrecognized option " + option);
            }
            days = Integer.parseInt(nextArgRequired());
            if (days <= 0) {
                throw new IllegalArgumentException("--days must be a positive integer");
            }
        }
        try {
            Configuration config = this.mAm.getConfiguration();
            if (config == null) {
                System.err.println("Activity manager has no configuration");
                return;
            }
            System.out.println("config: " + Configuration.resourceQualifierString(config));
            System.out.println("abi: " + TextUtils.join(",", Build.SUPPORTED_ABIS));
            List<Configuration> recentConfigs = getRecentConfigurations(days);
            int recentConfigSize = recentConfigs.size();
            if (recentConfigSize > 0) {
                System.out.println("recentConfigs:");
            }
            for (int i = 0; i < recentConfigSize; i++) {
                System.out.println("  config: " + Configuration.resourceQualifierString(recentConfigs.get(i)));
            }
        } catch (RemoteException e) {
        }
    }

    private static ParcelFileDescriptor openForSystemServer(File file, int mode) throws FileNotFoundException {
        ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, mode);
        String tcon = SELinux.getFileContext(file.getAbsolutePath());
        if (!SELinux.checkSELinuxAccess("u:r:system_server:s0", tcon, "file", "read")) {
            throw new FileNotFoundException("System server has no access to file context " + tcon);
        }
        return fd;
    }
}
