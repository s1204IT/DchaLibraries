package com.android.systemui.util.leak;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class LeakReporter {
    private final Context mContext;
    private final LeakDetector mLeakDetector;
    private final String mLeakReportEmail;

    public LeakReporter(Context context, LeakDetector leakDetector, String str) {
        this.mContext = context;
        this.mLeakDetector = leakDetector;
        this.mLeakReportEmail = str;
    }

    public void dumpLeak(int i) throws IOException {
        try {
            File file = new File(this.mContext.getCacheDir(), "leak");
            file.mkdir();
            File file2 = new File(file, "leak.hprof");
            Debug.dumpHprofData(file2.getAbsolutePath());
            File file3 = new File(file, "leak.dump");
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            try {
                PrintWriter printWriter = new PrintWriter(fileOutputStream);
                printWriter.print("Build: ");
                printWriter.println(SystemProperties.get("ro.build.description"));
                printWriter.println();
                printWriter.flush();
                this.mLeakDetector.dump(fileOutputStream.getFD(), printWriter, new String[0]);
                printWriter.close();
                fileOutputStream.close();
                NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
                NotificationChannel notificationChannel = new NotificationChannel("leak", "Leak Alerts", 4);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
                notificationManager.notify("LeakReporter", 0, new Notification.Builder(this.mContext, notificationChannel.getId()).setAutoCancel(true).setShowWhen(true).setContentTitle("Memory Leak Detected").setContentText(String.format("SystemUI has detected %d leaked objects. Tap to send", Integer.valueOf(i))).setSmallIcon(R.drawable.pointer_wait_49).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, getIntent(file2, file3), 201326592, null, UserHandle.CURRENT)).build());
            } finally {
            }
        } catch (IOException e) {
            Log.e("LeakReporter", "Couldn't dump heap for leak", e);
        }
    }

    private Intent getIntent(File file, File file2) throws XmlPullParserException, IOException {
        Uri uriForFile = FileProvider.getUriForFile(this.mContext, "com.android.systemui.fileprovider", file2);
        Uri uriForFile2 = FileProvider.getUriForFile(this.mContext, "com.android.systemui.fileprovider", file);
        Intent intent = new Intent("android.intent.action.SEND_MULTIPLE");
        intent.addFlags(1);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("application/vnd.android.leakreport");
        intent.putExtra("android.intent.extra.SUBJECT", "SystemUI leak report");
        intent.putExtra("android.intent.extra.TEXT", "Build info: " + SystemProperties.get("ro.build.description"));
        ClipData clipData = new ClipData(null, new String[]{"application/vnd.android.leakreport"}, new ClipData.Item(null, null, null, uriForFile));
        ArrayList<? extends Parcelable> arrayListNewArrayList = Lists.newArrayList(new Uri[]{uriForFile});
        clipData.addItem(new ClipData.Item(null, null, null, uriForFile2));
        arrayListNewArrayList.add(uriForFile2);
        intent.setClipData(clipData);
        intent.putParcelableArrayListExtra("android.intent.extra.STREAM", arrayListNewArrayList);
        String str = this.mLeakReportEmail;
        if (str != null) {
            intent.putExtra("android.intent.extra.EMAIL", new String[]{str});
        }
        return intent;
    }
}
