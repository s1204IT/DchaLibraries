package com.mediatek.gallerybasic.base;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.gl.MGLView;

public abstract class Layer implements Player.PlayListener {
    public static final int MSG_BOTTOM_CONTROL_HIDE = 1;
    public static final int MSG_BOTTOM_CONTROL_SHOW = 2;
    private LayerNotifier mLayerNotifier;

    public interface LayerNotifier {
        void sendMessage(Layer layer, int i);
    }

    public abstract MGLView getMGLView();

    public abstract View getView();

    public abstract void onCreate(Activity activity, ViewGroup viewGroup);

    public abstract void onDestroy();

    public abstract void onPause();

    public abstract void onResume(boolean z);

    public abstract void setData(MediaData mediaData);

    public abstract void setPlayer(Player player);

    public void sendMessageToNotifier(int i) {
        if (this.mLayerNotifier != null) {
            this.mLayerNotifier.sendMessage(this, i);
        }
    }

    public void onReceiveMessage(int i) {
    }

    public void setLayerNotifier(LayerNotifier layerNotifier) {
        this.mLayerNotifier = layerNotifier;
    }

    public void onActivityResume() {
    }

    public void onActivityPause() {
    }

    public boolean onSingleTapUp(float f, float f2) {
        return false;
    }

    public boolean onDoubleTap(float f, float f2) {
        return false;
    }

    public boolean onScroll(float f, float f2, float f3, float f4) {
        return false;
    }

    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    public boolean onScaleBegin(float f, float f2) {
        return false;
    }

    public boolean onScale(float f, float f2, float f3) {
        return false;
    }

    public void onScaleEnd() {
    }

    public void onDown(float f, float f2) {
    }

    public void onUp() {
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return true;
    }

    public void onKeyEvent(KeyEvent keyEvent) {
    }

    public boolean onBackPressed() {
        return false;
    }

    public boolean onUpPressed() {
        return false;
    }

    public boolean onActionBarVisibilityChange(boolean z) {
        return false;
    }

    public void onFilmModeChange(boolean z) {
    }

    public boolean fresh(boolean z) {
        return false;
    }
}
