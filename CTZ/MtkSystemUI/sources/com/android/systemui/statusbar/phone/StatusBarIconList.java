package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatusBarIconList {
    private ArrayList<Slot> mSlots = new ArrayList<>();
    private ISystemUIStatusBarExt mStatusBarExt;

    public StatusBarIconList(String[] strArr, Context context) {
        this.mStatusBarExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeSystemUIStatusBar(context);
        for (String str : this.mStatusBarExt.addSlot(strArr)) {
            this.mSlots.add(new Slot(str, null));
        }
        if (FeatureOptions.LOG_ENABLE) {
            Log.d("StatusBarIconList", "mSlots: " + this.mSlots);
        }
    }

    public int getSlotIndex(String str) {
        int size = this.mSlots.size();
        for (int i = 0; i < size; i++) {
            if (this.mSlots.get(i).getName().equals(str)) {
                return i;
            }
        }
        this.mSlots.add(0, new Slot(str, null));
        return 0;
    }

    protected ArrayList<Slot> getSlots() {
        return new ArrayList<>(this.mSlots);
    }

    protected Slot getSlot(String str) {
        return this.mSlots.get(getSlotIndex(str));
    }

    public void setIcon(int i, StatusBarIconHolder statusBarIconHolder) {
        this.mSlots.get(i).addHolder(statusBarIconHolder);
    }

    public void removeIcon(int i, int i2) {
        this.mSlots.get(i).removeForTag(i2);
    }

    public String getSlotName(int i) {
        return this.mSlots.get(i).getName();
    }

    public StatusBarIconHolder getIcon(int i, int i2) {
        return this.mSlots.get(i).getHolderForTag(i2);
    }

    public int getViewIndex(int i, int i2) {
        int iNumberOfIcons = 0;
        for (int i3 = 0; i3 < i; i3++) {
            Slot slot = this.mSlots.get(i3);
            if (slot.hasIconsInSlot()) {
                iNumberOfIcons += slot.numberOfIcons();
            }
        }
        return iNumberOfIcons + this.mSlots.get(i).viewIndexOffsetForTag(i2);
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("StatusBarIconList state:");
        int size = this.mSlots.size();
        printWriter.println("  icon slots: " + size);
        for (int i = 0; i < size; i++) {
            printWriter.printf("    %2d:%s\n", Integer.valueOf(i), this.mSlots.get(i).toString());
        }
    }

    public static class Slot {
        Comparator c = new Comparator<StatusBarIconHolder>() {
            @Override
            public int compare(StatusBarIconHolder statusBarIconHolder, StatusBarIconHolder statusBarIconHolder2) {
                if (statusBarIconHolder.getType() != 2 || statusBarIconHolder2.getType() != 2) {
                    return 0;
                }
                if (statusBarIconHolder.getMobileState().phoneId < statusBarIconHolder2.getMobileState().phoneId) {
                    return 1;
                }
                return -1;
            }
        };
        private StatusBarIconHolder mHolder;
        private final String mName;
        private ArrayList<StatusBarIconHolder> mSubSlots;

        public Slot(String str, StatusBarIconHolder statusBarIconHolder) {
            this.mName = str;
            this.mHolder = statusBarIconHolder;
        }

        public String getName() {
            return this.mName;
        }

        public StatusBarIconHolder getHolderForTag(int i) {
            if (i == 0) {
                return this.mHolder;
            }
            if (this.mSubSlots != null) {
                for (StatusBarIconHolder statusBarIconHolder : this.mSubSlots) {
                    if (statusBarIconHolder.getTag() == i) {
                        return statusBarIconHolder;
                    }
                }
                return null;
            }
            return null;
        }

        public void addHolder(StatusBarIconHolder statusBarIconHolder) {
            int tag = statusBarIconHolder.getTag();
            if (tag == 0) {
                this.mHolder = statusBarIconHolder;
            } else {
                setSubSlot(statusBarIconHolder, tag);
            }
        }

        public void removeForTag(int i) {
            if (i == 0) {
                this.mHolder = null;
                return;
            }
            int indexForTag = getIndexForTag(i);
            if (indexForTag != -1) {
                this.mSubSlots.remove(indexForTag);
            }
        }

        @VisibleForTesting
        public void clear() {
            this.mHolder = null;
            if (this.mSubSlots != null) {
                this.mSubSlots = null;
            }
        }

        private void setSubSlot(StatusBarIconHolder statusBarIconHolder, int i) {
            if (this.mSubSlots == null) {
                this.mSubSlots = new ArrayList<>();
                this.mSubSlots.add(statusBarIconHolder);
            } else {
                if (getIndexForTag(i) != -1) {
                    return;
                }
                this.mSubSlots.add(statusBarIconHolder);
                Collections.sort(this.mSubSlots, this.c);
            }
        }

        private int getIndexForTag(int i) {
            for (int i2 = 0; i2 < this.mSubSlots.size(); i2++) {
                if (this.mSubSlots.get(i2).getTag() == i) {
                    return i2;
                }
            }
            return -1;
        }

        public boolean hasIconsInSlot() {
            if (this.mHolder != null) {
                return true;
            }
            return this.mSubSlots != null && this.mSubSlots.size() > 0;
        }

        public int numberOfIcons() {
            int i = this.mHolder == null ? 0 : 1;
            return this.mSubSlots == null ? i : i + this.mSubSlots.size();
        }

        public int viewIndexOffsetForTag(int i) {
            if (this.mSubSlots == null) {
                return 0;
            }
            int size = this.mSubSlots.size();
            if (i == 0) {
                return size;
            }
            return (size - getIndexForTag(i)) - 1;
        }

        public List<StatusBarIconHolder> getHolderListInViewOrder() {
            ArrayList arrayList = new ArrayList();
            if (this.mSubSlots != null) {
                for (int size = this.mSubSlots.size() - 1; size >= 0; size--) {
                    arrayList.add(this.mSubSlots.get(size));
                }
            }
            if (this.mHolder != null) {
                arrayList.add(this.mHolder);
            }
            return arrayList;
        }

        public String toString() {
            return String.format("(%s) %s", this.mName, subSlotsString());
        }

        private String subSlotsString() {
            if (this.mSubSlots == null) {
                return "";
            }
            return "" + this.mSubSlots.size() + " subSlots";
        }
    }
}
