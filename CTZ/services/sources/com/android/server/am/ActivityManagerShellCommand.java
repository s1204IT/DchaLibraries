package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IStopUserCallback;
import android.app.IUidObserver;
import android.app.KeyguardManager;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.app.usage.AppStandbyInfo;
import android.app.usage.ConfigurationStats;
import android.app.usage.IUsageStatsManager;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.UserInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.util.NetworkConstants;
import android.opengl.GLES10;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IProgressListener;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.DisplayMetrics;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import com.android.internal.util.HexDump;
import com.android.internal.util.MemInfoReader;
import com.android.internal.util.Preconditions;
import com.android.server.am.ActivityManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.pm.DumpState;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.WindowManagerService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

final class ActivityManagerShellCommand extends ShellCommand {
    public static final String NO_CLASS_ERROR_CODE = "Error type 3";
    private static final String SHELL_PACKAGE_NAME = "com.android.shell";
    private int mActivityType;
    private String mAgent;
    private boolean mAttachAgentDuringBind;
    private boolean mAutoStop;
    private int mDisplayId;
    final boolean mDumping;
    final IActivityManager mInterface;
    final ActivityManagerService mInternal;
    private boolean mIsLockTask;
    private boolean mIsTaskOverlay;
    private String mProfileFile;
    private String mReceiverPermission;
    private int mSamplingInterval;
    private boolean mStreaming;
    private int mTaskId;
    private int mUserId;
    private int mWindowingMode;
    private int mStartFlags = 0;
    private boolean mWaitOption = false;
    private boolean mStopOption = false;
    private int mRepeat = 0;
    final IPackageManager mPm = AppGlobals.getPackageManager();

    static int access$076(ActivityManagerShellCommand activityManagerShellCommand, int i) {
        int i2 = i | activityManagerShellCommand.mStartFlags;
        activityManagerShellCommand.mStartFlags = i2;
        return i2;
    }

    ActivityManagerShellCommand(ActivityManagerService activityManagerService, boolean z) {
        this.mInterface = activityManagerService;
        this.mInternal = activityManagerService;
        this.mDumping = z;
    }

