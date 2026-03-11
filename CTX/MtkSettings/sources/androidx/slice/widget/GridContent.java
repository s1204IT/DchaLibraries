package androidx.slice.widget;

import android.content.Context;
import android.content.res.Resources;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;
import java.util.ArrayList;
import java.util.List;

public class GridContent {
    private boolean mAllImages;
    private int mAllImagesHeight;
    private int mBigPicMaxHeight;
    private int mBigPicMinHeight;
    private SliceItem mColorItem;
    private SliceItem mContentDescr;
    private boolean mHasImage;
    private int mImageTextHeight;
    private SliceItem mLayoutDirItem;
    private int mMaxCellLineCount;
    private int mMaxHeight;
    private int mMinHeight;
    private SliceItem mPrimaryAction;
    private SliceItem mSeeMoreItem;
    private SliceItem mTitleItem;
    private ArrayList<CellContent> mGridContent = new ArrayList<>();
    private int mLargestImageMode = -1;

    public GridContent(Context context, SliceItem gridItem) {
        populate(gridItem);
        if (context != null) {
            Resources res = context.getResources();
            this.mBigPicMinHeight = res.getDimensionPixelSize(R.dimen.abc_slice_big_pic_min_height);
            this.mBigPicMaxHeight = res.getDimensionPixelSize(R.dimen.abc_slice_big_pic_max_height);
            this.mAllImagesHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_only_height);
            this.mImageTextHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_text_height);
            this.mMinHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_min_height);
            this.mMaxHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_max_height);
        }
    }

    private boolean populate(SliceItem gridItem) {
        this.mColorItem = SliceQuery.findSubtype(gridItem, "int", "color");
        if ("slice".equals(gridItem.getFormat()) || "action".equals(gridItem.getFormat())) {
            this.mLayoutDirItem = SliceQuery.findTopLevelItem(gridItem.getSlice(), "int", "layout_direction", null, null);
            if (this.mLayoutDirItem != null) {
                this.mLayoutDirItem = SliceViewUtil.resolveLayoutDirection(this.mLayoutDirItem.getInt()) != -1 ? this.mLayoutDirItem : null;
            }
        }
        this.mSeeMoreItem = SliceQuery.find(gridItem, (String) null, "see_more", (String) null);
        if (this.mSeeMoreItem != null && "slice".equals(this.mSeeMoreItem.getFormat())) {
            this.mSeeMoreItem = this.mSeeMoreItem.getSlice().getItems().get(0);
        }
        String[] hints = {"shortcut", "title"};
        this.mPrimaryAction = SliceQuery.find(gridItem, "slice", hints, new String[]{"actions"});
        this.mAllImages = true;
        if ("slice".equals(gridItem.getFormat())) {
            List<SliceItem> items = gridItem.getSlice().getItems();
            if (items.size() == 1 && "slice".equals(items.get(0).getFormat())) {
                items = items.get(0).getSlice().getItems();
            }
            List<SliceItem> items2 = filterAndProcessItems(items);
            if (items2.size() == 1 && items2.get(0).getFormat().equals("slice")) {
                items2 = items2.get(0).getSlice().getItems();
            }
            for (int i = 0; i < items2.size(); i++) {
                SliceItem item = items2.get(i);
                if ("content_description".equals(item.getSubType())) {
                    this.mContentDescr = item;
                } else {
                    CellContent cc = new CellContent(item);
                    processContent(cc);
                }
            }
        } else {
            CellContent cc2 = new CellContent(gridItem);
            processContent(cc2);
        }
        return isValid();
    }

    private void processContent(CellContent cc) {
        if (cc.isValid()) {
            if (this.mTitleItem == null && cc.getTitleItem() != null) {
                this.mTitleItem = cc.getTitleItem();
            }
            this.mGridContent.add(cc);
            if (!cc.isImageOnly()) {
                this.mAllImages = false;
            }
            this.mMaxCellLineCount = Math.max(this.mMaxCellLineCount, cc.getTextCount());
            this.mHasImage |= cc.hasImage();
            this.mLargestImageMode = Math.max(this.mLargestImageMode, cc.getImageMode());
        }
    }

    public ArrayList<CellContent> getGridContent() {
        return this.mGridContent;
    }

    public SliceItem getLayoutDirItem() {
        return this.mLayoutDirItem;
    }

    public SliceItem getContentIntent() {
        return this.mPrimaryAction;
    }

    public SliceItem getSeeMoreItem() {
        return this.mSeeMoreItem;
    }

    public CharSequence getContentDescription() {
        if (this.mContentDescr != null) {
            return this.mContentDescr.getText();
        }
        return null;
    }

    public boolean isValid() {
        return this.mGridContent.size() > 0;
    }

    public boolean isAllImages() {
        return this.mAllImages;
    }

    public int getLargestImageMode() {
        return this.mLargestImageMode;
    }

    private List<SliceItem> filterAndProcessItems(List<SliceItem> items) {
        List<SliceItem> filteredItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            SliceItem item = items.get(i);
            boolean isNonCellContent = true;
            boolean containsSeeMore = SliceQuery.find(item, (String) null, "see_more", (String) null) != null;
            if (!containsSeeMore && !item.hasAnyHints("shortcut", "see_more", "keywords", "ttl", "last_updated")) {
                isNonCellContent = false;
            }
            if ("content_description".equals(item.getSubType())) {
                this.mContentDescr = item;
            } else if (!isNonCellContent) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    public int getMaxCellLineCount() {
        return this.mMaxCellLineCount;
    }

    public boolean hasImage() {
        return this.mHasImage;
    }

    public int getSmallHeight() {
        return getHeight(true);
    }

    public int getActualHeight() {
        return getHeight(false);
    }

    private int getHeight(boolean isSmall) {
        if (!isValid()) {
            return 0;
        }
        if (this.mAllImages) {
            return this.mGridContent.size() == 1 ? isSmall ? this.mBigPicMinHeight : this.mBigPicMaxHeight : this.mLargestImageMode == 0 ? this.mMinHeight : this.mAllImagesHeight;
        }
        boolean twoLines = getMaxCellLineCount() > 1;
        boolean hasImage = hasImage();
        if (!twoLines || isSmall) {
            if (this.mLargestImageMode != 0) {
                return this.mImageTextHeight;
            }
        } else if (hasImage) {
            return this.mMaxHeight;
        }
        return this.mMinHeight;
    }

    public static class CellContent {
        private SliceItem mContentDescr;
        private SliceItem mContentIntent;
        private boolean mHasImage;
        private int mTextCount;
        private SliceItem mTitleItem;
        private ArrayList<SliceItem> mCellItems = new ArrayList<>();
        private int mImageMode = -1;

        public CellContent(SliceItem cellItem) {
            populate(cellItem);
        }

        public boolean populate(SliceItem cellItem) {
            String format = cellItem.getFormat();
            if (!cellItem.hasHint("shortcut") && ("slice".equals(format) || "action".equals(format))) {
                List<SliceItem> items = cellItem.getSlice().getItems();
                if (items.size() == 1 && ("action".equals(items.get(0).getFormat()) || "slice".equals(items.get(0).getFormat()))) {
                    this.mContentIntent = items.get(0);
                    items = items.get(0).getSlice().getItems();
                }
                if ("action".equals(format)) {
                    this.mContentIntent = cellItem;
                }
                this.mTextCount = 0;
                int imageCount = 0;
                for (int imageCount2 = 0; imageCount2 < items.size(); imageCount2++) {
                    SliceItem item = items.get(imageCount2);
                    String itemFormat = item.getFormat();
                    if ("content_description".equals(item.getSubType())) {
                        this.mContentDescr = item;
                    } else if (this.mTextCount < 2 && ("text".equals(itemFormat) || "long".equals(itemFormat))) {
                        this.mTextCount++;
                        this.mCellItems.add(item);
                        if (this.mTitleItem == null || (!this.mTitleItem.hasHint("title") && item.hasHint("title"))) {
                            this.mTitleItem = item;
                        }
                    } else if (imageCount < 1 && "image".equals(item.getFormat())) {
                        if (item.hasHint("no_tint")) {
                            this.mImageMode = item.hasHint("large") ? 2 : 1;
                        } else {
                            this.mImageMode = 0;
                        }
                        imageCount++;
                        this.mHasImage = true;
                        this.mCellItems.add(item);
                    }
                }
            } else if (isValidCellContent(cellItem)) {
                this.mCellItems.add(cellItem);
            }
            return isValid();
        }

        public SliceItem getTitleItem() {
            return this.mTitleItem;
        }

        public SliceItem getContentIntent() {
            return this.mContentIntent;
        }

        public ArrayList<SliceItem> getCellItems() {
            return this.mCellItems;
        }

        private boolean isValidCellContent(SliceItem cellItem) {
            String format = cellItem.getFormat();
            boolean isNonCellContent = "content_description".equals(cellItem.getSubType()) || cellItem.hasAnyHints("keywords", "ttl", "last_updated");
            return !isNonCellContent && ("text".equals(format) || "long".equals(format) || "image".equals(format));
        }

        public boolean isValid() {
            return this.mCellItems.size() > 0 && this.mCellItems.size() <= 3;
        }

        public boolean isImageOnly() {
            return this.mCellItems.size() == 1 && "image".equals(this.mCellItems.get(0).getFormat());
        }

        public int getTextCount() {
            return this.mTextCount;
        }

        public boolean hasImage() {
            return this.mHasImage;
        }

        public int getImageMode() {
            return this.mImageMode;
        }

        public CharSequence getContentDescription() {
            if (this.mContentDescr != null) {
                return this.mContentDescr.getText();
            }
            return null;
        }
    }
}
