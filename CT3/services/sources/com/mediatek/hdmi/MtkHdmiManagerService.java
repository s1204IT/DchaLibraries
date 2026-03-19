package com.mediatek.hdmi;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioSystem;
import android.net.dhcp.DhcpPacket;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;
import com.android.server.audio.AudioService;
import com.android.server.power.ShutdownThread;
import com.mediatek.hdmi.IMtkHdmiManager;
import com.mediatek.hdmi.NvRAMAgent;
import com.mediatek.internal.R;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public final class MtkHdmiManagerService extends IMtkHdmiManager.Stub {
    private static final String ACTION_CLEARMOTION_DIMMED = "com.mediatek.clearmotion.DIMMED_UPDATE";
    private static final String ACTION_IPO_BOOT = "android.intent.action.ACTION_BOOT_IPO";
    private static final String ACTION_IPO_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final int AP_CFG_RDCL_FILE_HDCP_KEY_LID = 45;
    private static final int HDMI_COLOR_SPACE_DEFAULT = 0;
    private static final int HDMI_DEEP_COLOR_DEFAULT = 1;
    private static final int HDMI_ENABLE_STATUS_DEFAULT = 1;
    private static final int HDMI_VIDEO_RESOLUTION_DEFAULT = 100;
    private static final int HDMI_VIDEO_SCALE_DEFAULT = 0;
    private static final String KEY_CLEARMOTION_DIMMED = "sys.display.clearMotion.dimmed";
    private static final int MSG_CABLE_STATE = 2;
    private static final int MSG_DEINIT = 1;
    private static final int MSG_INIT = 0;
    private static final int MSG_USER_SWITCH = 3;
    private static final String TAG = "MtkHdmiService";
    private static String sHdmi = "HDMI";
    private static String sMhl = "MHL";
    private static String sSlimPort = "SLIMPORT";
    private AlertDialog mAudioOutputDialog;
    private boolean mCablePlugged;
    private int mCapabilities;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int[] mEdid;
    private HandlerThread mHandlerThread;
    private int mHdmiColorSpace;
    private int mHdmiDeepColor;
    private boolean mHdmiEnabled;
    private HdmiObserver mHdmiObserver;
    private int mHdmiVideoResolution;
    private int mHdmiVideoScale;
    private int[] mPreEdid;
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mInitialized = false;
    private boolean mIsSmartBookPluggedIn = false;
    private boolean mIsHdVideoPlaying = false;
    private boolean mHdVideoRestore = false;
    private boolean mCallComing = false;
    private boolean mCallRestore = false;
    private TelephonyManager mTelephonyManager = null;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, " Phone state changed, new state= " + state);
            MtkHdmiManagerService.this.handleCallStateChanged(state);
        }
    };
    private int mAudioOutputMode = 0;
    private BroadcastReceiver mActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "receive: " + action);
            if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) || "android.intent.action.ACTION_BOOT_IPO".equals(action)) {
                sendMsg(0);
                return;
            }
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                Log.d(MtkHdmiManagerService.TAG, "intent.getExtra_mode" + intent.getExtra("_mode"));
                if (intent.getExtra("_mode") == null) {
                    Log.d(MtkHdmiManagerService.TAG, "SHUTDOWN_REQUESTED=" + FeatureOption.SHUTDOWN_REQUESTED);
                    if (!FeatureOption.SHUTDOWN_REQUESTED) {
                        return;
                    }
                    sendMsg(1);
                    return;
                }
                sendMsg(1);
                return;
            }
            if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                sendMsg(1);
                return;
            }
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                sendMsg(3);
            } else {
                if (!"android.intent.action.SMARTBOOK_PLUG".equals(action)) {
                    return;
                }
                MtkHdmiManagerService.this.mIsSmartBookPluggedIn = intent.getBooleanExtra(AudioService.CONNECT_INTENT_KEY_STATE, false);
                Log.d(MtkHdmiManagerService.TAG, "smartbook plug:" + MtkHdmiManagerService.this.mIsSmartBookPluggedIn);
                MtkHdmiManagerService.this.handleNotification(false);
            }
        }

        private void sendMsg(int msgInit) {
            if (MtkHdmiManagerService.this.mHandler.hasMessages(msgInit)) {
                return;
            }
            MtkHdmiManagerService.this.mHandler.sendEmptyMessage(msgInit);
            MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "send msg: " + msgInit);
        }
    };
    private HdmiHandler mHandler;
    private ContentObserver mHdmiSettingsObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(MtkHdmiManagerService.TAG, "hdmiSettingsObserver onChanged: " + selfChange);
            MtkHdmiManagerService.this.mHdmiEnabled = Settings.System.getIntForUser(MtkHdmiManagerService.this.mContentResolver, "hdmi_enable_status", 1, -2) == 1;
            MtkHdmiManagerService.this.updateWakeLock(MtkHdmiManagerService.this.mCablePlugged, MtkHdmiManagerService.this.mHdmiEnabled);
        }
    };

    public native boolean nativeEnableAudio(boolean z);

    public native boolean nativeEnableCec(boolean z);

    public native boolean nativeEnableHdcp(boolean z);

    public native boolean nativeEnableHdmi(boolean z);

    public native boolean nativeEnableHdmiIpo(boolean z);

    public native boolean nativeEnableVideo(boolean z);

    public native int nativeGetCapabilities();

    public native char[] nativeGetCecAddr();

    public native int[] nativeGetCecCmd();

    public native int nativeGetDisplayType();

    public native int[] nativeGetEdid();

    public native boolean nativeHdmiPortraitEnable(boolean z);

    public native boolean nativeHdmiPowerEnable(boolean z);

    public native boolean nativeIsHdmiForceAwake();

    public native boolean nativeNeedSwDrmProtect();

    public native boolean nativeNotifyOtgState(int i);

    public native boolean nativeSetAudioConfig(int i);

    public native boolean nativeSetCecAddr(byte b, byte[] bArr, char c, char c2);

    public native boolean nativeSetCecCmd(byte b, byte b2, char c, byte[] bArr, int i, byte b3);

    public native boolean nativeSetDeepColor(int i, int i2);

    public native boolean nativeSetHdcpKey(byte[] bArr);

    public native boolean nativeSetHdmiDrmKey();

    public native boolean nativeSetVideoConfig(int i);

    private void handleCallStateChanged(int state) {
        log(TAG, "mCallComing: " + this.mCallComing + " mCallRestore: " + this.mCallRestore);
        if (state == 2) {
            this.mCallComing = true;
            if (!isSignalOutputting()) {
                return;
            }
            String contentStr = this.mContext.getResources().getString(R.string.hdmi_mutex_call_content);
            int type = getDisplayType();
            if (type == 2) {
                contentStr = contentStr.replaceAll(sHdmi, sMhl);
            } else if (type == 3) {
                contentStr = contentStr.replaceAll(sHdmi, sSlimPort);
            }
            Toast.makeText(this.mContext, contentStr, 1).show();
            this.mCallRestore = true;
            enableHdmi(false);
            return;
        }
        this.mCallComing = false;
        if (!this.mCallRestore) {
            return;
        }
        this.mCallRestore = false;
        enableHdmi(true);
    }

    private class HdmiHandler extends Handler {
        public HdmiHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "handleMessage: " + msg.what);
            if (MtkHdmiManagerService.this.mHandlerThread == null || !MtkHdmiManagerService.this.mHandlerThread.isAlive() || MtkHdmiManagerService.this.mHandlerThread.isInterrupted()) {
                MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "handler thread is error");
                return;
            }
            switch (msg.what) {
                case 0:
                    initHdmi(false);
                    if (isRealyBootComplete()) {
                        MtkHdmiManagerService.this.mInitialized = true;
                        MtkHdmiManagerService.this.hdmiCableStateChanged(MtkHdmiManagerService.this.mCablePlugged ? 1 : 0);
                    } else {
                        deinitHdmi();
                    }
                    break;
                case 1:
                    MtkHdmiManagerService.this.mInitialized = false;
                    deinitHdmi();
                    break;
                case 2:
                    int state = ((Integer) msg.obj).intValue();
                    MtkHdmiManagerService.this.hdmiCableStateChanged(state);
                    break;
                case 3:
                    deinitHdmi();
                    initHdmi(true);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

        private boolean isRealyBootComplete() {
            boolean bRet = false;
            String state = SystemProperties.get("ro.crypto.state");
            String decrypt = SystemProperties.get("vold.decrypt");
            String type = SystemProperties.get("ro.crypto.type");
            if ("unencrypted".equals(state)) {
                if ("".equals(decrypt)) {
                    bRet = true;
                }
            } else if ("unsupported".equals(state)) {
                if ("".equals(decrypt)) {
                    bRet = true;
                }
            } else if (!"".equals(state) && "encrypted".equals(state)) {
                if ("block".equals(type)) {
                    if ("trigger_restart_framework".equals(decrypt)) {
                        bRet = true;
                    }
                } else if ("file".equals(type)) {
                    bRet = true;
                }
            }
            MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "ro.crypto.state=" + state + " vold.decrypt=" + decrypt + " realBoot=" + bRet);
            return bRet;
        }

        private void deinitHdmi() {
            MtkHdmiManagerService.this.unregisterCallListener();
            MtkHdmiManagerService.this.enableHdmiImpl(false);
            if (!MtkHdmiManagerService.this.isSignalOutputting()) {
                return;
            }
            MtkHdmiManagerService.this.mCablePlugged = false;
            MtkHdmiManagerService.this.handleCablePlugged(false);
        }

        private void initHdmi(boolean bSwitchUser) {
            MtkHdmiManagerService.this.loadHdmiSettings();
            MtkHdmiManagerService.this.enableHdmiImpl(MtkHdmiManagerService.this.mHdmiEnabled);
            if (bSwitchUser && MtkHdmiManagerService.this.mInitialized) {
                MtkHdmiManagerService.this.handleCablePlugged(MtkHdmiManagerService.this.mCablePlugged);
                Settings.System.putIntForUser(MtkHdmiManagerService.this.mContentResolver, "hdmi_cable_plugged", MtkHdmiManagerService.this.mCablePlugged ? 1 : 0, -2);
            }
            MtkHdmiManagerService.this.registerCallListener();
        }
    }

    private void hdmiCableStateChanged(int state) {
        this.mCablePlugged = state == 1;
        if (!this.mInitialized) {
            return;
        }
        int type = getDisplayType();
        if (this.mIsHdVideoPlaying && this.mCablePlugged) {
            if (type != 1) {
                String contentStr = this.mContext.getResources().getString(R.string.hdmi_hdvideo_toast);
                if (type == 2) {
                    contentStr = contentStr.replaceAll(sHdmi, sMhl);
                } else if (type == 3) {
                    contentStr = contentStr.replaceAll(sHdmi, sSlimPort);
                }
                log(TAG, "disable hdmi when play HD video");
                Toast.makeText(this.mContext, contentStr, 1).show();
                this.mHdVideoRestore = true;
                log(TAG, "mIsHdVideoPlaying: " + this.mIsHdVideoPlaying + " mHdVideoRestore: " + this.mHdVideoRestore);
                enableHdmi(false);
                return;
            }
        } else if (this.mCallComing && this.mCablePlugged) {
            String contentStr2 = this.mContext.getResources().getString(R.string.hdmi_mutex_call_content);
            if (type == 2) {
                contentStr2 = contentStr2.replaceAll(sHdmi, sMhl);
            } else if (type == 3) {
                contentStr2 = contentStr2.replaceAll(sHdmi, sSlimPort);
            }
            log(TAG, "disable hdmi when call coming");
            Toast.makeText(this.mContext, contentStr2, 1).show();
            this.mCallRestore = true;
            log(TAG, "mCallComing: " + this.mCallComing + " mCallRestore: " + this.mCallRestore);
            enableHdmi(false);
            return;
        }
        getCapabilities();
        handleCablePlugged(this.mCablePlugged);
        Settings.System.putIntForUser(this.mContentResolver, "hdmi_cable_plugged", state, -2);
    }

    private void unregisterCallListener() {
        if (!hasCapability(4) || this.mTelephonyManager == null) {
            return;
        }
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
    }

    private void registerCallListener() {
        if (!hasCapability(4)) {
            return;
        }
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        log(TAG, "register phone state change listener...");
    }

    private void handleCablePlugged(boolean plugged) {
        boolean isShowNotification = false;
        updateClearMotionDimmed(plugged);
        if (plugged) {
            refreshEdid(plugged);
            if (FeatureOption.MTK_MT8193_HDMI_SUPPORT || FeatureOption.MTK_INTERNAL_HDMI_SUPPORT || FeatureOption.MTK_INTERNAL_MHL_SUPPORT) {
                setColorAndDeepImpl(this.mHdmiColorSpace, this.mHdmiDeepColor);
            }
            initVideoResolution(this.mHdmiVideoResolution, this.mHdmiVideoScale);
        } else {
            refreshEdid(plugged);
        }
        if (plugged && !this.mIsSmartBookPluggedIn) {
            isShowNotification = true;
        }
        handleNotification(isShowNotification);
        updateWakeLock(plugged, this.mHdmiEnabled);
        if (!plugged) {
            return;
        }
        handleMultiChannel();
    }

    private boolean isSupportMultiChannel() {
        return getAudioParameter(120, 3) > 2;
    }

    private void handleMultiChannel() {
        if (isSupportMultiChannel()) {
            this.mAudioOutputMode = Settings.System.getIntForUser(this.mContentResolver, "hdmi_audio_output_mode", 0, -2);
            log(TAG, "current mode from setting provider : " + this.mAudioOutputMode);
            if (this.mAudioOutputDialog == null) {
                String title = this.mContext.getResources().getString(R.string.hdmi_audio_output);
                String stereo = this.mContext.getResources().getString(R.string.hdmi_audio_output_stereo);
                String multiChannel = this.mContext.getResources().getString(R.string.hdmi_audio_output_multi_channel);
                this.mAudioOutputDialog = new AlertDialog.Builder(this.mContext).setTitle(title).setSingleChoiceItems(new String[]{stereo, multiChannel}, this.mAudioOutputMode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MtkHdmiManagerService.this.mAudioOutputMode = which;
                        MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "mAudioOutputDialog clicked.. which: " + which);
                        MtkHdmiManagerService.this.setAudioParameters(which == 0);
                        dialog.dismiss();
                        MtkHdmiManagerService.this.mAudioOutputDialog = null;
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MtkHdmiManagerService.this.setAudioParameters(MtkHdmiManagerService.this.mAudioOutputMode == 0);
                        dialog.dismiss();
                        MtkHdmiManagerService.this.mAudioOutputDialog = null;
                    }
                }).create();
                this.mAudioOutputDialog.setCancelable(false);
                Window win = this.mAudioOutputDialog.getWindow();
                win.setType(2003);
            }
            this.mAudioOutputDialog.show();
            return;
        }
        setAudioParameters(false);
    }

    private void setAudioParameters(boolean isStereoChecked) {
        int maxChannel = getAudioParameter(120, 3);
        if (isStereoChecked) {
            maxChannel = 2;
        }
        int maxSampleate = getAudioParameter(896, 7);
        int maxBitwidth = getAudioParameter(3072, 10);
        AudioSystem.setParameters("HDMI_channel=" + maxChannel);
        AudioSystem.setParameters("HDMI_maxsamplingrate=" + maxSampleate);
        AudioSystem.setParameters("HDMI_bitwidth=" + maxBitwidth);
        Settings.System.putIntForUser(this.mContentResolver, "hdmi_audio_output_mode", this.mAudioOutputMode, -2);
        log(TAG, "setAudioParameters mAudioOutputMode: " + this.mAudioOutputMode + " ,maxChannel: " + maxChannel + " ,maxSampleate: " + maxSampleate + " ,maxBitwidth: " + maxBitwidth);
    }

    public int getAudioParameter(int mask, int offsets) {
        int param = (this.mCapabilities & mask) >> offsets;
        log(TAG, "getAudioParameter() mask: " + mask + " ,offsets: " + offsets + " ,param: " + param + " ,mCapabilities: 0x" + Integer.toHexString(this.mCapabilities));
        return param;
    }

    private void updateClearMotionDimmed(boolean plugged) {
        if (!FeatureOption.MTK_CLEARMOTION_SUPPORT) {
            return;
        }
        SystemProperties.set(KEY_CLEARMOTION_DIMMED, plugged ? "1" : "0");
        this.mContext.sendBroadcastAsUser(new Intent("com.mediatek.clearmotion.DIMMED_UPDATE"), UserHandle.ALL);
    }

    private void refreshEdid(boolean plugged) {
        if (plugged) {
            this.mEdid = getResolutionMask();
            if (this.mEdid != null) {
                for (int i = 0; i < this.mEdid.length; i++) {
                    log(TAG, String.format("mEdid[%d] = %d", Integer.valueOf(i), Integer.valueOf(this.mEdid[i])));
                }
            } else {
                log(TAG, "mEdid is null!");
            }
            if (this.mPreEdid != null) {
                for (int i2 = 0; i2 < this.mPreEdid.length; i2++) {
                    log(TAG, String.format("mPreEdid[%d] = %d", Integer.valueOf(i2), Integer.valueOf(this.mPreEdid[i2])));
                }
                return;
            }
            log(TAG, "mPreEdid is null!");
            return;
        }
        this.mPreEdid = this.mEdid;
    }

    private void handleNotification(boolean showNoti) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager == null) {
            Log.w(TAG, "Fail to get NotificationManager");
            return;
        }
        if (showNoti) {
            log(TAG, "Show notification now");
            Notification notification = new Notification();
            String titleStr = this.mContext.getResources().getString(R.string.hdmi_notification_title);
            String contentStr = this.mContext.getResources().getString(R.string.hdmi_notification_content);
            notification.icon = R.drawable.ic_hdmi_notification;
            int type = getDisplayType();
            if (type == 2) {
                titleStr = titleStr.replaceAll(sHdmi, sMhl);
                contentStr = contentStr.replaceAll(sHdmi, sMhl);
                notification.icon = R.drawable.ic_mhl_notification;
            } else if (type == 3) {
                titleStr = titleStr.replaceAll(sHdmi, sSlimPort);
                contentStr = contentStr.replaceAll(sHdmi, sSlimPort);
                notification.icon = R.drawable.ic_sp_notification;
            }
            notification.tickerText = titleStr;
            notification.flags = 35;
            Intent intent = Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.HDMISettings"));
            PendingIntent pendingIntent = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            if (BenesseExtension.getDchaState() != 0) {
                pendingIntent = null;
            }
            notification.setLatestEventInfo(this.mContext, titleStr, contentStr, pendingIntent);
            notificationManager.notifyAsUser(null, R.drawable.ic_hdmi_notification, notification, UserHandle.CURRENT);
            return;
        }
        log(TAG, "Clear notification now");
        notificationManager.cancelAsUser(null, R.drawable.ic_hdmi_notification, UserHandle.CURRENT);
    }

    public MtkHdmiManagerService(Context context) {
        log(TAG, "MtkHdmiManagerService constructor");
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        initial();
    }

    private void initial() {
        if (this.mHandlerThread == null || !this.mHandlerThread.isAlive()) {
            this.mHandlerThread = new HandlerThread("HdmiService");
            this.mHandlerThread.start();
            this.mHandler = new HdmiHandler(this.mHandlerThread.getLooper());
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.ACTION_SHUTDOWN");
            filter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
            filter.addAction("android.intent.action.ACTION_BOOT_IPO");
            if (FeatureOption.MTK_SMARTBOOK_SUPPORT) {
                filter.addAction("android.intent.action.SMARTBOOK_PLUG");
            }
            this.mContext.registerReceiverAsUser(this.mActionReceiver, UserHandle.ALL, filter, null, this.mHandler);
        }
        if (this.mWakeLock == null) {
            PowerManager mPowerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mWakeLock = mPowerManager.newWakeLock(536870922, "HDMI");
            this.mWakeLock.setReferenceCounted(false);
        }
        if (this.mHdmiObserver == null) {
            this.mHdmiObserver = new HdmiObserver(this.mContext);
            this.mHdmiObserver.startObserve();
        }
        if (FeatureOption.MTK_MT8193_HDCP_SUPPORT || FeatureOption.MTK_HDMI_HDCP_SUPPORT) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (FeatureOption.MTK_DRM_KEY_MNG_SUPPORT) {
                        MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "setDrmKey: " + MtkHdmiManagerService.this.setDrmKey());
                    } else {
                        MtkHdmiManagerService.log(MtkHdmiManagerService.TAG, "setHdcpKey: " + MtkHdmiManagerService.this.setHdcpKey());
                    }
                }
            });
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                MtkHdmiManagerService.this.getCapabilities();
                String unused = MtkHdmiManagerService.sHdmi = MtkHdmiManagerService.this.mContext.getResources().getString(R.string.hdmi_replace_hdmi);
                String unused2 = MtkHdmiManagerService.sMhl = MtkHdmiManagerService.this.mContext.getResources().getString(R.string.hdmi_replace_mhl);
            }
        });
        observeSettings();
    }

    private void updateWakeLock(boolean plugged, boolean hdmiEnabled) {
        if (plugged && hdmiEnabled && nativeIsHdmiForceAwake()) {
            this.mWakeLock.acquire();
        } else {
            this.mWakeLock.release();
        }
    }

    private boolean setHdcpKey() {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
        if (agent != null) {
            try {
                log(TAG, "Read HDCP key from nvram");
                byte[] key = agent.readFile(AP_CFG_RDCL_FILE_HDCP_KEY_LID);
                for (int i = 0; i < 287; i++) {
                    log(TAG, String.format("HDCP key[%d] = %d", Integer.valueOf(i), Byte.valueOf(key[i])));
                }
                if (key != null) {
                    return nativeSetHdcpKey(key);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "NvRAMAgent read file fail");
            }
        }
        return false;
    }

    private boolean setDrmKey() {
        boolean zNativeSetHdmiDrmKey;
        synchronized (this) {
            zNativeSetHdmiDrmKey = nativeSetHdmiDrmKey();
        }
        return zNativeSetHdmiDrmKey;
    }

    private void loadHdmiSettings() {
        this.mHdmiEnabled = Settings.System.getIntForUser(this.mContentResolver, "hdmi_enable_status", 1, -2) == 1;
        this.mHdmiVideoResolution = Settings.System.getIntForUser(this.mContentResolver, "hdmi_video_resolution", 100, -2);
        this.mHdmiVideoScale = Settings.System.getIntForUser(this.mContentResolver, "hdmi_video_scale", 0, -2);
        this.mHdmiColorSpace = Settings.System.getIntForUser(this.mContentResolver, "hdmi_color_space", 0, -2);
        this.mHdmiDeepColor = Settings.System.getIntForUser(this.mContentResolver, "hdmi_deep_color", 1, -2);
        this.mIsHdVideoPlaying = false;
        this.mHdVideoRestore = false;
        this.mCallComing = false;
        this.mCallRestore = false;
    }

    private void observeSettings() {
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor("hdmi_enable_status"), false, this.mHdmiSettingsObserver, -1);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        pw.println("MTK HDMI MANAGER (dumpsys HDMI)");
        pw.println("HDMI mHdmiEnabled: " + this.mHdmiEnabled);
        pw.println("HDMI mHdmiVideoResolution: " + this.mHdmiVideoResolution);
        pw.println("HDMI mHdmiVideoScale: " + this.mHdmiVideoScale);
        pw.println("HDMI mHdmiColorSpace: " + this.mHdmiColorSpace);
        pw.println("HDMI mHdmiDeepColor: " + this.mHdmiDeepColor);
        pw.println("HDMI mCapabilities: " + this.mCapabilities);
        pw.println("HDMI mCablePlugged: " + this.mCablePlugged);
        pw.println("HDMI mEdid: " + Arrays.toString(this.mEdid));
        pw.println("HDMI mPreEdid: " + Arrays.toString(this.mPreEdid));
        pw.println("HDMI mInitialized: " + this.mInitialized);
        pw.println();
    }

    public boolean enableHdmi(boolean enabled) {
        log(TAG, "enableHdmi: " + enabled);
        boolean ret = false;
        if (enabled == this.mHdmiEnabled) {
            log(TAG, "mHdmiEnabled is the same: " + enabled);
        } else {
            ret = enableHdmiImpl(enabled);
            if (ret) {
                long ident = Binder.clearCallingIdentity();
                try {
                    this.mHdmiEnabled = enabled;
                    Settings.System.putIntForUser(this.mContentResolver, "hdmi_enable_status", this.mHdmiEnabled ? 1 : 0, -2);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
        return ret;
    }

    private boolean enableHdmiImpl(boolean enabled) {
        boolean zNativeEnableHdmi;
        synchronized (this) {
            zNativeEnableHdmi = nativeEnableHdmi(enabled);
        }
        return zNativeEnableHdmi;
    }

    public int[] getResolutionMask() {
        int[] iArrNativeGetEdid;
        log(TAG, "getResolutionMask");
        synchronized (this) {
            iArrNativeGetEdid = nativeGetEdid();
        }
        return iArrNativeGetEdid;
    }

    public boolean isSignalOutputting() {
        log(TAG, "isSignalOutputting");
        if (this.mCablePlugged) {
            return this.mHdmiEnabled;
        }
        return false;
    }

    public boolean setColorAndDeep(int color, int deep) {
        log(TAG, "setColorAndDeep: " + color + ", " + deep);
        boolean ret = setColorAndDeepImpl(color, deep);
        if (ret) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mHdmiColorSpace = color;
                this.mHdmiDeepColor = deep;
                Settings.System.putIntForUser(this.mContentResolver, "hdmi_color_space", color, -2);
                Settings.System.putIntForUser(this.mContentResolver, "hdmi_deep_color", deep, -2);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return ret;
    }

    private boolean setColorAndDeepImpl(int color, int deep) {
        boolean zNativeSetDeepColor;
        synchronized (this) {
            zNativeSetDeepColor = nativeSetDeepColor(color, deep);
        }
        return zNativeSetDeepColor;
    }

    public boolean setVideoResolution(int resolution) {
        log(TAG, "setVideoResolution: " + resolution);
        int suitableResolution = resolution;
        if (resolution >= 100) {
            suitableResolution = getSuitableResolution(resolution);
        }
        if (suitableResolution == this.mHdmiVideoResolution) {
            log(TAG, "setVideoResolution is the same");
        }
        int finalResolution = suitableResolution >= 100 ? suitableResolution - 100 : suitableResolution;
        log(TAG, "final video resolution: " + finalResolution + " scale: " + this.mHdmiVideoScale);
        boolean ret = setVideoResolutionImpl(finalResolution, this.mHdmiVideoScale);
        if (ret) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mHdmiVideoResolution = suitableResolution;
                Settings.System.putIntForUser(this.mContentResolver, "hdmi_video_resolution", this.mHdmiVideoResolution, -2);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return ret;
    }

    private void initVideoResolution(int resolution, int scale) {
        log(TAG, "initVideoResolution: " + resolution + " scale: " + scale);
        if (isResolutionSupported(resolution)) {
            setVideoResolutionImpl(resolution, scale);
            return;
        }
        int suitableResolution = getSuitableResolution(resolution);
        int finalResolution = suitableResolution >= 100 ? suitableResolution - 100 : suitableResolution;
        log(TAG, "initVideoResolution final video resolution: " + finalResolution);
        if (!setVideoResolutionImpl(finalResolution, scale)) {
            return;
        }
        this.mHdmiVideoResolution = suitableResolution;
        Settings.System.putIntForUser(this.mContentResolver, "hdmi_video_resolution", this.mHdmiVideoResolution, -2);
    }

    private boolean isResolutionSupported(int resolution) {
        log(TAG, "isResolutionSupported: " + resolution);
        if (resolution >= 100) {
            return false;
        }
        int[] supportedResolutions = getSupportedResolutions();
        for (int res : supportedResolutions) {
            if (res == resolution) {
                log(TAG, "resolution is supported");
                return true;
            }
        }
        return false;
    }

    private boolean setVideoResolutionImpl(int resolution, int scale) {
        boolean zNativeSetVideoConfig;
        int type = getDisplayType();
        if (type == 1) {
            log(TAG, "revise resolution for SMB to 2");
            resolution = 2;
        }
        int param = (resolution & DhcpPacket.MAX_OPTION_LEN) | ((scale & DhcpPacket.MAX_OPTION_LEN) << 8);
        log(TAG, "set video resolution&scale: 0x" + Integer.toHexString(param));
        synchronized (this) {
            zNativeSetVideoConfig = nativeSetVideoConfig(param);
        }
        return zNativeSetVideoConfig;
    }

    private int getSuitableResolution(int videoResolution) {
        int index;
        int i = 0;
        int[] supportedResolutions = getSupportedResolutions();
        ArrayList<Integer> resolutionList = new ArrayList<>();
        for (int i2 : supportedResolutions) {
            resolutionList.add(Integer.valueOf(i2));
        }
        if (needUpdate(videoResolution)) {
            log(TAG, "upate resolution");
            if (this.mEdid != null) {
                int edidTemp = this.mEdid[0] | this.mEdid[1];
                if (FeatureOption.MTK_INTERNAL_HDMI_SUPPORT || FeatureOption.MTK_INTERNAL_MHL_SUPPORT) {
                    index = 1;
                } else if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
                    index = 0;
                } else if (FeatureOption.MTK_TB6582_HDMI_SUPPORT) {
                    index = 2;
                } else {
                    index = 3;
                }
                int[] prefered = HdmiDef.getPreferedResolutions(index);
                int length = prefered.length;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    int res = prefered[i];
                    int act = res;
                    if (res >= 100) {
                        act = res - 100;
                    }
                    if ((HdmiDef.sResolutionMask[act] & edidTemp) == 0 || !resolutionList.contains(Integer.valueOf(act))) {
                        i++;
                    } else {
                        videoResolution = res;
                        break;
                    }
                }
            }
        }
        log(TAG, "suiteable video resolution: " + videoResolution);
        return videoResolution;
    }

    private boolean needUpdate(int videoResolution) {
        log(TAG, "needUpdate: " + videoResolution);
        boolean needUpdate = true;
        if (this.mPreEdid != null && Arrays.equals(this.mEdid, this.mPreEdid)) {
            needUpdate = false;
        }
        if (videoResolution >= 100) {
            return true;
        }
        return needUpdate;
    }

    public boolean setVideoScale(int scale) {
        log(TAG, "setVideoScale: " + scale);
        boolean ret = false;
        if (scale >= 0 && scale <= 10) {
            ret = true;
        }
        if (ret) {
            this.mHdmiVideoScale = scale;
            int finalResolution = this.mHdmiVideoResolution >= 100 ? this.mHdmiVideoResolution - 100 : this.mHdmiVideoResolution;
            log(TAG, "set video resolution: " + finalResolution + " scale: " + this.mHdmiVideoScale);
            ret = setVideoResolutionImpl(finalResolution, this.mHdmiVideoScale);
            if (ret) {
                long ident = Binder.clearCallingIdentity();
                try {
                    Settings.System.putIntForUser(this.mContentResolver, "hdmi_video_scale", this.mHdmiVideoScale, -2);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
        return ret;
    }

    public int[] getSupportedResolutions() {
        log(TAG, "getSupportedResolutions");
        return getSupportedResolutionsImpl();
    }

    private int[] getSupportedResolutionsImpl() {
        int[] resolutions;
        if (this.mEdid == null) {
            if (FeatureOption.MTK_TB6582_HDMI_SUPPORT) {
                return HdmiDef.getDefaultResolutions(2);
            }
            return HdmiDef.getDefaultResolutions(3);
        }
        if (FeatureOption.MTK_INTERNAL_HDMI_SUPPORT || FeatureOption.MTK_INTERNAL_MHL_SUPPORT) {
            if (FeatureOption.MTK_HDMI_4K_SUPPORT) {
                return HdmiDef.getDefaultResolutions(1);
            }
            return HdmiDef.getDefaultResolutions(4);
        }
        if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            resolutions = HdmiDef.getDefaultResolutions(0);
        } else {
            resolutions = HdmiDef.getAllResolutions();
        }
        int edidTemp = this.mEdid[0] | this.mEdid[1];
        ArrayList<Integer> list = new ArrayList<>();
        for (int res : resolutions) {
            try {
                int mask = HdmiDef.sResolutionMask[res];
                if ((edidTemp & mask) != 0 && !list.contains(Integer.valueOf(res))) {
                    list.add(Integer.valueOf(res));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.w(TAG, e.getMessage());
            }
        }
        int[] resolutions2 = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            resolutions2[i] = list.get(i).intValue();
        }
        log(TAG, "getSupportedResolutionsImpl: " + Arrays.toString(resolutions2));
        return resolutions2;
    }

    public int getDisplayType() {
        int ret;
        log(TAG, "getDisplayType");
        synchronized (this) {
            ret = nativeGetDisplayType();
        }
        return ret;
    }

    public void notifyHdVideoState(boolean playing) {
        log(TAG, "notifyHdVideoState: " + playing);
        synchronized (this) {
            if (this.mIsHdVideoPlaying == playing) {
                return;
            }
            log(TAG, "mIsHdVideoPlaying: " + this.mIsHdVideoPlaying + " mNeedRestore: " + this.mHdVideoRestore);
            this.mIsHdVideoPlaying = playing;
            if (!this.mIsHdVideoPlaying && this.mHdVideoRestore) {
                this.mHdVideoRestore = false;
                enableHdmi(true);
            }
        }
    }

    public boolean enableHdmiPower(boolean enabled) {
        boolean ret;
        log(TAG, "enableHdmiPower");
        synchronized (this) {
            ret = nativeHdmiPowerEnable(enabled);
        }
        return ret;
    }

    public boolean needSwDrmProtect() {
        boolean ret;
        log(TAG, "needSwDrmProtect");
        synchronized (this) {
            ret = nativeNeedSwDrmProtect();
        }
        return ret;
    }

    public boolean hasCapability(int mask) {
        log(TAG, "hasCapability: " + mask);
        return (this.mCapabilities & mask) != 0;
    }

    private void getCapabilities() {
        synchronized (this) {
            this.mCapabilities = nativeGetCapabilities();
        }
        log(TAG, "getCapabilities: 0x" + Integer.toHexString(this.mCapabilities));
    }

    private static void log(String tag, Object obj) {
        if (!Log.isLoggable(tag, 4)) {
            return;
        }
        Log.i(tag, obj.toString());
    }

    private class HdmiObserver extends UEventObserver {
        private static final String HDMI_NAME_PATH = "/sys/class/switch/hdmi/name";
        private static final String HDMI_STATE_PATH = "/sys/class/switch/hdmi/state";
        private static final String HDMI_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/hdmi";
        private static final int MSG_HDMI = 10;
        private static final int MSG_OTG = 11;
        private static final String OTG_NAME_PATH = "/sys/class/switch/otg_state/name";
        private static final String OTG_STATE_PATH = "/sys/class/switch/otg_state/state";
        private static final String OTG_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/otg_state";
        private static final String TAG = "HdmiObserver";
        private final Context mContext;
        private final Handler mHandler;
        private String mHdmiName;
        private int mHdmiState;
        private String mOtgName;
        private int mPrevHdmiState;
        private final PowerManager.WakeLock mWakeLock;

        public HdmiObserver(Context context) {
            this.mHandler = new Handler(MtkHdmiManagerService.this.mHandler.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 10:
                            HdmiObserver.this.sendIntents(msg.arg1, msg.arg2, (String) msg.obj);
                            break;
                        case 11:
                            HdmiObserver.this.handleOtgStateChanged(msg.arg1);
                            break;
                        default:
                            super.handleMessage(msg);
                            break;
                    }
                    HdmiObserver.this.mWakeLock.release();
                }
            };
            this.mContext = context;
            PowerManager pm = (PowerManager) context.getSystemService("power");
            this.mWakeLock = pm.newWakeLock(1, TAG);
            this.mWakeLock.setReferenceCounted(false);
            init();
        }

        public void startObserve() {
            startObserving(HDMI_UEVENT_MATCH);
            startObserving(OTG_UEVENT_MATCH);
        }

        public void stopObserve() {
            stopObserving();
        }

        public void onUEvent(UEventObserver.UEvent event) {
            MtkHdmiManagerService.log(TAG, "HdmiObserver: onUEvent: " + event.toString());
            String name = event.get("SWITCH_NAME");
            int state = 0;
            try {
                state = Integer.parseInt(event.get("SWITCH_STATE"));
            } catch (NumberFormatException e) {
                Log.w(TAG, "HdmiObserver: Could not parse switch state from event " + event);
            }
            MtkHdmiManagerService.log(TAG, "HdmiObserver.onUEvent(), name=" + name + ", state=" + state);
            if (name.equals(this.mOtgName)) {
                updateOtgState(state);
            } else {
                update(name, state);
            }
        }

        private synchronized void init() {
            String str = this.mHdmiName;
            int i = this.mHdmiState;
            this.mPrevHdmiState = this.mHdmiState;
            String newName = getContentFromFile(HDMI_NAME_PATH);
            try {
                int newState = Integer.parseInt(getContentFromFile(HDMI_STATE_PATH));
                update(newName, newState);
                initOtgState();
            } catch (NumberFormatException e) {
                Log.w(TAG, "HDMI state fail");
            }
        }

        private String getContentFromFile(String filePath) throws Throwable {
            FileReader reader;
            char[] buffer = new char[1024];
            FileReader reader2 = null;
            String content = null;
            try {
                try {
                    reader = new FileReader(filePath);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
            } catch (IndexOutOfBoundsException e3) {
                e = e3;
            }
            try {
                int len = reader.read(buffer, 0, buffer.length);
                content = String.valueOf(buffer, 0, len).trim();
                MtkHdmiManagerService.log(TAG, filePath + " content is " + content);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                        Log.w(TAG, "close reader fail: " + e4.getMessage());
                    }
                }
                reader2 = reader;
            } catch (FileNotFoundException e5) {
                reader2 = reader;
                Log.w(TAG, "can't find file " + filePath);
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e6) {
                        Log.w(TAG, "close reader fail: " + e6.getMessage());
                    }
                }
            } catch (IOException e7) {
                reader2 = reader;
                Log.w(TAG, "IO exception when read file " + filePath);
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e8) {
                        Log.w(TAG, "close reader fail: " + e8.getMessage());
                    }
                }
            } catch (IndexOutOfBoundsException e9) {
                e = e9;
                reader2 = reader;
                Log.w(TAG, "index exception: " + e.getMessage());
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e10) {
                        Log.w(TAG, "close reader fail: " + e10.getMessage());
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e11) {
                        Log.w(TAG, "close reader fail: " + e11.getMessage());
                    }
                }
                throw th;
            }
            return content;
        }

        private synchronized void update(String newName, int newState) {
            MtkHdmiManagerService.log(TAG, "HDMIOberver.update(), oldState=" + this.mHdmiState + ", newState=" + newState);
            int newOrOld = newState | this.mHdmiState;
            if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
                if (this.mHdmiState == newState && 3 != this.mHdmiState) {
                    return;
                }
            } else if (this.mHdmiState == newState || ((newOrOld - 1) & newOrOld) != 0) {
                return;
            }
            this.mHdmiName = newName;
            this.mPrevHdmiState = this.mHdmiState;
            this.mHdmiState = newState;
            this.mWakeLock.acquire();
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(10, this.mHdmiState, this.mPrevHdmiState, this.mHdmiName), 0L);
        }

        private synchronized void sendIntents(int hdmiState, int prevHdmiState, String hdmiName) {
            sendIntent(1, hdmiState, prevHdmiState, hdmiName);
        }

        private void sendIntent(int hdmi, int hdmiState, int prevHdmiState, String hdmiName) {
            if ((hdmiState & hdmi) == (prevHdmiState & hdmi)) {
                return;
            }
            Intent intent = new Intent("android.intent.action.HDMI_PLUG");
            intent.addFlags(1073741824);
            int state = 0;
            if ((hdmiState & hdmi) != 0) {
                state = 1;
            }
            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, state);
            intent.putExtra("name", hdmiName);
            MtkHdmiManagerService.log(TAG, "HdmiObserver: Broadcast HDMI event, state: " + state + " name: " + hdmiName);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            MtkHdmiManagerService.this.mHandler.obtainMessage(2, Integer.valueOf(state)).sendToTarget();
        }

        private void initOtgState() {
            this.mOtgName = getContentFromFile(OTG_NAME_PATH);
            try {
                int otgState = Integer.parseInt(getContentFromFile(OTG_STATE_PATH));
                Log.i(TAG, "HDMIObserver.initOtgState(), state=" + otgState + ", name=" + this.mOtgName);
                updateOtgState(otgState);
            } catch (NumberFormatException e) {
                Log.w(TAG, "OTG state fail");
            }
        }

        private void updateOtgState(int otgState) {
            Log.i(TAG, "HDMIObserver.updateOtgState(), otgState=" + otgState);
            this.mWakeLock.acquire();
            Message msg = this.mHandler.obtainMessage(11);
            msg.arg1 = otgState;
            this.mHandler.sendMessage(msg);
        }

        private void handleOtgStateChanged(int otgState) {
            Log.i(TAG, "HDMIObserver.handleOtgStateChanged(), otgState=" + otgState);
            boolean ret = MtkHdmiManagerService.this.nativeNotifyOtgState(otgState);
            Log.i(TAG, "notifyOtgState: " + ret);
        }
    }

    private static class FeatureOption {
        public static final boolean MTK_ENABLE_HDMI_MULTI_CHANNEL = true;
        public static final boolean MTK_DRM_KEY_MNG_SUPPORT = getValue("ro.mtk_key_manager_support");
        public static final boolean MTK_HDMI_HDCP_SUPPORT = getValue("ro.mtk_hdmi_hdcp_support");
        public static final boolean MTK_MT8193_HDCP_SUPPORT = getValue("ro.mtk_mt8193_hdcp_support");
        public static final boolean MTK_SMARTBOOK_SUPPORT = getValue("ro.mtk_smartbook_support");
        public static final boolean MTK_CLEARMOTION_SUPPORT = getValue("ro.mtk_clearmotion_support");
        public static final boolean MTK_INTERNAL_MHL_SUPPORT = getValue("ro.mtk_internal_mhl_support");
        public static final boolean MTK_INTERNAL_HDMI_SUPPORT = getValue("ro.mtk_internal_hdmi_support");
        public static final boolean MTK_MT8193_HDMI_SUPPORT = getValue("ro.mtk_mt8193_hdmi_support");
        public static final boolean MTK_TB6582_HDMI_SUPPORT = getValue("ro.hdmi.1080p60.disable");
        public static final boolean MTK_HDMI_4K_SUPPORT = getValue("ro.mtk_hdmi_4k_support");
        public static final boolean SHUTDOWN_REQUESTED = getValue(ShutdownThread.SHUTDOWN_ACTION_PROPERTY);

        private FeatureOption() {
        }

        private static boolean getValue(String key) {
            return SystemProperties.get(key).equals("1");
        }
    }
}
