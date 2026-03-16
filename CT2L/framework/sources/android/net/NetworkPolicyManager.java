package android.net;

import android.content.Context;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.format.Time;
import java.io.PrintWriter;

public class NetworkPolicyManager {
    private static final boolean ALLOW_PLATFORM_APP_POLICY = true;
    public static final String EXTRA_NETWORK_TEMPLATE = "android.net.NETWORK_TEMPLATE";
    public static final int POLICY_ALLOW_BACKGROUND_BATTERY_SAVE = 2;
    public static final int POLICY_NONE = 0;
    public static final int POLICY_REJECT_METERED_BACKGROUND = 1;
    public static final int RULE_ALLOW_ALL = 0;
    public static final int RULE_REJECT_METERED = 1;
    private INetworkPolicyManager mService;

    public NetworkPolicyManager(INetworkPolicyManager service) {
        if (service == null) {
            throw new IllegalArgumentException("missing INetworkPolicyManager");
        }
        this.mService = service;
    }

    public static NetworkPolicyManager from(Context context) {
        return (NetworkPolicyManager) context.getSystemService(Context.NETWORK_POLICY_SERVICE);
    }

    public void setUidPolicy(int uid, int policy) {
        try {
            this.mService.setUidPolicy(uid, policy);
        } catch (RemoteException e) {
        }
    }

    public void addUidPolicy(int uid, int policy) {
        try {
            this.mService.addUidPolicy(uid, policy);
        } catch (RemoteException e) {
        }
    }

    public void removeUidPolicy(int uid, int policy) {
        try {
            this.mService.removeUidPolicy(uid, policy);
        } catch (RemoteException e) {
        }
    }

    public int getUidPolicy(int uid) {
        try {
            return this.mService.getUidPolicy(uid);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int[] getUidsWithPolicy(int policy) {
        try {
            return this.mService.getUidsWithPolicy(policy);
        } catch (RemoteException e) {
            return new int[0];
        }
    }

    public int[] getPowerSaveAppIdWhitelist() {
        try {
            return this.mService.getPowerSaveAppIdWhitelist();
        } catch (RemoteException e) {
            return new int[0];
        }
    }

    public void registerListener(INetworkPolicyListener listener) {
        try {
            this.mService.registerListener(listener);
        } catch (RemoteException e) {
        }
    }

    public void unregisterListener(INetworkPolicyListener listener) {
        try {
            this.mService.unregisterListener(listener);
        } catch (RemoteException e) {
        }
    }

    public void setNetworkPolicies(NetworkPolicy[] policies) {
        try {
            this.mService.setNetworkPolicies(policies);
        } catch (RemoteException e) {
        }
    }

    public NetworkPolicy[] getNetworkPolicies() {
        try {
            return this.mService.getNetworkPolicies();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        try {
            this.mService.setRestrictBackground(restrictBackground);
        } catch (RemoteException e) {
        }
    }

    public boolean getRestrictBackground() {
        try {
            return this.mService.getRestrictBackground();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static long computeLastCycleBoundary(long currentTime, NetworkPolicy policy) {
        if (policy.cycleDay == -1) {
            throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
        }
        Time now = new Time(policy.cycleTimezone);
        now.set(currentTime);
        Time cycle = new Time(now);
        cycle.second = 0;
        cycle.minute = 0;
        cycle.hour = 0;
        snapToCycleDay(cycle, policy.cycleDay);
        if (Time.compare(cycle, now) >= 0) {
            Time lastMonth = new Time(now);
            lastMonth.second = 0;
            lastMonth.minute = 0;
            lastMonth.hour = 0;
            lastMonth.monthDay = 1;
            lastMonth.month--;
            lastMonth.normalize(true);
            cycle.set(lastMonth);
            snapToCycleDay(cycle, policy.cycleDay);
        }
        return cycle.toMillis(true);
    }

    public static long computeNextCycleBoundary(long currentTime, NetworkPolicy policy) {
        if (policy.cycleDay == -1) {
            throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
        }
        Time now = new Time(policy.cycleTimezone);
        now.set(currentTime);
        Time cycle = new Time(now);
        cycle.second = 0;
        cycle.minute = 0;
        cycle.hour = 0;
        snapToCycleDay(cycle, policy.cycleDay);
        if (Time.compare(cycle, now) <= 0) {
            Time nextMonth = new Time(now);
            nextMonth.second = 0;
            nextMonth.minute = 0;
            nextMonth.hour = 0;
            nextMonth.monthDay = 1;
            nextMonth.month++;
            nextMonth.normalize(true);
            cycle.set(nextMonth);
            snapToCycleDay(cycle, policy.cycleDay);
        }
        return cycle.toMillis(true);
    }

    public static void snapToCycleDay(Time time, int cycleDay) {
        if (cycleDay > time.getActualMaximum(4)) {
            time.month++;
            time.monthDay = 1;
            time.second = -1;
        } else {
            time.monthDay = cycleDay;
        }
        time.normalize(true);
    }

    @Deprecated
    public static boolean isUidValidForPolicy(Context context, int uid) {
        return UserHandle.isApp(uid);
    }

    public static void dumpPolicy(PrintWriter fout, int policy) {
        fout.write("[");
        if ((policy & 1) != 0) {
            fout.write("REJECT_METERED_BACKGROUND");
        }
        fout.write("]");
    }

    public static void dumpRules(PrintWriter fout, int rules) {
        fout.write("[");
        if ((rules & 1) != 0) {
            fout.write("REJECT_METERED");
        }
        fout.write("]");
    }
}
