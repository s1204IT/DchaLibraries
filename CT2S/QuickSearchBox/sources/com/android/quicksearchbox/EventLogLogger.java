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

    protected Context getContext() {
        return this.mContext;
    }

    protected int getVersionCode() {
        return QsbApplication.get(getContext()).getVersionCode();
    }

    @Override
    public void logStart(int onCreateLatency, int latency, String intentSource) {
        EventLogTags.writeQsbStart(this.mPackageName, getVersionCode(), intentSource, latency, null, null, onCreateLatency);
    }

    @Override
    public void logSuggestionClick(long id, SuggestionCursor suggestionCursor, int clickType) {
        String suggestions = getSuggestions(suggestionCursor);
        int numChars = suggestionCursor.getUserQuery().length();
        EventLogTags.writeQsbClick(id, suggestions, null, numChars, clickType);
    }

    @Override
    public void logSearch(int startMethod, int numChars) {
        EventLogTags.writeQsbSearch(null, startMethod, numChars);
    }

    @Override
    public void logVoiceSearch() {
        EventLogTags.writeQsbVoiceSearch(null);
    }

    @Override
    public void logExit(SuggestionCursor suggestionCursor, int numChars) {
        String suggestions = getSuggestions(suggestionCursor);
        EventLogTags.writeQsbExit(suggestions, numChars);
    }

    @Override
    public void logLatency(SourceResult result) {
    }

    private String getSuggestions(SuggestionCursor cursor) {
        StringBuilder sb = new StringBuilder();
        int count = cursor == null ? 0 : cursor.getCount();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('|');
            }
            cursor.moveTo(i);
            String source = cursor.getSuggestionSource().getName();
            String type = cursor.getSuggestionLogType();
            if (type == null) {
                type = "";
            }
            String shortcut = cursor.isSuggestionShortcut() ? "shortcut" : "";
            sb.append(source).append(':').append(type).append(':').append(shortcut);
        }
        return sb.toString();
    }
}
