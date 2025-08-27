package com.android.launcher3.allapps;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.util.PackageManagerHelper;
import java.util.List;

/* loaded from: classes.dex */
public class AllAppsGridAdapter extends RecyclerView.Adapter<ViewHolder> {
    public static final String TAG = "AppsGridAdapter";
    public static final int VIEW_TYPE_ALL_APPS_DIVIDER = 16;
    public static final int VIEW_TYPE_EMPTY_SEARCH = 4;
    public static final int VIEW_TYPE_ICON = 2;
    public static final int VIEW_TYPE_MASK_DIVIDER = 16;
    public static final int VIEW_TYPE_MASK_ICON = 2;
    public static final int VIEW_TYPE_SEARCH_MARKET = 8;
    public static final int VIEW_TYPE_WORK_TAB_FOOTER = 32;
    private final AlphabeticalAppsList mApps;
    private final int mAppsPerRow;
    private BindViewCallback mBindViewCallback;
    private String mEmptySearchMessage;
    private final GridLayoutManager mGridLayoutMgr;
    private final GridSpanSizer mGridSizer;
    private View.OnFocusChangeListener mIconFocusListener;
    private final Launcher mLauncher;
    private final LayoutInflater mLayoutInflater;
    private Intent mMarketSearchIntent;

    public interface BindViewCallback {
        void onBindView(ViewHolder viewHolder);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    public class AppsGridLayoutManager extends GridLayoutManager {
        public AppsGridLayoutManager(Context context) {
            super(context, 1, 1, false);
        }

        @Override // android.support.v7.widget.LinearLayoutManager, android.support.v7.widget.RecyclerView.LayoutManager
        public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
            super.onInitializeAccessibilityEvent(accessibilityEvent);
            AccessibilityRecordCompat accessibilityRecordCompatAsRecord = AccessibilityEventCompat.asRecord(accessibilityEvent);
            accessibilityRecordCompatAsRecord.setItemCount(AllAppsGridAdapter.this.mApps.getNumFilteredApps());
            accessibilityRecordCompatAsRecord.setFromIndex(Math.max(0, accessibilityRecordCompatAsRecord.getFromIndex() - getRowsNotForAccessibility(accessibilityRecordCompatAsRecord.getFromIndex())));
            accessibilityRecordCompatAsRecord.setToIndex(Math.max(0, accessibilityRecordCompatAsRecord.getToIndex() - getRowsNotForAccessibility(accessibilityRecordCompatAsRecord.getToIndex())));
        }

        @Override // android.support.v7.widget.GridLayoutManager, android.support.v7.widget.RecyclerView.LayoutManager
        public int getRowCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
            return super.getRowCountForAccessibility(recycler, state) - getRowsNotForAccessibility(AllAppsGridAdapter.this.mApps.getAdapterItems().size() - 1);
        }

