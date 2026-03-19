package com.android.gallery3d.app;

import android.content.Context;
import android.content.res.Resources;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.ui.SlotView;
import com.mediatek.gallery3d.layout.FancyHelper;

public final class Config {

    public static class AlbumSetPage {
        private static AlbumSetPage sInstance;
        public AlbumSetSlotRenderer.LabelSpec labelSpec;
        public int paddingBottom;
        public int paddingTop;
        public int placeholderColor;
        public SlotView.Spec slotViewSpec;

        public static synchronized AlbumSetPage get(Context context) {
            sInstance = new AlbumSetPage(context);
            return sInstance;
        }

        public static synchronized AlbumSetPage getConfigInMultiWindow(Context context) {
            AlbumSetPage albumSetPage;
            albumSetPage = new AlbumSetPage(context);
            albumSetPage.slotViewSpec.colsLand = 2;
            albumSetPage.slotViewSpec.colsPort = 2;
            return albumSetPage;
        }

        private AlbumSetPage(Context context) {
            Resources resources = context.getResources();
            this.placeholderColor = resources.getColor(R.color.albumset_placeholder);
            this.slotViewSpec = new SlotView.Spec();
            this.slotViewSpec.rowsLand = resources.getInteger(R.integer.albumset_rows_land);
            this.slotViewSpec.rowsPort = resources.getInteger(R.integer.albumset_rows_port);
            if (FancyHelper.isFancyLayoutSupported()) {
                this.slotViewSpec.colsLand = 4;
                this.slotViewSpec.colsPort = 2;
            }
            this.slotViewSpec.slotGap = resources.getDimensionPixelSize(R.dimen.albumset_slot_gap);
            this.slotViewSpec.slotHeightAdditional = 0;
            this.paddingTop = resources.getDimensionPixelSize(R.dimen.albumset_padding_top);
            this.paddingBottom = resources.getDimensionPixelSize(R.dimen.albumset_padding_bottom);
            this.labelSpec = new AlbumSetSlotRenderer.LabelSpec();
            this.labelSpec.labelBackgroundHeight = resources.getDimensionPixelSize(R.dimen.albumset_label_background_height);
            this.labelSpec.titleOffset = resources.getDimensionPixelSize(R.dimen.albumset_title_offset);
            this.labelSpec.countOffset = resources.getDimensionPixelSize(R.dimen.albumset_count_offset);
            this.labelSpec.titleFontSize = resources.getDimensionPixelSize(R.dimen.albumset_title_font_size);
            this.labelSpec.countFontSize = resources.getDimensionPixelSize(R.dimen.albumset_count_font_size);
            this.labelSpec.leftMargin = resources.getDimensionPixelSize(R.dimen.albumset_left_margin);
            if (FancyHelper.isFancyLayoutSupported()) {
                this.labelSpec.titleRightMargin = resources.getDimensionPixelSize(R.dimen.albumset_title_right_margin_fancy);
            } else {
                this.labelSpec.titleRightMargin = resources.getDimensionPixelSize(R.dimen.albumset_title_right_margin);
            }
            this.labelSpec.iconSize = resources.getDimensionPixelSize(R.dimen.albumset_icon_size);
            if (FancyHelper.isFancyLayoutSupported()) {
                this.labelSpec.backgroundColor = resources.getColor(R.color.albumset_label_background_fancy);
            } else {
                this.labelSpec.backgroundColor = resources.getColor(R.color.albumset_label_background);
            }
            this.labelSpec.titleColor = resources.getColor(R.color.albumset_label_title);
            this.labelSpec.countColor = resources.getColor(R.color.albumset_label_count);
        }
    }

    public static class AlbumPage {
        private static AlbumPage sInstance;
        public int placeholderColor;
        public SlotView.Spec slotViewSpec;

        public static synchronized AlbumPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumPage(context);
            }
            return sInstance;
        }

        private AlbumPage(Context context) {
            Resources resources = context.getResources();
            this.placeholderColor = resources.getColor(R.color.album_placeholder);
            this.slotViewSpec = new SlotView.Spec();
            this.slotViewSpec.rowsLand = resources.getInteger(R.integer.album_rows_land);
            this.slotViewSpec.rowsPort = resources.getInteger(R.integer.album_rows_port);
            if (FancyHelper.isFancyLayoutSupported()) {
                this.slotViewSpec.colsLand = 4;
                this.slotViewSpec.colsPort = 3;
            }
            this.slotViewSpec.slotGap = resources.getDimensionPixelSize(R.dimen.album_slot_gap);
        }
    }
}
