package com.mediatek.providers.calendar.extension;

public class ExtensionFactory {
    public static ITableExt getCalendarsTableExt(String str) {
        return new PCSyncAccountExt(str);
    }

    public static IDatabaseUpgradeExt getDatabaseUpgradeExt() {
        return new MTKDatabaseUpgradeExt();
    }
}
