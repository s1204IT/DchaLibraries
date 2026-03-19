package com.mediatek.gallerybasic.dynamic;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.base.LayerManager;
import com.mediatek.gallerybasic.base.MediaCenter;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.dynamic.PlayGestureRecognizer;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MGLView;
import com.mediatek.gallerybasic.util.Log;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class LayerManagerImpl implements LayerManager {
    static final boolean $assertionsDisabled = false;
    private static final String TAG = "MtkGallery2/LayerManagerImpl";
    private Activity mActivity;
    private Layer mCurrentLayer;
    private Player mCurrentPlayer;
    private MGLView mGLRootView;
    private PlayGestureRecognizer mGesureRecognizer;
    private boolean mIsFilmMode;
    private final LinkedHashMap<Integer, Layer> mLayers;
    private MediaCenter mMediaCenter;
    private Menu mOptionsMenu;
    private ViewGroup mRootView;

    public LayerManagerImpl(Activity activity, MediaCenter mediaCenter) {
        this.mMediaCenter = mediaCenter;
        this.mLayers = mediaCenter.getAllLayer();
        this.mActivity = activity;
        this.mGesureRecognizer = new PlayGestureRecognizer(activity.getApplicationContext(), new GestureListener());
    }

    @Override
    public boolean onTouch(MotionEvent motionEvent) {
        return this.mGesureRecognizer.onTouch(motionEvent);
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent) {
        if (this.mCurrentLayer != null) {
            this.mCurrentLayer.onKeyEvent(keyEvent);
        }
    }

    @Override
    public void onFilmModeChange(boolean z) {
        if (z == this.mIsFilmMode) {
            return;
        }
        this.mIsFilmMode = z;
        if (this.mCurrentLayer != null) {
            this.mCurrentLayer.onFilmModeChange(this.mIsFilmMode);
        }
    }

    @Override
    public void init(ViewGroup viewGroup, MGLView mGLView) {
        this.mRootView = viewGroup;
        this.mGLRootView = mGLView;
        Iterator<Map.Entry<Integer, Layer>> it = this.mLayers.entrySet().iterator();
        while (it.hasNext()) {
            Layer value = it.next().getValue();
            if (value != null) {
                value.onCreate(this.mActivity, this.mRootView);
            }
        }
    }

    @Override
    public void resume() {
        Iterator<Map.Entry<Integer, Layer>> it = this.mLayers.entrySet().iterator();
        while (it.hasNext()) {
            Layer value = it.next().getValue();
            if (value != null) {
                if (this.mRootView != null && value.getView() != null) {
                    this.mRootView.addView(value.getView());
                }
                if (this.mGLRootView != null && value.getMGLView() != null) {
                    this.mGLRootView.addComponent(value.getMGLView());
                }
                value.onActivityResume();
            }
        }
    }

    @Override
    public void pause() {
        Iterator<Map.Entry<Integer, Layer>> it = this.mLayers.entrySet().iterator();
        while (it.hasNext()) {
            Layer value = it.next().getValue();
            if (value != null) {
                value.onActivityPause();
            }
        }
        unbind();
        Iterator<Map.Entry<Integer, Layer>> it2 = this.mLayers.entrySet().iterator();
        while (it2.hasNext()) {
            Layer value2 = it2.next().getValue();
            if (value2 != null) {
                if (this.mRootView != null && value2.getView() != null) {
                    this.mRootView.removeView(value2.getView());
                }
                if (this.mGLRootView != null && value2.getMGLView() != null) {
                    this.mGLRootView.removeComponent(value2.getMGLView());
                }
            }
        }
    }

    @Override
    public void destroy() {
        Iterator<Map.Entry<Integer, Layer>> it = this.mLayers.entrySet().iterator();
        while (it.hasNext()) {
            Layer value = it.next().getValue();
            if (value != null) {
                value.onDestroy();
            }
        }
    }

    @Override
    public void switchLayer(Player player, MediaData mediaData) {
        Layer layer;
        if (mediaData != null) {
            layer = this.mMediaCenter.getLayer(this.mActivity, mediaData);
            if (this.mCurrentPlayer != null && this.mCurrentPlayer == player && this.mCurrentLayer == layer) {
                Log.d(TAG, "<switchLayer> same layer and player, return");
                return;
            }
        } else {
            layer = null;
        }
        unbind();
        if (mediaData == null) {
            Log.d(TAG, "<switchLayer> null player or data, return");
        } else if (layer != null) {
            bind(player, layer, mediaData);
        }
    }

    @Override
    public void drawLayer(MGLCanvas mGLCanvas, int i, int i2) {
        if (this.mCurrentLayer != null && this.mCurrentLayer.getMGLView() != null) {
            this.mCurrentLayer.getMGLView().doDraw(mGLCanvas, i, i2);
        }
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        Iterator<Map.Entry<Integer, Layer>> it = this.mLayers.entrySet().iterator();
        while (it.hasNext()) {
            Layer value = it.next().getValue();
            if (value != null && value.getMGLView() != null) {
                value.getMGLView().doLayout(z, i, i2, i3, i4);
            }
        }
    }

    private void bind(Player player, Layer layer, MediaData mediaData) {
        this.mCurrentLayer = layer;
        this.mCurrentPlayer = player;
        this.mCurrentLayer.setPlayer(player);
        this.mCurrentLayer.setData(mediaData);
        if (this.mCurrentPlayer != null) {
            this.mCurrentPlayer.registerPlayListener(this.mCurrentLayer);
        }
        if (this.mOptionsMenu != null) {
            this.mCurrentLayer.onPrepareOptionsMenu(this.mOptionsMenu);
        }
        this.mCurrentLayer.onResume(this.mIsFilmMode);
    }

    private void unbind() {
        if (this.mCurrentPlayer != null) {
            this.mCurrentPlayer.unRegisterPlayListener(this.mCurrentLayer);
        }
        if (this.mCurrentLayer == null) {
            return;
        }
        this.mCurrentLayer.onPause();
        this.mCurrentLayer.setPlayer(null);
        this.mCurrentLayer.setData(null);
        this.mCurrentPlayer = null;
        this.mCurrentLayer = null;
    }

    private class GestureListener implements PlayGestureRecognizer.Listener {
        private GestureListener() {
        }

        @Override
        public boolean onSingleTapUp(float f, float f2) {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                return LayerManagerImpl.this.mCurrentLayer.onSingleTapUp(f, f2);
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(float f, float f2) {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                return LayerManagerImpl.this.mCurrentLayer.onDoubleTap(f, f2);
            }
            return false;
        }

        @Override
        public boolean onScroll(float f, float f2, float f3, float f4) {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                return LayerManagerImpl.this.mCurrentLayer.onScroll(f, f2, f3, f4);
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                return LayerManagerImpl.this.mCurrentLayer.onFling(motionEvent, motionEvent2, f, f2);
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(float f, float f2) {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                return LayerManagerImpl.this.mCurrentLayer.onScaleBegin(f, f2);
            }
            return false;
        }

        @Override
        public boolean onScale(float f, float f2, float f3) {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                return LayerManagerImpl.this.mCurrentLayer.onScale(f, f2, f3);
            }
            return false;
        }

        @Override
        public void onScaleEnd() {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                LayerManagerImpl.this.mCurrentLayer.onScaleEnd();
            }
        }

        @Override
        public void onDown(float f, float f2) {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                LayerManagerImpl.this.mCurrentLayer.onDown(f, f2);
            }
        }

        @Override
        public void onUp() {
            if (LayerManagerImpl.this.mCurrentLayer != null) {
                LayerManagerImpl.this.mCurrentLayer.onUp();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Iterator<Map.Entry<Integer, Layer>> it = this.mLayers.entrySet().iterator();
        while (it.hasNext()) {
            Layer value = it.next().getValue();
            if (value != null) {
                value.onCreateOptionsMenu(menu);
            }
        }
        this.mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mCurrentLayer != null) {
            this.mCurrentLayer.onPrepareOptionsMenu(menu);
            return true;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mCurrentLayer != null) {
            this.mCurrentLayer.onOptionsItemSelected(menuItem);
            return true;
        }
        return true;
    }

    @Override
    public boolean onActionBarVisibilityChange(boolean z) {
        if (this.mCurrentLayer != null) {
            return this.mCurrentLayer.onActionBarVisibilityChange(z);
        }
        return false;
    }

    @Override
    public boolean freshLayers(boolean z) {
        if (this.mLayers != null) {
            Iterator<Map.Entry<Integer, Layer>> it = this.mLayers.entrySet().iterator();
            while (it.hasNext()) {
                Layer value = it.next().getValue();
                if (value != null) {
                    if (this.mCurrentLayer == value) {
                        value.fresh(z);
                    } else {
                        value.fresh(false);
                    }
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onBackPressed() {
        if (this.mCurrentLayer != null) {
            return this.mCurrentLayer.onBackPressed();
        }
        return false;
    }

    @Override
    public boolean onUpPressed() {
        if (this.mCurrentLayer != null) {
            return this.mCurrentLayer.onUpPressed();
        }
        return false;
    }
}
