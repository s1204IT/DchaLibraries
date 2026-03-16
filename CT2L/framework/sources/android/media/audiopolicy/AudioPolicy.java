package android.media.audiopolicy;

import android.Manifest;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.IAudioService;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;

public class AudioPolicy {
    private static final boolean DEBUG = false;
    public static final int FOCUS_POLICY_DUCKING_DEFAULT = 0;
    public static final int FOCUS_POLICY_DUCKING_IN_APP = 0;
    public static final int FOCUS_POLICY_DUCKING_IN_POLICY = 1;
    private static final int MSG_FOCUS_GRANT = 1;
    private static final int MSG_FOCUS_LOSS = 2;
    private static final int MSG_POLICY_STATUS_CHANGE = 0;
    public static final int POLICY_STATUS_REGISTERED = 2;
    public static final int POLICY_STATUS_UNREGISTERED = 1;
    private static final String TAG = "AudioPolicy";
    private static IAudioService sService;
    private AudioPolicyConfig mConfig;
    private Context mContext;
    private final EventHandler mEventHandler;
    private AudioPolicyFocusListener mFocusListener;
    private final Object mLock;
    private final IAudioPolicyCallback mPolicyCb;
    private String mRegistrationId;
    private int mStatus;
    private AudioPolicyStatusListener mStatusListener;

    public AudioPolicyConfig getConfig() {
        return this.mConfig;
    }

    public boolean hasFocusListener() {
        return this.mFocusListener != null;
    }

