package com.mediatek.calendar.extension;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.mediatek.calendar.MTKUtils;

public class EventInfoOptionsMenuExt implements IOptionsMenuExt {
    private static boolean mIsTabletConfig = false;
    private Context mContext;
    private long mEventId;

    public EventInfoOptionsMenuExt(Context context, long j) {
        this.mContext = context;
        this.mEventId = j;
        mIsTabletConfig = Utils.getConfigBool(this.mContext, R.bool.tablet_config);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        MenuItem menuItemFindItem = menu.findItem(R.id.info_action_share);
        if (menuItemFindItem != null && !mIsTabletConfig) {
            menuItemFindItem.setEnabled(true);
            menuItemFindItem.setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(int i) {
        if (R.id.info_action_share == i) {
            MTKUtils.sendShareEvent(this.mContext, this.mEventId);
            return true;
        }
        return false;
    }
}
