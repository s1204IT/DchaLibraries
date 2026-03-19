package android.net.sip;

import android.content.Context;
import android.media.AudioManager;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.sip.SimpleSessionDescription;
import android.net.sip.SipSession;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.telephony.Rlog;
import android.text.TextUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SipAudioCall {
    private static final boolean DBG = true;
    private static final boolean DONT_RELEASE_SOCKET = false;
    private static final String LOG_TAG = SipAudioCall.class.getSimpleName();
    private static final boolean RELEASE_SOCKET = true;
    private static final int SESSION_TIMEOUT = 5;
    private static final int TRANSFER_TIMEOUT = 15;
    private AudioGroup mAudioGroup;
    private AudioStream mAudioStream;
    private Context mContext;
    private String mErrorMessage;
    private Listener mListener;
    private SipProfile mLocalProfile;
    private String mPeerSd;
    private SipSession mSipSession;
    private SipSession mTransferringSession;
    private WifiManager.WifiLock mWifiHighPerfLock;
    private WifiManager mWm;
    private long mSessionId = System.currentTimeMillis();
    private boolean mInCall = DONT_RELEASE_SOCKET;
    private boolean mMuted = DONT_RELEASE_SOCKET;
    private boolean mHold = DONT_RELEASE_SOCKET;
    private int mErrorCode = 0;

    public static class Listener {
        public void onReadyToCall(SipAudioCall sipAudioCall) {
            onChanged(sipAudioCall);
        }

        public void onCalling(SipAudioCall sipAudioCall) {
            onChanged(sipAudioCall);
        }

        public void onRinging(SipAudioCall sipAudioCall, SipProfile sipProfile) {
            onChanged(sipAudioCall);
        }

        public void onRingingBack(SipAudioCall sipAudioCall) {
            onChanged(sipAudioCall);
        }

        public void onCallEstablished(SipAudioCall sipAudioCall) {
            onChanged(sipAudioCall);
        }

        public void onCallEnded(SipAudioCall sipAudioCall) {
            onChanged(sipAudioCall);
        }

        public void onCallBusy(SipAudioCall sipAudioCall) {
            onChanged(sipAudioCall);
        }

        public void onCallHeld(SipAudioCall sipAudioCall) {
            onChanged(sipAudioCall);
        }

        public void onError(SipAudioCall sipAudioCall, int i, String str) {
        }

        public void onChanged(SipAudioCall sipAudioCall) {
        }
    }

    public SipAudioCall(Context context, SipProfile sipProfile) {
        this.mContext = context;
        this.mLocalProfile = sipProfile;
        this.mWm = (WifiManager) context.getSystemService("wifi");
    }

    public void setListener(Listener listener) {
        setListener(listener, DONT_RELEASE_SOCKET);
    }

    public void setListener(Listener listener, boolean z) {
        this.mListener = listener;
        if (listener != null && z) {
            try {
                if (this.mErrorCode != 0) {
                    listener.onError(this, this.mErrorCode, this.mErrorMessage);
                } else if (this.mInCall) {
                    if (this.mHold) {
                        listener.onCallHeld(this);
                    } else {
                        listener.onCallEstablished(this);
                    }
                } else {
                    int state = getState();
                    if (state == 0) {
                        listener.onReadyToCall(this);
                    } else if (state == 3) {
                        listener.onRinging(this, getPeerProfile());
                    } else {
                        switch (state) {
                            case 5:
                                listener.onCalling(this);
                                break;
                            case SipSession.State.OUTGOING_CALL_RING_BACK:
                                listener.onRingingBack(this);
                                break;
                        }
                    }
                }
            } catch (Throwable th) {
                loge("setListener()", th);
            }
        }
    }

    public boolean isInCall() {
        boolean z;
        synchronized (this) {
            z = this.mInCall;
        }
        return z;
    }

    public boolean isOnHold() {
        boolean z;
        synchronized (this) {
            z = this.mHold;
        }
        return z;
    }

    public void close() {
        close(true);
    }

    private synchronized void close(boolean z) {
        if (z) {
            try {
                stopCall(true);
            } catch (Throwable th) {
                throw th;
            }
        }
        this.mInCall = DONT_RELEASE_SOCKET;
        this.mHold = DONT_RELEASE_SOCKET;
        this.mSessionId = System.currentTimeMillis();
        this.mErrorCode = 0;
        this.mErrorMessage = null;
        if (this.mSipSession != null) {
            this.mSipSession.setListener(null);
            this.mSipSession = null;
        }
    }

    public SipProfile getLocalProfile() {
        SipProfile sipProfile;
        synchronized (this) {
            sipProfile = this.mLocalProfile;
        }
        return sipProfile;
    }

    public SipProfile getPeerProfile() {
        SipProfile peerProfile;
        synchronized (this) {
            peerProfile = this.mSipSession == null ? null : this.mSipSession.getPeerProfile();
        }
        return peerProfile;
    }

    public int getState() {
        synchronized (this) {
            if (this.mSipSession == null) {
                return 0;
            }
            return this.mSipSession.getState();
        }
    }

    public SipSession getSipSession() {
        SipSession sipSession;
        synchronized (this) {
            sipSession = this.mSipSession;
        }
        return sipSession;
    }

    private synchronized void transferToNewSession() {
        if (this.mTransferringSession == null) {
            return;
        }
        SipSession sipSession = this.mSipSession;
        this.mSipSession = this.mTransferringSession;
        this.mTransferringSession = null;
        if (this.mAudioStream != null) {
            this.mAudioStream.join(null);
        } else {
            try {
                this.mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
            } catch (Throwable th) {
                loge("transferToNewSession():", th);
            }
        }
        if (sipSession != null) {
            sipSession.endCall();
        }
        startAudio();
    }

    private SipSession.Listener createListener() {
        return new SipSession.Listener() {
            @Override
            public void onCalling(SipSession sipSession) {
                SipAudioCall.this.log("onCalling: session=" + sipSession);
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onCalling(SipAudioCall.this);
                    } catch (Throwable th) {
                        SipAudioCall.this.loge("onCalling():", th);
                    }
                }
            }

            @Override
            public void onRingingBack(SipSession sipSession) {
                SipAudioCall.this.log("onRingingBackk: " + sipSession);
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onRingingBack(SipAudioCall.this);
                    } catch (Throwable th) {
                        SipAudioCall.this.loge("onRingingBack():", th);
                    }
                }
            }

            @Override
            public void onRinging(SipSession sipSession, SipProfile sipProfile, String str) {
                synchronized (SipAudioCall.this) {
                    if (SipAudioCall.this.mSipSession != null && SipAudioCall.this.mInCall && sipSession.getCallId().equals(SipAudioCall.this.mSipSession.getCallId())) {
                        try {
                            SipAudioCall.this.mSipSession.answerCall(SipAudioCall.this.createAnswer(str).encode(), 5);
                        } catch (Throwable th) {
                            SipAudioCall.this.loge("onRinging():", th);
                            sipSession.endCall();
                        }
                        return;
                    }
                    sipSession.endCall();
                }
            }

            @Override
            public void onCallEstablished(SipSession sipSession, String str) {
                SipAudioCall.this.mPeerSd = str;
                SipAudioCall.this.log("onCallEstablished(): " + SipAudioCall.this.mPeerSd);
                if (SipAudioCall.this.mTransferringSession == null || sipSession != SipAudioCall.this.mTransferringSession) {
                    Listener listener = SipAudioCall.this.mListener;
                    if (listener != null) {
                        try {
                            if (SipAudioCall.this.mHold) {
                                listener.onCallHeld(SipAudioCall.this);
                            } else {
                                listener.onCallEstablished(SipAudioCall.this);
                            }
                            return;
                        } catch (Throwable th) {
                            SipAudioCall.this.loge("onCallEstablished(): ", th);
                            return;
                        }
                    }
                    return;
                }
                SipAudioCall.this.transferToNewSession();
            }

            @Override
            public void onCallEnded(SipSession sipSession) {
                SipAudioCall.this.log("onCallEnded: " + sipSession + " mSipSession:" + SipAudioCall.this.mSipSession);
                if (sipSession == SipAudioCall.this.mTransferringSession) {
                    SipAudioCall.this.mTransferringSession = null;
                    return;
                }
                if (SipAudioCall.this.mTransferringSession == null && sipSession == SipAudioCall.this.mSipSession) {
                    Listener listener = SipAudioCall.this.mListener;
                    if (listener != null) {
                        try {
                            listener.onCallEnded(SipAudioCall.this);
                        } catch (Throwable th) {
                            SipAudioCall.this.loge("onCallEnded(): ", th);
                        }
                    }
                    SipAudioCall.this.close();
                }
            }

            @Override
            public void onCallBusy(SipSession sipSession) {
                SipAudioCall.this.log("onCallBusy: " + sipSession);
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onCallBusy(SipAudioCall.this);
                    } catch (Throwable th) {
                        SipAudioCall.this.loge("onCallBusy(): ", th);
                    }
                }
                SipAudioCall.this.close(SipAudioCall.DONT_RELEASE_SOCKET);
            }

            @Override
            public void onCallChangeFailed(SipSession sipSession, int i, String str) {
                SipAudioCall.this.log("onCallChangedFailed: " + str);
                SipAudioCall.this.mErrorCode = i;
                SipAudioCall.this.mErrorMessage = str;
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onError(SipAudioCall.this, SipAudioCall.this.mErrorCode, str);
                    } catch (Throwable th) {
                        SipAudioCall.this.loge("onCallBusy():", th);
                    }
                }
            }

            @Override
            public void onError(SipSession sipSession, int i, String str) {
                SipAudioCall.this.onError(i, str);
            }

            @Override
            public void onRegistering(SipSession sipSession) {
            }

            @Override
            public void onRegistrationTimeout(SipSession sipSession) {
            }

            @Override
            public void onRegistrationFailed(SipSession sipSession, int i, String str) {
            }

            @Override
            public void onRegistrationDone(SipSession sipSession, int i) {
            }

            @Override
            public void onCallTransferring(SipSession sipSession, String str) {
                SipAudioCall.this.log("onCallTransferring: mSipSession=" + SipAudioCall.this.mSipSession + " newSession=" + sipSession);
                SipAudioCall.this.mTransferringSession = sipSession;
                try {
                    if (str == null) {
                        sipSession.makeCall(sipSession.getPeerProfile(), SipAudioCall.this.createOffer().encode(), SipAudioCall.TRANSFER_TIMEOUT);
                    } else {
                        sipSession.answerCall(SipAudioCall.this.createAnswer(str).encode(), 5);
                    }
                } catch (Throwable th) {
                    SipAudioCall.this.loge("onCallTransferring()", th);
                    sipSession.endCall();
                }
            }
        };
    }

    private void onError(int i, String str) {
        log("onError: " + SipErrorCode.toString(i) + ": " + str);
        this.mErrorCode = i;
        this.mErrorMessage = str;
        Listener listener = this.mListener;
        if (listener != null) {
            try {
                listener.onError(this, i, str);
            } catch (Throwable th) {
                loge("onError():", th);
            }
        }
        synchronized (this) {
            if (i != -10) {
                try {
                    if (!isInCall()) {
                        close(true);
                    }
                } catch (Throwable th2) {
                    throw th2;
                }
            } else {
                close(true);
            }
        }
    }

    public void attachCall(SipSession sipSession, String str) throws SipException {
        if (!SipManager.isVoipSupported(this.mContext)) {
            throw new SipException("VOIP API is not supported");
        }
        synchronized (this) {
            this.mSipSession = sipSession;
            this.mPeerSd = str;
            log("attachCall(): " + this.mPeerSd);
            try {
                sipSession.setListener(createListener());
            } catch (Throwable th) {
                loge("attachCall()", th);
                throwSipException(th);
            }
        }
    }

    public void makeCall(SipProfile sipProfile, SipSession sipSession, int i) throws SipException {
        log("makeCall: " + sipProfile + " session=" + sipSession + " timeout=" + i);
        if (!SipManager.isVoipSupported(this.mContext)) {
            throw new SipException("VOIP API is not supported");
        }
        synchronized (this) {
            this.mSipSession = sipSession;
            try {
                this.mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
                sipSession.setListener(createListener());
                sipSession.makeCall(sipProfile, createOffer().encode(), i);
            } catch (IOException e) {
                loge("makeCall:", e);
                throw new SipException("makeCall()", e);
            }
        }
    }

    public void endCall() throws SipException {
        log("endCall: mSipSession" + this.mSipSession);
        synchronized (this) {
            stopCall(true);
            this.mInCall = DONT_RELEASE_SOCKET;
            if (this.mSipSession != null) {
                this.mSipSession.endCall();
            }
        }
    }

    public void holdCall(int i) throws SipException {
        log("holdCall: mSipSession" + this.mSipSession + " timeout=" + i);
        synchronized (this) {
            if (this.mHold) {
                return;
            }
            if (this.mSipSession == null) {
                loge("holdCall:");
                throw new SipException("Not in a call to hold call");
            }
            this.mSipSession.changeCall(createHoldOffer().encode(), i);
            this.mHold = true;
            setAudioGroupMode();
        }
    }

    public void answerCall(int i) throws SipException {
        log("answerCall: mSipSession" + this.mSipSession + " timeout=" + i);
        synchronized (this) {
            if (this.mSipSession == null) {
                throw new SipException("No call to answer");
            }
            try {
                this.mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
                this.mSipSession.answerCall(createAnswer(this.mPeerSd).encode(), i);
            } catch (IOException e) {
                loge("answerCall:", e);
                throw new SipException("answerCall()", e);
            }
        }
    }

    public void continueCall(int i) throws SipException {
        log("continueCall: mSipSession" + this.mSipSession + " timeout=" + i);
        synchronized (this) {
            if (this.mHold) {
                this.mSipSession.changeCall(createContinueOffer().encode(), i);
                this.mHold = DONT_RELEASE_SOCKET;
                setAudioGroupMode();
            }
        }
    }

    private SimpleSessionDescription createOffer() {
        SimpleSessionDescription simpleSessionDescription = new SimpleSessionDescription(this.mSessionId, getLocalIp());
        AudioCodec.getCodecs();
        SimpleSessionDescription.Media mediaNewMedia = simpleSessionDescription.newMedia("audio", this.mAudioStream.getLocalPort(), 1, "RTP/AVP");
        for (AudioCodec audioCodec : AudioCodec.getCodecs()) {
            mediaNewMedia.setRtpPayload(audioCodec.type, audioCodec.rtpmap, audioCodec.fmtp);
        }
        mediaNewMedia.setRtpPayload(127, "telephone-event/8000", "0-15");
        log("createOffer: offer=" + simpleSessionDescription);
        return simpleSessionDescription;
    }

    private SimpleSessionDescription createAnswer(String str) {
        if (TextUtils.isEmpty(str)) {
            return createOffer();
        }
        SimpleSessionDescription simpleSessionDescription = new SimpleSessionDescription(str);
        SimpleSessionDescription simpleSessionDescription2 = new SimpleSessionDescription(this.mSessionId, getLocalIp());
        AudioCodec audioCodec = null;
        for (SimpleSessionDescription.Media media : simpleSessionDescription.getMedia()) {
            if (audioCodec == null && media.getPort() > 0 && "audio".equals(media.getType()) && "RTP/AVP".equals(media.getProtocol())) {
                AudioCodec codec = audioCodec;
                for (int i : media.getRtpPayloadTypes()) {
                    codec = AudioCodec.getCodec(i, media.getRtpmap(i), media.getFmtp(i));
                    if (codec != null) {
                        break;
                    }
                }
                audioCodec = codec;
                if (audioCodec != null) {
                    SimpleSessionDescription.Media mediaNewMedia = simpleSessionDescription2.newMedia("audio", this.mAudioStream.getLocalPort(), 1, "RTP/AVP");
                    mediaNewMedia.setRtpPayload(audioCodec.type, audioCodec.rtpmap, audioCodec.fmtp);
                    for (int i2 : media.getRtpPayloadTypes()) {
                        String rtpmap = media.getRtpmap(i2);
                        if (i2 != audioCodec.type && rtpmap != null && rtpmap.startsWith("telephone-event")) {
                            mediaNewMedia.setRtpPayload(i2, rtpmap, media.getFmtp(i2));
                        }
                    }
                    if (media.getAttribute("recvonly") != null) {
                        simpleSessionDescription2.setAttribute("sendonly", "");
                    } else if (media.getAttribute("sendonly") != null) {
                        simpleSessionDescription2.setAttribute("recvonly", "");
                    } else if (simpleSessionDescription.getAttribute("recvonly") != null) {
                        simpleSessionDescription2.setAttribute("sendonly", "");
                    } else if (simpleSessionDescription.getAttribute("sendonly") != null) {
                        simpleSessionDescription2.setAttribute("recvonly", "");
                    }
                }
            } else {
                SimpleSessionDescription.Media mediaNewMedia2 = simpleSessionDescription2.newMedia(media.getType(), 0, 1, media.getProtocol());
                for (String str2 : media.getFormats()) {
                    mediaNewMedia2.setFormat(str2, null);
                }
            }
        }
        if (audioCodec == null) {
            loge("createAnswer: no suitable codes");
            throw new IllegalStateException("Reject SDP: no suitable codecs");
        }
        log("createAnswer: answer=" + simpleSessionDescription2);
        return simpleSessionDescription2;
    }

    private SimpleSessionDescription createHoldOffer() {
        SimpleSessionDescription simpleSessionDescriptionCreateContinueOffer = createContinueOffer();
        simpleSessionDescriptionCreateContinueOffer.setAttribute("sendonly", "");
        log("createHoldOffer: offer=" + simpleSessionDescriptionCreateContinueOffer);
        return simpleSessionDescriptionCreateContinueOffer;
    }

    private SimpleSessionDescription createContinueOffer() {
        log("createContinueOffer");
        SimpleSessionDescription simpleSessionDescription = new SimpleSessionDescription(this.mSessionId, getLocalIp());
        SimpleSessionDescription.Media mediaNewMedia = simpleSessionDescription.newMedia("audio", this.mAudioStream.getLocalPort(), 1, "RTP/AVP");
        AudioCodec codec = this.mAudioStream.getCodec();
        mediaNewMedia.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
        int dtmfType = this.mAudioStream.getDtmfType();
        if (dtmfType != -1) {
            mediaNewMedia.setRtpPayload(dtmfType, "telephone-event/8000", "0-15");
        }
        return simpleSessionDescription;
    }

    private void grabWifiHighPerfLock() {
        if (this.mWifiHighPerfLock == null) {
            log("grabWifiHighPerfLock:");
            this.mWifiHighPerfLock = ((WifiManager) this.mContext.getSystemService("wifi")).createWifiLock(3, LOG_TAG);
            this.mWifiHighPerfLock.acquire();
        }
    }

    private void releaseWifiHighPerfLock() {
        if (this.mWifiHighPerfLock != null) {
            log("releaseWifiHighPerfLock:");
            this.mWifiHighPerfLock.release();
            this.mWifiHighPerfLock = null;
        }
    }

    private boolean isWifiOn() {
        if (this.mWm.getConnectionInfo().getBSSID() == null) {
            return DONT_RELEASE_SOCKET;
        }
        return true;
    }

    public void toggleMute() {
        synchronized (this) {
            this.mMuted = !this.mMuted;
            setAudioGroupMode();
        }
    }

    public boolean isMuted() {
        boolean z;
        synchronized (this) {
            z = this.mMuted;
        }
        return z;
    }

    public void setSpeakerMode(boolean z) {
        synchronized (this) {
            ((AudioManager) this.mContext.getSystemService("audio")).setSpeakerphoneOn(z);
            setAudioGroupMode();
        }
    }

    private boolean isSpeakerOn() {
        return ((AudioManager) this.mContext.getSystemService("audio")).isSpeakerphoneOn();
    }

    public void sendDtmf(int i) {
        sendDtmf(i, null);
    }

    public void sendDtmf(int i, Message message) {
        synchronized (this) {
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup != null && this.mSipSession != null && 8 == getState()) {
                log("sendDtmf: code=" + i + " result=" + message);
                audioGroup.sendDtmf(i);
            }
            if (message != null) {
                message.sendToTarget();
            }
        }
    }

    public AudioStream getAudioStream() {
        AudioStream audioStream;
        synchronized (this) {
            audioStream = this.mAudioStream;
        }
        return audioStream;
    }

    public AudioGroup getAudioGroup() {
        synchronized (this) {
            if (this.mAudioGroup != null) {
                return this.mAudioGroup;
            }
            return this.mAudioStream == null ? null : this.mAudioStream.getGroup();
        }
    }

    public void setAudioGroup(AudioGroup audioGroup) {
        synchronized (this) {
            log("setAudioGroup: group=" + audioGroup);
            if (this.mAudioStream != null && this.mAudioStream.getGroup() != null) {
                this.mAudioStream.join(audioGroup);
            }
            this.mAudioGroup = audioGroup;
        }
    }

    public void startAudio() {
        try {
            startAudioInternal();
        } catch (UnknownHostException e) {
            onError(-7, e.getMessage());
        } catch (Throwable th) {
            onError(-4, th.getMessage());
        }
    }

    private synchronized void startAudioInternal() throws UnknownHostException {
        loge("startAudioInternal: mPeerSd=" + this.mPeerSd);
        if (this.mPeerSd == null) {
            throw new IllegalStateException("mPeerSd = null");
        }
        stopCall(DONT_RELEASE_SOCKET);
        this.mInCall = true;
        SimpleSessionDescription simpleSessionDescription = new SimpleSessionDescription(this.mPeerSd);
        AudioStream audioStream = this.mAudioStream;
        SimpleSessionDescription.Media[] media = simpleSessionDescription.getMedia();
        int length = media.length;
        AudioCodec audioCodec = null;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            SimpleSessionDescription.Media media2 = media[i];
            if (audioCodec == null && media2.getPort() > 0 && "audio".equals(media2.getType()) && "RTP/AVP".equals(media2.getProtocol())) {
                AudioCodec codec = audioCodec;
                for (int i2 : media2.getRtpPayloadTypes()) {
                    codec = AudioCodec.getCodec(i2, media2.getRtpmap(i2), media2.getFmtp(i2));
                    if (codec != null) {
                        break;
                    }
                }
                audioCodec = codec;
                if (audioCodec != null) {
                    String address = media2.getAddress();
                    if (address == null) {
                        address = simpleSessionDescription.getAddress();
                    }
                    audioStream.associate(InetAddress.getByName(address), media2.getPort());
                    audioStream.setDtmfType(-1);
                    audioStream.setCodec(audioCodec);
                    for (int i3 : media2.getRtpPayloadTypes()) {
                        String rtpmap = media2.getRtpmap(i3);
                        if (i3 != audioCodec.type && rtpmap != null && rtpmap.startsWith("telephone-event")) {
                            audioStream.setDtmfType(i3);
                        }
                    }
                    if (this.mHold) {
                        audioStream.setMode(0);
                    } else if (media2.getAttribute("recvonly") != null) {
                        audioStream.setMode(1);
                    } else if (media2.getAttribute("sendonly") != null) {
                        audioStream.setMode(2);
                    } else if (simpleSessionDescription.getAttribute("recvonly") != null) {
                        audioStream.setMode(1);
                    } else if (simpleSessionDescription.getAttribute("sendonly") != null) {
                        audioStream.setMode(2);
                    } else {
                        audioStream.setMode(0);
                    }
                }
            }
            i++;
        }
        if (audioCodec == null) {
            throw new IllegalStateException("Reject SDP: no suitable codecs");
        }
        if (isWifiOn()) {
            grabWifiHighPerfLock();
        }
        AudioGroup audioGroup = getAudioGroup();
        if (!this.mHold) {
            if (audioGroup == null) {
                audioGroup = new AudioGroup();
            }
            audioStream.join(audioGroup);
        }
        setAudioGroupMode();
    }

    private void setAudioGroupMode() {
        AudioGroup audioGroup = getAudioGroup();
        log("setAudioGroupMode: audioGroup=" + audioGroup);
        if (audioGroup != null) {
            if (this.mHold) {
                audioGroup.setMode(0);
                return;
            }
            if (this.mMuted) {
                audioGroup.setMode(1);
            } else if (isSpeakerOn()) {
                audioGroup.setMode(3);
            } else {
                audioGroup.setMode(2);
            }
        }
    }

    private void stopCall(boolean z) {
        log("stopCall: releaseSocket=" + z);
        releaseWifiHighPerfLock();
        if (this.mAudioStream != null) {
            this.mAudioStream.join(null);
            if (z) {
                this.mAudioStream.release();
                this.mAudioStream = null;
            }
        }
    }

    private String getLocalIp() {
        return this.mSipSession.getLocalIp();
    }

    private void throwSipException(Throwable th) throws SipException {
        if (th instanceof SipException) {
            throw ((SipException) th);
        }
        throw new SipException("", th);
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private void loge(String str, Throwable th) {
        Rlog.e(LOG_TAG, str, th);
    }
}
