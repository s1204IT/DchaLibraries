package com.android.shell;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IDumpstate;
import android.os.IDumpstateListener;
import android.os.IDumpstateToken;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.util.SparseArray;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.google.android.collect.Lists;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import libcore.io.Streams;

public class BugreportProgressService extends Service {
    private Context mContext;
    private boolean mIsWatch;
    private int mLastProgressPercent;
    private Handler mMainThreadHandler;
    private ScreenshotHandler mScreenshotHandler;
    private File mScreenshotsDir;
    private ServiceHandler mServiceHandler;
    private boolean mTakingScreenshot;
    private static final Bundle sNotificationBundle = new Bundle();
    private static final String SHORT_EXTRA_ORIGINAL_INTENT = "android.intent.extra.ORIGINAL_INTENT".substring("android.intent.extra.ORIGINAL_INTENT".lastIndexOf(46) + 1);
    private final Object mLock = new Object();
    private final SparseArray<DumpstateListener> mProcesses = new SparseArray<>();
    private final BugreportInfoDialog mInfoDialog = new BugreportInfoDialog();
    private int mForegroundId = -1;

    @Override
    public void onCreate() {
        this.mContext = getApplicationContext();
        this.mMainThreadHandler = new Handler(Looper.getMainLooper());
        this.mServiceHandler = new ServiceHandler("BugreportProgressServiceMainThread");
        this.mScreenshotHandler = new ScreenshotHandler("BugreportProgressServiceScreenshotThread");
        this.mScreenshotsDir = new File(getFilesDir(), "bugreports");
        if (!this.mScreenshotsDir.exists()) {
            Log.i("BugreportProgressService", "Creating directory " + this.mScreenshotsDir + " to store temporary screenshots");
            if (!this.mScreenshotsDir.mkdir()) {
                Log.w("BugreportProgressService", "Could not create directory " + this.mScreenshotsDir);
            }
        }
        this.mIsWatch = (this.mContext.getResources().getConfiguration().uiMode & 15) == 6;
        NotificationManager.from(this.mContext).createNotificationChannel(new NotificationChannel("bugreports", this.mContext.getString(R.string.bugreport_notification_channel), isTv(this) ? 3 : 2));
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.v("BugreportProgressService", "onStartCommand(): " + dumpIntent(intent));
        if (intent != null) {
            Message messageObtainMessage = this.mServiceHandler.obtainMessage();
            messageObtainMessage.what = 1;
            messageObtainMessage.obj = intent;
            this.mServiceHandler.sendMessage(messageObtainMessage);
            return 2;
        }
        return 2;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        this.mServiceHandler.getLooper().quit();
        this.mScreenshotHandler.getLooper().quit();
        super.onDestroy();
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        int size = this.mProcesses.size();
        if (size == 0) {
            printWriter.println("No monitored processes");
            return;
        }
        printWriter.print("Foreground id: ");
        printWriter.println(this.mForegroundId);
        printWriter.println("\n");
        printWriter.println("Monitored dumpstate processes");
        printWriter.println("-----------------------------");
        int i = 0;
        while (i < size) {
            printWriter.print("#");
            int i2 = i + 1;
            printWriter.println(i2);
            printWriter.println(this.mProcesses.valueAt(i).info);
            i = i2;
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(String str) {
            super(BugreportProgressService.newLooper(str));
        }

        @Override
        public void handleMessage(Message message) {
            Intent intent;
            String action;
            int intExtra;
            int intExtra2;
            int intExtra3;
            String stringExtra;
            if (message.what == 2) {
                BugreportProgressService.this.takeScreenshot(message.arg1, message.arg2);
            }
            if (message.what == 4) {
                BugreportProgressService.this.handleScreenshotResponse(message);
                return;
            }
            if (message.what != 1) {
                Log.e("BugreportProgressService", "Invalid message type: " + message.what);
                return;
            }
            if (!(message.obj instanceof Intent)) {
                Log.wtf("BugreportProgressService", "handleMessage(): invalid msg.obj type: " + message.obj);
                return;
            }
            Parcelable parcelableExtra = ((Intent) message.obj).getParcelableExtra("android.intent.extra.ORIGINAL_INTENT");
            StringBuilder sb = new StringBuilder();
            sb.append("handleMessage(): ");
            intent = (Intent) parcelableExtra;
            sb.append(BugreportProgressService.dumpIntent(intent));
            Log.v("BugreportProgressService", sb.toString());
            if (!(parcelableExtra instanceof Intent)) {
                intent = (Intent) message.obj;
            }
            action = intent.getAction();
            intExtra = intent.getIntExtra("android.intent.extra.PID", 0);
            intExtra2 = intent.getIntExtra("android.intent.extra.ID", 0);
            intExtra3 = intent.getIntExtra("android.intent.extra.MAX", -1);
            stringExtra = intent.getStringExtra("android.intent.extra.NAME");
            switch (action) {
                case "com.android.internal.intent.action.BUGREPORT_STARTED":
                    if (!BugreportProgressService.this.startProgress(stringExtra, intExtra2, intExtra, intExtra3)) {
                        BugreportProgressService.this.stopSelfWhenDone();
                        break;
                    }
                    break;
                case "com.android.internal.intent.action.BUGREPORT_FINISHED":
                    if (intExtra2 == 0) {
                        Log.w("BugreportProgressService", "Missing android.intent.extra.ID on intent " + intent);
                    }
                    BugreportProgressService.this.onBugreportFinished(intExtra2, intent);
                    break;
                case "android.intent.action.BUGREPORT_INFO_LAUNCH":
                    BugreportProgressService.this.launchBugreportInfoDialog(intExtra2);
                    break;
                case "android.intent.action.BUGREPORT_SCREENSHOT":
                    BugreportProgressService.this.takeScreenshot(intExtra2);
                    break;
                case "android.intent.action.BUGREPORT_SHARE":
                    BugreportProgressService.this.shareBugreport(intExtra2, (BugreportInfo) intent.getParcelableExtra("android.intent.extra.INFO"));
                    break;
                case "android.intent.action.BUGREPORT_CANCEL":
                    BugreportProgressService.this.cancel(intExtra2);
                    break;
                default:
                    Log.w("BugreportProgressService", "Unsupported intent: " + action);
                    break;
            }
        }
    }

    private final class ScreenshotHandler extends Handler {
        public ScreenshotHandler(String str) {
            super(BugreportProgressService.newLooper(str));
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 3) {
                BugreportProgressService.this.handleScreenshotRequest(message);
                return;
            }
            Log.e("BugreportProgressService", "Invalid message type: " + message.what);
        }
    }

