package com.mediatek.apm.suppression;

import android.os.Build;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;
import com.mediatek.apm.frc.FocusRelationshipChainPolicy;
import java.util.ArrayList;
import java.util.List;

public class SuppressionPolicy {
    public static final int SUPPRESSION_POLICY_ALL = 0;
    public static final int SUPPRESSION_POLICY_EXCLUDE_EXTRA_ALLOW_LIST = 2;
    public static final int SUPPRESSION_POLICY_EXCLUDE_FOCUS_RELATIONSHIP_CHAIN = 1;
    public static final int SUPPRESSION_POLICY_EXCLUDE_SYSTEM_CALLER = 4;
    private static String TAG = "SuppressionPolicy";
    private static SuppressionPolicy s = null;
    private boolean n;
    private boolean o = "user".equals(Build.TYPE);
    private ArrayMap<String, a> t;
    private FocusRelationshipChainPolicy u;

    private SuppressionPolicy() {
        this.n = false;
        new com.mediatek.common.jpe.a().a();
        if (this.t == null) {
            this.t = new ArrayMap<>();
        }
        this.u = FocusRelationshipChainPolicy.getInstance();
        this.n = SystemProperties.get("persist.sys.apm.debug_mode").equals("1");
    }

    public static SuppressionPolicy getInstance() {
        if (s == null) {
            s = new SuppressionPolicy();
        }
        return s;
    }

    public void startSuppression(String str, int i, int i2, String str2, List<String> list) {
        synchronized (this) {
            if (str == null) {
                Slog.w(TAG, "The tag is null");
                return;
            }
            if (e(str)) {
                Slog.w(TAG, "The tag:" + str + " exist");
                return;
            }
            a aVar = new a();
            aVar.v = i;
            aVar.w = i2;
            aVar.x = str2;
            aVar.y = list;
            this.t.put(str, aVar);
            Slog.v(TAG, "startSuppression(" + str + ", " + i + ", " + str2 + ")");
        }
    }

    public void stopSuppression(String str) {
        synchronized (this) {
            if (str == null) {
                Slog.w(TAG, "The tag is null");
            } else if (!e(str)) {
                Slog.w(TAG, "The tag:" + str + " does not exist");
            } else {
                this.t.remove(str);
                Slog.v(TAG, "stopSuppression(" + str + ")");
            }
        }
    }

    public void updateExtraAllowList(String str, List<String> list) {
        synchronized (this) {
            if (str == null) {
                Slog.w(TAG, "The tag is null");
            } else if (!e(str)) {
                Slog.w(TAG, "The tag:" + str + " does not exist");
            } else {
                this.t.get(str).y = list;
            }
        }
    }

    public boolean isPackageInSuppression(String str, String str2, int i) {
        synchronized (this) {
            if (str == null) {
                Slog.w(TAG, "The tag is null");
                return false;
            }
            if (!e(str)) {
                Slog.w(TAG, "The tag:" + str + " does not exist");
                return false;
            }
            boolean z = i == 9999;
            a aVar = this.t.get(str);
            int i2 = aVar.v;
            if ((i2 & 1) != 0 && this.u.isPackageInFrc(aVar.x, str2)) {
                if ((!this.o || this.n) && !z) {
                    Slog.v(TAG, "Caller " + str2 + " is not suppressed by 1");
                }
                return false;
            }
            if ((i2 & 2) != 0 && aVar.y.contains(str2)) {
                if ((!this.o || this.n) && !z) {
                    Slog.v(TAG, "Caller " + str2 + " is not suppressed by 2");
                }
                return false;
            }
            if ((i2 & 4) != 0 && i == 1000) {
                if ((!this.o || this.n) && !z) {
                    Slog.v(TAG, "Caller " + str2 + " is not suppressed by 4");
                }
                return false;
            }
            if (this.n && !z) {
                Slog.v(TAG, "Caller " + str2 + "(" + i + ") is suppressed");
            }
            return true;
        }
    }

    public ArrayList<String> getSuppressionList() {
        synchronized (this) {
            if (this.t.size() == 0) {
                return null;
            }
            return new ArrayList<>(this.t.keySet());
        }
    }

    int d(String str) {
        synchronized (this) {
            if (str == null) {
                Slog.w(TAG, "The tag is null");
                return -1;
            }
            if (!e(str)) {
                Slog.w(TAG, "The tag:" + str + " does not exist");
                return -1;
            }
            return this.t.get(str).w;
        }
    }

    private boolean e(String str) {
        return this.t.containsKey(str);
    }

    class a {
        int v;
        int w;
        String x;
        List<String> y;

        a() {
        }
    }
}
