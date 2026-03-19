package com.android.documentsui;

import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.MenuManager;
import com.android.documentsui.base.DebugHelper;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.dirlist.DocsStableIdProvider;
import com.android.documentsui.dirlist.DocumentsAdapter;
import com.android.documentsui.prefs.ScopedPreferences;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.selection.ContentLock;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.ui.DialogController;
import com.android.documentsui.ui.MessageBuilder;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.function.Consumer;

public class Injector<T extends ActionHandler> {
    static final boolean $assertionsDisabled = false;
    public ActionModeController actionModeController;
    public T actions;
    public final ActivityConfig config;
    public final DebugHelper debugHelper;
    public DialogController dialogs;
    public final Features features;
    public final Lookup<String, String> fileTypeLookup;
    public FocusManager focusManager;
    private final Model mModel;
    public MenuManager menuManager;
    public final MessageBuilder messages;
    public final ScopedPreferences prefs;
    public SearchViewManager searchManager;
    public DocsSelectionHelper selectionMgr;
    public final Consumer<Collection<RootInfo>> shortcutsUpdater;

    public Injector(Features features, ActivityConfig activityConfig, ScopedPreferences scopedPreferences, MessageBuilder messageBuilder, DialogController dialogController, Lookup<String, String> lookup, Consumer<Collection<RootInfo>> consumer) {
        this(features, activityConfig, scopedPreferences, messageBuilder, dialogController, lookup, consumer, new Model(features));
    }

    @VisibleForTesting
    public Injector(Features features, ActivityConfig activityConfig, ScopedPreferences scopedPreferences, MessageBuilder messageBuilder, DialogController dialogController, Lookup<String, String> lookup, Consumer<Collection<RootInfo>> consumer, Model model) {
        this.features = features;
        this.config = activityConfig;
        this.prefs = scopedPreferences;
        this.messages = messageBuilder;
        this.dialogs = dialogController;
        this.fileTypeLookup = lookup;
        this.shortcutsUpdater = consumer;
        this.mModel = model;
        this.debugHelper = new DebugHelper(this);
    }

    public Model getModel() {
        return this.mModel;
    }

    public FocusManager getFocusManager(RecyclerView recyclerView, Model model) {
        return this.focusManager.reset(recyclerView, model);
    }

    public SelectionHelper getSelectionManager(DocumentsAdapter documentsAdapter, SelectionHelper.SelectionPredicate selectionPredicate) {
        return this.selectionMgr.reset(documentsAdapter, new DocsStableIdProvider(documentsAdapter), selectionPredicate);
    }

    public final ActionModeController getActionModeController(MenuManager.SelectionDetails selectionDetails, EventHandler<MenuItem> eventHandler) {
        return this.actionModeController.reset(selectionDetails, eventHandler);
    }

    public T getActionHandler(ContentLock contentLock) {
        if (contentLock == null) {
            return this.actions;
        }
        return (T) this.actions.reset(contentLock);
    }
}
