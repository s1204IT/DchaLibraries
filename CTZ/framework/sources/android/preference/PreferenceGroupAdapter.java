package android.preference;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferenceGroupAdapter extends BaseAdapter implements Preference.OnPreferenceChangeInternalListener {
    private static final String TAG = "PreferenceGroupAdapter";
    private static ViewGroup.LayoutParams sWrapperLayoutParams = new ViewGroup.LayoutParams(-1, -2);
    private Drawable mHighlightedDrawable;
    private PreferenceGroup mPreferenceGroup;
    private ArrayList<PreferenceLayout> mPreferenceLayouts;
    private List<Preference> mPreferenceList;
    private PreferenceLayout mTempPreferenceLayout = new PreferenceLayout();
    private boolean mHasReturnedViewTypeCount = false;
    private volatile boolean mIsSyncing = false;
    private Handler mHandler = new Handler();
    private Runnable mSyncRunnable = new Runnable() {
        @Override
        public void run() {
            PreferenceGroupAdapter.this.syncMyPreferences();
        }
    };
    private int mHighlightedPosition = -1;

    private static class PreferenceLayout implements Comparable<PreferenceLayout> {
        private String name;
        private int resId;
        private int widgetResId;

        private PreferenceLayout() {
        }

        @Override
        public int compareTo(PreferenceLayout preferenceLayout) {
            int iCompareTo = this.name.compareTo(preferenceLayout.name);
            if (iCompareTo == 0) {
                if (this.resId == preferenceLayout.resId) {
                    if (this.widgetResId == preferenceLayout.widgetResId) {
                        return 0;
                    }
                    return this.widgetResId - preferenceLayout.widgetResId;
                }
                return this.resId - preferenceLayout.resId;
            }
            return iCompareTo;
        }
    }

    public PreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
        this.mPreferenceGroup = preferenceGroup;
        this.mPreferenceGroup.setOnPreferenceChangeInternalListener(this);
        this.mPreferenceList = new ArrayList();
        this.mPreferenceLayouts = new ArrayList<>();
        syncMyPreferences();
    }

    private void syncMyPreferences() {
        synchronized (this) {
            if (this.mIsSyncing) {
                return;
            }
            this.mIsSyncing = true;
            ArrayList arrayList = new ArrayList(this.mPreferenceList.size());
            flattenPreferenceGroup(arrayList, this.mPreferenceGroup);
            this.mPreferenceList = arrayList;
            notifyDataSetChanged();
            synchronized (this) {
                this.mIsSyncing = false;
                notifyAll();
            }
        }
    }

    private void flattenPreferenceGroup(List<Preference> list, PreferenceGroup preferenceGroup) {
        preferenceGroup.sortPreferences();
        int preferenceCount = preferenceGroup.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = preferenceGroup.getPreference(i);
            list.add(preference);
            if (!this.mHasReturnedViewTypeCount && preference.isRecycleEnabled()) {
                addPreferenceClassName(preference);
            }
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup2 = (PreferenceGroup) preference;
                if (preferenceGroup2.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(list, preferenceGroup2);
                }
            }
            preference.setOnPreferenceChangeInternalListener(this);
        }
    }

    private PreferenceLayout createPreferenceLayout(Preference preference, PreferenceLayout preferenceLayout) {
        if (preferenceLayout == null) {
            preferenceLayout = new PreferenceLayout();
        }
        preferenceLayout.name = preference.getClass().getName();
        preferenceLayout.resId = preference.getLayoutResource();
        preferenceLayout.widgetResId = preference.getWidgetLayoutResource();
        return preferenceLayout;
    }

    private void addPreferenceClassName(Preference preference) {
        PreferenceLayout preferenceLayoutCreatePreferenceLayout = createPreferenceLayout(preference, null);
        if (Collections.binarySearch(this.mPreferenceLayouts, preferenceLayoutCreatePreferenceLayout) < 0) {
            this.mPreferenceLayouts.add((r0 * (-1)) - 1, preferenceLayoutCreatePreferenceLayout);
        }
    }

    @Override
    public int getCount() {
        return this.mPreferenceList.size();
    }

    @Override
    public Preference getItem(int i) {
        if (i < 0 || i >= getCount()) {
            return null;
        }
        return this.mPreferenceList.get(i);
    }

    @Override
    public long getItemId(int i) {
        if (i < 0 || i >= getCount()) {
            return Long.MIN_VALUE;
        }
        return getItem(i).getId();
    }

    public void setHighlighted(int i) {
        this.mHighlightedPosition = i;
    }

    public void setHighlightedDrawable(Drawable drawable) {
        this.mHighlightedDrawable = drawable;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Preference item = getItem(i);
        this.mTempPreferenceLayout = createPreferenceLayout(item, this.mTempPreferenceLayout);
        if (Collections.binarySearch(this.mPreferenceLayouts, this.mTempPreferenceLayout) < 0 || getItemViewType(i) == getHighlightItemViewType()) {
            view = null;
        }
        View view2 = item.getView(view, viewGroup);
        if (i == this.mHighlightedPosition && this.mHighlightedDrawable != null) {
            FrameLayout frameLayout = new FrameLayout(viewGroup.getContext());
            frameLayout.setLayoutParams(sWrapperLayoutParams);
            frameLayout.setBackgroundDrawable(this.mHighlightedDrawable);
            frameLayout.addView(view2);
            return frameLayout;
        }
        return view2;
    }

    @Override
    public boolean isEnabled(int i) {
        if (i < 0 || i >= getCount()) {
            return true;
        }
        return getItem(i).isSelectable();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public void onPreferenceChange(Preference preference) {
        notifyDataSetChanged();
    }

    @Override
    public void onPreferenceHierarchyChange(Preference preference) {
        this.mHandler.removeCallbacks(this.mSyncRunnable);
        this.mHandler.post(this.mSyncRunnable);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private int getHighlightItemViewType() {
        return getViewTypeCount() - 1;
    }

    @Override
    public int getItemViewType(int i) {
        if (i == this.mHighlightedPosition) {
            return getHighlightItemViewType();
        }
        if (!this.mHasReturnedViewTypeCount) {
            this.mHasReturnedViewTypeCount = true;
        }
        Preference item = getItem(i);
        if (!item.isRecycleEnabled()) {
            return -1;
        }
        this.mTempPreferenceLayout = createPreferenceLayout(item, this.mTempPreferenceLayout);
        int iBinarySearch = Collections.binarySearch(this.mPreferenceLayouts, this.mTempPreferenceLayout);
        if (iBinarySearch < 0) {
            return -1;
        }
        return iBinarySearch;
    }

    @Override
    public int getViewTypeCount() {
        if (!this.mHasReturnedViewTypeCount) {
            this.mHasReturnedViewTypeCount = true;
        }
        return Math.max(1, this.mPreferenceLayouts.size()) + 1;
    }
}
