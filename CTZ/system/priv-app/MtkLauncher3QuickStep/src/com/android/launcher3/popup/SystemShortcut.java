package com.android.launcher3.popup;

import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.util.InstantAppResolver;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.widget.WidgetsBottomSheet;

/* loaded from: classes.dex */
public abstract class SystemShortcut<T extends BaseDraggingActivity> extends ItemInfo {
    public final int iconResId;
    public final int labelResId;

    public abstract View.OnClickListener getOnClickListener(T t, ItemInfo itemInfo);

    public SystemShortcut(int i, int i2) {
        this.iconResId = i;
        this.labelResId = i2;
    }

    public static class Widgets extends SystemShortcut<Launcher> {
        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        /* JADX DEBUG: Method merged with bridge method: getOnClickListener(Lcom/android/launcher3/BaseDraggingActivity;Lcom/android/launcher3/ItemInfo;)Landroid/view/View$OnClickListener; */
        @Override // com.android.launcher3.popup.SystemShortcut
        public View.OnClickListener getOnClickListener(final Launcher launcher, final ItemInfo itemInfo) {
            if (launcher.getPopupDataProvider().getWidgetsForPackageUser(new PackageUserKey(itemInfo.getTargetComponent().getPackageName(), itemInfo.user)) == null) {
                return null;
            }
            return new View.OnClickListener() { // from class: com.android.launcher3.popup.-$$Lambda$SystemShortcut$Widgets$kv8wGFcliolz3NAO5Ak3G19e-ac
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SystemShortcut.Widgets.lambda$getOnClickListener$0(launcher, itemInfo, view);
                }
            };
        }

        static /* synthetic */ void lambda$getOnClickListener$0(Launcher launcher, ItemInfo itemInfo, View view) {
            AbstractFloatingView.closeAllOpenViews(launcher);
            ((WidgetsBottomSheet) launcher.getLayoutInflater().inflate(R.layout.widgets_bottom_sheet, (ViewGroup) launcher.getDragLayer(), false)).populateAndShow(itemInfo);
            launcher.getUserEventDispatcher().logActionOnControl(0, 2, view);
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override // com.android.launcher3.popup.SystemShortcut
        public View.OnClickListener getOnClickListener(final BaseDraggingActivity baseDraggingActivity, final ItemInfo itemInfo) {
            return new View.OnClickListener() { // from class: com.android.launcher3.popup.-$$Lambda$SystemShortcut$AppInfo$b46WgC1q68gh-bbGdztUSCwTf48
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SystemShortcut.AppInfo.lambda$getOnClickListener$0(baseDraggingActivity, itemInfo, view);
                }
            };
        }

        static /* synthetic */ void lambda$getOnClickListener$0(BaseDraggingActivity baseDraggingActivity, ItemInfo itemInfo, View view) {
            new PackageManagerHelper(baseDraggingActivity).startDetailsActivityForInfo(itemInfo, baseDraggingActivity.getViewBounds(view), baseDraggingActivity.getActivityLaunchOptionsAsBundle(view));
            baseDraggingActivity.getUserEventDispatcher().logActionOnControl(0, 7, view);
        }
    }

    public static class Install extends SystemShortcut {
        public Install() {
            super(R.drawable.ic_install_no_shadow, R.string.install_drop_target_label);
        }

        @Override // com.android.launcher3.popup.SystemShortcut
        public View.OnClickListener getOnClickListener(BaseDraggingActivity baseDraggingActivity, ItemInfo itemInfo) {
            boolean zIsInstantApp;
            boolean z = true;
            boolean z2 = (itemInfo instanceof ShortcutInfo) && ((ShortcutInfo) itemInfo).hasStatusFlag(16);
            if (itemInfo instanceof com.android.launcher3.AppInfo) {
                zIsInstantApp = InstantAppResolver.newInstance(baseDraggingActivity).isInstantApp((com.android.launcher3.AppInfo) itemInfo);
            } else {
                zIsInstantApp = false;
            }
            if (!z2 && !zIsInstantApp) {
                z = false;
            }
            if (!z) {
                return null;
            }
            return createOnClickListener(baseDraggingActivity, itemInfo);
        }

        public View.OnClickListener createOnClickListener(final BaseDraggingActivity baseDraggingActivity, final ItemInfo itemInfo) {
            return new View.OnClickListener() { // from class: com.android.launcher3.popup.-$$Lambda$SystemShortcut$Install$58NbgSBxD2QmaoUGCsQWNJ4UVd8
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SystemShortcut.Install.lambda$createOnClickListener$0(itemInfo, baseDraggingActivity, view);
                }
            };
        }

        static /* synthetic */ void lambda$createOnClickListener$0(ItemInfo itemInfo, BaseDraggingActivity baseDraggingActivity, View view) {
            baseDraggingActivity.startActivitySafely(view, new PackageManagerHelper(view.getContext()).getMarketIntent(itemInfo.getTargetComponent().getPackageName()), itemInfo);
            AbstractFloatingView.closeAllOpenViews(baseDraggingActivity);
        }
    }
}