    public int onCommand(String str) {
        byte b;
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            switch (str.hashCode()) {
                case -2121667104:
                    b = !str.equals("dumpheap") ? (byte) -1 : (byte) 14;
                    break;
                case -1969672196:
                    if (str.equals("set-debug-app")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_BOS;
                        break;
                    }
                    break;
                case -1719979774:
                    if (str.equals("get-inactive")) {
                        b = 49;
                        break;
                    }
                    break;
                case -1710503333:
                    if (str.equals("package-importance")) {
                        b = 32;
                        break;
                    }
                    break;
                case -1667670943:
                    if (str.equals("get-standby-bucket")) {
                        b = 51;
                        break;
                    }
                    break;
                case -1619282346:
                    if (str.equals("start-user")) {
                        b = 38;
                        break;
                    }
                    break;
                case -1618876223:
                    if (str.equals("broadcast")) {
                        b = 10;
                        break;
                    }
                    break;
                case -1324660647:
                    if (str.equals("suppress-resize-config-changes")) {
                        b = 47;
                        break;
                    }
                    break;
                case -1303445945:
                    if (str.equals("send-trim-memory")) {
                        b = 52;
                        break;
                    }
                    break;
                case -1131287478:
                    if (str.equals("start-service")) {
                        b = 3;
                        break;
                    }
                    break;
                case -1002578147:
                    if (str.equals("get-uid-state")) {
                        b = 45;
                        break;
                    }
                    break;
                case -965273485:
                    if (str.equals("stopservice")) {
                        b = 8;
                        break;
                    }
                    break;
                case -930080590:
                    if (str.equals("startfgservice")) {
                        b = 5;
                        break;
                    }
                    break;
                case -907667276:
                    if (str.equals("unlock-user")) {
                        b = 39;
                        break;
                    }
                    break;
                case -892396682:
                    if (str.equals("start-foreground-service")) {
                        b = 6;
                        break;
                    }
                    break;
                case -870018278:
                    if (str.equals("to-uri")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_HID;
                        break;
                    }
                    break;
                case -812219210:
                    if (str.equals("get-current-user")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_AUDIO_ENDPOINT;
                        break;
                    }
                    break;
                case -747637291:
                    if (str.equals("set-standby-bucket")) {
                        b = 50;
                        break;
                    }
                    break;
                case -699625063:
                    if (str.equals("get-config")) {
                        b = 46;
                        break;
                    }
                    break;
                case -606123342:
                    if (str.equals("kill-all")) {
                        b = 24;
                        break;
                    }
                    break;
                case -548621938:
                    if (str.equals("is-user-stopped")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_HUB;
                        break;
                    }
                    break;
                case -387147436:
                    if (str.equals("track-associations")) {
                        b = 43;
                        break;
                    }
                    break;
                case -354890749:
                    if (str.equals("screen-compat")) {
                        b = 31;
                        break;
                    }
                    break;
                case -309425751:
                    if (str.equals("profile")) {
                        b = UsbACInterface.ACI_SAMPLE_RATE_CONVERTER;
                        break;
                    }
                    break;
                case -170987146:
                    if (str.equals("set-inactive")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION;
                        break;
                    }
                    break;
                case -146027423:
                    if (str.equals("watch-uids")) {
                        b = 27;
                        break;
                    }
                    break;
                case -100644880:
                    if (str.equals("startforegroundservice")) {
                        b = 4;
                        break;
                    }
                    break;
                case -27715536:
                    if (str.equals("make-uid-idle")) {
                        b = 25;
                        break;
                    }
                    break;
                case 3194994:
                    if (str.equals("hang")) {
                        b = 28;
                        break;
                    }
                    break;
                case 3291998:
                    if (str.equals("kill")) {
                        b = 23;
                        break;
                    }
                    break;
                case 3552645:
                    if (str.equals("task")) {
                        b = 55;
                        break;
                    }
                    break;
                case 88586660:
                    if (str.equals("force-stop")) {
                        b = 21;
                        break;
                    }
                    break;
                case 94921639:
                    if (str.equals("crash")) {
                        b = 22;
                        break;
                    }
                    break;
                case 109757064:
                    if (str.equals("stack")) {
                        b = 54;
                        break;
                    }
                    break;
                case 109757538:
                    if (str.equals("start")) {
                        b = 0;
                        break;
                    }
                    break;
                case 113399775:
                    if (str.equals("write")) {
                        b = 56;
                        break;
                    }
                    break;
                case 185053203:
                    if (str.equals("startservice")) {
                        b = 2;
                        break;
                    }
                    break;
                case 237240942:
                    if (str.equals("to-app-uri")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_PHYSICAL;
                        break;
                    }
                    break;
                case 549617690:
                    if (str.equals("start-activity")) {
                        b = 1;
                        break;
                    }
                    break;
                case 622433197:
                    if (str.equals("untrack-associations")) {
                        b = 44;
                        break;
                    }
                    break;
                case 667014829:
                    if (str.equals("bug-report")) {
                        b = 20;
                        break;
                    }
                    break;
                case 680834441:
                    if (str.equals("supports-split-screen-multi-window")) {
                        b = 59;
                        break;
                    }
                    break;
                case 723112852:
                    if (str.equals("trace-ipc")) {
                        b = 12;
                        break;
                    }
                    break;
                case 764545184:
                    if (str.equals("supports-multiwindow")) {
                        b = 58;
                        break;
                    }
                    break;
                case 808179021:
                    if (str.equals("to-intent-uri")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_REPORT;
                        break;
                    }
                    break;
                case 810242677:
                    if (str.equals("set-watch-heap")) {
                        b = 18;
                        break;
                    }
                    break;
                case 817137578:
                    if (str.equals("clear-watch-heap")) {
                        b = 19;
                        break;
                    }
                    break;
                case 822490030:
                    if (str.equals("set-agent-app")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_CAPABILITY;
                        break;
                    }
                    break;
                case 900455412:
                    if (str.equals("start-fg-service")) {
                        b = 7;
                        break;
                    }
                    break;
                case 1024703869:
                    if (str.equals("attach-agent")) {
                        b = 57;
                        break;
                    }
                    break;
                case 1078591527:
                    if (str.equals("clear-debug-app")) {
                        b = 17;
                        break;
                    }
                    break;
                case 1097506319:
                    if (str.equals("restart")) {
                        b = 29;
                        break;
                    }
                    break;
                case 1129261387:
                    if (str.equals("update-appinfo")) {
                        b = 60;
                        break;
                    }
                    break;
                case 1219773618:
                    if (str.equals("get-started-user-state")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_SUPERSPEED_HUB;
                        break;
                    }
                    break;
                case 1236319578:
                    if (str.equals("monitor")) {
                        b = 26;
                        break;
                    }
                    break;
                case 1395483623:
                    if (str.equals("instrument")) {
                        b = 11;
                        break;
                    }
                    break;
                case 1583986358:
                    if (str.equals("stop-user")) {
                        b = 40;
                        break;
                    }
                    break;
                case 1618908732:
                    if (str.equals("wait-for-broadcast-idle")) {
                        b = 62;
                        break;
                    }
                    break;
                case 1671764162:
                    if (str.equals("display")) {
                        b = 53;
                        break;
                    }
                    break;
                case 1852789518:
                    if (str.equals("no-home-screen")) {
                        b = 61;
                        break;
                    }
                    break;
                case 1861559962:
                    if (str.equals("idle-maintenance")) {
                        b = 30;
                        break;
                    }
                    break;
                case 1863290858:
                    if (str.equals("stop-service")) {
                        b = 9;
                        break;
                    }
                    break;
                case 2083239620:
                    if (str.equals("switch-user")) {
                        b = UsbDescriptor.DESCRIPTORTYPE_AUDIO_INTERFACE;
                        break;
                    }
                    break;
                default:
                    break;
            }
            switch (b) {
                case 0:
                case 1:
                    return runStartActivity(outPrintWriter);
                case 2:
                case 3:
                    return runStartService(outPrintWriter, false);
                case 4:
                case 5:
                case 6:
                case 7:
                    return runStartService(outPrintWriter, true);
                case 8:
                case 9:
                    return runStopService(outPrintWriter);
                case 10:
                    return runSendBroadcast(outPrintWriter);
                case 11:
                    getOutPrintWriter().println("Error: must be invoked through 'am instrument'.");
                    return -1;
                case 12:
                    return runTraceIpc(outPrintWriter);
                case 13:
                    return runProfile(outPrintWriter);
                case 14:
                    return runDumpHeap(outPrintWriter);
                case 15:
                    return runSetDebugApp(outPrintWriter);
                case 16:
                    return runSetAgentApp(outPrintWriter);
                case 17:
                    return runClearDebugApp(outPrintWriter);
                case 18:
                    return runSetWatchHeap(outPrintWriter);
                case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                    return runClearWatchHeap(outPrintWriter);
                case 20:
                    return runBugReport(outPrintWriter);
                case BackupHandler.MSG_OP_COMPLETE:
                    return runForceStop(outPrintWriter);
                case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                    return runCrash(outPrintWriter);
                case WindowManagerService.H.BOOT_TIMEOUT:
                    return runKill(outPrintWriter);
                case 24:
                    return runKillAll(outPrintWriter);
                case WindowManagerService.H.SHOW_STRICT_MODE_VIOLATION:
                    return runMakeIdle(outPrintWriter);
                case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                    return runMonitor(outPrintWriter);
                case 27:
                    return runWatchUids(outPrintWriter);
                case NetworkConstants.ARP_PAYLOAD_LEN:
                    return runHang(outPrintWriter);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_ENTRY_MODE:
                    return runRestart(outPrintWriter);
                case 30:
                    return runIdleMaintenance(outPrintWriter);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_12:
                    return runScreenCompat(outPrintWriter);
                case 32:
                    return runPackageImportance(outPrintWriter);
                case 33:
                    return runToUri(outPrintWriter, 0);
                case 34:
                    return runToUri(outPrintWriter, 1);
                case 35:
                    return runToUri(outPrintWriter, 2);
                case 36:
                    return runSwitchUser(outPrintWriter);
                case 37:
                    return runGetCurrentUser(outPrintWriter);
                case 38:
                    return runStartUser(outPrintWriter);
                case 39:
                    return runUnlockUser(outPrintWriter);
                case 40:
                    return runStopUser(outPrintWriter);
                case 41:
                    return runIsUserStopped(outPrintWriter);
                case HdmiCecKeycode.CEC_KEYCODE_DOT:
                    return runGetStartedUserState(outPrintWriter);
                case HdmiCecKeycode.CEC_KEYCODE_ENTER:
                    return runTrackAssociations(outPrintWriter);
                case HdmiCecKeycode.CEC_KEYCODE_CLEAR:
                    return runUntrackAssociations(outPrintWriter);
                case NetworkPolicyManagerService.TYPE_RAPID:
                    return getUidState(outPrintWriter);
                case WindowManagerService.H.WINDOW_REPLACEMENT_TIMEOUT:
                    return runGetConfig(outPrintWriter);
                case 47:
                    return runSuppressResizeConfigChanges(outPrintWriter);
                case 48:
                    return runSetInactive(outPrintWriter);
                case 49:
                    return runGetInactive(outPrintWriter);
                case HdmiCecKeycode.CEC_KEYCODE_PREVIOUS_CHANNEL:
                    return runSetStandbyBucket(outPrintWriter);
                case 51:
                    return runGetStandbyBucket(outPrintWriter);
                case 52:
                    return runSendTrimMemory(outPrintWriter);
                case 53:
                    return runDisplay(outPrintWriter);
                case 54:
                    return runStack(outPrintWriter);
                case 55:
                    return runTask(outPrintWriter);
                case 56:
                    return runWrite(outPrintWriter);
                case WindowManagerService.H.NOTIFY_KEYGUARD_TRUSTED_CHANGED:
                    return runAttachAgent(outPrintWriter);
                case WindowManagerService.H.SET_HAS_OVERLAY_UI:
                    return runSupportsMultiwindow(outPrintWriter);
                case WindowManagerService.H.SET_RUNNING_REMOTE_ANIMATION:
                    return runSupportsSplitScreenMultiwindow(outPrintWriter);
                case 60:
                    return runUpdateApplicationInfo(outPrintWriter);
                case WindowManagerService.H.RECOMPUTE_FOCUS:
                    return runNoHomeScreen(outPrintWriter);
                case 62:
                    return runWaitForBroadcastIdle(outPrintWriter);
                default:
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    private Intent makeIntent(int i) throws URISyntaxException {
        this.mStartFlags = 0;
        this.mWaitOption = false;
        this.mStopOption = false;
        this.mRepeat = 0;
        this.mProfileFile = null;
        this.mSamplingInterval = 0;
        this.mAutoStop = false;
        this.mStreaming = false;
        this.mUserId = i;
        this.mDisplayId = -1;
        this.mWindowingMode = 0;
        this.mActivityType = 0;
        this.mTaskId = -1;
        this.mIsTaskOverlay = false;
        this.mIsLockTask = false;
        return Intent.parseCommandArgs(this, new Intent.CommandOptionHandler() {
            public boolean handleOption(String str, ShellCommand shellCommand) {
                if (str.equals("-D")) {
                    ActivityManagerShellCommand.access$076(ActivityManagerShellCommand.this, 2);
                } else if (str.equals("-N")) {
                    ActivityManagerShellCommand.access$076(ActivityManagerShellCommand.this, 8);
                } else if (str.equals("-W")) {
                    ActivityManagerShellCommand.this.mWaitOption = true;
                } else if (str.equals("-P")) {
                    ActivityManagerShellCommand.this.mProfileFile = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAutoStop = true;
                } else if (str.equals("--start-profiler")) {
                    ActivityManagerShellCommand.this.mProfileFile = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAutoStop = false;
                } else if (str.equals("--sampling")) {
                    ActivityManagerShellCommand.this.mSamplingInterval = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (str.equals("--streaming")) {
                    ActivityManagerShellCommand.this.mStreaming = true;
                } else if (str.equals("--attach-agent")) {
                    if (ActivityManagerShellCommand.this.mAgent != null) {
                        shellCommand.getErrPrintWriter().println("Multiple --attach-agent(-bind) not supported");
                        return false;
                    }
                    ActivityManagerShellCommand.this.mAgent = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAttachAgentDuringBind = false;
                } else if (str.equals("--attach-agent-bind")) {
                    if (ActivityManagerShellCommand.this.mAgent != null) {
                        shellCommand.getErrPrintWriter().println("Multiple --attach-agent(-bind) not supported");
                        return false;
                    }
                    ActivityManagerShellCommand.this.mAgent = ActivityManagerShellCommand.this.getNextArgRequired();
                    ActivityManagerShellCommand.this.mAttachAgentDuringBind = true;
                } else if (str.equals("-R")) {
                    ActivityManagerShellCommand.this.mRepeat = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (str.equals("-S")) {
                    ActivityManagerShellCommand.this.mStopOption = true;
                } else if (str.equals("--track-allocation")) {
                    ActivityManagerShellCommand.access$076(ActivityManagerShellCommand.this, 4);
                } else if (str.equals("--user")) {
                    ActivityManagerShellCommand.this.mUserId = UserHandle.parseUserArg(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (str.equals("--receiver-permission")) {
                    ActivityManagerShellCommand.this.mReceiverPermission = ActivityManagerShellCommand.this.getNextArgRequired();
                } else if (str.equals("--display")) {
                    ActivityManagerShellCommand.this.mDisplayId = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (str.equals("--windowingMode")) {
                    ActivityManagerShellCommand.this.mWindowingMode = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (str.equals("--activityType")) {
                    ActivityManagerShellCommand.this.mActivityType = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (str.equals("--task")) {
                    ActivityManagerShellCommand.this.mTaskId = Integer.parseInt(ActivityManagerShellCommand.this.getNextArgRequired());
                } else if (str.equals("--task-overlay")) {
                    ActivityManagerShellCommand.this.mIsTaskOverlay = true;
                } else {
                    if (!str.equals("--lock-task")) {
                        return false;
                    }
                    ActivityManagerShellCommand.this.mIsLockTask = true;
                }
                return true;
            }
        });
    }

    int runStartActivity(PrintWriter printWriter) throws RemoteException {
        ParcelFileDescriptor parcelFileDescriptor;
        ProfilerInfo profilerInfo;
        ActivityOptions activityOptions;
        int i;
        String str;
        ?? r22;
        int i2;
        int iStartActivityAsUser;
        WaitResult waitResult;
        PrintWriter errPrintWriter;
        ?? r3;
        String packageName;
        try {
            Intent intentMakeIntent = makeIntent(-2);
            int i3 = -1;
            ?? r13 = 1;
            if (this.mUserId == -1) {
                getErrPrintWriter().println("Error: Can't start service with user 'all'");
                return 1;
            }
            String type = intentMakeIntent.getType();
            if (type == null && intentMakeIntent.getData() != null && "content".equals(intentMakeIntent.getData().getScheme())) {
                type = this.mInterface.getProviderMimeType(intentMakeIntent.getData(), this.mUserId);
            }
            String str2 = type;
            while (true) {
                if (this.mStopOption) {
                    if (intentMakeIntent.getComponent() != null) {
                        packageName = intentMakeIntent.getComponent().getPackageName();
                    } else {
                        List list = this.mPm.queryIntentActivities(intentMakeIntent, str2, 0, this.mUserId).getList();
                        if (list != null && list.size() > 0) {
                            if (list.size() > r13) {
                                getErrPrintWriter().println("Error: Intent matches multiple activities; can't stop: " + intentMakeIntent);
                                return r13;
                            }
                            packageName = ((ResolveInfo) list.get(0)).activityInfo.packageName;
                        }
                    }
                    printWriter.println("Stopping: " + packageName);
                    printWriter.flush();
                    this.mInterface.forceStopPackage(packageName, this.mUserId);
                    try {
                        Thread.sleep(250L);
                    } catch (InterruptedException e) {
                    }
                }
                if (this.mProfileFile != null || this.mAgent != null) {
                    if (this.mProfileFile != null) {
                        ParcelFileDescriptor parcelFileDescriptorOpenFileForSystem = openFileForSystem(this.mProfileFile, "w");
                        if (parcelFileDescriptorOpenFileForSystem == null) {
                            return r13;
                        }
                        parcelFileDescriptor = parcelFileDescriptorOpenFileForSystem;
                    } else {
                        parcelFileDescriptor = null;
                    }
                    profilerInfo = new ProfilerInfo(this.mProfileFile, parcelFileDescriptor, this.mSamplingInterval, this.mAutoStop, this.mStreaming, this.mAgent, this.mAttachAgentDuringBind);
                } else {
                    profilerInfo = null;
                }
                printWriter.println("Starting: " + intentMakeIntent);
                printWriter.flush();
                intentMakeIntent.addFlags(268435456);
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (this.mDisplayId != i3) {
                    ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
                    activityOptionsMakeBasic.setLaunchDisplayId(this.mDisplayId);
                    activityOptions = activityOptionsMakeBasic;
                } else {
                    activityOptions = null;
                }
                ActivityOptions activityOptionsMakeBasic2 = activityOptions;
                ActivityOptions activityOptions2 = activityOptions;
                if (this.mWindowingMode != 0) {
                    if (activityOptions == null) {
                        activityOptionsMakeBasic2 = ActivityOptions.makeBasic();
                    }
                    activityOptionsMakeBasic2.setLaunchWindowingMode(this.mWindowingMode);
                    activityOptions2 = activityOptionsMakeBasic2;
                }
                ActivityOptions activityOptionsMakeBasic3 = activityOptions2;
                ActivityOptions activityOptions3 = activityOptions2;
                if (this.mActivityType != 0) {
                    if (activityOptions2 == null) {
                        activityOptionsMakeBasic3 = ActivityOptions.makeBasic();
                    }
                    activityOptionsMakeBasic3.setLaunchActivityType(this.mActivityType);
                    activityOptions3 = activityOptionsMakeBasic3;
                }
                ?? MakeBasic = activityOptions3;
                ?? r32 = activityOptions3;
                if (this.mTaskId != i3) {
                    if (activityOptions3 == null) {
                        MakeBasic = ActivityOptions.makeBasic();
                    }
                    MakeBasic.setLaunchTaskId(this.mTaskId);
                    r32 = MakeBasic;
                    if (this.mIsTaskOverlay) {
                        MakeBasic.setTaskOverlay(r13, r13);
                        r32 = MakeBasic;
                    }
                }
                ?? MakeBasic2 = r32;
                ?? r33 = r32;
                if (this.mIsLockTask) {
                    if (r32 == 0) {
                        MakeBasic2 = ActivityOptions.makeBasic();
                    }
                    MakeBasic2.setLockTaskEnabled(r13);
                    r33 = MakeBasic2;
                }
                if (this.mWaitOption) {
                    i = 0;
                    str = str2;
                    r22 = r13;
                    i2 = i3;
                    waitResult = this.mInterface.startActivityAndWait((IApplicationThread) null, (String) null, intentMakeIntent, str2, (IBinder) null, (String) null, 0, this.mStartFlags, profilerInfo, r33 != 0 ? r33.toBundle() : null, this.mUserId);
                    iStartActivityAsUser = waitResult.result;
                } else {
                    i = 0;
                    str = str2;
                    r22 = r13;
                    i2 = i3;
                    iStartActivityAsUser = this.mInterface.startActivityAsUser((IApplicationThread) null, (String) null, intentMakeIntent, str, (IBinder) null, (String) null, 0, this.mStartFlags, profilerInfo, r33 != 0 ? r33.toBundle() : null, this.mUserId);
                    waitResult = null;
                }
                long jUptimeMillis2 = SystemClock.uptimeMillis();
                if (!this.mWaitOption) {
                    errPrintWriter = getErrPrintWriter();
                } else {
                    errPrintWriter = printWriter;
                }
                if (iStartActivityAsUser == 100) {
                    errPrintWriter.println("Warning: Activity not started because the  current activity is being kept for the user.");
                } else {
                    switch (iStartActivityAsUser) {
                        case -98:
                            errPrintWriter.println("Error: Not allowed to start background user activity that shouldn't be displayed for all users.");
                            break;
                        case -97:
                            errPrintWriter.println("Error: Activity not started, voice control not allowed for: " + intentMakeIntent);
                            break;
                        default:
                            switch (iStartActivityAsUser) {
                                case -94:
                                    errPrintWriter.println("Error: Activity not started, you do not have permission to access it.");
                                    break;
                                case -93:
                                    errPrintWriter.println("Error: Activity not started, you requested to both forward and receive its result");
                                    break;
                                case -92:
                                    errPrintWriter.println(NO_CLASS_ERROR_CODE);
                                    errPrintWriter.println("Error: Activity class " + intentMakeIntent.getComponent().toShortString() + " does not exist.");
                                    break;
                                case -91:
                                    errPrintWriter.println("Error: Activity not started, unable to resolve " + intentMakeIntent.toString());
                                    break;
                                default:
                                    switch (iStartActivityAsUser) {
                                        case 0:
                                            break;
                                        case 1:
                                            errPrintWriter.println("Warning: Activity not started because intent should be handled by the caller");
                                            break;
                                        case 2:
                                            errPrintWriter.println("Warning: Activity not started, its current task has been brought to the front");
                                            break;
                                        case 3:
                                            errPrintWriter.println("Warning: Activity not started, intent has been delivered to currently running top-most instance.");
                                            break;
                                        default:
                                            errPrintWriter.println("Error: Activity not started, unknown error code " + iStartActivityAsUser);
                                            break;
                                    }
                                    errPrintWriter.flush();
                                    if (this.mWaitOption && r3 != 0) {
                                        if (waitResult == null) {
                                            waitResult = new WaitResult();
                                            waitResult.who = intentMakeIntent.getComponent();
                                        }
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("Status: ");
                                        sb.append(!waitResult.timeout ? "timeout" : "ok");
                                        printWriter.println(sb.toString());
                                        if (waitResult.who != null) {
                                            printWriter.println("Activity: " + waitResult.who.flattenToShortString());
                                        }
                                        if (waitResult.thisTime >= 0) {
                                            printWriter.println("ThisTime: " + waitResult.thisTime);
                                        }
                                        if (waitResult.totalTime >= 0) {
                                            printWriter.println("TotalTime: " + waitResult.totalTime);
                                        }
                                        printWriter.println("WaitTime: " + (jUptimeMillis2 - jUptimeMillis));
                                        printWriter.println("Complete");
                                        printWriter.flush();
                                    }
                                    this.mRepeat--;
                                    if (this.mRepeat > 0) {
                                        this.mInterface.unhandledBack();
                                    }
                                    if (this.mRepeat > 0) {
                                        str2 = str;
                                        r13 = r22;
                                        i3 = i2;
                                    } else {
                                        return i;
                                    }
                                    break;
                            }
                            break;
                    }
                    r3 = i;
                    errPrintWriter.flush();
                    if (this.mWaitOption) {
                        if (waitResult == null) {
                        }
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("Status: ");
                        sb2.append(!waitResult.timeout ? "timeout" : "ok");
                        printWriter.println(sb2.toString());
                        if (waitResult.who != null) {
                        }
                        if (waitResult.thisTime >= 0) {
                        }
                        if (waitResult.totalTime >= 0) {
                        }
                        printWriter.println("WaitTime: " + (jUptimeMillis2 - jUptimeMillis));
                        printWriter.println("Complete");
                        printWriter.flush();
                    }
                    this.mRepeat--;
                    if (this.mRepeat > 0) {
                    }
                    if (this.mRepeat > 0) {
                    }
                }
                r3 = r22;
                errPrintWriter.flush();
                if (this.mWaitOption) {
                }
                this.mRepeat--;
                if (this.mRepeat > 0) {
                }
                if (this.mRepeat > 0) {
                }
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    int runStartService(PrintWriter printWriter, boolean z) throws RemoteException {
        PrintWriter errPrintWriter = getErrPrintWriter();
        try {
            Intent intentMakeIntent = makeIntent(-2);
            if (this.mUserId == -1) {
                errPrintWriter.println("Error: Can't start activity with user 'all'");
                return -1;
            }
            printWriter.println("Starting service: " + intentMakeIntent);
            printWriter.flush();
            ComponentName componentNameStartService = this.mInterface.startService((IApplicationThread) null, intentMakeIntent, intentMakeIntent.getType(), z, SHELL_PACKAGE_NAME, this.mUserId);
            if (componentNameStartService == null) {
                errPrintWriter.println("Error: Not found; no service started.");
                return -1;
            }
            if (componentNameStartService.getPackageName().equals("!")) {
                errPrintWriter.println("Error: Requires permission " + componentNameStartService.getClassName());
                return -1;
            }
            if (componentNameStartService.getPackageName().equals("!!")) {
                errPrintWriter.println("Error: " + componentNameStartService.getClassName());
                return -1;
            }
            if (componentNameStartService.getPackageName().equals("?")) {
                errPrintWriter.println("Error: " + componentNameStartService.getClassName());
                return -1;
            }
            return 0;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runStopService(PrintWriter printWriter) throws RemoteException {
        PrintWriter errPrintWriter = getErrPrintWriter();
        try {
            Intent intentMakeIntent = makeIntent(-2);
            if (this.mUserId == -1) {
                errPrintWriter.println("Error: Can't stop activity with user 'all'");
                return -1;
            }
            printWriter.println("Stopping service: " + intentMakeIntent);
            printWriter.flush();
            int iStopService = this.mInterface.stopService((IApplicationThread) null, intentMakeIntent, intentMakeIntent.getType(), this.mUserId);
            if (iStopService == 0) {
                errPrintWriter.println("Service not stopped: was not running.");
                return -1;
            }
            if (iStopService == 1) {
                errPrintWriter.println("Service stopped");
                return -1;
            }
            if (iStopService == -1) {
                errPrintWriter.println("Error stopping service");
                return -1;
            }
            return 0;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static final class IntentReceiver extends IIntentReceiver.Stub {
        private boolean mFinished = false;
        private final PrintWriter mPw;

        IntentReceiver(PrintWriter printWriter) {
            this.mPw = printWriter;
        }

        public void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
            String str2 = "Broadcast completed: result=" + i;
            if (str != null) {
                str2 = str2 + ", data=\"" + str + "\"";
            }
            if (bundle != null) {
                str2 = str2 + ", extras: " + bundle;
            }
            this.mPw.println(str2);
            this.mPw.flush();
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

    int runSendBroadcast(PrintWriter printWriter) throws RemoteException {
        try {
            Intent intentMakeIntent = makeIntent(-2);
            intentMakeIntent.addFlags(DumpState.DUMP_CHANGES);
            IntentReceiver intentReceiver = new IntentReceiver(printWriter);
            String[] strArr = this.mReceiverPermission == null ? null : new String[]{this.mReceiverPermission};
            printWriter.println("Broadcasting: " + intentMakeIntent);
            printWriter.flush();
            this.mInterface.broadcastIntent((IApplicationThread) null, intentMakeIntent, (String) null, intentReceiver, 0, (String) null, (Bundle) null, strArr, -1, (Bundle) null, true, false, this.mUserId);
            intentReceiver.waitForFinish();
            return 0;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runTraceIpc(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired = getNextArgRequired();
        if (nextArgRequired.equals("start")) {
            return runTraceIpcStart(printWriter);
        }
        if (nextArgRequired.equals("stop")) {
            return runTraceIpcStop(printWriter);
        }
        getErrPrintWriter().println("Error: unknown trace ipc command '" + nextArgRequired + "'");
        return -1;
    }

    int runTraceIpcStart(PrintWriter printWriter) throws RemoteException {
        printWriter.println("Starting IPC tracing.");
        printWriter.flush();
        this.mInterface.startBinderTracking();
        return 0;
    }

    int runTraceIpcStop(PrintWriter printWriter) throws RemoteException {
        PrintWriter errPrintWriter = getErrPrintWriter();
        String nextArgRequired = null;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--dump-file")) {
                    nextArgRequired = getNextArgRequired();
                } else {
                    errPrintWriter.println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                if (nextArgRequired == null) {
                    errPrintWriter.println("Error: Specify filename to dump logs to.");
                    return -1;
                }
                ParcelFileDescriptor parcelFileDescriptorOpenFileForSystem = openFileForSystem(nextArgRequired, "w");
                if (parcelFileDescriptorOpenFileForSystem == null) {
                    return -1;
                }
                if (!this.mInterface.stopBinderTrackingAndDump(parcelFileDescriptorOpenFileForSystem)) {
                    errPrintWriter.println("STOP TRACE FAILED.");
                    return -1;
                }
                printWriter.println("Stopped IPC tracing. Dumping logs to: " + nextArgRequired);
                return 0;
            }
        }
    }

    static void removeWallOption() {
        String str = SystemProperties.get("dalvik.vm.extra-opts");
        if (str != null && str.contains("-Xprofile:wallclock")) {
            SystemProperties.set("dalvik.vm.extra-opts", str.replace("-Xprofile:wallclock", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).trim());
        }
    }

    private int runProfile(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired;
        boolean z;
        int i;
        boolean z2;
        ProfilerInfo profilerInfo;
        PrintWriter errPrintWriter = getErrPrintWriter();
        this.mSamplingInterval = 0;
        this.mStreaming = false;
        String nextArgRequired2 = getNextArgRequired();
        int userArg = -2;
        if ("start".equals(nextArgRequired2)) {
            z2 = false;
            while (true) {
                String nextOption = getNextOption();
                if (nextOption == null) {
                    nextArgRequired = getNextArgRequired();
                    z = true;
                    i = userArg;
                    break;
                }
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else if (nextOption.equals("--wall")) {
                    z2 = true;
                } else if (nextOption.equals("--streaming")) {
                    this.mStreaming = true;
                } else {
                    if (!nextOption.equals("--sampling")) {
                        errPrintWriter.println("Error: Unknown option: " + nextOption);
                        return -1;
                    }
                    this.mSamplingInterval = Integer.parseInt(getNextArgRequired());
                }
            }
        } else {
            if ("stop".equals(nextArgRequired2)) {
                while (true) {
                    String nextOption2 = getNextOption();
                    if (nextOption2 == null) {
                        nextArgRequired2 = getNextArg();
                        break;
                    }
                    if (!nextOption2.equals("--user")) {
                        errPrintWriter.println("Error: Unknown option: " + nextOption2);
                        return -1;
                    }
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                }
            } else {
                String nextArgRequired3 = getNextArgRequired();
                if ("start".equals(nextArgRequired3)) {
                    nextArgRequired = nextArgRequired2;
                    z = true;
                    i = -2;
                    z2 = false;
                } else if (!"stop".equals(nextArgRequired3)) {
                    throw new IllegalArgumentException("Profile command " + nextArgRequired2 + " not valid");
                }
            }
            z = false;
            nextArgRequired = nextArgRequired2;
            i = userArg;
            z2 = false;
        }
        if (i == -1) {
            errPrintWriter.println("Error: Can't profile with user 'all'");
            return -1;
        }
        if (z) {
            String nextArgRequired4 = getNextArgRequired();
            ParcelFileDescriptor parcelFileDescriptorOpenFileForSystem = openFileForSystem(nextArgRequired4, "w");
            if (parcelFileDescriptorOpenFileForSystem == null) {
                return -1;
            }
            profilerInfo = new ProfilerInfo(nextArgRequired4, parcelFileDescriptorOpenFileForSystem, this.mSamplingInterval, false, this.mStreaming, (String) null, false);
        } else {
            profilerInfo = null;
        }
        if (z2) {
            try {
                String str = SystemProperties.get("dalvik.vm.extra-opts");
                if (str == null || !str.contains("-Xprofile:wallclock")) {
                    String str2 = str + " -Xprofile:wallclock";
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (this.mInterface.profileControl(nextArgRequired, i, z, profilerInfo, 0)) {
            return 0;
        }
        errPrintWriter.println("PROFILE FAILED on process " + nextArgRequired);
        return -1;
    }

    int runDumpHeap(PrintWriter printWriter) throws RemoteException {
        PrintWriter errPrintWriter = getErrPrintWriter();
        boolean z = true;
        boolean z2 = false;
        boolean z3 = false;
        int userArg = -2;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption == null) {
                String nextArgRequired = getNextArgRequired();
                String nextArgRequired2 = getNextArgRequired();
                ParcelFileDescriptor parcelFileDescriptorOpenFileForSystem = openFileForSystem(nextArgRequired2, "w");
                if (parcelFileDescriptorOpenFileForSystem == null) {
                    return -1;
                }
                if (this.mInterface.dumpHeap(nextArgRequired, userArg, z, z2, z3, nextArgRequired2, parcelFileDescriptorOpenFileForSystem)) {
                    return 0;
                }
                errPrintWriter.println("HEAP DUMP FAILED on process " + nextArgRequired);
                return -1;
            }
            if (nextOption.equals("--user")) {
                userArg = UserHandle.parseUserArg(getNextArgRequired());
                if (userArg == -1) {
                    errPrintWriter.println("Error: Can't dump heap with user 'all'");
                    return -1;
                }
            } else {
                if (!nextOption.equals("-n")) {
                    if (nextOption.equals("-g")) {
                        z3 = true;
                    } else {
                        if (!nextOption.equals("-m")) {
                            errPrintWriter.println("Error: Unknown option: " + nextOption);
                            return -1;
                        }
                        z2 = true;
                    }
                }
                z = false;
            }
        }
    }

    int runSetDebugApp(PrintWriter printWriter) throws RemoteException {
        boolean z = false;
        boolean z2 = false;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("-w")) {
                    z = true;
                } else if (nextOption.equals("--persistent")) {
                    z2 = true;
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                this.mInterface.setDebugApp(getNextArgRequired(), z, z2);
                return 0;
            }
        }
    }

    int runSetAgentApp(PrintWriter printWriter) throws RemoteException {
        this.mInterface.setAgentApp(getNextArgRequired(), getNextArg());
        return 0;
    }

    int runClearDebugApp(PrintWriter printWriter) throws RemoteException {
        this.mInterface.setDebugApp((String) null, false, true);
        return 0;
    }

    int runSetWatchHeap(PrintWriter printWriter) throws RemoteException {
        this.mInterface.setDumpHeapDebugLimit(getNextArgRequired(), 0, Long.parseLong(getNextArgRequired()), (String) null);
        return 0;
    }

    int runClearWatchHeap(PrintWriter printWriter) throws RemoteException {
        this.mInterface.setDumpHeapDebugLimit(getNextArgRequired(), 0, -1L, (String) null);
        return 0;
    }

    int runBugReport(PrintWriter printWriter) throws RemoteException {
        int i = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--progress")) {
                    i = 1;
                } else if (nextOption.equals("--telephony")) {
                    i = 4;
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                this.mInterface.requestBugReport(i);
                printWriter.println("Your lovely bug report is being created; please be patient.");
                return 0;
            }
        }
    }

    int runForceStop(PrintWriter printWriter) throws RemoteException {
        int userArg = -1;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                this.mInterface.forceStopPackage(getNextArgRequired(), userArg);
                return 0;
            }
        }
    }

    int runCrash(PrintWriter printWriter) throws RemoteException {
        int i;
        String str;
        int userArg = -1;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                String nextArgRequired = getNextArgRequired();
                try {
                    i = Integer.parseInt(nextArgRequired);
                    str = null;
                } catch (NumberFormatException e) {
                    i = -1;
                    str = nextArgRequired;
                }
                this.mInterface.crashApplication(-1, i, str, userArg, "shell-induced crash", false);
                return 0;
            }
        }
    }

    int runKill(PrintWriter printWriter) throws RemoteException {
        int userArg = -1;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                this.mInterface.killBackgroundProcesses(getNextArgRequired(), userArg);
                return 0;
            }
        }
    }

    int runKillAll(PrintWriter printWriter) throws RemoteException {
        this.mInterface.killAllBackgroundProcesses();
        return 0;
    }

    int runMakeIdle(PrintWriter printWriter) throws RemoteException {
        int userArg = -1;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                this.mInterface.makePackageIdle(getNextArgRequired(), userArg);
                return 0;
            }
        }
    }

    static final class MyActivityController extends IActivityController.Stub {
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
        final InputStream mInput;
        final IActivityManager mInterface;
        final boolean mMonkey;
        final PrintWriter mPw;
        int mResult;
        int mState;

        MyActivityController(IActivityManager iActivityManager, PrintWriter printWriter, InputStream inputStream, String str, boolean z) {
            this.mInterface = iActivityManager;
            this.mPw = printWriter;
            this.mInput = inputStream;
            this.mGdbPort = str;
            this.mMonkey = z;
        }

        public boolean activityResuming(String str) {
            synchronized (this) {
                this.mPw.println("** Activity resuming: " + str);
                this.mPw.flush();
            }
            return true;
        }

        public boolean activityStarting(Intent intent, String str) {
            synchronized (this) {
                this.mPw.println("** Activity starting: " + str);
                this.mPw.flush();
            }
            return true;
        }

        public boolean appCrashed(String str, int i, String str2, String str3, long j, String str4) {
            boolean z;
            synchronized (this) {
                this.mPw.println("** ERROR: PROCESS CRASHED");
                this.mPw.println("processName: " + str);
                this.mPw.println("processPid: " + i);
                this.mPw.println("shortMsg: " + str2);
                this.mPw.println("longMsg: " + str3);
                this.mPw.println("timeMillis: " + j);
                this.mPw.println("stack:");
                this.mPw.print(str4);
                this.mPw.println("#");
                this.mPw.flush();
                z = waitControllerLocked(i, 1) != 1;
            }
            return z;
        }

        public int appEarlyNotResponding(String str, int i, String str2) {
            synchronized (this) {
                this.mPw.println("** ERROR: EARLY PROCESS NOT RESPONDING");
                this.mPw.println("processName: " + str);
                this.mPw.println("processPid: " + i);
                this.mPw.println("annotation: " + str2);
                this.mPw.flush();
                return waitControllerLocked(i, 2) == 1 ? -1 : 0;
            }
        }

        public int appNotResponding(String str, int i, String str2) {
            synchronized (this) {
                this.mPw.println("** ERROR: PROCESS NOT RESPONDING");
                this.mPw.println("processName: " + str);
                this.mPw.println("processPid: " + i);
                this.mPw.println("processStats:");
                this.mPw.print(str2);
                this.mPw.println("#");
                this.mPw.flush();
                int iWaitControllerLocked = waitControllerLocked(i, 3);
                if (iWaitControllerLocked == 1) {
                    return -1;
                }
                if (iWaitControllerLocked == 1) {
                    return 1;
                }
                return 0;
            }
        }

        public int systemNotResponding(String str) {
            synchronized (this) {
                this.mPw.println("** ERROR: PROCESS NOT RESPONDING");
                this.mPw.println("message: " + str);
                this.mPw.println("#");
                this.mPw.println("Allowing system to die.");
                this.mPw.flush();
            }
            return -1;
        }

        void killGdbLocked() {
            this.mGotGdbPrint = false;
            if (this.mGdbProcess != null) {
                this.mPw.println("Stopping gdbserver");
                this.mPw.flush();
                this.mGdbProcess.destroy();
                this.mGdbProcess = null;
            }
            if (this.mGdbThread != null) {
                this.mGdbThread.interrupt();
                this.mGdbThread = null;
            }
        }

        int waitControllerLocked(int i, int i2) {
            if (this.mGdbPort != null) {
                killGdbLocked();
                try {
                    this.mPw.println("Starting gdbserver on port " + this.mGdbPort);
                    this.mPw.println("Do the following:");
                    this.mPw.println("  adb forward tcp:" + this.mGdbPort + " tcp:" + this.mGdbPort);
                    PrintWriter printWriter = this.mPw;
                    StringBuilder sb = new StringBuilder();
                    sb.append("  gdbclient app_process :");
                    sb.append(this.mGdbPort);
                    printWriter.println(sb.toString());
                    this.mPw.flush();
                    this.mGdbProcess = Runtime.getRuntime().exec(new String[]{"gdbserver", ":" + this.mGdbPort, "--attach", Integer.toString(i)});
                    final InputStreamReader inputStreamReader = new InputStreamReader(this.mGdbProcess.getInputStream());
                    this.mGdbThread = new Thread() {
                        @Override
                        public void run() {
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                            int i3 = 0;
                            while (true) {
                                synchronized (MyActivityController.this) {
                                    if (MyActivityController.this.mGdbThread == null) {
                                        return;
                                    }
                                    if (i3 == 2) {
                                        MyActivityController.this.mGotGdbPrint = true;
                                        MyActivityController.this.notifyAll();
                                    }
                                    try {
                                        String line = bufferedReader.readLine();
                                        if (line == null) {
                                            return;
                                        }
                                        MyActivityController.this.mPw.println("GDB: " + line);
                                        MyActivityController.this.mPw.flush();
                                        i3++;
                                    } catch (IOException e) {
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
                    this.mPw.println("Failure starting gdbserver: " + e2);
                    this.mPw.flush();
                    killGdbLocked();
                }
            }
            this.mState = i2;
            this.mPw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printMessageForState();
            this.mPw.flush();
            while (this.mState != 0) {
                try {
                    wait();
                } catch (InterruptedException e3) {
                }
            }
            killGdbLocked();
            return this.mResult;
        }

        void resumeController(int i) {
            synchronized (this) {
                this.mState = 0;
                this.mResult = i;
                notifyAll();
            }
        }

        void printMessageForState() {
            switch (this.mState) {
                case 0:
                    this.mPw.println("Monitoring activity manager...  available commands:");
                    break;
                case 1:
                    this.mPw.println("Waiting after crash...  available commands:");
                    this.mPw.println("(c)ontinue: show crash dialog");
                    this.mPw.println("(k)ill: immediately kill app");
                    break;
                case 2:
                    this.mPw.println("Waiting after early ANR...  available commands:");
                    this.mPw.println("(c)ontinue: standard ANR processing");
                    this.mPw.println("(k)ill: immediately kill app");
                    break;
                case 3:
                    this.mPw.println("Waiting after ANR...  available commands:");
                    this.mPw.println("(c)ontinue: show ANR dialog");
                    this.mPw.println("(k)ill: immediately kill app");
                    this.mPw.println("(w)ait: wait some more");
                    break;
            }
            this.mPw.println("(q)uit: finish monitoring");
        }

        void run() throws RemoteException {
            try {
                try {
                    printMessageForState();
                    this.mPw.flush();
                    this.mInterface.setActivityController(this, this.mMonkey);
                    this.mState = 0;
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.mInput));
                    while (true) {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
                        boolean z = true;
                        if (line.length() > 0) {
                            if ("q".equals(line) || "quit".equals(line)) {
                                break;
                            }
                            if (this.mState == 1) {
                                if ("c".equals(line) || "continue".equals(line)) {
                                    resumeController(0);
                                } else if ("k".equals(line) || "kill".equals(line)) {
                                    resumeController(1);
                                } else {
                                    this.mPw.println("Invalid command: " + line);
                                }
                            } else if (this.mState == 3) {
                                if ("c".equals(line) || "continue".equals(line)) {
                                    resumeController(0);
                                } else if ("k".equals(line) || "kill".equals(line) || "w".equals(line) || "wait".equals(line)) {
                                    resumeController(1);
                                } else {
                                    this.mPw.println("Invalid command: " + line);
                                }
                            } else if (this.mState == 2) {
                                if ("c".equals(line) || "continue".equals(line)) {
                                    resumeController(0);
                                } else if ("k".equals(line) || "kill".equals(line)) {
                                    resumeController(1);
                                } else {
                                    this.mPw.println("Invalid command: " + line);
                                }
                            } else {
                                this.mPw.println("Invalid command: " + line);
                            }
                        } else {
                            z = false;
                        }
                        synchronized (this) {
                            if (z) {
                                try {
                                    this.mPw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                } finally {
                                }
                            }
                            printMessageForState();
                            this.mPw.flush();
                        }
                    }
                    resumeController(0);
                } catch (IOException e) {
                    e.printStackTrace(this.mPw);
                    this.mPw.flush();
                }
            } finally {
                this.mInterface.setActivityController((IActivityController) null, this.mMonkey);
            }
        }
    }

    int runMonitor(PrintWriter printWriter) throws RemoteException {
        boolean z = false;
        String nextArgRequired = null;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--gdb")) {
                    nextArgRequired = getNextArgRequired();
                } else if (nextOption.equals("-m")) {
                    z = true;
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                new MyActivityController(this.mInterface, printWriter, getRawInputStream(), nextArgRequired, z).run();
                return 0;
            }
        }
    }

    static final class MyUidObserver extends IUidObserver.Stub implements ActivityManagerService.OomAdjObserver {
        static final int STATE_NORMAL = 0;
        final InputStream mInput;
        final IActivityManager mInterface;
        final ActivityManagerService mInternal;
        final PrintWriter mPw;
        int mState;
        final int mUid;

        MyUidObserver(ActivityManagerService activityManagerService, PrintWriter printWriter, InputStream inputStream, int i) {
            this.mInterface = activityManagerService;
            this.mInternal = activityManagerService;
            this.mPw = printWriter;
            this.mInput = inputStream;
            this.mUid = i;
        }

        public void onUidStateChanged(int i, int i2, long j) throws RemoteException {
            synchronized (this) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(i);
                    this.mPw.print(" procstate ");
                    this.mPw.print(ProcessList.makeProcStateString(i2));
                    this.mPw.print(" seq ");
                    this.mPw.println(j);
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
                }
            }
        }

        public void onUidGone(int i, boolean z) throws RemoteException {
            synchronized (this) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(i);
                    this.mPw.print(" gone");
                    if (z) {
                        this.mPw.print(" disabled");
                    }
                    this.mPw.println();
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
                }
            }
        }

        public void onUidActive(int i) throws RemoteException {
            synchronized (this) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(i);
                    this.mPw.println(" active");
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
                }
            }
        }

        public void onUidIdle(int i, boolean z) throws RemoteException {
            synchronized (this) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(i);
                    this.mPw.print(" idle");
                    if (z) {
                        this.mPw.print(" disabled");
                    }
                    this.mPw.println();
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
                }
            }
        }

