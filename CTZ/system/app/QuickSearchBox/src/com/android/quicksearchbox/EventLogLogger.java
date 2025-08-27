package com.android.quicksearchbox;

import android.content.Context;
import java.util.Random;

/* loaded from: classes.dex */
public class EventLogLogger implements Logger {
    private final Config mConfig;
    private final Context mContext;
    private final String mPackageName;
    private final Random mRandom = new Random();

    public EventLogLogger(Context context, Config config) {
        this.mContext = context;
        this.mConfig = config;
        this.mPackageName = this.mContext.getPackageName();
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected int getVersionCode() {
        return QsbApplication.get(getContext()).getVersionCode();
    }

    @Override // com.android.quicksearchbox.Logger
    public void logStart(int i, int i2, String str) {
        EventLogTags.writeQsbStart(this.mPackageName, getVersionCode(), str, i2, null, null, i);
    }

    @Override // com.android.quicksearchbox.Logger
    public void logSuggestionClick(long j, SuggestionCursor suggestionCursor, int i) {
        EventLogTags.writeQsbClick(j, getSuggestions(suggestionCursor), null, suggestionCursor.getUserQuery().length(), i);
    }

    @Override // com.android.quicksearchbox.Logger
    public void logSearch(int i, int i2) {
        EventLogTags.writeQsbSearch(null, i, i2);
    }

    @Override // com.android.quicksearchbox.Logger
    public void logVoiceSearch() {
        EventLogTags.writeQsbVoiceSearch(null);
    }

    @Override // com.android.quicksearchbox.Logger
    public void logExit(SuggestionCursor suggestionCursor, int i) {
        EventLogTags.writeQsbExit(getSuggestions(suggestionCursor), i);
    }

    @Override // com.android.quicksearchbox.Logger
    public void logLatency(SourceResult sourceResult) {
    }

    private String getSuggestions(SuggestionCursor suggestionCursor) {
        int count;
        StringBuilder sb = new StringBuilder();
        if (suggestionCursor != null) {
            count = suggestionCursor.getCount();
        } else {
            count = 0;
        }
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('|');
            }
            suggestionCursor.moveTo(i);
            String name = suggestionCursor.getSuggestionSource().getName();
            String suggestionLogType = suggestionCursor.getSuggestionLogType();
            if (suggestionLogType == null) {
                suggestionLogType = "";
            }
            String str = suggestionCursor.isSuggestionShortcut() ? "shortcut" : "";
            sb.append(name);
            sb.append(':');
            sb.append(suggestionLogType);
            sb.append(':');
            sb.append(str);
        }
        return sb.toString();
    }
}
