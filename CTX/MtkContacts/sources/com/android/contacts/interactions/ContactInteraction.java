package com.android.contacts.interactions;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.Spannable;

public interface ContactInteraction {
    Drawable getBodyIcon(Context context);

    Spannable getContentDescription(Context context);

    Drawable getFooterIcon(Context context);

    Drawable getIcon(Context context);

    int getIconResourceId();

    Intent getIntent();

    long getInteractionDate();

    Drawable getSimIcon(Context context);

    String getSimName(Context context);

    String getViewBody(Context context);

    String getViewFooter(Context context);

    String getViewHeader(Context context);
}
