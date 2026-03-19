package android.media.audiopolicy;

import android.Manifest;
import android.annotation.SystemApi;
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
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SystemApi
public class AudioPolicy {
    private static final boolean DEBUG = false;
    public static final int FOCUS_POLICY_DUCKING_DEFAULT = 0;

    @SystemApi
    public static final int FOCUS_POLICY_DUCKING_IN_APP = 0;

    @SystemApi
    public static final int FOCUS_POLICY_DUCKING_IN_POLICY = 1;
    private static final int MSG_FOCUS_ABANDON = 5;
    private static final int MSG_FOCUS_GRANT = 1;
    private static final int MSG_FOCUS_LOSS = 2;
    private static final int MSG_FOCUS_REQUEST = 4;
    private static final int MSG_MIX_STATE_UPDATE = 3;
    private static final int MSG_POLICY_STATUS_CHANGE = 0;
    private static final int MSG_VOL_ADJUST = 6;

    @SystemApi
    public static final int POLICY_STATUS_REGISTERED = 2;

    @SystemApi
    public static final int POLICY_STATUS_UNREGISTERED = 1;
    private static final String TAG = "AudioPolicy";
    private static IAudioService sService;
    private AudioPolicyConfig mConfig;
    private Context mContext;
    private final EventHandler mEventHandler;
    private AudioPolicyFocusListener mFocusListener;
    private boolean mIsFocusPolicy;
    private final Object mLock;
    private final IAudioPolicyCallback mPolicyCb;
    private String mRegistrationId;
    private int mStatus;
    private AudioPolicyStatusListener mStatusListener;
    private final AudioPolicyVolumeCallback mVolCb;

    @Retention(RetentionPolicy.SOURCE)
    public @interface PolicyStatus {
    }

    public AudioPolicyConfig getConfig() {
        return this.mConfig;
    }

    public boolean hasFocusListener() {
        return this.mFocusListener != null;
    }

    public boolean isFocusPolicy() {
        return this.mIsFocusPolicy;
    }

    public boolean isVolumeController() {
        return this.mVolCb != null;
    }

