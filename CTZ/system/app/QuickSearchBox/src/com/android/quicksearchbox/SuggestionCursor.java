package com.android.quicksearchbox;

import com.android.quicksearchbox.util.QuietlyCloseable;
import java.util.Collection;

/* loaded from: classes.dex */
public interface SuggestionCursor extends Suggestion, QuietlyCloseable {
    void close();

    int getCount();

    Collection<String> getExtraColumns();

    String getUserQuery();

    void moveTo(int i);
}
