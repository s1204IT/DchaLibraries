package com.android.settingslib;

import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.Time;
import com.android.internal.util.Preconditions;
import com.google.android.collect.Lists;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class NetworkPolicyEditor {
    private ArrayList<NetworkPolicy> mPolicies = Lists.newArrayList();
    private NetworkPolicyManager mPolicyManager;

    public NetworkPolicyEditor(NetworkPolicyManager policyManager) {
        this.mPolicyManager = (NetworkPolicyManager) Preconditions.checkNotNull(policyManager);
    }

    public void read() {
        NetworkPolicy[] policies = this.mPolicyManager.getNetworkPolicies();
        boolean modified = false;
        this.mPolicies.clear();
        for (NetworkPolicy policy : policies) {
            if (policy.limitBytes < -1) {
                policy.limitBytes = -1L;
                modified = true;
            }
            if (policy.warningBytes < -1) {
                policy.warningBytes = -1L;
                modified = true;
            }
            this.mPolicies.add(policy);
        }
        if (modified) {
            writeAsync();
        }
    }

    /* JADX WARN: Type inference failed for: r1v1, types: [com.android.settingslib.NetworkPolicyEditor$1] */
    public void writeAsync() {
        final NetworkPolicy[] policies = (NetworkPolicy[]) this.mPolicies.toArray(new NetworkPolicy[this.mPolicies.size()]);
        new AsyncTask<Void, Void, Void>() { // from class: com.android.settingslib.NetworkPolicyEditor.1
            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.AsyncTask
            public Void doInBackground(Void... params) {
                NetworkPolicyEditor.this.write(policies);
                return null;
            }
        }.execute(new Void[0]);
    }

    public void write(NetworkPolicy[] policies) {
        this.mPolicyManager.setNetworkPolicies(policies);
    }

    public NetworkPolicy getOrCreatePolicy(NetworkTemplate template) {
        NetworkPolicy policy = getPolicy(template);
        if (policy == null) {
            NetworkPolicy policy2 = buildDefaultPolicy(template);
            this.mPolicies.add(policy2);
            return policy2;
        }
        return policy;
    }

    public NetworkPolicy getPolicy(NetworkTemplate template) {
        for (NetworkPolicy policy : this.mPolicies) {
            if (policy.template.equals(template)) {
                return policy;
            }
        }
        return null;
    }

    public NetworkPolicy getPolicyMaybeUnquoted(NetworkTemplate template) {
        NetworkPolicy policy = getPolicy(template);
        if (policy != null) {
            return policy;
        }
        return getPolicy(buildUnquotedNetworkTemplate(template));
    }

    @Deprecated
    private static NetworkPolicy buildDefaultPolicy(NetworkTemplate template) {
        int cycleDay;
        String cycleTimezone;
        boolean metered;
        if (template.getMatchRule() == 4) {
            cycleDay = -1;
            cycleTimezone = "UTC";
            metered = false;
        } else {
            Time time = new Time();
            time.setToNow();
            cycleDay = time.monthDay;
            cycleTimezone = time.timezone;
            metered = true;
        }
        return new NetworkPolicy(template, cycleDay, cycleTimezone, -1L, -1L, -1L, -1L, metered, true);
    }

    public int getPolicyCycleDay(NetworkTemplate template) {
        NetworkPolicy policy = getPolicy(template);
        if (policy != null) {
            return policy.cycleDay;
        }
        return -1;
    }

    public void setPolicyCycleDay(NetworkTemplate template, int cycleDay, String cycleTimezone) {
        NetworkPolicy policy = getOrCreatePolicy(template);
        policy.cycleDay = cycleDay;
        policy.cycleTimezone = cycleTimezone;
        policy.inferred = false;
        policy.clearSnooze();
        writeAsync();
    }

    public long getPolicyWarningBytes(NetworkTemplate template) {
        NetworkPolicy policy = getPolicy(template);
        if (policy != null) {
            return policy.warningBytes;
        }
        return -1L;
    }

    public void setPolicyWarningBytes(NetworkTemplate template, long warningBytes) {
        NetworkPolicy policy = getOrCreatePolicy(template);
        policy.warningBytes = warningBytes;
        policy.inferred = false;
        policy.clearSnooze();
        writeAsync();
    }

    public long getPolicyLimitBytes(NetworkTemplate template) {
        NetworkPolicy policy = getPolicy(template);
        if (policy != null) {
            return policy.limitBytes;
        }
        return -1L;
    }

    public void setPolicyLimitBytes(NetworkTemplate template, long limitBytes) {
        NetworkPolicy policy = getOrCreatePolicy(template);
        policy.limitBytes = limitBytes;
        policy.inferred = false;
        policy.clearSnooze();
        writeAsync();
    }

    public void setPolicyMetered(NetworkTemplate template, boolean metered) {
        boolean modified = false;
        NetworkPolicy policy = getPolicy(template);
        if (metered) {
            if (policy == null) {
                NetworkPolicy policy2 = buildDefaultPolicy(template);
                policy2.metered = true;
                policy2.inferred = false;
                this.mPolicies.add(policy2);
                modified = true;
            } else if (!policy.metered) {
                policy.metered = true;
                policy.inferred = false;
                modified = true;
            }
        } else if (policy != null && policy.metered) {
            policy.metered = false;
            policy.inferred = false;
            modified = true;
        }
        NetworkTemplate unquoted = buildUnquotedNetworkTemplate(template);
        NetworkPolicy unquotedPolicy = getPolicy(unquoted);
        if (unquotedPolicy != null) {
            this.mPolicies.remove(unquotedPolicy);
            modified = true;
        }
        if (modified) {
            writeAsync();
        }
    }

    private static NetworkTemplate buildUnquotedNetworkTemplate(NetworkTemplate template) {
        if (template == null) {
            return null;
        }
        String networkId = template.getNetworkId();
        String strippedNetworkId = WifiInfo.removeDoubleQuotes(networkId);
        if (TextUtils.equals(strippedNetworkId, networkId)) {
            return null;
        }
        return new NetworkTemplate(template.getMatchRule(), template.getSubscriberId(), strippedNetworkId);
    }
}
