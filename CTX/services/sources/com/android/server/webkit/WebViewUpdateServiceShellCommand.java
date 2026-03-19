package com.android.server.webkit;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.webkit.IWebViewUpdateService;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;

class WebViewUpdateServiceShellCommand extends ShellCommand {
    final IWebViewUpdateService mInterface;

    WebViewUpdateServiceShellCommand(IWebViewUpdateService iWebViewUpdateService) {
        this.mInterface = iWebViewUpdateService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            switch (str) {
                case "enable-redundant-packages":
                    return enableFallbackLogic(false);
                case "disable-redundant-packages":
                    return enableFallbackLogic(true);
                case "set-webview-implementation":
                    return setWebViewImplementation();
                case "enable-multiprocess":
                    return enableMultiProcess(true);
                case "disable-multiprocess":
                    return enableMultiProcess(false);
                default:
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    private int enableFallbackLogic(boolean z) throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        this.mInterface.enableFallbackLogic(z);
        outPrintWriter.println("Success");
        return 0;
    }

    private int setWebViewImplementation() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        String strChangeProviderAndSetting = this.mInterface.changeProviderAndSetting(nextArg);
        if (nextArg.equals(strChangeProviderAndSetting)) {
            outPrintWriter.println("Success");
            return 0;
        }
        outPrintWriter.println(String.format("Failed to switch to %s, the WebView implementation is now provided by %s.", nextArg, strChangeProviderAndSetting));
        return 1;
    }

    private int enableMultiProcess(boolean z) throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        this.mInterface.enableMultiProcess(z);
        outPrintWriter.println("Success");
        return 0;
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("WebView updater commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  enable-redundant-packages");
        outPrintWriter.println("    Allow a fallback package to be installed and enabled even when a");
        outPrintWriter.println("    more-preferred package is available. This command is useful when testing");
        outPrintWriter.println("    fallback packages.");
        outPrintWriter.println("  disable-redundant-packages");
        outPrintWriter.println("    Disallow installing and enabling fallback packages when a more-preferred");
        outPrintWriter.println("    package is available.");
        outPrintWriter.println("  set-webview-implementation PACKAGE");
        outPrintWriter.println("    Set the WebView implementation to the specified package.");
        outPrintWriter.println("  enable-multiprocess");
        outPrintWriter.println("    Enable multi-process mode for WebView");
        outPrintWriter.println("  disable-multiprocess");
        outPrintWriter.println("    Disable multi-process mode for WebView");
        outPrintWriter.println();
    }
}
