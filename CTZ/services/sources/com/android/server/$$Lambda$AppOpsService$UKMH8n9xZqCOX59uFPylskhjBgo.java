package com.android.server;

import com.android.internal.util.function.TriConsumer;

public final class $$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo implements TriConsumer {
    public static final $$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo INSTANCE = new $$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo();

    private $$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((AppOpsService) obj).notifyWatchersOfChange(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
    }
}
