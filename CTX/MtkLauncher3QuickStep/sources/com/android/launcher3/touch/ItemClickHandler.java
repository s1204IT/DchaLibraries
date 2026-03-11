package com.android.launcher3.touch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Process;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.PromiseAppInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.widget.PendingAppWidgetHostView;
import com.android.launcher3.widget.WidgetAddFlowHandler;

public class ItemClickHandler {
    public static final View.OnClickListener INSTANCE = new View.OnClickListener() {
        @Override
        public final void onClick(View view) {
            ItemClickHandler.onClick(view);
        }
    };

    public static void onClick(View view) {
        if (view.getWindowToken() == null) {
            return;
        }
        Launcher launcher = Launcher.getLauncher(view.getContext());
        if (!launcher.getWorkspace().isFinishedSwitchingState()) {
            return;
        }
        Object tag = view.getTag();
        if (tag instanceof ShortcutInfo) {
            onClickAppShortcut(view, (ShortcutInfo) tag, launcher);
            return;
        }
        if (tag instanceof FolderInfo) {
            if (view instanceof FolderIcon) {
                onClickFolderIcon(view);
            }
        } else if (tag instanceof AppInfo) {
            startAppShortcutOrInfoActivity(view, (AppInfo) tag, launcher);
        } else if ((tag instanceof LauncherAppWidgetInfo) && (view instanceof PendingAppWidgetHostView)) {
            onClickPendingWidget((PendingAppWidgetHostView) view, launcher);
        }
    }

    private static void onClickFolderIcon(View view) {
        Folder folder = ((FolderIcon) view).getFolder();
        if (!folder.isOpen() && !folder.isDestroyed()) {
            folder.animateOpen();
        }
    }

    private static void onClickPendingWidget(PendingAppWidgetHostView pendingAppWidgetHostView, Launcher launcher) {
        if (launcher.getPackageManager().isSafeMode()) {
            Toast.makeText(launcher, R.string.safemode_widget_error, 0).show();
            return;
        }
        LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) pendingAppWidgetHostView.getTag();
        if (pendingAppWidgetHostView.isReadyForClickSetup()) {
            LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfoFindProvider = AppWidgetManagerCompat.getInstance(launcher).findProvider(launcherAppWidgetInfo.providerName, launcherAppWidgetInfo.user);
            if (launcherAppWidgetProviderInfoFindProvider == null) {
                return;
            }
            WidgetAddFlowHandler widgetAddFlowHandler = new WidgetAddFlowHandler(launcherAppWidgetProviderInfoFindProvider);
            if (launcherAppWidgetInfo.hasRestoreFlag(1)) {
                if (!launcherAppWidgetInfo.hasRestoreFlag(16)) {
                    return;
                }
                widgetAddFlowHandler.startBindFlow(launcher, launcherAppWidgetInfo.appWidgetId, launcherAppWidgetInfo, 12);
                return;
            }
            widgetAddFlowHandler.startConfigActivity(launcher, launcherAppWidgetInfo, 13);
            return;
        }
        onClickPendingAppItem(pendingAppWidgetHostView, launcher, launcherAppWidgetInfo.providerName.getPackageName(), launcherAppWidgetInfo.installProgress >= 0);
    }

    private static void onClickPendingAppItem(final View view, final Launcher launcher, final String str, boolean z) {
        if (z) {
            startMarketIntentForPackage(view, launcher, str);
        } else {
            new AlertDialog.Builder(launcher).setTitle(R.string.abandoned_promises_title).setMessage(R.string.abandoned_promise_explanation).setPositiveButton(R.string.abandoned_search, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ItemClickHandler.startMarketIntentForPackage(view, launcher, str);
                }
            }).setNeutralButton(R.string.abandoned_clean_this, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    launcher.getWorkspace().removeAbandonedPromise(str, Process.myUserHandle());
                }
            }).create().show();
        }
    }

    public static void startMarketIntentForPackage(View view, Launcher launcher, String str) {
        launcher.startActivitySafely(view, new PackageManagerHelper(launcher).getMarketIntent(str), (ItemInfo) view.getTag());
    }

    private static void onClickAppShortcut(View view, ShortcutInfo shortcutInfo, Launcher launcher) {
        if (shortcutInfo.isDisabled() && (shortcutInfo.runtimeStatusFlags & 63 & (-5) & (-9)) != 0) {
            if (!TextUtils.isEmpty(shortcutInfo.disabledMessage)) {
                Toast.makeText(launcher, shortcutInfo.disabledMessage, 0).show();
                return;
            }
            int i = R.string.activity_not_available;
            if ((shortcutInfo.runtimeStatusFlags & 1) != 0) {
                i = R.string.safemode_shortcut_error;
            } else if ((shortcutInfo.runtimeStatusFlags & 16) != 0 || (shortcutInfo.runtimeStatusFlags & 32) != 0) {
                i = R.string.shortcut_not_available;
            }
            Toast.makeText(launcher, i, 0).show();
            return;
        }
        if ((view instanceof BubbleTextView) && shortcutInfo.hasPromiseIconUi()) {
            String packageName = shortcutInfo.intent.getComponent() != null ? shortcutInfo.intent.getComponent().getPackageName() : shortcutInfo.intent.getPackage();
            if (!TextUtils.isEmpty(packageName)) {
                onClickPendingAppItem(view, launcher, packageName, shortcutInfo.hasStatusFlag(4));
                return;
            }
        }
        startAppShortcutOrInfoActivity(view, shortcutInfo, launcher);
    }

    private static void startAppShortcutOrInfoActivity(View view, ItemInfo itemInfo, Launcher launcher) {
        Intent intent;
        if (itemInfo instanceof PromiseAppInfo) {
            intent = ((PromiseAppInfo) itemInfo).getMarketIntent(launcher);
        } else {
            intent = itemInfo.getIntent();
        }
        if (intent == null) {
            throw new IllegalArgumentException("Input must have a valid intent");
        }
        if ((itemInfo instanceof ShortcutInfo) && ((ShortcutInfo) itemInfo).hasStatusFlag(16) && intent.getAction() == "android.intent.action.VIEW") {
            Intent intent2 = new Intent(intent);
            intent2.setPackage(null);
            intent = intent2;
        }
        launcher.startActivitySafely(view, intent, itemInfo);
    }
}