    private BugreportInfo getInfo(int i) {
        DumpstateListener dumpstateListener = this.mProcesses.get(i);
        if (dumpstateListener == null) {
            Log.w("BugreportProgressService", "Not monitoring process with ID " + i);
            return null;
        }
        return dumpstateListener.info;
    }

    private boolean startProgress(String str, int i, int i2, int i3) {
        if (str == null) {
            Log.w("BugreportProgressService", "Missing android.intent.extra.NAME on start intent");
        }
        if (i == -1) {
            Log.e("BugreportProgressService", "Missing android.intent.extra.ID on start intent");
            return false;
        }
        if (i2 == -1) {
            Log.e("BugreportProgressService", "Missing android.intent.extra.PID on start intent");
            return false;
        }
        if (i3 <= 0) {
            Log.e("BugreportProgressService", "Invalid value for extra android.intent.extra.MAX: " + i3);
            return false;
        }
        BugreportInfo bugreportInfo = new BugreportInfo(this.mContext, i, i2, str, i3);
        if (this.mProcesses.indexOfKey(i) >= 0) {
            Log.w("BugreportProgressService", "ID " + i + " already watched");
            return true;
        }
        DumpstateListener dumpstateListener = new DumpstateListener(bugreportInfo);
        this.mProcesses.put(bugreportInfo.id, dumpstateListener);
        if (dumpstateListener.connect()) {
            updateProgress(bugreportInfo);
            return true;
        }
        Log.w("BugreportProgressService", "not updating progress because it could not connect to dumpstate");
        return false;
    }

    private void updateProgress(BugreportInfo bugreportInfo) {
        PendingIntent service;
        if (bugreportInfo.max <= 0 || bugreportInfo.progress < 0) {
            Log.e("BugreportProgressService", "Invalid progress values for " + bugreportInfo);
            return;
        }
        if (bugreportInfo.finished) {
            Log.w("BugreportProgressService", "Not sending progress notification because bugreport has finished already (" + bugreportInfo + ")");
            return;
        }
        NumberFormat percentInstance = NumberFormat.getPercentInstance();
        percentInstance.setMinimumFractionDigits(2);
        percentInstance.setMaximumFractionDigits(2);
        String str = percentInstance.format(((double) bugreportInfo.progress) / ((double) bugreportInfo.max));
        String string = this.mContext.getString(R.string.bugreport_in_progress_title, Integer.valueOf(bugreportInfo.id));
        if (this.mIsWatch) {
            percentInstance.setMinimumFractionDigits(0);
            percentInstance.setMaximumFractionDigits(0);
            string = string + "\n" + percentInstance.format(((double) bugreportInfo.progress) / ((double) bugreportInfo.max));
        }
        Notification.Builder ongoing = newBaseNotification(this.mContext).setContentTitle(string).setTicker(string).setContentText(bugreportInfo.name != null ? bugreportInfo.name : this.mContext.getString(R.string.bugreport_unnamed)).setProgress(bugreportInfo.max, bugreportInfo.progress, false).setOngoing(true);
        if (!this.mIsWatch) {
            Notification.Action actionBuild = new Notification.Action.Builder((Icon) null, this.mContext.getString(android.R.string.cancel), newCancelIntent(this.mContext, bugreportInfo)).build();
            Intent intent = new Intent(this.mContext, (Class<?>) BugreportProgressService.class);
            intent.setAction("android.intent.action.BUGREPORT_INFO_LAUNCH");
            intent.putExtra("android.intent.extra.ID", bugreportInfo.id);
            PendingIntent service2 = PendingIntent.getService(this.mContext, bugreportInfo.id, intent, 134217728);
            Notification.Action actionBuild2 = new Notification.Action.Builder((Icon) null, this.mContext.getString(R.string.bugreport_info_action), service2).build();
            Intent intent2 = new Intent(this.mContext, (Class<?>) BugreportProgressService.class);
            intent2.setAction("android.intent.action.BUGREPORT_SCREENSHOT");
            intent2.putExtra("android.intent.extra.ID", bugreportInfo.id);
            if (!this.mTakingScreenshot) {
                service = PendingIntent.getService(this.mContext, bugreportInfo.id, intent2, 134217728);
            } else {
                service = null;
            }
            ongoing.setContentIntent(service2).setActions(actionBuild2, new Notification.Action.Builder((Icon) null, this.mContext.getString(R.string.bugreport_screenshot_action), service).build(), actionBuild);
        }
        int i = (bugreportInfo.progress * 100) / bugreportInfo.max;
        if (bugreportInfo.progress == 0 || bugreportInfo.progress >= 100 || i / 10 != this.mLastProgressPercent / 10) {
            Log.d("BugreportProgressService", "Progress #" + bugreportInfo.id + ": " + str);
        }
        this.mLastProgressPercent = i;
        sendForegroundabledNotification(bugreportInfo.id, ongoing.build());
    }

    private void sendForegroundabledNotification(int i, Notification notification) {
        if (this.mForegroundId >= 0) {
            NotificationManager.from(this.mContext).notify(i, notification);
            return;
        }
        this.mForegroundId = i;
        Log.d("BugreportProgressService", "Start running as foreground service on id " + this.mForegroundId);
        startForeground(this.mForegroundId, notification);
    }

    private static PendingIntent newCancelIntent(Context context, BugreportInfo bugreportInfo) {
        Intent intent = new Intent("android.intent.action.BUGREPORT_CANCEL");
        intent.setClass(context, BugreportProgressService.class);
        intent.putExtra("android.intent.extra.ID", bugreportInfo.id);
        return PendingIntent.getService(context, bugreportInfo.id, intent, 134217728);
    }

    private void stopProgress(int i) {
        if (this.mProcesses.indexOfKey(i) < 0) {
            Log.w("BugreportProgressService", "ID not watched: " + i);
        } else {
            Log.d("BugreportProgressService", "Removing ID " + i);
            this.mProcesses.remove(i);
        }
        stopForegroundWhenDone(i);
        Log.d("BugreportProgressService", "stopProgress(" + i + "): cancel notification");
        NotificationManager.from(this.mContext).cancel(i);
        stopSelfWhenDone();
    }

    private void cancel(int i) {
        MetricsLogger.action(this, 296);
        Log.v("BugreportProgressService", "cancel: ID=" + i);
        this.mInfoDialog.cancel();
        BugreportInfo info = getInfo(i);
        if (info != null && !info.finished) {
            Log.i("BugreportProgressService", "Cancelling bugreport service (ID=" + i + ") on user's request");
            setSystemProperty("ctl.stop", "bugreport");
            deleteScreenshots(info);
        }
        stopProgress(i);
    }

