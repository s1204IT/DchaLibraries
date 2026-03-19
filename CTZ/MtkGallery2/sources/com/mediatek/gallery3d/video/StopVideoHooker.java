package com.mediatek.gallery3d.video;

import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.ext.DefaultActivityHooker;

public class StopVideoHooker extends MovieHooker {
    private static final int MENU_STOP = 1;
    private MenuItem mMenuStop;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.mMenuStop = menu.add(DefaultActivityHooker.MENU_HOOKER_GROUP_ID, getMenuActivityId(1), 0, R.string.stop);
        return true;
    }

    @Override
    public void setVisibility(boolean z) {
        if (this.mMenuStop != null) {
            this.mMenuStop.setVisible(z);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateStop();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        super.onOptionsItemSelected(menuItem);
        if (getMenuOriginalId(menuItem.getItemId()) == 1) {
            getPlayer().stopVideo();
            return true;
        }
        return false;
    }

    private void updateStop() {
        if (getPlayer() != null && this.mMenuStop != null) {
            this.mMenuStop.setEnabled(getPlayer().canStop());
        }
    }
}
