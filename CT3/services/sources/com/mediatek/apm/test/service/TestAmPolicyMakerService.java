package com.mediatek.apm.test.service;

import android.content.Context;
import com.android.server.SystemService;
import com.mediatek.apm.frc.FocusRelationshipChainPolicy;
import com.mediatek.apm.suppression.SuppressionPolicy;
import com.mediatek.apm.test.service.ITestAmPolicyMakerService;
import java.util.List;

public class TestAmPolicyMakerService extends ITestAmPolicyMakerService.Stub {
    private final String TAG = "TestAmPolicyMakerService";
    private FocusRelationshipChainPolicy B = FocusRelationshipChainPolicy.getInstance();
    private SuppressionPolicy C = SuppressionPolicy.getInstance();

    public static final class LifeCycle extends SystemService {
        private final TestAmPolicyMakerService D;

        public LifeCycle(Context context) {
            super(context);
            this.D = new TestAmPolicyMakerService();
        }

        @Override
        public void onStart() {
            publishBinderService("TestAmPolicyMakerService", this.D);
        }
    }

    @Override
    public void startFrc(String str, int i, List<String> list) {
        this.B.startFrc(str, i, list);
    }

    @Override
    public void stopFrc(String str) {
        this.B.stopFrc(str);
    }

    @Override
    public List<String> getFrcPackageList(String str) {
        return this.B.getFrcPackageList(str);
    }

    @Override
    public void updateFrcExtraAllowList(String str, List<String> list) {
        this.B.updateFrcExtraAllowList(str, list);
    }

    @Override
    public void startSuppression(String str, int i, int i2, String str2, List<String> list) {
        this.C.startSuppression(str, i, i2, str2, list);
    }

    @Override
    public void stopSuppression(String str) {
        this.C.stopSuppression(str);
    }

    @Override
    public void updateSuppressionExtraAllowList(String str, List<String> list) {
        this.C.updateExtraAllowList(str, list);
    }

    @Override
    public List<String> getSuppressionList() {
        return this.C.getSuppressionList();
    }

    @Override
    public boolean isPackageInSuppression(String str, String str2, int i) {
        return this.C.isPackageInSuppression(str, str2, i);
    }
}
