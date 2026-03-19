package android.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import java.util.LinkedList;

public class AsyncPlayer {
    private static final int PLAY = 1;
    private static final int STOP = 2;
    private static final boolean mDebug = false;
    private MediaPlayer mPlayer;
    private String mTag;
    private Thread mThread;
    private PowerManager.WakeLock mWakeLock;
    private final LinkedList<Command> mCmdQueue = new LinkedList<>();
    private int mState = 2;

    private static final class Command {
        AudioAttributes attributes;
        int code;
        Context context;
        boolean looping;
        long requestTime;
        Uri uri;

        private Command() {
        }

        public String toString() {
            return "{ code=" + this.code + " looping=" + this.looping + " attr=" + this.attributes + " uri=" + this.uri + " }";
        }
    }

    private void startSound(Command command) {
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(command.attributes);
            mediaPlayer.setDataSource(command.context, command.uri);
            mediaPlayer.setLooping(command.looping);
            mediaPlayer.prepare();
            mediaPlayer.start();
            if (this.mPlayer != null) {
                this.mPlayer.release();
            }
            this.mPlayer = mediaPlayer;
            long jUptimeMillis = SystemClock.uptimeMillis() - command.requestTime;
            if (jUptimeMillis > 1000) {
                Log.w(this.mTag, "Notification sound delayed by " + jUptimeMillis + "msecs");
            }
        } catch (Exception e) {
            Log.w(this.mTag, "error loading sound for " + command.uri, e);
        }
    }

    private final class Thread extends java.lang.Thread {
        Thread() {
            super("AsyncPlayer-" + AsyncPlayer.this.mTag);
        }

        @Override
        public void run() {
            Command command;
            while (true) {
                synchronized (AsyncPlayer.this.mCmdQueue) {
                    command = (Command) AsyncPlayer.this.mCmdQueue.removeFirst();
                }
                switch (command.code) {
                    case 1:
                        AsyncPlayer.this.startSound(command);
                        break;
                    case 2:
                        if (AsyncPlayer.this.mPlayer == null) {
                            Log.w(AsyncPlayer.this.mTag, "STOP command without a player");
                        } else {
                            long jUptimeMillis = SystemClock.uptimeMillis() - command.requestTime;
                            if (jUptimeMillis > 1000) {
                                Log.w(AsyncPlayer.this.mTag, "Notification stop delayed by " + jUptimeMillis + "msecs");
                            }
                            AsyncPlayer.this.mPlayer.stop();
                            AsyncPlayer.this.mPlayer.release();
                            AsyncPlayer.this.mPlayer = null;
                        }
                        break;
                }
                synchronized (AsyncPlayer.this.mCmdQueue) {
                    if (AsyncPlayer.this.mCmdQueue.size() == 0) {
                        AsyncPlayer.this.mThread = null;
                        AsyncPlayer.this.releaseWakeLock();
                        return;
                    }
                }
            }
        }
    }

    public AsyncPlayer(String str) {
        if (str != null) {
            this.mTag = str;
        } else {
            this.mTag = "AsyncPlayer";
        }
    }

    public void play(Context context, Uri uri, boolean z, int i) {
        PlayerBase.deprecateStreamTypeForPlayback(i, "AsyncPlayer", "play()");
        if (context == null || uri == null) {
            return;
        }
        try {
            play(context, uri, z, new AudioAttributes.Builder().setInternalLegacyStreamType(i).build());
        } catch (IllegalArgumentException e) {
            Log.e(this.mTag, "Call to deprecated AsyncPlayer.play() method caused:", e);
        }
    }

    public void play(Context context, Uri uri, boolean z, AudioAttributes audioAttributes) throws IllegalArgumentException {
        if (context == null || uri == null || audioAttributes == null) {
            throw new IllegalArgumentException("Illegal null AsyncPlayer.play() argument");
        }
        Command command = new Command();
        command.requestTime = SystemClock.uptimeMillis();
        command.code = 1;
        command.context = context;
        command.uri = uri;
        command.looping = z;
        command.attributes = audioAttributes;
        synchronized (this.mCmdQueue) {
            enqueueLocked(command);
            this.mState = 1;
        }
    }

    public void stop() {
        synchronized (this.mCmdQueue) {
            if (this.mState != 2) {
                Command command = new Command();
                command.requestTime = SystemClock.uptimeMillis();
                command.code = 2;
                enqueueLocked(command);
                this.mState = 2;
            }
        }
    }

    private void enqueueLocked(Command command) {
        this.mCmdQueue.add(command);
        if (this.mThread == null) {
            acquireWakeLock();
            this.mThread = new Thread();
            this.mThread.start();
        }
    }

    public void setUsesWakeLock(Context context) {
        if (this.mWakeLock != null || this.mThread != null) {
            throw new RuntimeException("assertion failed mWakeLock=" + this.mWakeLock + " mThread=" + this.mThread);
        }
        this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(1, this.mTag);
    }

    private void acquireWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
    }
}