    private void launchBugreportInfoDialog(int i) {
        MetricsLogger.action(this, 297);
        final BugreportInfo info = getInfo(i);
        if (info == null) {
            Log.w("BugreportProgressService", "launchBugreportInfoDialog(): canceling notification because id " + i + " was not found");
            NotificationManager.from(this.mContext).cancel(i);
            return;
        }
        collapseNotificationBar();
        try {
            IWindowManager.Stub.asInterface(ServiceManager.getService("window")).dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
        } catch (Exception e) {
        }
        this.mMainThreadHandler.post(new Runnable() {
            @Override
            public final void run() {
                BugreportProgressService bugreportProgressService = this.f$0;
                bugreportProgressService.mInfoDialog.initialize(bugreportProgressService.mContext, info);
            }
        });
    }

    private void takeScreenshot(int i) {
        MetricsLogger.action(this, 298);
        if (getInfo(i) == null) {
            Log.w("BugreportProgressService", "takeScreenshot(): canceling notification because id " + i + " was not found");
            NotificationManager.from(this.mContext).cancel(i);
            return;
        }
        setTakingScreenshot(true);
        collapseNotificationBar();
        String quantityString = this.mContext.getResources().getQuantityString(android.R.plurals.bugreport_countdown, 3, 3);
        Log.i("BugreportProgressService", quantityString);
        Toast.makeText(this.mContext, quantityString, 0).show();
        takeScreenshot(i, 3);
    }

    private void takeScreenshot(int i, int i2) {
        if (i2 > 0) {
            Log.d("BugreportProgressService", "Taking screenshot for " + i + " in " + i2 + " seconds");
            Message messageObtainMessage = this.mServiceHandler.obtainMessage();
            messageObtainMessage.what = 2;
            messageObtainMessage.arg1 = i;
            messageObtainMessage.arg2 = i2 + (-1);
            this.mServiceHandler.sendMessageDelayed(messageObtainMessage, 1000L);
            return;
        }
        BugreportInfo info = getInfo(i);
        if (info == null) {
            return;
        }
        Message.obtain(this.mScreenshotHandler, 3, i, -2, new File(this.mScreenshotsDir, info.getPathNextScreenshot()).getAbsolutePath()).sendToTarget();
    }

    private void setTakingScreenshot(boolean z) {
        synchronized (this) {
            this.mTakingScreenshot = z;
            for (int i = 0; i < this.mProcesses.size(); i++) {
                BugreportInfo bugreportInfo = this.mProcesses.valueAt(i).info;
                if (bugreportInfo.finished) {
                    Log.d("BugreportProgressService", "Not updating progress for " + bugreportInfo.id + " while taking screenshot because share notification was already sent");
                } else {
                    updateProgress(bugreportInfo);
                }
            }
        }
    }

    private void handleScreenshotRequest(Message message) {
        String str = (String) message.obj;
        boolean zTakeScreenshot = takeScreenshot(this.mContext, str);
        setTakingScreenshot(false);
        Message.obtain(this.mServiceHandler, 4, message.arg1, zTakeScreenshot ? 1 : 0, str).sendToTarget();
    }

    private void handleScreenshotResponse(Message message) {
        String string;
        boolean z = message.arg2 != 0;
        BugreportInfo info = getInfo(message.arg1);
        if (info == null) {
            return;
        }
        File file = new File((String) message.obj);
        if (z) {
            info.addScreenshot(file);
            if (info.finished) {
                Log.d("BugreportProgressService", "Screenshot finished after bugreport; updating share notification");
                info.renameScreenshots(this.mScreenshotsDir);
                sendBugreportNotification(info, this.mTakingScreenshot);
            }
            string = this.mContext.getString(R.string.bugreport_screenshot_taken);
        } else {
            string = this.mContext.getString(R.string.bugreport_screenshot_failed);
            Toast.makeText(this.mContext, string, 0).show();
        }
        Log.d("BugreportProgressService", string);
    }

    private void deleteScreenshots(BugreportInfo bugreportInfo) {
        for (File file : bugreportInfo.screenshotFiles) {
            Log.i("BugreportProgressService", "Deleting screenshot file " + file);
            file.delete();
        }
    }

    private void stopForegroundWhenDone(int i) {
        if (i != this.mForegroundId) {
            Log.d("BugreportProgressService", "stopForegroundWhenDone(" + i + "): ignoring since foreground id is " + this.mForegroundId);
            return;
        }
        Log.d("BugreportProgressService", "detaching foreground from id " + this.mForegroundId);
        stopForeground(2);
        this.mForegroundId = -1;
        int size = this.mProcesses.size();
        if (size > 0) {
            for (int i2 = 0; i2 < size; i2++) {
                BugreportInfo bugreportInfo = this.mProcesses.valueAt(i2).info;
                if (!bugreportInfo.finished) {
                    updateProgress(bugreportInfo);
                    return;
                }
            }
        }
    }

    private void stopSelfWhenDone() {
        if (this.mProcesses.size() > 0) {
            return;
        }
        Log.v("BugreportProgressService", "No more processes to handle, shutting down");
        stopSelf();
    }

    private void onBugreportFinished(int i, Intent intent) {
        File fileExtra = getFileExtra(intent, "android.intent.extra.BUGREPORT");
        if (fileExtra == null) {
            Log.wtf("BugreportProgressService", "Missing android.intent.extra.BUGREPORT on intent " + intent);
            return;
        }
        this.mInfoDialog.onBugreportFinished();
        BugreportInfo info = getInfo(i);
        if (info == null) {
            Log.v("BugreportProgressService", "Creating info for untracked ID " + i);
            info = new BugreportInfo(this.mContext, i);
            this.mProcesses.put(i, new DumpstateListener(info));
        }
        info.renameScreenshots(this.mScreenshotsDir);
        info.bugreportFile = fileExtra;
        int intExtra = intent.getIntExtra("android.intent.extra.MAX", -1);
        if (intExtra != -1) {
            MetricsLogger.histogram(this, "dumpstate_duration", intExtra);
            info.max = intExtra;
        }
        File fileExtra2 = getFileExtra(intent, "android.intent.extra.SCREENSHOT");
        if (fileExtra2 != null) {
            info.addScreenshot(fileExtra2);
        }
        String stringExtra = intent.getStringExtra("android.intent.extra.TITLE");
        if (!TextUtils.isEmpty(stringExtra)) {
            info.title = stringExtra;
            String stringExtra2 = intent.getStringExtra("android.intent.extra.DESCRIPTION");
            if (!TextUtils.isEmpty(stringExtra2)) {
                info.shareDescription = stringExtra2;
            }
            Log.d("BugreportProgressService", "Bugreport title is " + info.title + ", shareDescription is " + info.shareDescription);
        }
        info.finished = true;
        stopForegroundWhenDone(i);
        triggerLocalNotification(this.mContext, info);
    }

