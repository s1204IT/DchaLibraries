package com.mediatek.gallerybasic.base;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MGLView;

public interface LayerManager {
    void destroy();

    void drawLayer(MGLCanvas mGLCanvas, int i, int i2);

    boolean freshLayers(boolean z);

    void init(ViewGroup viewGroup, MGLView mGLView);

    boolean onActionBarVisibilityChange(boolean z);

    boolean onBackPressed();

    boolean onCreateOptionsMenu(Menu menu);

    void onFilmModeChange(boolean z);

    void onKeyEvent(KeyEvent keyEvent);

    void onLayout(boolean z, int i, int i2, int i3, int i4);

    boolean onOptionsItemSelected(MenuItem menuItem);

    boolean onPrepareOptionsMenu(Menu menu);

    boolean onTouch(MotionEvent motionEvent);

    boolean onUpPressed();

    void pause();

    void resume();

    void switchLayer(Player player, MediaData mediaData);
}
