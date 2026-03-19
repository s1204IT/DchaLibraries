package com.android.commands.uiautomator;

import android.app.UiAutomation;
import android.graphics.Point;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Environment;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.commands.uiautomator.Launcher;
import com.android.uiautomator.core.AccessibilityNodeInfoDumper;
import com.android.uiautomator.core.UiAutomationShellWrapper;
import java.io.File;
import java.util.concurrent.TimeoutException;

public class DumpCommand extends Launcher.Command {
    private static final File DEFAULT_DUMP_FILE = new File(Environment.getLegacyExternalStorageDirectory(), "window_dump.xml");

    public DumpCommand() {
        super("dump");
    }

    @Override
    public String shortHelp() {
        return "creates an XML dump of current UI hierarchy";
    }

    @Override
    public String detailedOptions() {
        return "    dump [--verbose][file]\n      [--compressed]: dumps compressed layout information.\n      [file]: the location where the dumped XML should be stored, default is\n      " + DEFAULT_DUMP_FILE.getAbsolutePath() + "\n";
    }

    @Override
    public void run(String[] strArr) {
        File file = DEFAULT_DUMP_FILE;
        boolean z = true;
        for (String str : strArr) {
            if (str.equals("--compressed")) {
                z = false;
            } else if (!str.startsWith("-")) {
                file = new File(str);
            }
        }
        UiAutomationShellWrapper uiAutomationShellWrapper = new UiAutomationShellWrapper();
        uiAutomationShellWrapper.connect();
        if (z) {
            uiAutomationShellWrapper.setCompressedLayoutHierarchy(false);
        } else {
            uiAutomationShellWrapper.setCompressedLayoutHierarchy(true);
        }
        try {
            UiAutomation uiAutomation = uiAutomationShellWrapper.getUiAutomation();
            uiAutomation.waitForIdle(1000L, 10000L);
            AccessibilityNodeInfo rootInActiveWindow = uiAutomation.getRootInActiveWindow();
            if (rootInActiveWindow == null) {
                System.err.println("ERROR: null root node returned by UiTestAutomationBridge.");
                return;
            }
            Display realDisplay = DisplayManagerGlobal.getInstance().getRealDisplay(0);
            int rotation = realDisplay.getRotation();
            Point point = new Point();
            realDisplay.getSize(point);
            AccessibilityNodeInfoDumper.dumpWindowToFile(rootInActiveWindow, file, rotation, point.x, point.y);
            uiAutomationShellWrapper.disconnect();
            System.out.println(String.format("UI hierchary dumped to: %s", file.getAbsolutePath()));
        } catch (TimeoutException e) {
            System.err.println("ERROR: could not get idle state.");
        } finally {
            uiAutomationShellWrapper.disconnect();
        }
    }
}
