package com.android.systemui.globalactions;

import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.SystemUI;
import com.android.systemui.plugins.GlobalActions;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.ExtensionController;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GlobalActionsComponent extends SystemUI implements GlobalActions.GlobalActionsManager, CommandQueue.Callbacks {
    private IStatusBarService mBarService;
    private ExtensionController.Extension<GlobalActions> mExtension;
    private GlobalActions mPlugin;

    @Override
    public void start() {
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(GlobalActions.class).withPlugin(GlobalActions.class).withDefault(new Supplier() {
            @Override
            public final Object get() {
                return GlobalActionsComponent.lambda$start$0(this.f$0);
            }
        }).withCallback(new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.onExtensionCallback((GlobalActions) obj);
            }
        }).build();
        this.mPlugin = this.mExtension.get();
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).addCallbacks(this);
    }

    public static GlobalActions lambda$start$0(GlobalActionsComponent globalActionsComponent) {
        return new GlobalActionsImpl(globalActionsComponent.mContext);
    }

    public void onExtensionCallback(GlobalActions globalActions) {
        if (this.mPlugin != null) {
            this.mPlugin.destroy();
        }
        this.mPlugin = globalActions;
    }

    @Override
    public void handleShowShutdownUi(boolean z, String str) {
        this.mExtension.get().showShutdownUi(z, str);
    }

    @Override
    public void handleShowGlobalActionsMenu() {
        this.mExtension.get().showGlobalActions(this);
    }

    @Override
    public void onGlobalActionsShown() {
        try {
            this.mBarService.onGlobalActionsShown();
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onGlobalActionsHidden() {
        try {
            this.mBarService.onGlobalActionsHidden();
        } catch (RemoteException e) {
        }
    }

    @Override
    public void shutdown() {
        try {
            this.mBarService.shutdown();
        } catch (RemoteException e) {
        }
    }

    @Override
    public void reboot(boolean z) {
        try {
            this.mBarService.reboot(z);
        } catch (RemoteException e) {
        }
    }
}
