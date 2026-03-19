package com.mediatek.contacts.ext;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Menu;
import android.view.ViewGroup;
import com.android.contacts.list.ContactListItemView;

public interface IContactsCommonPresenceExtension {
    void addRefreshMenu(Menu menu);

    void addVideoCallView(long j, ViewGroup viewGroup);

    void bindPhoneNumber(ContactListItemView contactListItemView, Cursor cursor);

    int getWidthWithPadding();

    boolean isShowVideoIcon();

    boolean isVideoCallCapable(String str);

    boolean isVideoIconClickable(Uri uri);

    void onHostActivityPaused();

    void onHostActivityResumed(Activity activity);

    void onHostActivityStopped();

    void onLayout(boolean z, int i, int i2, int i3, int i4);

    void onMeasure(int i, int i2);

    boolean onOptionsItemSelected(int i, long j);

    void processIntent(Intent intent);

    void setVideoIconAlpha(String str, Drawable drawable, boolean z);
}
