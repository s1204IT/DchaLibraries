package com.android.documentsui.picker;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.AbstractActionHandler;
import com.android.documentsui.ActivityConfig;
import com.android.documentsui.DocumentsAccess;
import com.android.documentsui.Injector;
import com.android.documentsui.Metrics;
import com.android.documentsui.Model;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.picker.ActionHandler.Addons;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.roots.ProvidersAccess;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

class ActionHandler<T extends Activity & Addons> extends AbstractActionHandler<T> {
    static final boolean $assertionsDisabled = false;
    private final ActivityConfig mConfig;
    private final Features mFeatures;
    private final LastAccessedStorage mLastAccessed;
    private final Model mModel;

    public interface Addons extends AbstractActionHandler.CommonAddons {
        @Override
        void onDocumentPicked(DocumentInfo documentInfo);

        @VisibleForTesting
        void setResult(int i, Intent intent, int i2);
    }

    ActionHandler(T t, State state, ProvidersAccess providersAccess, DocumentsAccess documentsAccess, SearchViewManager searchViewManager, Lookup<String, Executor> lookup, Injector injector, LastAccessedStorage lastAccessedStorage) {
        super(t, state, providersAccess, documentsAccess, searchViewManager, lookup, injector);
        this.mConfig = injector.config;
        this.mFeatures = injector.features;
        this.mModel = injector.getModel();
        this.mLastAccessed = lastAccessedStorage;
    }

    public void initLocation(Intent intent) {
        if (this.mState.stack.isInitialized()) {
            if (SharedMinimal.DEBUG) {
                Log.d("PickerActionHandler", "Stack already resolved for uri: " + intent.getData());
            }
            restoreRootAndDirectory();
            return;
        }
        this.mActivity.setTitle("");
        if (launchHomeForCopyDestination(intent)) {
            if (SharedMinimal.DEBUG) {
                Log.d("PickerActionHandler", "Launching directly into Home directory for copy destination.");
            }
        } else if (this.mFeatures.isLaunchToDocumentEnabled() && launchToDocument(intent)) {
            if (SharedMinimal.DEBUG) {
                Log.d("PickerActionHandler", "Launched to a document.");
            }
        } else {
            if (SharedMinimal.DEBUG) {
                Log.d("PickerActionHandler", "Load last accessed stack.");
            }
            loadLastAccessedStack();
        }
    }

    @Override
    protected void launchToDefaultLocation() {
        loadLastAccessedStack();
    }

    private boolean launchHomeForCopyDestination(Intent intent) {
        if ("com.android.documentsui.PICK_COPY_DESTINATION".equals(intent.getAction())) {
            loadHomeDir();
            return true;
        }
        return false;
    }

    private boolean launchToDocument(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("android.provider.extra.INITIAL_URI");
        if (uri != null) {
            return launchToDocument(uri);
        }
        return false;
    }

