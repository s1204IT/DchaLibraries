package com.android.server.notification;

public interface RankingConfig {
    int getPackagePriority(String str, int i);

    int getPackageVisibilityOverride(String str, int i);

    void setPackagePriority(String str, int i, int i2);

    void setPackageVisibilityOverride(String str, int i, int i2);
}
