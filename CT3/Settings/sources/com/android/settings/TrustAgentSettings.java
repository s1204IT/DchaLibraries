package com.android.settings;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import java.util.List;

public class TrustAgentSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private final ArraySet<ComponentName> mActiveAgents = new ArraySet<>();
    private ArrayMap<ComponentName, AgentInfo> mAvailableAgents;
    private DevicePolicyManager mDpm;
    private LockPatternUtils mLockPatternUtils;

    public static final class AgentInfo {
        ComponentName component;
        public Drawable icon;
        CharSequence label;
        SwitchPreference preference;

        public boolean equals(Object other) {
            if (other instanceof AgentInfo) {
                return this.component.equals(((AgentInfo) other).component);
            }
            return true;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 91;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService(DevicePolicyManager.class);
        addPreferencesFromResource(R.xml.trust_agent_settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        removePreference("dummy_preference");
        updateAgents();
    }

    private void updateAgents() {
        Context context = getActivity();
        if (this.mAvailableAgents == null) {
            this.mAvailableAgents = findAvailableTrustAgents();
        }
        if (this.mLockPatternUtils == null) {
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
        }
        loadActiveAgents();
        PreferenceGroup category = (PreferenceGroup) getPreferenceScreen().findPreference("trust_agents");
        category.removeAll();
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(context, 16, UserHandle.myUserId());
        int count = this.mAvailableAgents.size();
        for (int i = 0; i < count; i++) {
            AgentInfo agent = this.mAvailableAgents.valueAt(i);
            RestrictedSwitchPreference preference = new RestrictedSwitchPreference(getPrefContext());
            preference.useAdminDisabledSummary(true);
            agent.preference = preference;
            preference.setPersistent(false);
            preference.setTitle(agent.label);
            preference.setIcon(agent.icon);
            preference.setPersistent(false);
            preference.setOnPreferenceChangeListener(this);
            preference.setChecked(this.mActiveAgents.contains(agent.component));
            if (admin != null && this.mDpm.getTrustAgentConfiguration(null, agent.component) == null) {
                preference.setChecked(false);
                preference.setDisabledByAdmin(admin);
            }
            category.addPreference(agent.preference);
        }
    }

    private void loadActiveAgents() {
        List<ComponentName> activeTrustAgents = this.mLockPatternUtils.getEnabledTrustAgents(UserHandle.myUserId());
        if (activeTrustAgents == null) {
            return;
        }
        this.mActiveAgents.addAll(activeTrustAgents);
    }

    private void saveActiveAgents() {
        this.mLockPatternUtils.setEnabledTrustAgents(this.mActiveAgents, UserHandle.myUserId());
    }

    ArrayMap<ComponentName, AgentInfo> findAvailableTrustAgents() {
        PackageManager pm = getActivity().getPackageManager();
        Intent trustAgentIntent = new Intent("android.service.trust.TrustAgentService");
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(trustAgentIntent, 128);
        ArrayMap<ComponentName, AgentInfo> agents = new ArrayMap<>();
        int count = resolveInfos.size();
        agents.ensureCapacity(count);
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            if (resolveInfo.serviceInfo != null && TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) {
                ComponentName name = TrustAgentUtils.getComponentName(resolveInfo);
                AgentInfo agentInfo = new AgentInfo();
                agentInfo.label = resolveInfo.loadLabel(pm);
                agentInfo.icon = resolveInfo.loadIcon(pm);
                agentInfo.component = name;
                agents.put(name, agentInfo);
            }
        }
        return agents;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof SwitchPreference) {
            int count = this.mAvailableAgents.size();
            for (int i = 0; i < count; i++) {
                AgentInfo agent = this.mAvailableAgents.valueAt(i);
                if (agent.preference == preference) {
                    if (((Boolean) newValue).booleanValue()) {
                        if (!this.mActiveAgents.contains(agent.component)) {
                            this.mActiveAgents.add(agent.component);
                        }
                    } else {
                        this.mActiveAgents.remove(agent.component);
                    }
                    saveActiveAgents();
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
