package com.android.settingslib;

import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.AsyncTask;
import android.util.RecurrenceRule;
import com.android.internal.util.Preconditions;
import com.google.android.collect.Lists;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class NetworkPolicyEditor {
    private ArrayList<NetworkPolicy> mPolicies = Lists.newArrayList();
    private NetworkPolicyManager mPolicyManager;

    public NetworkPolicyEditor(NetworkPolicyManager networkPolicyManager) {
        this.mPolicyManager = (NetworkPolicyManager) Preconditions.checkNotNull(networkPolicyManager);
    }

    public void read() {
        NetworkPolicy[] networkPolicies = this.mPolicyManager.getNetworkPolicies();
        this.mPolicies.clear();
        boolean z = false;
        for (NetworkPolicy networkPolicy : networkPolicies) {
            if (networkPolicy.limitBytes < -1) {
                networkPolicy.limitBytes = -1L;
                z = true;
            }
            if (networkPolicy.warningBytes < -1) {
                networkPolicy.warningBytes = -1L;
                z = true;
            }
            this.mPolicies.add(networkPolicy);
        }
        if (z) {
            writeAsync();
        }
    }

    /* JADX WARN: Type inference failed for: r1v3, types: [com.android.settingslib.NetworkPolicyEditor$1] */
    public void writeAsync() {
        final NetworkPolicy[] networkPolicyArr = (NetworkPolicy[]) this.mPolicies.toArray(new NetworkPolicy[this.mPolicies.size()]);
        new AsyncTask<Void, Void, Void>() { // from class: com.android.settingslib.NetworkPolicyEditor.1
            /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
            @Override // android.os.AsyncTask
            protected Void doInBackground(Void... voidArr) {
                NetworkPolicyEditor.this.write(networkPolicyArr);
                return null;
            }
        }.execute(new Void[0]);
    }

    public void write(NetworkPolicy[] networkPolicyArr) {
        this.mPolicyManager.setNetworkPolicies(networkPolicyArr);
    }

    public NetworkPolicy getOrCreatePolicy(NetworkTemplate networkTemplate) {
        NetworkPolicy policy = getPolicy(networkTemplate);
        if (policy == null) {
            NetworkPolicy networkPolicyBuildDefaultPolicy = buildDefaultPolicy(networkTemplate);
            this.mPolicies.add(networkPolicyBuildDefaultPolicy);
            return networkPolicyBuildDefaultPolicy;
        }
        return policy;
    }

    public NetworkPolicy getPolicy(NetworkTemplate networkTemplate) {
        Iterator<NetworkPolicy> it = this.mPolicies.iterator();
        while (it.hasNext()) {
            NetworkPolicy next = it.next();
            if (next.template.equals(networkTemplate)) {
                return next;
            }
        }
        return null;
    }

    @Deprecated
    private static NetworkPolicy buildDefaultPolicy(NetworkTemplate networkTemplate) {
        RecurrenceRule recurrenceRuleBuildRecurringMonthly;
        boolean z;
        if (networkTemplate.getMatchRule() == 4) {
            recurrenceRuleBuildRecurringMonthly = RecurrenceRule.buildNever();
            z = false;
        } else {
            recurrenceRuleBuildRecurringMonthly = RecurrenceRule.buildRecurringMonthly(ZonedDateTime.now().getDayOfMonth(), ZoneId.systemDefault());
            z = true;
        }
        return new NetworkPolicy(networkTemplate, recurrenceRuleBuildRecurringMonthly, -1L, -1L, -1L, -1L, z, true);
    }

    @Deprecated
    public int getPolicyCycleDay(NetworkTemplate networkTemplate) {
        NetworkPolicy policy = getPolicy(networkTemplate);
        if (policy != null && policy.cycleRule.isMonthly()) {
            return policy.cycleRule.start.getDayOfMonth();
        }
        return -1;
    }

    @Deprecated
    public void setPolicyCycleDay(NetworkTemplate networkTemplate, int i, String str) {
        NetworkPolicy orCreatePolicy = getOrCreatePolicy(networkTemplate);
        orCreatePolicy.cycleRule = NetworkPolicy.buildRule(i, ZoneId.of(str));
        orCreatePolicy.inferred = false;
        orCreatePolicy.clearSnooze();
        writeAsync();
    }

    public long getPolicyWarningBytes(NetworkTemplate networkTemplate) {
        NetworkPolicy policy = getPolicy(networkTemplate);
        if (policy != null) {
            return policy.warningBytes;
        }
        return -1L;
    }

    private void setPolicyWarningBytesInner(NetworkTemplate networkTemplate, long j) {
        NetworkPolicy orCreatePolicy = getOrCreatePolicy(networkTemplate);
        orCreatePolicy.warningBytes = j;
        orCreatePolicy.inferred = false;
        orCreatePolicy.clearSnooze();
        writeAsync();
    }

    public void setPolicyWarningBytes(NetworkTemplate networkTemplate, long j) {
        long policyLimitBytes = getPolicyLimitBytes(networkTemplate);
        if (policyLimitBytes != -1) {
            j = Math.min(j, policyLimitBytes);
        }
        setPolicyWarningBytesInner(networkTemplate, j);
    }

    public long getPolicyLimitBytes(NetworkTemplate networkTemplate) {
        NetworkPolicy policy = getPolicy(networkTemplate);
        if (policy != null) {
            return policy.limitBytes;
        }
        return -1L;
    }

    public void setPolicyLimitBytes(NetworkTemplate networkTemplate, long j) {
        if (getPolicyWarningBytes(networkTemplate) > j && j != -1) {
            setPolicyWarningBytesInner(networkTemplate, j);
        }
        NetworkPolicy orCreatePolicy = getOrCreatePolicy(networkTemplate);
        orCreatePolicy.limitBytes = j;
        orCreatePolicy.inferred = false;
        orCreatePolicy.clearSnooze();
        writeAsync();
    }
}
