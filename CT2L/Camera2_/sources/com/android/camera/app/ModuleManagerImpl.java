package com.android.camera.app;

import android.content.Context;
import android.util.SparseArray;
import com.android.camera.app.ModuleManager;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import com.android.camera2.R;
import java.util.ArrayList;
import java.util.List;

public class ModuleManagerImpl implements ModuleManager {
    private static final Log.Tag TAG = new Log.Tag("ModuleManagerImpl");
    private final SparseArray<ModuleManager.ModuleAgent> mRegisteredModuleAgents = new SparseArray<>(2);
    private int mDefaultModuleId = -1;

    @Override
    public void registerModule(ModuleManager.ModuleAgent agent) {
        if (agent == null) {
            throw new NullPointerException("Registering a null ModuleAgent.");
        }
        int moduleId = agent.getModuleId();
        if (moduleId == -1) {
            throw new IllegalArgumentException("ModuleManager: The module ID can not be MODULE_INDEX_NONE");
        }
        if (this.mRegisteredModuleAgents.get(moduleId) != null) {
            throw new IllegalArgumentException("Module ID is registered already:" + moduleId);
        }
        this.mRegisteredModuleAgents.put(moduleId, agent);
    }

    @Override
    public boolean unregisterModule(int moduleId) {
        if (this.mRegisteredModuleAgents.get(moduleId) == null) {
            return false;
        }
        this.mRegisteredModuleAgents.delete(moduleId);
        if (moduleId == this.mDefaultModuleId) {
            this.mDefaultModuleId = -1;
        }
        return true;
    }

    @Override
    public List<ModuleManager.ModuleAgent> getRegisteredModuleAgents() {
        List<ModuleManager.ModuleAgent> agents = new ArrayList<>();
        for (int i = 0; i < this.mRegisteredModuleAgents.size(); i++) {
            agents.add(this.mRegisteredModuleAgents.valueAt(i));
        }
        return agents;
    }

    @Override
    public List<Integer> getSupportedModeIndexList() {
        List<Integer> modeIndexList = new ArrayList<>();
        for (int i = 0; i < this.mRegisteredModuleAgents.size(); i++) {
            modeIndexList.add(Integer.valueOf(this.mRegisteredModuleAgents.keyAt(i)));
        }
        return modeIndexList;
    }

    @Override
    public boolean setDefaultModuleIndex(int moduleId) {
        if (this.mRegisteredModuleAgents.get(moduleId) == null) {
            return false;
        }
        this.mDefaultModuleId = moduleId;
        return true;
    }

    @Override
    public int getDefaultModuleIndex() {
        return this.mDefaultModuleId;
    }

    @Override
    public ModuleManager.ModuleAgent getModuleAgent(int moduleId) {
        ModuleManager.ModuleAgent agent = this.mRegisteredModuleAgents.get(moduleId);
        return agent == null ? this.mRegisteredModuleAgents.get(this.mDefaultModuleId) : agent;
    }

    @Override
    public int getQuickSwitchToModuleId(int moduleId, SettingsManager settingsManager, Context context) {
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        int videoModuleId = context.getResources().getInteger(R.integer.camera_mode_video);
        int quickSwitchTo = moduleId;
        if (moduleId == photoModuleId || moduleId == context.getResources().getInteger(R.integer.camera_mode_gcam)) {
            quickSwitchTo = videoModuleId;
        } else if (moduleId == videoModuleId) {
            quickSwitchTo = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED).intValue();
        }
        return this.mRegisteredModuleAgents.get(quickSwitchTo) != null ? quickSwitchTo : moduleId;
    }
}
