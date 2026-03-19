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
import com.android.internal.annotations.GuardedBy;
import java.lang.Thread;
import java.util.LinkedList;

public class NotificationPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    @GuardedBy("mQueueAudioFocusLock")
    private AudioManager mAudioManagerWithAudioFocus;

    @GuardedBy("mCompletionHandlingLock")
    private CreationAndCompletionThread mCompletionThread;

    @GuardedBy("mCompletionHandlingLock")
    private Looper mLooper;
    private MediaPlayer mPlayer;
    private String mTag;

    @GuardedBy("mCmdQueue")
    private CmdThread mThread;

    @GuardedBy("mCmdQueue")
    private PowerManager.WakeLock mWakeLock;
    private final LinkedList<Command> mCmdQueue = new LinkedList<>();
    private final Object mCompletionHandlingLock = new Object();
    private final Object mQueueAudioFocusLock = new Object();
    private int mNotificationRampTimeMs = 0;
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
            return "{ code=" + this.code + " looping=" + this.looping + " attributes=" + this.attributes + " uri=" + this.uri + " }";
        }
    }

    private final class CreationAndCompletionThread extends Thread {
        public Command mCmd;

        public CreationAndCompletionThread(Command command) {
            this.mCmd = command;
        }

        @Override
        public void run() {
            Looper.prepare();
            NotificationPlayer.this.mLooper = Looper.myLooper();
            synchronized (this) {
                AudioManager audioManager = (AudioManager) this.mCmd.context.getSystemService("audio");
                try {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    if (this.mCmd.attributes == null) {
                        this.mCmd.attributes = new AudioAttributes.Builder().setUsage(5).setContentType(4).build();
                    }
                    mediaPlayer.setAudioAttributes(this.mCmd.attributes);
                    mediaPlayer.setDataSource(this.mCmd.context, this.mCmd.uri);
                    mediaPlayer.setLooping(this.mCmd.looping);
                    mediaPlayer.setOnCompletionListener(NotificationPlayer.this);
                    mediaPlayer.setOnErrorListener(NotificationPlayer.this);
                    mediaPlayer.prepare();
                    if (this.mCmd.uri != null && this.mCmd.uri.getEncodedPath() != null && this.mCmd.uri.getEncodedPath().length() > 0 && !audioManager.isMusicActiveRemotely()) {
                        synchronized (NotificationPlayer.this.mQueueAudioFocusLock) {
                            if (NotificationPlayer.this.mAudioManagerWithAudioFocus == null) {
                                int i = 3;
                                if (this.mCmd.looping) {
                                    i = 1;
                                }
                                NotificationPlayer.this.mNotificationRampTimeMs = audioManager.getFocusRampTimeMs(i, this.mCmd.attributes);
                                audioManager.requestAudioFocus(null, this.mCmd.attributes, i, 0);
                                NotificationPlayer.this.mAudioManagerWithAudioFocus = audioManager;
                            }
                        }
                    }
                    try {
                        Thread.sleep(NotificationPlayer.this.mNotificationRampTimeMs);
                    } catch (InterruptedException e) {
                        Log.e(NotificationPlayer.this.mTag, "Exception while sleeping to sync notification playback with ducking", e);
                    }
                    try {
                        mediaPlayer.start();
                    } catch (Exception e2) {
                        mediaPlayer.release();
                        NotificationPlayer.this.abandonAudioFocusAfterError();
                        mediaPlayer = null;
                    }
                    if (NotificationPlayer.this.mPlayer != null) {
                        NotificationPlayer.this.mPlayer.release();
                    }
                    NotificationPlayer.this.mPlayer = mediaPlayer;
                } catch (Exception e3) {
                    Log.w(NotificationPlayer.this.mTag, "error loading sound for " + this.mCmd.uri, e3);
                    NotificationPlayer.this.abandonAudioFocusAfterError();
                }
                notify();
            }
            Looper.loop();
        }
    }

    private void abandonAudioFocusAfterError() {
        synchronized (this.mQueueAudioFocusLock) {
            if (this.mAudioManagerWithAudioFocus != null) {
                this.mAudioManagerWithAudioFocus.abandonAudioFocus(null);
                this.mAudioManagerWithAudioFocus = null;
            }
        }
    }

    private void startSound(Command command) {
        try {
            synchronized (this.mCompletionHandlingLock) {
                if (this.mLooper != null && this.mLooper.getThread().getState() != Thread.State.TERMINATED) {
                    this.mLooper.quit();
                }
                this.mCompletionThread = new CreationAndCompletionThread(command);
                synchronized (this.mCompletionThread) {
                    this.mCompletionThread.start();
                    this.mCompletionThread.wait();
                }
            }
            long jUptimeMillis = SystemClock.uptimeMillis() - command.requestTime;
            if (jUptimeMillis > 1000) {
                Log.w(this.mTag, "Notification sound delayed by " + jUptimeMillis + "msecs");
            }
        } catch (Exception e) {
            Log.w(this.mTag, "error loading sound for " + command.uri, e);
        }
    }

    private final class CmdThread extends Thread {
        CmdThread() {
            super("NotificationPlayer-" + NotificationPlayer.this.mTag);
        }

        @Override
        public void run() {
            Command command;
            while (true) {
                synchronized (NotificationPlayer.this.mCmdQueue) {
                    command = (Command) NotificationPlayer.this.mCmdQueue.removeFirst();
                }
                switch (command.code) {
                    case 1:
                        NotificationPlayer.this.startSound(command);
                        break;
                    case 2:
                        if (NotificationPlayer.this.mPlayer == null) {
                            Log.w(NotificationPlayer.this.mTag, "STOP command without a player");
                        } else {
                            long jUptimeMillis = SystemClock.uptimeMillis() - command.requestTime;
                            if (jUptimeMillis > 1000) {
                                Log.w(NotificationPlayer.this.mTag, "Notification stop delayed by " + jUptimeMillis + "msecs");
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
                            synchronized (NotificationPlayer.this.mCompletionHandlingLock) {
                                if (NotificationPlayer.this.mLooper != null && NotificationPlayer.this.mLooper.getThread().getState() != Thread.State.TERMINATED) {
                                    NotificationPlayer.this.mLooper.quit();
                                }
                            }
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
    public void onCompletion(MediaPlayer mediaPlayer) {
        synchronized (this.mQueueAudioFocusLock) {
            if (this.mAudioManagerWithAudioFocus != null) {
                this.mAudioManagerWithAudioFocus.abandonAudioFocus(null);
                this.mAudioManagerWithAudioFocus = null;
            }
        }
        synchronized (this.mCmdQueue) {
            synchronized (this.mCompletionHandlingLock) {
                if (this.mCmdQueue.size() == 0) {
                    if (this.mLooper != null) {
                        this.mLooper.quit();
                    }
                    this.mCompletionThread = null;
                }
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Log.e(this.mTag, "error " + i + " (extra=" + i2 + ") playing notification");
        onCompletion(mediaPlayer);
        return true;
    }

    public NotificationPlayer(String str) {
        if (str != null) {
            this.mTag = str;
        } else {
            this.mTag = "NotificationPlayer";
        }
    }

    public void play(Context context, Uri uri, boolean z, AudioAttributes audioAttributes) {
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

    @GuardedBy("mCmdQueue")
    private void enqueueLocked(Command command) {
        this.mCmdQueue.add(command);
        if (this.mThread == null) {
            acquireWakeLock();
            this.mThread = new CmdThread();
            this.mThread.start();
        }
    }

    public void setUsesWakeLock(Context context) {
        synchronized (this.mCmdQueue) {
            if (this.mWakeLock != null || this.mThread != null) {
                throw new RuntimeException("assertion failed mWakeLock=" + this.mWakeLock + " mThread=" + this.mThread);
            }
            this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, this.mTag);
        }
    }

    @GuardedBy("mCmdQueue")
    private void acquireWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.acquire();
        }
    }

    @GuardedBy("mCmdQueue")
    private void releaseWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
    }
}
