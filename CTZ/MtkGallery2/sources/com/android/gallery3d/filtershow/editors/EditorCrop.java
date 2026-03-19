package com.android.gallery3d.filtershow.editors;

import android.content.Context;
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
import com.mediatek.gallery3d.util.Log;

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

        AspectInfo(int i, int i2, int i3) {
            this.mStringId = i;
            this.mAspectX = i2;
            this.mAspectY = i3;
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
        Log.w(TAG, "createEditor. ");
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
        MasterImage image = MasterImage.getImage();
        image.setCurrentFilterRepresentation(image.getPreset().getFilterWithSerializationName("CROP"));
        super.reflectCurrentFilter();
        FilterRepresentation localRepresentation = getLocalRepresentation();
        if (localRepresentation == null || (localRepresentation instanceof FilterCropRepresentation)) {
            this.mImageCrop.setFilterCropRepresentation((FilterCropRepresentation) localRepresentation);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: " + FilterCropRepresentation.class.getSimpleName());
        }
        this.mImageCrop.invalidate();
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(this.mImageCrop.getFinalRepresentation());
        this.mImageCrop = null;
    }

    @Override
    public void openUtilityPanel(final LinearLayout linearLayout) {
        Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        button.setText(this.mContext.getString(R.string.crop));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorCrop.this.showPopupMenu(linearLayout);
            }
        });
    }

    private void changeCropAspect(int i) {
        AspectInfo aspectInfo = sAspects.get(i);
        if (aspectInfo == null) {
            throw new IllegalArgumentException("Invalid resource ID: " + i);
        }
        if (this.mImageCrop == null) {
            Log.w(TAG, "changeCropAspect. mImageCrop is null");
            return;
        }
        if (i == R.id.crop_menu_original) {
            this.mImageCrop.applyOriginalAspect();
        } else if (i == R.id.crop_menu_none) {
            this.mImageCrop.applyFreeAspect();
        } else {
            this.mImageCrop.applyAspect(aspectInfo.mAspectX, aspectInfo.mAspectY);
        }
        setAspectString(this.mContext.getString(aspectInfo.mStringId));
    }

    private void showPopupMenu(LinearLayout linearLayout) {
        PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), (Button) linearLayout.findViewById(R.id.applyEffect));
        popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_crop, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                EditorCrop.this.changeCropAspect(menuItem.getItemId());
                return true;
            }
        });
        popupMenu.show();
        ((FilterShowActivity) this.mContext).onShowMenu(popupMenu);
    }

    @Override
    public void setUtilityPanelUI(View view, View view2) {
        super.setUtilityPanelUI(view, view2);
        setMenuIcon(true);
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    private void setAspectString(String str) {
        this.mAspectString = str;
    }

    @Override
    public void finalCancelCalled() {
        this.mImageCrop = null;
    }
}
