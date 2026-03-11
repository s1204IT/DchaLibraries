package com.android.launcher3.allapps;

import com.android.launcher3.allapps.AlphabeticalAppsList;

final class FullMergeAlgorithm implements AlphabeticalAppsList.MergeAlgorithm {
    FullMergeAlgorithm() {
    }

    @Override
    public boolean continueMerging(AlphabeticalAppsList.SectionInfo section, AlphabeticalAppsList.SectionInfo withSection, int sectionAppCount, int numAppsPerRow, int mergeCount) {
        return section.firstAppItem.viewType == 1;
    }
}
