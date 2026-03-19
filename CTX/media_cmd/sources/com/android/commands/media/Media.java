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

public class Media extends BaseCommand {
    private static final String PACKAGE_NAME = "";
    private ISessionManager mSessionService;

    public static void main(String[] strArr) {
        new Media().run(strArr);
    }

    public void onShowUsage(PrintStream printStream) {
        printStream.println("usage: media [subcommand] [options]\n       media dispatch KEY\n       media list-sessions\n       media monitor <tag>\n       media volume [options]\n\nmedia dispatch: dispatch a media key to the system.\n                KEY may be: play, pause, play-pause, mute, headsethook,\n                stop, next, previous, rewind, record, fast-forword.\nmedia list-sessions: print a list of the current sessions.\nmedia monitor: monitor updates to the specified session.\n                       Use the tag from list-sessions.\nmedia volume:  " + VolumeCtrl.USAGE);
    }

    public void onRun() throws Exception {
        this.mSessionService = ISessionManager.Stub.asInterface(ServiceManager.checkService("media_session"));
        if (this.mSessionService == null) {
            System.err.println("Error type 2");
            throw new AndroidException("Can't connect to media session service; is the system running?");
        }
        String strNextArgRequired = nextArgRequired();
        if (strNextArgRequired.equals("dispatch")) {
            runDispatch();
            return;
        }
        if (strNextArgRequired.equals("list-sessions")) {
            runListSessions();
            return;
        }
        if (strNextArgRequired.equals("monitor")) {
            runMonitor();
            return;
        }
        if (strNextArgRequired.equals("volume")) {
            runVolume();
            return;
        }
        showError("Error: unknown command '" + strNextArgRequired + "'");
    }

    private void sendMediaKey(KeyEvent keyEvent) {
        try {
            this.mSessionService.dispatchMediaKeyEvent(PACKAGE_NAME, false, keyEvent, false);
        } catch (RemoteException e) {
        }
    }

