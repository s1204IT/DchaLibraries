package com.android.launcher3;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;

public class Stats {
    private final String mLaunchBroadcastPermission;
    private final Launcher mLauncher;

    public interface LaunchSourceProvider {
        void fillInLaunchSourceData(View view, Bundle bundle);
    }

    public static class LaunchSourceUtils {
        public static Bundle createSourceData() {
            Bundle sourceData = new Bundle();
            sourceData.putString("container", "homescreen");
            sourceData.putInt("container_page", 0);
            sourceData.putInt("sub_container_page", 0);
            return sourceData;
        }

        public static void populateSourceDataFromAncestorProvider(View v, Bundle sourceData) {
            if (v == null) {
                return;
            }
            LaunchSourceProvider provider = null;
            ViewParent parent = v.getParent();
            while (true) {
                if (parent == null || !(parent instanceof View)) {
                    break;
                }
                if (parent instanceof LaunchSourceProvider) {
                    provider = (LaunchSourceProvider) parent;
                    break;
                }
                parent = parent.getParent();
            }
            if (provider != null) {
                provider.fillInLaunchSourceData(v, sourceData);
            } else if (!LauncherAppState.isDogfoodBuild()) {
            } else {
                throw new RuntimeException("Expected LaunchSourceProvider");
            }
        }
    }

    public Stats(Launcher launcher) {
        this.mLauncher = launcher;
        this.mLaunchBroadcastPermission = launcher.getResources().getString(R.string.receive_launch_broadcasts_permission);
    }

    public void recordLaunch(View v, Intent intent, ShortcutInfo shortcut) {
        if ("eng".equals(Build.TYPE)) {
            Intent intent2 = new Intent(intent);
            intent2.setSourceBounds(null);
            String flat = intent2.toUri(0);
            Intent broadcastIntent = new Intent("com.android.launcher3.action.LAUNCH").putExtra("intent", flat);
            if (shortcut != null) {
                broadcastIntent.putExtra("container", shortcut.container).putExtra("screen", shortcut.screenId).putExtra("cellX", shortcut.cellX).putExtra("cellY", shortcut.cellY);
            }
            Bundle sourceExtras = LaunchSourceUtils.createSourceData();
            LaunchSourceUtils.populateSourceDataFromAncestorProvider(v, sourceExtras);
            broadcastIntent.putExtra("source", sourceExtras);
            String[] packages = this.mLauncher.getResources().getStringArray(R.array.launch_broadcast_targets);
            for (String p : packages) {
                broadcastIntent.setPackage(p);
                this.mLauncher.sendBroadcast(broadcastIntent, this.mLaunchBroadcastPermission);
            }
        }
    }
}
