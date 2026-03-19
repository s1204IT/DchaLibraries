package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mediatek.gallery3d.ext.IActivityHooker;
import java.util.ArrayList;
import java.util.Iterator;

public class ActivityHookerGroup extends MovieHooker {
    private ArrayList<IActivityHooker> mHooks = new ArrayList<>();

    public boolean addHooker(IActivityHooker iActivityHooker) {
        return this.mHooks.add(iActivityHooker);
    }

    public boolean removeHooker(IActivityHooker iActivityHooker) {
        return this.mHooks.remove(iActivityHooker);
    }

    public int size() {
        return this.mHooks.size();
    }

    public IActivityHooker getHooker(int i) {
        return this.mHooks.get(i);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().onCreate(bundle);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().onStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().onDestroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        boolean z = false;
        while (it.hasNext()) {
            boolean zOnCreateOptionsMenu = it.next().onCreateOptionsMenu(menu);
            if (!z) {
                z = zOnCreateOptionsMenu;
            }
        }
        return z;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        boolean z = false;
        while (it.hasNext()) {
            boolean zOnPrepareOptionsMenu = it.next().onPrepareOptionsMenu(menu);
            if (!z) {
                z = zOnPrepareOptionsMenu;
            }
        }
        return z;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        super.onOptionsItemSelected(menuItem);
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        boolean z = false;
        while (it.hasNext()) {
            boolean zOnOptionsItemSelected = it.next().onOptionsItemSelected(menuItem);
            if (!z) {
                z = zOnOptionsItemSelected;
            }
        }
        return z;
    }

    @Override
    public void setParameter(String str, Object obj) {
        super.setParameter(str, obj);
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().setParameter(str, obj);
        }
    }

    @Override
    public void setVisibility(boolean z) {
        super.setVisibility(z);
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().setVisibility(z);
        }
    }

    @Override
    public void init(Activity activity, Intent intent) {
        super.init(activity, intent);
        Iterator<IActivityHooker> it = this.mHooks.iterator();
        while (it.hasNext()) {
            it.next().init(activity, intent);
        }
    }
}
