package com.mediatek.gallery3d.ext;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public interface IActivityHooker {
    void init(Activity activity, Intent intent);

    void onCreate(Bundle bundle);

    boolean onCreateOptionsMenu(Menu menu);

    void onDestroy();

    boolean onOptionsItemSelected(MenuItem menuItem);

    void onPause();

    boolean onPrepareOptionsMenu(Menu menu);

    void onResume();

    void onStart();

    void onStop();

    void setParameter(String str, Object obj);

    void setVisibility(boolean z);
}
