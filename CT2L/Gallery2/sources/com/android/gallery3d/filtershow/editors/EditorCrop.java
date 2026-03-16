package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageCrop;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorCrop extends Editor {
    public static final String TAG = EditorCrop.class.getSimpleName();
    protected static final SparseArray<AspectInfo> sAspects = new SparseArray<>();
    private String mAspectString;
    protected ImageCrop mImageCrop;

    static {
        sAspects.put(R.id.crop_menu_1to1, new AspectInfo(R.string.aspect1to1_effect, 1, 1));
        sAspects.put(R.id.crop_menu_4to3, new AspectInfo(R.string.aspect4to3_effect, 4, 3));
        sAspects.put(R.id.crop_menu_3to4, new AspectInfo(R.string.aspect3to4_effect, 3, 4));
        sAspects.put(R.id.crop_menu_5to7, new AspectInfo(R.string.aspect5to7_effect, 5, 7));
        sAspects.put(R.id.crop_menu_7to5, new AspectInfo(R.string.aspect7to5_effect, 7, 5));
        sAspects.put(R.id.crop_menu_none, new AspectInfo(R.string.aspectNone_effect, 0, 0));
        sAspects.put(R.id.crop_menu_original, new AspectInfo(R.string.aspectOriginal_effect, 0, 0));
    }

    protected static final class AspectInfo {
        int mAspectX;
        int mAspectY;
        int mStringId;

        AspectInfo(int stringID, int x, int y) {
            this.mStringId = stringID;
            this.mAspectX = x;
            this.mAspectY = y;
        }
    }

    public EditorCrop() {
        super(R.id.editorCrop);
        this.mAspectString = "";
        this.mChangesGeometry = true;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (this.mImageCrop == null) {
            this.mImageCrop = new ImageCrop(context);
        }
        ImageCrop imageCrop = this.mImageCrop;
        this.mImageShow = imageCrop;
        this.mView = imageCrop;
        this.mImageCrop.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        MasterImage master = MasterImage.getImage();
        master.setCurrentFilterRepresentation(master.getPreset().getFilterWithSerializationName("CROP"));
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || (rep instanceof FilterCropRepresentation)) {
            this.mImageCrop.setFilterCropRepresentation((FilterCropRepresentation) rep);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: " + FilterCropRepresentation.class.getSimpleName());
        }
        this.mImageCrop.invalidate();
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(this.mImageCrop.getFinalRepresentation());
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        Button view = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        view.setText(this.mContext.getString(R.string.crop));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditorCrop.this.showPopupMenu(accessoryViewList);
            }
        });
    }

    private void changeCropAspect(int itemId) {
        AspectInfo info = sAspects.get(itemId);
        if (info == null) {
            throw new IllegalArgumentException("Invalid resource ID: " + itemId);
        }
        if (itemId == R.id.crop_menu_original) {
            this.mImageCrop.applyOriginalAspect();
        } else if (itemId == R.id.crop_menu_none) {
            this.mImageCrop.applyFreeAspect();
        } else {
            this.mImageCrop.applyAspect(info.mAspectX, info.mAspectY);
        }
        setAspectString(this.mContext.getString(info.mStringId));
    }

    private void showPopupMenu(LinearLayout accessoryViewList) {
        Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), button);
        popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_crop, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                EditorCrop.this.changeCropAspect(item.getItemId());
                return true;
            }
        });
        popupMenu.show();
        ((FilterShowActivity) this.mContext).onShowMenu(popupMenu);
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        super.setUtilityPanelUI(actionButton, editControl);
        setMenuIcon(true);
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    private void setAspectString(String s) {
        this.mAspectString = s;
    }
}
