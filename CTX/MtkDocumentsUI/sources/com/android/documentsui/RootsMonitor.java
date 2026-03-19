package com.android.documentsui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import com.android.documentsui.AbstractActionHandler;
import com.android.documentsui.AbstractActionHandler.CommonAddons;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.PairedTask;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.State;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.roots.ProvidersAccess;
import java.util.Iterator;

final class RootsMonitor<T extends Activity & AbstractActionHandler.CommonAddons> {
    private final LocalBroadcastManager mManager;
    private final BroadcastReceiver mReceiver;

    RootsMonitor(final T t, final ActionHandler actionHandler, final ProvidersAccess providersAccess, final DocumentsAccess documentsAccess, final State state, final SearchViewManager searchViewManager, final Runnable runnable) {
        this.mManager = LocalBroadcastManager.getInstance(t);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new HandleRootsChangedTask(t, actionHandler, providersAccess, documentsAccess, state, searchViewManager, runnable).execute(new RootInfo[]{((AbstractActionHandler.CommonAddons) t).getCurrentRoot()});
            }
        };
    }

    void start() {
        this.mManager.registerReceiver(this.mReceiver, new IntentFilter("com.android.documentsui.action.ROOT_CHANGED"));
    }

    void stop() {
        this.mManager.unregisterReceiver(this.mReceiver);
    }

    private static class HandleRootsChangedTask<T extends Activity & AbstractActionHandler.CommonAddons> extends PairedTask<T, RootInfo, RootInfo> {
        static final boolean $assertionsDisabled = false;
        private final Runnable mActionModeFinisher;
        private final ActionHandler mActions;
        private RootInfo mCurrentRoot;
        private DocumentInfo mDefaultRootDocument;
        private final DocumentsAccess mDocs;
        private final ProvidersAccess mProviders;
        private final SearchViewManager mSearchMgr;
        private final State mState;

        private HandleRootsChangedTask(T t, ActionHandler actionHandler, ProvidersAccess providersAccess, DocumentsAccess documentsAccess, State state, SearchViewManager searchViewManager, Runnable runnable) {
            super(t);
            this.mActions = actionHandler;
            this.mProviders = providersAccess;
            this.mDocs = documentsAccess;
            this.mState = state;
            this.mSearchMgr = searchViewManager;
            this.mActionModeFinisher = runnable;
        }

        @Override
        protected RootInfo run(RootInfo... rootInfoArr) {
            this.mCurrentRoot = rootInfoArr[0];
            Iterator<RootInfo> it = this.mProviders.getRootsBlocking().iterator();
            while (it.hasNext()) {
                if (it.next().getUri().equals(this.mCurrentRoot.getUri())) {
                    return null;
                }
            }
            RootInfo defaultRootBlocking = this.mProviders.getDefaultRootBlocking(this.mState);
            if (!defaultRootBlocking.isRecents()) {
                this.mDefaultRootDocument = this.mDocs.getRootDocument(defaultRootBlocking);
            }
            return defaultRootBlocking;
        }

        @Override
        protected void finish(RootInfo rootInfo) {
            if (rootInfo == null) {
                return;
            }
            Uri data = this.mOwner.getIntent().getData();
            if (data != null && data.equals(this.mCurrentRoot.getUri())) {
                this.mOwner.finish();
                return;
            }
            this.mActionModeFinisher.run();
            this.mState.stack.changeRoot(rootInfo);
            this.mSearchMgr.update(this.mState.stack);
            if (rootInfo.isRecents()) {
                ((AbstractActionHandler.CommonAddons) this.mOwner).refreshCurrentRootAndDirectory(1);
            } else {
                this.mActions.openContainerDocument(this.mDefaultRootDocument);
            }
        }
    }
}
