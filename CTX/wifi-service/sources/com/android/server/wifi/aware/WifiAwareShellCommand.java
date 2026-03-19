package com.android.server.wifi.aware;

import android.os.Binder;
import android.os.ShellCommand;
import android.text.TextUtils;
import android.util.Log;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WifiAwareShellCommand extends ShellCommand {
    private static final String TAG = "WifiAwareShellCommand";
    private Map<String, DelegatedShellCommand> mDelegatedCommands = new HashMap();

    public interface DelegatedShellCommand {
        int onCommand(ShellCommand shellCommand);

        void onHelp(String str, ShellCommand shellCommand);

        void onReset();
    }

    public void register(String str, DelegatedShellCommand delegatedShellCommand) {
        if (this.mDelegatedCommands.containsKey(str)) {
            Log.e(TAG, "register: overwriting existing command -- '" + str + "'");
        }
        this.mDelegatedCommands.put(str, delegatedShellCommand);
    }

    public int onCommand(String str) {
        checkRootPermission();
        PrintWriter errPrintWriter = getErrPrintWriter();
        try {
            if ("reset".equals(str)) {
                Iterator<DelegatedShellCommand> it = this.mDelegatedCommands.values().iterator();
                while (it.hasNext()) {
                    it.next().onReset();
                }
                return 0;
            }
            DelegatedShellCommand delegatedShellCommand = null;
            if (!TextUtils.isEmpty(str)) {
                delegatedShellCommand = this.mDelegatedCommands.get(str);
            }
            if (delegatedShellCommand != null) {
                return delegatedShellCommand.onCommand(this);
            }
            return handleDefaultCommands(str);
        } catch (Exception e) {
            errPrintWriter.println("Exception: " + e);
            return -1;
        }
    }

    private void checkRootPermission() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0) {
            return;
        }
        throw new SecurityException("Uid " + callingUid + " does not have access to wifiaware commands");
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Wi-Fi Aware (wifiaware) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println("  reset");
        outPrintWriter.println("    Reset parameters to default values.");
        for (Map.Entry<String, DelegatedShellCommand> entry : this.mDelegatedCommands.entrySet()) {
            entry.getValue().onHelp(entry.getKey(), this);
        }
        outPrintWriter.println();
    }
}
