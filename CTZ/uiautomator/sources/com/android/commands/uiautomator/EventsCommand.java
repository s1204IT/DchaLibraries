package com.android.commands.uiautomator;

import android.app.UiAutomation;
import android.view.accessibility.AccessibilityEvent;
import com.android.commands.uiautomator.Launcher;
import com.android.uiautomator.core.UiAutomationShellWrapper;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EventsCommand extends Launcher.Command {
    private Object mQuitLock;

    public EventsCommand() {
        super("events");
        this.mQuitLock = new Object();
    }

    @Override
    public String shortHelp() {
        return "prints out accessibility events until terminated";
    }

    @Override
    public String detailedOptions() {
        return null;
    }

    @Override
    public void run(String[] strArr) {
        UiAutomationShellWrapper uiAutomationShellWrapper = new UiAutomationShellWrapper();
        uiAutomationShellWrapper.connect();
        uiAutomationShellWrapper.getUiAutomation().setOnAccessibilityEventListener(new UiAutomation.OnAccessibilityEventListener() {
            @Override
            public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
                System.out.println(String.format("%s %s", new SimpleDateFormat("MM-dd HH:mm:ss.SSS").format(new Date()), accessibilityEvent.toString()));
            }
        });
        synchronized (this.mQuitLock) {
            try {
                this.mQuitLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        uiAutomationShellWrapper.disconnect();
    }
}
