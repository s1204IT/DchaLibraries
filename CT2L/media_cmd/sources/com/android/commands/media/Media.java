package com.android.commands.media;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.ParceledListSlice;
import android.media.MediaMetadata;
import android.media.session.ISessionController;
import android.media.session.ISessionControllerCallback;
import android.media.session.ISessionManager;
import android.media.session.ParcelableVolumeInfo;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.AndroidException;
import android.view.KeyEvent;
import com.android.internal.os.BaseCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

public class Media extends BaseCommand {
    private ISessionManager mSessionService;

    public static void main(String[] args) {
        new Media().run(args);
    }

    public void onShowUsage(PrintStream out) {
        out.println("usage: media [subcommand] [options]\n       media dispatch KEY\n       media list-sessions\n       media monitor <tag>\n\nmedia dispatch: dispatch a media key to the system.\n                KEY may be: play, pause, play-pause, mute, headsethook,\n                stop, next, previous, rewind, record, fast-forword.\nmedia list-sessions: print a list of the current sessions.\nmedia monitor: monitor updates to the specified session.\n                       Use the tag from list-sessions.\n");
    }

    public void onRun() throws Exception {
        this.mSessionService = ISessionManager.Stub.asInterface(ServiceManager.checkService("media_session"));
        if (this.mSessionService == null) {
            System.err.println("Error type 2");
            throw new AndroidException("Can't connect to media session service; is the system running?");
        }
        String op = nextArgRequired();
        if (op.equals("dispatch")) {
            runDispatch();
            return;
        }
        if (op.equals("list-sessions")) {
            runListSessions();
        } else if (op.equals("monitor")) {
            runMonitor();
        } else {
            showError("Error: unknown command '" + op + "'");
        }
    }

    private void sendMediaKey(KeyEvent event) {
        try {
            this.mSessionService.dispatchMediaKeyEvent(event, false);
        } catch (RemoteException e) {
        }
    }