    private AudioPolicy(AudioPolicyConfig config, Context context, Looper looper, AudioPolicyFocusListener fl, AudioPolicyStatusListener sl) {
        this.mLock = new Object();
        this.mPolicyCb = new IAudioPolicyCallback.Stub() {
            @Override
            public void notifyAudioFocusGrant(AudioFocusInfo afi, int requestResult) {
                AudioPolicy.this.sendMsg(1, afi, requestResult);
            }

            @Override
            public void notifyAudioFocusLoss(AudioFocusInfo afi, boolean wasNotified) {
                AudioPolicy.this.sendMsg(2, afi, wasNotified ? 1 : 0);
            }
        };
        this.mConfig = config;
        this.mStatus = 1;
        this.mContext = context;
        looper = looper == null ? Looper.getMainLooper() : looper;
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            this.mEventHandler = null;
            Log.e(TAG, "No event handler due to looper without a thread");
        }
        this.mFocusListener = fl;
        this.mStatusListener = sl;
    }

    public static class Builder {
        private Context mContext;
        private AudioPolicyFocusListener mFocusListener;
        private Looper mLooper;
        private ArrayList<AudioMix> mMixes = new ArrayList<>();
        private AudioPolicyStatusListener mStatusListener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder addMix(AudioMix mix) throws IllegalArgumentException {
            if (mix == null) {
                throw new IllegalArgumentException("Illegal null AudioMix argument");
            }
            this.mMixes.add(mix);
            return this;
        }

        public Builder setLooper(Looper looper) throws IllegalArgumentException {
            if (looper == null) {
                throw new IllegalArgumentException("Illegal null Looper argument");
            }
            this.mLooper = looper;
            return this;
        }

        public void setAudioPolicyFocusListener(AudioPolicyFocusListener l) {
            this.mFocusListener = l;
        }

        public void setAudioPolicyStatusListener(AudioPolicyStatusListener l) {
            this.mStatusListener = l;
        }

        public AudioPolicy build() {
            return new AudioPolicy(new AudioPolicyConfig(this.mMixes), this.mContext, this.mLooper, this.mFocusListener, this.mStatusListener);
        }
    }

    public void setRegistration(String regId) {
        synchronized (this.mLock) {
            this.mRegistrationId = regId;
            this.mConfig.setRegistration(regId);
            if (regId != null) {
                this.mStatus = 2;
            } else {
                this.mStatus = 1;
            }
        }
        sendMsg(0);
    }

    private boolean policyReadyToUse() {
        synchronized (this.mLock) {
            if (this.mStatus != 2) {
                Log.e(TAG, "Cannot use unregistered AudioPolicy");
                return false;
            }
            if (this.mContext == null) {
                Log.e(TAG, "Cannot use AudioPolicy without context");
                return false;
            }
            if (this.mRegistrationId == null) {
                Log.e(TAG, "Cannot use unregistered AudioPolicy");
                return false;
            }
            if (this.mContext.checkCallingOrSelfPermission(Manifest.permission.MODIFY_AUDIO_ROUTING) != 0) {
                Slog.w(TAG, "Cannot use AudioPolicy for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", needs MODIFY_AUDIO_ROUTING");
                return false;
            }
            return true;
        }
    }

    private void checkMixReadyToUse(AudioMix mix, boolean forTrack) throws IllegalArgumentException {
        if (mix == null) {
            String msg = forTrack ? "Invalid null AudioMix for AudioTrack creation" : "Invalid null AudioMix for AudioRecord creation";
            throw new IllegalArgumentException(msg);
        }
        if (!this.mConfig.mMixes.contains(mix)) {
            throw new IllegalArgumentException("Invalid mix: not part of this policy");
        }
        if ((mix.getRouteFlags() & 2) != 2) {
            throw new IllegalArgumentException("Invalid AudioMix: not defined for loop back");
        }
        if (forTrack && mix.getMixType() != 1) {
            throw new IllegalArgumentException("Invalid AudioMix: not defined for being a recording source");
        }
        if (!forTrack && mix.getMixType() != 0) {
            throw new IllegalArgumentException("Invalid AudioMix: not defined for capturing playback");
        }
    }

    public int getFocusDuckingBehavior() {
        return this.mConfig.mDuckingPolicy;
    }

    public int setFocusDuckingBehavior(int behavior) throws IllegalStateException, IllegalArgumentException {
        int status;
        if (behavior != 0 && behavior != 1) {
            throw new IllegalArgumentException("Invalid ducking behavior " + behavior);
        }
        synchronized (this.mLock) {
            if (this.mStatus != 2) {
                throw new IllegalStateException("Cannot change ducking behavior for unregistered policy");
            }
            if (behavior == 1 && this.mFocusListener == null) {
                throw new IllegalStateException("Cannot handle ducking without an audio focus listener");
            }
            IAudioService service = getService();
            try {
                status = service.setFocusPropertiesForPolicy(behavior, cb());
                if (status == 0) {
                    this.mConfig.mDuckingPolicy = behavior;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Dead object in setFocusPropertiesForPolicy for behavior", e);
                status = -1;
            }
        }
        return status;
    }

    public AudioRecord createAudioRecordSink(AudioMix mix) throws IllegalArgumentException {
        if (!policyReadyToUse()) {
            Log.e(TAG, "Cannot create AudioRecord sink for AudioMix");
            return null;
        }
        checkMixReadyToUse(mix, false);
        AudioFormat mixFormat = new AudioFormat.Builder(mix.getFormat()).setChannelMask(AudioFormat.inChannelMaskFromOutChannelMask(mix.getFormat().getChannelMask())).build();
        return new AudioRecord(new AudioAttributes.Builder().setInternalCapturePreset(8).addTag(addressForTag(mix)).build(), mixFormat, AudioRecord.getMinBufferSize(mix.getFormat().getSampleRate(), 12, mix.getFormat().getEncoding()), 0);
    }

    public AudioTrack createAudioTrackSource(AudioMix mix) throws IllegalArgumentException {
        if (!policyReadyToUse()) {
            Log.e(TAG, "Cannot create AudioTrack source for AudioMix");
            return null;
        }
        checkMixReadyToUse(mix, true);
        return new AudioTrack(new AudioAttributes.Builder().setUsage(15).addTag(addressForTag(mix)).build(), mix.getFormat(), AudioTrack.getMinBufferSize(mix.getFormat().getSampleRate(), mix.getFormat().getChannelMask(), mix.getFormat().getEncoding()), 1, 0);
    }

    public int getStatus() {
        return this.mStatus;
    }

    public static abstract class AudioPolicyStatusListener {
        public void onStatusChange() {
        }

        public void onMixStateUpdate(AudioMix mix) {
        }
    }

    public static abstract class AudioPolicyFocusListener {
        public void onAudioFocusGrant(AudioFocusInfo afi, int requestResult) {
        }

        public void onAudioFocusLoss(AudioFocusInfo afi, boolean wasNotified) {
        }
    }

    private void onPolicyStatusChange() {
        synchronized (this.mLock) {
            if (this.mStatusListener != null) {
                AudioPolicyStatusListener l = this.mStatusListener;
                l.onStatusChange();
            }
        }
    }

    public IAudioPolicyCallback cb() {
        return this.mPolicyCb;
    }

    private class EventHandler extends Handler {
        public EventHandler(AudioPolicy ap, Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    AudioPolicy.this.onPolicyStatusChange();
                    break;
                case 1:
                    if (AudioPolicy.this.mFocusListener != null) {
                        AudioPolicy.this.mFocusListener.onAudioFocusGrant((AudioFocusInfo) msg.obj, msg.arg1);
                    }
                    break;
                case 2:
                    if (AudioPolicy.this.mFocusListener != null) {
                        AudioPolicy.this.mFocusListener.onAudioFocusLoss((AudioFocusInfo) msg.obj, msg.arg1 != 0);
                    }
                    break;
                default:
                    Log.e(AudioPolicy.TAG, "Unknown event " + msg.what);
                    break;
            }
        }
    }

    private static String addressForTag(AudioMix mix) {
        return "addr=" + mix.getRegistration();
    }

    private void sendMsg(int msg) {
        if (this.mEventHandler != null) {
            this.mEventHandler.sendEmptyMessage(msg);
        }
    }

    private void sendMsg(int msg, Object obj, int i) {
        if (this.mEventHandler != null) {
            this.mEventHandler.sendMessage(this.mEventHandler.obtainMessage(msg, i, 0, obj));
        }
    }

    private static IAudioService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
        sService = IAudioService.Stub.asInterface(b);
        return sService;
    }

    public String toLogFriendlyString() {
        String textDump = new String("android.media.audiopolicy.AudioPolicy:\n");
        return textDump + "config=" + this.mConfig.toLogFriendlyString();
    }
}
