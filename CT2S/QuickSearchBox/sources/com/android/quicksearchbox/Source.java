package com.android.quicksearchbox;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import com.android.quicksearchbox.util.NowOrLater;

public interface Source extends SuggestionCursorProvider<SourceResult> {
    Intent createSearchIntent(String str, Bundle bundle);

    Intent createVoiceSearchIntent(Bundle bundle);

    String getDefaultIntentAction();

    String getDefaultIntentData();

    NowOrLater<Drawable> getIcon(String str);

    Uri getIconUri(String str);

    ComponentName getIntentComponent();

    Source getRoot();

    Drawable getSourceIcon();

    Uri getSourceIconUri();
}
