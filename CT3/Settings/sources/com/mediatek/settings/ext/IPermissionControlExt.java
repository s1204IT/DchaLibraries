package com.mediatek.settings.ext;

import android.provider.SearchIndexableData;
import android.support.v7.preference.PreferenceGroup;
import java.util.List;

public interface IPermissionControlExt {
    void addAutoBootPrf(PreferenceGroup preferenceGroup);

    void addPermSwitchPrf(PreferenceGroup preferenceGroup);

    void enablerPause();

    void enablerResume();

    List<SearchIndexableData> getRawDataToIndex(boolean z);
}
