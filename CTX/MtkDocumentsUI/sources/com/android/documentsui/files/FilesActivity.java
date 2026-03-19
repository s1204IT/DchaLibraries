package com.android.documentsui.files;

import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MenuItem;
import com.android.documentsui.ActionModeController;
import com.android.documentsui.BaseActivity;
import com.android.documentsui.DocsSelectionHelper;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.FocusManager;
import com.android.documentsui.Injector;
import com.android.documentsui.MenuManager;
import com.android.documentsui.Model;
import com.android.documentsui.OperationDialogFragment;
import com.android.documentsui.ProviderExecutor;
import com.android.documentsui.R;
import com.android.documentsui.SharedInputHandler;
import com.android.documentsui.ShortcutsUpdater;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.Procedure;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.State;
import com.android.documentsui.clipping.DocumentClipper;
import com.android.documentsui.dirlist.DirectoryFragment;
import com.android.documentsui.files.ActionHandler;
import com.android.documentsui.prefs.ScopedPreferences;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.roots.ProvidersCache;
import com.android.documentsui.sidebar.RootsFragment;
import com.android.documentsui.ui.DialogController;
import com.android.documentsui.ui.MessageBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class FilesActivity extends BaseActivity implements ActionHandler.Addons {
    static final boolean $assertionsDisabled = false;
    private ActivityInputHandler mActivityInputHandler;
    private Injector<ActionHandler<FilesActivity>> mInjector;
    private SharedInputHandler mSharedInputHandler;

    public FilesActivity() {
        super(R.layout.files_activity, "FilesActivity");
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
        MessageBuilder messageBuilder = new MessageBuilder(this);
        Features featuresCreate = Features.create(this);
        ScopedPreferences scopedPreferencesCreate = ScopedPreferences.create(this, "files");
        Config config = new Config();
        ScopedPreferences scopedPreferencesCreate2 = ScopedPreferences.create(this, "files");
        DialogController dialogControllerCreate = DialogController.create(featuresCreate, this, messageBuilder);
        Lookup<String, String> fileTypeLookup = DocumentsApplication.getFileTypeLookup(this);
        final ShortcutsUpdater shortcutsUpdater = new ShortcutsUpdater(this, scopedPreferencesCreate);
        this.mInjector = new Injector<>(featuresCreate, config, scopedPreferencesCreate2, messageBuilder, dialogControllerCreate, fileTypeLookup, new Consumer() {
            @Override
            public final void accept(Object obj) {
                shortcutsUpdater.update((Collection) obj);
            }
        });
        super.onCreate(bundle);
        final DocumentClipper documentClipper = DocumentsApplication.getDocumentClipper(this);
        this.mInjector.selectionMgr = DocsSelectionHelper.createMultiSelect();
        this.mInjector.focusManager = new FocusManager(this.mInjector.features, this.mInjector.selectionMgr, this.mDrawer, new Procedure() {
            @Override
            public final boolean run() {
                return this.f$0.focusSidebar();
            }
        }, getColor(R.color.accent_dark));
        Injector<ActionHandler<FilesActivity>> injector = this.mInjector;
        Features features = this.mInjector.features;
        SearchViewManager searchViewManager = this.mSearchManager;
        State state = this.mState;
        MenuManager.DirectoryDetails directoryDetails = new MenuManager.DirectoryDetails(this) {
            @Override
            public boolean hasItemsToPaste() {
                return documentClipper.hasItemsToPaste();
            }
        };
        Context applicationContext = getApplicationContext();
        DocsSelectionHelper docsSelectionHelper = this.mInjector.selectionMgr;
        final ProvidersCache providersCache = this.mProviders;
        Objects.requireNonNull(providersCache);
        Lookup lookup = new Lookup() {
            @Override
            public final Object lookup(Object obj) {
                return providersCache.getApplicationName((String) obj);
            }
        };
        final Model model = this.mInjector.getModel();
        Objects.requireNonNull(model);
        injector.menuManager = new MenuManager(features, searchViewManager, state, directoryDetails, applicationContext, docsSelectionHelper, lookup, new Lookup() {
            @Override
            public final Object lookup(Object obj) {
                return model.getItemUri((String) obj);
            }
        });
        this.mInjector.actionModeController = new ActionModeController(this, this.mInjector.selectionMgr, this.mInjector.menuManager, this.mInjector.messages);
        this.mInjector.actions = new ActionHandler(this, this.mState, this.mProviders, this.mDocs, this.mSearchManager, new Lookup() {
            @Override
            public final Object lookup(Object obj) {
                return ProviderExecutor.forAuthority((String) obj);
            }
        }, this.mInjector.actionModeController, documentClipper, DocumentsApplication.getClipStore(this), DocumentsApplication.getDragAndDropManager(this), this.mInjector);
        this.mInjector.searchManager = this.mSearchManager;
        final ActionHandler actionHandler = (ActionHandler) this.mInjector.actions;
        Objects.requireNonNull(actionHandler);
        this.mActivityInputHandler = new ActivityInputHandler(new Runnable() {
            @Override
            public final void run() {
                actionHandler.deleteSelectedDocuments();
            }
        });
        FocusManager focusManager = this.mInjector.focusManager;
        DocsSelectionHelper docsSelectionHelper2 = this.mInjector.selectionMgr;
        final SearchViewManager searchViewManager2 = this.mInjector.searchManager;
        Objects.requireNonNull(searchViewManager2);
        this.mSharedInputHandler = new SharedInputHandler(focusManager, docsSelectionHelper2, new Procedure() {
            @Override
            public final boolean run() {
                return searchViewManager2.cancelSearch();
            }
        }, new Procedure() {
            @Override
            public final boolean run() {
                return this.f$0.popDir();
            }
        }, this.mInjector.features);
        RootsFragment.show(getFragmentManager(), null);
        Intent intent = getIntent();
        ((ActionHandler) this.mInjector.actions).initLocation(intent);
        if (intent.hasExtra("com.android.documentsui.taskLabel") && intent.hasExtra("com.android.documentsui.taskIcon")) {
            updateTaskDescription(intent);
        }
        presentFileErrors(bundle, intent);
    }

    private void updateTaskDescription(Intent intent) {
        setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(intent.getIntExtra("com.android.documentsui.taskLabel", -1)), flattenDrawableToBitmap(getResources().getDrawable(intent.getIntExtra("com.android.documentsui.taskIcon", -1), null))));
    }

    private Bitmap flattenDrawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        if (!(drawable instanceof AdaptiveIconDrawable)) {
            return null;
        }
        AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) drawable;
        int iMax = Math.max(getResources().getDimensionPixelSize(android.R.dimen.app_icon_size), adaptiveIconDrawable.getIntrinsicHeight());
        adaptiveIconDrawable.setBounds(0, 0, iMax, iMax);
        float f = iMax;
        float f2 = 0.010416667f * f;
        float f3 = 0.020833334f * f;
        int i = (int) (f + (2.0f * f2) + f3);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate((f3 / 2.0f) + f2, f2);
        Paint paint = new Paint(1);
        paint.setColor(0);
        paint.setShadowLayer(f2, 0.0f, 0.0f, 503316480);
        canvas.drawPath(adaptiveIconDrawable.getIconMask(), paint);
        canvas.translate(0.0f, f3);
        paint.setShadowLayer(f2, 0.0f, 0.0f, 1023410176);
        canvas.drawPath(adaptiveIconDrawable.getIconMask(), paint);
        adaptiveIconDrawable.draw(canvas);
        canvas.setBitmap(null);
        return bitmapCreateBitmap;
    }

    private void presentFileErrors(Bundle bundle, Intent intent) {
        int intExtra = intent.getIntExtra("com.android.documentsui.DIALOG_TYPE", 0);
        if (bundle == null && intExtra != 0) {
            int intExtra2 = intent.getIntExtra("com.android.documentsui.OPERATION_TYPE", 1);
            OperationDialogFragment.show(getFragmentManager(), intExtra, intent.getParcelableArrayListExtra("com.android.documentsui.FAILED_DOCS"), intent.getParcelableArrayListExtra("com.android.documentsui.FAILED_URIS"), this.mState.stack, intExtra2);
        }
    }

    @Override
    public void includeState(State state) {
        state.initAcceptMimes(getIntent(), "*/*");
        state.action = 1;
        state.allowMultiple = true;
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        if (this.mSearchManager.isSearching()) {
            this.mNavigator.update();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        RootInfo currentRoot = getCurrentRoot();
        if (this.mProviders.getRootBlocking(currentRoot.authority, currentRoot.rootId) == null) {
            finish();
        }
    }

    @Override
    public String getDrawerTitle() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("android.intent.extra.TITLE")) {
            return intent.getStringExtra("android.intent.extra.TITLE");
        }
        return getString(R.string.app_label);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.mInjector.menuManager.updateOptionMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.option_menu_create_dir:
                ((ActionHandler) this.mInjector.actions).showCreateDirectoryDialog();
                return true;
            case R.id.option_menu_inspect:
                ((ActionHandler) this.mInjector.actions).showInspector(getCurrentDirectory());
                return true;
            case R.id.option_menu_new_window:
                ((ActionHandler) this.mInjector.actions).openInNewWindow(this.mState.stack);
                return true;
            case R.id.option_menu_select_all:
                ((ActionHandler) this.mInjector.actions).selectAllFiles();
                return true;
            case R.id.option_menu_settings:
                ((ActionHandler) this.mInjector.actions).openSettings(getCurrentRoot());
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> list, Menu menu, int i) {
        this.mInjector.menuManager.updateKeyboardShortcutsMenu(list, new IntFunction() {
            @Override
            public final Object apply(int i2) {
                return this.f$0.getString(i2);
            }
        });
    }

    @Override
    public void refreshDirectory(int i) {
        FragmentManager fragmentManager = getFragmentManager();
        RootInfo currentRoot = getCurrentRoot();
        DocumentInfo currentDirectory = getCurrentDirectory();
        if (this.mState.stack.isRecents()) {
            DirectoryFragment.showRecentsOpen(fragmentManager, i);
        } else {
            DirectoryFragment.showDirectory(fragmentManager, currentRoot, currentDirectory, i);
        }
    }

    @Override
    public void onDocumentsPicked(List<DocumentInfo> list) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDocumentPicked(DocumentInfo documentInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDirectoryCreated(DocumentInfo documentInfo) {
        this.mInjector.focusManager.focusDocument(documentInfo.documentId);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return this.mActivityInputHandler.onKeyDown(i, keyEvent) || this.mSharedInputHandler.onKeyDown(i, keyEvent) || super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyShortcut(int i, KeyEvent keyEvent) {
        if (i == 29) {
            ((ActionHandler) this.mInjector.actions).selectAllFiles();
            return true;
        }
        if (i == 31) {
            ((ActionHandler) this.mInjector.actions).copyToClipboard();
            return true;
        }
        if (i != 50) {
            if (i == 52) {
                ((ActionHandler) this.mInjector.actions).cutToClipboard();
                return true;
            }
            return super.onKeyShortcut(i, keyEvent);
        }
        DirectoryFragment directoryFragment = getDirectoryFragment();
        if (directoryFragment != null) {
            directoryFragment.pasteFromClipboard();
        }
        return true;
    }

    @Override
    public Injector<ActionHandler<FilesActivity>> getInjector() {
        return this.mInjector;
    }
}
