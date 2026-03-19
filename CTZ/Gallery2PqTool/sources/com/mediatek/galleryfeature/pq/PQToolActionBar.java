package com.mediatek.galleryfeature.pq;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import com.mediatek.gallerybasic.base.IActionBar;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.plugin.component.ComponentSupport;
import java.io.File;

public class PQToolActionBar implements IActionBar {
    private static final String PLUGIN_ID = "PQTool";
    private static boolean SUPPORT_PQ = new File(Environment.getExternalStorageDirectory(), "SUPPORT_PQ").exists();
    private static final String TAG = "MtkGallery/PQToolActionBar";
    private Context mContext;
    private MenuItem mMenu;
    private int mMenuId;

    public PQToolActionBar(Context context) {
        Log.d(TAG, "<PQToolActionBar> context = " + context + " this class = " + getClass());
        this.mContext = context;
    }

    @Override
    public void onCreateOptionsMenu(ActionBar actionBar, Menu menu) {
        if (SUPPORT_PQ) {
            this.mMenuId = 2;
            this.mMenu = menu.add(0, this.mMenuId, 1, "PQ");
        }
        Log.d(TAG, "<onCreateOptionsMenu>");
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, MediaData mediaData) {
        if (this.mMenu != null) {
            if (mediaData != null && "image/jpeg".equalsIgnoreCase(mediaData.mimeType)) {
                Log.d(TAG, "<onPrepareOptionsMenu> data mimeType = " + mediaData.mimeType);
                this.mMenu.setVisible(true);
                return;
            }
            this.mMenu.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem, MediaData mediaData) {
        if (this.mMenu != null && mediaData != null && menuItem.getItemId() == this.mMenuId) {
            Intent intent = new Intent();
            intent.setClassName(this.mContext, "com.mediatek.galleryfeature.pq.PictureQualityActivity");
            Bundle bundle = new Bundle();
            bundle.putString("PQUri", mediaData.uri.toString());
            bundle.putString("PQMineType", mediaData.mimeType);
            intent.putExtras(bundle);
            ComponentSupport.startActivity(this.mContext, intent, PLUGIN_ID);
            Log.d(TAG, "<onOptionsItemSelected>  id == Menu.FIRST");
        }
        Log.d(TAG, "<onOptionsItemSelected>");
        return false;
    }
}
