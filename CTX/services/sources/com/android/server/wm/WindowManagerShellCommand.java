package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.view.IWindowManager;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.UiModeManagerService;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowManagerShellCommand extends ShellCommand {
    private final IWindowManager mInterface;
    private final WindowManagerService mInternal;

    public WindowManagerShellCommand(WindowManagerService windowManagerService) {
        this.mInterface = windowManagerService;
        this.mInternal = windowManagerService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            switch (str) {
                case "size":
                    return runDisplaySize(outPrintWriter);
                case "density":
                    return runDisplayDensity(outPrintWriter);
                case "overscan":
                    return runDisplayOverscan(outPrintWriter);
                case "scaling":
                    return runDisplayScaling(outPrintWriter);
                case "dismiss-keyguard":
                    return runDismissKeyguard(outPrintWriter);
                case "tracing":
                    return this.mInternal.mWindowTracing.onShellCommand(this, getNextArgRequired());
                default:
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runDisplaySize(PrintWriter printWriter) throws RemoteException {
        int dimension;
        int dimension2;
        String nextArg = getNextArg();
        if (nextArg == null) {
            Point point = new Point();
            Point point2 = new Point();
            try {
                this.mInterface.getInitialDisplaySize(0, point);
                this.mInterface.getBaseDisplaySize(0, point2);
                printWriter.println("Physical size: " + point.x + "x" + point.y);
                if (!point.equals(point2)) {
                    printWriter.println("Override size: " + point2.x + "x" + point2.y);
                }
            } catch (RemoteException e) {
            }
            return 0;
        }
        if (!"reset".equals(nextArg)) {
            int iIndexOf = nextArg.indexOf(120);
            if (iIndexOf <= 0 || iIndexOf >= nextArg.length() - 1) {
                getErrPrintWriter().println("Error: bad size " + nextArg);
                return -1;
            }
            String strSubstring = nextArg.substring(0, iIndexOf);
            String strSubstring2 = nextArg.substring(iIndexOf + 1);
            try {
                dimension = parseDimension(strSubstring);
                dimension2 = parseDimension(strSubstring2);
            } catch (NumberFormatException e2) {
                getErrPrintWriter().println("Error: bad number " + e2);
                return -1;
            }
        } else {
            dimension2 = -1;
            dimension = -1;
        }
        if (dimension < 0 || dimension2 < 0) {
            this.mInterface.clearForcedDisplaySize(0);
        } else {
            this.mInterface.setForcedDisplaySize(0, dimension, dimension2);
        }
        return 0;
    }

    private int runDisplayDensity(PrintWriter printWriter) throws RemoteException {
        int i;
        String nextArg = getNextArg();
        if (nextArg == null) {
            try {
                int initialDisplayDensity = this.mInterface.getInitialDisplayDensity(0);
                int baseDisplayDensity = this.mInterface.getBaseDisplayDensity(0);
                printWriter.println("Physical density: " + initialDisplayDensity);
                if (initialDisplayDensity != baseDisplayDensity) {
                    printWriter.println("Override density: " + baseDisplayDensity);
                }
            } catch (RemoteException e) {
            }
            return 0;
        }
        if (!"reset".equals(nextArg)) {
            try {
                i = Integer.parseInt(nextArg);
                if (i < 72) {
                    getErrPrintWriter().println("Error: density must be >= 72");
                    return -1;
                }
            } catch (NumberFormatException e2) {
                getErrPrintWriter().println("Error: bad number " + e2);
                return -1;
            }
        } else {
            i = -1;
        }
        if (i > 0) {
            this.mInterface.setForcedDisplayDensityForUser(0, i, -2);
        } else {
            this.mInterface.clearForcedDisplayDensityForUser(0, -2);
        }
        return 0;
    }

    private int runDisplayOverscan(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired = getNextArgRequired();
        Rect rect = new Rect();
        if ("reset".equals(nextArgRequired)) {
            rect.set(0, 0, 0, 0);
        } else {
            Matcher matcher = Pattern.compile("(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+)").matcher(nextArgRequired);
            if (!matcher.matches()) {
                getErrPrintWriter().println("Error: bad rectangle arg: " + nextArgRequired);
                return -1;
            }
            rect.left = Integer.parseInt(matcher.group(1));
            rect.top = Integer.parseInt(matcher.group(2));
            rect.right = Integer.parseInt(matcher.group(3));
            rect.bottom = Integer.parseInt(matcher.group(4));
        }
        this.mInterface.setOverscan(0, rect.left, rect.top, rect.right, rect.bottom);
        return 0;
    }

    private int runDisplayScaling(PrintWriter printWriter) throws RemoteException {
        String nextArgRequired = getNextArgRequired();
        if (UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO.equals(nextArgRequired)) {
            this.mInterface.setForcedDisplayScalingMode(0, 0);
        } else if ("off".equals(nextArgRequired)) {
            this.mInterface.setForcedDisplayScalingMode(0, 1);
        } else {
            getErrPrintWriter().println("Error: scaling must be 'auto' or 'off'");
            return -1;
        }
        return 0;
    }

    private int runDismissKeyguard(PrintWriter printWriter) throws RemoteException {
        this.mInterface.dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
        return 0;
    }

    private int parseDimension(String str) throws NumberFormatException {
        int baseDisplayDensity;
        if (str.endsWith("px")) {
            return Integer.parseInt(str.substring(0, str.length() - 2));
        }
        if (str.endsWith("dp")) {
            try {
                baseDisplayDensity = this.mInterface.getBaseDisplayDensity(0);
            } catch (RemoteException e) {
                baseDisplayDensity = 160;
            }
            return (Integer.parseInt(str.substring(0, str.length() - 2)) * baseDisplayDensity) / 160;
        }
        return Integer.parseInt(str);
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Window manager (window) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("      Print this help text.");
        outPrintWriter.println("  size [reset|WxH|WdpxHdp]");
        outPrintWriter.println("    Return or override display size.");
        outPrintWriter.println("    width and height in pixels unless suffixed with 'dp'.");
        outPrintWriter.println("  density [reset|DENSITY]");
        outPrintWriter.println("    Return or override display density.");
        outPrintWriter.println("  overscan [reset|LEFT,TOP,RIGHT,BOTTOM]");
        outPrintWriter.println("    Set overscan area for display.");
        outPrintWriter.println("  scaling [off|auto]");
        outPrintWriter.println("    Set display scaling mode.");
        outPrintWriter.println("  dismiss-keyguard");
        outPrintWriter.println("    Dismiss the keyguard, prompting user for auth if necessary.");
        if (!Build.IS_USER) {
            outPrintWriter.println("  tracing (start | stop)");
            outPrintWriter.println("    Start or stop window tracing.");
        }
    }
}
