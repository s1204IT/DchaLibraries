package com.android.systemui.statusbar.car;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.util.SimpleArrayMap;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.ActivityStarter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CarNavigationBarController {
    private ActivityStarter mActivityStarter;
    private Context mContext;
    private int mCurrentFacetIndex;
    private List<Intent> mIntents;
    private List<Intent> mLongPressIntents;
    private CarNavigationBarView mNavBar;
    private List<String[]> mFacetCategories = new ArrayList();
    private List<String[]> mFacetPackages = new ArrayList();
    private SimpleArrayMap<String, Integer> mFacetCategoryMap = new SimpleArrayMap<>();
    private SimpleArrayMap<String, Integer> mFacetPackageMap = new SimpleArrayMap<>();
    private List<CarNavigationButton> mNavButtons = new ArrayList();
    private SparseBooleanArray mFacetHasMultipleAppsCache = new SparseBooleanArray();

    public CarNavigationBarController(Context context, CarNavigationBarView navBar, ActivityStarter activityStarter) {
        this.mContext = context;
        this.mNavBar = navBar;
        this.mActivityStarter = activityStarter;
        bind();
    }

    public void taskChanged(String packageName) {
        if (this.mFacetPackageMap.containsKey(packageName)) {
            setCurrentFacet(this.mFacetPackageMap.get(packageName).intValue());
        }
        String category = getPackageCategory(packageName);
        if (category == null) {
            return;
        }
        setCurrentFacet(this.mFacetCategoryMap.get(category).intValue());
    }

    public void onPackageChange(String packageName) {
        if (this.mFacetPackageMap.containsKey(packageName)) {
            int index = this.mFacetPackageMap.get(packageName).intValue();
            this.mFacetHasMultipleAppsCache.put(index, facetHasMultiplePackages(index));
            return;
        }
        String category = getPackageCategory(packageName);
        if (!this.mFacetCategoryMap.containsKey(category)) {
            return;
        }
        int index2 = this.mFacetCategoryMap.get(category).intValue();
        this.mFacetHasMultipleAppsCache.put(index2, facetHasMultiplePackages(index2));
    }

    private void bind() {
        Resources r = this.mContext.getResources();
        TypedArray icons = r.obtainTypedArray(R.array.car_facet_icons);
        TypedArray intents = r.obtainTypedArray(R.array.car_facet_intent_uris);
        TypedArray longpressIntents = r.obtainTypedArray(R.array.car_facet_longpress_intent_uris);
        TypedArray facetPackageNames = r.obtainTypedArray(R.array.car_facet_package_filters);
        TypedArray facetCategories = r.obtainTypedArray(R.array.car_facet_category_filters);
        if (icons.length() != intents.length() || icons.length() != longpressIntents.length() || icons.length() != facetPackageNames.length() || icons.length() != facetCategories.length()) {
            throw new RuntimeException("car_facet array lengths do not match");
        }
        this.mIntents = createEmptyIntentList(icons.length());
        this.mLongPressIntents = createEmptyIntentList(icons.length());
        for (int i = 0; i < icons.length(); i++) {
            Drawable icon = icons.getDrawable(i);
            try {
                this.mIntents.set(i, Intent.parseUri(intents.getString(i), 1));
                String longpressUri = longpressIntents.getString(i);
                boolean hasLongpress = !longpressUri.isEmpty();
                if (hasLongpress) {
                    this.mLongPressIntents.set(i, Intent.parseUri(longpressUri, 1));
                }
                CarNavigationButton button = createNavButton(icon, i, hasLongpress);
                this.mNavButtons.add(button);
                this.mNavBar.addButton(button, createNavButton(icon, i, hasLongpress));
                initFacetFilterMaps(i, facetPackageNames.getString(i).split(";"), facetCategories.getString(i).split(";"));
                this.mFacetHasMultipleAppsCache.put(i, facetHasMultiplePackages(i));
            } catch (URISyntaxException e) {
                throw new RuntimeException("Malformed intent uri", e);
            }
        }
    }

    private void initFacetFilterMaps(int id, String[] packageNames, String[] categories) {
        this.mFacetCategories.add(categories);
        for (String str : categories) {
            this.mFacetCategoryMap.put(str, Integer.valueOf(id));
        }
        this.mFacetPackages.add(packageNames);
        for (String str2 : packageNames) {
            this.mFacetPackageMap.put(str2, Integer.valueOf(id));
        }
    }

    private String getPackageCategory(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        int size = this.mFacetCategories.size();
        for (int i = 0; i < size; i++) {
            String[] categories = this.mFacetCategories.get(i);
            for (String category : categories) {
                Intent intent = new Intent();
                intent.setPackage(packageName);
                intent.setAction("android.intent.action.MAIN");
                intent.addCategory(category);
                List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
                if (list.size() > 0) {
                    this.mFacetPackageMap.put(packageName, this.mFacetCategoryMap.get(category));
                    return category;
                }
            }
        }
        return null;
    }

    private boolean facetHasMultiplePackages(int index) {
        PackageManager pm = this.mContext.getPackageManager();
        String[] packages = this.mFacetPackages.get(index);
        if (packages.length > 1) {
            int count = 0;
            for (String str : packages) {
                count += pm.getLaunchIntentForPackage(str) != null ? 1 : 0;
                if (count > 1) {
                    return true;
                }
            }
        }
        String[] categories = this.mFacetCategories.get(index);
        int count2 = 0;
        for (String category : categories) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory(category);
            count2 += pm.queryIntentActivities(intent, 0).size();
            if (count2 > 1) {
                return true;
            }
        }
        return false;
    }

    private void setCurrentFacet(int index) {
        if (index == this.mCurrentFacetIndex) {
            return;
        }
        if (this.mNavButtons.get(this.mCurrentFacetIndex) != null) {
            this.mNavButtons.get(this.mCurrentFacetIndex).setSelected(false, false);
        }
        if (this.mNavButtons.get(index) != null) {
            this.mNavButtons.get(index).setSelected(true, this.mFacetHasMultipleAppsCache.get(index));
        }
        this.mCurrentFacetIndex = index;
    }

    private CarNavigationButton createNavButton(Drawable icon, final int id, boolean longClickEnabled) {
        CarNavigationButton button = (CarNavigationButton) View.inflate(this.mContext, R.layout.car_navigation_button, null);
        button.setResources(icon);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1.0f);
        button.setLayoutParams(lp);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarNavigationBarController.this.onFacetClicked(id);
            }
        });
        if (longClickEnabled) {
            button.setLongClickable(true);
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    CarNavigationBarController.this.onFacetLongClicked(id);
                    return true;
                }
            });
        } else {
            button.setLongClickable(false);
        }
        return button;
    }

    private void startActivity(Intent intent) {
        if (this.mActivityStarter == null || intent == null) {
            return;
        }
        this.mActivityStarter.startActivity(intent, false);
    }

    public void onFacetClicked(int index) {
        Intent intent = this.mIntents.get(index);
        String packageName = intent.getPackage();
        if (packageName == null) {
            return;
        }
        intent.putExtra("categories", this.mFacetCategories.get(index));
        intent.putExtra("packages", this.mFacetPackages.get(index));
        intent.putExtra("filter_id", Integer.toString(index));
        intent.putExtra("launch_picker", index == this.mCurrentFacetIndex);
        setCurrentFacet(index);
        startActivity(intent);
    }

    public void onFacetLongClicked(int index) {
        setCurrentFacet(index);
        startActivity(this.mLongPressIntents.get(index));
    }

    private List<Intent> createEmptyIntentList(int size) {
        return Arrays.asList(new Intent[size]);
    }
}
