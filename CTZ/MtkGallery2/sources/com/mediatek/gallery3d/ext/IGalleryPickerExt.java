package com.mediatek.gallery3d.ext;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;

public interface IGalleryPickerExt {
    ActionModeHandler onCreate(AbstractGalleryActivity abstractGalleryActivity, Bundle bundle, ActionModeHandler actionModeHandler, SelectionManager selectionManager);

    void onCreateActionBar(Menu menu);

    void onDestroy();

    boolean onItemSelected(MenuItem menuItem);

    void onPause();

    void onResume(SelectionManager selectionManager);

    boolean onSingleTapUp(SlotView slotView, MediaItem mediaItem);
}
