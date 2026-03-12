package com.android.settings.intelligence.instrumentation;

import com.android.settings.intelligence.nano.SettingsIntelligenceLogProto;

public interface EventLogger {
    void log(SettingsIntelligenceLogProto.SettingsIntelligenceEvent settingsIntelligenceEvent);
}
