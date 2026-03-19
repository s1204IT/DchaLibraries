package androidx.slice.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;
import androidx.slice.widget.GridContent;
import androidx.slice.widget.SliceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GridRowView extends SliceChildView implements View.OnClickListener {
    private GridContent mGridContent;
    private int mGutter;
    private int mIconSize;
    private int mLargeImageHeight;
    private boolean mMaxCellUpdateScheduled;
    private int mMaxCells;
    private ViewTreeObserver.OnPreDrawListener mMaxCellsUpdater;
    private int mRowCount;
    private int mRowIndex;
    private int mSmallImageMinWidth;
    private int mSmallImageSize;
    private int mTextPadding;
    private LinearLayout mViewContainer;
    private static final int TITLE_TEXT_LAYOUT = R.layout.abc_slice_title;
    private static final int TEXT_LAYOUT = R.layout.abc_slice_secondary_text;

    public GridRowView(Context context) {
        this(context, null);
    }

    public GridRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMaxCells = -1;
        this.mMaxCellsUpdater = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                GridRowView.this.mMaxCells = GridRowView.this.getMaxCells();
                GridRowView.this.populateViews();
                GridRowView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                GridRowView.this.mMaxCellUpdateScheduled = false;
                return true;
            }
        };
        Resources res = getContext().getResources();
        this.mViewContainer = new LinearLayout(getContext());
        this.mViewContainer.setOrientation(0);
        addView(this.mViewContainer, new FrameLayout.LayoutParams(-1, -1));
        this.mViewContainer.setGravity(16);
        this.mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        this.mSmallImageSize = res.getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
        this.mLargeImageHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_only_height);
        this.mSmallImageMinWidth = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_min_width);
        this.mGutter = res.getDimensionPixelSize(R.dimen.abc_slice_grid_gutter);
        this.mTextPadding = res.getDimensionPixelSize(R.dimen.abc_slice_grid_text_padding);
    }

    @Override
    public int getSmallHeight() {
        if (this.mGridContent == null) {
            return 0;
        }
        return this.mGridContent.getSmallHeight() + getExtraTopPadding() + getExtraBottomPadding();
    }

    @Override
    public int getActualHeight() {
        if (this.mGridContent == null) {
            return 0;
        }
        return this.mGridContent.getActualHeight() + getExtraTopPadding() + getExtraBottomPadding();
    }

    private int getExtraTopPadding() {
        if (this.mGridContent != null && this.mGridContent.isAllImages() && this.mRowIndex == 0) {
            return this.mGridTopPadding;
        }
        return 0;
    }

    private int getExtraBottomPadding() {
        if (this.mGridContent != null && this.mGridContent.isAllImages()) {
            if (this.mRowIndex == this.mRowCount - 1 || getMode() == 1) {
                return this.mGridBottomPadding;
            }
            return 0;
        }
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = getMode() == 1 ? getSmallHeight() : getActualHeight();
        int heightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(height, 1073741824);
        this.mViewContainer.getLayoutParams().height = height;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec2);
    }

    @Override
    public void setTint(int tintColor) {
        super.setTint(tintColor);
        if (this.mGridContent != null) {
            resetView();
            populateViews();
        }
    }

    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader, int rowIndex, int rowCount, SliceView.OnSliceActionListener observer) {
        resetView();
        setSliceActionListener(observer);
        this.mRowIndex = rowIndex;
        this.mRowCount = rowCount;
        this.mGridContent = new GridContent(getContext(), slice);
        if (!scheduleMaxCellsUpdate()) {
            populateViews();
        }
        this.mViewContainer.setPadding(0, getExtraTopPadding(), 0, getExtraBottomPadding());
    }

    private boolean scheduleMaxCellsUpdate() {
        if (this.mGridContent == null || !this.mGridContent.isValid()) {
            return true;
        }
        if (getWidth() == 0) {
            this.mMaxCellUpdateScheduled = true;
            getViewTreeObserver().addOnPreDrawListener(this.mMaxCellsUpdater);
            return true;
        }
        this.mMaxCells = getMaxCells();
        return false;
    }

    private int getMaxCells() {
        if (this.mGridContent == null || !this.mGridContent.isValid() || getWidth() == 0) {
            return -1;
        }
        ArrayList<GridContent.CellContent> cells = this.mGridContent.getGridContent();
        if (cells.size() <= 1) {
            return 1;
        }
        int desiredCellWidth = this.mGridContent.getLargestImageMode() == 2 ? this.mLargeImageHeight : this.mSmallImageMinWidth;
        return getWidth() / (this.mGutter + desiredCellWidth);
    }

    private void populateViews() {
        if (this.mGridContent == null || !this.mGridContent.isValid()) {
            resetView();
            return;
        }
        if (scheduleMaxCellsUpdate()) {
            return;
        }
        if (this.mGridContent.getLayoutDirItem() != null) {
            setLayoutDirection(this.mGridContent.getLayoutDirItem().getInt());
        }
        if (this.mGridContent.getContentIntent() != null) {
            EventInfo info = new EventInfo(getMode(), 3, 1, this.mRowIndex);
            Pair<SliceItem, EventInfo> tagItem = new Pair<>(this.mGridContent.getContentIntent(), info);
            this.mViewContainer.setTag(tagItem);
            makeClickable(this.mViewContainer, true);
        }
        CharSequence contentDescr = this.mGridContent.getContentDescription();
        if (contentDescr != null) {
            this.mViewContainer.setContentDescription(contentDescr);
        }
        ArrayList<GridContent.CellContent> cells = this.mGridContent.getGridContent();
        if (this.mGridContent.getLargestImageMode() == 2) {
            this.mViewContainer.setGravity(48);
        } else {
            this.mViewContainer.setGravity(16);
        }
        int maxCells = this.mMaxCells;
        int i = 0;
        boolean hasSeeMore = this.mGridContent.getSeeMoreItem() != null;
        while (true) {
            int i2 = i;
            int i3 = cells.size();
            if (i2 < i3) {
                if (this.mViewContainer.getChildCount() >= maxCells) {
                    if (hasSeeMore) {
                        addSeeMoreCount(cells.size() - maxCells);
                        return;
                    }
                    return;
                }
                addCell(cells.get(i2), i2, Math.min(cells.size(), maxCells));
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private void addSeeMoreCount(int numExtra) {
        ViewGroup seeMoreView;
        TextView extraText;
        View last = this.mViewContainer.getChildAt(this.mViewContainer.getChildCount() - 1);
        this.mViewContainer.removeView(last);
        SliceItem seeMoreItem = this.mGridContent.getSeeMoreItem();
        int index = this.mViewContainer.getChildCount();
        int total = this.mMaxCells;
        if (("slice".equals(seeMoreItem.getFormat()) || "action".equals(seeMoreItem.getFormat())) && seeMoreItem.getSlice().getItems().size() > 0) {
            addCell(new GridContent.CellContent(seeMoreItem), index, total);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (this.mGridContent.isAllImages()) {
            seeMoreView = (FrameLayout) inflater.inflate(R.layout.abc_slice_grid_see_more_overlay, (ViewGroup) this.mViewContainer, false);
            seeMoreView.addView(last, 0, new FrameLayout.LayoutParams(-1, -1));
            extraText = (TextView) seeMoreView.findViewById(R.id.text_see_more_count);
        } else {
            seeMoreView = (LinearLayout) inflater.inflate(R.layout.abc_slice_grid_see_more, (ViewGroup) this.mViewContainer, false);
            extraText = (TextView) seeMoreView.findViewById(R.id.text_see_more_count);
            TextView moreText = (TextView) seeMoreView.findViewById(R.id.text_see_more);
            moreText.setTextSize(0, this.mGridTitleSize);
            moreText.setTextColor(this.mTitleColor);
        }
        this.mViewContainer.addView(seeMoreView, new LinearLayout.LayoutParams(0, -1, 1.0f));
        extraText.setText(getResources().getString(R.string.abc_slice_more_content, Integer.valueOf(numExtra)));
        EventInfo info = new EventInfo(getMode(), 4, 1, this.mRowIndex);
        info.setPosition(2, index, total);
        Pair<SliceItem, EventInfo> tagItem = new Pair<>(seeMoreItem, info);
        seeMoreView.setTag(tagItem);
        makeClickable(seeMoreView, true);
    }

    private void addCell(GridContent.CellContent cell, int index, int total) {
        SliceItem item;
        int i;
        int imageCount;
        int textCount;
        int maxCellText = getMode() == 1 ? 1 : 2;
        LinearLayout cellContainer = new LinearLayout(getContext());
        cellContainer.setOrientation(1);
        cellContainer.setGravity(1);
        ArrayList<SliceItem> cellItems = cell.getCellItems();
        SliceItem contentIntentItem = cell.getContentIntent();
        boolean isSingleItem = cellItems.size() == 1;
        List<SliceItem> textItems = null;
        if (!isSingleItem && getMode() == 1) {
            textItems = new ArrayList<>();
            for (SliceItem cellItem : cellItems) {
                if ("text".equals(cellItem.getFormat())) {
                    textItems.add(cellItem);
                }
            }
            Iterator<SliceItem> iterator = textItems.iterator();
            while (textItems.size() > 1) {
                if (!iterator.next().hasAnyHints("title", "large")) {
                    iterator.remove();
                }
            }
        }
        List<SliceItem> textItems2 = textItems;
        int textCount2 = 0;
        int imageCount2 = 0;
        boolean added = false;
        SliceItem prevItem = null;
        int textCount3 = 0;
        while (true) {
            int i2 = textCount3;
            if (i2 >= cellItems.size()) {
                break;
            }
            SliceItem item2 = cellItems.get(i2);
            String itemFormat = item2.getFormat();
            int padding = determinePadding(prevItem);
            if (textCount2 >= maxCellText) {
                item = item2;
                i = i2;
                imageCount = imageCount2;
                textCount = textCount2;
            } else if ("text".equals(itemFormat) || "long".equals(itemFormat)) {
                if (textItems2 == null || textItems2.contains(item2)) {
                    i = i2;
                    imageCount = imageCount2;
                    textCount = textCount2;
                    if (addItem(item2, this.mTintColor, cellContainer, padding, isSingleItem)) {
                        textCount2 = textCount + 1;
                        prevItem = item2;
                        added = true;
                        imageCount2 = imageCount;
                        textCount3 = i + 1;
                    }
                } else {
                    i = i2;
                    imageCount = imageCount2;
                    textCount = textCount2;
                }
                imageCount2 = imageCount;
                textCount2 = textCount;
                textCount3 = i + 1;
            } else {
                item = item2;
                i = i2;
                imageCount = imageCount2;
                textCount = textCount2;
            }
            if (imageCount < 1) {
                SliceItem item3 = item;
                if ("image".equals(item3.getFormat()) && addItem(item3, this.mTintColor, cellContainer, 0, isSingleItem)) {
                    imageCount2 = imageCount + 1;
                    prevItem = item3;
                    added = true;
                } else {
                    imageCount2 = imageCount;
                }
                textCount2 = textCount;
            }
            textCount3 = i + 1;
        }
        if (added) {
            CharSequence contentDescr = cell.getContentDescription();
            if (contentDescr != null) {
                cellContainer.setContentDescription(contentDescr);
            }
            this.mViewContainer.addView(cellContainer, new LinearLayout.LayoutParams(0, -2, 1.0f));
            if (index != total - 1) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) cellContainer.getLayoutParams();
                lp.setMarginEnd(this.mGutter);
                cellContainer.setLayoutParams(lp);
            }
            if (contentIntentItem != null) {
                EventInfo info = new EventInfo(getMode(), 1, 1, this.mRowIndex);
                info.setPosition(2, index, total);
                Pair<SliceItem, EventInfo> tagItem = new Pair<>(contentIntentItem, info);
                cellContainer.setTag(tagItem);
                makeClickable(cellContainer, true);
            }
        }
    }

    private boolean addItem(SliceItem item, int color, ViewGroup container, int padding, boolean isSingle) {
        CharSequence text;
        LinearLayout.LayoutParams lp;
        String format = item.getFormat();
        View addedView = null;
        if ("text".equals(format) || "long".equals(format)) {
            boolean title = SliceQuery.hasAnyHints(item, "large", "title");
            TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(title ? TITLE_TEXT_LAYOUT : TEXT_LAYOUT, (ViewGroup) null);
            tv.setTextSize(0, title ? this.mGridTitleSize : this.mGridSubtitleSize);
            tv.setTextColor(title ? this.mTitleColor : this.mSubtitleColor);
            if ("long".equals(format)) {
                text = SliceViewUtil.getRelativeTimeString(item.getTimestamp());
            } else {
                text = item.getText();
            }
            tv.setText(text);
            container.addView(tv);
            tv.setPadding(0, padding, 0, 0);
            addedView = tv;
        } else if ("image".equals(format)) {
            ImageView iv = new ImageView(getContext());
            iv.setImageDrawable(item.getIcon().loadDrawable(getContext()));
            if (item.hasHint("large")) {
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int height = isSingle ? -1 : this.mLargeImageHeight;
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, height);
                lp = lp2;
            } else {
                boolean isIcon = !item.hasHint("no_tint");
                int size = isIcon ? this.mIconSize : this.mSmallImageSize;
                iv.setScaleType(isIcon ? ImageView.ScaleType.CENTER_INSIDE : ImageView.ScaleType.CENTER_CROP);
                lp = new LinearLayout.LayoutParams(size, size);
            }
            LinearLayout.LayoutParams lp3 = lp;
            if (color != -1 && !item.hasHint("no_tint")) {
                iv.setColorFilter(color);
            }
            container.addView(iv, lp3);
            addedView = iv;
        }
        return addedView != null;
    }

    private int determinePadding(SliceItem prevItem) {
        if (prevItem == null) {
            return 0;
        }
        if ("image".equals(prevItem.getFormat())) {
            return this.mTextPadding;
        }
        if (!"text".equals(prevItem.getFormat()) && !"long".equals(prevItem.getFormat())) {
            return 0;
        }
        return this.mVerticalGridTextPadding;
    }

    private void makeClickable(View layout, boolean isClickable) {
        Drawable drawable = null;
        layout.setOnClickListener(isClickable ? this : null);
        if (isClickable) {
            drawable = SliceViewUtil.getDrawable(getContext(), android.R.attr.selectableItemBackground);
        }
        layout.setBackground(drawable);
        layout.setClickable(isClickable);
    }

    @Override
    public void onClick(View view) {
        Pair<SliceItem, EventInfo> tagItem = (Pair) view.getTag();
        SliceItem actionItem = (SliceItem) tagItem.first;
        EventInfo info = (EventInfo) tagItem.second;
        if (actionItem != null && "action".equals(actionItem.getFormat())) {
            try {
                actionItem.fireAction(null, null);
                if (this.mObserver != null) {
                    this.mObserver.onSliceAction(info, actionItem);
                }
            } catch (PendingIntent.CanceledException e) {
                Log.e("GridView", "PendingIntent for slice cannot be sent", e);
            }
        }
    }

    @Override
    public void resetView() {
        if (this.mMaxCellUpdateScheduled) {
            this.mMaxCellUpdateScheduled = false;
            getViewTreeObserver().removeOnPreDrawListener(this.mMaxCellsUpdater);
        }
        this.mViewContainer.removeAllViews();
        setLayoutDirection(2);
        makeClickable(this.mViewContainer, false);
    }
}