    private void loadLastAccessedStack() {
        if (SharedMinimal.DEBUG) {
            Log.d("PickerActionHandler", "Attempting to load last used stack for calling package.");
        }
        new LoadLastAccessedStackTask(this.mActivity, this.mLastAccessed, this.mState, this.mProviders, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.onLastAccessedStackLoaded((DocumentStack) obj);
            }
        }).execute(new Void[0]);
    }

    private void onLastAccessedStackLoaded(DocumentStack documentStack) {
        if (documentStack == null) {
            loadDefaultLocation();
        } else {
            this.mState.stack.reset(documentStack);
            this.mActivity.refreshCurrentRootAndDirectory(1);
        }
    }

    private void loadDefaultLocation() {
        switch (this.mState.action) {
            case 3:
            case 5:
            case 6:
                this.mState.stack.changeRoot(this.mProviders.getRecentsRoot());
                this.mActivity.refreshCurrentRootAndDirectory(1);
                return;
            case 4:
                loadHomeDir();
                return;
            default:
                throw new UnsupportedOperationException("Unexpected action type: " + this.mState.action);
        }
    }

    @Override
    public void showAppDetails(ResolveInfo resolveInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", resolveInfo.activityInfo.packageName, null));
        intent.addFlags(524288);
        this.mActivity.startActivity(intent);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (SharedMinimal.DEBUG) {
            Log.d("PickerActionHandler", "onActivityResult() code=" + i2);
        }
        if (i == 42) {
            onExternalAppResult(i2, intent);
        } else {
            super.onActivityResult(i, i2, intent);
        }
    }

    private void onExternalAppResult(int i, Intent intent) {
        if (i != 0) {
            this.mLastAccessed.setLastAccessedToExternalApp(this.mActivity);
            ((Addons) this.mActivity).setResult(i, intent, 0);
            this.mActivity.finish();
        }
    }

    @Override
    public void openInNewWindow(DocumentStack documentStack) {
        throw new UnsupportedOperationException("Can't open in new window");
    }

    @Override
    public void openRoot(RootInfo rootInfo) {
        Metrics.logRootVisited(this.mActivity, 2, rootInfo);
        this.mActivity.onRootPicked(rootInfo);
    }

    @Override
    public void openRoot(ResolveInfo resolveInfo) {
        Metrics.logAppVisited(this.mActivity, resolveInfo);
        Intent intent = new Intent(this.mActivity.getIntent());
        intent.setFlags(intent.getFlags() & (-33554433));
        intent.setComponent(new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name));
        this.mActivity.startActivityForResult(intent, 42);
    }

    @Override
    public void springOpenDirectory(DocumentInfo documentInfo) {
    }

    @Override
    public boolean openItem(ItemDetailsLookup.ItemDetails itemDetails, int i, int i2) {
        DocumentInfo document = this.mModel.getDocument(itemDetails.getStableId());
        if (document == null) {
            Log.w("PickerActionHandler", "Can't view item. No Document available for modeId: " + itemDetails.getStableId());
            return false;
        }
        if (!this.mConfig.isDocumentEnabled(document.mimeType, document.flags, this.mState)) {
            return false;
        }
        ((Addons) this.mActivity).onDocumentPicked(document);
        this.mSelectionMgr.clearSelection();
        return true;
    }

    void pickDocument(DocumentInfo documentInfo) {
        Uri uriBuildTreeDocumentUri;
        int i = this.mState.action;
        if (i == 2) {
            uriBuildTreeDocumentUri = documentInfo.derivedUri;
        } else if (i == 6) {
            uriBuildTreeDocumentUri = DocumentsContract.buildTreeDocumentUri(documentInfo.authority, documentInfo.documentId);
        } else {
            throw new IllegalStateException("Invalid mState.action");
        }
        finishPicking(uriBuildTreeDocumentUri);
    }

    void saveDocument(String str, String str2, BooleanConsumer booleanConsumer) {
        new CreatePickedDocumentTask(this.mActivity, this.mDocs, this.mLastAccessed, this.mState.stack, str, str2, booleanConsumer, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.onPickFinished((Uri) obj);
            }
        }).executeOnExecutor(getExecutorForCurrentDirectory(), new Void[0]);
    }

    void saveDocument(FragmentManager fragmentManager, DocumentInfo documentInfo) {
        if (this.mFeatures.isOverwriteConfirmationEnabled()) {
            this.mInjector.dialogs.confirmOverwrite(fragmentManager, documentInfo);
        } else {
            finishPicking(documentInfo.derivedUri);
        }
    }

    void finishPicking(final Uri... uriArr) {
        new SetLastAccessedStackTask(this.mActivity, this.mLastAccessed, this.mState.stack, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onPickFinished(uriArr);
            }
        }).executeOnExecutor(getExecutorForCurrentDirectory(), new Void[0]);
    }

    private void onPickFinished(Uri... uriArr) {
        if (SharedMinimal.DEBUG) {
            Log.d("PickerActionHandler", "onFinished() " + Arrays.toString(uriArr));
        }
        Intent intent = new Intent();
        if (uriArr.length == 1) {
            intent.setData(uriArr[0]);
        } else if (uriArr.length > 1) {
            ClipData clipData = new ClipData(null, this.mState.acceptMimes, new ClipData.Item(uriArr[0]));
            for (int i = 1; i < uriArr.length; i++) {
                clipData.addItem(new ClipData.Item(uriArr[i]));
            }
            intent.setClipData(clipData);
        }
        if (this.mState.action == 5) {
            intent.addFlags(1);
        } else if (this.mState.action == 6) {
            intent.addFlags(195);
        } else if (this.mState.action == 2) {
            intent.putExtra("com.android.documentsui.STACK", this.mState.stack);
            intent.putExtra("com.android.documentsui.OPERATION_TYPE", this.mState.copyOperationSubType);
        } else {
            intent.addFlags(67);
        }
        ((Addons) this.mActivity).setResult(-1, intent, 0);
        this.mActivity.finish();
    }

    private Executor getExecutorForCurrentDirectory() {
        DocumentInfo documentInfoPeek = this.mState.stack.peek();
        if (documentInfoPeek != null && documentInfoPeek.authority != null) {
            return this.mExecutors.lookup(documentInfoPeek.authority);
        }
        return AsyncTask.THREAD_POOL_EXECUTOR;
    }
}
