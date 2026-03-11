package com.android.systemui.statusbar.phone;

import android.R;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.ViewGroup;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarIconList;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.IconLogger;
import com.android.systemui.tuner.TunerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class StatusBarIconControllerImpl extends StatusBarIconList implements Dumpable, CommandQueue.Callbacks, StatusBarIconController, ConfigurationController.ConfigurationListener, TunerService.Tunable {
    private Context mContext;
    private final ArraySet<String> mIconBlacklist;
    private final ArrayList<StatusBarIconController.IconManager> mIconGroups;
    private final IconLogger mIconLogger;
    private boolean mIsDark;

    public StatusBarIconControllerImpl(Context context) {
        super(context.getResources().getStringArray(R.array.config_deviceSpecificSystemServices), context);
        this.mIconGroups = new ArrayList<>();
        this.mIconBlacklist = new ArraySet<>();
        this.mIconLogger = (IconLogger) Dependency.get(IconLogger.class);
        this.mIsDark = false;
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mContext = context;
        loadDimens();
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).addCallbacks(this);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
    }

    @Override
    public void addIconGroup(StatusBarIconController.IconManager iconManager) {
        this.mIconGroups.add(iconManager);
        ArrayList<StatusBarIconList.Slot> slots = getSlots();
        for (int i = 0; i < slots.size(); i++) {
            StatusBarIconList.Slot slot = slots.get(i);
            List<StatusBarIconHolder> holderListInViewOrder = slot.getHolderListInViewOrder();
            boolean zContains = this.mIconBlacklist.contains(slot.getName());
            for (StatusBarIconHolder statusBarIconHolder : holderListInViewOrder) {
                statusBarIconHolder.getTag();
                iconManager.onIconAdded(getViewIndex(getSlotIndex(slot.getName()), statusBarIconHolder.getTag()), slot.getName(), zContains, statusBarIconHolder);
            }
        }
    }

    @Override
    public void removeIconGroup(StatusBarIconController.IconManager iconManager) {
        iconManager.destroy();
        this.mIconGroups.remove(iconManager);
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        if (!"icon_blacklist".equals(str)) {
            return;
        }
        this.mIconBlacklist.clear();
        this.mIconBlacklist.addAll((ArraySet<? extends String>) StatusBarIconController.getIconBlacklist(str2));
        ArrayList<StatusBarIconList.Slot> slots = getSlots();
        ArrayMap arrayMap = new ArrayMap();
        for (int size = slots.size() - 1; size >= 0; size--) {
            StatusBarIconList.Slot slot = slots.get(size);
            arrayMap.put(slot, slot.getHolderListInViewOrder());
            removeAllIconsForSlot(slot.getName());
        }
        for (int i = 0; i < slots.size(); i++) {
            StatusBarIconList.Slot slot2 = slots.get(i);
            List list = (List) arrayMap.get(slot2);
            if (list != null) {
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    setIcon(getSlotIndex(slot2.getName()), (StatusBarIconHolder) it.next());
                }
            }
        }
    }

    private void loadDimens() {
    }

    private void addSystemIcon(int i, final StatusBarIconHolder statusBarIconHolder) {
        final String slotName = getSlotName(i);
        final int viewIndex = getViewIndex(i, statusBarIconHolder.getTag());
        final boolean zContains = this.mIconBlacklist.contains(slotName);
        this.mIconLogger.onIconVisibility(getSlotName(i), statusBarIconHolder.isVisible());
        this.mIconGroups.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((StatusBarIconController.IconManager) obj).onIconAdded(viewIndex, slotName, zContains, statusBarIconHolder);
            }
        });
    }

    @Override
    public void setIcon(String str, int i, CharSequence charSequence) {
        int slotIndex = getSlotIndex(str);
        StatusBarIconHolder icon = getIcon(slotIndex, 0);
        if (icon == null) {
            setIcon(slotIndex, StatusBarIconHolder.fromIcon(new StatusBarIcon(UserHandle.SYSTEM, this.mContext.getPackageName(), Icon.createWithResource(this.mContext, i), 0, 0, charSequence)));
            return;
        }
        icon.getIcon().icon = Icon.createWithResource(this.mContext, i);
        icon.getIcon().contentDescription = charSequence;
        handleSet(slotIndex, icon);
    }

    @Override
    public void setSignalIcon(String str, StatusBarSignalPolicy.WifiIconState wifiIconState) {
        int slotIndex = getSlotIndex(str);
        if (wifiIconState == null) {
            removeIcon(slotIndex, 0);
            return;
        }
        StatusBarIconHolder icon = getIcon(slotIndex, 0);
        if (icon == null) {
            setIcon(slotIndex, StatusBarIconHolder.fromWifiIconState(wifiIconState));
        } else {
            icon.setWifiState(wifiIconState);
            handleSet(slotIndex, icon);
        }
    }

    @Override
    public void setMobileIcons(String str, List<StatusBarSignalPolicy.MobileIconState> list) {
        StatusBarIconList.Slot slot = getSlot(str);
        int slotIndex = getSlotIndex(str);
        for (StatusBarSignalPolicy.MobileIconState mobileIconState : list) {
            StatusBarIconHolder holderForTag = slot.getHolderForTag(mobileIconState.subId);
            if (holderForTag == null) {
                setIcon(slotIndex, StatusBarIconHolder.fromMobileIconState(mobileIconState));
            } else {
                holderForTag.setMobileState(mobileIconState);
                handleSet(slotIndex, holderForTag);
            }
        }
    }

    @Override
    public void setExternalIcon(String str) {
        final int viewIndex = getViewIndex(getSlotIndex(str), 0);
        final int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_icon_drawing_size);
        this.mIconGroups.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((StatusBarIconController.IconManager) obj).onIconExternal(viewIndex, dimensionPixelSize);
            }
        });
    }

    @Override
    public void setIcon(String str, StatusBarIcon statusBarIcon) {
        setIcon(getSlotIndex(str), statusBarIcon);
    }

    private void setIcon(int i, StatusBarIcon statusBarIcon) {
        if (statusBarIcon == null) {
            removeAllIconsForSlot(getSlotName(i));
        } else {
            setIcon(i, StatusBarIconHolder.fromIcon(statusBarIcon));
        }
    }

    @Override
    public void setIcon(int i, StatusBarIconHolder statusBarIconHolder) {
        boolean z = getIcon(i, statusBarIconHolder.getTag()) == null;
        super.setIcon(i, statusBarIconHolder);
        if (z) {
            addSystemIcon(i, statusBarIconHolder);
        } else {
            handleSet(i, statusBarIconHolder);
        }
    }

    @Override
    public void setIconVisibility(String str, boolean z) {
        int slotIndex = getSlotIndex(str);
        StatusBarIconHolder icon = getIcon(slotIndex, 0);
        if (icon == null || icon.isVisible() == z) {
            return;
        }
        icon.setVisible(z);
        handleSet(slotIndex, icon);
    }

    @Override
    public void removeIcon(String str) {
        removeAllIconsForSlot(str);
    }

    @Override
    public void removeAllIconsForSlot(String str) {
        StatusBarIconList.Slot slot = getSlot(str);
        if (!slot.hasIconsInSlot()) {
            return;
        }
        this.mIconLogger.onIconHidden(str);
        int slotIndex = getSlotIndex(str);
        for (StatusBarIconHolder statusBarIconHolder : slot.getHolderListInViewOrder()) {
            final int viewIndex = getViewIndex(slotIndex, statusBarIconHolder.getTag());
            slot.removeForTag(statusBarIconHolder.getTag());
            this.mIconGroups.forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((StatusBarIconController.IconManager) obj).onRemoveIcon(viewIndex);
                }
            });
        }
    }

    @Override
    public void removeIcon(int i, int i2) {
        if (getIcon(i, i2) == null) {
            return;
        }
        this.mIconLogger.onIconHidden(getSlotName(i));
        super.removeIcon(i, i2);
        final int viewIndex = getViewIndex(i, 0);
        this.mIconGroups.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((StatusBarIconController.IconManager) obj).onRemoveIcon(viewIndex);
            }
        });
    }

    private void handleSet(int i, final StatusBarIconHolder statusBarIconHolder) {
        final int viewIndex = getViewIndex(i, statusBarIconHolder.getTag());
        this.mIconLogger.onIconVisibility(getSlotName(i), statusBarIconHolder.isVisible());
        this.mIconGroups.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((StatusBarIconController.IconManager) obj).onSetIconHolder(viewIndex, statusBarIconHolder);
            }
        });
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("StatusBarIconController state:");
        for (StatusBarIconController.IconManager iconManager : this.mIconGroups) {
            if (iconManager.shouldLog()) {
                ViewGroup viewGroup = iconManager.mGroup;
                int childCount = viewGroup.getChildCount();
                printWriter.println("  icon views: " + childCount);
                for (int i = 0; i < childCount; i++) {
                    printWriter.println("    [" + i + "] icon=" + ((StatusIconDisplayable) viewGroup.getChildAt(i)));
                }
            }
        }
        super.dump(printWriter);
    }

    public void dispatchDemoCommand(String str, Bundle bundle) {
        for (StatusBarIconController.IconManager iconManager : this.mIconGroups) {
            if (iconManager.isDemoable()) {
                iconManager.dispatchDemoCommand(str, bundle);
            }
        }
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        loadDimens();
    }
}
