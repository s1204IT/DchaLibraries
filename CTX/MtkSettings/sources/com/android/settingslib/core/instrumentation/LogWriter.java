package com.android.settingslib.core.instrumentation;

import android.content.Context;
import android.util.Pair;

public interface LogWriter {
    void action(int i, boolean z, Pair<Integer, Object>... pairArr);

    @Deprecated
    void action(Context context, int i, int i2);

    void action(Context context, int i, String str, Pair<Integer, Object>... pairArr);

    @Deprecated
    void action(Context context, int i, boolean z);

    void action(Context context, int i, Pair<Integer, Object>... pairArr);

    void actionWithSource(Context context, int i, int i2);

    void count(Context context, String str, int i);

    void hidden(Context context, int i);

    void visible(Context context, int i, int i2);
}
