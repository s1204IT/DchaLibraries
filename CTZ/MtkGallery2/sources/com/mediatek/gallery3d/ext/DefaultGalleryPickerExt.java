package com.mediatek.gallery3d.ext;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;

public class DefaultGalleryPickerExt implements IGalleryPickerExt {
    private static final String TAG = "Gallery2/DefaultGalleryPickerExt";

    public DefaultGalleryPickerExt(Context context) {
    }

    @Override
    public ActionModeHandler onCreate(AbstractGalleryActivity abstractGalleryActivity, Bundle bundle, ActionModeHandler actionModeHandler, SelectionManager selectionManager) {
        return actionModeHandler;
    }

    @Override
    public void onResume(SelectionManager selectionManager) {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onCreateActionBar(Menu menu) {
    }

    @Override
    public boolean onSingleTapUp(SlotView slotView, MediaItem mediaItem) {
        return false;
    }

    @Override
    public boolean onItemSelected(MenuItem menuItem) {
        return false;
    }
}
