package com.android.documentsui.files;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.DragEvent;
import android.widget.Toast;
import com.android.documentsui.AbstractActionHandler;
import com.android.documentsui.ActionModeAddons;
import com.android.documentsui.ActivityConfig;
import com.android.documentsui.DocumentsAccess;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.DragAndDropManager;
import com.android.documentsui.Injector;
import com.android.documentsui.Metrics;
import com.android.documentsui.Model;
import com.android.documentsui.R;
import com.android.documentsui.base.ConfirmationCallback;
import com.android.documentsui.base.DebugFlags;
import com.android.documentsui.base.DocumentFilters;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.MimeTypes;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.clipping.ClipStore;
import com.android.documentsui.clipping.DocumentClipper;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.files.ActionHandler.Addons;
import com.android.documentsui.inspector.InspectorActivity;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.roots.ProvidersAccess;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MutableSelection;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.services.FileOperation;
import com.android.documentsui.services.FileOperations;
import com.android.documentsui.ui.DialogController;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class ActionHandler<T extends Activity & Addons> extends AbstractActionHandler<T> {
    static final boolean $assertionsDisabled = false;
    private final ActionModeAddons mActionModeAddons;
    private final ClipStore mClipStore;
    private final DocumentClipper mClipper;
    private final ActivityConfig mConfig;
    private final DialogController mDialogs;
    private final DragAndDropManager mDragAndDropManager;
    private final Features mFeatures;
    private final Model mModel;

    public interface Addons extends AbstractActionHandler.CommonAddons {
    }

    ActionHandler(T t, State state, ProvidersAccess providersAccess, DocumentsAccess documentsAccess, SearchViewManager searchViewManager, Lookup<String, Executor> lookup, ActionModeAddons actionModeAddons, DocumentClipper documentClipper, ClipStore clipStore, DragAndDropManager dragAndDropManager, Injector injector) {
        super(t, state, providersAccess, documentsAccess, searchViewManager, lookup, injector);
        this.mActionModeAddons = actionModeAddons;
        this.mFeatures = injector.features;
        this.mConfig = injector.config;
        this.mDialogs = injector.dialogs;
        this.mClipper = documentClipper;
        this.mClipStore = clipStore;
        this.mDragAndDropManager = dragAndDropManager;
        this.mModel = injector.getModel();
    }

    @Override
    public boolean dropOn(DragEvent dragEvent, RootInfo rootInfo) {
        if (!rootInfo.supportsCreate() || rootInfo.isLibrary()) {
            return false;
        }
        ClipData clipData = dragEvent.getClipData();
        Object localState = dragEvent.getLocalState();
        DragAndDropManager dragAndDropManager = this.mDragAndDropManager;
        DialogController dialogController = this.mDialogs;
        Objects.requireNonNull(dialogController);
        return dragAndDropManager.drop(clipData, localState, rootInfo, this, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController));
    }

    @Override
    public void openSelectedInNewWindow() {
        openInNewWindow(new DocumentStack(this.mState.stack, this.mModel.getDocument(getStableSelection().iterator().next())));
    }

    @Override
    public void openSettings(RootInfo rootInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Metrics.logUserAction(this.mActivity, 10);
        Intent intent = new Intent("android.provider.action.DOCUMENT_ROOT_SETTINGS");
        intent.setDataAndType(rootInfo.getUri(), "vnd.android.document/root");
        this.mActivity.startActivity(intent);
    }

    @Override
    public void pasteIntoFolder(final RootInfo rootInfo) {
        getRootDocument(rootInfo, -1, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.pasteIntoFolder(rootInfo, (DocumentInfo) obj);
            }
        });
    }

    private void pasteIntoFolder(RootInfo rootInfo, DocumentInfo documentInfo) {
        DocumentStack documentStack = new DocumentStack(rootInfo, documentInfo);
        DocumentClipper documentClipper = this.mClipper;
        DialogController dialogController = this.mDialogs;
        Objects.requireNonNull(dialogController);
        documentClipper.copyFromClipboard(documentInfo, documentStack, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController));
    }

    @Override
    public DocumentInfo renameDocument(String str, DocumentInfo documentInfo) throws Throwable {
        ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
        ContentResolver contentResolver = this.mActivity.getContentResolver();
        try {
            contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(contentResolver, documentInfo.derivedUri.getAuthority());
            try {
                try {
                    DocumentInfo documentInfoFromUri = DocumentInfo.fromUri(contentResolver, DocumentsContract.renameDocument(contentProviderClientAcquireUnstableProviderOrThrow, documentInfo.derivedUri, str));
                    ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                    return documentInfoFromUri;
                } catch (Exception e) {
                    e = e;
                    Log.w("ManagerActionHandler", "Failed to rename file", e);
                    ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            contentProviderClientAcquireUnstableProviderOrThrow = null;
        } catch (Throwable th2) {
            th = th2;
            contentProviderClientAcquireUnstableProviderOrThrow = null;
            ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
            throw th;
        }
    }

    @Override
    public void openRoot(RootInfo rootInfo) {
        Metrics.logRootVisited(this.mActivity, 1, rootInfo);
        this.mActivity.onRootPicked(rootInfo);
    }

    @Override
    public boolean openItem(ItemDetailsLookup.ItemDetails itemDetails, int i, int i2) {
        DocumentInfo document = this.mModel.getDocument(itemDetails.getStableId());
        if (document == null) {
            Log.w("ManagerActionHandler", "Can't view item. No Document available for modeId: " + itemDetails.getStableId());
            return false;
        }
        return openDocument(document, i, i2);
    }

    @VisibleForTesting
    public boolean openDocument(DocumentInfo documentInfo, int i, int i2) {
        if (this.mConfig.isDocumentEnabled(documentInfo.mimeType, documentInfo.flags, this.mState)) {
            onDocumentPicked(documentInfo, i, i2);
            this.mSelectionMgr.clearSelection();
            return true;
        }
        return false;
    }

    @Override
    public void springOpenDirectory(DocumentInfo documentInfo) {
        this.mActionModeAddons.finishActionMode();
        openContainerDocument(documentInfo);
    }

    private Selection getSelectedOrFocused() {
        String focusModelId;
        MutableSelection stableSelection = getStableSelection();
        if (stableSelection.isEmpty() && (focusModelId = this.mFocusHandler.getFocusModelId()) != null) {
            stableSelection.add(focusModelId);
        }
        return stableSelection;
    }

    @Override
    public void cutToClipboard() {
        Metrics.logUserAction(this.mActivity, 26);
        Selection selectedOrFocused = getSelectedOrFocused();
        if (selectedOrFocused.isEmpty()) {
            return;
        }
        if (this.mModel.hasDocuments(selectedOrFocused, DocumentFilters.NOT_MOVABLE)) {
            this.mDialogs.showOperationUnsupported();
            return;
        }
        this.mSelectionMgr.clearSelection();
        DocumentClipper documentClipper = this.mClipper;
        Model model = this.mModel;
        Objects.requireNonNull(model);
        documentClipper.clipDocumentsForCut(new $$Lambda$Ybyjy1_9Q7LLRKtoO6YABR_lvPc(model), selectedOrFocused, this.mState.stack.peek());
        this.mDialogs.showDocumentsClipped(selectedOrFocused.size());
    }

    @Override
    public void copyToClipboard() {
        Metrics.logUserAction(this.mActivity, 23);
        Selection selectedOrFocused = getSelectedOrFocused();
        if (selectedOrFocused.isEmpty()) {
            return;
        }
        this.mSelectionMgr.clearSelection();
        DocumentClipper documentClipper = this.mClipper;
        Model model = this.mModel;
        Objects.requireNonNull(model);
        documentClipper.clipDocumentsForCopy(new $$Lambda$Ybyjy1_9Q7LLRKtoO6YABR_lvPc(model), selectedOrFocused);
        this.mDialogs.showDocumentsClipped(selectedOrFocused.size());
    }

    @Override
    public void viewInOwner() {
        Metrics.logUserAction(this.mActivity, 29);
        Selection selectedOrFocused = getSelectedOrFocused();
        if (selectedOrFocused.isEmpty() || selectedOrFocused.size() > 1) {
            return;
        }
        DocumentInfo document = this.mModel.getDocument(selectedOrFocused.iterator().next());
        Intent intent = new Intent("android.provider.action.DOCUMENT_SETTINGS");
        intent.setPackage(this.mProviders.getPackageName(document.authority));
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setData(document.derivedUri);
        try {
            this.mActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("ManagerActionHandler", "Failed to view settings in application for " + document.derivedUri, e);
            this.mDialogs.showNoApplicationFound();
        }
    }

    @Override
    public void deleteSelectedDocuments() {
        Metrics.logUserAction(this.mActivity, 13);
        final Selection selectedOrFocused = getSelectedOrFocused();
        if (selectedOrFocused.isEmpty()) {
            return;
        }
        final DocumentInfo documentInfoPeek = this.mState.stack.peek();
        this.mDialogs.confirmDelete(this.mModel.getDocuments(selectedOrFocused), new ConfirmationCallback() {
            @Override
            public final void accept(int i) {
                ActionHandler.lambda$deleteSelectedDocuments$1(this.f$0, selectedOrFocused, documentInfoPeek, i);
            }
        });
    }

    public static void lambda$deleteSelectedDocuments$1(ActionHandler actionHandler, Selection selection, DocumentInfo documentInfo, int i) {
        actionHandler.mActionModeAddons.finishOnConfirmed(i);
        if (i != 0) {
            return;
        }
        try {
            Model model = actionHandler.mModel;
            Objects.requireNonNull(model);
            FileOperation fileOperationBuild = new FileOperation.Builder().withOpType(5).withDestination(actionHandler.mState.stack).withSrcs(UrisSupplier.create(selection, new $$Lambda$Ybyjy1_9Q7LLRKtoO6YABR_lvPc(model), actionHandler.mClipStore)).withSrcParent(documentInfo == null ? null : documentInfo.derivedUri).build();
            Activity activity = actionHandler.mActivity;
            DialogController dialogController = actionHandler.mDialogs;
            Objects.requireNonNull(dialogController);
            FileOperations.start(activity, fileOperationBuild, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController), FileOperations.createJobId());
        } catch (Exception e) {
            Log.e("ManagerActionHandler", "Failed to delete a file because we were unable to get item URIs.", e);
            actionHandler.mDialogs.showFileOperationStatus(2, 5, selection.size());
        }
    }

    @Override
    public void shareSelectedDocuments() {
        Intent intent;
        Metrics.logUserAction(this.mActivity, 17);
        MutableSelection stableSelection = getStableSelection();
        List<DocumentInfo> listLoadDocuments = this.mModel.loadDocuments(stableSelection, DocumentFilters.sharable(this.mFeatures));
        for (DocumentInfo documentInfo : listLoadDocuments) {
            if (documentInfo.isDrm && documentInfo.drmMethod < 0) {
                Log.d("ManagerActionHandler", "The select drm file has been deleted, don't forward");
                return;
            }
            if (documentInfo.isDrm && documentInfo.drmMethod >= 0 && documentInfo.drmMethod != 4) {
                Toast toastMakeText = Toast.makeText(this.mActivity, 134545485, 0);
                toastMakeText.setText(134545485);
                toastMakeText.show();
                Log.d("ManagerActionHandler", "Choose drm file '" + documentInfo.displayName + "' is " + documentInfo.drmMethod + " cann't shared!");
                return;
            }
        }
        if (listLoadDocuments.size() == 1) {
            intent = new Intent("android.intent.action.SEND");
            DocumentInfo documentInfo2 = listLoadDocuments.get(0);
            intent.setType(documentInfo2.mimeType);
            intent.putExtra("android.intent.extra.STREAM", documentInfo2.derivedUri);
        } else if (listLoadDocuments.size() > 1) {
            intent = new Intent("android.intent.action.SEND_MULTIPLE");
            ArrayList arrayList = new ArrayList();
            ArrayList<? extends Parcelable> arrayList2 = new ArrayList<>();
            for (DocumentInfo documentInfo3 : listLoadDocuments) {
                arrayList.add(documentInfo3.mimeType);
                arrayList2.add(documentInfo3.derivedUri);
            }
            intent.setType(MimeTypes.findCommonMimeType(arrayList));
            intent.putParcelableArrayListExtra("android.intent.extra.STREAM", arrayList2);
        } else {
            return;
        }
        intent.addFlags(1);
        intent.addCategory("android.intent.category.DEFAULT");
        if (this.mFeatures.isVirtualFilesSharingEnabled() && this.mModel.hasDocuments(stableSelection, DocumentFilters.VIRTUAL)) {
            intent.addCategory("android.intent.category.TYPED_OPENABLE");
        }
        this.mActivity.startActivity(Intent.createChooser(intent, this.mActivity.getResources().getText(R.string.share_via)));
    }

    public void initLocation(Intent intent) {
        if (this.mState.stack.isInitialized()) {
            if (SharedMinimal.DEBUG) {
                Log.d("ManagerActionHandler", "Stack already resolved for uri: " + intent.getData());
            }
            restoreRootAndDirectory();
            return;
        }
        if (launchToStackLocation(intent)) {
            if (SharedMinimal.DEBUG) {
                Log.d("ManagerActionHandler", "Launched to location from stack.");
            }
        } else if (launchToRoot(intent)) {
            if (SharedMinimal.DEBUG) {
                Log.d("ManagerActionHandler", "Launched to root for browsing.");
            }
        } else if (launchToDocument(intent)) {
            if (SharedMinimal.DEBUG) {
                Log.d("ManagerActionHandler", "Launched to a document.");
            }
        } else {
            if (SharedMinimal.DEBUG) {
                Log.d("ManagerActionHandler", "Launching directly into Home directory.");
            }
            loadHomeDir();
        }
    }

    @Override
    protected void launchToDefaultLocation() {
        loadHomeDir();
    }

    private boolean launchToStackLocation(Intent intent) {
        DocumentStack documentStack = (DocumentStack) intent.getParcelableExtra("com.android.documentsui.STACK");
        if (documentStack == null || documentStack.getRoot() == null) {
            return false;
        }
        this.mState.stack.reset(documentStack);
        if (this.mState.stack.isEmpty()) {
            this.mActivity.onRootPicked(this.mState.stack.getRoot());
        } else {
            this.mActivity.refreshCurrentRootAndDirectory(1);
        }
        return true;
    }

    private boolean launchToRoot(Intent intent) {
        if ("android.intent.action.VIEW".equals(intent.getAction())) {
            Uri data = intent.getData();
            if (DocumentsContract.isRootUri(this.mActivity, data)) {
                if (SharedMinimal.DEBUG) {
                    Log.d("ManagerActionHandler", "Launching with root URI.");
                }
                loadRoot(data);
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean launchToDocument(Intent intent) {
        if ("android.intent.action.VIEW".equals(intent.getAction())) {
            if (DocumentsContract.isDocumentUri(this.mActivity, intent.getData())) {
                return launchToDocument(intent.getData());
            }
            return false;
        }
        return false;
    }

    @Override
    public void showChooserForDoc(DocumentInfo documentInfo) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        if (manageDocument(documentInfo)) {
            Log.w("ManagerActionHandler", "Open with is not yet supported for managed doc.");
            return;
        }
        Intent intentCreateChooser = Intent.createChooser(buildViewIntent(documentInfo), null);
        if (Features.OMC_RUNTIME) {
            intentCreateChooser.putExtra("android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE", false);
        }
        try {
            this.mActivity.startActivity(intentCreateChooser);
        } catch (ActivityNotFoundException e) {
            this.mDialogs.showNoApplicationFound();
        }
    }

    private void onDocumentPicked(DocumentInfo documentInfo, int i, int i2) {
        if (documentInfo.isContainer()) {
            openContainerDocument(documentInfo);
            return;
        }
        if (manageDocument(documentInfo) || MimeTypes.isApkType(documentInfo.mimeType)) {
            return;
        }
        switch (i) {
            case 1:
                if (viewDocument(documentInfo)) {
                    return;
                }
                break;
            case 2:
                if (previewDocument(documentInfo)) {
                    return;
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal view type.");
        }
        switch (i2) {
            case 0:
                break;
            case 1:
                if (viewDocument(documentInfo)) {
                    return;
                }
                break;
            case 2:
                if (previewDocument(documentInfo)) {
                    return;
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal fallback view type.");
        }
        if (i != 0 && i2 != 0 && documentInfo.isInArchive()) {
            this.mDialogs.showViewInArchivesUnsupported();
        }
    }

    private boolean viewDocument(DocumentInfo documentInfo) {
        if (documentInfo.isPartial()) {
            Log.w("ManagerActionHandler", "Can't view partial file.");
            return false;
        }
        if (documentInfo.isInArchive()) {
            Log.w("ManagerActionHandler", "Can't view files in archives.");
            return false;
        }
        if (documentInfo.isDirectory()) {
            Log.w("ManagerActionHandler", "Can't view directories.");
            return true;
        }
        Intent intentBuildViewIntent = buildViewIntent(documentInfo);
        if (SharedMinimal.DEBUG && intentBuildViewIntent.getClipData() != null) {
            Log.d("ManagerActionHandler", "Starting intent w/ clip data: " + intentBuildViewIntent.getClipData());
        }
        try {
            this.mActivity.startActivity(intentBuildViewIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            this.mDialogs.showNoApplicationFound();
            return false;
        }
    }

    private boolean previewDocument(DocumentInfo documentInfo) {
        if (documentInfo.isPartial()) {
            Log.w("ManagerActionHandler", "Can't view partial file.");
            return false;
        }
        Intent intentBuild = new QuickViewIntentBuilder(this.mActivity.getPackageManager(), this.mActivity.getResources(), documentInfo, this.mModel).build();
        if (intentBuild != null) {
            try {
                this.mActivity.startActivity(intentBuild);
                return true;
            } catch (SecurityException e) {
                Log.e("ManagerActionHandler", "Caught security error: " + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private boolean manageDocument(DocumentInfo documentInfo) {
        if (isManagedDownload(documentInfo)) {
            Intent intent = new Intent("android.provider.action.MANAGE_DOCUMENT");
            intent.setData(documentInfo.derivedUri);
            try {
                this.mActivity.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    private boolean isManagedDownload(DocumentInfo documentInfo) {
        if ((!"android.intent.action.VIEW".equals(this.mActivity.getIntent().getAction()) || this.mState.stack.size() <= 1) && this.mActivity.getCurrentRoot().isDownloads()) {
            return documentInfo.isPartial();
        }
        return false;
    }

    private Intent buildViewIntent(DocumentInfo documentInfo) {
        int i;
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(documentInfo.derivedUri, documentInfo.mimeType);
        if (documentInfo.isWriteSupported()) {
            i = 3;
        } else {
            i = 1;
        }
        intent.setFlags(i);
        return intent;
    }

    @Override
    public void showInspector(DocumentInfo documentInfo) {
        Metrics.logUserAction(this.mActivity, 30);
        Intent intent = new Intent(this.mActivity, (Class<?>) InspectorActivity.class);
        intent.setData(documentInfo.derivedUri);
        intent.putExtra("com.android.documentsui.SHOW_DEBUG", this.mFeatures.isDebugSupportEnabled() && (Build.IS_DEBUGGABLE || DebugFlags.getDocumentDetailsEnabled()));
        if (documentInfo.isDirectory() && this.mState.stack.size() == 1 && this.mState.stack.get(0).equals(documentInfo)) {
            intent.putExtra("android.intent.extra.TITLE", this.mActivity.getCurrentRoot().title);
        }
        this.mActivity.startActivity(intent);
    }
}
