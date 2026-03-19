package com.android.wallpaperpicker;

import android.animation.LayoutTransition;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import com.android.wallpaperpicker.WallpaperCropActivity;
import com.android.wallpaperpicker.tileinfo.DefaultWallpaperInfo;
import com.android.wallpaperpicker.tileinfo.LiveWallpaperInfo;
import com.android.wallpaperpicker.tileinfo.PickImageInfo;
import com.android.wallpaperpicker.tileinfo.ResourceWallpaperInfo;
import com.android.wallpaperpicker.tileinfo.ThirdPartyWallpaperInfo;
import com.android.wallpaperpicker.tileinfo.UriWallpaperInfo;
import com.android.wallpaperpicker.tileinfo.WallpaperTileInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WallpaperPickerActivity extends WallpaperCropActivity implements ActionMode.Callback, View.OnClickListener, View.OnLongClickListener {
    private ActionMode mActionMode;
    private SavedWallpaperImages mSavedImages;
    private View mSelectedTile;
    private float mWallpaperParallaxOffset;
    private HorizontalScrollView mWallpaperScrollContainer;
    private View mWallpaperStrip;
    private LinearLayout mWallpapersView;
    ArrayList<Uri> mTempWallpaperTiles = new ArrayList<>();
    private int mSelectedIndex = -1;

    protected void setSystemWallpaperVisiblity(final boolean z) {
        if (!z) {
            this.mCropView.setVisibility(0);
        } else {
            changeWallpaperFlags(z);
        }
        this.mCropView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!z) {
                    WallpaperPickerActivity.this.changeWallpaperFlags(z);
                } else {
                    WallpaperPickerActivity.this.mCropView.setVisibility(4);
                }
            }
        }, 200L);
    }

    private void changeWallpaperFlags(boolean z) {
        int i;
        if (!z) {
            i = 0;
        } else {
            i = 1048576;
        }
        if (i != (getWindow().getAttributes().flags & 1048576)) {
            getWindow().setFlags(i, 1048576);
        }
    }

    @Override
    protected void onLoadRequestComplete(WallpaperCropActivity.LoadRequest loadRequest, boolean z) {
        super.onLoadRequestComplete(loadRequest, z);
        if (z) {
            setSystemWallpaperVisiblity(false);
        }
    }

    @Override
    protected void init() {
        setContentView(R.layout.wallpaper_picker);
        this.mCropView = (CropView) findViewById(R.id.cropView);
        this.mCropView.setVisibility(4);
        this.mProgressView = findViewById(R.id.loading);
        this.mWallpaperScrollContainer = (HorizontalScrollView) findViewById(R.id.wallpaper_scroll_container);
        this.mWallpaperStrip = findViewById(R.id.wallpaper_strip);
        this.mCropView.setTouchCallback(new ToggleOnTapCallback(this.mWallpaperStrip));
        this.mWallpaperParallaxOffset = getIntent().getFloatExtra("com.android.launcher3.WALLPAPER_OFFSET", 0.0f);
        this.mWallpapersView = (LinearLayout) findViewById(R.id.wallpaper_list);
        this.mSavedImages = new SavedWallpaperImages(this);
        populateWallpapers(this.mWallpapersView, this.mSavedImages.loadThumbnailsAndImageIdList(), true);
        populateWallpapers(this.mWallpapersView, findBundledWallpapers(), false);
        new LiveWallpaperInfo.LoaderTask(this) {
            @Override
            protected void onPostExecute(List<LiveWallpaperInfo> list) {
                WallpaperPickerActivity.this.populateWallpapers((LinearLayout) WallpaperPickerActivity.this.findViewById(R.id.live_wallpaper_list), list, false);
                WallpaperPickerActivity.this.initializeScrollForRtl();
                WallpaperPickerActivity.this.updateTileIndices();
            }
        }.execute(new Void[0]);
        populateWallpapers((LinearLayout) findViewById(R.id.third_party_wallpaper_list), ThirdPartyWallpaperInfo.getAll(this), false);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.master_wallpaper_list);
        linearLayout.addView(createTileView(linearLayout, new PickImageInfo(), false), 0);
        this.mCropView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                if (i3 - i > 0 && i4 - i2 > 0) {
                    if (WallpaperPickerActivity.this.mSelectedIndex >= 0 && WallpaperPickerActivity.this.mSelectedIndex < WallpaperPickerActivity.this.mWallpapersView.getChildCount()) {
                        WallpaperPickerActivity.this.onClick(WallpaperPickerActivity.this.mWallpapersView.getChildAt(WallpaperPickerActivity.this.mSelectedIndex));
                        WallpaperPickerActivity.this.setSystemWallpaperVisiblity(false);
                    }
                    view.removeOnLayoutChangeListener(this);
                }
            }
        });
        updateTileIndices();
        initializeScrollForRtl();
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setDuration(200L);
        layoutTransition.setStartDelay(1, 0L);
        layoutTransition.setAnimator(3, null);
        this.mWallpapersView.setLayoutTransition(layoutTransition);
        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (WallpaperPickerActivity.this.mSelectedTile != null && WallpaperPickerActivity.this.mCropView.getTileSource() != null) {
                    WallpaperPickerActivity.this.mWallpaperStrip.setVisibility(8);
                    actionBar.hide();
                    ((WallpaperTileInfo) WallpaperPickerActivity.this.mSelectedTile.getTag()).onSave(WallpaperPickerActivity.this);
                    return;
                }
                Log.w("WallpaperPickerActivity", "\"Set wallpaper\" was clicked when no tile was selected");
            }
        });
        this.mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);
    }

    @Override
    public void onClick(View view) {
        if (this.mActionMode != null) {
            if (view.isLongClickable()) {
                onLongClick(view);
            }
        } else {
            WallpaperTileInfo wallpaperTileInfo = (WallpaperTileInfo) view.getTag();
            if (wallpaperTileInfo.isSelectable() && view.getVisibility() == 0) {
                selectTile(view);
                setWallpaperButtonEnabled(true);
            }
            wallpaperTileInfo.onClick(this);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        ((CheckableFrameLayout) view).toggle();
        if (this.mActionMode != null) {
            this.mActionMode.invalidate();
            return true;
        }
        this.mActionMode = startActionMode(this);
        int childCount = this.mWallpapersView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mWallpapersView.getChildAt(i).setSelected(false);
        }
        return true;
    }

    public void setWallpaperButtonEnabled(boolean z) {
        this.mSetWallpaperButton.setEnabled(z);
    }

    public float getWallpaperParallaxOffset() {
        return this.mWallpaperParallaxOffset;
    }

    public void selectTile(View view) {
        if (this.mSelectedTile != null) {
            this.mSelectedTile.setSelected(false);
            this.mSelectedTile = null;
        }
        this.mSelectedTile = view;
        view.setSelected(true);
        this.mSelectedIndex = this.mWallpapersView.indexOfChild(view);
        view.announceForAccessibility(getString(R.string.announce_selection, view.getContentDescription()));
    }

    private void initializeScrollForRtl() {
        if (WallpaperUtils.isRtl(getResources())) {
            this.mWallpaperScrollContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    WallpaperPickerActivity.this.mWallpaperScrollContainer.scrollTo(((LinearLayout) WallpaperPickerActivity.this.findViewById(R.id.master_wallpaper_list)).getWidth(), 0);
                    WallpaperPickerActivity.this.mWallpaperScrollContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mWallpaperStrip = findViewById(R.id.wallpaper_strip);
        if (this.mWallpaperStrip.getAlpha() < 1.0f) {
            this.mWallpaperStrip.setAlpha(1.0f);
            this.mWallpaperStrip.setVisibility(0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putParcelableArrayList("TEMP_WALLPAPER_TILES", this.mTempWallpaperTiles);
        bundle.putInt("SELECTED_INDEX", this.mSelectedIndex);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        Iterator it = bundle.getParcelableArrayList("TEMP_WALLPAPER_TILES").iterator();
        while (it.hasNext()) {
            addTemporaryWallpaperTile((Uri) it.next(), true);
        }
        this.mSelectedIndex = bundle.getInt("SELECTED_INDEX", -1);
    }

    private void updateTileIndices() {
        int childCount;
        LinearLayout linearLayout;
        int i;
        LinearLayout linearLayout2 = (LinearLayout) findViewById(R.id.master_wallpaper_list);
        int childCount2 = linearLayout2.getChildCount();
        Resources resources = getResources();
        int i2 = 0;
        int i3 = 0;
        while (i2 < 2) {
            int i4 = 0;
            int i5 = i3;
            for (int i6 = 0; i6 < childCount2; i6++) {
                View childAt = linearLayout2.getChildAt(i6);
                if (!(childAt.getTag() instanceof WallpaperTileInfo)) {
                    LinearLayout linearLayout3 = (LinearLayout) childAt;
                    childCount = linearLayout3.getChildCount();
                    linearLayout = linearLayout3;
                    i = 0;
                } else {
                    linearLayout = linearLayout2;
                    childCount = i6 + 1;
                    i = i6;
                }
                while (i < childCount) {
                    WallpaperTileInfo wallpaperTileInfo = (WallpaperTileInfo) linearLayout.getChildAt(i).getTag();
                    if (wallpaperTileInfo.isNamelessWallpaper()) {
                        if (i2 == 0) {
                            i5++;
                        } else {
                            i4++;
                            wallpaperTileInfo.onIndexUpdated(resources.getString(R.string.wallpaper_accessibility_name, Integer.valueOf(i4), Integer.valueOf(i5)));
                        }
                    }
                    i++;
                }
            }
            i2++;
            i3 = i5;
        }
    }

    private void addTemporaryWallpaperTile(Uri uri, boolean z) {
        View viewCreateTileView;
        UriWallpaperInfo uriWallpaperInfo;
        int i = 0;
        while (true) {
            if (i < this.mWallpapersView.getChildCount()) {
                viewCreateTileView = this.mWallpapersView.getChildAt(i);
                ?? tag = viewCreateTileView.getTag();
                if ((tag instanceof UriWallpaperInfo) && tag.mUri.equals(uri)) {
                    break;
                } else {
                    i++;
                }
            } else {
                viewCreateTileView = null;
                break;
            }
        }
        if (viewCreateTileView != null) {
            this.mWallpapersView.removeViewAt(i);
            uriWallpaperInfo = (UriWallpaperInfo) viewCreateTileView.getTag();
        } else {
            UriWallpaperInfo uriWallpaperInfo2 = new UriWallpaperInfo(uri);
            viewCreateTileView = createTileView(this.mWallpapersView, uriWallpaperInfo2, true);
            this.mTempWallpaperTiles.add(uri);
            uriWallpaperInfo = uriWallpaperInfo2;
        }
        this.mWallpapersView.addView(viewCreateTileView, 0);
        uriWallpaperInfo.loadThumbnaleAsync(this);
        updateTileIndices();
        if (!z) {
            onClick(viewCreateTileView);
        }
    }

    private void populateWallpapers(ViewGroup viewGroup, List<? extends WallpaperTileInfo> list, boolean z) {
        Iterator<? extends WallpaperTileInfo> it = list.iterator();
        while (it.hasNext()) {
            viewGroup.addView(createTileView(viewGroup, it.next(), z));
        }
    }

    private View createTileView(ViewGroup viewGroup, WallpaperTileInfo wallpaperTileInfo, boolean z) {
        View viewCreateView = wallpaperTileInfo.createView(this, getLayoutInflater(), viewGroup);
        viewCreateView.setTag(wallpaperTileInfo);
        if (z) {
            viewCreateView.setOnLongClickListener(this);
        }
        viewCreateView.setOnClickListener(this);
        return viewCreateView;
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 5 && i2 == -1) {
            if (intent != null && intent.getData() != null) {
                addTemporaryWallpaperTile(intent.getData(), false);
                return;
            }
            return;
        }
        if (i == 6 && i2 == -1) {
            setResult(-1);
            finish();
        }
    }

    public ArrayList<WallpaperTileInfo> findBundledWallpapers() {
        ArrayList<WallpaperTileInfo> arrayList = new ArrayList<>(24);
        Pair<ApplicationInfo, Integer> wallpaperArrayResourceId = getWallpaperArrayResourceId();
        if (wallpaperArrayResourceId != null) {
            try {
                addWallpapers(arrayList, getPackageManager().getResourcesForApplication((ApplicationInfo) wallpaperArrayResourceId.first), ((PackageItemInfo) ((ApplicationInfo) wallpaperArrayResourceId.first)).packageName, ((Integer) wallpaperArrayResourceId.second).intValue());
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        WallpaperTileInfo wallpaperTileInfo = DefaultWallpaperInfo.get(this);
        if (wallpaperTileInfo != null) {
            arrayList.add(0, wallpaperTileInfo);
        }
        return arrayList;
    }

    public Pair<ApplicationInfo, Integer> getWallpaperArrayResourceId() {
        return new Pair<>(getApplicationInfo(), Integer.valueOf(R.array.wallpapers));
    }

    public void addWallpapers(ArrayList<WallpaperTileInfo> arrayList, Resources resources, String str, int i) {
        for (String str2 : resources.getStringArray(i)) {
            int identifier = resources.getIdentifier(str2, "drawable", str);
            if (identifier != 0) {
                int identifier2 = resources.getIdentifier(str2 + "_small", "drawable", str);
                if (identifier2 != 0) {
                    arrayList.add(new ResourceWallpaperInfo(resources, identifier, resources.getDrawable(identifier2)));
                }
            } else {
                Log.e("WallpaperPickerActivity", "Couldn't find wallpaper " + str2);
            }
        }
    }

    public SavedWallpaperImages getSavedImages() {
        return this.mSavedImages;
    }

    public void startActivityForResultSafely(Intent intent, int i) {
        startActivityForResult(intent, i);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.cab_delete_wallpapers, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        int childCount = this.mWallpapersView.getChildCount();
        int i = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            if (((CheckableFrameLayout) this.mWallpapersView.getChildAt(i2)).isChecked()) {
                i++;
            }
        }
        if (i == 0) {
            actionMode.finish();
            return true;
        }
        actionMode.setTitle(getResources().getQuantityString(R.plurals.number_of_items_selected, i, Integer.valueOf(i)));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        if (menuItem.getItemId() != R.id.menu_delete) {
            return false;
        }
        int childCount = this.mWallpapersView.getChildCount();
        ArrayList arrayList = new ArrayList();
        boolean z = false;
        for (int i = 0; i < childCount; i++) {
            CheckableFrameLayout checkableFrameLayout = (CheckableFrameLayout) this.mWallpapersView.getChildAt(i);
            if (checkableFrameLayout.isChecked()) {
                ((WallpaperTileInfo) checkableFrameLayout.getTag()).onDelete(this);
                arrayList.add(checkableFrameLayout);
                if (i == this.mSelectedIndex) {
                    z = true;
                }
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            this.mWallpapersView.removeView((View) it.next());
        }
        if (z) {
            this.mSelectedIndex = -1;
            this.mSelectedTile = null;
            setSystemWallpaperVisiblity(true);
        }
        updateTileIndices();
        actionMode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        int childCount = this.mWallpapersView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((CheckableFrameLayout) this.mWallpapersView.getChildAt(i)).setChecked(false);
        }
        if (this.mSelectedTile != null) {
            this.mSelectedTile.setSelected(true);
        }
        this.mActionMode = null;
    }
}
