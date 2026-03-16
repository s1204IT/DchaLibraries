package com.android.gallery3d.app;

import android.content.Context;
import android.content.res.Resources;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.ui.SlotView;

final class Config {

    public static class AlbumSetPage {
        private static AlbumSetPage sInstance;
        public AlbumSetSlotRenderer.LabelSpec labelSpec;
        public int paddingBottom;
        public int paddingTop;
        public int placeholderColor;
        public SlotView.Spec slotViewSpec;

        public static synchronized AlbumSetPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumSetPage(context);
            }
            return sInstance;
        }

        private AlbumSetPage(Context context) {
            Resources r = context.getResources();
            this.placeholderColor = r.getColor(R.color.albumset_placeholder);
            this.slotViewSpec = new SlotView.Spec();
            this.slotViewSpec.rowsLand = r.getInteger(R.integer.albumset_rows_land);
            this.slotViewSpec.rowsPort = r.getInteger(R.integer.albumset_rows_port);
            this.slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.albumset_slot_gap);
            this.slotViewSpec.slotHeightAdditional = 0;
            this.paddingTop = r.getDimensionPixelSize(R.dimen.albumset_padding_top);
            this.paddingBottom = r.getDimensionPixelSize(R.dimen.albumset_padding_bottom);
            this.labelSpec = new AlbumSetSlotRenderer.LabelSpec();
            this.labelSpec.labelBackgroundHeight = r.getDimensionPixelSize(R.dimen.albumset_label_background_height);
            this.labelSpec.titleOffset = r.getDimensionPixelSize(R.dimen.albumset_title_offset);
            this.labelSpec.countOffset = r.getDimensionPixelSize(R.dimen.albumset_count_offset);
            this.labelSpec.titleFontSize = r.getDimensionPixelSize(R.dimen.albumset_title_font_size);
            this.labelSpec.countFontSize = r.getDimensionPixelSize(R.dimen.albumset_count_font_size);
            this.labelSpec.leftMargin = r.getDimensionPixelSize(R.dimen.albumset_left_margin);
            this.labelSpec.titleRightMargin = r.getDimensionPixelSize(R.dimen.albumset_title_right_margin);
            this.labelSpec.iconSize = r.getDimensionPixelSize(R.dimen.albumset_icon_size);
            this.labelSpec.backgroundColor = r.getColor(R.color.albumset_label_background);
            this.labelSpec.titleColor = r.getColor(R.color.albumset_label_title);
            this.labelSpec.countColor = r.getColor(R.color.albumset_label_count);
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
            Resources r = context.getResources();
            this.placeholderColor = r.getColor(R.color.album_placeholder);
            this.slotViewSpec = new SlotView.Spec();
            this.slotViewSpec.rowsLand = r.getInteger(R.integer.album_rows_land);
            this.slotViewSpec.rowsPort = r.getInteger(R.integer.album_rows_port);
            this.slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.album_slot_gap);
        }
    }

    public static class ManageCachePage extends AlbumSetPage {
        private static ManageCachePage sInstance;
        public final int cachePinMargin;
        public final int cachePinSize;

        public static synchronized ManageCachePage get(Context context) {
            if (sInstance == null) {
                sInstance = new ManageCachePage(context);
            }
            return sInstance;
        }

        public ManageCachePage(Context context) {
            super(context);
            Resources r = context.getResources();
            this.cachePinSize = r.getDimensionPixelSize(R.dimen.cache_pin_size);
            this.cachePinMargin = r.getDimensionPixelSize(R.dimen.cache_pin_margin);
        }
    }
}
