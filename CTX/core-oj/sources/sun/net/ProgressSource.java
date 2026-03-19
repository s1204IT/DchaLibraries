package sun.net;

import java.net.URL;

public class ProgressSource {
    private boolean connected;
    private String contentType;
    private long expected;
    private long lastProgress;
    private String method;
    private long progress;
    private ProgressMonitor progressMonitor;
    private State state;
    private int threshold;
    private URL url;

    public enum State {
        NEW,
        CONNECTED,
        UPDATE,
        DELETE
    }

    public ProgressSource(URL url, String str) {
        this(url, str, -1L);
    }

    public ProgressSource(URL url, String str, long j) {
        this.progress = 0L;
        this.lastProgress = 0L;
        this.expected = -1L;
        this.connected = false;
        this.threshold = 8192;
        this.url = url;
        this.method = str;
        this.contentType = "content/unknown";
        this.progress = 0L;
        this.lastProgress = 0L;
        this.expected = j;
        this.state = State.NEW;
        this.progressMonitor = ProgressMonitor.getDefault();
        this.threshold = this.progressMonitor.getProgressUpdateThreshold();
    }

    public boolean connected() {
        if (this.connected) {
            return true;
        }
        this.connected = true;
        this.state = State.CONNECTED;
        return false;
    }

    public void close() {
        this.state = State.DELETE;
    }

    public URL getURL() {
        return this.url;
    }

    public String getMethod() {
        return this.method;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String str) {
        this.contentType = str;
    }

    public long getProgress() {
        return this.progress;
    }

    public long getExpected() {
        return this.expected;
    }

    public State getState() {
        return this.state;
    }

    public void beginTracking() {
        this.progressMonitor.registerSource(this);
    }

    public void finishTracking() {
        this.progressMonitor.unregisterSource(this);
    }

    public void updateProgress(long j, long j2) {
        this.lastProgress = this.progress;
        this.progress = j;
        this.expected = j2;
        if (!connected()) {
            this.state = State.CONNECTED;
        } else {
            this.state = State.UPDATE;
        }
        if (this.lastProgress / ((long) this.threshold) != this.progress / ((long) this.threshold)) {
            this.progressMonitor.updateProgress(this);
        }
        if (this.expected != -1 && this.progress >= this.expected && this.progress != 0) {
            close();
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        return getClass().getName() + "[url=" + ((Object) this.url) + ", method=" + this.method + ", state=" + ((Object) this.state) + ", content-type=" + this.contentType + ", progress=" + this.progress + ", expected=" + this.expected + "]";
    }
}
