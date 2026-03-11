package com.android.systemui.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import java.lang.Thread;
import java.util.LinkedList;

public class NotificationPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private AudioManager mAudioManagerWithAudioFocus;
    private CreationAndCompletionThread mCompletionThread;
    private Looper mLooper;
    private MediaPlayer mPlayer;
    private String mTag;
    private CmdThread mThread;
    private PowerManager.WakeLock mWakeLock;
    private LinkedList<Command> mCmdQueue = new LinkedList<>();
    private final Object mCompletionHandlingLock = new Object();
    private final Object mQueueAudioFocusLock = new Object();
    private int mState = 2;

    private static final class Command {
        AudioAttributes attributes;
        int code;
        Context context;
        boolean looping;
        long requestTime;
        Uri uri;

        Command(Command command) {
            this();
        }

        private Command() {
        }

        public String toString() {
            return "{ code=" + this.code + " looping=" + this.looping + " attributes=" + this.attributes + " uri=" + this.uri + " }";
        }
    }

    private final class CreationAndCompletionThread extends Thread {
        public Command mCmd;

        public CreationAndCompletionThread(Command cmd) {
            this.mCmd = cmd;
        }

        @Override
        public void run() {
            Looper.prepare();
            NotificationPlayer.this.mLooper = Looper.myLooper();
            synchronized (this) {
                AudioManager audioManager = (AudioManager) this.mCmd.context.getSystemService("audio");
                try {
                    MediaPlayer player = new MediaPlayer();
                    player.setAudioAttributes(this.mCmd.attributes);
                    player.setDataSource(this.mCmd.context, this.mCmd.uri);
                    player.setLooping(this.mCmd.looping);
                    player.prepare();
                    if (this.mCmd.uri != null && this.mCmd.uri.getEncodedPath() != null && this.mCmd.uri.getEncodedPath().length() > 0 && !audioManager.isMusicActiveRemotely()) {
                        synchronized (NotificationPlayer.this.mQueueAudioFocusLock) {
                            if (NotificationPlayer.this.mAudioManagerWithAudioFocus == null) {
                                if (this.mCmd.looping) {
                                    audioManager.requestAudioFocus(null, AudioAttributes.toLegacyStreamType(this.mCmd.attributes), 1);
                                } else {
                                    audioManager.requestAudioFocus(null, AudioAttributes.toLegacyStreamType(this.mCmd.attributes), 3);
                                }
                                NotificationPlayer.this.mAudioManagerWithAudioFocus = audioManager;
                            }
                        }
                    }
                    player.setOnCompletionListener(NotificationPlayer.this);
                    player.setOnErrorListener(NotificationPlayer.this);
                    player.start();
                    if (NotificationPlayer.this.mPlayer != null) {
                        NotificationPlayer.this.mPlayer.release();
                    }
                    NotificationPlayer.this.mPlayer = player;
                } catch (Exception e) {
                    Log.w(NotificationPlayer.this.mTag, "error loading sound for " + this.mCmd.uri, e);
                }
                notify();
            }
            Looper.loop();
        }
    }

    public void startSound(Command cmd) {
        try {
            synchronized (this.mCompletionHandlingLock) {
                if (this.mLooper != null && this.mLooper.getThread().getState() != Thread.State.TERMINATED) {
                    this.mLooper.quit();
                }
                this.mCompletionThread = new CreationAndCompletionThread(cmd);
                synchronized (this.mCompletionThread) {
                    this.mCompletionThread.start();
                    this.mCompletionThread.wait();
                }
            }
            long delay = SystemClock.uptimeMillis() - cmd.requestTime;
            if (delay <= 1000) {
                return;
            }
            Log.w(this.mTag, "Notification sound delayed by " + delay + "msecs");
        } catch (Exception e) {
            Log.w(this.mTag, "error loading sound for " + cmd.uri, e);
        }
    }

    private final class CmdThread extends Thread {
        CmdThread() {
            super("NotificationPlayer-" + NotificationPlayer.this.mTag);
        }

        @Override
        public void run() {
            Command cmd;
            while (true) {
                synchronized (NotificationPlayer.this.mCmdQueue) {
                    cmd = (Command) NotificationPlayer.this.mCmdQueue.removeFirst();
                }
                switch (cmd.code) {
                    case 1:
                        NotificationPlayer.this.startSound(cmd);
                        break;
                    case 2:
                        if (NotificationPlayer.this.mPlayer != null) {
                            long delay = SystemClock.uptimeMillis() - cmd.requestTime;
                            if (delay > 1000) {
                                Log.w(NotificationPlayer.this.mTag, "Notification stop delayed by " + delay + "msecs");
                            }
                            NotificationPlayer.this.mPlayer.stop();
                            NotificationPlayer.this.mPlayer.release();
                            NotificationPlayer.this.mPlayer = null;
                            synchronized (NotificationPlayer.this.mQueueAudioFocusLock) {
                                if (NotificationPlayer.this.mAudioManagerWithAudioFocus != null) {
                                    NotificationPlayer.this.mAudioManagerWithAudioFocus.abandonAudioFocus(null);
                                    NotificationPlayer.this.mAudioManagerWithAudioFocus = null;
                                }
                            }
                            if (NotificationPlayer.this.mLooper != null && NotificationPlayer.this.mLooper.getThread().getState() != Thread.State.TERMINATED) {
                                NotificationPlayer.this.mLooper.quit();
                            }
                        } else {
                            Log.w(NotificationPlayer.this.mTag, "STOP command without a player");
                        }
                        break;
                }
                synchronized (NotificationPlayer.this.mCmdQueue) {
                    if (NotificationPlayer.this.mCmdQueue.size() == 0) {
                        NotificationPlayer.this.mThread = null;
                        NotificationPlayer.this.releaseWakeLock();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        synchronized (this.mQueueAudioFocusLock) {
            if (this.mAudioManagerWithAudioFocus != null) {
                this.mAudioManagerWithAudioFocus.abandonAudioFocus(null);
                this.mAudioManagerWithAudioFocus = null;
            }
        }
        synchronized (this.mCmdQueue) {
            if (this.mCmdQueue.size() == 0) {
                synchronized (this.mCompletionHandlingLock) {
                    if (this.mLooper != null) {
                        this.mLooper.quit();
                    }
                    this.mCompletionThread = null;
                }
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(this.mTag, "error " + what + " (extra=" + extra + ") playing notification");
        onCompletion(mp);
        return true;
    }

    public NotificationPlayer(String tag) {
        if (tag != null) {
            this.mTag = tag;
        } else {
            this.mTag = "NotificationPlayer";
        }
    }

    public void play(Context context, Uri uri, boolean looping, AudioAttributes attributes) {
        Command cmd = new Command(null);
        cmd.requestTime = SystemClock.uptimeMillis();
        cmd.code = 1;
        cmd.context = context;
        cmd.uri = uri;
        cmd.looping = looping;
        cmd.attributes = attributes;
        synchronized (this.mCmdQueue) {
            enqueueLocked(cmd);
            this.mState = 1;
        }
    }

    public void stop() {
        synchronized (this.mCmdQueue) {
            if (this.mState != 2) {
                Command cmd = new Command(null);
                cmd.requestTime = SystemClock.uptimeMillis();
                cmd.code = 2;
                enqueueLocked(cmd);
                this.mState = 2;
            }
        }
    }

    private void enqueueLocked(Command cmd) {
        this.mCmdQueue.add(cmd);
        if (this.mThread != null) {
            return;
        }
        acquireWakeLock();
        this.mThread = new CmdThread();
        this.mThread.start();
    }

    public void setUsesWakeLock(Context context) {
        if (this.mWakeLock != null || this.mThread != null) {
            throw new RuntimeException("assertion failed mWakeLock=" + this.mWakeLock + " mThread=" + this.mThread);
        }
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, this.mTag);
    }

    private void acquireWakeLock() {
        if (this.mWakeLock == null) {
            return;
        }
        this.mWakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (this.mWakeLock == null) {
            return;
        }
        this.mWakeLock.release();
    }
}
