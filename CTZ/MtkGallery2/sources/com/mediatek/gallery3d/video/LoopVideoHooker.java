package com.mediatek.gallery3d.video;

import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.ext.DefaultActivityHooker;
import com.mediatek.gallery3d.util.Log;

public class LoopVideoHooker extends MovieHooker {
    private static final boolean LOG = true;
    private static final int MENU_LOOP = 1;
    private static final String TAG = "VP_LoopVideoHooker";
    private MenuItem mMenuLoopButton;
    private boolean mNeedShow = LOG;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.mMenuLoopButton = menu.add(DefaultActivityHooker.MENU_HOOKER_GROUP_ID, getMenuActivityId(1), 0, R.string.loop);
        return LOG;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateLoop();
        return LOG;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        super.onOptionsItemSelected(menuItem);
        if (getMenuOriginalId(menuItem.getItemId()) == 1) {
            getPlayer().setLoop(getPlayer().getLoop() ^ LOG);
            updateLoop();
            return LOG;
        }
        return false;
    }

    @Override
    public void setVisibility(boolean z) {
        if (this.mMenuLoopButton != null) {
            this.mMenuLoopButton.setVisible(z);
            this.mNeedShow = z;
            Log.v(TAG, "setVisibility() visible=" + z);
        }
    }

    private void updateLoop() {
        Log.v(TAG, "updateLoop() mLoopButton=" + this.mMenuLoopButton);
        if (this.mMenuLoopButton != null && this.mNeedShow) {
            if (MovieUtils.isLocalFile(getMovieItem().getUri(), getMovieItem().getMimeType())) {
                this.mMenuLoopButton.setVisible(LOG);
            } else {
                this.mMenuLoopButton.setVisible(false);
            }
            if (getPlayer() != null ? getPlayer().getLoop() : false) {
                this.mMenuLoopButton.setTitle(R.string.single);
                this.mMenuLoopButton.setIcon(R.drawable.m_ic_menu_unloop);
            } else {
                this.mMenuLoopButton.setTitle(R.string.loop);
                this.mMenuLoopButton.setIcon(R.drawable.m_ic_menu_loop);
            }
        }
    }
}
