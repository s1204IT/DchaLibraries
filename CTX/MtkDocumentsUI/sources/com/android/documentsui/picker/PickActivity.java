package com.android.documentsui.picker;

import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import com.android.documentsui.ActionModeController;
import com.android.documentsui.BaseActivity;
import com.android.documentsui.DocsSelectionHelper;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.FocusManager;
import com.android.documentsui.Injector;
import com.android.documentsui.MenuManager;
import com.android.documentsui.ProviderExecutor;
import com.android.documentsui.R;
import com.android.documentsui.SharedInputHandler;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.MimeTypes;
import com.android.documentsui.base.Procedure;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.State;
import com.android.documentsui.dirlist.DirectoryFragment;
import com.android.documentsui.picker.ActionHandler;
import com.android.documentsui.prefs.ScopedPreferences;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.sidebar.RootsFragment;
import com.android.documentsui.ui.DialogController;
import com.android.documentsui.ui.MessageBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PickActivity extends BaseActivity implements ActionHandler.Addons {
    static final boolean $assertionsDisabled = false;
    private Injector<ActionHandler<PickActivity>> mInjector;
    private LastAccessedStorage mLastAccessed;
    private SharedInputHandler mSharedInputHandler;

    public PickActivity() {
        super(R.layout.documents_activity, "PickActivity");
    }

    @Override
    protected boolean focusSidebar() {
        return super.focusSidebar();
    }

    @Override
    protected boolean popDir() {
        return super.popDir();
    }

    @Override
    public void onCreate(Bundle bundle) {
        DocsSelectionHelper docsSelectionHelperCreateSingleSelect;
        Features featuresCreate = Features.create(this);
        this.mInjector = new Injector<>(featuresCreate, new Config(), ScopedPreferences.create(this, "picker"), new MessageBuilder(this), DialogController.create(featuresCreate, this, null), DocumentsApplication.getFileTypeLookup(this), new Consumer() {
            @Override
            public final void accept(Object obj) {
                PickActivity.lambda$onCreate$0((Collection) obj);
            }
        });
        super.onCreate(bundle);
        Injector<ActionHandler<PickActivity>> injector = this.mInjector;
        if (this.mState.allowMultiple) {
            docsSelectionHelperCreateSingleSelect = DocsSelectionHelper.createMultiSelect();
        } else {
            docsSelectionHelperCreateSingleSelect = DocsSelectionHelper.createSingleSelect();
        }
        injector.selectionMgr = docsSelectionHelperCreateSingleSelect;
        this.mInjector.focusManager = new FocusManager(this.mInjector.features, this.mInjector.selectionMgr, this.mDrawer, new Procedure() {
            @Override
            public final boolean run() {
                return this.f$0.focusSidebar();
            }
        }, getColor(R.color.accent_dark));
        this.mInjector.menuManager = new MenuManager(this.mSearchManager, this.mState, new MenuManager.DirectoryDetails(this));
        this.mInjector.actionModeController = new ActionModeController(this, this.mInjector.selectionMgr, this.mInjector.menuManager, this.mInjector.messages);
        this.mLastAccessed = LastAccessedStorage.create();
        this.mInjector.actions = new ActionHandler(this, this.mState, this.mProviders, this.mDocs, this.mSearchManager, new Lookup() {
            @Override
            public final Object lookup(Object obj) {
                return ProviderExecutor.forAuthority((String) obj);
            }
        }, this.mInjector, this.mLastAccessed);
        this.mInjector.searchManager = this.mSearchManager;
        Intent intent = getIntent();
        FocusManager focusManager = this.mInjector.focusManager;
        DocsSelectionHelper docsSelectionHelper = this.mInjector.selectionMgr;
        final SearchViewManager searchViewManager = this.mInjector.searchManager;
        Objects.requireNonNull(searchViewManager);
        this.mSharedInputHandler = new SharedInputHandler(focusManager, docsSelectionHelper, new Procedure() {
            @Override
            public final boolean run() {
                return searchViewManager.cancelSearch();
            }
        }, new Procedure() {
            @Override
            public final boolean run() {
                return this.f$0.popDir();
            }
        }, this.mInjector.features);
        setupLayout(intent);
        ((ActionHandler) this.mInjector.actions).initLocation(intent);
    }

    static void lambda$onCreate$0(Collection collection) {
    }

    private void setupLayout(Intent intent) {
        if (this.mState.action == 4) {
            SaveFragment.show(getFragmentManager(), intent.getType(), intent.getStringExtra("android.intent.extra.TITLE"));
        } else if (this.mState.action == 6 || this.mState.action == 2) {
            PickFragment.show(getFragmentManager());
        }
        if (this.mState.action == 5) {
            Intent intent2 = new Intent(intent);
            intent2.setComponent(null);
            intent2.setPackage(null);
            RootsFragment.show(getFragmentManager(), intent2);
            return;
        }
        if (this.mState.action == 3 || this.mState.action == 4 || this.mState.action == 6 || this.mState.action == 2) {
            RootsFragment.show(getFragmentManager(), (Intent) null);
        }
    }

    @Override
    protected void includeState(State state) {
        Intent intent = getIntent();
        state.initAcceptMimes(intent, intent.getType() == null ? "*/*" : intent.getType());
        String action = intent.getAction();
        if ("android.intent.action.OPEN_DOCUMENT".equals(action)) {
            state.action = 3;
        } else if ("android.intent.action.CREATE_DOCUMENT".equals(action)) {
            state.action = 4;
        } else if ("android.intent.action.GET_CONTENT".equals(action)) {
            state.action = 5;
        } else if ("android.intent.action.OPEN_DOCUMENT_TREE".equals(action)) {
            state.action = 6;
        } else if ("com.android.documentsui.PICK_COPY_DESTINATION".equals(action)) {
            state.action = 2;
        }
        if (state.action == 3 || state.action == 5) {
            state.allowMultiple = intent.getBooleanExtra("android.intent.extra.ALLOW_MULTIPLE", false);
        }
        if (state.action == 3 || state.action == 5 || state.action == 4) {
            state.openableOnly = intent.hasCategory("android.intent.category.OPENABLE");
        }
        if (state.action == 2) {
            state.directoryCopy = intent.getBooleanExtra("com.android.documentsui.DIRECTORY_COPY", false);
            state.copyOperationSubType = intent.getIntExtra("com.android.documentsui.OPERATION_TYPE", 1);
        }
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        this.mDrawer.update();
        this.mNavigator.update();
    }

    @Override
    public String getDrawerTitle() {
        String stringExtra = getIntent().getStringExtra("android.provider.extra.PROMPT");
        if (stringExtra == null) {
            if (this.mState.action == 3 || this.mState.action == 5 || this.mState.action == 6) {
                return getResources().getString(R.string.title_open);
            }
            if (this.mState.action == 4 || this.mState.action == 2) {
                return getResources().getString(R.string.title_save);
            }
            return getResources().getString(R.string.app_label);
        }
        return stringExtra;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.mInjector.menuManager.updateOptionMenu(menu);
        DocumentInfo currentDirectory = getCurrentDirectory();
        if (this.mState.action == 4) {
            SaveFragment saveFragment = SaveFragment.get(getFragmentManager());
            if (saveFragment != null) {
                saveFragment.prepareForDirectory(currentDirectory);
                return true;
            }
            Log.e("PickActivity", "onPrepareOptionsMenu, SaveFragment is null");
            return true;
        }
        return true;
    }

    @Override
    protected void refreshDirectory(int i) {
        PickFragment pickFragment;
        SaveFragment saveFragment;
        FragmentManager fragmentManager = getFragmentManager();
        RootInfo currentRoot = getCurrentRoot();
        DocumentInfo currentDirectory = getCurrentDirectory();
        if (this.mState.stack.isRecents()) {
            DirectoryFragment.showRecentsOpen(fragmentManager, i);
            boolean zMimeMatches = MimeTypes.mimeMatches(MimeTypes.VISUAL_MIMES, this.mState.acceptMimes);
            this.mState.derivedMode = zMimeMatches ? 2 : 1;
        } else {
            DirectoryFragment.showDirectory(fragmentManager, currentRoot, currentDirectory, i);
        }
        if (this.mState.action == 4 && (saveFragment = SaveFragment.get(fragmentManager)) != null) {
            saveFragment.setReplaceTarget(null);
        }
        if ((this.mState.action == 6 || this.mState.action == 2) && (pickFragment = PickFragment.get(fragmentManager)) != null) {
            pickFragment.setPickTarget(this.mState.action, this.mState.copyOperationSubType, currentDirectory);
        }
    }

    @Override
    protected void onDirectoryCreated(DocumentInfo documentInfo) {
        ((ActionHandler) this.mInjector.actions).openContainerDocument(documentInfo);
    }

    @Override
    public void onDocumentPicked(DocumentInfo documentInfo) {
        FragmentManager fragmentManager = getFragmentManager();
        if (documentInfo.isDirectory()) {
            ((ActionHandler) this.mInjector.actions).openContainerDocument(documentInfo);
            return;
        }
        if (this.mState.action == 3 || this.mState.action == 5) {
            ((ActionHandler) this.mInjector.actions).finishPicking(documentInfo.derivedUri);
        } else if (this.mState.action == 4) {
            SaveFragment.get(fragmentManager).setReplaceTarget(documentInfo);
        }
    }

    @Override
    public void onDocumentsPicked(List<DocumentInfo> list) {
        if (this.mState.action == 3 || this.mState.action == 5) {
            int size = list.size();
            Uri[] uriArr = new Uri[size];
            for (int i = 0; i < size; i++) {
                uriArr[i] = list.get(i).derivedUri;
            }
            ((ActionHandler) this.mInjector.actions).finishPicking(uriArr);
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return this.mSharedInputHandler.onKeyDown(i, keyEvent) || super.onKeyDown(i, keyEvent);
    }

    @Override
    public void setResult(int i, Intent intent, int i2) {
        setResult(i, intent);
    }

    @Override
    public Injector<ActionHandler<PickActivity>> getInjector() {
        return this.mInjector;
    }
}