    private void triggerLocalNotification(Context context, BugreportInfo bugreportInfo) {
        if (!bugreportInfo.bugreportFile.exists() || !bugreportInfo.bugreportFile.canRead()) {
            Log.e("BugreportProgressService", "Could not read bugreport file " + bugreportInfo.bugreportFile);
            Toast.makeText(context, R.string.bugreport_unreadable_text, 1).show();
            stopProgress(bugreportInfo.id);
            return;
        }
        if (!bugreportInfo.bugreportFile.getName().toLowerCase().endsWith(".txt")) {
            sendBugreportNotification(bugreportInfo, this.mTakingScreenshot);
        } else {
            sendZippedBugreportNotification(bugreportInfo, this.mTakingScreenshot);
        }
    }

    private static Intent buildWarningIntent(Context context, Intent intent) {
        Intent intent2 = new Intent(context, (Class<?>) BugreportWarningActivity.class);
        intent2.putExtra("android.intent.extra.INTENT", intent);
        return intent2;
    }

    private static Intent buildSendIntent(Context context, BugreportInfo bugreportInfo) {
        int length;
        try {
            Uri uri = getUri(context, bugreportInfo.bugreportFile);
            Intent intent = new Intent("android.intent.action.SEND_MULTIPLE");
            intent.addFlags(1);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setType("application/vnd.android.bugreport");
            String lastPathSegment = !TextUtils.isEmpty(bugreportInfo.title) ? bugreportInfo.title : uri.getLastPathSegment();
            intent.putExtra("android.intent.extra.SUBJECT", lastPathSegment);
            StringBuilder sb = new StringBuilder("Build info: ");
            sb.append(SystemProperties.get("ro.build.description"));
            sb.append("\nSerial number: ");
            sb.append(SystemProperties.get("ro.serialno"));
            if (!TextUtils.isEmpty(bugreportInfo.description)) {
                sb.append("\nDescription: ");
                sb.append(bugreportInfo.description);
                length = bugreportInfo.description.length();
            } else {
                length = 0;
            }
            intent.putExtra("android.intent.extra.TEXT", sb.toString());
            ClipData clipData = new ClipData(null, new String[]{"application/vnd.android.bugreport"}, new ClipData.Item(null, null, null, uri));
            Log.d("BugreportProgressService", "share intent: bureportUri=" + uri);
            ArrayList<? extends Parcelable> arrayListNewArrayList = Lists.newArrayList(new Uri[]{uri});
            Iterator<File> it = bugreportInfo.screenshotFiles.iterator();
            while (it.hasNext()) {
                Uri uri2 = getUri(context, it.next());
                Log.d("BugreportProgressService", "share intent: screenshotUri=" + uri2);
                clipData.addItem(new ClipData.Item(null, null, null, uri2));
                arrayListNewArrayList.add(uri2);
            }
            intent.setClipData(clipData);
            intent.putParcelableArrayListExtra("android.intent.extra.STREAM", arrayListNewArrayList);
            Pair<UserHandle, Account> pairFindSendToAccount = findSendToAccount(context, SystemProperties.get("sendbug.preferred.domain"));
            if (pairFindSendToAccount != null) {
                intent.putExtra("android.intent.extra.EMAIL", new String[]{((Account) pairFindSendToAccount.second).name});
            }
            Log.d("BugreportProgressService", "share intent: EXTRA_SUBJECT=" + lastPathSegment + ", EXTRA_TEXT=" + sb.length() + " chars, description=" + length + " chars");
            return intent;
        } catch (IllegalArgumentException e) {
            Log.wtf("BugreportProgressService", "Could not get URI for " + bugreportInfo.bugreportFile, e);
            return null;
        }
    }

    private void shareBugreport(int i, BugreportInfo bugreportInfo) {
        MetricsLogger.action(this, 299);
        BugreportInfo info = getInfo(i);
        if (info == null) {
            Log.d("BugreportProgressService", "shareBugreport(): no info for ID " + i + " on managed processes (" + this.mProcesses + "), using info from intent instead (" + bugreportInfo + ")");
        } else {
            Log.v("BugreportProgressService", "shareBugReport(): id " + i + " info = " + info);
            bugreportInfo = info;
        }
        addDetailsToZipFile(bugreportInfo);
        Intent intentBuildSendIntent = buildSendIntent(this.mContext, bugreportInfo);
        if (intentBuildSendIntent == null) {
            Log.w("BugreportProgressService", "Stopping progres on ID " + i + " because share intent could not be built");
            stopProgress(i);
            return;
        }
        boolean z = true;
        if (BugreportPrefs.getWarningState(this.mContext, 0) != 2) {
            intentBuildSendIntent = buildWarningIntent(this.mContext, intentBuildSendIntent);
            z = false;
        }
        intentBuildSendIntent.addFlags(268435456);
        if (z) {
            sendShareIntent(this.mContext, intentBuildSendIntent);
        } else {
            this.mContext.startActivity(intentBuildSendIntent);
        }
        stopProgress(i);
    }

    static void sendShareIntent(Context context, Intent intent) {
        Intent intentCreateChooser = Intent.createChooser(intent, context.getResources().getText(R.string.bugreport_intent_chooser_title));
        intentCreateChooser.putExtra("com.android.internal.app.ChooserActivity.EXTRA_PRIVATE_RETAIN_IN_ON_STOP", true);
        intentCreateChooser.addFlags(268435456);
        context.startActivity(intentCreateChooser);
    }

