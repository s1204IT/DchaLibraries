package android.net.rtp;

import android.app.ActivityThread;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AudioGroup {
    public static final int MODE_ECHO_SUPPRESSION = 3;
    private static final int MODE_LAST = 3;
    public static final int MODE_MUTED = 1;
    public static final int MODE_NORMAL = 2;
    public static final int MODE_ON_HOLD = 0;
    private long mNative;
    private int mMode = 0;
    private final Map<AudioStream, Long> mStreams = new HashMap();

    private native long nativeAdd(int i, int i2, String str, int i3, String str2, int i4, String str3);

    private native void nativeRemove(long j);

    private native void nativeSendDtmf(int i);

    private native void nativeSetMode(int i);

    static {
        System.loadLibrary("rtp_jni");
    }

    public AudioStream[] getStreams() {
        AudioStream[] audioStreamArr;
        synchronized (this) {
            audioStreamArr = (AudioStream[]) this.mStreams.keySet().toArray(new AudioStream[this.mStreams.size()]);
        }
        return audioStreamArr;
    }

    public int getMode() {
        return this.mMode;
    }

    public void setMode(int i) {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("Invalid mode");
        }
        synchronized (this) {
            nativeSetMode(i);
            this.mMode = i;
        }
    }

    synchronized void add(AudioStream audioStream) {
        if (!this.mStreams.containsKey(audioStream)) {
            try {
                AudioCodec codec = audioStream.getCodec();
                this.mStreams.put(audioStream, Long.valueOf(nativeAdd(audioStream.getMode(), audioStream.getSocket(), audioStream.getRemoteAddress().getHostAddress(), audioStream.getRemotePort(), String.format(Locale.US, "%d %s %s", Integer.valueOf(codec.type), codec.rtpmap, codec.fmtp), audioStream.getDtmfType(), ActivityThread.currentOpPackageName())));
            } catch (NullPointerException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    synchronized void remove(AudioStream audioStream) {
        Long lRemove = this.mStreams.remove(audioStream);
        if (lRemove != null) {
            nativeRemove(lRemove.longValue());
        }
    }

    public void sendDtmf(int i) {
        if (i < 0 || i > 15) {
            throw new IllegalArgumentException("Invalid event");
        }
        synchronized (this) {
            nativeSendDtmf(i);
        }
    }

    public void clear() {
        for (AudioStream audioStream : getStreams()) {
            audioStream.join(null);
        }
    }

    protected void finalize() throws Throwable {
        nativeRemove(0L);
        super.finalize();
    }
}
