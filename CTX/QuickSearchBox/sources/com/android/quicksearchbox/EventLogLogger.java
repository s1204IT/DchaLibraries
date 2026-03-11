package com.android.quicksearchbox;

import android.content.Context;
import java.util.Random;

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

    private String getSuggestions(SuggestionCursor suggestionCursor) {
        int count;
        int i;
        StringBuilder sb = new StringBuilder();
        if (suggestionCursor == null) {
            count = 0;
            i = 0;
        } else {
            count = suggestionCursor.getCount();
            i = 0;
        }
        while (i < count) {
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
            i++;
        }
        return sb.toString();
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected int getVersionCode() {
        return QsbApplication.get(getContext()).getVersionCode();
    }

    @Override
    public void logExit(SuggestionCursor suggestionCursor, int i) {
        EventLogTags.writeQsbExit(getSuggestions(suggestionCursor), i);
    }

    @Override
    public void logLatency(SourceResult sourceResult) {
    }

    @Override
    public void logSearch(int i, int i2) {
        EventLogTags.writeQsbSearch(null, i, i2);
    }

    @Override
    public void logStart(int i, int i2, String str) {
        EventLogTags.writeQsbStart(this.mPackageName, getVersionCode(), str, i2, null, null, i);
    }

    @Override
    public void logSuggestionClick(long j, SuggestionCursor suggestionCursor, int i) {
        EventLogTags.writeQsbClick(j, getSuggestions(suggestionCursor), null, suggestionCursor.getUserQuery().length(), i);
    }

    @Override
    public void logVoiceSearch() {
        EventLogTags.writeQsbVoiceSearch(null);
    }
}
