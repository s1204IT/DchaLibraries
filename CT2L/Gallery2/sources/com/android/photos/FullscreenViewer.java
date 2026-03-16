package com.android.photos;

import android.app.Activity;
import android.os.Bundle;
import com.android.photos.views.TiledImageView;

public class FullscreenViewer extends Activity {
    private TiledImageView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getData().toString();
        this.mTextureView = new TiledImageView(this);
        this.mTextureView.setTileSource(new BitmapRegionTileSource(this, path, 0, 0), null);
        setContentView(this.mTextureView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mTextureView.destroy();
    }
}
