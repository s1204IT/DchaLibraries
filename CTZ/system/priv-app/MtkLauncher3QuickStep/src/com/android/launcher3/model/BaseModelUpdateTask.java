package com.android.launcher3.model;

import android.os.UserHandle;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.widget.WidgetListRowEntry;
import java.util.ArrayList;
import java.util.concurrent.Executor;

/* loaded from: classes.dex */
public abstract class BaseModelUpdateTask implements LauncherModel.ModelUpdateTask {
    private static final boolean DEBUG_TASKS = false;
    private static final String TAG = "BaseModelUpdateTask";
    private AllAppsList mAllAppsList;
    private LauncherAppState mApp;
    private BgDataModel mDataModel;
    private LauncherModel mModel;
    private Executor mUiExecutor;

    public abstract void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList);

    @Override // com.android.launcher3.LauncherModel.ModelUpdateTask
    public void init(LauncherAppState launcherAppState, LauncherModel launcherModel, BgDataModel bgDataModel, AllAppsList allAppsList, Executor executor) {
        this.mApp = launcherAppState;
        this.mModel = launcherModel;
        this.mDataModel = bgDataModel;
        this.mAllAppsList = allAppsList;
        this.mUiExecutor = executor;
    }

    @Override // java.lang.Runnable
    public final void run() {
        if (!this.mModel.isModelLoaded()) {
            return;
        }
        execute(this.mApp, this.mDataModel, this.mAllAppsList);
    }

    public final void scheduleCallbackTask(final LauncherModel.CallbackTask callbackTask) {
        final LauncherModel.Callbacks callback = this.mModel.getCallback();
        this.mUiExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.-$$Lambda$BaseModelUpdateTask$wC_xrSA2wDogNJ7aI_9kn7ZSyLg
            @Override // java.lang.Runnable
            public final void run() {
                BaseModelUpdateTask.lambda$scheduleCallbackTask$0(this.f$0, callback, callbackTask);
            }
        });
    }

    public static /* synthetic */ void lambda$scheduleCallbackTask$0(BaseModelUpdateTask baseModelUpdateTask, LauncherModel.Callbacks callbacks, LauncherModel.CallbackTask callbackTask) {
        LauncherModel.Callbacks callback = baseModelUpdateTask.mModel.getCallback();
        if (callbacks == callback && callback != null) {
            callbackTask.execute(callbacks);
        }
    }

    public ModelWriter getModelWriter() {
        return this.mModel.getWriter(false, false);
    }

    public void bindUpdatedShortcuts(final ArrayList<ShortcutInfo> arrayList, final UserHandle userHandle) {
        if (!arrayList.isEmpty()) {
            scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.BaseModelUpdateTask.1
                @Override // com.android.launcher3.LauncherModel.CallbackTask
                public void execute(LauncherModel.Callbacks callbacks) {
                    callbacks.bindShortcutsChanged(arrayList, userHandle);
                }
            });
        }
    }

    public void bindDeepShortcuts(BgDataModel bgDataModel) {
        final MultiHashMap<ComponentKey, String> multiHashMapClone = bgDataModel.deepShortcutMap.clone();
        scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.BaseModelUpdateTask.2
            @Override // com.android.launcher3.LauncherModel.CallbackTask
            public void execute(LauncherModel.Callbacks callbacks) {
                callbacks.bindDeepShortcutMap(multiHashMapClone);
            }
        });
    }

    public void bindUpdatedWidgets(BgDataModel bgDataModel) {
        final ArrayList<WidgetListRowEntry> widgetsList = bgDataModel.widgetsModel.getWidgetsList(this.mApp.getContext());
        scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.BaseModelUpdateTask.3
            @Override // com.android.launcher3.LauncherModel.CallbackTask
            public void execute(LauncherModel.Callbacks callbacks) {
                callbacks.bindAllWidgets(widgetsList);
            }
        });
    }

    public void deleteAndBindComponentsRemoved(final ItemInfoMatcher itemInfoMatcher) {
        getModelWriter().deleteItemsFromDatabase(itemInfoMatcher);
        scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.BaseModelUpdateTask.4
            @Override // com.android.launcher3.LauncherModel.CallbackTask
            public void execute(LauncherModel.Callbacks callbacks) {
                callbacks.bindWorkspaceComponentsRemoved(itemInfoMatcher);
            }
        });
    }
}