    private void runMonitor() throws Exception {
        String strNextArgRequired = nextArgRequired();
        if (strNextArgRequired == null) {
            showError("Error: must include a session id");
            return;
        }
        boolean z = false;
        try {
            Iterator it = this.mSessionService.getSessions((ComponentName) null, ActivityManager.getCurrentUser()).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ISessionController iSessionControllerAsInterface = ISessionController.Stub.asInterface((IBinder) it.next());
                if (iSessionControllerAsInterface != null) {
                    try {
                        if (strNextArgRequired.equals(iSessionControllerAsInterface.getTag())) {
                            new ControllerMonitor(iSessionControllerAsInterface).run();
                            z = true;
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
        if (!z) {
            System.out.println("No session found with id " + strNextArgRequired);
        }
    }

    private void runDispatch() throws Exception {
        int i;
        String strNextArgRequired = nextArgRequired();
        if ("play".equals(strNextArgRequired)) {
            i = 126;
        } else if ("pause".equals(strNextArgRequired)) {
            i = 127;
        } else if ("play-pause".equals(strNextArgRequired)) {
            i = 85;
        } else if ("mute".equals(strNextArgRequired)) {
            i = 91;
        } else if ("headsethook".equals(strNextArgRequired)) {
            i = 79;
        } else if ("stop".equals(strNextArgRequired)) {
            i = 86;
        } else if ("next".equals(strNextArgRequired)) {
            i = 87;
        } else if ("previous".equals(strNextArgRequired)) {
            i = 88;
        } else if ("rewind".equals(strNextArgRequired)) {
            i = 89;
        } else if ("record".equals(strNextArgRequired)) {
            i = 130;
        } else if ("fast-forward".equals(strNextArgRequired)) {
            i = 90;
        } else {
            showError("Error: unknown dispatch code '" + strNextArgRequired + "'");
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        sendMediaKey(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i, 0, 0, -1, 0, 0, 257));
        sendMediaKey(new KeyEvent(jUptimeMillis, jUptimeMillis, 1, i, 0, 0, -1, 0, 0, 257));
    }

    class ControllerMonitor extends ISessionControllerCallback.Stub {
        private final ISessionController mController;

        public ControllerMonitor(ISessionController iSessionController) {
            this.mController = iSessionController;
        }

        public void onSessionDestroyed() {
            System.out.println("onSessionDestroyed. Enter q to quit.");
        }

        public void onEvent(String str, Bundle bundle) {
            System.out.println("onSessionEvent event=" + str + ", extras=" + bundle);
        }

        public void onPlaybackStateChanged(PlaybackState playbackState) {
            System.out.println("onPlaybackStateChanged " + playbackState);
        }

        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            String str;
            if (mediaMetadata == null) {
                str = null;
            } else {
                str = "title=" + mediaMetadata.getDescription();
            }
            System.out.println("onMetadataChanged " + str);
        }

        public void onQueueChanged(ParceledListSlice parceledListSlice) throws RemoteException {
            String str;
            PrintStream printStream = System.out;
            StringBuilder sb = new StringBuilder();
            sb.append("onQueueChanged, ");
            if (parceledListSlice == null) {
                str = "null queue";
            } else {
                str = " size=" + parceledListSlice.getList().size();
            }
            sb.append(str);
            printStream.println(sb.toString());
        }

        public void onQueueTitleChanged(CharSequence charSequence) throws RemoteException {
            System.out.println("onQueueTitleChange " + ((Object) charSequence));
        }

        public void onExtrasChanged(Bundle bundle) throws RemoteException {
            System.out.println("onExtrasChanged " + bundle);
        }

        public void onVolumeInfoChanged(ParcelableVolumeInfo parcelableVolumeInfo) throws RemoteException {
            System.out.println("onVolumeInfoChanged " + parcelableVolumeInfo);
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
            HandlerThread handlerThread = new HandlerThread("MediaCb") {
                @Override
                protected void onLooperPrepared() {
                    try {
                        ControllerMonitor.this.mController.registerCallbackListener(Media.PACKAGE_NAME, ControllerMonitor.this);
                    } catch (RemoteException e) {
                        System.out.println("Error registering monitor callback");
                    }
                }
            };
            handlerThread.start();
            try {
                try {
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                        while (true) {
                            String line = bufferedReader.readLine();
                            if (line == null) {
                                break;
                            }
                            boolean z = true;
                            if (line.length() > 0) {
                                if ("q".equals(line) || "quit".equals(line)) {
                                    break;
                                }
                                if ("play".equals(line)) {
                                    dispatchKeyCode(126);
                                } else if ("pause".equals(line)) {
                                    dispatchKeyCode(127);
                                } else if ("next".equals(line)) {
                                    dispatchKeyCode(87);
                                } else if ("previous".equals(line)) {
                                    dispatchKeyCode(88);
                                } else {
                                    System.out.println("Invalid command: " + line);
                                }
                            } else {
                                z = false;
                            }
                            synchronized (this) {
                                if (z) {
                                    try {
                                        System.out.println(Media.PACKAGE_NAME);
                                    } catch (Throwable th) {
                                        throw th;
                                    }
                                }
                                printUsageMessage();
                            }
                        }
                        handlerThread.getLooper().quit();
                        this.mController.unregisterCallbackListener(this);
                    } catch (IOException e) {
                        e.printStackTrace();
                        handlerThread.getLooper().quit();
                        this.mController.unregisterCallbackListener(this);
                    }
                } catch (Exception e2) {
                }
            } catch (Throwable th2) {
                handlerThread.getLooper().quit();
                try {
                    this.mController.unregisterCallbackListener(this);
                } catch (Exception e3) {
                }
                throw th2;
            }
        }

        private void dispatchKeyCode(int i) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            KeyEvent keyEvent = new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i, 0, 0, -1, 0, 0, 257);
            KeyEvent keyEvent2 = new KeyEvent(jUptimeMillis, jUptimeMillis, 1, i, 0, 0, -1, 0, 0, 257);
            try {
                this.mController.sendMediaButton(Media.PACKAGE_NAME, (ISessionControllerCallback) null, false, keyEvent);
                this.mController.sendMediaButton(Media.PACKAGE_NAME, (ISessionControllerCallback) null, false, keyEvent2);
            } catch (RemoteException e) {
                System.out.println("Failed to dispatch " + i);
            }
        }
    }

    private void runListSessions() {
        System.out.println("Sessions:");
        try {
            Iterator it = this.mSessionService.getSessions((ComponentName) null, ActivityManager.getCurrentUser()).iterator();
            while (it.hasNext()) {
                ISessionController iSessionControllerAsInterface = ISessionController.Stub.asInterface((IBinder) it.next());
                if (iSessionControllerAsInterface != null) {
                    try {
                        System.out.println("  tag=" + iSessionControllerAsInterface.getTag() + ", package=" + iSessionControllerAsInterface.getPackageName());
                    } catch (RemoteException e) {
                    }
                }
            }
        } catch (Exception e2) {
            System.out.println("***Error listing sessions***");
        }
    }

    private void runVolume() throws Exception {
        VolumeCtrl.run(this);
    }
}