    private void runMonitor() throws Exception {
        String id = nextArgRequired();
        if (id == null) {
            showError("Error: must include a session id");
            return;
        }
        boolean success = false;
        try {
            List<IBinder> sessions = this.mSessionService.getSessions((ComponentName) null, ActivityManager.getCurrentUser());
            Iterator<IBinder> it = sessions.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                IBinder session = it.next();
                ISessionController controller = ISessionController.Stub.asInterface(session);
                if (controller != null) {
                    try {
                        if (id.equals(controller.getTag())) {
                            ControllerMonitor monitor = new ControllerMonitor(controller);
                            monitor.run();
                            success = true;
                            break;
                        }
                        continue;
                    } catch (RemoteException e) {
                    }
                }
            }
        } catch (Exception e2) {
            System.out.println("***Error monitoring session*** " + e2.getMessage());
        }
        if (!success) {
            System.out.println("No session found with id " + id);
        }
    }

    private void runDispatch() throws Exception {
        int keycode;
        String cmd = nextArgRequired();
        if ("play".equals(cmd)) {
            keycode = 126;
        } else if ("pause".equals(cmd)) {
            keycode = 127;
        } else if ("play-pause".equals(cmd)) {
            keycode = 85;
        } else if ("mute".equals(cmd)) {
            keycode = 91;
        } else if ("headsethook".equals(cmd)) {
            keycode = 79;
        } else if ("stop".equals(cmd)) {
            keycode = 86;
        } else if ("next".equals(cmd)) {
            keycode = 87;
        } else if ("previous".equals(cmd)) {
            keycode = 88;
        } else if ("rewind".equals(cmd)) {
            keycode = 89;
        } else if ("record".equals(cmd)) {
            keycode = 130;
        } else if ("fast-forward".equals(cmd)) {
            keycode = 90;
        } else {
            showError("Error: unknown dispatch code '" + cmd + "'");
            return;
        }
        long now = SystemClock.uptimeMillis();
        sendMediaKey(new KeyEvent(now, now, 0, keycode, 0, 0, -1, 0, 0, 257));
        sendMediaKey(new KeyEvent(now, now, 1, keycode, 0, 0, -1, 0, 0, 257));
    }

    class ControllerMonitor extends ISessionControllerCallback.Stub {
        private final ISessionController mController;

        public ControllerMonitor(ISessionController controller) {
            this.mController = controller;
        }

        public void onSessionDestroyed() {
            System.out.println("onSessionDestroyed. Enter q to quit.");
        }

        public void onEvent(String event, Bundle extras) {
            System.out.println("onSessionEvent event=" + event + ", extras=" + extras);
        }

        public void onPlaybackStateChanged(PlaybackState state) {
            System.out.println("onPlaybackStateChanged " + state);
        }

        public void onMetadataChanged(MediaMetadata metadata) {
            String mmString = metadata == null ? null : "title=" + metadata.getDescription();
            System.out.println("onMetadataChanged " + mmString);
        }

        public void onQueueChanged(ParceledListSlice queue) throws RemoteException {
            System.out.println("onQueueChanged, " + (queue == null ? "null queue" : " size=" + queue.getList().size()));
        }

        public void onQueueTitleChanged(CharSequence title) throws RemoteException {
            System.out.println("onQueueTitleChange " + ((Object) title));
        }

        public void onExtrasChanged(Bundle extras) throws RemoteException {
            System.out.println("onExtrasChanged " + extras);
        }

        public void onVolumeInfoChanged(ParcelableVolumeInfo info) throws RemoteException {
            System.out.println("onVolumeInfoChanged " + info);
        }

        void printUsageMessage() {
            try {
                System.out.println("V2Monitoring session " + this.mController.getTag() + "...  available commands: play, pause, next, previous");
            } catch (RemoteException e) {
                System.out.println("Error trying to monitor session!");
            }
            System.out.println("(q)uit: finish monitoring");
        }

        void run() throws RemoteException {
            printUsageMessage();
            HandlerThread cbThread = new HandlerThread("MediaCb") {
                @Override
                protected void onLooperPrepared() {
                    try {
                        ControllerMonitor.this.mController.registerCallbackListener(ControllerMonitor.this);
                    } catch (RemoteException e) {
                        System.out.println("Error registering monitor callback");
                    }
                }
            };
            cbThread.start();
            try {
                try {
                    InputStreamReader converter = new InputStreamReader(System.in);
                    BufferedReader in = new BufferedReader(converter);
                    while (true) {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }
                        boolean addNewline = true;
                        if (line.length() <= 0) {
                            addNewline = false;
                        } else {
                            if ("q".equals(line) || "quit".equals(line)) {
                                break;
                            }
                            if ("play".equals(line)) {
                                this.mController.play();
                            } else if ("pause".equals(line)) {
                                this.mController.pause();
                            } else if ("next".equals(line)) {
                                this.mController.next();
                            } else if ("previous".equals(line)) {
                                this.mController.previous();
                            } else {
                                System.out.println("Invalid command: " + line);
                            }
                        }
                        synchronized (this) {
                            if (addNewline) {
                                System.out.println("");
                                printUsageMessage();
                            } else {
                                printUsageMessage();
                            }
                        }
                    }
                    cbThread.getLooper().quit();
                    try {
                        this.mController.unregisterCallbackListener(this);
                    } catch (Exception e) {
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                    cbThread.getLooper().quit();
                    try {
                        this.mController.unregisterCallbackListener(this);
                    } catch (Exception e3) {
                    }
                }
            } catch (Throwable th) {
                cbThread.getLooper().quit();
                try {
                    this.mController.unregisterCallbackListener(this);
                } catch (Exception e4) {
                }
                throw th;
            }
        }
    }

    private void runListSessions() {
        System.out.println("Sessions:");
        try {
            List<IBinder> sessions = this.mSessionService.getSessions((ComponentName) null, ActivityManager.getCurrentUser());
            for (IBinder session : sessions) {
                ISessionController controller = ISessionController.Stub.asInterface(session);
                if (controller != null) {
                    try {
                        System.out.println("  tag=" + controller.getTag() + ", package=" + controller.getPackageName());
                    } catch (RemoteException e) {
                    }
                }
            }
        } catch (Exception e2) {
            System.out.println("***Error listing sessions***");
        }
    }
}
