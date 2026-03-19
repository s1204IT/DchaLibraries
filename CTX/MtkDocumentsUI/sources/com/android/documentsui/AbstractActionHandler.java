package com.android.documentsui;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import com.android.documentsui.AbstractActionHandler.CommonAddons;
import com.android.documentsui.LoadDocStackTask;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.CheckedTask;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.Providers;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.Shared;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.dirlist.FocusHandler;
import com.android.documentsui.files.LauncherActivity;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.roots.GetRootDocumentTask;
import com.android.documentsui.roots.LoadRootTask;
import com.android.documentsui.roots.ProvidersAccess;
import com.android.documentsui.selection.ContentLock;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MutableSelection;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.sidebar.EjectRootTask;
import com.android.documentsui.ui.Snackbars;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public abstract class AbstractActionHandler<T extends Activity & CommonAddons> implements ActionHandler {
    static final boolean $assertionsDisabled = false;
    public static final int CODE_FORWARD = 42;
    static final int LOADER_ID = 42;
    protected final T mActivity;
    private final AbstractActionHandler<T>.LoaderBindings mBindings = new LoaderBindings();
    private ContentLock mContentLock;
    private Runnable mDisplayStateChangedListener;
    protected final DocumentsAccess mDocs;
    protected final Lookup<String, Executor> mExecutors;
    protected final FocusHandler mFocusHandler;
    protected final Injector<?> mInjector;
    protected final ProvidersAccess mProviders;
    protected final SearchViewManager mSearchMgr;
    protected final SelectionHelper mSelectionMgr;
    protected final State mState;

    public interface CommonAddons {
        RootInfo getCurrentRoot();

        void notifyDirectoryNavigated(Uri uri);

        void onDocumentPicked(DocumentInfo documentInfo);

        void onDocumentsPicked(List<DocumentInfo> list);

        void onRootPicked(RootInfo rootInfo);

        void refreshCurrentRootAndDirectory(int i);

        void restoreRootAndDirectory();

        void setRootsDrawerOpen(boolean z);

        void updateNavigator();
    }

    protected abstract void launchToDefaultLocation();

    @Override
    public void registerDisplayStateChangedListener(Runnable runnable) {
        this.mDisplayStateChangedListener = runnable;
    }

    @Override
    public void unregisterDisplayStateChangedListener(Runnable runnable) {
        if (this.mDisplayStateChangedListener == runnable) {
            this.mDisplayStateChangedListener = null;
        }
    }

    public AbstractActionHandler(T t, State state, ProvidersAccess providersAccess, DocumentsAccess documentsAccess, SearchViewManager searchViewManager, Lookup<String, Executor> lookup, Injector<?> injector) {
        this.mActivity = t;
        this.mState = state;
        this.mProviders = providersAccess;
        this.mDocs = documentsAccess;
        this.mFocusHandler = injector.focusManager;
        this.mSelectionMgr = injector.selectionMgr;
        this.mSearchMgr = searchViewManager;
        this.mExecutors = lookup;
        this.mInjector = injector;
    }

    @Override
    public void ejectRoot(RootInfo rootInfo, BooleanConsumer booleanConsumer) {
        new EjectRootTask(this.mActivity.getContentResolver(), rootInfo.authority, rootInfo.rootId, booleanConsumer).executeOnExecutor(ProviderExecutor.forAuthority(rootInfo.authority), new Void[0]);
    }

    @Override
    public void startAuthentication(PendingIntent pendingIntent) {
        try {
            this.mActivity.startIntentSenderForResult(pendingIntent.getIntentSender(), 43, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.d("AbstractActionHandler", "Authentication Pending Intent either canceled or ignored.");
        }
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 43) {
            onAuthenticationResult(i2);
        }
    }

    private void onAuthenticationResult(int i) {
        if (i == -1) {
            Log.v("AbstractActionHandler", "Authentication was successful. Refreshing directory now.");
            this.mActivity.refreshCurrentRootAndDirectory(1);
        }
    }

    @Override
    public void getRootDocument(RootInfo rootInfo, int i, Consumer<DocumentInfo> consumer) {
        new GetRootDocumentTask(rootInfo, this.mActivity, i, this.mDocs, consumer).executeOnExecutor(this.mExecutors.lookup(rootInfo.authority), new Void[0]);
    }

    @Override
    public void refreshDocument(DocumentInfo documentInfo, BooleanConsumer booleanConsumer) {
        Features features = this.mInjector.features;
        State state = this.mState;
        Context applicationContext = this.mActivity.getApplicationContext();
        final T t = this.mActivity;
        Objects.requireNonNull(t);
        new RefreshTask(features, state, documentInfo, 500L, applicationContext, new CheckedTask.Check() {
            @Override
            public final boolean stop() {
                return t.isDestroyed();
            }
        }, booleanConsumer).executeOnExecutor(this.mExecutors.lookup(documentInfo == null ? null : documentInfo.authority), new Void[0]);
    }

    @Override
    public void openSelectedInNewWindow() {
        throw new UnsupportedOperationException("Can't open in new window.");
    }

    @Override
    public void openInNewWindow(DocumentStack documentStack) {
        Metrics.logUserAction(this.mActivity, 21);
        Intent intentCreateLaunchIntent = LauncherActivity.createLaunchIntent(this.mActivity);
        intentCreateLaunchIntent.putExtra("com.android.documentsui.STACK", documentStack);
        if (this.mActivity.isInMultiWindowMode()) {
            intentCreateLaunchIntent.addFlags(4096);
        }
        this.mActivity.startActivity(intentCreateLaunchIntent);
    }

    @Override
    public boolean openItem(ItemDetailsLookup.ItemDetails itemDetails, int i, int i2) {
        throw new UnsupportedOperationException("Can't open document.");
    }

    @Override
    public void showInspector(DocumentInfo documentInfo) {
        throw new UnsupportedOperationException("Can't open properties.");
    }

    @Override
    public void springOpenDirectory(DocumentInfo documentInfo) {
        throw new UnsupportedOperationException("Can't spring open directories.");
    }

    @Override
    public void openSettings(RootInfo rootInfo) {
        throw new UnsupportedOperationException("Can't open settings.");
    }

    @Override
    public void openRoot(ResolveInfo resolveInfo) {
        throw new UnsupportedOperationException("Can't open an app.");
    }

    @Override
    public void showAppDetails(ResolveInfo resolveInfo) {
        throw new UnsupportedOperationException("Can't show app details.");
    }

    @Override
    public boolean dropOn(DragEvent dragEvent, RootInfo rootInfo) {
        throw new UnsupportedOperationException("Can't open an app.");
    }

    @Override
    public void pasteIntoFolder(RootInfo rootInfo) {
        throw new UnsupportedOperationException("Can't paste into folder.");
    }

    @Override
    public void viewInOwner() {
        throw new UnsupportedOperationException("Can't view in application.");
    }

    @Override
    public void selectAllFiles() {
        Metrics.logUserAction(this.mActivity, 16);
        Model model = this.mInjector.getModel();
        ArrayList arrayList = new ArrayList();
        for (String str : model.getModelIds()) {
            Cursor item = model.getItem(str);
            if (item == null) {
                Log.w("AbstractActionHandler", "Skipping selection. Can't obtain cursor for modeId: " + str);
            } else {
                if (this.mInjector.config.isDocumentEnabled(DocumentInfo.getCursorString(item, "mime_type"), DocumentInfo.getCursorInt(item, "flags"), this.mState)) {
                    arrayList.add(str);
                }
            }
        }
        if (this.mSelectionMgr.setItemsSelected(arrayList, true)) {
            this.mDisplayStateChangedListener.run();
        }
    }

    @Override
    public void showCreateDirectoryDialog() {
        Metrics.logUserAction(this.mActivity, 15);
        CreateDirectoryFragment.show(this.mActivity.getFragmentManager());
    }

    @Override
    public DocumentInfo renameDocument(String str, DocumentInfo documentInfo) {
        throw new UnsupportedOperationException("Can't rename documents.");
    }

    @Override
    public void showChooserForDoc(DocumentInfo documentInfo) {
        throw new UnsupportedOperationException("Show chooser for doc not supported!");
    }

    @Override
    public void openRootDocument(DocumentInfo documentInfo) {
        if (documentInfo == null) {
            this.mActivity.refreshCurrentRootAndDirectory(1);
        } else {
            openContainerDocument(documentInfo);
        }
    }

    @Override
    public void openContainerDocument(final DocumentInfo documentInfo) {
        if (this.mSearchMgr.isSearching()) {
            loadDocument(documentInfo.derivedUri, new LoadDocStackTask.LoadDocStackCallback() {
                @Override
                public final void onDocumentStackLoaded(DocumentStack documentStack) {
                    this.f$0.openFolderInSearchResult(documentStack, documentInfo);
                }
            });
        } else {
            openChildContainer(documentInfo);
        }
    }

    private void openFolderInSearchResult(DocumentStack documentStack, DocumentInfo documentInfo) {
        if (documentStack == null) {
            this.mState.stack.popToRootDocument();
            this.mActivity.updateNavigator();
            this.mState.stack.push(documentInfo);
        } else {
            if (!Objects.equals(this.mState.stack.getRoot(), documentStack.getRoot())) {
                Log.w("AbstractActionHandler", "Provider returns " + documentStack.getRoot() + " rather than expected " + this.mState.stack.getRoot());
            }
            DocumentInfo documentInfoPeek = documentStack.peek();
            if (documentInfoPeek.isArchive()) {
                documentStack.pop();
                documentStack.push(this.mDocs.getArchiveDocument(documentInfoPeek.derivedUri));
            }
            this.mState.stack.reset();
            this.mActivity.updateNavigator();
            this.mState.stack.reset(documentStack);
        }
        int i = 1;
        if (this.mState.stack.hasLocationChanged() && this.mState.stack.size() > 1) {
            i = 4;
        }
        this.mActivity.refreshCurrentRootAndDirectory(i);
    }

    private void openChildContainer(DocumentInfo documentInfo) {
        if (!documentInfo.isDirectory()) {
            if (documentInfo.isArchive()) {
                documentInfo = this.mDocs.getArchiveDocument(documentInfo.derivedUri);
            } else {
                documentInfo = null;
            }
        }
        this.mActivity.notifyDirectoryNavigated(documentInfo.derivedUri);
        if (!this.mState.stack.isPresent(documentInfo)) {
            this.mState.stack.push(documentInfo);
        }
        int i = 1;
        if (this.mState.stack.hasLocationChanged() && this.mState.stack.size() > 1) {
            i = 4;
        }
        this.mActivity.refreshCurrentRootAndDirectory(i);
    }

    @Override
    public void setDebugMode(boolean z) {
        if (!this.mInjector.features.isDebugSupportEnabled()) {
            return;
        }
        this.mState.debugMode = z;
        this.mInjector.features.forceFeature(R.bool.feature_command_interceptor, z);
        this.mInjector.features.forceFeature(R.bool.feature_inspector, z);
        this.mActivity.invalidateOptionsMenu();
        if (z) {
            showDebugMessage();
        } else {
            this.mActivity.getActionBar().setBackgroundDrawable(new ColorDrawable(this.mActivity.getResources().getColor(R.color.primary)));
            this.mActivity.getWindow().setStatusBarColor(this.mActivity.getResources().getColor(R.color.primary_dark));
        }
    }

    @Override
    public void showDebugMessage() {
        int[] nextColors = this.mInjector.debugHelper.getNextColors();
        Pair<String, Integer> nextMessage = this.mInjector.debugHelper.getNextMessage();
        Snackbars.showCustomTextWithImage(this.mActivity, (String) nextMessage.first, ((Integer) nextMessage.second).intValue());
        this.mActivity.getActionBar().setBackgroundDrawable(new ColorDrawable(nextColors[0]));
        this.mActivity.getWindow().setStatusBarColor(nextColors[1]);
    }

    @Override
    public void cutToClipboard() {
        throw new UnsupportedOperationException("Cut not supported!");
    }

    @Override
    public void copyToClipboard() {
        throw new UnsupportedOperationException("Copy not supported!");
    }

    @Override
    public void deleteSelectedDocuments() {
        throw new UnsupportedOperationException("Delete not supported!");
    }

    @Override
    public void shareSelectedDocuments() {
        throw new UnsupportedOperationException("Share not supported!");
    }

    protected final void loadDocument(Uri uri, LoadDocStackTask.LoadDocStackCallback loadDocStackCallback) {
        new LoadDocStackTask(this.mActivity, this.mProviders, this.mDocs, loadDocStackCallback).executeOnExecutor(this.mExecutors.lookup(uri.getAuthority()), new Uri[]{uri});
    }

    public final void loadRoot(Uri uri) {
        new LoadRootTask(this.mActivity, this.mProviders, this.mState, uri).executeOnExecutor(this.mExecutors.lookup(uri.getAuthority()), new Void[0]);
    }

    @Override
    public void loadDocumentsForCurrentStack() {
        DocumentStack documentStack = this.mState.stack;
        if (!documentStack.isRecents() && documentStack.isEmpty()) {
            DirectoryResult directoryResult = new DirectoryResult();
            directoryResult.exception = new IllegalStateException("Failed to load root document.");
            this.mInjector.getModel().update(directoryResult);
            return;
        }
        this.mActivity.getLoaderManager().restartLoader(42, null, this.mBindings);
    }

    protected final boolean launchToDocument(Uri uri) {
        if (!Providers.isArchiveUri(uri)) {
            loadDocument(uri, new LoadDocStackTask.LoadDocStackCallback() {
                @Override
                public final void onDocumentStackLoaded(DocumentStack documentStack) {
                    this.f$0.onStackLoaded(documentStack);
                }
            });
            return true;
        }
        return $assertionsDisabled;
    }

    private void onStackLoaded(DocumentStack documentStack) {
        if (documentStack != null) {
            if (!documentStack.peek().isDirectory()) {
                documentStack.pop();
            }
            this.mState.stack.reset(documentStack);
            this.mActivity.refreshCurrentRootAndDirectory(1);
            Metrics.logLaunchAtLocation(this.mActivity, this.mState, documentStack.getRoot().getUri());
            return;
        }
        Log.w("AbstractActionHandler", "Failed to launch into the given uri. Launch to default location.");
        launchToDefaultLocation();
        Metrics.logLaunchAtLocation(this.mActivity, this.mState, null);
    }

    protected void restoreRootAndDirectory() {
        if (!this.mState.stack.getRoot().isRecents() && this.mState.stack.isEmpty()) {
            this.mActivity.onRootPicked(this.mState.stack.getRoot());
        } else {
            this.mActivity.restoreRootAndDirectory();
        }
    }

    protected final void loadHomeDir() {
        loadRoot(Shared.getDefaultRootUri(this.mActivity));
    }

    protected MutableSelection getStableSelection() {
        MutableSelection mutableSelection = new MutableSelection();
        this.mSelectionMgr.copySelection(mutableSelection);
        return mutableSelection;
    }

    @Override
    public ActionHandler reset(ContentLock contentLock) {
        this.mContentLock = contentLock;
        this.mActivity.getLoaderManager().destroyLoader(42);
        return this;
    }

    private final class LoaderBindings implements LoaderManager.LoaderCallbacks<DirectoryResult> {
        static final boolean $assertionsDisabled = false;

        private LoaderBindings() {
        }

        @Override
        public Loader<DirectoryResult> onCreateLoader(int i, Bundle bundle) {
            Uri uriBuildChildDocumentsUri;
            T t = AbstractActionHandler.this.mActivity;
            if (AbstractActionHandler.this.mState.stack.isRecents()) {
                if (SharedMinimal.DEBUG) {
                    Log.d("AbstractActionHandler", "Creating new loader recents.");
                }
                return new RecentsLoader(t, AbstractActionHandler.this.mProviders, AbstractActionHandler.this.mState, AbstractActionHandler.this.mInjector.features, AbstractActionHandler.this.mExecutors, AbstractActionHandler.this.mInjector.fileTypeLookup);
            }
            if (AbstractActionHandler.this.mSearchMgr.isSearching()) {
                uriBuildChildDocumentsUri = DocumentsContract.buildSearchDocumentsUri(AbstractActionHandler.this.mState.stack.getRoot().authority, AbstractActionHandler.this.mState.stack.getRoot().rootId, AbstractActionHandler.this.mSearchMgr.getCurrentSearch());
            } else {
                uriBuildChildDocumentsUri = DocumentsContract.buildChildDocumentsUri(AbstractActionHandler.this.mState.stack.peek().authority, AbstractActionHandler.this.mState.stack.peek().documentId);
            }
            if (AbstractActionHandler.this.mInjector.config.managedModeEnabled(AbstractActionHandler.this.mState.stack)) {
                uriBuildChildDocumentsUri = DocumentsContract.setManageMode(uriBuildChildDocumentsUri);
            }
            Uri uri = uriBuildChildDocumentsUri;
            if (SharedMinimal.DEBUG) {
                Log.d("AbstractActionHandler", "Creating new directory loader for: " + DocumentInfo.debugString(AbstractActionHandler.this.mState.stack.peek()));
            }
            return new DirectoryLoader(AbstractActionHandler.this.mInjector.features, t, AbstractActionHandler.this.mState.stack.getRoot(), AbstractActionHandler.this.mState.stack.peek(), uri, AbstractActionHandler.this.mState.sortModel, AbstractActionHandler.this.mInjector.fileTypeLookup, AbstractActionHandler.this.mContentLock, AbstractActionHandler.this.mSearchMgr.isSearching());
        }

        @Override
        public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult directoryResult) {
            if (SharedMinimal.DEBUG) {
                Log.d("AbstractActionHandler", "Loader has finished for: " + DocumentInfo.debugString(AbstractActionHandler.this.mState.stack.peek()));
            }
            AbstractActionHandler.this.mInjector.getModel().update(directoryResult);
        }

        @Override
        public void onLoaderReset(Loader<DirectoryResult> loader) {
        }
    }
}