    private AudioPolicy(AudioPolicyConfig audioPolicyConfig, Context context, Looper looper, AudioPolicyFocusListener audioPolicyFocusListener, AudioPolicyStatusListener audioPolicyStatusListener, boolean z, AudioPolicyVolumeCallback audioPolicyVolumeCallback) {
        this.mLock = new Object();
        this.mPolicyCb = new IAudioPolicyCallback.Stub() {
            @Override
            public void notifyAudioFocusGrant(AudioFocusInfo audioFocusInfo, int i) {
                AudioPolicy.this.sendMsg(1, audioFocusInfo, i);
            }

            @Override
            public void notifyAudioFocusLoss(AudioFocusInfo audioFocusInfo, boolean z2) {
                AudioPolicy.this.sendMsg(2, audioFocusInfo, z2 ? 1 : 0);
            }

            @Override
            public void notifyAudioFocusRequest(AudioFocusInfo audioFocusInfo, int i) {
                AudioPolicy.this.sendMsg(4, audioFocusInfo, i);
            }

            @Override
            public void notifyAudioFocusAbandon(AudioFocusInfo audioFocusInfo) {
                AudioPolicy.this.sendMsg(5, audioFocusInfo, 0);
            }

            @Override
            public void notifyMixStateUpdate(String str, int i) {
                for (AudioMix audioMix : AudioPolicy.this.mConfig.getMixes()) {
                    if (audioMix.getRegistration().equals(str)) {
                        audioMix.mMixState = i;
                        AudioPolicy.this.sendMsg(3, audioMix, 0);
                    }
                }
            }

            @Override
            public void notifyVolumeAdjust(int i) {
                AudioPolicy.this.sendMsg(6, null, i);
            }
        };
        this.mConfig = audioPolicyConfig;
        this.mStatus = 1;
        this.mContext = context;
        looper = looper == null ? Looper.getMainLooper() : looper;
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            this.mEventHandler = null;
            Log.e(TAG, "No event handler due to looper without a thread");
        }
        this.mFocusListener = audioPolicyFocusListener;
        this.mStatusListener = audioPolicyStatusListener;
        this.mIsFocusPolicy = z;
        this.mVolCb = audioPolicyVolumeCallback;
    }

    @SystemApi
    public static class Builder {
        private Context mContext;
        private AudioPolicyFocusListener mFocusListener;
        private Looper mLooper;
        private AudioPolicyStatusListener mStatusListener;
        private AudioPolicyVolumeCallback mVolCb;
        private boolean mIsFocusPolicy = false;
        private ArrayList<AudioMix> mMixes = new ArrayList<>();

        @SystemApi
        public Builder(Context context) {
            this.mContext = context;
        }

        @SystemApi
        public Builder addMix(AudioMix audioMix) throws IllegalArgumentException {
            if (audioMix == null) {
                throw new IllegalArgumentException("Illegal null AudioMix argument");
            }
            this.mMixes.add(audioMix);
            return this;
        }

        @SystemApi
        public Builder setLooper(Looper looper) throws IllegalArgumentException {
            if (looper == null) {
                throw new IllegalArgumentException("Illegal null Looper argument");
            }
            this.mLooper = looper;
            return this;
        }

        @SystemApi
        public void setAudioPolicyFocusListener(AudioPolicyFocusListener audioPolicyFocusListener) {
            this.mFocusListener = audioPolicyFocusListener;
        }

        @SystemApi
        public Builder setIsAudioFocusPolicy(boolean z) {
            this.mIsFocusPolicy = z;
            return this;
        }

        @SystemApi
        public void setAudioPolicyStatusListener(AudioPolicyStatusListener audioPolicyStatusListener) {
            this.mStatusListener = audioPolicyStatusListener;
        }

        @SystemApi
        public Builder setAudioPolicyVolumeCallback(AudioPolicyVolumeCallback audioPolicyVolumeCallback) {
            if (audioPolicyVolumeCallback == null) {
                throw new IllegalArgumentException("Invalid null volume callback");
            }
            this.mVolCb = audioPolicyVolumeCallback;
            return this;
        }

        @SystemApi
        public AudioPolicy build() {
            if (this.mStatusListener != null) {
                Iterator<AudioMix> it = this.mMixes.iterator();
                while (it.hasNext()) {
                    it.next().mCallbackFlags |= 1;
                }
            }
            if (this.mIsFocusPolicy && this.mFocusListener == null) {
                throw new IllegalStateException("Cannot be a focus policy without an AudioPolicyFocusListener");
            }
            return new AudioPolicy(new AudioPolicyConfig(this.mMixes), this.mContext, this.mLooper, this.mFocusListener, this.mStatusListener, this.mIsFocusPolicy, this.mVolCb);
        }
    }

    @SystemApi
    public int attachMixes(List<AudioMix> list) {
        int iAddMixForPolicy;
        if (list == null) {
            throw new IllegalArgumentException("Illegal null list of AudioMix");
        }
        synchronized (this.mLock) {
            if (this.mStatus != 2) {
                throw new IllegalStateException("Cannot alter unregistered AudioPolicy");
            }
            ArrayList<AudioMix> arrayList = new ArrayList<>(list.size());
            for (AudioMix audioMix : list) {
                if (audioMix == null) {
                    throw new IllegalArgumentException("Illegal null AudioMix in attachMixes");
                }
                arrayList.add(audioMix);
            }
            AudioPolicyConfig audioPolicyConfig = new AudioPolicyConfig(arrayList);
            try {
                iAddMixForPolicy = getService().addMixForPolicy(audioPolicyConfig, cb());
                if (iAddMixForPolicy == 0) {
                    this.mConfig.add(arrayList);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Dead object in attachMixes", e);
                return -1;
            }
        }
        return iAddMixForPolicy;
    }

    @SystemApi
    public int detachMixes(List<AudioMix> list) {
        int iRemoveMixForPolicy;
        if (list == null) {
            throw new IllegalArgumentException("Illegal null list of AudioMix");
        }
        synchronized (this.mLock) {
            if (this.mStatus != 2) {
                throw new IllegalStateException("Cannot alter unregistered AudioPolicy");
            }
            ArrayList<AudioMix> arrayList = new ArrayList<>(list.size());
            for (AudioMix audioMix : list) {
                if (audioMix == null) {
                    throw new IllegalArgumentException("Illegal null AudioMix in detachMixes");
                }
                arrayList.add(audioMix);
            }
            AudioPolicyConfig audioPolicyConfig = new AudioPolicyConfig(arrayList);
            try {
                iRemoveMixForPolicy = getService().removeMixForPolicy(audioPolicyConfig, cb());
                if (iRemoveMixForPolicy == 0) {
                    this.mConfig.remove(arrayList);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Dead object in detachMixes", e);
                return -1;
            }
        }
        return iRemoveMixForPolicy;
    }

    public void setRegistration(String str) {
        synchronized (this.mLock) {
            this.mRegistrationId = str;
            this.mConfig.setRegistration(str);
            if (str != null) {
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

    private void checkMixReadyToUse(AudioMix audioMix, boolean z) throws IllegalArgumentException {
        if (audioMix == null) {
            throw new IllegalArgumentException(z ? "Invalid null AudioMix for AudioTrack creation" : "Invalid null AudioMix for AudioRecord creation");
        }
        if (!this.mConfig.mMixes.contains(audioMix)) {
            throw new IllegalArgumentException("Invalid mix: not part of this policy");
        }
        if ((audioMix.getRouteFlags() & 2) != 2) {
            throw new IllegalArgumentException("Invalid AudioMix: not defined for loop back");
        }
        if (z && audioMix.getMixType() != 1) {
            throw new IllegalArgumentException("Invalid AudioMix: not defined for being a recording source");
        }
        if (!z && audioMix.getMixType() != 0) {
            throw new IllegalArgumentException("Invalid AudioMix: not defined for capturing playback");
        }
    }

    @SystemApi
    public int getFocusDuckingBehavior() {
        return this.mConfig.mDuckingPolicy;
    }

    @SystemApi
    public int setFocusDuckingBehavior(int i) throws IllegalStateException, IllegalArgumentException {
        int focusPropertiesForPolicy;
        if (i != 0 && i != 1) {
            throw new IllegalArgumentException("Invalid ducking behavior " + i);
        }
        synchronized (this.mLock) {
            if (this.mStatus != 2) {
                throw new IllegalStateException("Cannot change ducking behavior for unregistered policy");
            }
            if (i == 1 && this.mFocusListener == null) {
                throw new IllegalStateException("Cannot handle ducking without an audio focus listener");
            }
            try {
                focusPropertiesForPolicy = getService().setFocusPropertiesForPolicy(i, cb());
                if (focusPropertiesForPolicy == 0) {
                    this.mConfig.mDuckingPolicy = i;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Dead object in setFocusPropertiesForPolicy for behavior", e);
                return -1;
            }
        }
        return focusPropertiesForPolicy;
    }

    @SystemApi
    public AudioRecord createAudioRecordSink(AudioMix audioMix) throws IllegalArgumentException {
        if (!policyReadyToUse()) {
            Log.e(TAG, "Cannot create AudioRecord sink for AudioMix");
            return null;
        }
        checkMixReadyToUse(audioMix, false);
        return new AudioRecord(new AudioAttributes.Builder().setInternalCapturePreset(8).addTag(addressForTag(audioMix)).addTag(AudioRecord.SUBMIX_FIXED_VOLUME).build(), new AudioFormat.Builder(audioMix.getFormat()).setChannelMask(AudioFormat.inChannelMaskFromOutChannelMask(audioMix.getFormat().getChannelMask())).build(), AudioRecord.getMinBufferSize(audioMix.getFormat().getSampleRate(), 12, audioMix.getFormat().getEncoding()), 0);
    }

    @SystemApi
    public AudioTrack createAudioTrackSource(AudioMix audioMix) throws IllegalArgumentException {
        if (!policyReadyToUse()) {
            Log.e(TAG, "Cannot create AudioTrack source for AudioMix");
            return null;
        }
        checkMixReadyToUse(audioMix, true);
        return new AudioTrack(new AudioAttributes.Builder().setUsage(15).addTag(addressForTag(audioMix)).build(), audioMix.getFormat(), AudioTrack.getMinBufferSize(audioMix.getFormat().getSampleRate(), audioMix.getFormat().getChannelMask(), audioMix.getFormat().getEncoding()), 1, 0);
    }

    @SystemApi
    public int getStatus() {
        return this.mStatus;
    }

    @SystemApi
    public static abstract class AudioPolicyStatusListener {
        public void onStatusChange() {
        }

        public void onMixStateUpdate(AudioMix audioMix) {
        }
    }

    @SystemApi
    public static abstract class AudioPolicyFocusListener {
        public void onAudioFocusGrant(AudioFocusInfo audioFocusInfo, int i) {
        }

        public void onAudioFocusLoss(AudioFocusInfo audioFocusInfo, boolean z) {
        }

        public void onAudioFocusRequest(AudioFocusInfo audioFocusInfo, int i) {
        }

        public void onAudioFocusAbandon(AudioFocusInfo audioFocusInfo) {
        }
    }

    @SystemApi
    public static abstract class AudioPolicyVolumeCallback {
        public void onVolumeAdjustment(int i) {
        }
    }

    private void onPolicyStatusChange() {
        synchronized (this.mLock) {
            if (this.mStatusListener == null) {
                return;
            }
            this.mStatusListener.onStatusChange();
        }
    }

    public IAudioPolicyCallback cb() {
        return this.mPolicyCb;
    }

    private class EventHandler extends Handler {
        public EventHandler(AudioPolicy audioPolicy, Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    AudioPolicy.this.onPolicyStatusChange();
                    break;
                case 1:
                    if (AudioPolicy.this.mFocusListener != null) {
                        AudioPolicy.this.mFocusListener.onAudioFocusGrant((AudioFocusInfo) message.obj, message.arg1);
                    }
                    break;
                case 2:
                    if (AudioPolicy.this.mFocusListener != null) {
                        AudioPolicy.this.mFocusListener.onAudioFocusLoss((AudioFocusInfo) message.obj, message.arg1 != 0);
                    }
                    break;
                case 3:
                    if (AudioPolicy.this.mStatusListener != null) {
                        AudioPolicy.this.mStatusListener.onMixStateUpdate((AudioMix) message.obj);
                    }
                    break;
                case 4:
                    if (AudioPolicy.this.mFocusListener != null) {
                        AudioPolicy.this.mFocusListener.onAudioFocusRequest((AudioFocusInfo) message.obj, message.arg1);
                    } else {
                        Log.e(AudioPolicy.TAG, "Invalid null focus listener for focus request event");
                    }
                    break;
                case 5:
                    if (AudioPolicy.this.mFocusListener != null) {
                        AudioPolicy.this.mFocusListener.onAudioFocusAbandon((AudioFocusInfo) message.obj);
                    } else {
                        Log.e(AudioPolicy.TAG, "Invalid null focus listener for focus abandon event");
                    }
                    break;
                case 6:
                    if (AudioPolicy.this.mVolCb != null) {
                        AudioPolicy.this.mVolCb.onVolumeAdjustment(message.arg1);
                    } else {
                        Log.e(AudioPolicy.TAG, "Invalid null volume event");
                    }
                    break;
                default:
                    Log.e(AudioPolicy.TAG, "Unknown event " + message.what);
                    break;
            }
        }
    }

    private static String addressForTag(AudioMix audioMix) {
        return "addr=" + audioMix.getRegistration();
    }

    private void sendMsg(int i) {
        if (this.mEventHandler != null) {
            this.mEventHandler.sendEmptyMessage(i);
        }
    }

    private void sendMsg(int i, Object obj, int i2) {
        if (this.mEventHandler != null) {
            this.mEventHandler.sendMessage(this.mEventHandler.obtainMessage(i, i2, 0, obj));
        }
    }

    private static IAudioService getService() {
        if (sService != null) {
            return sService;
        }
        sService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        return sService;
    }

    public String toLogFriendlyString() {
        return new String("android.media.audiopolicy.AudioPolicy:\n") + "config=" + this.mConfig.toLogFriendlyString();
    }
}
