package com.mediatek.gallery3d.ext;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mediatek.gallery3d.video.IMovieItem;

public class DefaultActivityHooker implements IActivityHooker {
    public static final int MENU_HOOKER_GROUP_ID = 999;
    private static final int MENU_MAX_NUMBER = 100;
    private static int sMenuId = 1;
    private static Object sMenuLock = new Object();
    private Activity mContext;
    private Intent mIntent;
    private int mMenuId;
    private IMovieItem mMovieItem;

    public DefaultActivityHooker() {
        synchronized (sMenuLock) {
            sMenuId++;
            this.mMenuId = sMenuId * MENU_MAX_NUMBER;
        }
    }

    public int getMenuActivityId(int i) {
        return this.mMenuId + i;
    }

    public int getMenuOriginalId(int i) {
        return i - this.mMenuId;
    }

    @Override
    public void init(Activity activity, Intent intent) {
        this.mContext = activity;
        this.mIntent = intent;
    }

    public Activity getContext() {
        return this.mContext;
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    @Override
    public void onCreate(Bundle bundle) {
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void setVisibility(boolean z) {
    }

    @Override
    public void setParameter(String str, Object obj) {
        if (obj instanceof IMovieItem) {
            this.mMovieItem = (IMovieItem) obj;
            onMovieItemChanged(this.mMovieItem);
        }
    }

    public IMovieItem getMovieItem() {
        return this.mMovieItem;
    }

    public void onMovieItemChanged(IMovieItem iMovieItem) {
    }
}
