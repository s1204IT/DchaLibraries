package com.android.launcher3.allapps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
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
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.compat.PackageInstallerCompat;
import java.util.HashMap;
import java.util.List;

public class AllAppsGridAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final AlphabeticalAppsList mApps;
    private int mAppsPerRow;
    private final Rect mBackgroundPadding = new Rect();
    private BindViewCallback mBindViewCallback;
    private String mEmptySearchMessage;
    private final GridLayoutManager mGridLayoutMgr;
    private final GridSpanSizer mGridSizer;
    private final View.OnClickListener mIconClickListener;
    private final View.OnLongClickListener mIconLongClickListener;
    private final boolean mIsRtl;
    private final GridItemDecoration mItemDecoration;
    private final Launcher mLauncher;
    private final LayoutInflater mLayoutInflater;
    private String mMarketAppName;
    private Intent mMarketSearchIntent;
    private String mMarketSearchMessage;
    private final Paint mPredictedAppsDividerPaint;
    private final int mPredictionBarDividerOffset;
    private AllAppsSearchBarController mSearchController;
    private final int mSectionHeaderOffset;
    private final int mSectionNamesMargin;
    private final Paint mSectionTextPaint;
    private final View.OnTouchListener mTouchListener;

    public interface BindViewCallback {
        void onBindView(ViewHolder viewHolder);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View mContent;

        public ViewHolder(View v) {
            super(v);
            this.mContent = v;
        }
    }

    public class AppsGridLayoutManager extends GridLayoutManager {
        public AppsGridLayoutManager(Context context) {
            super(context, 1, 1, false);
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            AccessibilityRecordCompat record = AccessibilityEventCompat.asRecord(event);
            int numEmptyNode = getEmptyRowForAccessibility(-1);
            record.setFromIndex(event.getFromIndex() - numEmptyNode);
            record.setToIndex(event.getToIndex() - numEmptyNode);
            record.setItemCount(AllAppsGridAdapter.this.mApps.getNumFilteredApps());
        }

        @Override
        public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
            int viewType = getItemViewType(host);
            if (viewType != 1 && viewType != 2) {
                return;
            }
            super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);
            AccessibilityNodeInfoCompat.CollectionItemInfoCompat itemInfo = info.getCollectionItemInfo();
            if (itemInfo == null) {
                return;
            }
            AccessibilityNodeInfoCompat.CollectionItemInfoCompat dstItemInfo = AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(itemInfo.getRowIndex() - getEmptyRowForAccessibility(viewType), itemInfo.getRowSpan(), itemInfo.getColumnIndex(), itemInfo.getColumnSpan(), itemInfo.isHeading(), itemInfo.isSelected());
            info.setCollectionItemInfo(dstItemInfo);
        }

        @Override
        public int getRowCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
            return super.getRowCountForAccessibility(recycler, state) - getEmptyRowForAccessibility(-1);
        }

        private int getEmptyRowForAccessibility(int viewType) {
            if (AllAppsGridAdapter.this.mApps.hasFilter()) {
                return 1;
            }
            if (AllAppsGridAdapter.this.mApps.hasPredictedComponents()) {
                if (viewType == 2 || viewType != 1) {
                    return 1;
                }
                return 2;
            }
            if (viewType != 1) {
                return 1;
            }
            return 1;
        }
    }

    public class GridSpanSizer extends GridLayoutManager.SpanSizeLookup {
        public GridSpanSizer() {
            setSpanIndexCacheEnabled(true);
        }

        @Override
        public int getSpanSize(int position) {
            switch (AllAppsGridAdapter.this.mApps.getAdapterItems().get(position).viewType) {
                case PackageInstallerCompat.STATUS_INSTALLING:
                case PackageInstallerCompat.STATUS_FAILED:
                    return 1;
                default:
                    return AllAppsGridAdapter.this.mAppsPerRow;
            }
        }
    }

    public class GridItemDecoration extends RecyclerView.ItemDecoration {
        private HashMap<String, PointF> mCachedSectionBounds = new HashMap<>();
        private Rect mTmpBounds = new Rect();

        public GridItemDecoration() {
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int x;
            if (AllAppsGridAdapter.this.mApps.hasFilter() || AllAppsGridAdapter.this.mAppsPerRow == 0) {
                return;
            }
            List<AlphabeticalAppsList.AdapterItem> items = AllAppsGridAdapter.this.mApps.getAdapterItems();
            boolean hasDrawnPredictedAppsDivider = false;
            boolean showSectionNames = AllAppsGridAdapter.this.mSectionNamesMargin > 0;
            int childCount = parent.getChildCount();
            int lastSectionTop = 0;
            int lastSectionHeight = 0;
            int i = 0;
            while (i < childCount) {
                View child = parent.getChildAt(i);
                ViewHolder holder = (ViewHolder) parent.getChildViewHolder(child);
                if (isValidHolderAndChild(holder, child, items)) {
                    if (shouldDrawItemDivider(holder, items) && !hasDrawnPredictedAppsDivider) {
                        int top = child.getTop() + child.getHeight() + AllAppsGridAdapter.this.mPredictionBarDividerOffset;
                        c.drawLine(AllAppsGridAdapter.this.mBackgroundPadding.left, top, parent.getWidth() - AllAppsGridAdapter.this.mBackgroundPadding.right, top, AllAppsGridAdapter.this.mPredictedAppsDividerPaint);
                        hasDrawnPredictedAppsDivider = true;
                    } else if (showSectionNames && shouldDrawItemSection(holder, i, items)) {
                        int viewTopOffset = child.getPaddingTop() * 2;
                        int pos = holder.getPosition();
                        AlphabeticalAppsList.AdapterItem item = items.get(pos);
                        AlphabeticalAppsList.SectionInfo sectionInfo = item.sectionInfo;
                        String lastSectionName = item.sectionName;
                        int j = item.sectionAppIndex;
                        while (j < sectionInfo.numApps) {
                            AlphabeticalAppsList.AdapterItem nextItem = items.get(pos);
                            String sectionName = nextItem.sectionName;
                            if (nextItem.sectionInfo != sectionInfo) {
                                break;
                            }
                            if (j <= item.sectionAppIndex || !sectionName.equals(lastSectionName)) {
                                PointF sectionBounds = getAndCacheSectionBounds(sectionName);
                                int sectionBaseline = (int) (viewTopOffset + sectionBounds.y);
                                if (AllAppsGridAdapter.this.mIsRtl) {
                                    x = (parent.getWidth() - AllAppsGridAdapter.this.mBackgroundPadding.left) - AllAppsGridAdapter.this.mSectionNamesMargin;
                                } else {
                                    x = AllAppsGridAdapter.this.mBackgroundPadding.left;
                                }
                                int x2 = x + ((int) ((AllAppsGridAdapter.this.mSectionNamesMargin - sectionBounds.x) / 2.0f));
                                int y = child.getTop() + sectionBaseline;
                                int appIndexInSection = items.get(pos).sectionAppIndex;
                                int nextRowPos = Math.min(items.size() - 1, (AllAppsGridAdapter.this.mAppsPerRow + pos) - (appIndexInSection % AllAppsGridAdapter.this.mAppsPerRow));
                                AlphabeticalAppsList.AdapterItem nextRowItem = items.get(nextRowPos);
                                boolean fixedToRow = !sectionName.equals(nextRowItem.sectionName);
                                if (!fixedToRow) {
                                    y = Math.max(sectionBaseline, y);
                                }
                                if (lastSectionHeight > 0 && y <= lastSectionTop + lastSectionHeight) {
                                    y += (lastSectionTop - y) + lastSectionHeight;
                                }
                                c.drawText(sectionName, x2, y, AllAppsGridAdapter.this.mSectionTextPaint);
                                lastSectionTop = y;
                                lastSectionHeight = (int) (sectionBounds.y + AllAppsGridAdapter.this.mSectionHeaderOffset);
                                lastSectionName = sectionName;
                            }
                            j++;
                            pos++;
                        }
                        i += sectionInfo.numApps - item.sectionAppIndex;
                    }
                }
                i++;
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        }

        private PointF getAndCacheSectionBounds(String sectionName) {
            PointF bounds = this.mCachedSectionBounds.get(sectionName);
            if (bounds == null) {
                AllAppsGridAdapter.this.mSectionTextPaint.getTextBounds(sectionName, 0, sectionName.length(), this.mTmpBounds);
                PointF bounds2 = new PointF(AllAppsGridAdapter.this.mSectionTextPaint.measureText(sectionName), this.mTmpBounds.height());
                this.mCachedSectionBounds.put(sectionName, bounds2);
                return bounds2;
            }
            return bounds;
        }

        private boolean isValidHolderAndChild(ViewHolder holder, View child, List<AlphabeticalAppsList.AdapterItem> items) {
            int pos;
            GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams) child.getLayoutParams();
            return !lp.isItemRemoved() && holder != null && (pos = holder.getPosition()) >= 0 && pos < items.size();
        }

        private boolean shouldDrawItemDivider(ViewHolder holder, List<AlphabeticalAppsList.AdapterItem> items) {
            int pos = holder.getPosition();
            return items.get(pos).viewType == 2;
        }

        private boolean shouldDrawItemSection(ViewHolder holder, int childIndex, List<AlphabeticalAppsList.AdapterItem> items) {
            int pos = holder.getPosition();
            AlphabeticalAppsList.AdapterItem item = items.get(pos);
            if (item.viewType != 1) {
                return false;
            }
            if (childIndex != 0 && items.get(pos - 1).viewType != 0) {
                return false;
            }
            return true;
        }
    }

    public AllAppsGridAdapter(Launcher launcher, AlphabeticalAppsList apps, View.OnTouchListener touchListener, View.OnClickListener iconClickListener, View.OnLongClickListener iconLongClickListener) {
        Resources res = launcher.getResources();
        this.mLauncher = launcher;
        this.mApps = apps;
        this.mEmptySearchMessage = res.getString(R.string.all_apps_loading_message);
        this.mGridSizer = new GridSpanSizer();
        this.mGridLayoutMgr = new AppsGridLayoutManager(launcher);
        this.mGridLayoutMgr.setSpanSizeLookup(this.mGridSizer);
        this.mItemDecoration = new GridItemDecoration();
        this.mLayoutInflater = LayoutInflater.from(launcher);
        this.mTouchListener = touchListener;
        this.mIconClickListener = iconClickListener;
        this.mIconLongClickListener = iconLongClickListener;
        this.mSectionNamesMargin = res.getDimensionPixelSize(R.dimen.all_apps_grid_view_start_margin);
        this.mSectionHeaderOffset = res.getDimensionPixelSize(R.dimen.all_apps_grid_section_y_offset);
        this.mIsRtl = Utilities.isRtl(res);
        this.mSectionTextPaint = new Paint(1);
        this.mSectionTextPaint.setTextSize(res.getDimensionPixelSize(R.dimen.all_apps_grid_section_text_size));
        this.mSectionTextPaint.setColor(res.getColor(R.color.all_apps_grid_section_text_color));
        this.mPredictedAppsDividerPaint = new Paint(1);
        this.mPredictedAppsDividerPaint.setStrokeWidth(Utilities.pxFromDp(1.0f, res.getDisplayMetrics()));
        this.mPredictedAppsDividerPaint.setColor(503316480);
        this.mPredictionBarDividerOffset = ((-res.getDimensionPixelSize(R.dimen.all_apps_prediction_icon_bottom_padding)) + res.getDimensionPixelSize(R.dimen.all_apps_icon_top_bottom_padding)) / 2;
    }

    public void setNumAppsPerRow(int appsPerRow) {
        this.mAppsPerRow = appsPerRow;
        this.mGridLayoutMgr.setSpanCount(appsPerRow);
    }

    public void setSearchController(AllAppsSearchBarController searchController) {
        this.mSearchController = searchController;
        PackageManager pm = this.mLauncher.getPackageManager();
        ResolveInfo marketInfo = pm.resolveActivity(this.mSearchController.createMarketSearchIntent(""), 65536);
        if (marketInfo == null) {
            return;
        }
        this.mMarketAppName = marketInfo.loadLabel(pm).toString();
    }

    public void setLastSearchQuery(String query) {
        Resources res = this.mLauncher.getResources();
        String formatStr = res.getString(R.string.all_apps_no_search_results);
        this.mEmptySearchMessage = String.format(formatStr, query);
        if (this.mMarketAppName == null) {
            return;
        }
        this.mMarketSearchMessage = String.format(res.getString(R.string.all_apps_search_market_message), this.mMarketAppName);
        this.mMarketSearchIntent = this.mSearchController.createMarketSearchIntent(query);
    }

    public void setBindViewCallback(BindViewCallback cb) {
        this.mBindViewCallback = cb;
    }

    public void updateBackgroundPadding(Rect padding) {
        this.mBackgroundPadding.set(padding);
    }

    public GridLayoutManager getLayoutManager() {
        return this.mGridLayoutMgr;
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return this.mItemDecoration;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                return new ViewHolder(new View(parent.getContext()));
            case PackageInstallerCompat.STATUS_INSTALLING:
                BubbleTextView icon = (BubbleTextView) this.mLayoutInflater.inflate(R.layout.all_apps_icon, parent, false);
                icon.setOnTouchListener(this.mTouchListener);
                icon.setOnClickListener(this.mIconClickListener);
                icon.setOnLongClickListener(this.mIconLongClickListener);
                ViewConfiguration.get(parent.getContext());
                icon.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
                icon.setFocusable(true);
                return new ViewHolder(icon);
            case PackageInstallerCompat.STATUS_FAILED:
                BubbleTextView icon2 = (BubbleTextView) this.mLayoutInflater.inflate(R.layout.all_apps_prediction_bar_icon, parent, false);
                icon2.setOnTouchListener(this.mTouchListener);
                icon2.setOnClickListener(this.mIconClickListener);
                icon2.setOnLongClickListener(this.mIconLongClickListener);
                ViewConfiguration.get(parent.getContext());
                icon2.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
                icon2.setFocusable(true);
                return new ViewHolder(icon2);
            case 3:
                return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_empty_search, parent, false));
            case 4:
                return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_search_market_divider, parent, false));
            case 5:
                View searchMarketView = this.mLayoutInflater.inflate(R.layout.all_apps_search_market, parent, false);
                searchMarketView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AllAppsGridAdapter.this.mLauncher.startActivitySafely(v, AllAppsGridAdapter.this.mMarketSearchIntent, null);
                    }
                });
                return new ViewHolder(searchMarketView);
            default:
                throw new RuntimeException("Unexpected view type");
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case PackageInstallerCompat.STATUS_INSTALLING:
                AppInfo info = this.mApps.getAdapterItems().get(position).appInfo;
                BubbleTextView icon = (BubbleTextView) holder.mContent;
                icon.applyFromApplicationInfo(info);
                icon.setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
                break;
            case PackageInstallerCompat.STATUS_FAILED:
                AppInfo info2 = this.mApps.getAdapterItems().get(position).appInfo;
                BubbleTextView icon2 = (BubbleTextView) holder.mContent;
                icon2.applyFromApplicationInfo(info2);
                icon2.setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
                break;
            case 3:
                TextView emptyViewText = (TextView) holder.mContent;
                emptyViewText.setText(this.mEmptySearchMessage);
                emptyViewText.setGravity(this.mApps.hasNoFilteredResults() ? 17 : 8388627);
                break;
            case 5:
                TextView searchView = (TextView) holder.mContent;
                if (this.mMarketSearchIntent != null) {
                    searchView.setVisibility(0);
                    searchView.setContentDescription(this.mMarketSearchMessage);
                    searchView.setGravity(this.mApps.hasNoFilteredResults() ? 17 : 8388627);
                    searchView.setText(this.mMarketSearchMessage);
                } else {
                    searchView.setVisibility(8);
                }
                break;
        }
        if (this.mBindViewCallback == null) {
            return;
        }
        this.mBindViewCallback.onBindView(holder);
    }

    @Override
    public boolean onFailedToRecycleView(ViewHolder holder) {
        return true;
    }

    @Override
    public int getItemCount() {
        return this.mApps.getAdapterItems().size();
    }

    @Override
    public int getItemViewType(int position) {
        AlphabeticalAppsList.AdapterItem item = this.mApps.getAdapterItems().get(position);
        return item.viewType;
    }
}
