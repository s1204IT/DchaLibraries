package com.mediatek.calendar.extension;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import com.android.calendar.R;
import com.mediatek.calendar.clearevents.SelectClearableCalendarsActivity;

public class ClearAllEventsExt implements IOptionsMenuExt {
    Context mContext;

    public ClearAllEventsExt(Context context) {
        this.mContext = context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_delete_all_events).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(int i) {
        if (R.id.action_delete_all_events == i) {
            Log.w("ClearAllEventsExt", "delete all events.");
            launchSelectClearableCalendars();
            return true;
        }
        return false;
    }

    private void launchSelectClearableCalendars() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this.mContext, SelectClearableCalendarsActivity.class);
        intent.setFlags(537001984);
        this.mContext.startActivity(intent);
    }
}
