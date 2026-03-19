package com.mediatek.gallerybasic.dynamic;

import android.os.SystemClock;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.dynamic.PlayList;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.gallerybasic.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

class PlayThreads {
    private static final String TAG = "MtkGallery2/PlayThreads";
    private int mThreadNum;
    private ArrayList<WorkThread> mThreads;
    private boolean mThreadsStart = false;
    private ArrayList<Command> mWaitingCommands = new ArrayList<>();
    private ArrayList<Command> mRunningCommands = new ArrayList<>();

    public PlayThreads(int i) {
        this.mThreadNum = i;
    }

    public synchronized void start() {
        if (this.mThreadsStart) {
            return;
        }
        this.mThreads = new ArrayList<>();
        for (int i = 0; i < this.mThreadNum; i++) {
            WorkThread workThread = new WorkThread(i);
            workThread.setName("WorkThread-" + i);
            this.mThreads.add(i, workThread);
            workThread.start();
        }
        this.mThreadsStart = true;
        Log.d(TAG, "<start>");
    }

    public synchronized void stop() {
        if (this.mThreadsStart) {
            for (int i = 0; i < this.mThreadNum; i++) {
                this.mThreads.get(i).interrupt();
                this.mThreads.set(i, null);
            }
            this.mThreads = null;
            this.mThreadsStart = false;
            Log.d(TAG, "<stop>");
        }
    }

