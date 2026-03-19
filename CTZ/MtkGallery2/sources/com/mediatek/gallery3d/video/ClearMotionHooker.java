package com.mediatek.gallery3d.video;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.ext.DefaultActivityHooker;
import com.mediatek.gallery3d.util.Log;

public class ClearMotionHooker extends MovieHooker {
    private static final int MENU_CLEAR_MOTION = 1;
    private static final String TAG = "VP_ClearMotionHooker";
    private MenuItem mMenuClearMotion;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.mMenuClearMotion = menu.add(DefaultActivityHooker.MENU_HOOKER_GROUP_ID, getMenuActivityId(1), 0, R.string.clear_motion);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (MtkVideoFeature.isClearMotionMenuEnabled(getContext())) {
            if (this.mMenuClearMotion != null) {
                this.mMenuClearMotion.setVisible(true);
            }
        } else if (this.mMenuClearMotion != null) {
            this.mMenuClearMotion.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        super.onOptionsItemSelected(menuItem);
        if (getMenuOriginalId(menuItem.getItemId()) == 1) {
            gotoClearMotion();
            return true;
        }
        return false;
    }

    private void gotoClearMotion() {
        Log.v(TAG, "gotoClearMotion() entry");
        Intent intent = new Intent(ClearMotionTool.ACTION_ClearMotionTool);
        intent.setClass(getContext(), ClearMotionTool.class);
        getContext().startActivity(intent);
    }
}
