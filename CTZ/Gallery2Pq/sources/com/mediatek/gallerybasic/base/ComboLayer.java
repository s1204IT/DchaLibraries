package com.mediatek.gallerybasic.base;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.gl.MGLView;
import com.mediatek.gallerybasic.gl.MGLViewGroup;
import java.util.ArrayList;
import java.util.Iterator;

public class ComboLayer extends Layer implements Layer.LayerNotifier {
    static final boolean $assertionsDisabled = false;
    private static final String TAG = "MtkGallery2/ComboLayer";
    private Context mContext;
    private ArrayList<Layer> mLayers;
    private MGLViewGroup mMGLViewGroup;
    private ViewGroup mViewGroup;

    public ComboLayer(Context context, ArrayList<Layer> arrayList) {
        this.mContext = context;
        this.mLayers = arrayList;
    }

    @Override
    public void onCreate(Activity activity, ViewGroup viewGroup) {
        for (Layer layer : this.mLayers) {
            layer.onCreate(activity, viewGroup);
            layer.setLayerNotifier(this);
        }
    }

    @Override
    public void onResume(boolean z) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onResume(z);
        }
    }

    @Override
    public void onPause() {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onPause();
        }
    }

    @Override
    public void onDestroy() {
        for (Layer layer : this.mLayers) {
            layer.onDestroy();
            layer.setLayerNotifier(null);
        }
    }

    @Override
    public void setData(MediaData mediaData) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().setData(mediaData);
        }
    }

    @Override
    public void setPlayer(Player player) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().setPlayer(player);
        }
    }

    @Override
    public View getView() {
        if (this.mViewGroup == null) {
            this.mViewGroup = new RelativeLayout(this.mContext);
            Iterator<Layer> it = this.mLayers.iterator();
            while (it.hasNext()) {
                View view = it.next().getView();
                if (view != null) {
                    this.mViewGroup.addView(view);
                }
            }
        }
        return this.mViewGroup;
    }

    @Override
    public MGLView getMGLView() {
        if (this.mMGLViewGroup == null) {
            ArrayList arrayList = new ArrayList();
            Iterator<Layer> it = this.mLayers.iterator();
            while (it.hasNext()) {
                MGLView mGLView = it.next().getMGLView();
                if (mGLView != null) {
                    arrayList.add(mGLView);
                }
            }
            if (arrayList.size() >= 1) {
                this.mMGLViewGroup = new MGLViewGroup(arrayList);
            }
        }
        return this.mMGLViewGroup;
    }

    @Override
    public void onActivityResume() {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onActivityResume();
        }
    }

    @Override
    public void onActivityPause() {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onActivityPause();
        }
    }

    @Override
    public boolean onSingleTapUp(float f, float f2) {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onSingleTapUp(f, f2)) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public boolean onDoubleTap(float f, float f2) {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onDoubleTap(f, f2)) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public boolean onScroll(float f, float f2, float f3, float f4) {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onScroll(f, f2, f3, f4)) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onFling(motionEvent, motionEvent2, f, f2)) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public boolean onScaleBegin(float f, float f2) {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onScaleBegin(f, f2)) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public boolean onScale(float f, float f2, float f3) {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onScale(f, f2, f3)) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public void onScaleEnd() {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onScaleEnd();
        }
    }

    @Override
    public void onDown(float f, float f2) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onDown(f, f2);
        }
    }

    @Override
    public void onUp() {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onUp();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onCreateOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onPrepareOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onOptionsItemSelected(menuItem);
        }
        return true;
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onKeyEvent(keyEvent);
        }
    }

    @Override
    public boolean onUpPressed() {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onUpPressed()) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public boolean onBackPressed() {
        boolean z = false;
        for (Layer layer : this.mLayers) {
            if (z || layer.onBackPressed()) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public boolean onActionBarVisibilityChange(boolean z) {
        boolean z2 = false;
        for (Layer layer : this.mLayers) {
            if (z2 || layer.onActionBarVisibilityChange(z)) {
                z2 = true;
            } else {
                z2 = false;
            }
        }
        return z2;
    }

    @Override
    public void onFilmModeChange(boolean z) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onFilmModeChange(z);
        }
    }

    @Override
    public boolean fresh(boolean z) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().fresh(z);
        }
        return $assertionsDisabled;
    }

    @Override
    public void onChange(Player player, int i, int i2, Object obj) {
        Iterator<Layer> it = this.mLayers.iterator();
        while (it.hasNext()) {
            it.next().onChange(player, i, i2, obj);
        }
    }

    @Override
    public void sendMessage(Layer layer, int i) {
        for (Layer layer2 : this.mLayers) {
            if (layer2 != layer) {
                layer2.onReceiveMessage(i);
            }
        }
    }
}