    public synchronized void submit(PlayList.Entry entry, Player.State state) {
        for (Command command : this.mWaitingCommands) {
            if (command.entry == entry && command.targetState == state) {
                Log.d(TAG, "<submit> has same cmd in waiting cmds, not submit");
                return;
            }
        }
        for (Command command2 : this.mRunningCommands) {
            if (command2.entry == entry && command2.targetState == state) {
                Log.d(TAG, "<submit> has same cmd in running cmds, not submit");
                return;
            }
        }
        Command command3 = new Command(entry, state);
        this.mWaitingCommands.add(command3);
        Log.d(TAG, "<submit> Add new command = " + command3);
        if (state == Player.State.RELEASED) {
            Iterator<Command> it = this.mRunningCommands.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Command next = it.next();
                if (next.entry == entry) {
                    if (next.targetState != Player.State.RELEASED) {
                        next.cancel();
                        Log.d(TAG, "<submit> Cancel command = " + next);
                    }
                }
            }
        }
        if (this.mThreads == null) {
            start();
        }
        if (entry.threadIndex != -1) {
            WorkThread workThread = this.mThreads.get(entry.threadIndex);
            synchronized (workThread) {
                workThread.notifyAll();
            }
        } else {
            for (WorkThread workThread2 : this.mThreads) {
                synchronized (workThread2) {
                    workThread2.notifyAll();
                }
            }
        }
    }

    public synchronized void clearAllCmds() {
        Iterator<Command> it = this.mWaitingCommands.iterator();
        while (it.hasNext()) {
            if (it.next().targetState != Player.State.RELEASED) {
                it.remove();
            }
        }
    }

    private synchronized Command getCmdsForThread(int i) {
        Iterator<Command> it = this.mWaitingCommands.iterator();
        while (it.hasNext()) {
            Command next = it.next();
            if (next.entry.threadIndex == -1) {
                it.remove();
                next.entry.threadIndex = i;
                this.mRunningCommands.add(next);
                return next;
            }
            if (next.entry.threadIndex == i) {
                it.remove();
                this.mRunningCommands.add(next);
                return next;
            }
        }
        Log.d(TAG, "<getCmdsForThread> No cmd for thread " + i);
        return null;
    }

    class Command implements Player.TaskCanceller {
        public final PlayList.Entry entry;
        private boolean mIsCancelled = false;
        public final Player.State targetState;

        public Command(PlayList.Entry entry, Player.State state) {
            this.entry = entry;
            this.targetState = state;
        }

        public void cancel() {
            this.mIsCancelled = true;
            if (this.entry != null && this.entry.player != null) {
                this.entry.player.onCancel();
            }
        }

        @Override
        public boolean isCancelled() {
            return this.mIsCancelled;
        }

        public String toString() {
            return (((((("[filePath = ") + this.entry.data.filePath) + ", targetState = ") + this.targetState) + ", mIsCancelled = ") + this.mIsCancelled) + "]";
        }
    }

    class WorkThread extends Thread {
        private boolean mActive = true;
        private int mIndex;

        public WorkThread(int i) {
            this.mIndex = i;
        }

        @Override
        public synchronized void interrupt() {
            this.mActive = false;
            super.interrupt();
        }

        @Override
        public void run() {
            Log.d(PlayThreads.TAG, "<WorkThread.run> begin, mIndex = " + this.mIndex);
            while (true) {
                Command cmdsForThread = PlayThreads.this.getCmdsForThread(this.mIndex);
                synchronized (this) {
                    if (!this.mActive && cmdsForThread == null) {
                        Log.d(PlayThreads.TAG, "<WorkThread.run> exit, mIndex = " + this.mIndex);
                        return;
                    }
                    if (cmdsForThread == null && this.mActive) {
                        Log.d(PlayThreads.TAG, "<WorkThread.run> wait, mIndex = " + this.mIndex);
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            Log.d(PlayThreads.TAG, "<WorkThread.run> InterruptedException, mIndex = " + this.mIndex);
                        }
                    }
                }
                if (cmdsForThread != null) {
                    runCmd(cmdsForThread);
                    synchronized (PlayThreads.this) {
                        PlayThreads.this.mRunningCommands.remove(cmdsForThread);
                    }
                }
            }
        }

        private void runCmd(Command command) {
            if (command.isCancelled()) {
                Log.d(PlayThreads.TAG, "<runCmd> cancelled, return");
                return;
            }
            long jUptimeMillis = SystemClock.uptimeMillis();
            command.entry.player.setTaskCanceller(command);
            switch (AnonymousClass1.$SwitchMap$com$mediatek$gallerybasic$base$Player$State[command.targetState.ordinal()]) {
                case 1:
                    gotoStatePrepared(command);
                    break;
                case 2:
                    gotoStatePlaying(command);
                    break;
                case 3:
                    gotoStateReleased(command);
                    break;
                default:
                    Log.d(PlayThreads.TAG, "<runCmd> error targetState = " + command.targetState);
                    break;
            }
            command.entry.player.setTaskCanceller(null);
            Log.d(PlayThreads.TAG, "<runCmd> cost " + (SystemClock.uptimeMillis() - jUptimeMillis) + " ms");
        }

        private void gotoStatePrepared(Command command) {
            Log.d(PlayThreads.TAG, "<gotoStatePrepared> begin, filePath = " + command.entry.data.filePath + ", player = " + command.entry.player);
            switch (AnonymousClass1.$SwitchMap$com$mediatek$gallerybasic$base$Player$State[command.entry.player.getState().ordinal()]) {
                case 2:
                    boolean zPause = command.entry.player.pause();
                    if (!command.isCancelled() && zPause) {
                        command.entry.player.stop();
                    }
                    break;
                case 3:
                    command.entry.player.prepare();
                    break;
            }
            Log.d(PlayThreads.TAG, "<gotoStatePrepared> end, filePath = " + command.entry.data.filePath + ", player = " + command.entry.player + ", state = " + command.entry.player.getState() + ", cancelled = " + command.isCancelled());
        }

        private void gotoStatePlaying(Command command) {
            Log.d(PlayThreads.TAG, "<gotoStatePlaying> begin, filePath = " + command.entry.data.filePath + ", player = " + command.entry.player);
            switch (AnonymousClass1.$SwitchMap$com$mediatek$gallerybasic$base$Player$State[command.entry.player.getState().ordinal()]) {
                case 1:
                    command.entry.player.start();
                    break;
                case 3:
                    boolean zPrepare = command.entry.player.prepare();
                    if (!command.isCancelled() && zPrepare) {
                        command.entry.player.start();
                    }
                    break;
            }
            Log.d(PlayThreads.TAG, "<gotoStatePlaying> end, filePath = " + command.entry.data.filePath + ", player = " + command.entry.player + ", state = " + command.entry.player.getState() + ", cancelled = " + command.isCancelled());
        }

        private void gotoStateReleased(Command command) {
            Log.d(PlayThreads.TAG, "<gotoStateReleased> begin, filePath = " + command.entry.data.filePath + ", player = " + command.entry.player);
            switch (AnonymousClass1.$SwitchMap$com$mediatek$gallerybasic$base$Player$State[command.entry.player.getState().ordinal()]) {
                case 1:
                    command.entry.player.release();
                    break;
                case 2:
                    boolean zPause = command.entry.player.pause();
                    if (!command.isCancelled() && zPause) {
                        command.entry.player.stop();
                    }
                    if (!command.isCancelled()) {
                        command.entry.player.release();
                    }
                    break;
            }
            Log.d(PlayThreads.TAG, "<gotoStateReleased> end, filePath = " + command.entry.data.filePath + ", player = " + command.entry.player + ", state = " + command.entry.player.getState() + ", cancelled = " + command.isCancelled());
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$gallerybasic$base$Player$State = new int[Player.State.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$gallerybasic$base$Player$State[Player.State.PREPARED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$gallerybasic$base$Player$State[Player.State.PLAYING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$gallerybasic$base$Player$State[Player.State.RELEASED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public synchronized void logCmdsWaitToRun(String str) {
        if (DebugUtils.DEBUG_PLAY_ENGINE) {
            Iterator<Command> it = this.mWaitingCommands.iterator();
            int i = 0;
            Log.d(TAG, str + " begin ----------------------------------------");
            while (it.hasNext()) {
                Log.d(TAG, str + " [" + i + "] " + it.next());
                i++;
            }
            Log.d(TAG, str + " end   ----------------------------------------");
        }
    }
}
