package org.gsma.joyn.ipcall;

import java.util.HashSet;
import java.util.Set;
import org.gsma.joyn.ipcall.IIPCallRenderer;

public abstract class IPCallRenderer extends IIPCallRenderer.Stub {
    private Set<IPCallRendererListener> listeners = new HashSet();

    @Override
    public abstract void close();

    @Override
    public abstract AudioCodec getAudioCodec();

    @Override
    public abstract int getLocalAudioRtpPort();

    @Override
    public abstract int getLocalVideoRtpPort();

    @Override
    public abstract AudioCodec[] getSupportedAudioCodecs();

    @Override
    public abstract VideoCodec[] getSupportedVideoCodecs();

    @Override
    public abstract VideoCodec getVideoCodec();

    @Override
    public abstract void open(AudioCodec audioCodec, VideoCodec videoCodec, String str, int i, int i2);

    @Override
    public abstract void start();

    @Override
    public abstract void stop();

    public static class Error {
        public static final int INTERNAL_ERROR = 0;
        public static final int NETWORK_FAILURE = 1;

        private Error() {
        }
    }

    public Set<IPCallRendererListener> getEventListeners() {
        return this.listeners;
    }

    public void addEventListener(IPCallRendererListener listener) {
        this.listeners.add(listener);
    }

    public void removeEventListener(IPCallRendererListener listener) {
        this.listeners.remove(listener);
    }

    public void removeAllEventListeners() {
        this.listeners.clear();
    }
}