    private void sendBugreportNotification(BugreportInfo bugreportInfo, boolean z) {
        String string;
        String string2;
        addDetailsToZipFile(bugreportInfo);
        Intent intent = new Intent("android.intent.action.BUGREPORT_SHARE");
        intent.setClass(this.mContext, BugreportProgressService.class);
        intent.setAction("android.intent.action.BUGREPORT_SHARE");
        intent.putExtra("android.intent.extra.ID", bugreportInfo.id);
        intent.putExtra("android.intent.extra.INFO", bugreportInfo);
        if (z) {
            string = this.mContext.getString(R.string.bugreport_finished_pending_screenshot_text);
        } else {
            string = this.mContext.getString(R.string.bugreport_finished_text);
        }
        if (!TextUtils.isEmpty(bugreportInfo.title)) {
            string2 = bugreportInfo.title;
            if (!TextUtils.isEmpty(bugreportInfo.shareDescription) && !z) {
                string = bugreportInfo.shareDescription;
            }
        } else {
            string2 = this.mContext.getString(R.string.bugreport_finished_title, Integer.valueOf(bugreportInfo.id));
        }
        Notification.Builder deleteIntent = newBaseNotification(this.mContext).setContentTitle(string2).setTicker(string2).setContentText(string).setContentIntent(PendingIntent.getService(this.mContext, bugreportInfo.id, intent, 134217728)).setDeleteIntent(newCancelIntent(this.mContext, bugreportInfo));
        if (!TextUtils.isEmpty(bugreportInfo.name)) {
            deleteIntent.setSubText(bugreportInfo.name);
        }
        Log.v("BugreportProgressService", "Sending 'Share' notification for ID " + bugreportInfo.id + ": " + string2);
        NotificationManager.from(this.mContext).notify(bugreportInfo.id, deleteIntent.build());
    }

    private void sendBugreportBeingUpdatedNotification(Context context, int i) {
        String string = context.getString(R.string.bugreport_updating_title);
        Notification.Builder contentText = newBaseNotification(context).setContentTitle(string).setTicker(string).setContentText(context.getString(R.string.bugreport_updating_wait));
        Log.v("BugreportProgressService", "Sending 'Updating zip' notification for ID " + i + ": " + string);
        sendForegroundabledNotification(i, contentText.build());
    }

    private static Notification.Builder newBaseNotification(Context context) {
        if (sNotificationBundle.isEmpty()) {
            sNotificationBundle.putString("android.substName", context.getString(android.R.string.PERSOSUBSTATE_RUIM_HRPD_SUCCESS));
        }
        return new Notification.Builder(context, "bugreports").addExtras(sNotificationBundle).setSmallIcon(isTv(context) ? R.drawable.ic_bug_report_black_24dp : android.R.drawable.pointer_hand_large_icon).setLocalOnly(true).setColor(context.getColor(android.R.color.car_colorPrimary)).extend(new Notification.TvExtender());
    }

