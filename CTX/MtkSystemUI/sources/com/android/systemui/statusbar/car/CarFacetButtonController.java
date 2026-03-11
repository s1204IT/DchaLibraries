package com.android.systemui.statusbar.car;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.Display;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CarFacetButtonController {
    protected Context mContext;
    protected CarFacetButton mSelectedFacetButton;
    protected HashMap<String, CarFacetButton> mButtonsByCategory = new HashMap<>();
    protected HashMap<String, CarFacetButton> mButtonsByPackage = new HashMap<>();
    protected HashMap<String, CarFacetButton> mButtonsByComponentName = new HashMap<>();

    public CarFacetButtonController(Context context) {
        this.mContext = context;
    }

    public void addFacetButton(CarFacetButton carFacetButton) {
        for (String str : carFacetButton.getCategories()) {
            this.mButtonsByCategory.put(str, carFacetButton);
        }
        for (String str2 : carFacetButton.getFacetPackages()) {
            this.mButtonsByPackage.put(str2, carFacetButton);
        }
        for (String str3 : carFacetButton.getComponentName()) {
            this.mButtonsByComponentName.put(str3, carFacetButton);
        }
        this.mSelectedFacetButton = carFacetButton;
    }

    public void removeAll() {
        this.mButtonsByCategory.clear();
        this.mButtonsByPackage.clear();
        this.mButtonsByComponentName.clear();
        this.mSelectedFacetButton = null;
    }

    public void taskChanged(List<ActivityManager.StackInfo> list) {
        ActivityManager.StackInfo next;
        String packageCategory;
        int displayId = getDisplayId();
        Iterator<ActivityManager.StackInfo> it = list.iterator();
        while (true) {
            if (it.hasNext()) {
                next = it.next();
                if (displayId == -1 || displayId == next.displayId) {
                    if (next.topActivity != null) {
                        break;
                    }
                }
            } else {
                next = null;
                break;
            }
        }
        if (next == null) {
            return;
        }
        if (this.mSelectedFacetButton != null) {
            this.mSelectedFacetButton.setSelected(false);
        }
        String packageName = next.topActivity.getPackageName();
        CarFacetButton carFacetButtonFindFacetButtongByComponentName = findFacetButtongByComponentName(next.topActivity);
        if (carFacetButtonFindFacetButtongByComponentName == null) {
            carFacetButtonFindFacetButtongByComponentName = this.mButtonsByPackage.get(packageName);
        }
        if (carFacetButtonFindFacetButtongByComponentName == null && (packageCategory = getPackageCategory(packageName)) != null) {
            carFacetButtonFindFacetButtongByComponentName = this.mButtonsByCategory.get(packageCategory);
        }
        if (carFacetButtonFindFacetButtongByComponentName != null && carFacetButtonFindFacetButtongByComponentName.getVisibility() == 0) {
            carFacetButtonFindFacetButtongByComponentName.setSelected(true);
            this.mSelectedFacetButton = carFacetButtonFindFacetButtongByComponentName;
        }
    }

    private int getDisplayId() {
        Display display;
        if (this.mSelectedFacetButton != null && (display = this.mSelectedFacetButton.getDisplay()) != null) {
            return display.getDisplayId();
        }
        return -1;
    }

    private CarFacetButton findFacetButtongByComponentName(ComponentName componentName) {
        CarFacetButton carFacetButton = this.mButtonsByComponentName.get(componentName.flattenToShortString());
        if (carFacetButton != null) {
            return carFacetButton;
        }
        return this.mButtonsByComponentName.get(componentName.flattenToString());
    }

    protected String getPackageCategory(String str) {
        PackageManager packageManager = this.mContext.getPackageManager();
        for (String str2 : this.mButtonsByCategory.keySet()) {
            Intent intent = new Intent();
            intent.setPackage(str);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory(str2);
            if (packageManager.queryIntentActivities(intent, 0).size() > 0) {
                this.mButtonsByPackage.put(str, this.mButtonsByCategory.get(str2));
                return str2;
            }
        }
        return null;
    }
}
