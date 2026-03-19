package com.android.commands.am;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.pm.IPackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.util.AndroidException;
import com.android.internal.os.BaseCommand;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class Am extends BaseCommand {
    private IActivityManager mAm;
    private IPackageManager mPm;

    public static void main(String[] strArr) {
        new Am().run(strArr);
    }

    public void onShowUsage(PrintStream printStream) {
        try {
            runAmCmd(new String[]{"help"});
        } catch (AndroidException e) {
            e.printStackTrace(System.err);
        }
    }

    public void onRun() throws Exception {
        this.mAm = ActivityManager.getService();
        if (this.mAm == null) {
            System.err.println("Error type 2");
            throw new AndroidException("Can't connect to activity manager; is the system running?");
        }
        this.mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (this.mPm == null) {
            System.err.println("Error type 2");
            throw new AndroidException("Can't connect to package manager; is the system running?");
        }
        if (nextArgRequired().equals("instrument")) {
            runInstrument();
        } else {
            runAmCmd(getRawArgs());
        }
    }

    int parseUserArg(String str) {
        if ("all".equals(str)) {
            return -1;
        }
        if ("current".equals(str) || "cur".equals(str)) {
            return -2;
        }
        return Integer.parseInt(str);
    }

    static final class MyShellCallback extends ShellCallback {
        boolean mActive = true;

        MyShellCallback() {
        }

        public ParcelFileDescriptor onOpenFile(String str, String str2, String str3) {
            if (!this.mActive) {
                System.err.println("Open attempt after active for: " + str);
                return null;
            }
            File file = new File(str);
            try {
                ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(file, 738197504);
                if (str2 != null) {
                    String fileContext = SELinux.getFileContext(file.getAbsolutePath());
                    if (!SELinux.checkSELinuxAccess(str2, fileContext, "file", "write")) {
                        try {
                            parcelFileDescriptorOpen.close();
                        } catch (IOException e) {
                        }
                        String str4 = "System server has no access to file context " + fileContext;
                        System.err.println(str4 + " (from path " + file.getAbsolutePath() + ", context " + str2 + ")");
                        throw new IllegalArgumentException(str4);
                    }
                }
                return parcelFileDescriptorOpen;
            } catch (FileNotFoundException e2) {
                String str5 = "Unable to open file " + str + ": " + e2;
                System.err.println(str5);
                throw new IllegalArgumentException(str5);
            }
        }
    }

    void runAmCmd(String[] strArr) throws AndroidException {
        MyShellCallback myShellCallback = new MyShellCallback();
        try {
            try {
                this.mAm.asBinder().shellCommand(FileDescriptor.in, FileDescriptor.out, FileDescriptor.err, strArr, myShellCallback, new ResultReceiver(null) {
                });
            } catch (RemoteException e) {
                System.err.println("Error type 2");
                throw new AndroidException("Can't call activity manager; is the system running?");
            }
        } finally {
            myShellCallback.mActive = false;
        }
    }

    public void runInstrument() throws Exception {
        Instrument instrument = new Instrument(this.mAm, this.mPm);
        while (true) {
            String strNextOption = nextOption();
            if (strNextOption != null) {
                if (strNextOption.equals("-p")) {
                    instrument.profileFile = nextArgRequired();
                } else if (strNextOption.equals("-w")) {
                    instrument.wait = true;
                } else if (strNextOption.equals("-r")) {
                    instrument.rawMode = true;
                } else if (strNextOption.equals("-m")) {
                    instrument.protoStd = true;
                } else if (strNextOption.equals("-f")) {
                    instrument.protoFile = true;
                    if (peekNextArg() != null && !peekNextArg().startsWith("-")) {
                        instrument.logPath = nextArg();
                    }
                } else if (strNextOption.equals("-e")) {
                    instrument.args.putString(nextArgRequired(), nextArgRequired());
                } else if (strNextOption.equals("--no_window_animation") || strNextOption.equals("--no-window-animation")) {
                    instrument.noWindowAnimation = true;
                } else if (strNextOption.equals("--no-hidden-api-checks")) {
                    instrument.disableHiddenApiChecks = true;
                } else if (strNextOption.equals("--user")) {
                    instrument.userId = parseUserArg(nextArgRequired());
                } else if (strNextOption.equals("--abi")) {
                    instrument.abi = nextArgRequired();
                } else {
                    System.err.println("Error: Unknown option: " + strNextOption);
                    return;
                }
            } else if (instrument.userId == -1) {
                System.err.println("Error: Can't start instrumentation with user 'all'");
                return;
            } else {
                instrument.componentNameArg = nextArgRequired();
                instrument.run();
                return;
            }
        }
    }
}