    private void sendZippedBugreportNotification(final BugreportInfo bugreportInfo, final boolean z) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) throws Exception {
                BugreportProgressService.zipBugreport(bugreportInfo);
                BugreportProgressService.this.sendBugreportNotification(bugreportInfo, z);
                return null;
            }
        }.execute(new Void[0]);
    }

    private static void zipBugreport(BugreportInfo bugreportInfo) throws Exception {
        Throwable th;
        String absolutePath = bugreportInfo.bugreportFile.getAbsolutePath();
        String strReplace = absolutePath.replace(".txt", ".zip");
        Log.v("BugreportProgressService", "zipping " + absolutePath + " as " + strReplace);
        File file = new File(strReplace);
        try {
            FileInputStream fileInputStream = new FileInputStream(bugreportInfo.bugreportFile);
            try {
                ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                try {
                    addEntry(zipOutputStream, bugreportInfo.bugreportFile.getName(), fileInputStream);
                    if (bugreportInfo.bugreportFile.delete()) {
                        Log.v("BugreportProgressService", "deleted original bugreport (" + absolutePath + ")");
                    } else {
                        Log.e("BugreportProgressService", "could not delete original bugreport (" + absolutePath + ")");
                    }
                    bugreportInfo.bugreportFile = file;
                    $closeResource(null, zipOutputStream);
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    $closeResource(th, zipOutputStream);
                    throw th;
                }
            } finally {
                $closeResource(null, fileInputStream);
            }
        } catch (IOException e) {
            Log.e("BugreportProgressService", "exception zipping file " + strReplace, e);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private void addDetailsToZipFile(BugreportInfo bugreportInfo) {
        synchronized (this.mLock) {
            addDetailsToZipFileLocked(bugreportInfo);
        }
    }

    private void addDetailsToZipFileLocked(BugreportInfo bugreportInfo) {
        Throwable th;
        if (bugreportInfo.bugreportFile == null) {
            Log.wtf("BugreportProgressService", "addDetailsToZipFile(): no bugreportFile on " + bugreportInfo);
            return;
        }
        if (TextUtils.isEmpty(bugreportInfo.title) && TextUtils.isEmpty(bugreportInfo.description)) {
            Log.d("BugreportProgressService", "Not touching zip file since neither title nor description are set");
            return;
        }
        if (bugreportInfo.addedDetailsToZip || bugreportInfo.addingDetailsToZip) {
            Log.d("BugreportProgressService", "Already added details to zip file for " + bugreportInfo);
            return;
        }
        bugreportInfo.addingDetailsToZip = true;
        sendBugreportBeingUpdatedNotification(this.mContext, bugreportInfo.id);
        File file = new File(bugreportInfo.bugreportFile.getParentFile(), "tmp-" + bugreportInfo.bugreportFile.getName());
        Log.d("BugreportProgressService", "Writing temporary zip file (" + file + ") with title and/or description");
        try {
            try {
                ZipFile zipFile = new ZipFile(bugreportInfo.bugreportFile);
                try {
                    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
                    try {
                        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                        while (enumerationEntries.hasMoreElements()) {
                            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                            String name = zipEntryNextElement.getName();
                            if (zipEntryNextElement.isDirectory()) {
                                Log.w("BugreportProgressService", "skipping directory entry: " + name);
                            } else {
                                addEntry(zipOutputStream, name, zipEntryNextElement.getTime(), zipFile.getInputStream(zipEntryNextElement));
                            }
                        }
                        addEntry(zipOutputStream, "title.txt", bugreportInfo.title);
                        addEntry(zipOutputStream, "description.txt", bugreportInfo.description);
                        $closeResource(null, zipOutputStream);
                        bugreportInfo.addedDetailsToZip = true;
                        bugreportInfo.addingDetailsToZip = false;
                        stopForegroundWhenDone(bugreportInfo.id);
                        if (file.renameTo(bugreportInfo.bugreportFile)) {
                            return;
                        }
                        Log.e("BugreportProgressService", "Could not rename " + file + " to " + bugreportInfo.bugreportFile);
                    } catch (Throwable th2) {
                        th = th2;
                        th = null;
                        $closeResource(th, zipOutputStream);
                        throw th;
                    }
                } finally {
                    $closeResource(null, zipFile);
                }
            } catch (IOException e) {
                Log.e("BugreportProgressService", "exception zipping file " + file, e);
                Toast.makeText(this.mContext, R.string.bugreport_add_details_to_zip_failed, 1).show();
                bugreportInfo.addedDetailsToZip = true;
                bugreportInfo.addingDetailsToZip = false;
                stopForegroundWhenDone(bugreportInfo.id);
            }
        } catch (Throwable th3) {
            bugreportInfo.addedDetailsToZip = true;
            bugreportInfo.addingDetailsToZip = false;
            stopForegroundWhenDone(bugreportInfo.id);
            throw th3;
        }
    }

    private static void addEntry(ZipOutputStream zipOutputStream, String str, String str2) throws IOException {
        if (!TextUtils.isEmpty(str2)) {
            addEntry(zipOutputStream, str, new ByteArrayInputStream(str2.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static void addEntry(ZipOutputStream zipOutputStream, String str, InputStream inputStream) throws IOException {
        addEntry(zipOutputStream, str, System.currentTimeMillis(), inputStream);
    }

    private static void addEntry(ZipOutputStream zipOutputStream, String str, long j, InputStream inputStream) throws IOException {
        ZipEntry zipEntry = new ZipEntry(str);
        zipEntry.setTime(j);
        zipOutputStream.putNextEntry(zipEntry);
        Streams.copy(inputStream, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    @VisibleForTesting
    static Pair<UserHandle, Account> findSendToAccount(Context context, String str) {
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        AccountManager accountManager = (AccountManager) context.getSystemService(AccountManager.class);
        if (str != null && !str.startsWith("@")) {
            str = "@" + str;
        }
        Pair<UserHandle, Account> pair = null;
        for (UserHandle userHandle : userManager.getUserProfiles()) {
            try {
                for (Account account : accountManager.getAccountsAsUser(userHandle.getIdentifier())) {
                    if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                        Pair<UserHandle, Account> pairCreate = Pair.create(userHandle, account);
                        if (!TextUtils.isEmpty(str)) {
                            if (account.name.endsWith(str)) {
                                return pairCreate;
                            }
                            if (pair == null) {
                                pair = pairCreate;
                            }
                        } else {
                            return pairCreate;
                        }
                    }
                }
            } catch (RuntimeException e) {
                Log.e("BugreportProgressService", "Could not get accounts for preferred domain " + str + " for user " + userHandle, e);
            }
        }
        return pair;
    }

    static Uri getUri(Context context, File file) {
        if (file != null) {
            return FileProvider.getUriForFile(context, "com.android.shell", file);
        }
        return null;
    }

    static File getFileExtra(Intent intent, String str) {
        String stringExtra = intent.getStringExtra(str);
        if (stringExtra != null) {
            return new File(stringExtra);
        }
        return null;
    }

    static String dumpIntent(Intent intent) {
        if (intent == null) {
            return "NO INTENT";
        }
        String action = intent.getAction();
        if (action == null) {
            action = "no action";
        }
        StringBuilder sb = new StringBuilder(action);
        sb.append(" extras: ");
        addExtra(sb, intent, "android.intent.extra.ID");
        addExtra(sb, intent, "android.intent.extra.PID");
        addExtra(sb, intent, "android.intent.extra.MAX");
        addExtra(sb, intent, "android.intent.extra.NAME");
        addExtra(sb, intent, "android.intent.extra.DESCRIPTION");
        addExtra(sb, intent, "android.intent.extra.BUGREPORT");
        addExtra(sb, intent, "android.intent.extra.SCREENSHOT");
        addExtra(sb, intent, "android.intent.extra.INFO");
        addExtra(sb, intent, "android.intent.extra.TITLE");
        if (intent.hasExtra("android.intent.extra.ORIGINAL_INTENT")) {
            sb.append(SHORT_EXTRA_ORIGINAL_INTENT);
            sb.append(": ");
            sb.append(dumpIntent((Intent) intent.getParcelableExtra("android.intent.extra.ORIGINAL_INTENT")));
        } else {
            sb.append("no ");
            sb.append(SHORT_EXTRA_ORIGINAL_INTENT);
        }
        return sb.toString();
    }

    private static void addExtra(StringBuilder sb, Intent intent, String str) {
        String strSubstring = str.substring(str.lastIndexOf(46) + 1);
        if (intent.hasExtra(str)) {
            sb.append(strSubstring);
            sb.append('=');
            sb.append(intent.getExtra(str));
        } else {
            sb.append("no ");
            sb.append(strSubstring);
        }
        sb.append(", ");
    }

    private static boolean setSystemProperty(String str, String str2) {
        try {
            SystemProperties.set(str, str2);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e("BugreportProgressService", "Could not set property " + str + " to " + str2, e);
            return false;
        }
    }

    private boolean setBugreportNameProperty(int i, String str) {
        Log.d("BugreportProgressService", "Updating bugreport name to " + str);
        return setSystemProperty("dumpstate." + i + ".name", str);
    }

    private void updateBugreportInfo(int i, String str, String str2, String str3) {
        BugreportInfo info = getInfo(i);
        if (info == null) {
            return;
        }
        if (str2 != null && !str2.equals(info.title)) {
            Log.d("BugreportProgressService", "updating bugreport title: " + str2);
            MetricsLogger.action(this, 301);
        }
        info.title = str2;
        if (str3 != null && !str3.equals(info.description)) {
            Log.d("BugreportProgressService", "updating bugreport description: " + str3.length() + " chars");
            MetricsLogger.action(this, 302);
        }
        info.description = str3;
        if (str != null && !str.equals(info.name)) {
            Log.d("BugreportProgressService", "updating bugreport name: " + str);
            MetricsLogger.action(this, 300);
            info.name = str;
            updateProgress(info);
        }
    }

    private void collapseNotificationBar() {
        sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }

    private static Looper newLooper(String str) {
        HandlerThread handlerThread = new HandlerThread(str, 10);
        handlerThread.start();
        return handlerThread.getLooper();
    }

    private static boolean takeScreenshot(Context context, String str) {
        Bitmap bitmapTakeScreenshot = Screenshooter.takeScreenshot();
        try {
            if (bitmapTakeScreenshot == null) {
                return false;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(str);
            try {
                if (bitmapTakeScreenshot.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                    ((Vibrator) context.getSystemService("vibrator")).vibrate(150L);
                    return true;
                }
                Log.e("BugreportProgressService", "Failed to save screenshot on " + str);
                return false;
            } finally {
                $closeResource(null, fileOutputStream);
            }
        } catch (IOException e) {
            Log.e("BugreportProgressService", "Failed to save screenshot on " + str, e);
            return false;
        } finally {
            bitmapTakeScreenshot.recycle();
        }
    }

    private static boolean isTv(Context context) {
        return context.getPackageManager().hasSystemFeature("android.software.leanback");
    }

    @VisibleForTesting
    static boolean isValid(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || ((c >= '0' && c <= '9') || c == '_' || c == '-');
    }

    private final class BugreportInfoDialog {
        private AlertDialog mDialog;
        private int mId;
        private EditText mInfoDescription;
        private EditText mInfoName;
        private EditText mInfoTitle;
        private Button mOkButton;
        private int mPid;
        private String mSavedName;
        private String mTempName;

        private BugreportInfoDialog() {
        }

        void initialize(final Context context, BugreportInfo bugreportInfo) {
            String string = context.getString(R.string.bugreport_info_dialog_title, Integer.valueOf(bugreportInfo.id));
            if (this.mDialog == null) {
                View viewInflate = View.inflate(context, R.layout.dialog_bugreport_info, null);
                this.mInfoName = (EditText) viewInflate.findViewById(R.id.name);
                this.mInfoTitle = (EditText) viewInflate.findViewById(R.id.title);
                this.mInfoDescription = (EditText) viewInflate.findViewById(R.id.description);
                this.mInfoName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean z) {
                        if (!z) {
                            BugreportInfoDialog.this.sanitizeName();
                        }
                    }
                });
                this.mDialog = new AlertDialog.Builder(context).setView(viewInflate).setTitle(string).setCancelable(true).setPositiveButton(context.getString(R.string.save), (DialogInterface.OnClickListener) null).setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MetricsLogger.action(context, 304);
                        if (!BugreportInfoDialog.this.mTempName.equals(BugreportInfoDialog.this.mSavedName)) {
                            BugreportProgressService.this.setBugreportNameProperty(BugreportInfoDialog.this.mPid, BugreportInfoDialog.this.mSavedName);
                        }
                    }
                }).create();
                this.mDialog.getWindow().setAttributes(new WindowManager.LayoutParams(2008));
            } else {
                this.mDialog.setTitle(string);
                this.mInfoName.setText((CharSequence) null);
                this.mInfoName.setEnabled(true);
                this.mInfoTitle.setText((CharSequence) null);
                this.mInfoDescription.setText((CharSequence) null);
            }
            String str = bugreportInfo.name;
            this.mTempName = str;
            this.mSavedName = str;
            this.mId = bugreportInfo.id;
            this.mPid = bugreportInfo.pid;
            if (!TextUtils.isEmpty(bugreportInfo.name)) {
                this.mInfoName.setText(bugreportInfo.name);
            }
            if (!TextUtils.isEmpty(bugreportInfo.title)) {
                this.mInfoTitle.setText(bugreportInfo.title);
            }
            if (!TextUtils.isEmpty(bugreportInfo.description)) {
                this.mInfoDescription.setText(bugreportInfo.description);
            }
            this.mDialog.show();
            if (this.mOkButton == null) {
                this.mOkButton = this.mDialog.getButton(-1);
                this.mOkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MetricsLogger.action(context, 303);
                        BugreportInfoDialog.this.sanitizeName();
                        BugreportProgressService.this.updateBugreportInfo(BugreportInfoDialog.this.mId, BugreportInfoDialog.this.mInfoName.getText().toString(), BugreportInfoDialog.this.mInfoTitle.getText().toString(), BugreportInfoDialog.this.mInfoDescription.getText().toString());
                        BugreportInfoDialog.this.mDialog.dismiss();
                    }
                });
            }
        }

        private void sanitizeName() {
            String string = this.mInfoName.getText().toString();
            if (string.equals(this.mTempName)) {
                return;
            }
            StringBuilder sb = new StringBuilder(string.length());
            boolean z = false;
            for (int i = 0; i < string.length(); i++) {
                char cCharAt = string.charAt(i);
                if (BugreportProgressService.isValid(cCharAt)) {
                    sb.append(cCharAt);
                } else {
                    sb.append('_');
                    z = true;
                }
            }
            if (z) {
                Log.v("BugreportProgressService", "changed invalid name '" + string + "' to '" + ((Object) sb) + "'");
                string = sb.toString();
                this.mInfoName.setText(string);
            }
            this.mTempName = string;
            BugreportProgressService.this.setBugreportNameProperty(this.mPid, string);
        }

        void onBugreportFinished() {
            if (this.mInfoName != null) {
                this.mInfoName.setEnabled(false);
                this.mInfoName.setText(this.mSavedName);
            }
        }

        void cancel() {
            if (this.mDialog != null) {
                this.mDialog.cancel();
            }
        }
    }

    private static final class BugreportInfo implements Parcelable {
        public static final Parcelable.Creator<BugreportInfo> CREATOR = new Parcelable.Creator<BugreportInfo>() {
            @Override
            public BugreportInfo createFromParcel(Parcel parcel) {
                return new BugreportInfo(parcel);
            }

            @Override
            public BugreportInfo[] newArray(int i) {
                return new BugreportInfo[i];
            }
        };
        boolean addedDetailsToZip;
        boolean addingDetailsToZip;
        File bugreportFile;
        private final Context context;
        String description;
        boolean finished;
        String formattedLastUpdate;
        final int id;
        long lastUpdate;
        int max;
        String name;
        final int pid;
        int progress;
        int realMax;
        int realProgress;
        int screenshotCounter;
        List<File> screenshotFiles;
        String shareDescription;
        String title;

        BugreportInfo(Context context, int i, int i2, String str, int i3) {
            this.lastUpdate = System.currentTimeMillis();
            this.screenshotFiles = new ArrayList(1);
            this.context = context;
            this.id = i;
            this.pid = i2;
            this.name = str;
            this.realMax = i3;
            this.max = i3;
        }

        BugreportInfo(Context context, int i) {
            this(context, i, i, null, 0);
            this.finished = true;
        }

        String getPathNextScreenshot() {
            this.screenshotCounter++;
            return "screenshot-" + this.pid + "-" + this.screenshotCounter + ".png";
        }

        void addScreenshot(File file) {
            this.screenshotFiles.add(file);
        }

        void renameScreenshots(File file) {
            if (TextUtils.isEmpty(this.name)) {
                return;
            }
            ArrayList arrayList = new ArrayList(this.screenshotFiles.size());
            for (File file2 : this.screenshotFiles) {
                String name = file2.getName();
                String strReplaceFirst = name.replaceFirst(Integer.toString(this.pid), this.name);
                if (!strReplaceFirst.equals(name)) {
                    File file3 = new File(file, strReplaceFirst);
                    Log.d("BugreportProgressService", "Renaming screenshot file " + file2 + " to " + file3);
                    if (file2.renameTo(file3)) {
                        file2 = file3;
                    }
                } else {
                    Log.w("BugreportProgressService", "Name didn't change: " + name);
                }
                arrayList.add(file2);
            }
            this.screenshotFiles = arrayList;
        }

        String getFormattedLastUpdate() {
            if (this.context == null) {
                return this.formattedLastUpdate == null ? Long.toString(this.lastUpdate) : this.formattedLastUpdate;
            }
            return DateUtils.formatDateTime(this.context, this.lastUpdate, 17);
        }

        public String toString() {
            float f = (this.progress * 100.0f) / this.max;
            float f2 = (this.realProgress * 100.0f) / this.realMax;
            StringBuilder sb = new StringBuilder();
            sb.append("\tid: ");
            sb.append(this.id);
            sb.append(", pid: ");
            sb.append(this.pid);
            sb.append(", name: ");
            sb.append(this.name);
            sb.append(", finished: ");
            sb.append(this.finished);
            sb.append("\n\ttitle: ");
            sb.append(this.title);
            sb.append("\n\tdescription: ");
            if (this.description == null) {
                sb.append("null");
            } else {
                if (TextUtils.getTrimmedLength(this.description) == 0) {
                    sb.append("empty ");
                }
                sb.append("(");
                sb.append(this.description.length());
                sb.append(" chars)");
            }
            sb.append("\n\tfile: ");
            sb.append(this.bugreportFile);
            sb.append("\n\tscreenshots: ");
            sb.append(this.screenshotFiles);
            sb.append("\n\tprogress: ");
            sb.append(this.progress);
            sb.append("/");
            sb.append(this.max);
            sb.append(" (");
            sb.append(f);
            sb.append(")");
            sb.append("\n\treal progress: ");
            sb.append(this.realProgress);
            sb.append("/");
            sb.append(this.realMax);
            sb.append(" (");
            sb.append(f2);
            sb.append(")");
            sb.append("\n\tlast_update: ");
            sb.append(getFormattedLastUpdate());
            sb.append("\n\taddingDetailsToZip: ");
            sb.append(this.addingDetailsToZip);
            sb.append(" addedDetailsToZip: ");
            sb.append(this.addedDetailsToZip);
            sb.append("\n\tshareDescription: ");
            sb.append(this.shareDescription);
            return sb.toString();
        }

        protected BugreportInfo(Parcel parcel) {
            this.lastUpdate = System.currentTimeMillis();
            this.screenshotFiles = new ArrayList(1);
            this.context = null;
            this.id = parcel.readInt();
            this.pid = parcel.readInt();
            this.name = parcel.readString();
            this.title = parcel.readString();
            this.description = parcel.readString();
            this.max = parcel.readInt();
            this.progress = parcel.readInt();
            this.realMax = parcel.readInt();
            this.realProgress = parcel.readInt();
            this.lastUpdate = parcel.readLong();
            this.formattedLastUpdate = parcel.readString();
            this.bugreportFile = readFile(parcel);
            int i = parcel.readInt();
            for (int i2 = 1; i2 <= i; i2++) {
                this.screenshotFiles.add(readFile(parcel));
            }
            this.finished = parcel.readInt() == 1;
            this.screenshotCounter = parcel.readInt();
            this.shareDescription = parcel.readString();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.id);
            parcel.writeInt(this.pid);
            parcel.writeString(this.name);
            parcel.writeString(this.title);
            parcel.writeString(this.description);
            parcel.writeInt(this.max);
            parcel.writeInt(this.progress);
            parcel.writeInt(this.realMax);
            parcel.writeInt(this.realProgress);
            parcel.writeLong(this.lastUpdate);
            parcel.writeString(getFormattedLastUpdate());
            writeFile(parcel, this.bugreportFile);
            parcel.writeInt(this.screenshotFiles.size());
            Iterator<File> it = this.screenshotFiles.iterator();
            while (it.hasNext()) {
                writeFile(parcel, it.next());
            }
            parcel.writeInt(this.finished ? 1 : 0);
            parcel.writeInt(this.screenshotCounter);
            parcel.writeString(this.shareDescription);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        private void writeFile(Parcel parcel, File file) {
            parcel.writeString(file == null ? null : file.getPath());
        }

        private File readFile(Parcel parcel) {
            String string = parcel.readString();
            if (string == null) {
                return null;
            }
            return new File(string);
        }
    }

    private final class DumpstateListener extends IDumpstateListener.Stub implements IBinder.DeathRecipient {
        private final BugreportInfo info;
        private IDumpstateToken token;

        DumpstateListener(BugreportInfo bugreportInfo) {
            this.info = bugreportInfo;
        }

        boolean connect() {
            if (this.token != null) {
                Log.d("BugreportProgressService", "connect(): " + this.info.id + " already connected");
                return true;
            }
            IBinder service = ServiceManager.getService("dumpstate");
            if (service == null) {
                Log.d("BugreportProgressService", "dumpstate service not bound yet");
                return true;
            }
            try {
                this.token = IDumpstate.Stub.asInterface(service).setListener("Shell", this, false);
                if (this.token != null) {
                    this.token.asBinder().linkToDeath(this, 0);
                }
            } catch (Exception e) {
                Log.e("BugreportProgressService", "Could not set dumpstate listener: " + e);
            }
            return this.token != null;
        }

        @Override
        public void binderDied() {
            if (!this.info.finished) {
                Log.w("BugreportProgressService", "Dumpstate process died:\n" + this.info);
                BugreportProgressService.this.stopProgress(this.info.id);
            }
            this.token.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void onProgressUpdated(int i) throws RemoteException {
            this.info.realProgress = i;
            int i2 = 10000;
            int i3 = (this.info.progress * 10000) / this.info.max;
            int i4 = (this.info.realProgress * 10000) / this.info.realMax;
            int i5 = this.info.realMax;
            if (i4 > 9900) {
                i = 9900;
                i4 = 9900;
            } else {
                i2 = i5;
            }
            if (i4 > i3) {
                this.info.progress = i;
                this.info.max = i2;
                this.info.lastUpdate = System.currentTimeMillis();
                BugreportProgressService.this.updateProgress(this.info);
            }
        }

        @Override
        public void onMaxProgressUpdated(int i) throws RemoteException {
            Log.d("BugreportProgressService", "onMaxProgressUpdated: " + i);
            this.info.realMax = i;
        }

        @Override
        public void onSectionComplete(String str, int i, int i2, int i3) throws RemoteException {
        }
    }
}
