package com.android.systemui.volume;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioService;
import android.media.AudioSystem;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.systemui.DemoMode;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.Interaction;
import com.android.systemui.volume.ZenModePanel;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VolumePanel extends Handler implements DemoMode {
    private static AlertDialog sSafetyWarning;
    private final AccessibilityManager mAccessibilityManager;
    private final AudioManager mAudioManager;
    private Callback mCallback;
    protected final Context mContext;
    private int mDemoIcon;
    private final Dialog mDialog;
    private float mDisabledAlpha;
    private boolean mHasVibrator;
    private final IconPulser mIconPulser;
    private ComponentName mNotificationEffectsSuppressor;
    private final ViewGroup mPanel;
    private final boolean mPlayMasterStreamTones;
    private boolean mRingIsSilent;
    private final ViewGroup mSliderPanel;
    private SparseArray<StreamControl> mStreamControls;
    private ToneGenerator[] mToneGenerators;
    private Vibrator mVibrator;
    private final View mView;
    private boolean mVoiceCapable;
    private final ZenModeController mZenController;
    private boolean mZenModeAvailable;
    private ZenModePanel mZenPanel;
    private boolean mZenPanelExpanded;
    private static boolean LOGD = Log.isLoggable("VolumePanel", 3);
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private static final StreamResources[] STREAMS = {StreamResources.BluetoothSCOStream, StreamResources.RingerStream, StreamResources.VoiceStream, StreamResources.FMStream, StreamResources.MediaStream, StreamResources.NotificationStream, StreamResources.AlarmStream, StreamResources.MasterStream, StreamResources.RemoteStream};
    private static Object sSafetyWarningLock = new Object();
    private int mTimeoutDelay = 3000;
    private int mLastRingerMode = 2;
    private int mLastRingerProgress = 0;
    private int mActiveStreamType = -1;
    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Object tag = seekBar.getTag();
            if (fromUser && (tag instanceof StreamControl)) {
                StreamControl sc = (StreamControl) tag;
                VolumePanel.this.setStreamVolume(sc, progress, 17);
            }
            VolumePanel.this.resetTimeout();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
    private final ZenModeController.Callback mZenCallback = new ZenModeController.Callback() {
        @Override
        public void onZenAvailableChanged(boolean available) {
            VolumePanel.this.obtainMessage(13, available ? 1 : 0, 0).sendToTarget();
        }

        @Override
        public void onEffectsSupressorChanged() {
            VolumePanel.this.mNotificationEffectsSuppressor = VolumePanel.this.mZenController.getEffectsSuppressor();
            VolumePanel.this.sendEmptyMessage(15);
        }
    };
    private final MediaController.Callback mMediaControllerCb = new MediaController.Callback() {
        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            VolumePanel.this.onRemoteVolumeUpdateIfShown();
        }
    };
    private final String mTag = String.format("%s.%08x", "VolumePanel", Integer.valueOf(hashCode()));
    private final SecondaryIconTransition mSecondaryIconTransition = new SecondaryIconTransition();

    public interface Callback {
        void onInteraction();

        void onVisible(boolean z);

        void onZenSettings();
    }

    private enum StreamResources {
        BluetoothSCOStream(6, R.string.httpErrorRedirectLoop, com.android.systemui.R.drawable.ic_audio_bt, com.android.systemui.R.drawable.ic_audio_bt_mute, false),
        RingerStream(2, R.string.httpErrorTimeout, com.android.systemui.R.drawable.ic_ringer_audible, com.android.systemui.R.drawable.ic_ringer_mute, false),
        VoiceStream(0, R.string.httpErrorTooManyRequests, com.android.systemui.R.drawable.ic_audio_phone, com.android.systemui.R.drawable.ic_audio_phone, false),
        FMStream(10, R.string.httpErrorIO, com.android.systemui.R.drawable.ic_audio_vol, com.android.systemui.R.drawable.ic_audio_vol_mute, false),
        AlarmStream(4, R.string.httpErrorLookup, com.android.systemui.R.drawable.ic_audio_alarm, com.android.systemui.R.drawable.ic_audio_alarm_mute, false),
        MediaStream(3, R.string.httpErrorUnsupportedAuthScheme, com.android.systemui.R.drawable.ic_audio_vol, com.android.systemui.R.drawable.ic_audio_vol_mute, true),
        NotificationStream(5, R.string.icu_abbrev_wday_month_day_no_year, com.android.systemui.R.drawable.ic_ringer_audible, com.android.systemui.R.drawable.ic_ringer_mute, true),
        MasterStream(-100, R.string.httpErrorUnsupportedAuthScheme, com.android.systemui.R.drawable.ic_audio_vol, com.android.systemui.R.drawable.ic_audio_vol_mute, false),
        RemoteStream(-200, R.string.httpErrorUnsupportedAuthScheme, com.android.systemui.R.drawable.ic_audio_remote, com.android.systemui.R.drawable.ic_audio_remote, false);

        int descRes;
        int iconMuteRes;
        int iconRes;
        boolean show;
        int streamType;

        StreamResources(int streamType, int descRes, int iconRes, int iconMuteRes, boolean show) {
            this.streamType = streamType;
            this.descRes = descRes;
            this.iconRes = iconRes;
            this.iconMuteRes = iconMuteRes;
            this.show = show;
        }
    }

    private class StreamControl {
        MediaController controller;
        View divider;
        ViewGroup group;
        ImageView icon;
        int iconMuteRes;
        int iconRes;
        int iconSuppressedRes;
        ImageView secondaryIcon;
        SeekBar seekbarView;
        int streamType;
        TextView suppressorView;

        private StreamControl() {
        }
    }

    private static class SafetyWarning extends SystemUIDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private final AudioManager mAudioManager;
        private final Context mContext;
        private boolean mNewVolumeUp;
        private final BroadcastReceiver mReceiver;
        private final VolumePanel mVolumePanel;

        SafetyWarning(Context context, VolumePanel volumePanel, AudioManager audioManager) {
            super(context);
            this.mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                        if (VolumePanel.LOGD) {
                            Log.d("VolumePanel", "Received ACTION_CLOSE_SYSTEM_DIALOGS");
                        }
                        SafetyWarning.this.cancel();
                        SafetyWarning.this.cleanUp();
                    }
                }
            };
            this.mContext = context;
            this.mVolumePanel = volumePanel;
            this.mAudioManager = audioManager;
            setMessage(this.mContext.getString(R.string.mediasize_iso_c10));
            setButton(-1, this.mContext.getString(R.string.yes), this);
            setButton(-2, this.mContext.getString(R.string.no), (DialogInterface.OnClickListener) null);
            setOnDismissListener(this);
            IntentFilter filter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            context.registerReceiver(this.mReceiver, filter);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == 24 && event.getRepeatCount() == 0) {
                this.mNewVolumeUp = true;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (keyCode == 24 && this.mNewVolumeUp) {
                if (VolumePanel.LOGD) {
                    Log.d("VolumePanel", "Confirmed warning via VOLUME_UP");
                }
                this.mAudioManager.disableSafeMediaVolume();
                dismiss();
            }
            return super.onKeyUp(keyCode, event);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            this.mAudioManager.disableSafeMediaVolume();
        }

        @Override
        public void onDismiss(DialogInterface unused) {
            this.mContext.unregisterReceiver(this.mReceiver);
            cleanUp();
        }

        private void cleanUp() {
            synchronized (VolumePanel.sSafetyWarningLock) {
                AlertDialog unused = VolumePanel.sSafetyWarning = null;
            }
            this.mVolumePanel.forceTimeout(0L);
            this.mVolumePanel.updateStates();
        }
    }

    public VolumePanel(Context context, ZenModeController zenController) {
        this.mContext = context;
        this.mZenController = zenController;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        this.mIconPulser = new IconPulser(context);
        Resources res = context.getResources();
        boolean useMasterVolume = res.getBoolean(R.^attr-private.alertDialogCenterButtons);
        if (useMasterVolume) {
            for (int i = 0; i < STREAMS.length; i++) {
                StreamResources streamRes = STREAMS[i];
                streamRes.show = streamRes.streamType == -100;
            }
        }
        if (LOGD) {
            Log.d(this.mTag, "new VolumePanel");
        }
        this.mDisabledAlpha = 0.5f;
        if (this.mContext.getTheme() != null) {
            TypedArray arr = this.mContext.getTheme().obtainStyledAttributes(new int[]{R.attr.disabledAlpha});
            this.mDisabledAlpha = arr.getFloat(0, this.mDisabledAlpha);
            arr.recycle();
        }
        this.mDialog = new Dialog(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (!isShowing() || event.getAction() != 4 || VolumePanel.sSafetyWarning != null) {
                    return false;
                }
                VolumePanel.this.forceTimeout(0L);
                return true;
            }
        };
        Window window = this.mDialog.getWindow();
        window.requestFeature(1);
        this.mDialog.setCanceledOnTouchOutside(true);
        this.mDialog.setContentView(com.android.systemui.R.layout.volume_dialog);
        this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                VolumePanel.this.mActiveStreamType = -1;
                VolumePanel.this.mAudioManager.forceVolumeControlStream(VolumePanel.this.mActiveStreamType);
                VolumePanel.this.setZenPanelVisible(false);
                VolumePanel.this.mDemoIcon = 0;
                VolumePanel.this.mSecondaryIconTransition.cancel();
            }
        });
        this.mDialog.create();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = null;
        lp.y = res.getDimensionPixelOffset(com.android.systemui.R.dimen.volume_panel_top);
        lp.type = 2014;
        lp.format = -3;
        lp.windowAnimations = com.android.systemui.R.style.VolumePanelAnimation;
        lp.setTitle("VolumePanel");
        window.setAttributes(lp);
        updateWidth();
        window.setBackgroundDrawable(new ColorDrawable(0));
        window.clearFlags(2);
        window.addFlags(R.string.config_systemAutomotiveCluster);
        this.mView = window.findViewById(R.id.content);
        Interaction.register(this.mView, new Interaction.Callback() {
            @Override
            public void onInteraction() {
                VolumePanel.this.resetTimeout();
            }
        });
        this.mPanel = (ViewGroup) this.mView.findViewById(com.android.systemui.R.id.visible_panel);
        this.mSliderPanel = (ViewGroup) this.mView.findViewById(com.android.systemui.R.id.slider_panel);
        this.mZenPanel = (ZenModePanel) this.mView.findViewById(com.android.systemui.R.id.zen_mode_panel);
        initZenModePanel();
        this.mToneGenerators = new ToneGenerator[AudioSystem.getNumStreamTypes()];
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mHasVibrator = this.mVibrator != null && this.mVibrator.hasVibrator();
        this.mVoiceCapable = context.getResources().getBoolean(R.^attr-private.externalRouteEnabledDrawable);
        if (this.mZenController != null && !useMasterVolume) {
            this.mZenModeAvailable = this.mZenController.isZenAvailable();
            this.mNotificationEffectsSuppressor = this.mZenController.getEffectsSuppressor();
            this.mZenController.addCallback(this.mZenCallback);
        }
        boolean masterVolumeOnly = res.getBoolean(R.^attr-private.alertDialogCenterButtons);
        boolean masterVolumeKeySounds = res.getBoolean(R.^attr-private.allowAutoRevokePermissionsExemption);
        this.mPlayMasterStreamTones = masterVolumeOnly && masterVolumeKeySounds;
        registerReceiver();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        updateWidth();
        if (this.mZenPanel != null) {
            this.mZenPanel.updateLocale();
        }
    }

    private void updateWidth() {
        Resources res = this.mContext.getResources();
        WindowManager.LayoutParams lp = this.mDialog.getWindow().getAttributes();
        lp.width = res.getDimensionPixelSize(com.android.systemui.R.dimen.notification_panel_width);
        lp.gravity = res.getInteger(com.android.systemui.R.integer.notification_panel_layout_gravity);
        this.mDialog.getWindow().setAttributes(lp);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("VolumePanel state:");
        pw.print("  mTag=");
        pw.println(this.mTag);
        pw.print("  mRingIsSilent=");
        pw.println(this.mRingIsSilent);
        pw.print("  mVoiceCapable=");
        pw.println(this.mVoiceCapable);
        pw.print("  mHasVibrator=");
        pw.println(this.mHasVibrator);
        pw.print("  mZenModeAvailable=");
        pw.println(this.mZenModeAvailable);
        pw.print("  mZenPanelExpanded=");
        pw.println(this.mZenPanelExpanded);
        pw.print("  mNotificationEffectsSuppressor=");
        pw.println(this.mNotificationEffectsSuppressor);
        pw.print("  mTimeoutDelay=");
        pw.println(this.mTimeoutDelay);
        pw.print("  mDisabledAlpha=");
        pw.println(this.mDisabledAlpha);
        pw.print("  mLastRingerMode=");
        pw.println(this.mLastRingerMode);
        pw.print("  mLastRingerProgress=");
        pw.println(this.mLastRingerProgress);
        pw.print("  mPlayMasterStreamTones=");
        pw.println(this.mPlayMasterStreamTones);
        pw.print("  isShowing()=");
        pw.println(isShowing());
        pw.print("  mCallback=");
        pw.println(this.mCallback);
        pw.print("  sConfirmSafeVolumeDialog=");
        pw.println(sSafetyWarning != null ? "<not null>" : null);
        pw.print("  mActiveStreamType=");
        pw.println(this.mActiveStreamType);
        pw.print("  mStreamControls=");
        if (this.mStreamControls == null) {
            pw.println("null");
        } else {
            int N = this.mStreamControls.size();
            pw.print("<size ");
            pw.print(N);
            pw.println('>');
            for (int i = 0; i < N; i++) {
                StreamControl sc = this.mStreamControls.valueAt(i);
                pw.print("    stream ");
                pw.print(sc.streamType);
                pw.print(":");
                if (sc.seekbarView != null) {
                    pw.print(" progress=");
                    pw.print(sc.seekbarView.getProgress());
                    pw.print(" of ");
                    pw.print(sc.seekbarView.getMax());
                    if (!sc.seekbarView.isEnabled()) {
                        pw.print(" (disabled)");
                    }
                }
                if (sc.icon != null && sc.icon.isClickable()) {
                    pw.print(" (clickable)");
                }
                pw.println();
            }
        }
        if (this.mZenPanel != null) {
            this.mZenPanel.dump(fd, pw, args);
        }
    }

    private void initZenModePanel() {
        this.mZenPanel.init(this.mZenController);
        this.mZenPanel.setCallback(new ZenModePanel.Callback() {
            @Override
            public void onMoreSettings() {
                if (VolumePanel.this.mCallback != null) {
                    VolumePanel.this.mCallback.onZenSettings();
                }
            }

            @Override
            public void onInteraction() {
                VolumePanel.this.resetTimeout();
            }

            @Override
            public void onExpanded(boolean expanded) {
                if (VolumePanel.this.mZenPanelExpanded != expanded) {
                    VolumePanel.this.mZenPanelExpanded = expanded;
                    VolumePanel.this.updateTimeoutDelay();
                    VolumePanel.this.resetTimeout();
                }
            }
        });
    }

    private void setLayoutDirection(int layoutDirection) {
        this.mPanel.setLayoutDirection(layoutDirection);
        updateStates();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.media.RINGER_MODE_CHANGED".equals(action)) {
                    VolumePanel.this.removeMessages(6);
                    VolumePanel.this.sendEmptyMessage(6);
                }
                if ("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION".equals(action)) {
                    VolumePanel.this.removeMessages(16);
                    VolumePanel.this.sendEmptyMessage(16);
                }
                if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    VolumePanel.this.postDismiss(0L);
                }
            }
        }, filter);
    }

    private boolean isMuted(int streamType) {
        if (streamType == -100) {
            return this.mAudioManager.isMasterMute();
        }
        if (streamType == -200) {
            return false;
        }
        return this.mAudioManager.isStreamMute(streamType);
    }

    private int getStreamMaxVolume(int streamType) {
        StreamControl sc;
        if (streamType == -100) {
            return this.mAudioManager.getMasterMaxVolume();
        }
        if (streamType == -200) {
            if (this.mStreamControls != null && (sc = this.mStreamControls.get(streamType)) != null && sc.controller != null) {
                MediaController.PlaybackInfo ai = sc.controller.getPlaybackInfo();
                return ai.getMaxVolume();
            }
            return -1;
        }
        return this.mAudioManager.getStreamMaxVolume(streamType);
    }

    private int getStreamVolume(int streamType) {
        StreamControl sc;
        if (streamType == -100) {
            return this.mAudioManager.getMasterVolume();
        }
        if (streamType == -200) {
            if (this.mStreamControls != null && (sc = this.mStreamControls.get(streamType)) != null && sc.controller != null) {
                MediaController.PlaybackInfo ai = sc.controller.getPlaybackInfo();
                return ai.getCurrentVolume();
            }
            return -1;
        }
        return this.mAudioManager.getStreamVolume(streamType);
    }

    private void setStreamVolume(StreamControl sc, int index, int flags) {
        if (sc.streamType == -200) {
            if (sc.controller != null) {
                sc.controller.setVolumeTo(index, flags);
                return;
            } else {
                Log.w(this.mTag, "Adjusting remote volume without a controller!");
                return;
            }
        }
        if (getStreamVolume(sc.streamType) != index) {
            if (sc.streamType == -100) {
                this.mAudioManager.setMasterVolume(index, flags);
            } else {
                this.mAudioManager.setStreamVolume(sc.streamType, index, flags);
            }
        }
    }

    private void createSliders() {
        Resources res = this.mContext.getResources();
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        this.mStreamControls = new SparseArray<>(STREAMS.length);
        StreamResources notificationStream = StreamResources.NotificationStream;
        for (int i = 0; i < STREAMS.length; i++) {
            StreamResources streamRes = STREAMS[i];
            int streamType = streamRes.streamType;
            boolean isNotification = isNotificationOrRing(streamType);
            final StreamControl sc = new StreamControl();
            sc.streamType = streamType;
            sc.group = (ViewGroup) inflater.inflate(com.android.systemui.R.layout.volume_panel_item, (ViewGroup) null);
            sc.group.setTag(sc);
            sc.icon = (ImageView) sc.group.findViewById(com.android.systemui.R.id.stream_icon);
            sc.icon.setTag(sc);
            sc.icon.setContentDescription(res.getString(streamRes.descRes));
            sc.iconRes = streamRes.iconRes;
            sc.iconMuteRes = streamRes.iconMuteRes;
            sc.icon.setImageResource(sc.iconRes);
            sc.icon.setClickable(isNotification && this.mHasVibrator);
            if (isNotification) {
                if (this.mHasVibrator) {
                    sc.icon.setSoundEffectsEnabled(false);
                    sc.iconMuteRes = com.android.systemui.R.drawable.ic_ringer_vibrate;
                    sc.icon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VolumePanel.this.resetTimeout();
                            VolumePanel.this.toggleRinger(sc);
                        }
                    });
                }
                sc.iconSuppressedRes = com.android.systemui.R.drawable.ic_ringer_mute;
            }
            sc.seekbarView = (SeekBar) sc.group.findViewById(com.android.systemui.R.id.seekbar);
            sc.suppressorView = (TextView) sc.group.findViewById(com.android.systemui.R.id.suppressor);
            sc.suppressorView.setVisibility(8);
            boolean showSecondary = !isNotification && notificationStream.show;
            sc.divider = sc.group.findViewById(com.android.systemui.R.id.divider);
            sc.secondaryIcon = (ImageView) sc.group.findViewById(com.android.systemui.R.id.secondary_icon);
            sc.secondaryIcon.setImageResource(com.android.systemui.R.drawable.ic_ringer_audible);
            sc.secondaryIcon.setContentDescription(res.getString(notificationStream.descRes));
            sc.secondaryIcon.setClickable(showSecondary);
            sc.divider.setVisibility(showSecondary ? 0 : 8);
            sc.secondaryIcon.setVisibility(showSecondary ? 0 : 8);
            if (showSecondary) {
                sc.secondaryIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        VolumePanel.this.mSecondaryIconTransition.start(sc);
                    }
                });
            }
            int plusOne = (streamType == 6 || streamType == 0) ? 1 : 0;
            sc.seekbarView.setMax(getStreamMaxVolume(streamType) + plusOne);
            sc.seekbarView.setOnSeekBarChangeListener(this.mSeekListener);
            sc.seekbarView.setTag(sc);
            this.mStreamControls.put(streamType, sc);
        }
    }

    private void toggleRinger(StreamControl sc) {
        if (this.mHasVibrator) {
            if (this.mAudioManager.getRingerModeInternal() == 2) {
                this.mAudioManager.setRingerModeInternal(1);
                postVolumeChanged(sc.streamType, 17);
            } else {
                this.mAudioManager.setRingerModeInternal(2);
                postVolumeChanged(sc.streamType, 4);
            }
        }
    }

    private void reorderSliders(int activeStreamType) {
        this.mSliderPanel.removeAllViews();
        StreamControl active = this.mStreamControls.get(activeStreamType);
        if (active == null) {
            Log.e("VolumePanel", "Missing stream type! - " + activeStreamType);
            this.mActiveStreamType = -1;
            return;
        }
        this.mSliderPanel.addView(active.group);
        this.mActiveStreamType = activeStreamType;
        active.group.setVisibility(0);
        updateSlider(active, true);
        updateTimeoutDelay();
        updateZenPanelVisible();
    }

    private void updateSliderProgress(StreamControl sc, int progress) {
        boolean isRinger = isNotificationOrRing(sc.streamType);
        if (isRinger && this.mAudioManager.getRingerModeInternal() == 0) {
            progress = this.mLastRingerProgress;
        }
        if (progress < 0) {
            progress = getStreamVolume(sc.streamType);
        }
        sc.seekbarView.setProgress(progress);
        if (isRinger) {
            this.mLastRingerProgress = progress;
        }
    }

    private void updateSliderIcon(StreamControl sc, boolean muted) {
        int i;
        ComponentName suppressor = null;
        if (isNotificationOrRing(sc.streamType)) {
            suppressor = this.mNotificationEffectsSuppressor;
            int ringerMode = this.mAudioManager.getRingerModeInternal();
            if (ringerMode == 0) {
                ringerMode = this.mLastRingerMode;
            } else {
                this.mLastRingerMode = ringerMode;
            }
            muted = this.mHasVibrator && ringerMode == 1;
        }
        ImageView imageView = sc.icon;
        if (this.mDemoIcon != 0) {
            i = this.mDemoIcon;
        } else {
            i = suppressor != null ? sc.iconSuppressedRes : muted ? sc.iconMuteRes : sc.iconRes;
        }
        imageView.setImageResource(i);
    }

    private void updateSliderSuppressor(StreamControl sc) {
        ComponentName suppressor = isNotificationOrRing(sc.streamType) ? this.mNotificationEffectsSuppressor : null;
        if (suppressor == null) {
            sc.seekbarView.setVisibility(0);
            sc.suppressorView.setVisibility(8);
        } else {
            sc.seekbarView.setVisibility(8);
            sc.suppressorView.setVisibility(0);
            sc.suppressorView.setText(this.mContext.getString(R.string.network_switch_type_name_unknown, getSuppressorCaption(suppressor)));
        }
    }

    private String getSuppressorCaption(ComponentName suppressor) {
        CharSequence seq;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ServiceInfo info = pm.getServiceInfo(suppressor, 0);
            if (info != null && (seq = info.loadLabel(pm)) != null) {
                String str = seq.toString().trim();
                if (str.length() > 0) {
                    return str;
                }
            }
        } catch (Throwable e) {
            Log.w("VolumePanel", "Error loading suppressor caption", e);
        }
        return suppressor.getPackageName();
    }

    private void updateSlider(StreamControl sc, boolean forceReloadIcon) {
        updateSliderProgress(sc, -1);
        boolean muted = isMuted(sc.streamType);
        if (forceReloadIcon) {
            sc.icon.setImageDrawable(null);
        }
        updateSliderIcon(sc, muted);
        updateSliderEnabled(sc, muted, false);
        updateSliderSuppressor(sc);
    }

    private void updateSliderEnabled(StreamControl sc, boolean muted, boolean fixedVolume) {
        boolean wasEnabled = sc.seekbarView.isEnabled();
        boolean isRinger = isNotificationOrRing(sc.streamType);
        if (sc.streamType == -200) {
            sc.seekbarView.setEnabled(fixedVolume ? false : true);
        } else if (isRinger && this.mNotificationEffectsSuppressor != null) {
            sc.icon.setEnabled(true);
            sc.icon.setAlpha(1.0f);
            sc.icon.setClickable(false);
        } else if (isRinger && this.mAudioManager.getRingerModeInternal() == 0) {
            sc.seekbarView.setEnabled(false);
            sc.icon.setEnabled(false);
            sc.icon.setAlpha(this.mDisabledAlpha);
            sc.icon.setClickable(false);
        } else if (fixedVolume || ((sc.streamType != this.mAudioManager.getMasterStreamType() && !isRinger && muted) || sSafetyWarning != null)) {
            sc.seekbarView.setEnabled(false);
        } else {
            sc.seekbarView.setEnabled(true);
            sc.icon.setEnabled(true);
            sc.icon.setAlpha(1.0f);
        }
        if (isRinger && wasEnabled != sc.seekbarView.isEnabled()) {
            if (sc.seekbarView.isEnabled()) {
                sc.group.setOnTouchListener(null);
                sc.icon.setClickable(this.mHasVibrator);
            } else {
                View.OnTouchListener showHintOnTouch = new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        VolumePanel.this.resetTimeout();
                        VolumePanel.this.showSilentHint();
                        return false;
                    }
                };
                sc.group.setOnTouchListener(showHintOnTouch);
            }
        }
    }

    private void showSilentHint() {
        if (this.mZenPanel != null) {
            this.mZenPanel.showSilentHint();
        }
    }

    private void showVibrateHint() {
        StreamControl active = this.mStreamControls.get(this.mActiveStreamType);
        if (active != null) {
            this.mIconPulser.start(active.icon);
            if (!hasMessages(4)) {
                sendEmptyMessageDelayed(4, 300L);
            }
        }
    }

    private static boolean isNotificationOrRing(int streamType) {
        return streamType == 2 || streamType == 5;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    private void updateTimeoutDelay() {
        int i = 10000;
        if (this.mDemoIcon == 0) {
            if (sSafetyWarning != null) {
                i = 5000;
            } else if (this.mActiveStreamType == 3) {
                i = 1500;
            } else if (!this.mZenPanelExpanded) {
                i = isZenPanelVisible() ? 4500 : 3000;
            }
        }
        this.mTimeoutDelay = i;
    }

    private boolean isZenPanelVisible() {
        return this.mZenPanel != null && this.mZenPanel.getVisibility() == 0;
    }

    private void setZenPanelVisible(boolean visible) {
        if (LOGD) {
            Log.d(this.mTag, "setZenPanelVisible " + visible + " mZenPanel=" + this.mZenPanel);
        }
        boolean changing = visible != isZenPanelVisible();
        if (visible) {
            this.mZenPanel.setHidden(false);
            resetTimeout();
        } else {
            this.mZenPanel.setHidden(true);
        }
        if (changing) {
            updateTimeoutDelay();
            resetTimeout();
        }
    }

    private void updateStates() {
        int count = this.mSliderPanel.getChildCount();
        for (int i = 0; i < count; i++) {
            StreamControl sc = (StreamControl) this.mSliderPanel.getChildAt(i).getTag();
            updateSlider(sc, true);
        }
    }

    private void updateActiveSlider() {
        StreamControl active = this.mStreamControls.get(this.mActiveStreamType);
        if (active != null) {
            updateSlider(active, false);
        }
    }

    private void updateZenPanelVisible() {
        setZenPanelVisible(this.mZenModeAvailable && isNotificationOrRing(this.mActiveStreamType));
    }

    public void postVolumeChanged(int streamType, int flags) {
        if (!hasMessages(0)) {
            synchronized (this) {
                if (this.mStreamControls == null) {
                    createSliders();
                }
            }
            removeMessages(1);
            obtainMessage(0, streamType, flags).sendToTarget();
        }
    }

    public void postRemoteVolumeChanged(MediaController controller, int flags) {
        if (!hasMessages(8)) {
            synchronized (this) {
                if (this.mStreamControls == null) {
                    createSliders();
                }
            }
            removeMessages(1);
            obtainMessage(8, flags, 0, controller).sendToTarget();
        }
    }

    public void postRemoteSliderVisibility(boolean visible) {
        obtainMessage(10, -200, visible ? 1 : 0).sendToTarget();
    }

    public void postMasterVolumeChanged(int flags) {
        postVolumeChanged(-100, flags);
    }

    public void postMuteChanged(int streamType, int flags) {
        if (!hasMessages(0)) {
            synchronized (this) {
                if (this.mStreamControls == null) {
                    createSliders();
                }
            }
            removeMessages(1);
            obtainMessage(7, streamType, flags).sendToTarget();
        }
    }

    public void postMasterMuteChanged(int flags) {
        postMuteChanged(-100, flags);
    }

    public void postDisplaySafeVolumeWarning(int flags) {
        if (!hasMessages(11)) {
            obtainMessage(11, flags, 0).sendToTarget();
        }
    }

    public void postDismiss(long delay) {
        forceTimeout(delay);
    }

    public void postLayoutDirection(int layoutDirection) {
        removeMessages(12);
        obtainMessage(12, layoutDirection, 0).sendToTarget();
    }

    private static String flagsToString(int flags) {
        return flags == 0 ? "0" : flags + "=" + AudioManager.flagsToString(flags);
    }

    private static String streamToString(int stream) {
        return AudioService.streamToString(stream);
    }

    protected void onVolumeChanged(int streamType, int flags) {
        if (LOGD) {
            Log.d(this.mTag, "onVolumeChanged(streamType: " + streamToString(streamType) + ", flags: " + flagsToString(flags) + ")");
        }
        if ((flags & 1) != 0) {
            synchronized (this) {
                if (this.mActiveStreamType != streamType) {
                    reorderSliders(streamType);
                }
                onShowVolumeChanged(streamType, flags, null);
            }
        }
        if ((flags & 4) != 0 && !this.mRingIsSilent) {
            removeMessages(2);
            sendMessageDelayed(obtainMessage(2, streamType, flags), 300L);
        }
        if ((flags & 8) != 0) {
            removeMessages(2);
            removeMessages(4);
            onStopSounds();
        }
        removeMessages(1);
        sendMessageDelayed(obtainMessage(1), 10000L);
        resetTimeout();
    }

    protected void onMuteChanged(int streamType, int flags) {
        if (LOGD) {
            Log.d(this.mTag, "onMuteChanged(streamType: " + streamToString(streamType) + ", flags: " + flagsToString(flags) + ")");
        }
        StreamControl sc = this.mStreamControls.get(streamType);
        if (sc != null) {
            updateSliderIcon(sc, isMuted(sc.streamType));
        }
        onVolumeChanged(streamType, flags);
    }

    protected void onShowVolumeChanged(int streamType, int flags, MediaController controller) {
        int index = getStreamVolume(streamType);
        this.mRingIsSilent = false;
        if (LOGD) {
            Log.d(this.mTag, "onShowVolumeChanged(streamType: " + streamToString(streamType) + ", flags: " + flagsToString(flags) + "), index: " + index);
        }
        int max = getStreamMaxVolume(streamType);
        StreamControl sc = this.mStreamControls.get(streamType);
        switch (streamType) {
            case -200:
                if (controller == null && sc != null) {
                    controller = sc.controller;
                }
                if (controller == null) {
                    Log.w(this.mTag, "sent remote volume change without a controller!");
                } else {
                    MediaController.PlaybackInfo vi = controller.getPlaybackInfo();
                    index = vi.getCurrentVolume();
                    max = vi.getMaxVolume();
                    if ((vi.getVolumeControl() & 0) != 0) {
                        flags |= 32;
                    }
                }
                if (LOGD) {
                    Log.d(this.mTag, "showing remote volume " + index + " over " + max);
                }
                break;
            case 0:
                index++;
                max++;
                break;
            case 2:
                Uri ringuri = RingtoneManager.getActualDefaultRingtoneUri(this.mContext, 1);
                if (ringuri == null) {
                    this.mRingIsSilent = true;
                }
                break;
            case 3:
                if ((this.mAudioManager.getDevicesForStream(3) & 896) != 0) {
                    setMusicIcon(com.android.systemui.R.drawable.ic_audio_bt, com.android.systemui.R.drawable.ic_audio_bt_mute);
                } else {
                    setMusicIcon(com.android.systemui.R.drawable.ic_audio_vol, com.android.systemui.R.drawable.ic_audio_vol_mute);
                }
                break;
            case 5:
                Uri ringuri2 = RingtoneManager.getActualDefaultRingtoneUri(this.mContext, 2);
                if (ringuri2 == null) {
                    this.mRingIsSilent = true;
                }
                break;
            case 6:
                index++;
                max++;
                break;
        }
        if (sc != null) {
            if (streamType == -200 && controller != sc.controller) {
                if (sc.controller != null) {
                    sc.controller.unregisterCallback(this.mMediaControllerCb);
                }
                sc.controller = controller;
                if (controller != null) {
                    sc.controller.registerCallback(this.mMediaControllerCb);
                }
            }
            if (sc.seekbarView.getMax() != max) {
                sc.seekbarView.setMax(max);
            }
            updateSliderProgress(sc, index);
            boolean muted = isMuted(streamType);
            updateSliderEnabled(sc, muted, (flags & 32) != 0);
            if (isNotificationOrRing(streamType)) {
                if (this.mSecondaryIconTransition.isRunning()) {
                    this.mSecondaryIconTransition.cancel();
                    sc.seekbarView.setAlpha(0.0f);
                    sc.seekbarView.animate().alpha(1.0f);
                    this.mZenPanel.setAlpha(0.0f);
                    this.mZenPanel.animate().alpha(1.0f);
                }
                updateSliderIcon(sc, muted);
            }
        }
        if (!isShowing()) {
            int stream = streamType == -200 ? -1 : streamType;
            if (stream != -100) {
                this.mAudioManager.forceVolumeControlStream(stream);
            }
            this.mDialog.show();
            if (this.mCallback != null) {
                this.mCallback.onVisible(true);
            }
            announceDialogShown();
        }
        if (streamType != -200 && (flags & 16) != 0 && isNotificationOrRing(streamType) && this.mAudioManager.getRingerModeInternal() == 1) {
            sendMessageDelayed(obtainMessage(4), 300L);
        }
        if ((flags & 128) != 0) {
            showSilentHint();
        }
        if ((flags & 2048) != 0) {
            showVibrateHint();
        }
    }

    private void announceDialogShown() {
        this.mView.sendAccessibilityEvent(32);
    }

    private boolean isShowing() {
        return this.mDialog.isShowing();
    }

    protected void onPlaySound(int streamType, int flags) {
        if (hasMessages(3)) {
            removeMessages(3);
            onStopSounds();
        }
        synchronized (this) {
            ToneGenerator toneGen = getOrCreateToneGenerator(streamType);
            if (toneGen != null) {
                toneGen.startTone(24);
                sendMessageDelayed(obtainMessage(3), 150L);
            }
        }
    }

    protected void onStopSounds() {
        synchronized (this) {
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int i = numStreamTypes - 1; i >= 0; i--) {
                ToneGenerator toneGen = this.mToneGenerators[i];
                if (toneGen != null) {
                    toneGen.stopTone();
                }
            }
        }
    }

    protected void onVibrate() {
        if (this.mAudioManager.getRingerModeInternal() == 1 && this.mVibrator != null) {
            this.mVibrator.vibrate(300L, VIBRATION_ATTRIBUTES);
        }
    }

    protected void onRemoteVolumeChanged(MediaController controller, int flags) {
        if (LOGD) {
            Log.d(this.mTag, "onRemoteVolumeChanged(controller:" + controller + ", flags: " + flagsToString(flags) + ")");
        }
        if ((flags & 1) != 0 || isShowing()) {
            synchronized (this) {
                if (this.mActiveStreamType != -200) {
                    reorderSliders(-200);
                }
                onShowVolumeChanged(-200, flags, controller);
            }
        } else if (LOGD) {
            Log.d(this.mTag, "not calling onShowVolumeChanged(), no FLAG_SHOW_UI or no UI");
        }
        removeMessages(1);
        sendMessageDelayed(obtainMessage(1), 10000L);
        resetTimeout();
    }

    protected void onRemoteVolumeUpdateIfShown() {
        if (LOGD) {
            Log.d(this.mTag, "onRemoteVolumeUpdateIfShown()");
        }
        if (isShowing() && this.mActiveStreamType == -200 && this.mStreamControls != null) {
            onShowVolumeChanged(-200, 0, null);
        }
    }

    private void clearRemoteStreamController() {
        StreamControl sc;
        if (this.mStreamControls != null && (sc = this.mStreamControls.get(-200)) != null && sc.controller != null) {
            sc.controller.unregisterCallback(this.mMediaControllerCb);
            sc.controller = null;
        }
    }

    protected synchronized void onSliderVisibilityChanged(int streamType, int visible) {
        synchronized (this) {
            if (LOGD) {
                Log.d(this.mTag, "onSliderVisibilityChanged(stream=" + streamType + ", visi=" + visible + ")");
            }
            boolean isVisible = visible == 1;
            int i = STREAMS.length - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                StreamResources streamRes = STREAMS[i];
                if (streamRes.streamType == streamType) {
                    break;
                } else {
                    i--;
                }
            }
        }
    }

    protected void onDisplaySafeVolumeWarning(int flags) {
        if ((flags & 1025) != 0 || isShowing()) {
            synchronized (sSafetyWarningLock) {
                if (sSafetyWarning == null) {
                    sSafetyWarning = new SafetyWarning(this.mContext, this, this.mAudioManager);
                    sSafetyWarning.show();
                    updateStates();
                } else {
                    return;
                }
            }
        }
        if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
            removeMessages(5);
        } else {
            updateTimeoutDelay();
            resetTimeout();
        }
    }

    private ToneGenerator getOrCreateToneGenerator(int streamType) {
        ToneGenerator toneGenerator;
        if (streamType == -100) {
            if (this.mPlayMasterStreamTones) {
                streamType = 1;
            } else {
                return null;
            }
        }
        synchronized (this) {
            if (this.mToneGenerators[streamType] == null) {
                try {
                    this.mToneGenerators[streamType] = new ToneGenerator(streamType, 100);
                } catch (RuntimeException e) {
                    if (LOGD) {
                        Log.d(this.mTag, "ToneGenerator constructor failed with RuntimeException: " + e);
                    }
                }
                toneGenerator = this.mToneGenerators[streamType];
            } else {
                toneGenerator = this.mToneGenerators[streamType];
            }
        }
        return toneGenerator;
    }

    private void setMusicIcon(int resId, int resMuteId) {
        StreamControl sc = this.mStreamControls.get(3);
        if (sc != null) {
            sc.iconRes = resId;
            sc.iconMuteRes = resMuteId;
            updateSliderIcon(sc, isMuted(sc.streamType));
        }
    }

    protected void onFreeResources() {
        synchronized (this) {
            for (int i = this.mToneGenerators.length - 1; i >= 0; i--) {
                if (this.mToneGenerators[i] != null) {
                    this.mToneGenerators[i].release();
                }
                this.mToneGenerators[i] = null;
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                onVolumeChanged(msg.arg1, msg.arg2);
                return;
            case 1:
                onFreeResources();
                return;
            case 2:
                onPlaySound(msg.arg1, msg.arg2);
                return;
            case 3:
                onStopSounds();
                return;
            case 4:
                onVibrate();
                return;
            case 5:
                if (isShowing()) {
                    this.mDialog.dismiss();
                    clearRemoteStreamController();
                    this.mActiveStreamType = -1;
                    if (this.mCallback != null) {
                        this.mCallback.onVisible(false);
                    }
                }
                synchronized (sSafetyWarningLock) {
                    if (sSafetyWarning != null) {
                        if (LOGD) {
                            Log.d(this.mTag, "SafetyWarning timeout");
                        }
                        sSafetyWarning.dismiss();
                    }
                    break;
                }
                return;
            case 6:
            case 15:
            case 16:
                if (isShowing()) {
                    updateActiveSlider();
                    return;
                }
                return;
            case 7:
                onMuteChanged(msg.arg1, msg.arg2);
                return;
            case 8:
                onRemoteVolumeChanged((MediaController) msg.obj, msg.arg1);
                return;
            case 9:
                onRemoteVolumeUpdateIfShown();
                return;
            case 10:
                onSliderVisibilityChanged(msg.arg1, msg.arg2);
                return;
            case 11:
                onDisplaySafeVolumeWarning(msg.arg1);
                return;
            case 12:
                setLayoutDirection(msg.arg1);
                return;
            case 13:
                this.mZenModeAvailable = msg.arg1 != 0;
                updateZenPanelVisible();
                return;
            case 14:
                if (this.mCallback != null) {
                    this.mCallback.onInteraction();
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void resetTimeout() {
        boolean touchExploration = this.mAccessibilityManager.isTouchExplorationEnabled();
        if (LOGD) {
            Log.d(this.mTag, "resetTimeout at " + System.currentTimeMillis() + " delay=" + this.mTimeoutDelay + " touchExploration=" + touchExploration);
        }
        if (sSafetyWarning == null || !touchExploration) {
            removeMessages(5);
            sendEmptyMessageDelayed(5, this.mTimeoutDelay);
            removeMessages(14);
            sendEmptyMessage(14);
        }
    }

    private void forceTimeout(long delay) {
        if (LOGD) {
            Log.d(this.mTag, "forceTimeout delay=" + delay + " callers=" + Debug.getCallers(3));
        }
        removeMessages(5);
        sendEmptyMessageDelayed(5, delay);
    }

    public ZenModeController getZenController() {
        return this.mZenController;
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if ("volume".equals(command)) {
            String icon = args.getString("icon");
            String iconMute = args.getString("iconmute");
            boolean mute = iconMute != null;
            if (mute) {
                icon = iconMute;
            }
            if (!icon.endsWith("Stream")) {
                icon = icon + "Stream";
            }
            StreamResources sr = StreamResources.valueOf(icon);
            this.mDemoIcon = mute ? sr.iconMuteRes : sr.iconRes;
            int forcedStreamType = StreamResources.MediaStream.streamType;
            this.mAudioManager.forceVolumeControlStream(forcedStreamType);
            this.mAudioManager.adjustStreamVolume(forcedStreamType, 0, 1);
        }
    }

    private final class SecondaryIconTransition extends AnimatorListenerAdapter implements Runnable {
        private final int mAnimationTime;
        private final int mDelayTime;
        private final int mFadeOutTime;
        private final Interpolator mIconInterpolator;
        private StreamControl mTarget;

        private SecondaryIconTransition() {
            this.mAnimationTime = (int) (400.0f * ValueAnimator.getDurationScale());
            this.mFadeOutTime = this.mAnimationTime / 2;
            this.mDelayTime = this.mAnimationTime / 3;
            this.mIconInterpolator = AnimationUtils.loadInterpolator(VolumePanel.this.mContext, R.interpolator.fast_out_slow_in);
        }

        public void start(StreamControl sc) {
            if (sc == null) {
                throw new IllegalArgumentException();
            }
            if (VolumePanel.LOGD) {
                Log.d(VolumePanel.this.mTag, "Secondary icon animation start");
            }
            if (this.mTarget != null) {
                cancel();
            }
            this.mTarget = sc;
            VolumePanel.this.mTimeoutDelay = this.mAnimationTime + 1000;
            VolumePanel.this.resetTimeout();
            this.mTarget.secondaryIcon.setClickable(false);
            int N = this.mTarget.group.getChildCount();
            for (int i = 0; i < N; i++) {
                View child = this.mTarget.group.getChildAt(i);
                if (child != this.mTarget.secondaryIcon) {
                    child.animate().alpha(0.0f).setDuration(this.mFadeOutTime).start();
                }
            }
            this.mTarget.secondaryIcon.animate().translationXBy(this.mTarget.icon.getX() - this.mTarget.secondaryIcon.getX()).setInterpolator(this.mIconInterpolator).setStartDelay(this.mDelayTime).setDuration(this.mAnimationTime - this.mDelayTime).setListener(this).start();
        }

        public boolean isRunning() {
            return this.mTarget != null;
        }

        public void cancel() {
            if (this.mTarget != null) {
                this.mTarget.secondaryIcon.setClickable(true);
                int N = this.mTarget.group.getChildCount();
                for (int i = 0; i < N; i++) {
                    View child = this.mTarget.group.getChildAt(i);
                    if (child != this.mTarget.secondaryIcon) {
                        child.animate().cancel();
                        child.setAlpha(1.0f);
                    }
                }
                this.mTarget.secondaryIcon.animate().cancel();
                this.mTarget.secondaryIcon.setTranslationX(0.0f);
                this.mTarget = null;
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (this.mTarget != null) {
                AsyncTask.execute(this);
            }
        }

        @Override
        public void run() {
            if (this.mTarget != null) {
                if (VolumePanel.LOGD) {
                    Log.d(VolumePanel.this.mTag, "Secondary icon animation complete, show notification slider");
                }
                VolumePanel.this.mAudioManager.forceVolumeControlStream(StreamResources.NotificationStream.streamType);
                VolumePanel.this.mAudioManager.adjustStreamVolume(StreamResources.NotificationStream.streamType, 0, 1);
            }
        }
    }
}
