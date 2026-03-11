package com.android.systemui.car;

import android.content.Context;
import android.util.ArrayMap;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.statusbar.NotificationEntryManager;
import com.android.systemui.statusbar.car.CarFacetButtonController;
import com.android.systemui.statusbar.car.CarStatusBarKeyguardViewManager;
import com.android.systemui.statusbar.car.hvac.HvacController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;

public class CarSystemUIFactory extends SystemUIFactory {
    @Override
    public StatusBarKeyguardViewManager createStatusBarKeyguardViewManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        return new CarStatusBarKeyguardViewManager(context, viewMediatorCallback, lockPatternUtils);
    }

    @Override
    public void injectDependencies(ArrayMap<Object, Dependency.DependencyProvider> arrayMap, final Context context) {
        super.injectDependencies(arrayMap, context);
        arrayMap.put(NotificationEntryManager.class, new Dependency.DependencyProvider() {
            @Override
            public final Object createDependency() {
                return CarSystemUIFactory.lambda$injectDependencies$0(context);
            }
        });
        arrayMap.put(CarFacetButtonController.class, new Dependency.DependencyProvider() {
            @Override
            public final Object createDependency() {
                return CarSystemUIFactory.lambda$injectDependencies$1(context);
            }
        });
        arrayMap.put(HvacController.class, new Dependency.DependencyProvider() {
            @Override
            public final Object createDependency() {
                return CarSystemUIFactory.lambda$injectDependencies$2(context);
            }
        });
    }

    static Object lambda$injectDependencies$0(Context context) {
        return new CarNotificationEntryManager(context);
    }

    static Object lambda$injectDependencies$1(Context context) {
        return new CarFacetButtonController(context);
    }

    static Object lambda$injectDependencies$2(Context context) {
        return new HvacController(context);
    }
}
