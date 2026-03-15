package com.android.launcher3;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.gallery3d.common.BitmapCropTask;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.launcher3.CropView;
import com.android.launcher3.WallpaperCropActivity;
import com.android.photos.BitmapRegionTileSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class WallpaperPickerActivity extends WallpaperCropActivity {
    ActionMode mActionMode;
    ActionMode.Callback mActionModeCallback;
    boolean mIgnoreNextTap;
    View.OnLongClickListener mLongClickListener;
    private SavedWallpaperImages mSavedImages;
    View mSelectedTile;
    View.OnClickListener mThumbnailOnClickListener;
    private float mWallpaperParallaxOffset;
    HorizontalScrollView mWallpaperScrollContainer;
    View mWallpaperStrip;
    LinearLayout mWallpapersView;
    ArrayList<Uri> mTempWallpaperTiles = new ArrayList<>();
    int mSelectedIndex = -1;

    public static abstract class WallpaperTileInfo {
        public Drawable mThumb;
        protected View mView;

        public void setView(View v) {
            this.mView = v;
        }

        public void onClick(WallpaperPickerActivity a) {
        }

        public void onSave(WallpaperPickerActivity a) {
        }

        public void onDelete(WallpaperPickerActivity a) {
        }

        public boolean isSelectable() {
            return false;
        }

        public boolean isNamelessWallpaper() {
            return false;
        }

        public void onIndexUpdated(CharSequence label) {
            if (!isNamelessWallpaper()) {
                return;
            }
            this.mView.setContentDescription(label);
        }
    }

    public static class PickImageInfo extends WallpaperTileInfo {
        @Override
        public void onClick(WallpaperPickerActivity a) {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/*");
            intent.putExtra("android.intent.extra.drm_level", 1);
            a.startActivityForResultSafely(intent, 5);
        }
    }

    public static class UriWallpaperInfo extends WallpaperTileInfo {
        private Uri mUri;

        public UriWallpaperInfo(Uri uri) {
            this.mUri = uri;
        }

        @Override
        public void onClick(final WallpaperPickerActivity a) {
            a.setWallpaperButtonEnabled(false);
            final BitmapRegionTileSource.UriBitmapSource bitmapSource = new BitmapRegionTileSource.UriBitmapSource(a.getContext(), this.mUri);
            a.setCropViewTileSource(bitmapSource, true, false, null, new Runnable() {
                @Override
                public void run() {
                    if (bitmapSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                        a.selectTile(UriWallpaperInfo.this.mView);
                        a.setWallpaperButtonEnabled(true);
                        return;
                    }
                    ViewGroup parent = (ViewGroup) UriWallpaperInfo.this.mView.getParent();
                    if (parent == null) {
                        return;
                    }
                    parent.removeView(UriWallpaperInfo.this.mView);
                    Toast.makeText(a.getContext(), R.string.image_load_fail, 0).show();
                }
            });
        }

        @Override
        public void onSave(final WallpaperPickerActivity a) {
            BitmapCropTask.OnBitmapCroppedHandler h = new BitmapCropTask.OnBitmapCroppedHandler() {
                @Override
                public void onBitmapCropped(byte[] imageBytes, Rect hint) {
                    Bitmap thumb = null;
                    Point thumbSize = WallpaperPickerActivity.getDefaultThumbnailSize(a.getResources());
                    if (imageBytes != null) {
                        Bitmap thumb2 = WallpaperPickerActivity.createThumbnail(thumbSize, null, null, imageBytes, null, 0, 0, true);
                        a.getSavedImages().writeImage(thumb2, imageBytes);
                        return;
                    }
                    try {
                        Point size = WallpaperPickerActivity.getDefaultThumbnailSize(a.getResources());
                        Rect finalCropped = new Rect();
                        Utils.getMaxCropRect(hint.width(), hint.height(), size.x, size.y, false).roundOut(finalCropped);
                        finalCropped.offset(hint.left, hint.top);
                        InputStream in = a.getContentResolver().openInputStream(UriWallpaperInfo.this.mUri);
                        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(in, true);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = finalCropped.width() / size.x;
                        thumb = decoder.decodeRegion(finalCropped, options);
                        decoder.recycle();
                        Utils.closeSilently(in);
                        if (thumb != null) {
                            thumb = Bitmap.createScaledBitmap(thumb, size.x, size.y, true);
                        }
                    } catch (IOException e) {
                    }
                    PointF center = a.mCropView.getCenter();
                    Float[] extras = {Float.valueOf(a.mCropView.getScale()), Float.valueOf(center.x), Float.valueOf(center.y)};
                    a.getSavedImages().writeImage(thumb, UriWallpaperInfo.this.mUri, extras);
                }
            };
            boolean shouldFadeOutOnFinish = a.getWallpaperParallaxOffset() == 0.0f;
            a.cropImageAndSetWallpaper(this.mUri, h, true, shouldFadeOutOnFinish);
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    public static class FileWallpaperInfo extends WallpaperTileInfo {
        protected File mFile;

        public FileWallpaperInfo(File target, Drawable thumb) {
            this.mFile = target;
            this.mThumb = thumb;
        }

        @Override
        public void onClick(final WallpaperPickerActivity a) {
            a.setWallpaperButtonEnabled(false);
            final BitmapRegionTileSource.UriBitmapSource bitmapSource = new BitmapRegionTileSource.UriBitmapSource(a.getContext(), Uri.fromFile(this.mFile));
            a.setCropViewTileSource(bitmapSource, false, true, getCropViewScaleAndOffsetProvider(), new Runnable() {
                @Override
                public void run() {
                    if (bitmapSource.getLoadingState() != BitmapRegionTileSource.BitmapSource.State.LOADED) {
                        return;
                    }
                    a.setWallpaperButtonEnabled(true);
                }
            });
        }

        protected WallpaperCropActivity.CropViewScaleAndOffsetProvider getCropViewScaleAndOffsetProvider() {
            return null;
        }

        @Override
        public void onSave(WallpaperPickerActivity a) {
            boolean shouldFadeOutOnFinish = a.getWallpaperParallaxOffset() == 0.0f;
            a.setWallpaper(Uri.fromFile(this.mFile), true, shouldFadeOutOnFinish);
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    public static class ResourceWallpaperInfo extends WallpaperTileInfo {
        private int mResId;
        private Resources mResources;

        public ResourceWallpaperInfo(Resources res, int resId, Drawable thumb) {
            this.mResources = res;
            this.mResId = resId;
            this.mThumb = thumb;
        }

        @Override
        public void onClick(final WallpaperPickerActivity a) {
            a.setWallpaperButtonEnabled(false);
            final BitmapRegionTileSource.ResourceBitmapSource bitmapSource = new BitmapRegionTileSource.ResourceBitmapSource(this.mResources, this.mResId);
            a.setCropViewTileSource(bitmapSource, false, false, new WallpaperCropActivity.CropViewScaleAndOffsetProvider() {
                @Override
                public float getScale(Point wallpaperSize, RectF crop) {
                    return wallpaperSize.x / crop.width();
                }

                @Override
                public float getParallaxOffset() {
                    return a.getWallpaperParallaxOffset();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    if (bitmapSource.getLoadingState() != BitmapRegionTileSource.BitmapSource.State.LOADED) {
                        return;
                    }
                    a.setWallpaperButtonEnabled(true);
                }
            });
        }

        @Override
        public void onSave(WallpaperPickerActivity a) {
            a.cropImageAndSetWallpaper(this.mResources, this.mResId, true, true);
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    @TargetApi(19)
    public static class DefaultWallpaperInfo extends WallpaperTileInfo {
        public DefaultWallpaperInfo(Drawable thumb) {
            this.mThumb = thumb;
        }

        @Override
        public void onClick(WallpaperPickerActivity a) {
            CropView c = a.getCropView();
            if (a.mProgressView != null) {
                Log.d("WallpaperPickerActivity", "DefaultWallpaperInfo.onClick(),a.mProgressView.setVisibility(View.INVISIBLE)");
                a.mProgressView.setVisibility(4);
            }
            Drawable defaultWallpaper = WallpaperManager.getInstance(a.getContext()).getBuiltInDrawable(c.getWidth(), c.getHeight(), false, 0.5f, 0.5f);
            if (defaultWallpaper == null) {
                Log.w("WallpaperPickerActivity", "Null default wallpaper encountered.");
                c.setTileSource(null, null);
                return;
            }
            WallpaperCropActivity.LoadRequest req = new WallpaperCropActivity.LoadRequest();
            req.moveToLeft = false;
            req.touchEnabled = false;
            req.scaleAndOffsetProvider = new WallpaperCropActivity.CropViewScaleAndOffsetProvider();
            req.result = new DrawableTileSource(a.getContext(), defaultWallpaper, 1024);
            a.onLoadRequestComplete(req, true);
        }

        @Override
        public void onSave(final WallpaperPickerActivity a) {
            if (!Utilities.ATLEAST_N) {
                try {
                    WallpaperManager.getInstance(a.getContext()).clear();
                    a.setResult(-1);
                } catch (IOException e) {
                    Log.e("WallpaperPickerActivity", "Setting wallpaper to default threw exception", e);
                } catch (SecurityException e2) {
                    Log.w("WallpaperPickerActivity", "Setting wallpaper to default threw exception", e2);
                    a.setResult(-1);
                }
                a.finish();
                return;
            }
            BitmapCropTask.OnEndCropHandler onEndCropHandler = new BitmapCropTask.OnEndCropHandler() {
                @Override
                public void run(boolean cropSucceeded) {
                    if (cropSucceeded) {
                        a.setResult(-1);
                    }
                    a.finish();
                }
            };
            BitmapCropTask setWallpaperTask = getDefaultWallpaperCropTask(a, onEndCropHandler);
            NycWallpaperUtils.executeCropTaskAfterPrompt(a, setWallpaperTask, a.getOnDialogCancelListener());
        }

        @NonNull
        private BitmapCropTask getDefaultWallpaperCropTask(final WallpaperPickerActivity wallpaperPickerActivity, BitmapCropTask.OnEndCropHandler onEndCropHandler) {
            int i = -1;
            return new BitmapCropTask(wallpaperPickerActivity, null, 0 == true ? 1 : 0, i, i, i, true, false, onEndCropHandler) {
                @Override
                protected Boolean doInBackground(Integer... params) {
                    int whichWallpaper = params[0].intValue();
                    boolean succeeded = true;
                    try {
                        if (whichWallpaper == 2) {
                            Bitmap defaultWallpaper = ((BitmapDrawable) WallpaperManager.getInstance(wallpaperPickerActivity.getApplicationContext()).getBuiltInDrawable()).getBitmap();
                            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream(2048);
                            if (defaultWallpaper.compress(Bitmap.CompressFormat.PNG, 100, tmpOut)) {
                                byte[] outByteArray = tmpOut.toByteArray();
                                NycWallpaperUtils.setStream(wallpaperPickerActivity.getApplicationContext(), new ByteArrayInputStream(outByteArray), null, true, 2);
                            }
                        } else {
                            NycWallpaperUtils.clear(wallpaperPickerActivity, whichWallpaper);
                        }
                    } catch (IOException e) {
                        Log.e("WallpaperPickerActivity", "Setting wallpaper to default threw exception", e);
                        succeeded = false;
                    } catch (SecurityException e2) {
                        Log.w("WallpaperPickerActivity", "Setting wallpaper to default threw exception", e2);
                        succeeded = true;
                    }
                    return Boolean.valueOf(succeeded);
                }
            };
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    protected void setSystemWallpaperVisiblity(final boolean visible) {
        if (!visible) {
            this.mCropView.setVisibility(0);
        } else {
            changeWallpaperFlags(visible);
        }
        this.mCropView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!visible) {
                    WallpaperPickerActivity.this.changeWallpaperFlags(visible);
                } else {
                    WallpaperPickerActivity.this.mCropView.setVisibility(4);
                }
            }
        }, 200L);
    }

    void changeWallpaperFlags(boolean visible) {
        int desiredWallpaperFlag = visible ? 1048576 : 0;
        int currentWallpaperFlag = getWindow().getAttributes().flags & 1048576;
        if (desiredWallpaperFlag == currentWallpaperFlag) {
            return;
        }
        getWindow().setFlags(desiredWallpaperFlag, 1048576);
    }

    @Override
    protected void onLoadRequestComplete(WallpaperCropActivity.LoadRequest req, boolean success) {
        super.onLoadRequestComplete(req, success);
        if (!success) {
            return;
        }
        setSystemWallpaperVisiblity(false);
    }

    @Override
    protected void init() {
        setContentView(R.layout.wallpaper_picker);
        this.mCropView = (CropView) findViewById(R.id.cropView);
        this.mCropView.setVisibility(4);
        this.mProgressView = findViewById(R.id.loading);
        this.mWallpaperScrollContainer = (HorizontalScrollView) findViewById(R.id.wallpaper_scroll_container);
        this.mWallpaperStrip = findViewById(R.id.wallpaper_strip);
        this.mCropView.setTouchCallback(new CropView.TouchCallback() {
            ViewPropertyAnimator mAnim;

            @Override
            public void onTouchDown() {
                if (this.mAnim != null) {
                    this.mAnim.cancel();
                }
                if (WallpaperPickerActivity.this.mWallpaperStrip.getAlpha() == 1.0f) {
                    WallpaperPickerActivity.this.mIgnoreNextTap = true;
                }
                this.mAnim = WallpaperPickerActivity.this.mWallpaperStrip.animate();
                this.mAnim.alpha(0.0f).setDuration(150L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        WallpaperPickerActivity.this.mWallpaperStrip.setVisibility(4);
                    }
                });
                this.mAnim.setInterpolator(new AccelerateInterpolator(0.75f));
                this.mAnim.start();
            }

            @Override
            public void onTouchUp() {
                WallpaperPickerActivity.this.mIgnoreNextTap = false;
            }

            @Override
            public void onTap() {
                boolean ignoreTap = WallpaperPickerActivity.this.mIgnoreNextTap;
                WallpaperPickerActivity.this.mIgnoreNextTap = false;
                if (ignoreTap) {
                    return;
                }
                if (this.mAnim != null) {
                    this.mAnim.cancel();
                }
                WallpaperPickerActivity.this.mWallpaperStrip.setVisibility(0);
                this.mAnim = WallpaperPickerActivity.this.mWallpaperStrip.animate();
                this.mAnim.alpha(1.0f).setDuration(150L).setInterpolator(new DecelerateInterpolator(0.75f));
                this.mAnim.start();
            }
        });
        this.mThumbnailOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WallpaperPickerActivity.this.mActionMode != null) {
                    if (v.isLongClickable()) {
                        WallpaperPickerActivity.this.mLongClickListener.onLongClick(v);
                        return;
                    }
                    return;
                }
                WallpaperTileInfo info = (WallpaperTileInfo) v.getTag();
                if (info == null) {
                    return;
                }
                if (info.isSelectable() && v.getVisibility() == 0) {
                    WallpaperPickerActivity.this.selectTile(v);
                    WallpaperPickerActivity.this.setWallpaperButtonEnabled(true);
                }
                info.onClick(WallpaperPickerActivity.this);
            }
        };
        this.mLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                CheckableFrameLayout c = (CheckableFrameLayout) view;
                c.toggle();
                if (WallpaperPickerActivity.this.mActionMode != null) {
                    WallpaperPickerActivity.this.mActionMode.invalidate();
                    return true;
                }
                WallpaperPickerActivity.this.mActionMode = WallpaperPickerActivity.this.startActionMode(WallpaperPickerActivity.this.mActionModeCallback);
                WallpaperPickerActivity.this.mActionMode.invalidate();
                int childCount = WallpaperPickerActivity.this.mWallpapersView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    WallpaperPickerActivity.this.mWallpapersView.getChildAt(i).setSelected(false);
                }
                return true;
            }
        };
        this.mWallpaperParallaxOffset = getIntent().getFloatExtra("com.android.launcher3.WALLPAPER_OFFSET", 0.0f);
        ArrayList<WallpaperTileInfo> wallpapers = findBundledWallpapers();
        this.mWallpapersView = (LinearLayout) findViewById(R.id.wallpaper_list);
        SimpleWallpapersAdapter ia = new SimpleWallpapersAdapter(getContext(), wallpapers);
        populateWallpapersFromAdapter(this.mWallpapersView, ia, false);
        this.mSavedImages = new SavedWallpaperImages(getContext());
        this.mSavedImages.loadThumbnailsAndImageIdList();
        populateWallpapersFromAdapter(this.mWallpapersView, this.mSavedImages, true);
        final LinearLayout liveWallpapersView = (LinearLayout) findViewById(R.id.live_wallpaper_list);
        final LiveWallpaperListAdapter a = new LiveWallpaperListAdapter(getContext());
        a.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                liveWallpapersView.removeAllViews();
                WallpaperPickerActivity.this.populateWallpapersFromAdapter(liveWallpapersView, a, false);
                WallpaperPickerActivity.this.initializeScrollForRtl();
                WallpaperPickerActivity.this.updateTileIndices();
            }
        });
        LinearLayout thirdPartyWallpapersView = (LinearLayout) findViewById(R.id.third_party_wallpaper_list);
        ThirdPartyWallpaperPickerListAdapter ta = new ThirdPartyWallpaperPickerListAdapter(getContext());
        populateWallpapersFromAdapter(thirdPartyWallpapersView, ta, false);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.master_wallpaper_list);
        FrameLayout frameLayout = (FrameLayout) getLayoutInflater().inflate(R.layout.wallpaper_picker_image_picker_item, (ViewGroup) linearLayout, false);
        linearLayout.addView(frameLayout, 0);
        Bitmap lastPhoto = getThumbnailOfLastPhoto();
        if (lastPhoto != null) {
            ImageView galleryThumbnailBg = (ImageView) frameLayout.findViewById(R.id.wallpaper_image);
            galleryThumbnailBg.setImageBitmap(lastPhoto);
            int colorOverlay = getResources().getColor(R.color.wallpaper_picker_translucent_gray);
            galleryThumbnailBg.setColorFilter(colorOverlay, PorterDuff.Mode.SRC_ATOP);
        }
        PickImageInfo pickImageInfo = new PickImageInfo();
        frameLayout.setTag(pickImageInfo);
        pickImageInfo.setView(frameLayout);
        frameLayout.setOnClickListener(this.mThumbnailOnClickListener);
        this.mCropView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left <= 0 || bottom - top <= 0) {
                    return;
                }
                if (WallpaperPickerActivity.this.mSelectedIndex >= 0 && WallpaperPickerActivity.this.mSelectedIndex < WallpaperPickerActivity.this.mWallpapersView.getChildCount()) {
                    WallpaperPickerActivity.this.mThumbnailOnClickListener.onClick(WallpaperPickerActivity.this.mWallpapersView.getChildAt(WallpaperPickerActivity.this.mSelectedIndex));
                    WallpaperPickerActivity.this.setSystemWallpaperVisiblity(false);
                }
                v.removeOnLayoutChangeListener(this);
            }
        });
        updateTileIndices();
        initializeScrollForRtl();
        LayoutTransition transitioner = new LayoutTransition();
        transitioner.setDuration(200L);
        transitioner.setStartDelay(1, 0L);
        transitioner.setAnimator(3, null);
        this.mWallpapersView.setLayoutTransition(transitioner);
        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WallpaperPickerActivity.this.mSelectedTile != null && WallpaperPickerActivity.this.mCropView.getTileSource() != null) {
                    WallpaperPickerActivity.this.mWallpaperStrip.setVisibility(8);
                    actionBar.hide();
                    WallpaperTileInfo info = (WallpaperTileInfo) WallpaperPickerActivity.this.mSelectedTile.getTag();
                    info.onSave(WallpaperPickerActivity.this);
                    return;
                }
                Log.w("WallpaperPickerActivity", "\"Set wallpaper\" was clicked when no tile was selected");
            }
        });
        this.mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);
        this.mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.cab_delete_wallpapers, menu);
                return true;
            }

            private int numCheckedItems() {
                int childCount = WallpaperPickerActivity.this.mWallpapersView.getChildCount();
                int numCheckedItems = 0;
                for (int i = 0; i < childCount; i++) {
                    CheckableFrameLayout c = (CheckableFrameLayout) WallpaperPickerActivity.this.mWallpapersView.getChildAt(i);
                    if (c.isChecked()) {
                        numCheckedItems++;
                    }
                }
                return numCheckedItems;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                int numCheckedItems = numCheckedItems();
                if (numCheckedItems == 0) {
                    mode.finish();
                    return true;
                }
                mode.setTitle(WallpaperPickerActivity.this.getResources().getQuantityString(R.plurals.number_of_items_selected, numCheckedItems, Integer.valueOf(numCheckedItems)));
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int itemId = item.getItemId();
                if (itemId != R.id.menu_delete) {
                    return false;
                }
                int childCount = WallpaperPickerActivity.this.mWallpapersView.getChildCount();
                ArrayList<View> viewsToRemove = new ArrayList<>();
                boolean selectedTileRemoved = false;
                for (int i = 0; i < childCount; i++) {
                    CheckableFrameLayout c = (CheckableFrameLayout) WallpaperPickerActivity.this.mWallpapersView.getChildAt(i);
                    if (c.isChecked()) {
                        WallpaperTileInfo info = (WallpaperTileInfo) c.getTag();
                        info.onDelete(WallpaperPickerActivity.this);
                        viewsToRemove.add(c);
                        if (i == WallpaperPickerActivity.this.mSelectedIndex) {
                            selectedTileRemoved = true;
                        }
                    }
                }
                for (View v : viewsToRemove) {
                    WallpaperPickerActivity.this.mWallpapersView.removeView(v);
                }
                if (selectedTileRemoved) {
                    WallpaperPickerActivity.this.mSelectedIndex = -1;
                    WallpaperPickerActivity.this.mSelectedTile = null;
                }
                WallpaperPickerActivity.this.updateTileIndices();
                mode.finish();
                if (selectedTileRemoved) {
                    WallpaperPickerActivity.this.mThumbnailOnClickListener.onClick(WallpaperPickerActivity.this.mWallpapersView.getChildAt(0));
                    return true;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                int childCount = WallpaperPickerActivity.this.mWallpapersView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    CheckableFrameLayout c = (CheckableFrameLayout) WallpaperPickerActivity.this.mWallpapersView.getChildAt(i);
                    c.setChecked(false);
                }
                if (WallpaperPickerActivity.this.mSelectedTile != null) {
                    WallpaperPickerActivity.this.mSelectedTile.setSelected(true);
                }
                WallpaperPickerActivity.this.mActionMode = null;
            }
        };
    }

    public void setWallpaperButtonEnabled(boolean enabled) {
        this.mSetWallpaperButton.setEnabled(enabled);
    }

    public float getWallpaperParallaxOffset() {
        return this.mWallpaperParallaxOffset;
    }

    void selectTile(View v) {
        if (this.mSelectedTile != null) {
            this.mSelectedTile.setSelected(false);
            this.mSelectedTile = null;
        }
        this.mSelectedTile = v;
        v.setSelected(true);
        this.mSelectedIndex = this.mWallpapersView.indexOfChild(v);
        v.announceForAccessibility(getContext().getString(R.string.announce_selection, v.getContentDescription()));
    }

    void initializeScrollForRtl() {
        if (!Utilities.isRtl(getResources())) {
            return;
        }
        ViewTreeObserver observer = this.mWallpaperScrollContainer.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                LinearLayout masterWallpaperList = (LinearLayout) WallpaperPickerActivity.this.findViewById(R.id.master_wallpaper_list);
                WallpaperPickerActivity.this.mWallpaperScrollContainer.scrollTo(masterWallpaperList.getWidth(), 0);
                WallpaperPickerActivity.this.mWallpaperScrollContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    protected Bitmap getThumbnailOfLastPhoto() {
        boolean canReadExternalStorage = getActivity().checkPermission("android.permission.READ_EXTERNAL_STORAGE", Process.myPid(), Process.myUid()) == 0;
        if (!canReadExternalStorage) {
            return null;
        }
        Cursor cursor = MediaStore.Images.Media.query(getContext().getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{"_id", "datetaken"}, null, null, "datetaken DESC LIMIT 1");
        Bitmap thumb = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                thumb = MediaStore.Images.Thumbnails.getThumbnail(getContext().getContentResolver(), id, 1, null);
            }
            cursor.close();
        }
        return thumb;
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mWallpaperStrip = findViewById(R.id.wallpaper_strip);
        if (this.mWallpaperStrip == null || this.mWallpaperStrip.getAlpha() >= 1.0f) {
            return;
        }
        this.mWallpaperStrip.setAlpha(1.0f);
        this.mWallpaperStrip.setVisibility(0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("TEMP_WALLPAPER_TILES", this.mTempWallpaperTiles);
        outState.putInt("SELECTED_INDEX", this.mSelectedIndex);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        ArrayList<Uri> uris = savedInstanceState.getParcelableArrayList("TEMP_WALLPAPER_TILES");
        for (Uri uri : uris) {
            addTemporaryWallpaperTile(uri, true);
        }
        this.mSelectedIndex = savedInstanceState.getInt("SELECTED_INDEX", -1);
    }

    void populateWallpapersFromAdapter(ViewGroup viewGroup, BaseAdapter adapter, boolean addLongPressHandler) {
        for (int i = 0; i < adapter.getCount(); i++) {
            FrameLayout frameLayout = (FrameLayout) adapter.getView(i, null, viewGroup);
            viewGroup.addView(frameLayout, i);
            WallpaperTileInfo wallpaperTileInfo = (WallpaperTileInfo) adapter.getItem(i);
            frameLayout.setTag(wallpaperTileInfo);
            wallpaperTileInfo.setView(frameLayout);
            if (addLongPressHandler) {
                addLongPressHandler(frameLayout);
            }
            frameLayout.setOnClickListener(this.mThumbnailOnClickListener);
        }
    }

    void updateTileIndices() {
        LinearLayout subList;
        int subListStart;
        int subListEnd;
        LinearLayout masterWallpaperList = (LinearLayout) findViewById(R.id.master_wallpaper_list);
        int childCount = masterWallpaperList.getChildCount();
        Resources res = getResources();
        int numTiles = 0;
        for (int passNum = 0; passNum < 2; passNum++) {
            int tileIndex = 0;
            for (int i = 0; i < childCount; i++) {
                View child = masterWallpaperList.getChildAt(i);
                if (child.getTag() instanceof WallpaperTileInfo) {
                    subList = masterWallpaperList;
                    subListStart = i;
                    subListEnd = i + 1;
                } else {
                    subList = (LinearLayout) child;
                    subListStart = 0;
                    subListEnd = subList.getChildCount();
                }
                for (int j = subListStart; j < subListEnd; j++) {
                    WallpaperTileInfo info = (WallpaperTileInfo) subList.getChildAt(j).getTag();
                    if (info != null && info.isNamelessWallpaper()) {
                        if (passNum == 0) {
                            numTiles++;
                        } else {
                            tileIndex++;
                            CharSequence label = res.getString(R.string.wallpaper_accessibility_name, Integer.valueOf(tileIndex), Integer.valueOf(numTiles));
                            info.onIndexUpdated(label);
                        }
                    }
                }
            }
        }
    }

    static Point getDefaultThumbnailSize(Resources res) {
        return new Point(res.getDimensionPixelSize(R.dimen.wallpaperThumbnailWidth), res.getDimensionPixelSize(R.dimen.wallpaperThumbnailHeight));
    }

    static Bitmap createThumbnail(Point size, Context context, Uri uri, byte[] imageBytes, Resources res, int resId, int rotation, boolean leftAligned) {
        BitmapCropTask cropTask;
        int width = size.x;
        int height = size.y;
        if (uri != null) {
            cropTask = new BitmapCropTask(context, uri, null, rotation, width, height, false, true, null);
        } else if (imageBytes != null) {
            cropTask = new BitmapCropTask(imageBytes, null, rotation, width, height, false, true, null);
        } else {
            cropTask = new BitmapCropTask(context, res, resId, null, rotation, width, height, false, true, null);
        }
        Point bounds = cropTask.getImageBounds();
        if (bounds == null || bounds.x == 0 || bounds.y == 0) {
            return null;
        }
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(rotation);
        float[] rotatedBounds = {bounds.x, bounds.y};
        rotateMatrix.mapPoints(rotatedBounds);
        rotatedBounds[0] = Math.abs(rotatedBounds[0]);
        rotatedBounds[1] = Math.abs(rotatedBounds[1]);
        RectF cropRect = Utils.getMaxCropRect((int) rotatedBounds[0], (int) rotatedBounds[1], width, height, leftAligned);
        cropTask.setCropBounds(cropRect);
        if (cropTask.cropBitmap(1)) {
            return cropTask.getCroppedBitmap();
        }
        return null;
    }

    private void addTemporaryWallpaperTile(final Uri uri, boolean z) {
        final FrameLayout frameLayout;
        FrameLayout frameLayout2 = null;
        int i = 0;
        while (true) {
            if (i >= this.mWallpapersView.getChildCount()) {
                break;
            }
            FrameLayout frameLayout3 = (FrameLayout) this.mWallpapersView.getChildAt(i);
            Object tag = frameLayout3.getTag();
            if (!(tag instanceof UriWallpaperInfo) || !((UriWallpaperInfo) tag).mUri.equals(uri)) {
                i++;
            } else {
                frameLayout2 = frameLayout3;
                break;
            }
        }
        if (frameLayout2 != null) {
            frameLayout = frameLayout2;
            this.mWallpapersView.removeViewAt(i);
            this.mWallpapersView.addView(frameLayout2, 0);
        } else {
            FrameLayout frameLayout4 = (FrameLayout) getLayoutInflater().inflate(R.layout.wallpaper_picker_item, this.mWallpapersView, false);
            frameLayout4.setVisibility(8);
            this.mWallpapersView.addView(frameLayout4, 0);
            this.mTempWallpaperTiles.add(uri);
            frameLayout = frameLayout4;
        }
        final ImageView imageView = (ImageView) frameLayout.findViewById(R.id.wallpaper_image);
        final Point defaultThumbnailSize = getDefaultThumbnailSize(getResources());
        final Context context = getContext();
        new AsyncTask<Void, Bitmap, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... args) {
                try {
                    int rotation = BitmapUtils.getRotationFromExif(context, uri);
                    return WallpaperPickerActivity.createThumbnail(defaultThumbnailSize, context, uri, null, null, 0, rotation, false);
                } catch (SecurityException securityException) {
                    if (WallpaperPickerActivity.this.isActivityDestroyed()) {
                        cancel(false);
                        return null;
                    }
                    throw securityException;
                }
            }

            @Override
            protected void onPostExecute(Bitmap thumb) {
                if (!isCancelled() && thumb != null) {
                    imageView.setImageBitmap(thumb);
                    Drawable thumbDrawable = imageView.getDrawable();
                    thumbDrawable.setDither(true);
                    frameLayout.setVisibility(0);
                    return;
                }
                Log.e("WallpaperPickerActivity", "Error loading thumbnail for uri=" + uri);
            }
        }.execute(new Void[0]);
        UriWallpaperInfo uriWallpaperInfo = new UriWallpaperInfo(uri);
        frameLayout.setTag(uriWallpaperInfo);
        uriWallpaperInfo.setView(frameLayout);
        addLongPressHandler(frameLayout);
        updateTileIndices();
        frameLayout.setOnClickListener(this.mThumbnailOnClickListener);
        if (z) {
            return;
        }
        this.mThumbnailOnClickListener.onClick(frameLayout);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 5 && resultCode == -1) {
            if (data == null || data.getData() == null) {
                return;
            }
            Uri uri = data.getData();
            addTemporaryWallpaperTile(uri, false);
            return;
        }
        if (requestCode != 6 || resultCode != -1) {
            return;
        }
        setResult(-1);
        finish();
    }

    private void addLongPressHandler(View v) {
        v.setOnLongClickListener(this.mLongClickListener);
        final StylusEventHelper stylusEventHelper = new StylusEventHelper(v);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return stylusEventHelper.checkAndPerformStylusEvent(event);
            }
        });
    }

    private ArrayList<WallpaperTileInfo> findBundledWallpapers() {
        PackageManager pm = getContext().getPackageManager();
        ArrayList<WallpaperTileInfo> bundled = new ArrayList<>(24);
        Partner partner = Partner.get(pm);
        if (partner != null) {
            Resources partnerRes = partner.getResources();
            int resId = partnerRes.getIdentifier("partner_wallpapers", "array", partner.getPackageName());
            if (resId != 0) {
                addWallpapers(bundled, partnerRes, partner.getPackageName(), resId);
            }
            File systemDir = partner.getWallpaperDirectory();
            if (systemDir != null && systemDir.isDirectory()) {
                for (File file : systemDir.listFiles()) {
                    if (file.isFile()) {
                        String name = file.getName();
                        int dotPos = name.lastIndexOf(46);
                        String extension = "";
                        if (dotPos >= -1) {
                            extension = name.substring(dotPos);
                            name = name.substring(0, dotPos);
                        }
                        if (!name.endsWith("_small")) {
                            File thumbnail = new File(systemDir, name + "_small" + extension);
                            Bitmap thumb = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
                            if (thumb != null) {
                                bundled.add(new FileWallpaperInfo(file, new BitmapDrawable(thumb)));
                            }
                        }
                    }
                }
            }
        }
        Pair<ApplicationInfo, Integer> r = getWallpaperArrayResourceId();
        if (r != null) {
            try {
                Resources wallpaperRes = getContext().getPackageManager().getResourcesForApplication((ApplicationInfo) r.first);
                addWallpapers(bundled, wallpaperRes, ((ApplicationInfo) r.first).packageName, ((Integer) r.second).intValue());
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (partner == null || !partner.hideDefaultWallpaper()) {
            WallpaperTileInfo defaultWallpaperInfo = Utilities.ATLEAST_KITKAT ? getDefaultWallpaper() : getPreKKDefaultWallpaperInfo();
            if (defaultWallpaperInfo != null) {
                bundled.add(0, defaultWallpaperInfo);
            }
        }
        return bundled;
    }

    private boolean writeImageToFileAsJpeg(File f, Bitmap b) {
        try {
            f.createNewFile();
            FileOutputStream thumbFileStream = getContext().openFileOutput(f.getName(), 0);
            b.compress(Bitmap.CompressFormat.JPEG, 95, thumbFileStream);
            thumbFileStream.close();
            return true;
        } catch (IOException e) {
            Log.e("WallpaperPickerActivity", "Error while writing bitmap to file " + e);
            f.delete();
            return false;
        }
    }

    private File getDefaultThumbFile() {
        return new File(getContext().getFilesDir(), Build.VERSION.SDK_INT + "_default_thumb2.jpg");
    }

    private boolean saveDefaultWallpaperThumb(Bitmap b) {
        new File(getContext().getFilesDir(), "default_thumb.jpg").delete();
        new File(getContext().getFilesDir(), "default_thumb2.jpg").delete();
        for (int i = 16; i < Build.VERSION.SDK_INT; i++) {
            new File(getContext().getFilesDir(), i + "_default_thumb2.jpg").delete();
        }
        return writeImageToFileAsJpeg(getDefaultThumbFile(), b);
    }

    private ResourceWallpaperInfo getPreKKDefaultWallpaperInfo() {
        Bitmap thumb;
        Resources sysRes = Resources.getSystem();
        int resId = sysRes.getIdentifier("default_wallpaper", "drawable", "android");
        File defaultThumbFile = getDefaultThumbFile();
        boolean defaultWallpaperExists = false;
        if (defaultThumbFile.exists()) {
            thumb = BitmapFactory.decodeFile(defaultThumbFile.getAbsolutePath());
            defaultWallpaperExists = true;
        } else {
            Resources res = getResources();
            Point defaultThumbSize = getDefaultThumbnailSize(res);
            int rotation = BitmapUtils.getRotationFromExif(res, resId);
            thumb = createThumbnail(defaultThumbSize, getContext(), null, null, sysRes, resId, rotation, false);
            if (thumb != null) {
                defaultWallpaperExists = saveDefaultWallpaperThumb(thumb);
            }
        }
        if (defaultWallpaperExists) {
            return new ResourceWallpaperInfo(sysRes, resId, new BitmapDrawable(thumb));
        }
        return null;
    }

    @TargetApi(19)
    private DefaultWallpaperInfo getDefaultWallpaper() {
        File defaultThumbFile = getDefaultThumbFile();
        Bitmap thumb = null;
        boolean defaultWallpaperExists = false;
        if (defaultThumbFile.exists()) {
            thumb = BitmapFactory.decodeFile(defaultThumbFile.getAbsolutePath());
            defaultWallpaperExists = true;
        } else {
            Resources res = getResources();
            Point defaultThumbSize = getDefaultThumbnailSize(res);
            Drawable wallpaperDrawable = WallpaperManager.getInstance(getContext()).getBuiltInDrawable(defaultThumbSize.x, defaultThumbSize.y, true, 0.5f, 0.5f);
            if (wallpaperDrawable != null) {
                thumb = Bitmap.createBitmap(defaultThumbSize.x, defaultThumbSize.y, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(thumb);
                wallpaperDrawable.setBounds(0, 0, defaultThumbSize.x, defaultThumbSize.y);
                wallpaperDrawable.draw(c);
                c.setBitmap(null);
            }
            if (thumb != null) {
                defaultWallpaperExists = saveDefaultWallpaperThumb(thumb);
            }
        }
        if (defaultWallpaperExists) {
            return new DefaultWallpaperInfo(new BitmapDrawable(thumb));
        }
        return null;
    }

    public Pair<ApplicationInfo, Integer> getWallpaperArrayResourceId() {
        String packageName = getResources().getResourcePackageName(R.array.wallpapers);
        try {
            ApplicationInfo info = getContext().getPackageManager().getApplicationInfo(packageName, 0);
            return new Pair<>(info, Integer.valueOf(R.array.wallpapers));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void addWallpapers(ArrayList<WallpaperTileInfo> known, Resources res, String packageName, int listResId) {
        String[] extras = res.getStringArray(listResId);
        for (String extra : extras) {
            int resId = res.getIdentifier(extra, "drawable", packageName);
            if (resId != 0) {
                int thumbRes = res.getIdentifier(extra + "_small", "drawable", packageName);
                if (thumbRes != 0) {
                    ResourceWallpaperInfo wallpaperInfo = new ResourceWallpaperInfo(res, resId, res.getDrawable(thumbRes));
                    known.add(wallpaperInfo);
                }
            } else {
                Log.e("WallpaperPickerActivity", "Couldn't find wallpaper " + extra);
            }
        }
    }

    public CropView getCropView() {
        return this.mCropView;
    }

    public SavedWallpaperImages getSavedImages() {
        return this.mSavedImages;
    }

    private static class SimpleWallpapersAdapter extends ArrayAdapter<WallpaperTileInfo> {
        private final LayoutInflater mLayoutInflater;

        SimpleWallpapersAdapter(Context context, ArrayList<WallpaperTileInfo> wallpapers) {
            super(context, R.layout.wallpaper_picker_item, wallpapers);
            this.mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Drawable thumb = getItem(position).mThumb;
            if (thumb == null) {
                Log.e("WallpaperPickerActivity", "Error decoding thumbnail for wallpaper #" + position);
            }
            return WallpaperPickerActivity.createImageTileView(this.mLayoutInflater, convertView, parent, thumb);
        }
    }

    public static View createImageTileView(LayoutInflater layoutInflater, View convertView, ViewGroup parent, Drawable thumb) {
        View view;
        if (convertView == null) {
            view = layoutInflater.inflate(R.layout.wallpaper_picker_item, parent, false);
        } else {
            view = convertView;
        }
        ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);
        if (thumb != null) {
            image.setImageDrawable(thumb);
            thumb.setDither(true);
        }
        return view;
    }

    public void startActivityForResultSafely(Intent intent, int requestCode) {
        Utilities.startActivityForResultSafely(getActivity(), intent, requestCode);
    }

    @Override
    public boolean enableRotation() {
        return true;
    }
}
