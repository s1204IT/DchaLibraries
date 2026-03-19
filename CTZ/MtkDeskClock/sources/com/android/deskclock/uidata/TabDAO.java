package com.android.deskclock.uidata;

import android.content.SharedPreferences;
import com.android.deskclock.uidata.UiDataModel;

final class TabDAO {
    private static final String KEY_SELECTED_TAB = "selected_tab";

    private TabDAO() {
    }

    static UiDataModel.Tab getSelectedTab(SharedPreferences sharedPreferences) {
        return UiDataModel.Tab.values()[sharedPreferences.getInt(KEY_SELECTED_TAB, UiDataModel.Tab.CLOCKS.ordinal())];
    }

    static void setSelectedTab(SharedPreferences sharedPreferences, UiDataModel.Tab tab) {
        sharedPreferences.edit().putInt(KEY_SELECTED_TAB, tab.ordinal()).apply();
    }
}
