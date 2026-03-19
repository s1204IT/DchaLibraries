package com.mediatek.hdmilocalservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.SystemService;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class HdmiLocalService extends SystemService {
    private static final boolean HDMI_TB_SUPPORT = !"".equals(SystemProperties.get("ro.vendor.mtk_tb_hdmi"));
    private final String TAG;
    private Context mContext;
    private HdmiObserver mHdmiObserver;

    public HdmiLocalService(Context context) {
        super(context);
        this.TAG = "HdmiLocalService";
        this.mContext = context;
    }

    public void onStart() {
        Slog.d("HdmiLocalService", "Start HdmiLocalService");
    }

    public void onBootPhase(int i) {
        if (i == 1000) {
            Slog.d("HdmiLocalService", "Do something in this phase(1000)");
            if (HDMI_TB_SUPPORT && this.mHdmiObserver == null) {
                this.mHdmiObserver = new HdmiObserver(this.mContext);
                this.mHdmiObserver.startObserve();
            }
        }
    }

    private class HdmiObserver extends UEventObserver {
        private static final String HDMI_NAME_PATH = "/sys/class/switch/hdmi/name";
        private static final String HDMI_NOTIFICATION_CHANNEL_ID = "hdmi_notification_channel";
        private static final String HDMI_NOTIFICATION_NAME = "HDMI";
        private static final String HDMI_STATE_PATH = "/sys/class/switch/hdmi/state";
        private static final String HDMI_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/hdmi";
        private static final int MSG_HDMI_PLUG_IN = 10;
        private static final int MSG_HDMI_PLUG_OUT = 11;
        private static final String TAG = "HdmiLocalService.HdmiObserver";
        private final Context mCxt;
        private String mHdmiName;
        private int mHdmiState;
        private int mPrevHdmiState;
        private final PowerManager.WakeLock mWakeLock;

        public HdmiObserver(Context context) {
            this.mCxt = context;
            this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(26, "HdmiObserver");
            this.mWakeLock.setReferenceCounted(false);
            init();
        }

        public void startObserve() {
            startObserving(HDMI_UEVENT_MATCH);
        }

        public void stopObserve() {
            stopObserving();
        }

        public void onUEvent(UEventObserver.UEvent uEvent) {
            int i;
            Slog.d(TAG, "HdmiObserver: onUEvent: " + uEvent.toString());
            String str = uEvent.get("SWITCH_NAME");
            try {
                i = Integer.parseInt(uEvent.get("SWITCH_STATE"));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "HdmiObserver: Could not parse switch state from event " + uEvent);
                i = 0;
            }
            Slog.d(TAG, "HdmiObserver.onUEvent(), name=" + str + ", state=" + i);
            update(str, i);
        }

        private void init() {
            String str = this.mHdmiName;
            int i = this.mHdmiState;
            this.mPrevHdmiState = this.mHdmiState;
            try {
                update(getContentFromFile(HDMI_NAME_PATH), Integer.parseInt(getContentFromFile(HDMI_STATE_PATH)));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "HDMI state fail");
            }
        }

        private String getContentFromFile(String str) throws Throwable {
            String strTrim;
            String str2;
            StringBuilder sb;
            char[] cArr = new char[1024];
            ?? r1 = 0;
            FileReader fileReader = null;
            FileReader fileReader2 = null;
            FileReader fileReader3 = null;
            try {
                try {
                    FileReader fileReader4 = new FileReader(str);
                    try {
                        try {
                            strTrim = String.valueOf(cArr, 0, fileReader4.read(cArr, 0, cArr.length)).trim();
                            try {
                                String str3 = TAG;
                                Slog.d(TAG, str + " content is " + strTrim);
                                try {
                                    fileReader4.close();
                                    r1 = str3;
                                } catch (IOException e) {
                                    e = e;
                                    str2 = TAG;
                                    sb = new StringBuilder();
                                    sb.append("close reader fail: ");
                                    sb.append(e.getMessage());
                                    Slog.w(str2, sb.toString());
                                }
                            } catch (FileNotFoundException e2) {
                                fileReader = fileReader4;
                                Slog.w(TAG, "can't find file " + str);
                                r1 = fileReader;
                                if (fileReader != null) {
                                    try {
                                        fileReader.close();
                                        r1 = fileReader;
                                    } catch (IOException e3) {
                                        e = e3;
                                        str2 = TAG;
                                        sb = new StringBuilder();
                                        sb.append("close reader fail: ");
                                        sb.append(e.getMessage());
                                        Slog.w(str2, sb.toString());
                                    }
                                }
                            } catch (IOException e4) {
                                fileReader2 = fileReader4;
                                Slog.w(TAG, "IO exception when read file " + str);
                                r1 = fileReader2;
                                if (fileReader2 != null) {
                                    try {
                                        fileReader2.close();
                                        r1 = fileReader2;
                                    } catch (IOException e5) {
                                        e = e5;
                                        str2 = TAG;
                                        sb = new StringBuilder();
                                        sb.append("close reader fail: ");
                                        sb.append(e.getMessage());
                                        Slog.w(str2, sb.toString());
                                    }
                                }
                            } catch (IndexOutOfBoundsException e6) {
                                e = e6;
                                fileReader3 = fileReader4;
                                Slog.w(TAG, "index exception: " + e.getMessage());
                                r1 = fileReader3;
                                if (fileReader3 != null) {
                                    try {
                                        fileReader3.close();
                                        r1 = fileReader3;
                                    } catch (IOException e7) {
                                        e = e7;
                                        str2 = TAG;
                                        sb = new StringBuilder();
                                        sb.append("close reader fail: ");
                                        sb.append(e.getMessage());
                                        Slog.w(str2, sb.toString());
                                    }
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            r1 = fileReader4;
                            if (r1 != 0) {
                                try {
                                    r1.close();
                                } catch (IOException e8) {
                                    Slog.w(TAG, "close reader fail: " + e8.getMessage());
                                }
                            }
                            throw th;
                        }
                    } catch (FileNotFoundException e9) {
                        strTrim = null;
                    } catch (IOException e10) {
                        strTrim = null;
                    } catch (IndexOutOfBoundsException e11) {
                        e = e11;
                        strTrim = null;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (FileNotFoundException e12) {
                strTrim = null;
            } catch (IOException e13) {
                strTrim = null;
            } catch (IndexOutOfBoundsException e14) {
                e = e14;
                strTrim = null;
            }
            return strTrim;
        }

        private void update(String str, int i) {
            Slog.d(TAG, "HDMIOberver.update(), oldState=" + this.mHdmiState + ", newState=" + i);
            int i2 = this.mHdmiState;
            this.mHdmiName = str;
            this.mPrevHdmiState = this.mHdmiState;
            this.mHdmiState = i;
            if (this.mHdmiState == 0) {
                this.mWakeLock.release();
                handleNotification(false);
                Slog.d(TAG, "HDMIOberver.update(), release");
            } else {
                this.mWakeLock.acquire();
                handleNotification(true);
                Slog.d(TAG, "HDMIOberver.update(), acquire");
            }
        }

        private void handleNotification(boolean z) {
            NotificationManager notificationManager = (NotificationManager) this.mCxt.getSystemService("notification");
            if (notificationManager == null) {
                Slog.w(TAG, "Fail to get NotificationManager");
                return;
            }
            if (z) {
                Slog.d(TAG, "Show notification now");
                notificationManager.createNotificationChannel(new NotificationChannel(HDMI_NOTIFICATION_CHANNEL_ID, HDMI_NOTIFICATION_NAME, 2));
                Notification notificationBuild = new Notification.Builder(this.mCxt, HDMI_NOTIFICATION_CHANNEL_ID).build();
                String string = this.mCxt.getResources().getString(134545612);
                String string2 = this.mCxt.getResources().getString(134545613);
                notificationBuild.icon = 134348898;
                notificationBuild.tickerText = string;
                notificationBuild.flags = 35;
                PendingIntent activityAsUser = PendingIntent.getActivityAsUser(this.mCxt, 0, Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.HdmiSettings")), 0, null, UserHandle.CURRENT);
                if (BenesseExtension.getDchaState() != 0) {
                    activityAsUser = null;
                }
                notificationBuild.setLatestEventInfo(this.mCxt, string, string2, activityAsUser);
                notificationManager.notifyAsUser(null, 134348898, notificationBuild, UserHandle.CURRENT);
                return;
            }
            Slog.d(TAG, "Clear notification now");
            notificationManager.cancelAsUser(null, 134348898, UserHandle.CURRENT);
        }
    }
}