        @Override // android.support.v7.widget.GridLayoutManager, android.support.v7.widget.RecyclerView.LayoutManager
        public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfoForItem(recycler, state, view, accessibilityNodeInfoCompat);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            AccessibilityNodeInfoCompat.CollectionItemInfoCompat collectionItemInfo = accessibilityNodeInfoCompat.getCollectionItemInfo();
            if (!(layoutParams instanceof GridLayoutManager.LayoutParams) || collectionItemInfo == null) {
                return;
            }
            accessibilityNodeInfoCompat.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(collectionItemInfo.getRowIndex() - getRowsNotForAccessibility(((GridLayoutManager.LayoutParams) layoutParams).getViewAdapterPosition()), collectionItemInfo.getRowSpan(), collectionItemInfo.getColumnIndex(), collectionItemInfo.getColumnSpan(), collectionItemInfo.isHeading(), collectionItemInfo.isSelected()));
        }

        private int getRowsNotForAccessibility(int i) {
            List<AlphabeticalAppsList.AdapterItem> adapterItems = AllAppsGridAdapter.this.mApps.getAdapterItems();
            int iMax = Math.max(i, AllAppsGridAdapter.this.mApps.getAdapterItems().size() - 1);
            int i2 = 0;
            for (int i3 = 0; i3 <= iMax; i3++) {
                if (!AllAppsGridAdapter.isViewType(adapterItems.get(i3).viewType, 2)) {
                    i2++;
                }
            }
            return i2;
        }
    }

    public class GridSpanSizer extends GridLayoutManager.SpanSizeLookup {
        public GridSpanSizer() {
            setSpanIndexCacheEnabled(true);
        }

        @Override // android.support.v7.widget.GridLayoutManager.SpanSizeLookup
        public int getSpanSize(int i) {
            if (!AllAppsGridAdapter.isIconViewType(AllAppsGridAdapter.this.mApps.getAdapterItems().get(i).viewType)) {
                return AllAppsGridAdapter.this.mAppsPerRow;
            }
            return 1;
        }
    }

    public AllAppsGridAdapter(Launcher launcher, AlphabeticalAppsList alphabeticalAppsList) {
        Resources resources = launcher.getResources();
        this.mLauncher = launcher;
        this.mApps = alphabeticalAppsList;
        this.mEmptySearchMessage = resources.getString(R.string.all_apps_loading_message);
        this.mGridSizer = new GridSpanSizer();
        this.mGridLayoutMgr = new AppsGridLayoutManager(launcher);
        this.mGridLayoutMgr.setSpanSizeLookup(this.mGridSizer);
        this.mLayoutInflater = LayoutInflater.from(launcher);
        this.mAppsPerRow = this.mLauncher.getDeviceProfile().inv.numColumns;
        this.mGridLayoutMgr.setSpanCount(this.mAppsPerRow);
    }

    public static boolean isDividerViewType(int i) {
        return isViewType(i, 16);
    }

    public static boolean isIconViewType(int i) {
        return isViewType(i, 2);
    }

    public static boolean isViewType(int i, int i2) {
        return (i & i2) != 0;
    }

    public void setIconFocusListener(View.OnFocusChangeListener onFocusChangeListener) {
        this.mIconFocusListener = onFocusChangeListener;
    }

    public void setLastSearchQuery(String str) {
        this.mEmptySearchMessage = this.mLauncher.getResources().getString(R.string.all_apps_no_search_results, str);
        this.mMarketSearchIntent = PackageManagerHelper.getMarketSearchIntent(this.mLauncher, str);
    }

    public void setBindViewCallback(BindViewCallback bindViewCallback) {
        this.mBindViewCallback = bindViewCallback;
    }

    public GridLayoutManager getLayoutManager() {
        return this.mGridLayoutMgr;
    }

    /* JADX DEBUG: Method merged with bridge method: onCreateViewHolder(Landroid/view/ViewGroup;I)Landroid/support/v7/widget/RecyclerView$ViewHolder; */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        if (i == 2) {
            BubbleTextView bubbleTextView = (BubbleTextView) this.mLayoutInflater.inflate(R.layout.all_apps_icon, viewGroup, false);
            bubbleTextView.setOnClickListener(ItemClickHandler.INSTANCE);
            bubbleTextView.setOnLongClickListener(ItemLongClickListener.INSTANCE_ALL_APPS);
            bubbleTextView.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
            bubbleTextView.setOnFocusChangeListener(this.mIconFocusListener);
            bubbleTextView.getLayoutParams().height = this.mLauncher.getDeviceProfile().allAppsCellHeightPx;
            return new ViewHolder(bubbleTextView);
        }
        if (i == 4) {
            return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_empty_search, viewGroup, false));
        }
        if (i == 8) {
            View viewInflate = this.mLayoutInflater.inflate(R.layout.all_apps_search_market, viewGroup, false);
            viewInflate.setOnClickListener(new View.OnClickListener() { // from class: com.android.launcher3.allapps.AllAppsGridAdapter.1
                AnonymousClass1() {
                }

                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    AllAppsGridAdapter.this.mLauncher.startActivitySafely(view, AllAppsGridAdapter.this.mMarketSearchIntent, null);
                }
            });
            return new ViewHolder(viewInflate);
        }
        if (i == 16) {
            return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_divider, viewGroup, false));
        }
        if (i == 32) {
            return new ViewHolder(this.mLayoutInflater.inflate(R.layout.work_tab_footer, viewGroup, false));
        }
        throw new RuntimeException("Unexpected view type");
    }

    /* renamed from: com.android.launcher3.allapps.AllAppsGridAdapter$1 */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            AllAppsGridAdapter.this.mLauncher.startActivitySafely(view, AllAppsGridAdapter.this.mMarketSearchIntent, null);
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onBindViewHolder(Landroid/support/v7/widget/RecyclerView$ViewHolder;I)V */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        int itemViewType = viewHolder.getItemViewType();
        if (itemViewType == 2) {
            AppInfo appInfo = this.mApps.getAdapterItems().get(i).appInfo;
            BubbleTextView bubbleTextView = (BubbleTextView) viewHolder.itemView;
            bubbleTextView.reset();
            bubbleTextView.applyFromApplicationInfo(appInfo);
        } else if (itemViewType == 4) {
            TextView textView = (TextView) viewHolder.itemView;
            textView.setText(this.mEmptySearchMessage);
            textView.setGravity(this.mApps.hasNoFilteredResults() ? 17 : 8388627);
        } else if (itemViewType == 8) {
            TextView textView2 = (TextView) viewHolder.itemView;
            if (this.mMarketSearchIntent != null) {
                textView2.setVisibility(0);
            } else {
                textView2.setVisibility(8);
            }
        } else if (itemViewType != 16 && itemViewType == 32) {
            ((WorkModeSwitch) viewHolder.itemView.findViewById(R.id.work_mode_toggle)).refresh();
            TextView textView3 = (TextView) viewHolder.itemView.findViewById(R.id.managed_by_label);
            textView3.setText(UserManagerCompat.getInstance(textView3.getContext()).isAnyProfileQuietModeEnabled() ? R.string.work_mode_off_label : R.string.work_mode_on_label);
        }
        if (this.mBindViewCallback != null) {
            this.mBindViewCallback.onBindView(viewHolder);
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onFailedToRecycleView(Landroid/support/v7/widget/RecyclerView$ViewHolder;)Z */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public boolean onFailedToRecycleView(ViewHolder viewHolder) {
        return true;
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mApps.getAdapterItems().size();
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemViewType(int i) {
        return this.mApps.getAdapterItems().get(i).viewType;
    }
}
