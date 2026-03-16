package com.android.camera.app;

import android.content.Context;
import com.android.camera.module.ModuleController;
import com.android.camera.settings.SettingsManager;
import java.util.List;

public interface ModuleManager {
    public static final int MODULE_INDEX_NONE = -1;

    public interface ModuleAgent {
        ModuleController createModule(AppController appController);

        int getModuleId();

        boolean requestAppForCamera();
    }

    int getDefaultModuleIndex();

    ModuleAgent getModuleAgent(int i);

    int getQuickSwitchToModuleId(int i, SettingsManager settingsManager, Context context);

    List<ModuleAgent> getRegisteredModuleAgents();

    List<Integer> getSupportedModeIndexList();

    void registerModule(ModuleAgent moduleAgent);

    boolean setDefaultModuleIndex(int i);

    boolean unregisterModule(int i);
}