        public void onUidCachedChanged(int i, boolean z) throws RemoteException {
            synchronized (this) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print(i);
                    this.mPw.println(z ? " cached" : " uncached");
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
                }
            }
        }

        @Override
        public void onOomAdjMessage(String str) {
            synchronized (this) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
                try {
                    this.mPw.print("# ");
                    this.mPw.println(str);
                    this.mPw.flush();
                } finally {
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
                }
            }
        }

        void printMessageForState() {
            if (this.mState == 0) {
                this.mPw.println("Watching uid states...  available commands:");
            }
            this.mPw.println("(q)uit: finish watching");
        }

        void run() throws RemoteException {
            try {
                try {
                    printMessageForState();
                    this.mPw.flush();
                    this.mInterface.registerUidObserver(this, 31, -1, (String) null);
                    if (this.mUid >= 0) {
                        this.mInternal.setOomAdjObserver(this.mUid, this);
                    }
                    this.mState = 0;
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.mInput));
                    while (true) {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
                        boolean z = true;
                        if (line.length() > 0) {
                            if ("q".equals(line) || "quit".equals(line)) {
                                break;
                            }
                            this.mPw.println("Invalid command: " + line);
                        } else {
                            z = false;
                        }
                        synchronized (this) {
                            if (z) {
                                try {
                                    this.mPw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                } finally {
                                }
                            }
                            printMessageForState();
                            this.mPw.flush();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace(this.mPw);
                    this.mPw.flush();
                    if (this.mUid >= 0) {
                    }
                }
                if (this.mUid >= 0) {
                    this.mInternal.clearOomAdjObserver();
                }
                this.mInterface.unregisterUidObserver(this);
            } catch (Throwable th) {
                if (this.mUid >= 0) {
                    this.mInternal.clearOomAdjObserver();
                }
                this.mInterface.unregisterUidObserver(this);
                throw th;
            }
        }
    }

    int runWatchUids(PrintWriter printWriter) throws RemoteException {
        int i = -1;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--oom")) {
                    i = Integer.parseInt(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                new MyUidObserver(this.mInternal, printWriter, getRawInputStream(), i).run();
                return 0;
            }
        }
    }

    int runHang(PrintWriter printWriter) throws RemoteException {
        boolean z = false;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--allow-restart")) {
                    z = true;
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                printWriter.println("Hanging the system...");
                printWriter.flush();
                this.mInterface.hang(new Binder(), z);
                return 0;
            }
        }
    }

    int runRestart(PrintWriter printWriter) throws RemoteException {
        String nextOption = getNextOption();
        if (nextOption != null) {
            getErrPrintWriter().println("Error: Unknown option: " + nextOption);
            return -1;
        }
        printWriter.println("Restart the system...");
        printWriter.flush();
        this.mInterface.restart();
        return 0;
    }

    int runIdleMaintenance(PrintWriter printWriter) throws RemoteException {
        String nextOption = getNextOption();
        if (nextOption != null) {
            getErrPrintWriter().println("Error: Unknown option: " + nextOption);
            return -1;
        }
        printWriter.println("Performing idle maintenance...");
        this.mInterface.sendIdleJobTrigger();
        return 0;
    }

    int runScreenCompat(PrintWriter printWriter) throws RemoteException {
        boolean z;
        String nextArgRequired = getNextArgRequired();
        if (!"on".equals(nextArgRequired)) {
            if (!"off".equals(nextArgRequired)) {
                getErrPrintWriter().println("Error: enabled mode must be 'on' or 'off' at " + nextArgRequired);
                return -1;
            }
            z = false;
        } else {
            z = true;
        }
        String nextArgRequired2 = getNextArgRequired();
        do {
            try {
                this.mInterface.setPackageScreenCompatMode(nextArgRequired2, z ? 1 : 0);
            } catch (RemoteException e) {
            }
            nextArgRequired2 = getNextArg();
        } while (nextArgRequired2 != null);
        return 0;
    }

    int runPackageImportance(PrintWriter printWriter) throws RemoteException {
        printWriter.println(ActivityManager.RunningAppProcessInfo.procStateToImportance(this.mInterface.getPackageProcessState(getNextArgRequired(), SHELL_PACKAGE_NAME)));
        return 0;
    }

    int runToUri(PrintWriter printWriter, int i) throws RemoteException {
        try {
            printWriter.println(makeIntent(-2).toUri(i));
            return 0;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runSwitchUser(PrintWriter printWriter) throws RemoteException {
        if (!((UserManager) this.mInternal.mContext.getSystemService(UserManager.class)).canSwitchUsers()) {
            getErrPrintWriter().println("Error: disallowed switching user");
            return -1;
        }
        this.mInterface.switchUser(Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runGetCurrentUser(PrintWriter printWriter) throws RemoteException {
        printWriter.println(((UserInfo) Preconditions.checkNotNull(this.mInterface.getCurrentUser(), "Current user not set")).id);
        return 0;
    }

    int runStartUser(PrintWriter printWriter) throws RemoteException {
        if (this.mInterface.startUserInBackground(Integer.parseInt(getNextArgRequired()))) {
            printWriter.println("Success: user started");
            return 0;
        }
        getErrPrintWriter().println("Error: could not start user");
        return 0;
    }

    private static byte[] argToBytes(String str) {
        if (str.equals("!")) {
            return null;
        }
        return HexDump.hexStringToByteArray(str);
    }

    int runUnlockUser(PrintWriter printWriter) throws RemoteException {
        if (this.mInterface.unlockUser(Integer.parseInt(getNextArgRequired()), argToBytes(getNextArgRequired()), argToBytes(getNextArgRequired()), (IProgressListener) null)) {
            printWriter.println("Success: user unlocked");
            return 0;
        }
        getErrPrintWriter().println("Error: could not unlock user");
        return 0;
    }

    static final class StopUserCallback extends IStopUserCallback.Stub {
        private boolean mFinished = false;

        StopUserCallback() {
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

        public synchronized void userStopped(int i) {
            this.mFinished = true;
            notifyAll();
        }

        public synchronized void userStopAborted(int i) {
            this.mFinished = true;
            notifyAll();
        }
    }

    int runStopUser(PrintWriter printWriter) throws RemoteException {
        boolean z = false;
        boolean z2 = false;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if ("-w".equals(nextOption)) {
                    z = true;
                } else if ("-f".equals(nextOption)) {
                    z2 = true;
                } else {
                    getErrPrintWriter().println("Error: unknown option: " + nextOption);
                    return -1;
                }
            } else {
                int i = Integer.parseInt(getNextArgRequired());
                StopUserCallback stopUserCallback = z ? new StopUserCallback() : null;
                int iStopUser = this.mInterface.stopUser(i, z2, stopUserCallback);
                if (iStopUser != 0) {
                    String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    switch (iStopUser) {
                        case -4:
                            str = " (Can't stop user " + i + " - one of its related users can't be stopped)";
                            break;
                        case -3:
                            str = " (System user cannot be stopped)";
                            break;
                        case -2:
                            str = " (Can't stop current user)";
                            break;
                        case -1:
                            str = " (Unknown user " + i + ")";
                            break;
                    }
                    getErrPrintWriter().println("Switch failed: " + iStopUser + str);
                    return -1;
                }
                if (stopUserCallback != null) {
                    stopUserCallback.waitForFinish();
                }
                return 0;
            }
        }
    }

    int runIsUserStopped(PrintWriter printWriter) {
        printWriter.println(this.mInternal.isUserStopped(UserHandle.parseUserArg(getNextArgRequired())));
        return 0;
    }

    int runGetStartedUserState(PrintWriter printWriter) throws RemoteException {
        this.mInternal.enforceCallingPermission("android.permission.DUMP", "runGetStartedUserState()");
        int i = Integer.parseInt(getNextArgRequired());
        try {
            printWriter.println(this.mInternal.getStartedUserState(i));
            return 0;
        } catch (NullPointerException e) {
            printWriter.println("User is not started: " + i);
            return 0;
        }
    }

    int runTrackAssociations(PrintWriter printWriter) {
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "registerUidObserver()");
        synchronized (this.mInternal) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (!this.mInternal.mTrackingAssociations) {
                    this.mInternal.mTrackingAssociations = true;
                    printWriter.println("Association tracking started.");
                } else {
                    printWriter.println("Association tracking already enabled.");
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return 0;
    }

    int runUntrackAssociations(PrintWriter printWriter) {
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "registerUidObserver()");
        synchronized (this.mInternal) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mInternal.mTrackingAssociations) {
                    this.mInternal.mTrackingAssociations = false;
                    this.mInternal.mAssociations.clear();
                    printWriter.println("Association tracking stopped.");
                } else {
                    printWriter.println("Association tracking not running.");
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return 0;
    }

    int getUidState(PrintWriter printWriter) throws RemoteException {
        this.mInternal.enforceCallingPermission("android.permission.DUMP", "getUidState()");
        int uidState = this.mInternal.getUidState(Integer.parseInt(getNextArgRequired()));
        printWriter.print(uidState);
        printWriter.print(" (");
        printWriter.printf(DebugUtils.valueToString(ActivityManager.class, "PROCESS_STATE_", uidState), new Object[0]);
        printWriter.println(")");
        return 0;
    }

    private List<Configuration> getRecentConfigurations(int i) {
        IUsageStatsManager iUsageStatsManagerAsInterface = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
        long jCurrentTimeMillis = System.currentTimeMillis();
        try {
            ParceledListSlice parceledListSliceQueryConfigurationStats = iUsageStatsManagerAsInterface.queryConfigurationStats(4, jCurrentTimeMillis - ((long) ((((i * 24) * 60) * 60) * 1000)), jCurrentTimeMillis, SHELL_PACKAGE_NAME);
            if (parceledListSliceQueryConfigurationStats == null) {
                return Collections.emptyList();
            }
            final ArrayMap arrayMap = new ArrayMap();
            List list = parceledListSliceQueryConfigurationStats.getList();
            int size = list.size();
            for (int i2 = 0; i2 < size; i2++) {
                ConfigurationStats configurationStats = (ConfigurationStats) list.get(i2);
                int iIndexOfKey = arrayMap.indexOfKey(configurationStats.getConfiguration());
                if (iIndexOfKey < 0) {
                    arrayMap.put(configurationStats.getConfiguration(), Integer.valueOf(configurationStats.getActivationCount()));
                } else {
                    arrayMap.setValueAt(iIndexOfKey, Integer.valueOf(((Integer) arrayMap.valueAt(iIndexOfKey)).intValue() + configurationStats.getActivationCount()));
                }
            }
            Comparator<Configuration> comparator = new Comparator<Configuration>() {
                @Override
                public int compare(Configuration configuration, Configuration configuration2) {
                    return ((Integer) arrayMap.get(configuration2)).compareTo((Integer) arrayMap.get(configuration));
                }
            };
            ArrayList arrayList = new ArrayList(arrayMap.size());
            arrayList.addAll(arrayMap.keySet());
            Collections.sort(arrayList, comparator);
            return arrayList;
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    private static void addExtensionsForConfig(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig eGLConfig, int[] iArr, int[] iArr2, Set<String> set) {
        EGLContext eGLContextEglCreateContext = egl10.eglCreateContext(eGLDisplay, eGLConfig, EGL10.EGL_NO_CONTEXT, iArr2);
        if (eGLContextEglCreateContext == EGL10.EGL_NO_CONTEXT) {
            return;
        }
        EGLSurface eGLSurfaceEglCreatePbufferSurface = egl10.eglCreatePbufferSurface(eGLDisplay, eGLConfig, iArr);
        if (eGLSurfaceEglCreatePbufferSurface == EGL10.EGL_NO_SURFACE) {
            egl10.eglDestroyContext(eGLDisplay, eGLContextEglCreateContext);
            return;
        }
        egl10.eglMakeCurrent(eGLDisplay, eGLSurfaceEglCreatePbufferSurface, eGLSurfaceEglCreatePbufferSurface, eGLContextEglCreateContext);
        String strGlGetString = GLES10.glGetString(7939);
        if (!TextUtils.isEmpty(strGlGetString)) {
            for (String str : strGlGetString.split(" ")) {
                set.add(str);
            }
        }
        egl10.eglMakeCurrent(eGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl10.eglDestroySurface(eGLDisplay, eGLSurfaceEglCreatePbufferSurface);
        egl10.eglDestroyContext(eGLDisplay, eGLContextEglCreateContext);
    }

    Set<String> getGlExtensionsFromDriver() {
        int i;
        HashSet hashSet = new HashSet();
        EGL10 egl10 = (EGL10) EGLContext.getEGL();
        if (egl10 == null) {
            getErrPrintWriter().println("Warning: couldn't get EGL");
            return hashSet;
        }
        EGLDisplay eGLDisplayEglGetDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl10.eglInitialize(eGLDisplayEglGetDisplay, new int[2]);
        int[] iArr = new int[1];
        if (!egl10.eglGetConfigs(eGLDisplayEglGetDisplay, null, 0, iArr)) {
            getErrPrintWriter().println("Warning: couldn't get EGL config count");
            return hashSet;
        }
        EGLConfig[] eGLConfigArr = new EGLConfig[iArr[0]];
        if (!egl10.eglGetConfigs(eGLDisplayEglGetDisplay, eGLConfigArr, iArr[0], iArr)) {
            getErrPrintWriter().println("Warning: couldn't get EGL configs");
            return hashSet;
        }
        int[] iArr2 = {12375, 1, 12374, 1, 12344};
        int[] iArr3 = {12440, 2, 12344};
        int[] iArr4 = new int[1];
        for (int i2 = 0; i2 < iArr[0]; i2 = i + 1) {
            egl10.eglGetConfigAttrib(eGLDisplayEglGetDisplay, eGLConfigArr[i2], 12327, iArr4);
            if (iArr4[0] == 12368) {
                i = i2;
            } else {
                egl10.eglGetConfigAttrib(eGLDisplayEglGetDisplay, eGLConfigArr[i2], 12339, iArr4);
                if ((iArr4[0] & 1) != 0) {
                    egl10.eglGetConfigAttrib(eGLDisplayEglGetDisplay, eGLConfigArr[i2], 12352, iArr4);
                    if ((iArr4[0] & 1) != 0) {
                        i = i2;
                        addExtensionsForConfig(egl10, eGLDisplayEglGetDisplay, eGLConfigArr[i2], iArr2, null, hashSet);
                    } else {
                        i = i2;
                    }
                    if ((iArr4[0] & 4) != 0) {
                        addExtensionsForConfig(egl10, eGLDisplayEglGetDisplay, eGLConfigArr[i], iArr2, iArr3, hashSet);
                    }
                }
            }
        }
        egl10.eglTerminate(eGLDisplayEglGetDisplay);
        return hashSet;
    }

    private void writeDeviceConfig(ProtoOutputStream protoOutputStream, long j, PrintWriter printWriter, Configuration configuration, DisplayManager displayManager) {
        long jStart;
        Point stableDisplaySize = displayManager.getStableDisplaySize();
        if (protoOutputStream != null) {
            jStart = protoOutputStream.start(j);
            protoOutputStream.write(1155346202625L, stableDisplaySize.x);
            protoOutputStream.write(1155346202626L, stableDisplaySize.y);
            protoOutputStream.write(1155346202627L, DisplayMetrics.DENSITY_DEVICE_STABLE);
        } else {
            jStart = -1;
        }
        if (printWriter != null) {
            printWriter.print("stable-width-px: ");
            printWriter.println(stableDisplaySize.x);
            printWriter.print("stable-height-px: ");
            printWriter.println(stableDisplaySize.y);
            printWriter.print("stable-density-dpi: ");
            printWriter.println(DisplayMetrics.DENSITY_DEVICE_STABLE);
        }
        MemInfoReader memInfoReader = new MemInfoReader();
        memInfoReader.readMemInfo();
        KeyguardManager keyguardManager = (KeyguardManager) this.mInternal.mContext.getSystemService(KeyguardManager.class);
        if (protoOutputStream != null) {
            protoOutputStream.write(1116691496964L, memInfoReader.getTotalSize());
            protoOutputStream.write(1133871366149L, ActivityManager.isLowRamDeviceStatic());
            protoOutputStream.write(1155346202630L, Runtime.getRuntime().availableProcessors());
            protoOutputStream.write(1133871366151L, keyguardManager.isDeviceSecure());
        }
        if (printWriter != null) {
            printWriter.print("total-ram: ");
            printWriter.println(memInfoReader.getTotalSize());
            printWriter.print("low-ram: ");
            printWriter.println(ActivityManager.isLowRamDeviceStatic());
            printWriter.print("max-cores: ");
            printWriter.println(Runtime.getRuntime().availableProcessors());
            printWriter.print("has-secure-screen-lock: ");
            printWriter.println(keyguardManager.isDeviceSecure());
        }
        ConfigurationInfo deviceConfigurationInfo = this.mInternal.getDeviceConfigurationInfo();
        if (deviceConfigurationInfo.reqGlEsVersion != 0) {
            if (protoOutputStream != null) {
                protoOutputStream.write(1155346202632L, deviceConfigurationInfo.reqGlEsVersion);
            }
            if (printWriter != null) {
                printWriter.print("opengl-version: 0x");
                printWriter.println(Integer.toHexString(deviceConfigurationInfo.reqGlEsVersion));
            }
        }
        Set<String> glExtensionsFromDriver = getGlExtensionsFromDriver();
        String[] strArr = (String[]) glExtensionsFromDriver.toArray(new String[glExtensionsFromDriver.size()]);
        Arrays.sort(strArr);
        for (int i = 0; i < strArr.length; i++) {
            if (protoOutputStream != null) {
                protoOutputStream.write(2237677961225L, strArr[i]);
            }
            if (printWriter != null) {
                printWriter.print("opengl-extensions: ");
                printWriter.println(strArr[i]);
            }
        }
        PackageManager packageManager = this.mInternal.mContext.getPackageManager();
        List<SharedLibraryInfo> sharedLibraries = packageManager.getSharedLibraries(0);
        Collections.sort(sharedLibraries, Comparator.comparing(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((SharedLibraryInfo) obj).getName();
            }
        }));
        for (int i2 = 0; i2 < sharedLibraries.size(); i2++) {
            if (protoOutputStream != null) {
                protoOutputStream.write(2237677961226L, sharedLibraries.get(i2).getName());
            }
            if (printWriter != null) {
                printWriter.print("shared-libraries: ");
                printWriter.println(sharedLibraries.get(i2).getName());
            }
        }
        FeatureInfo[] systemAvailableFeatures = packageManager.getSystemAvailableFeatures();
        Arrays.sort(systemAvailableFeatures, new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return ActivityManagerShellCommand.lambda$writeDeviceConfig$0((FeatureInfo) obj, (FeatureInfo) obj2);
            }
        });
        for (int i3 = 0; i3 < systemAvailableFeatures.length; i3++) {
            if (systemAvailableFeatures[i3].name != null) {
                if (protoOutputStream != null) {
                    protoOutputStream.write(2237677961227L, systemAvailableFeatures[i3].name);
                }
                if (printWriter != null) {
                    printWriter.print("features: ");
                    printWriter.println(systemAvailableFeatures[i3].name);
                }
            }
        }
        if (protoOutputStream != null) {
            protoOutputStream.end(jStart);
        }
    }

    static int lambda$writeDeviceConfig$0(FeatureInfo featureInfo, FeatureInfo featureInfo2) {
        if (featureInfo.name == featureInfo2.name) {
            return 0;
        }
        if (featureInfo.name == null) {
            return -1;
        }
        return featureInfo.name.compareTo(featureInfo2.name);
    }

    int runGetConfig(PrintWriter printWriter) throws RemoteException {
        List<Configuration> recentConfigurations;
        int size;
        int i = -1;
        boolean z = false;
        boolean z2 = false;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--days")) {
                    i = Integer.parseInt(getNextArgRequired());
                    if (i <= 0) {
                        throw new IllegalArgumentException("--days must be a positive integer");
                    }
                } else if (nextOption.equals(PriorityDump.PROTO_ARG)) {
                    z = true;
                } else if (nextOption.equals("--device")) {
                    z2 = true;
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                Configuration configuration = this.mInterface.getConfiguration();
                if (configuration == null) {
                    getErrPrintWriter().println("Activity manager has no configuration");
                    return -1;
                }
                DisplayManager displayManager = (DisplayManager) this.mInternal.mContext.getSystemService(DisplayManager.class);
                Display display = displayManager.getDisplay(0);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                display.getMetrics(displayMetrics);
                if (z) {
                    ProtoOutputStream protoOutputStream = new ProtoOutputStream(getOutFileDescriptor());
                    configuration.writeResConfigToProto(protoOutputStream, 1146756268033L, displayMetrics);
                    if (z2) {
                        writeDeviceConfig(protoOutputStream, 1146756268034L, null, configuration, displayManager);
                    }
                    protoOutputStream.flush();
                } else {
                    printWriter.println("config: " + Configuration.resourceQualifierString(configuration, displayMetrics));
                    printWriter.println("abi: " + TextUtils.join(",", Build.SUPPORTED_ABIS));
                    if (z2) {
                        writeDeviceConfig(null, -1L, printWriter, configuration, displayManager);
                    }
                    if (i >= 0 && (size = (recentConfigurations = getRecentConfigurations(i)).size()) > 0) {
                        printWriter.println("recentConfigs:");
                        for (int i2 = 0; i2 < size; i2++) {
                            printWriter.println("  config: " + Configuration.resourceQualifierString(recentConfigurations.get(i2)));
                        }
                    }
                }
                return 0;
            }
        }
    }

    int runSuppressResizeConfigChanges(PrintWriter printWriter) throws RemoteException {
        this.mInterface.suppressResizeConfigChanges(Boolean.valueOf(getNextArgRequired()).booleanValue());
        return 0;
    }

    int runSetInactive(PrintWriter printWriter) throws RemoteException {
        int userArg = -2;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats")).setAppInactive(getNextArgRequired(), Boolean.parseBoolean(getNextArgRequired()), userArg);
                return 0;
            }
        }
    }

    private int bucketNameToBucketValue(String str) {
        String lowerCase = str.toLowerCase();
        if (lowerCase.startsWith("ac")) {
            return 10;
        }
        if (lowerCase.startsWith("wo")) {
            return 20;
        }
        if (lowerCase.startsWith("fr")) {
            return 30;
        }
        if (lowerCase.startsWith("ra")) {
            return 40;
        }
        if (lowerCase.startsWith("ne")) {
            return 50;
        }
        try {
            return Integer.parseInt(lowerCase);
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: Unknown bucket: " + str);
            return -1;
        }
    }

    int runSetStandbyBucket(PrintWriter printWriter) throws RemoteException {
        int userArg = -2;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                String nextArgRequired = getNextArgRequired();
                String nextArgRequired2 = getNextArgRequired();
                int iBucketNameToBucketValue = bucketNameToBucketValue(nextArgRequired2);
                if (iBucketNameToBucketValue < 0) {
                    return -1;
                }
                boolean z = peekNextArg() != null;
                IUsageStatsManager iUsageStatsManagerAsInterface = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
                if (!z) {
                    iUsageStatsManagerAsInterface.setAppStandbyBucket(nextArgRequired, bucketNameToBucketValue(nextArgRequired2), userArg);
                } else {
                    ArrayList arrayList = new ArrayList();
                    arrayList.add(new AppStandbyInfo(nextArgRequired, iBucketNameToBucketValue));
                    while (true) {
                        String nextArg = getNextArg();
                        if (nextArg == null) {
                            break;
                        }
                        int iBucketNameToBucketValue2 = bucketNameToBucketValue(getNextArgRequired());
                        if (iBucketNameToBucketValue2 >= 0) {
                            arrayList.add(new AppStandbyInfo(nextArg, iBucketNameToBucketValue2));
                        }
                    }
                    iUsageStatsManagerAsInterface.setAppStandbyBuckets(new ParceledListSlice(arrayList), userArg);
                }
                return 0;
            }
        }
    }

    int runGetStandbyBucket(PrintWriter printWriter) throws RemoteException {
        int userArg = -2;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                String nextArg = getNextArg();
                IUsageStatsManager iUsageStatsManagerAsInterface = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
                if (nextArg != null) {
                    printWriter.println(iUsageStatsManagerAsInterface.getAppStandbyBucket(nextArg, (String) null, userArg));
                    return 0;
                }
                for (AppStandbyInfo appStandbyInfo : iUsageStatsManagerAsInterface.getAppStandbyBuckets(SHELL_PACKAGE_NAME, userArg).getList()) {
                    printWriter.print(appStandbyInfo.mPackageName);
                    printWriter.print(": ");
                    printWriter.println(appStandbyInfo.mStandbyBucket);
                }
                return 0;
            }
        }
    }

    int runGetInactive(PrintWriter printWriter) throws RemoteException {
        int userArg = -2;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                printWriter.println("Idle=" + IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats")).isAppInactive(getNextArgRequired(), userArg));
                return 0;
            }
        }
    }

    int runSendTrimMemory(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired;
        int i;
        int userArg = -2;
        do {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (nextOption.equals("--user")) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + nextOption);
                    return -1;
                }
            } else {
                String nextArgRequired2 = getNextArgRequired();
                nextArgRequired = getNextArgRequired();
                i = 5;
                switch (nextArgRequired) {
                    case "HIDDEN":
                        i = 20;
                        break;
                    case "RUNNING_MODERATE":
                        break;
                    case "BACKGROUND":
                        i = 40;
                        break;
                    case "RUNNING_LOW":
                        i = 10;
                        break;
                    case "MODERATE":
                        i = 60;
                        break;
                    case "RUNNING_CRITICAL":
                        i = 15;
                        break;
                    case "COMPLETE":
                        i = 80;
                        break;
                    default:
                        try {
                            i = Integer.parseInt(nextArgRequired);
                            break;
                        } catch (NumberFormatException e) {
                            getErrPrintWriter().println("Error: Unknown level option: " + nextArgRequired);
                            return -1;
                        }
                        break;
                }
                if (this.mInterface.setProcessMemoryTrimLevel(nextArgRequired2, userArg, i)) {
                    return 0;
                }
                getErrPrintWriter().println("Unknown error: failed to set trim level");
                return -1;
            }
        } while (userArg != -1);
        getErrPrintWriter().println("Error: Can't use user 'all'");
        return -1;
    }

    int runDisplay(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired = getNextArgRequired();
        if (((nextArgRequired.hashCode() == 1625698700 && nextArgRequired.equals("move-stack")) ? (byte) 0 : (byte) -1) == 0) {
            return runDisplayMoveStack(printWriter);
        }
        getErrPrintWriter().println("Error: unknown command '" + nextArgRequired + "'");
        return -1;
    }

    int runStack(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired;
        nextArgRequired = getNextArgRequired();
        switch (nextArgRequired) {
            case "start":
                return runStackStart(printWriter);
            case "move-task":
                return runStackMoveTask(printWriter);
            case "resize":
                return runStackResize(printWriter);
            case "resize-animated":
                return runStackResizeAnimated(printWriter);
            case "resize-docked-stack":
                return runStackResizeDocked(printWriter);
            case "positiontask":
                return runStackPositionTask(printWriter);
            case "list":
                return runStackList(printWriter);
            case "info":
                return runStackInfo(printWriter);
            case "move-top-activity-to-pinned-stack":
                return runMoveTopActivityToPinnedStack(printWriter);
            case "remove":
                return runStackRemove(printWriter);
            default:
                getErrPrintWriter().println("Error: unknown command '" + nextArgRequired + "'");
                return -1;
        }
    }

    private Rect getBounds() {
        String nextArgRequired = getNextArgRequired();
        int i = Integer.parseInt(nextArgRequired);
        String nextArgRequired2 = getNextArgRequired();
        int i2 = Integer.parseInt(nextArgRequired2);
        String nextArgRequired3 = getNextArgRequired();
        int i3 = Integer.parseInt(nextArgRequired3);
        String nextArgRequired4 = getNextArgRequired();
        int i4 = Integer.parseInt(nextArgRequired4);
        if (i < 0) {
            getErrPrintWriter().println("Error: bad left arg: " + nextArgRequired);
            return null;
        }
        if (i2 < 0) {
            getErrPrintWriter().println("Error: bad top arg: " + nextArgRequired2);
            return null;
        }
        if (i3 <= 0) {
            getErrPrintWriter().println("Error: bad right arg: " + nextArgRequired3);
            return null;
        }
        if (i4 <= 0) {
            getErrPrintWriter().println("Error: bad bottom arg: " + nextArgRequired4);
            return null;
        }
        return new Rect(i, i2, i3, i4);
    }

    int runDisplayMoveStack(PrintWriter printWriter) throws RemoteException {
        this.mInterface.moveStackToDisplay(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runStackStart(PrintWriter printWriter) throws RemoteException {
        int i = Integer.parseInt(getNextArgRequired());
        try {
            makeIntent(-2);
            this.mInterface.createStackOnDisplay(i);
            return 0;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    int runStackMoveTask(PrintWriter printWriter) throws RemoteException {
        boolean z;
        int i = Integer.parseInt(getNextArgRequired());
        int i2 = Integer.parseInt(getNextArgRequired());
        String nextArgRequired = getNextArgRequired();
        if ("true".equals(nextArgRequired)) {
            z = true;
        } else {
            if (!"false".equals(nextArgRequired)) {
                getErrPrintWriter().println("Error: bad toTop arg: " + nextArgRequired);
                return -1;
            }
            z = false;
        }
        this.mInterface.moveTaskToStack(i, i2, z);
        return 0;
    }

    int runStackResize(PrintWriter printWriter) throws RemoteException {
        int i = Integer.parseInt(getNextArgRequired());
        Rect bounds = getBounds();
        if (bounds == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        }
        return resizeStack(i, bounds, 0);
    }

    int runStackResizeAnimated(PrintWriter printWriter) throws RemoteException {
        Rect bounds;
        int i = Integer.parseInt(getNextArgRequired());
        if ("null".equals(peekNextArg())) {
            bounds = null;
        } else {
            bounds = getBounds();
            if (bounds == null) {
                getErrPrintWriter().println("Error: invalid input bounds");
                return -1;
            }
        }
        return resizeStackUnchecked(i, bounds, 0, true);
    }

    int resizeStackUnchecked(int i, Rect rect, int i2, boolean z) throws RemoteException {
        try {
            this.mInterface.resizeStack(i, rect, false, false, z, -1);
            Thread.sleep(i2);
            return 0;
        } catch (InterruptedException e) {
            return 0;
        }
    }

    int runStackResizeDocked(PrintWriter printWriter) throws RemoteException {
        Rect bounds = getBounds();
        Rect bounds2 = getBounds();
        if (bounds == null || bounds2 == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        }
        this.mInterface.resizeDockedStack(bounds, bounds2, (Rect) null, (Rect) null, (Rect) null);
        return 0;
    }

    int resizeStack(int i, Rect rect, int i2) throws RemoteException {
        if (rect == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        }
        return resizeStackUnchecked(i, rect, i2, false);
    }

    int runStackPositionTask(PrintWriter printWriter) throws RemoteException {
        this.mInterface.positionTaskInStack(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runStackList(PrintWriter printWriter) throws RemoteException {
        Iterator it = this.mInterface.getAllStackInfos().iterator();
        while (it.hasNext()) {
            printWriter.println((ActivityManager.StackInfo) it.next());
        }
        return 0;
    }

    int runStackInfo(PrintWriter printWriter) throws RemoteException {
        printWriter.println(this.mInterface.getStackInfo(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired())));
        return 0;
    }

    int runStackRemove(PrintWriter printWriter) throws RemoteException {
        this.mInterface.removeStack(Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runMoveTopActivityToPinnedStack(PrintWriter printWriter) throws RemoteException {
        int i = Integer.parseInt(getNextArgRequired());
        Rect bounds = getBounds();
        if (bounds == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        }
        if (!this.mInterface.moveTopActivityToPinnedStack(i, bounds)) {
            getErrPrintWriter().println("Didn't move top activity to pinned stack.");
            return -1;
        }
        return 0;
    }

    void setBoundsSide(Rect rect, String str, int i) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 98) {
            if (iHashCode != 108) {
                if (iHashCode != 114) {
                    b = (iHashCode == 116 && str.equals("t")) ? (byte) 2 : (byte) -1;
                } else if (str.equals("r")) {
                    b = 1;
                }
            } else if (str.equals("l")) {
                b = 0;
            }
        } else if (str.equals("b")) {
            b = 3;
        }
        switch (b) {
            case 0:
                rect.left = i;
                break;
            case 1:
                rect.right = i;
                break;
            case 2:
                rect.top = i;
                break;
            case 3:
                rect.bottom = i;
                break;
            default:
                getErrPrintWriter().println("Unknown set side: " + str);
                break;
        }
    }

    int runTask(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired = getNextArgRequired();
        if (nextArgRequired.equals("lock")) {
            return runTaskLock(printWriter);
        }
        if (nextArgRequired.equals("resizeable")) {
            return runTaskResizeable(printWriter);
        }
        if (nextArgRequired.equals("resize")) {
            return runTaskResize(printWriter);
        }
        if (nextArgRequired.equals("focus")) {
            return runTaskFocus(printWriter);
        }
        getErrPrintWriter().println("Error: unknown command '" + nextArgRequired + "'");
        return -1;
    }

    int runTaskLock(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired = getNextArgRequired();
        if (nextArgRequired.equals("stop")) {
            this.mInterface.stopSystemLockTaskMode();
        } else {
            this.mInterface.startSystemLockTaskMode(Integer.parseInt(nextArgRequired));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Activity manager is ");
        sb.append(this.mInterface.isInLockTaskMode() ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "not ");
        sb.append("in lockTaskMode");
        printWriter.println(sb.toString());
        return 0;
    }

    int runTaskResizeable(PrintWriter printWriter) throws RemoteException {
        this.mInterface.setTaskResizeable(Integer.parseInt(getNextArgRequired()), Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    int runTaskResize(PrintWriter printWriter) throws RemoteException {
        int i = Integer.parseInt(getNextArgRequired());
        Rect bounds = getBounds();
        if (bounds == null) {
            getErrPrintWriter().println("Error: invalid input bounds");
            return -1;
        }
        taskResize(i, bounds, 0, false);
        return 0;
    }

    void taskResize(int i, Rect rect, int i2, boolean z) throws RemoteException {
        this.mInterface.resizeTask(i, rect, z ? 1 : 0);
        try {
            Thread.sleep(i2);
        } catch (InterruptedException e) {
        }
    }

    int moveTask(int i, Rect rect, Rect rect2, int i2, int i3, boolean z, boolean z2, int i4) throws RemoteException {
        if (z) {
            while (i3 > 0 && ((z2 && rect.right < rect2.right) || (!z2 && rect.bottom < rect2.bottom))) {
                if (z2) {
                    int iMin = Math.min(i2, rect2.right - rect.right);
                    i3 -= iMin;
                    rect.right += iMin;
                    rect.left += iMin;
                } else {
                    int iMin2 = Math.min(i2, rect2.bottom - rect.bottom);
                    i3 -= iMin2;
                    rect.top += iMin2;
                    rect.bottom += iMin2;
                }
                taskResize(i, rect, i4, false);
            }
        } else {
            while (i3 < 0 && ((z2 && rect.left > rect2.left) || (!z2 && rect.top > rect2.top))) {
                if (z2) {
                    int iMin3 = Math.min(i2, rect.left - rect2.left);
                    i3 -= iMin3;
                    rect.right -= iMin3;
                    rect.left -= iMin3;
                } else {
                    int iMin4 = Math.min(i2, rect.top - rect2.top);
                    i3 -= iMin4;
                    rect.top -= iMin4;
                    rect.bottom -= iMin4;
                }
                taskResize(i, rect, i4, false);
            }
        }
        return i3;
    }

    int getStepSize(int i, int i2, int i3, boolean z) {
        int i4;
        if (!z || i2 >= i) {
            i4 = 0;
        } else {
            i -= i3;
            if (i2 > i) {
                i4 = i3 - (i2 - i);
            } else {
                i4 = i3;
            }
        }
        if (!z && i2 > i) {
            int i5 = i + i3;
            return i2 < i5 ? i3 + (i5 - i2) : i3;
        }
        return i4;
    }

    int runTaskFocus(PrintWriter printWriter) throws RemoteException {
        int i = Integer.parseInt(getNextArgRequired());
        printWriter.println("Setting focus to task " + i);
        this.mInterface.setFocusedTask(i);
        return 0;
    }

    int runWrite(PrintWriter printWriter) {
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "registerUidObserver()");
        this.mInternal.getRecentTasks().flush();
        printWriter.println("All tasks persisted.");
        return 0;
    }

    int runAttachAgent(PrintWriter printWriter) {
        this.mInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "attach-agent");
        String nextArgRequired = getNextArgRequired();
        String nextArgRequired2 = getNextArgRequired();
        String nextArg = getNextArg();
        if (nextArg != null) {
            printWriter.println("Error: Unknown option: " + nextArg);
            return -1;
        }
        this.mInternal.attachAgent(nextArgRequired, nextArgRequired2);
        return 0;
    }

    int runSupportsMultiwindow(PrintWriter printWriter) throws RemoteException {
        if (getResources(printWriter) == null) {
            return -1;
        }
        printWriter.println(ActivityManager.supportsMultiWindow(this.mInternal.mContext));
        return 0;
    }

    int runSupportsSplitScreenMultiwindow(PrintWriter printWriter) throws RemoteException {
        if (getResources(printWriter) == null) {
            return -1;
        }
        printWriter.println(ActivityManager.supportsSplitScreenMultiWindow(this.mInternal.mContext));
        return 0;
    }

    int runUpdateApplicationInfo(PrintWriter printWriter) throws RemoteException {
        int userArg = UserHandle.parseUserArg(getNextArgRequired());
        ArrayList arrayList = new ArrayList();
        arrayList.add(getNextArgRequired());
        while (true) {
            String nextArg = getNextArg();
            if (nextArg != null) {
                arrayList.add(nextArg);
            } else {
                this.mInternal.scheduleApplicationInfoChanged(arrayList, userArg);
                printWriter.println("Packages updated with most recent ApplicationInfos.");
                return 0;
            }
        }
    }

    int runNoHomeScreen(PrintWriter printWriter) throws RemoteException {
        Resources resources = getResources(printWriter);
        if (resources == null) {
            return -1;
        }
        printWriter.println(resources.getBoolean(R.^attr-private.lightZ));
        return 0;
    }

    int runWaitForBroadcastIdle(PrintWriter printWriter) throws RemoteException {
        this.mInternal.waitForBroadcastIdle(printWriter);
        return 0;
    }

    private Resources getResources(PrintWriter printWriter) throws RemoteException {
        Configuration configuration = this.mInterface.getConfiguration();
        if (configuration == null) {
            printWriter.println("Error: Activity manager has no configuration");
            return null;
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.setToDefaults();
        return new Resources(AssetManager.getSystem(), displayMetrics, configuration);
    }

    public void onHelp() {
        dumpHelp(getOutPrintWriter(), this.mDumping);
    }

    static void dumpHelp(PrintWriter printWriter, boolean z) {
        if (z) {
            printWriter.println("Activity manager dump options:");
            printWriter.println("  [-a] [-c] [-p PACKAGE] [-h] [WHAT] ...");
            printWriter.println("  WHAT may be one of:");
            printWriter.println("    a[ctivities]: activity stack state");
            printWriter.println("    r[recents]: recent activities state");
            printWriter.println("    b[roadcasts] [PACKAGE_NAME] [history [-s]]: broadcast state");
            printWriter.println("    broadcast-stats [PACKAGE_NAME]: aggregated broadcast statistics");
            printWriter.println("    i[ntents] [PACKAGE_NAME]: pending intent state");
            printWriter.println("    p[rocesses] [PACKAGE_NAME]: process state");
            printWriter.println("    o[om]: out of memory management");
            printWriter.println("    perm[issions]: URI permission grant state");
            printWriter.println("    prov[iders] [COMP_SPEC ...]: content provider state");
            printWriter.println("    provider [COMP_SPEC]: provider client-side state");
            printWriter.println("    s[ervices] [COMP_SPEC ...]: service state");
            printWriter.println("    as[sociations]: tracked app associations");
            printWriter.println("    settings: currently applied config settings");
            printWriter.println("    service [COMP_SPEC]: service client-side state");
            printWriter.println("    package [PACKAGE_NAME]: all state related to given package");
            printWriter.println("    all: dump all activities");
            printWriter.println("    top: dump the top activity");
            printWriter.println("  WHAT may also be a COMP_SPEC to dump activities.");
            printWriter.println("  COMP_SPEC may be a component name (com.foo/.myApp),");
            printWriter.println("    a partial substring in a component name, a");
            printWriter.println("    hex object identifier.");
            printWriter.println("  -a: include all available server state.");
            printWriter.println("  -c: include client state.");
            printWriter.println("  -p: limit output to given package.");
            printWriter.println("  --checkin: output checkin format, resetting data.");
            printWriter.println("  --C: output checkin format, not resetting data.");
            printWriter.println("  --proto: output dump in protocol buffer format.");
            return;
        }
        printWriter.println("Activity manager (activity) commands:");
        printWriter.println("  help");
        printWriter.println("      Print this help text.");
        printWriter.println("  start-activity [-D] [-N] [-W] [-P <FILE>] [--start-profiler <FILE>]");
        printWriter.println("          [--sampling INTERVAL] [--streaming] [-R COUNT] [-S]");
        printWriter.println("          [--track-allocation] [--user <USER_ID> | current] <INTENT>");
        printWriter.println("      Start an Activity.  Options are:");
        printWriter.println("      -D: enable debugging");
        printWriter.println("      -N: enable native debugging");
        printWriter.println("      -W: wait for launch to complete");
        printWriter.println("      --start-profiler <FILE>: start profiler and send results to <FILE>");
        printWriter.println("      --sampling INTERVAL: use sample profiling with INTERVAL microseconds");
        printWriter.println("          between samples (use with --start-profiler)");
        printWriter.println("      --streaming: stream the profiling output to the specified file");
        printWriter.println("          (use with --start-profiler)");
        printWriter.println("      -P <FILE>: like above, but profiling stops when app goes idle");
        printWriter.println("      --attach-agent <agent>: attach the given agent before binding");
        printWriter.println("      --attach-agent-bind <agent>: attach the given agent during binding");
        printWriter.println("      -R: repeat the activity launch <COUNT> times.  Prior to each repeat,");
        printWriter.println("          the top activity will be finished.");
        printWriter.println("      -S: force stop the target app before starting the activity");
        printWriter.println("      --track-allocation: enable tracking of object allocations");
        printWriter.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        printWriter.println("          specified then run as the current user.");
        printWriter.println("      --windowingMode <WINDOWING_MODE>: The windowing mode to launch the activity into.");
        printWriter.println("      --activityType <ACTIVITY_TYPE>: The activity type to launch the activity as.");
        printWriter.println("  start-service [--user <USER_ID> | current] <INTENT>");
        printWriter.println("      Start a Service.  Options are:");
        printWriter.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        printWriter.println("          specified then run as the current user.");
        printWriter.println("  start-foreground-service [--user <USER_ID> | current] <INTENT>");
        printWriter.println("      Start a foreground Service.  Options are:");
        printWriter.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        printWriter.println("          specified then run as the current user.");
        printWriter.println("  stop-service [--user <USER_ID> | current] <INTENT>");
        printWriter.println("      Stop a Service.  Options are:");
        printWriter.println("      --user <USER_ID> | current: Specify which user to run as; if not");
        printWriter.println("          specified then run as the current user.");
        printWriter.println("  broadcast [--user <USER_ID> | all | current] <INTENT>");
        printWriter.println("      Send a broadcast Intent.  Options are:");
        printWriter.println("      --user <USER_ID> | all | current: Specify which user to send to; if not");
        printWriter.println("          specified then send to all users.");
        printWriter.println("      --receiver-permission <PERMISSION>: Require receiver to hold permission.");
        printWriter.println("  instrument [-r] [-e <NAME> <VALUE>] [-p <FILE>] [-w]");
        printWriter.println("          [--user <USER_ID> | current] [--no-hidden-api-checks]");
        printWriter.println("          [--no-window-animation] [--abi <ABI>] <COMPONENT>");
        printWriter.println("      Start an Instrumentation.  Typically this target <COMPONENT> is in the");
        printWriter.println("      form <TEST_PACKAGE>/<RUNNER_CLASS> or only <TEST_PACKAGE> if there");
        printWriter.println("      is only one instrumentation.  Options are:");
        printWriter.println("      -r: print raw results (otherwise decode REPORT_KEY_STREAMRESULT).  Use with");
        printWriter.println("          [-e perf true] to generate raw output for performance measurements.");
        printWriter.println("      -e <NAME> <VALUE>: set argument <NAME> to <VALUE>.  For test runners a");
        printWriter.println("          common form is [-e <testrunner_flag> <value>[,<value>...]].");
        printWriter.println("      -p <FILE>: write profiling data to <FILE>");
        printWriter.println("      -m: Write output as protobuf to stdout (machine readable)");
        printWriter.println("      -f <Optional PATH/TO/FILE>: Write output as protobuf to a file (machine");
        printWriter.println("          readable). If path is not specified, default directory and file name will");
        printWriter.println("          be used: /sdcard/instrument-logs/log-yyyyMMdd-hhmmss-SSS.instrumentation_data_proto");
        printWriter.println("      -w: wait for instrumentation to finish before returning.  Required for");
        printWriter.println("          test runners.");
        printWriter.println("      --user <USER_ID> | current: Specify user instrumentation runs in;");
        printWriter.println("          current user if not specified.");
        printWriter.println("      --no-hidden-api-checks: disable restrictions on use of hidden API.");
        printWriter.println("      --no-window-animation: turn off window animations while running.");
        printWriter.println("      --abi <ABI>: Launch the instrumented process with the selected ABI.");
        printWriter.println("          This assumes that the process supports the selected ABI.");
        printWriter.println("  trace-ipc [start|stop] [--dump-file <FILE>]");
        printWriter.println("      Trace IPC transactions.");
        printWriter.println("      start: start tracing IPC transactions.");
        printWriter.println("      stop: stop tracing IPC transactions and dump the results to file.");
        printWriter.println("      --dump-file <FILE>: Specify the file the trace should be dumped to.");
        printWriter.println("  profile [start|stop] [--user <USER_ID> current] [--sampling INTERVAL]");
        printWriter.println("          [--streaming] <PROCESS> <FILE>");
        printWriter.println("      Start and stop profiler on a process.  The given <PROCESS> argument");
        printWriter.println("        may be either a process name or pid.  Options are:");
        printWriter.println("      --user <USER_ID> | current: When supplying a process name,");
        printWriter.println("          specify user of process to profile; uses current user if not specified.");
        printWriter.println("      --sampling INTERVAL: use sample profiling with INTERVAL microseconds");
        printWriter.println("          between samples");
        printWriter.println("      --streaming: stream the profiling output to the specified file");
        printWriter.println("  dumpheap [--user <USER_ID> current] [-n] [-g] <PROCESS> <FILE>");
        printWriter.println("      Dump the heap of a process.  The given <PROCESS> argument may");
        printWriter.println("        be either a process name or pid.  Options are:");
        printWriter.println("      -n: dump native heap instead of managed heap");
        printWriter.println("      -g: force GC before dumping the heap");
        printWriter.println("      --user <USER_ID> | current: When supplying a process name,");
        printWriter.println("          specify user of process to dump; uses current user if not specified.");
        printWriter.println("  set-debug-app [-w] [--persistent] <PACKAGE>");
        printWriter.println("      Set application <PACKAGE> to debug.  Options are:");
        printWriter.println("      -w: wait for debugger when application starts");
        printWriter.println("      --persistent: retain this value");
        printWriter.println("  clear-debug-app");
        printWriter.println("      Clear the previously set-debug-app.");
        printWriter.println("  set-watch-heap <PROCESS> <MEM-LIMIT>");
        printWriter.println("      Start monitoring pss size of <PROCESS>, if it is at or");
        printWriter.println("      above <HEAP-LIMIT> then a heap dump is collected for the user to report.");
        printWriter.println("  clear-watch-heap");
        printWriter.println("      Clear the previously set-watch-heap.");
        printWriter.println("  bug-report [--progress | --telephony]");
        printWriter.println("      Request bug report generation; will launch a notification");
        printWriter.println("        when done to select where it should be delivered. Options are:");
        printWriter.println("     --progress: will launch a notification right away to show its progress.");
        printWriter.println("     --telephony: will dump only telephony sections.");
        printWriter.println("  force-stop [--user <USER_ID> | all | current] <PACKAGE>");
        printWriter.println("      Completely stop the given application package.");
        printWriter.println("  crash [--user <USER_ID>] <PACKAGE|PID>");
        printWriter.println("      Induce a VM crash in the specified package or process");
        printWriter.println("  kill [--user <USER_ID> | all | current] <PACKAGE>");
        printWriter.println("      Kill all background processes associated with the given application.");
        printWriter.println("  kill-all");
        printWriter.println("      Kill all processes that are safe to kill (cached, etc).");
        printWriter.println("  make-uid-idle [--user <USER_ID> | all | current] <PACKAGE>");
        printWriter.println("      If the given application's uid is in the background and waiting to");
        printWriter.println("      become idle (not allowing background services), do that now.");
        printWriter.println("  monitor [--gdb <port>]");
        printWriter.println("      Start monitoring for crashes or ANRs.");
        printWriter.println("      --gdb: start gdbserv on the given port at crash/ANR");
        printWriter.println("  watch-uids [--oom <uid>]");
        printWriter.println("      Start watching for and reporting uid state changes.");
        printWriter.println("      --oom: specify a uid for which to report detailed change messages.");
        printWriter.println("  hang [--allow-restart]");
        printWriter.println("      Hang the system.");
        printWriter.println("      --allow-restart: allow watchdog to perform normal system restart");
        printWriter.println("  restart");
        printWriter.println("      Restart the user-space system.");
        printWriter.println("  idle-maintenance");
        printWriter.println("      Perform idle maintenance now.");
        printWriter.println("  screen-compat [on|off] <PACKAGE>");
        printWriter.println("      Control screen compatibility mode of <PACKAGE>.");
        printWriter.println("  package-importance <PACKAGE>");
        printWriter.println("      Print current importance of <PACKAGE>.");
        printWriter.println("  to-uri [INTENT]");
        printWriter.println("      Print the given Intent specification as a URI.");
        printWriter.println("  to-intent-uri [INTENT]");
        printWriter.println("      Print the given Intent specification as an intent: URI.");
        printWriter.println("  to-app-uri [INTENT]");
        printWriter.println("      Print the given Intent specification as an android-app: URI.");
        printWriter.println("  switch-user <USER_ID>");
        printWriter.println("      Switch to put USER_ID in the foreground, starting");
        printWriter.println("      execution of that user if it is currently stopped.");
        printWriter.println("  get-current-user");
        printWriter.println("      Returns id of the current foreground user.");
        printWriter.println("  start-user <USER_ID>");
        printWriter.println("      Start USER_ID in background if it is currently stopped;");
        printWriter.println("      use switch-user if you want to start the user in foreground");
        printWriter.println("  unlock-user <USER_ID> [TOKEN_HEX]");
        printWriter.println("      Attempt to unlock the given user using the given authorization token.");
        printWriter.println("  stop-user [-w] [-f] <USER_ID>");
        printWriter.println("      Stop execution of USER_ID, not allowing it to run any");
        printWriter.println("      code until a later explicit start or switch to it.");
        printWriter.println("      -w: wait for stop-user to complete.");
        printWriter.println("      -f: force stop even if there are related users that cannot be stopped.");
        printWriter.println("  is-user-stopped <USER_ID>");
        printWriter.println("      Returns whether <USER_ID> has been stopped or not.");
        printWriter.println("  get-started-user-state <USER_ID>");
        printWriter.println("      Gets the current state of the given started user.");
        printWriter.println("  track-associations");
        printWriter.println("      Enable association tracking.");
        printWriter.println("  untrack-associations");
        printWriter.println("      Disable and clear association tracking.");
        printWriter.println("  get-uid-state <UID>");
        printWriter.println("      Gets the process state of an app given its <UID>.");
        printWriter.println("  attach-agent <PROCESS> <FILE>");
        printWriter.println("    Attach an agent to the specified <PROCESS>, which may be either a process name or a PID.");
        printWriter.println("  get-config [--days N] [--device] [--proto]");
        printWriter.println("      Retrieve the configuration and any recent configurations of the device.");
        printWriter.println("      --days: also return last N days of configurations that have been seen.");
        printWriter.println("      --device: also output global device configuration info.");
        printWriter.println("      --proto: return result as a proto; does not include --days info.");
        printWriter.println("  supports-multiwindow");
        printWriter.println("      Returns true if the device supports multiwindow.");
        printWriter.println("  supports-split-screen-multi-window");
        printWriter.println("      Returns true if the device supports split screen multiwindow.");
        printWriter.println("  suppress-resize-config-changes <true|false>");
        printWriter.println("      Suppresses configuration changes due to user resizing an activity/task.");
        printWriter.println("  set-inactive [--user <USER_ID>] <PACKAGE> true|false");
        printWriter.println("      Sets the inactive state of an app.");
        printWriter.println("  get-inactive [--user <USER_ID>] <PACKAGE>");
        printWriter.println("      Returns the inactive state of an app.");
        printWriter.println("  set-standby-bucket [--user <USER_ID>] <PACKAGE> active|working_set|frequent|rare");
        printWriter.println("      Puts an app in the standby bucket.");
        printWriter.println("  get-standby-bucket [--user <USER_ID>] <PACKAGE>");
        printWriter.println("      Returns the standby bucket of an app.");
        printWriter.println("  send-trim-memory [--user <USER_ID>] <PROCESS>");
        printWriter.println("          [HIDDEN|RUNNING_MODERATE|BACKGROUND|RUNNING_LOW|MODERATE|RUNNING_CRITICAL|COMPLETE]");
        printWriter.println("      Send a memory trim event to a <PROCESS>.  May also supply a raw trim int level.");
        printWriter.println("  display [COMMAND] [...]: sub-commands for operating on displays.");
        printWriter.println("       move-stack <STACK_ID> <DISPLAY_ID>");
        printWriter.println("           Move <STACK_ID> from its current display to <DISPLAY_ID>.");
        printWriter.println("  stack [COMMAND] [...]: sub-commands for operating on activity stacks.");
        printWriter.println("       start <DISPLAY_ID> <INTENT>");
        printWriter.println("           Start a new activity on <DISPLAY_ID> using <INTENT>");
        printWriter.println("       move-task <TASK_ID> <STACK_ID> [true|false]");
        printWriter.println("           Move <TASK_ID> from its current stack to the top (true) or");
        printWriter.println("           bottom (false) of <STACK_ID>.");
        printWriter.println("       resize <STACK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        printWriter.println("           Change <STACK_ID> size and position to <LEFT,TOP,RIGHT,BOTTOM>.");
        printWriter.println("       resize-animated <STACK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        printWriter.println("           Same as resize, but allow animation.");
        printWriter.println("       resize-docked-stack <LEFT,TOP,RIGHT,BOTTOM> [<TASK_LEFT,TASK_TOP,TASK_RIGHT,TASK_BOTTOM>]");
        printWriter.println("           Change docked stack to <LEFT,TOP,RIGHT,BOTTOM>");
        printWriter.println("           and supplying temporary different task bounds indicated by");
        printWriter.println("           <TASK_LEFT,TOP,RIGHT,BOTTOM>");
        printWriter.println("       move-top-activity-to-pinned-stack: <STACK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        printWriter.println("           Moves the top activity from");
        printWriter.println("           <STACK_ID> to the pinned stack using <LEFT,TOP,RIGHT,BOTTOM> for the");
        printWriter.println("           bounds of the pinned stack.");
        printWriter.println("       positiontask <TASK_ID> <STACK_ID> <POSITION>");
        printWriter.println("           Place <TASK_ID> in <STACK_ID> at <POSITION>");
        printWriter.println("       list");
        printWriter.println("           List all of the activity stacks and their sizes.");
        printWriter.println("       info <WINDOWING_MODE> <ACTIVITY_TYPE>");
        printWriter.println("           Display the information about activity stack in <WINDOWING_MODE> and <ACTIVITY_TYPE>.");
        printWriter.println("       remove <STACK_ID>");
        printWriter.println("           Remove stack <STACK_ID>.");
        printWriter.println("  task [COMMAND] [...]: sub-commands for operating on activity tasks.");
        printWriter.println("       lock <TASK_ID>");
        printWriter.println("           Bring <TASK_ID> to the front and don't allow other tasks to run.");
        printWriter.println("       lock stop");
        printWriter.println("           End the current task lock.");
        printWriter.println("       resizeable <TASK_ID> [0|1|2|3]");
        printWriter.println("           Change resizeable mode of <TASK_ID> to one of the following:");
        printWriter.println("           0: unresizeable");
        printWriter.println("           1: crop_windows");
        printWriter.println("           2: resizeable");
        printWriter.println("           3: resizeable_and_pipable");
        printWriter.println("       resize <TASK_ID> <LEFT,TOP,RIGHT,BOTTOM>");
        printWriter.println("           Makes sure <TASK_ID> is in a stack with the specified bounds.");
        printWriter.println("           Forces the task to be resizeable and creates a stack if no existing stack");
        printWriter.println("           has the specified bounds.");
        printWriter.println("  update-appinfo <USER_ID> <PACKAGE_NAME> [<PACKAGE_NAME>...]");
        printWriter.println("      Update the ApplicationInfo objects of the listed packages for <USER_ID>");
        printWriter.println("      without restarting any processes.");
        printWriter.println("  write");
        printWriter.println("      Write all pending state to storage.");
        printWriter.println();
        Intent.printIntentArgsHelp(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }
}
