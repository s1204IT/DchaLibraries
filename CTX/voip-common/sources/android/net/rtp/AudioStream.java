package android.net.rtp;

import java.net.InetAddress;
import java.net.SocketException;

public class AudioStream extends RtpStream {
    private AudioCodec mCodec;
    private int mDtmfType;
    private AudioGroup mGroup;

    public AudioStream(InetAddress inetAddress) throws SocketException {
        super(inetAddress);
        this.mDtmfType = -1;
    }

    @Override
    public final boolean isBusy() {
        return this.mGroup != null;
    }

    public AudioGroup getGroup() {
        return this.mGroup;
    }

    public void join(AudioGroup audioGroup) {
        synchronized (this) {
            if (this.mGroup == audioGroup) {
                return;
            }
            if (this.mGroup != null) {
                this.mGroup.remove(this);
                this.mGroup = null;
            }
            if (audioGroup != null) {
                audioGroup.add(this);
                this.mGroup = audioGroup;
            }
        }
    }

    public AudioCodec getCodec() {
        return this.mCodec;
    }

    public void setCodec(AudioCodec audioCodec) {
        if (isBusy()) {
            throw new IllegalStateException("Busy");
        }
        if (audioCodec.type == this.mDtmfType) {
            throw new IllegalArgumentException("The type is used by DTMF");
        }
        this.mCodec = audioCodec;
    }

    public int getDtmfType() {
        return this.mDtmfType;
    }

    public void setDtmfType(int i) {
        if (isBusy()) {
            throw new IllegalStateException("Busy");
        }
        if (i != -1) {
            if (i < 96 || i > 127) {
                throw new IllegalArgumentException("Invalid type");
            }
            if (this.mCodec != null && i == this.mCodec.type) {
                throw new IllegalArgumentException("The type is used by codec");
            }
        }
        this.mDtmfType = i;
    }
}
