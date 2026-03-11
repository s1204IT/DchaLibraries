package androidx.slice.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;
import java.util.ArrayList;
import java.util.List;

public class RowContent {
    private SliceItem mContentDescr;
    private boolean mEndItemsContainAction;
    private boolean mIsHeader;
    private SliceItem mLayoutDirItem;
    private int mMaxHeight;
    private int mMinHeight;
    private SliceItem mPrimaryAction;
    private SliceItem mRange;
    private int mRangeHeight;
    private SliceItem mRowSlice;
    private SliceItem mStartItem;
    private SliceItem mSubtitleItem;
    private SliceItem mSummaryItem;
    private SliceItem mTitleItem;
    private ArrayList<SliceItem> mEndItems = new ArrayList<>();
    private ArrayList<SliceAction> mToggleItems = new ArrayList<>();
    private int mLineCount = 0;

    public RowContent(Context context, SliceItem rowSlice, boolean isHeader) {
        populate(rowSlice, isHeader);
        if (context != null) {
            this.mMaxHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_row_max_height);
            this.mMinHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_row_min_height);
            this.mRangeHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_row_range_height);
        }
    }

    private boolean populate(SliceItem rowSlice, boolean isHeader) {
        this.mIsHeader = isHeader;
        this.mRowSlice = rowSlice;
        if (!isValidRow(rowSlice)) {
            Log.w("RowContent", "Provided SliceItem is invalid for RowContent");
            return false;
        }
        determineStartAndPrimaryAction(rowSlice);
        this.mContentDescr = SliceQuery.findSubtype(rowSlice, "text", "content_description");
        ArrayList<SliceItem> rowItems = filterInvalidItems(rowSlice);
        if (rowItems.size() == 1 && (("action".equals(rowItems.get(0).getFormat()) || "slice".equals(rowItems.get(0).getFormat())) && !rowItems.get(0).hasAnyHints("shortcut", "title") && isValidRow(rowItems.get(0)))) {
            rowSlice = rowItems.get(0);
            rowItems = filterInvalidItems(rowSlice);
        }
        this.mLayoutDirItem = SliceQuery.findTopLevelItem(rowSlice.getSlice(), "int", "layout_direction", null, null);
        if (this.mLayoutDirItem != null) {
            this.mLayoutDirItem = SliceViewUtil.resolveLayoutDirection(this.mLayoutDirItem.getInt()) != -1 ? this.mLayoutDirItem : null;
        }
        if ("range".equals(rowSlice.getSubType())) {
            this.mRange = rowSlice;
        }
        if (rowItems.size() > 0) {
            if (this.mStartItem != null) {
                rowItems.remove(this.mStartItem);
            }
            if (this.mPrimaryAction != null) {
                rowItems.remove(this.mPrimaryAction);
            }
            ArrayList<SliceItem> endItems = new ArrayList<>();
            for (int i = 0; i < rowItems.size(); i++) {
                SliceItem item = rowItems.get(i);
                if ("text".equals(item.getFormat())) {
                    if ((this.mTitleItem == null || !this.mTitleItem.hasHint("title")) && item.hasHint("title") && !item.hasHint("summary")) {
                        this.mTitleItem = item;
                    } else if (this.mSubtitleItem == null && !item.hasHint("summary")) {
                        this.mSubtitleItem = item;
                    } else if (this.mSummaryItem == null && item.hasHint("summary")) {
                        this.mSummaryItem = item;
                    }
                } else {
                    endItems.add(item);
                }
            }
            if (hasText(this.mTitleItem)) {
                this.mLineCount++;
            }
            if (hasText(this.mSubtitleItem)) {
                this.mLineCount++;
            }
            boolean hasTimestamp = this.mStartItem != null && "long".equals(this.mStartItem.getFormat());
            boolean hasTimestamp2 = hasTimestamp;
            for (int i2 = 0; i2 < endItems.size(); i2++) {
                SliceItem item2 = endItems.get(i2);
                boolean isAction = SliceQuery.find(item2, "action") != null;
                if ("long".equals(item2.getFormat())) {
                    if (!hasTimestamp2) {
                        hasTimestamp2 = true;
                        this.mEndItems.add(item2);
                    }
                } else {
                    processContent(item2, isAction);
                }
            }
        }
        return isValid();
    }

    private void processContent(SliceItem item, boolean isAction) {
        if (isAction) {
            SliceAction ac = new SliceActionImpl(item);
            if (ac.isToggle()) {
                this.mToggleItems.add(ac);
            }
        }
        this.mEndItems.add(item);
        this.mEndItemsContainAction |= isAction;
    }

    private void determineStartAndPrimaryAction(SliceItem rowSlice) {
        List<SliceItem> possibleStartItems = SliceQuery.findAll(rowSlice, (String) null, "title", (String) null);
        if (possibleStartItems.size() > 0) {
            String format = possibleStartItems.get(0).getFormat();
            if (("action".equals(format) && SliceQuery.find(possibleStartItems.get(0), "image") != null) || "slice".equals(format) || "long".equals(format) || "image".equals(format)) {
                this.mStartItem = possibleStartItems.get(0);
            }
        }
        String[] hints = {"shortcut", "title"};
        List<SliceItem> possiblePrimaries = SliceQuery.findAll(rowSlice, "slice", hints, (String[]) null);
        if (possiblePrimaries.isEmpty() && "action".equals(rowSlice.getFormat()) && rowSlice.getSlice().getItems().size() == 1) {
            this.mPrimaryAction = rowSlice;
            return;
        }
        if (this.mStartItem != null && possiblePrimaries.size() > 1 && possiblePrimaries.get(0) == this.mStartItem) {
            this.mPrimaryAction = possiblePrimaries.get(1);
        } else if (possiblePrimaries.size() > 0) {
            this.mPrimaryAction = possiblePrimaries.get(0);
        }
    }

    public SliceItem getSlice() {
        return this.mRowSlice;
    }

    public SliceItem getLayoutDirItem() {
        return this.mLayoutDirItem;
    }

    public SliceItem getRange() {
        return this.mRange;
    }

    public SliceItem getInputRangeThumb() {
        if (this.mRange != null) {
            List<SliceItem> items = this.mRange.getSlice().getItems();
            for (int i = 0; i < items.size(); i++) {
                if ("image".equals(items.get(i).getFormat())) {
                    return items.get(i);
                }
            }
            return null;
        }
        return null;
    }

    public SliceItem getPrimaryAction() {
        return this.mPrimaryAction;
    }

    public SliceItem getStartItem() {
        if (this.mIsHeader) {
            return null;
        }
        return this.mStartItem;
    }

    public SliceItem getTitleItem() {
        return this.mTitleItem;
    }

    public SliceItem getSubtitleItem() {
        return this.mSubtitleItem;
    }

    public SliceItem getSummaryItem() {
        return this.mSummaryItem == null ? this.mSubtitleItem : this.mSummaryItem;
    }

    public ArrayList<SliceItem> getEndItems() {
        return this.mEndItems;
    }

    public ArrayList<SliceAction> getToggleItems() {
        return this.mToggleItems;
    }

    public CharSequence getContentDescription() {
        if (this.mContentDescr != null) {
            return this.mContentDescr.getText();
        }
        return null;
    }

    public int getLineCount() {
        return this.mLineCount;
    }

    public int getSmallHeight() {
        return getRange() != null ? getActualHeight() : this.mMaxHeight;
    }

    public int getActualHeight() {
        if (!isValid()) {
            return 0;
        }
        int rowHeight = (getLineCount() > 1 || this.mIsHeader) ? this.mMaxHeight : this.mMinHeight;
        if (getRange() != null) {
            if (getLineCount() > 0) {
                return rowHeight + this.mRangeHeight;
            }
            return this.mIsHeader ? this.mMaxHeight : this.mRangeHeight;
        }
        return rowHeight;
    }

    private static boolean hasText(SliceItem textSlice) {
        return textSlice != null && (textSlice.hasHint("partial") || !TextUtils.isEmpty(textSlice.getText()));
    }

    public boolean isDefaultSeeMore() {
        return "action".equals(this.mRowSlice.getFormat()) && this.mRowSlice.getSlice().hasHint("see_more") && this.mRowSlice.getSlice().getItems().isEmpty();
    }

    public boolean isValid() {
        return (this.mStartItem == null && this.mPrimaryAction == null && this.mTitleItem == null && this.mSubtitleItem == null && this.mEndItems.size() <= 0 && this.mRange == null && !isDefaultSeeMore()) ? false : true;
    }

    private static boolean isValidRow(SliceItem rowSlice) {
        if (rowSlice == null) {
            return false;
        }
        if ("slice".equals(rowSlice.getFormat()) || "action".equals(rowSlice.getFormat())) {
            List<SliceItem> rowItems = rowSlice.getSlice().getItems();
            if (rowSlice.hasHint("see_more") && rowItems.isEmpty()) {
                return true;
            }
            for (int i = 0; i < rowItems.size(); i++) {
                if (isValidRowContent(rowSlice, rowItems.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<SliceItem> filterInvalidItems(SliceItem rowSlice) {
        ArrayList<SliceItem> filteredList = new ArrayList<>();
        for (SliceItem i : rowSlice.getSlice().getItems()) {
            if (isValidRowContent(rowSlice, i)) {
                filteredList.add(i);
            }
        }
        return filteredList;
    }

    private static boolean isValidRowContent(SliceItem slice, SliceItem item) {
        if (item.hasAnyHints("keywords", "ttl", "last_updated", "horizontal") || "content_description".equals(item.getSubType())) {
            return false;
        }
        String itemFormat = item.getFormat();
        return "image".equals(itemFormat) || "text".equals(itemFormat) || "long".equals(itemFormat) || "action".equals(itemFormat) || "input".equals(itemFormat) || "slice".equals(itemFormat) || ("int".equals(itemFormat) && "range".equals(slice.getSubType()));
    }
}
