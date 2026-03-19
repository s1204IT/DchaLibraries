package org.gsma.joyn.vsh;

import java.util.HashSet;
import java.util.Set;
import org.gsma.joyn.vsh.IVideoPlayer;

public abstract class VideoPlayer extends IVideoPlayer.Stub {
    private Set<IVideoPlayerListener> listeners = new HashSet();

    @Override
    public abstract void close();

    @Override
    public abstract VideoCodec getCodec();

    @Override
    public abstract int getLocalRtpPort();

    @Override
    public abstract VideoCodec[] getSupportedCodecs();

    @Override
    public abstract void open(VideoCodec videoCodec, String str, int i);

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

    public Set<IVideoPlayerListener> getEventListeners() {
        return this.listeners;
    }

    @Override
    public void addEventListener(IVideoPlayerListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeEventListener(IVideoPlayerListener listener) {
        this.listeners.remove(listener);
    }

    public void removeAllEventListeners() {
        this.listeners.clear();
    }
}
