package com.mediatek.server;

import android.app.IAlarmListener;
import android.app.PendingIntent;
import com.android.server.AlarmManagerService;
import java.util.function.Predicate;

/* compiled from: lambda */
/* renamed from: com.mediatek.server.-$$Lambda$MtkAlarmManagerService$txYuroEYSu-sMcspIcw3eBOuLvk */
/* loaded from: classes.dex */
public final /* synthetic */ class lambda3 implements Predicate {
    private final /* synthetic */ PendingIntent f$0;
    private final /* synthetic */ IAlarmListener f$1;

    public /* synthetic */ lambda3(PendingIntent pendingIntent, IAlarmListener iAlarmListener) {
        pendingIntent = pendingIntent;
        iAlarmListener = iAlarmListener;
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return ((AlarmManagerService.Alarm) obj).matches(pendingIntent, iAlarmListener);
    }
}