package com.mediatek.contacts.ext;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import com.android.contacts.list.ContactListItemView;

public class DefaultContactsCommonPresenceExtension implements IContactsCommonPresenceExtension {
    private static final String TAG = "DefaultContactsCommonPresenceExtension";

    @Override
    public boolean isVideoCallCapable(String str) {
        Log.d(TAG, "[isVideoCallCapable] number:" + str);
        return true;
    }

    @Override
    public boolean isShowVideoIcon() {
        Log.d(TAG, "[isShowVideoIcon] default implementation");
        return false;
    }

    @Override
    public void setVideoIconAlpha(String str, Drawable drawable, boolean z) {
    }

    @Override
    public boolean isVideoIconClickable(Uri uri) {
        return true;
    }

    @Override
    public void addVideoCallView(long j, ViewGroup viewGroup) {
        Log.d(TAG, "[addVideoCallView] contactId:" + j);
    }

    @Override
    public void onMeasure(int i, int i2) {
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
    }

    @Override
    public int getWidthWithPadding() {
        return 0;
    }

    @Override
    public void processIntent(Intent intent) {
        Log.d(TAG, "[processIntent]");
    }

    @Override
    public void bindPhoneNumber(ContactListItemView contactListItemView, Cursor cursor) {
        Log.d(TAG, "[bindPhoneNumber]");
    }

    @Override
    public void addRefreshMenu(Menu menu) {
        Log.d(TAG, "[addRefreshMenu]");
    }

    @Override
    public void onHostActivityResumed(Activity activity) {
    }

    @Override
    public void onHostActivityPaused() {
    }

    @Override
    public void onHostActivityStopped() {
    }

    @Override
    public boolean onOptionsItemSelected(int i, long j) {
        return false;
    }
}
