package com.android.server.am;

public final class $$Lambda$ejtzn5TCL2GSsOkwaLFeot_Ozqg implements Runnable {
    private final ActivityManagerService f$0;

    public $$Lambda$ejtzn5TCL2GSsOkwaLFeot_Ozqg(ActivityManagerService activityManagerService) {
        this.f$0 = activityManagerService;
    }

    @Override
    public final void run() {
        this.f$0.updateOomAdj();
    }
}
