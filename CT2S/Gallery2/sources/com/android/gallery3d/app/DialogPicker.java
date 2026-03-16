package com.android.gallery3d.app;

import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.util.GalleryUtils;

public class DialogPicker extends PickerActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int typeBits = GalleryUtils.determineTypeBits(this, getIntent());
        setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        Bundle data = extras == null ? new Bundle() : new Bundle(extras);
        data.putBoolean("get-content", true);
        data.putString("media-path", getDataManager().getTopSetPath(typeBits));
        getStateManager().startState(AlbumSetPage.class, data);
    }
}
