package com.android.settings.intelligence.instrumentation;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.intelligence.nano.SettingsIntelligenceLogProto;
import com.android.settings.intelligence.search.SearchResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MetricsFeatureProvider {
    protected Context mContext;
    protected List<EventLogger> mLoggers = new ArrayList();

    public MetricsFeatureProvider(Context context) {
        this.mContext = context;
        this.mLoggers.add(new LocalEventLogger());
    }

    public void logGetSuggestion(List<String> list, long j) {
        SettingsIntelligenceLogProto.SettingsIntelligenceEvent settingsIntelligenceEvent = new SettingsIntelligenceLogProto.SettingsIntelligenceEvent();
        settingsIntelligenceEvent.eventType = 1;
        settingsIntelligenceEvent.latencyMillis = j;
        if (list != null) {
            settingsIntelligenceEvent.suggestionIds = (String[]) list.toArray(new String[0]);
        }
        logEvent(settingsIntelligenceEvent);
    }

    public void logDismissSuggestion(String str, long j) {
        SettingsIntelligenceLogProto.SettingsIntelligenceEvent settingsIntelligenceEvent = new SettingsIntelligenceLogProto.SettingsIntelligenceEvent();
        settingsIntelligenceEvent.eventType = 2;
        settingsIntelligenceEvent.latencyMillis = j;
        if (!TextUtils.isEmpty(str)) {
            settingsIntelligenceEvent.suggestionIds = new String[]{str};
        }
        logEvent(settingsIntelligenceEvent);
    }

    public void logLaunchSuggestion(String str, long j) {
        SettingsIntelligenceLogProto.SettingsIntelligenceEvent settingsIntelligenceEvent = new SettingsIntelligenceLogProto.SettingsIntelligenceEvent();
        settingsIntelligenceEvent.eventType = 3;
        settingsIntelligenceEvent.latencyMillis = j;
        if (!TextUtils.isEmpty(str)) {
            settingsIntelligenceEvent.suggestionIds = new String[]{str};
        }
        logEvent(settingsIntelligenceEvent);
    }

    public void logSearchResultClick(SearchResult searchResult, String str, int i, int i2, int i3) {
        SettingsIntelligenceLogProto.SettingsIntelligenceEvent settingsIntelligenceEvent = new SettingsIntelligenceLogProto.SettingsIntelligenceEvent();
        settingsIntelligenceEvent.eventType = i;
        settingsIntelligenceEvent.searchResultMetadata = new SettingsIntelligenceLogProto.SettingsIntelligenceEvent.SearchResultMetadata();
        settingsIntelligenceEvent.searchResultMetadata.resultCount = i2;
        settingsIntelligenceEvent.searchResultMetadata.searchResultRank = i3;
        settingsIntelligenceEvent.searchResultMetadata.searchResultKey = searchResult.dataKey != null ? searchResult.dataKey : "";
        settingsIntelligenceEvent.searchResultMetadata.searchQueryLength = str != null ? str.length() : 0;
        logEvent(settingsIntelligenceEvent);
    }

    public void logEvent(int i) {
        logEvent(i, 0L);
    }

    public void logEvent(int i, long j) {
        SettingsIntelligenceLogProto.SettingsIntelligenceEvent settingsIntelligenceEvent = new SettingsIntelligenceLogProto.SettingsIntelligenceEvent();
        settingsIntelligenceEvent.eventType = i;
        settingsIntelligenceEvent.latencyMillis = j;
        logEvent(settingsIntelligenceEvent);
    }

    private void logEvent(SettingsIntelligenceLogProto.SettingsIntelligenceEvent settingsIntelligenceEvent) {
        Iterator<EventLogger> it = this.mLoggers.iterator();
        while (it.hasNext()) {
            it.next().log(settingsIntelligenceEvent);
        }
    }
}
