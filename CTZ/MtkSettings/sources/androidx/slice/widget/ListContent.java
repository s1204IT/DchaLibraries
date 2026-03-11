package androidx.slice.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;
import java.util.ArrayList;
import java.util.List;

public class ListContent {
    private SliceItem mColorItem;
    private Context mContext;
    private int mGridBottomPadding;
    private int mGridSubtitleSize;
    private int mGridTitleSize;
    private int mGridTopPadding;
    private SliceItem mHeaderItem;
    private int mHeaderSubtitleSize;
    private int mHeaderTitleSize;
    private int mLargeHeight;
    private SliceItem mLayoutDirItem;
    private int mMinScrollHeight;
    private ArrayList<SliceItem> mRowItems;
    private SliceItem mSeeMoreItem;
    private Slice mSlice;
    private List<SliceAction> mSliceActions;
    private int mSubtitleSize;
    private int mTitleSize;
    private int mVerticalGridTextPadding;
    private int mVerticalHeaderTextPadding;
    private int mVerticalTextPadding;

    public ListContent(Context context, Slice slice) {
        this(context, slice, null, 0, 0);
    }

    public ListContent(Context context, Slice slice, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mRowItems = new ArrayList<>();
        this.mSlice = slice;
        if (this.mSlice == null) {
            return;
        }
        this.mContext = context;
        if (context != null) {
            this.mMinScrollHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_row_min_height);
            this.mLargeHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_large_height);
            Resources.Theme theme = context.getTheme();
            if (theme != null) {
                TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.SliceView, defStyleAttr, defStyleRes);
                try {
                    this.mHeaderTitleSize = (int) a.getDimension(R.styleable.SliceView_headerTitleSize, 0.0f);
                    this.mHeaderSubtitleSize = (int) a.getDimension(R.styleable.SliceView_headerSubtitleSize, 0.0f);
                    this.mVerticalHeaderTextPadding = (int) a.getDimension(R.styleable.SliceView_headerTextVerticalPadding, 0.0f);
                    this.mTitleSize = (int) a.getDimension(R.styleable.SliceView_titleSize, 0.0f);
                    this.mSubtitleSize = (int) a.getDimension(R.styleable.SliceView_subtitleSize, 0.0f);
                    this.mVerticalTextPadding = (int) a.getDimension(R.styleable.SliceView_textVerticalPadding, 0.0f);
                    this.mGridTitleSize = (int) a.getDimension(R.styleable.SliceView_gridTitleSize, 0.0f);
                    this.mGridSubtitleSize = (int) a.getDimension(R.styleable.SliceView_gridSubtitleSize, 0.0f);
                    int defaultVerticalGridPadding = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_grid_text_inner_padding);
                    this.mVerticalGridTextPadding = (int) a.getDimension(R.styleable.SliceView_gridTextVerticalPadding, defaultVerticalGridPadding);
                    this.mGridTopPadding = (int) a.getDimension(R.styleable.SliceView_gridTopPadding, 0.0f);
                    this.mGridBottomPadding = (int) a.getDimension(R.styleable.SliceView_gridTopPadding, 0.0f);
                } finally {
                    a.recycle();
                }
            }
        }
        populate(slice);
    }

    private boolean populate(Slice slice) {
        if (slice == null) {
            return false;
        }
        this.mColorItem = SliceQuery.findTopLevelItem(slice, "int", "color", null, null);
        this.mLayoutDirItem = SliceQuery.findTopLevelItem(slice, "int", "layout_direction", null, null);
        if (this.mLayoutDirItem != null) {
            this.mLayoutDirItem = SliceViewUtil.resolveLayoutDirection(this.mLayoutDirItem.getInt()) != -1 ? this.mLayoutDirItem : null;
        }
        this.mSliceActions = SliceMetadata.getSliceActions(slice);
        this.mHeaderItem = findHeaderItem(slice);
        if (this.mHeaderItem != null) {
            this.mRowItems.add(this.mHeaderItem);
        }
        this.mSeeMoreItem = getSeeMoreItem(slice);
        List<SliceItem> children = slice.getItems();
        for (int i = 0; i < children.size(); i++) {
            SliceItem child = children.get(i);
            String format = child.getFormat();
            boolean isNonRowContent = child.hasAnyHints("actions", "see_more", "keywords", "ttl", "last_updated");
            if (!isNonRowContent && ("action".equals(format) || "slice".equals(format))) {
                if (this.mHeaderItem == null && !child.hasHint("list_item")) {
                    this.mHeaderItem = child;
                    this.mRowItems.add(0, child);
                } else if (child.hasHint("list_item")) {
                    this.mRowItems.add(child);
                }
            }
        }
        if (this.mHeaderItem == null && this.mRowItems.size() >= 1) {
            this.mHeaderItem = this.mRowItems.get(0);
        }
        return isValid();
    }

    public int getSmallHeight() {
        return getHeight(this.mHeaderItem, true, 0, 1, 1);
    }

    public int getLargeHeight(int maxHeight, boolean scrollable) {
        int desiredHeight = getListHeight(this.mRowItems);
        int maxLargeHeight = maxHeight != -1 ? maxHeight : this.mLargeHeight;
        boolean bigEnoughToScroll = desiredHeight - maxLargeHeight >= this.mMinScrollHeight;
        int height = bigEnoughToScroll ? maxLargeHeight : maxHeight == -1 ? desiredHeight : Math.min(maxLargeHeight, desiredHeight);
        if (!scrollable) {
            return getListHeight(getItemsForNonScrollingList(height));
        }
        return height;
    }

    public int getListHeight(List<SliceItem> listItems) {
        if (listItems == null || this.mContext == null) {
            return 0;
        }
        boolean hasRealHeader = false;
        SliceItem maybeHeader = null;
        if (!listItems.isEmpty()) {
            SliceItem maybeHeader2 = listItems.get(0);
            maybeHeader = maybeHeader2;
            hasRealHeader = !maybeHeader.hasAnyHints("list_item", "horizontal");
        }
        if (listItems.size() == 1 && !maybeHeader.hasHint("horizontal")) {
            return getHeight(maybeHeader, true, 0, 1, 2);
        }
        int rowCount = listItems.size();
        int height = 0;
        int height2 = 0;
        while (height2 < listItems.size()) {
            height += getHeight(listItems.get(height2), height2 == 0 && hasRealHeader, height2, rowCount, 2);
            height2++;
        }
        return height;
    }

    public ArrayList<SliceItem> getItemsForNonScrollingList(int height) {
        ArrayList<SliceItem> visibleItems = new ArrayList<>();
        if (this.mRowItems == null || this.mRowItems.size() == 0) {
            return visibleItems;
        }
        int minItemCount = hasHeader() ? 2 : 1;
        int visibleHeight = 0;
        if (this.mSeeMoreItem != null) {
            RowContent rc = new RowContent(this.mContext, this.mSeeMoreItem, false);
            visibleHeight = 0 + rc.getActualHeight();
        }
        int rowCount = this.mRowItems.size();
        int visibleHeight2 = visibleHeight;
        int visibleHeight3 = 0;
        while (visibleHeight3 < rowCount) {
            int itemHeight = getHeight(this.mRowItems.get(visibleHeight3), visibleHeight3 == 0, visibleHeight3, rowCount, 2);
            if (height > 0 && visibleHeight2 + itemHeight > height) {
                break;
            }
            visibleHeight2 += itemHeight;
            visibleItems.add(this.mRowItems.get(visibleHeight3));
            visibleHeight3++;
        }
        if (this.mSeeMoreItem != null && visibleItems.size() >= minItemCount && visibleItems.size() != rowCount) {
            visibleItems.add(this.mSeeMoreItem);
        }
        if (visibleItems.size() == 0) {
            visibleItems.add(this.mRowItems.get(0));
        }
        return visibleItems;
    }

    private int getHeight(SliceItem item, boolean isHeader, int index, int count, int mode) {
        int bottomPadding = 0;
        if (this.mContext == null || item == null) {
            return 0;
        }
        if (item.hasHint("horizontal")) {
            GridContent gc = new GridContent(this.mContext, item);
            int topPadding = (gc.isAllImages() && index == 0) ? this.mGridTopPadding : 0;
            if (gc.isAllImages() && index == count - 1) {
                bottomPadding = this.mGridBottomPadding;
            }
            int height = mode == 1 ? gc.getSmallHeight() : gc.getActualHeight();
            return height + topPadding + bottomPadding;
        }
        RowContent rc = new RowContent(this.mContext, item, isHeader);
        return mode == 1 ? rc.getSmallHeight() : rc.getActualHeight();
    }

    public boolean isValid() {
        return this.mSlice != null && this.mRowItems.size() > 0;
    }

    public Slice getSlice() {
        return this.mSlice;
    }

    public SliceItem getLayoutDirItem() {
        return this.mLayoutDirItem;
    }

    public SliceItem getColorItem() {
        return this.mColorItem;
    }

    public SliceItem getHeaderItem() {
        return this.mHeaderItem;
    }

    public List<SliceAction> getSliceActions() {
        return this.mSliceActions;
    }

    public ArrayList<SliceItem> getRowItems() {
        return this.mRowItems;
    }

    public boolean hasHeader() {
        return this.mHeaderItem != null && isValidHeader(this.mHeaderItem);
    }

    public int getHeaderTemplateType() {
        return getRowType(this.mContext, this.mHeaderItem, true, this.mSliceActions);
    }

    public static int getRowType(Context context, SliceItem rowItem, boolean isHeader, List<SliceAction> actions) {
        if (rowItem == null) {
            return 0;
        }
        if (rowItem.hasHint("horizontal")) {
            return 1;
        }
        RowContent rc = new RowContent(context, rowItem, isHeader);
        SliceItem actionItem = rc.getPrimaryAction();
        SliceAction primaryAction = null;
        if (actionItem != null) {
            primaryAction = new SliceActionImpl(actionItem);
        }
        if (rc.getRange() != null) {
            return "action".equals(rc.getRange().getFormat()) ? 4 : 5;
        }
        if (primaryAction != null && primaryAction.isToggle()) {
            return 3;
        }
        if (isHeader && actions != null) {
            for (int i = 0; i < actions.size(); i++) {
                if (actions.get(i).isToggle()) {
                    return 3;
                }
            }
            return 0;
        }
        if (rc.getToggleItems().size() <= 0) {
            return 0;
        }
        return 3;
    }

    public SliceItem getPrimaryAction() {
        if (this.mHeaderItem != null) {
            if (this.mHeaderItem.hasHint("horizontal")) {
                GridContent gc = new GridContent(this.mContext, this.mHeaderItem);
                return gc.getContentIntent();
            }
            RowContent rc = new RowContent(this.mContext, this.mHeaderItem, false);
            return rc.getPrimaryAction();
        }
        return null;
    }

    private static SliceItem findHeaderItem(Slice slice) {
        String[] nonHints = {"list_item", "shortcut", "actions", "keywords", "ttl", "last_updated", "horizontal"};
        SliceItem header = SliceQuery.find(slice, "slice", (String[]) null, nonHints);
        if (header == null || !isValidHeader(header)) {
            return null;
        }
        return header;
    }

    private static SliceItem getSeeMoreItem(Slice slice) {
        SliceItem item = SliceQuery.findTopLevelItem(slice, null, null, new String[]{"see_more"}, null);
        if (item == null || !"slice".equals(item.getFormat())) {
            return null;
        }
        List<SliceItem> items = item.getSlice().getItems();
        if (items.size() == 1 && "action".equals(items.get(0).getFormat())) {
            return items.get(0);
        }
        return item;
    }

    public static boolean isValidHeader(SliceItem sliceItem) {
        if (!"slice".equals(sliceItem.getFormat()) || sliceItem.hasAnyHints("list_item", "actions", "keywords", "see_more")) {
            return false;
        }
        SliceItem item = SliceQuery.find(sliceItem, "text", (String) null, (String) null);
        return item != null;
    }
}
