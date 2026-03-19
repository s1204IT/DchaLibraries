package com.android.gallery3d.app;

import android.os.Bundle;
import com.android.gallery3d.util.GalleryUtils;

public class DialogPicker extends PickerActivity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int iDetermineTypeBits = GalleryUtils.determineTypeBits(this, getIntent());
        setTitle(GalleryUtils.getSelectionModePrompt(iDetermineTypeBits));
        Bundle extras = getIntent().getExtras();
        Bundle bundle2 = extras == null ? new Bundle() : new Bundle(extras);
        bundle2.putBoolean("get-content", true);
        bundle2.putString("media-path", getDataManager().getTopSetPath(iDetermineTypeBits));
        getStateManager().startState(AlbumSetPage.class, bundle2);
    }
}
