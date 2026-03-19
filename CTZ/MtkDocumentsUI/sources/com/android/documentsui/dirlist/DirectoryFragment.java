package com.android.documentsui.dirlist;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.ActionModeController;
import com.android.documentsui.BaseActivity;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.FocusManager;
import com.android.documentsui.Injector;
import com.android.documentsui.Metrics;
import com.android.documentsui.Model;
import com.android.documentsui.R;
import com.android.documentsui.ThumbnailCache;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.DocumentFilters;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.EventListener;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.clipping.ClipStore;
import com.android.documentsui.clipping.DocumentClipper;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.dirlist.DocumentsAdapter;
import com.android.documentsui.dirlist.DragStartListener;
import com.android.documentsui.picker.PickActivity;
import com.android.documentsui.selection.BandSelectionHelper;
import com.android.documentsui.selection.ContentLock;
import com.android.documentsui.selection.DefaultBandHost;
import com.android.documentsui.selection.DefaultBandPredicate;
import com.android.documentsui.selection.GestureRouter;
import com.android.documentsui.selection.GestureSelectionHelper;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MouseInputHandler;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.selection.TouchEventRouter;
import com.android.documentsui.services.FileOperation;
import com.android.documentsui.services.FileOperations;
import com.android.documentsui.sorting.SortDimension;
import com.android.documentsui.sorting.SortModel;
import com.android.documentsui.ui.DialogController;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class DirectoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    static final boolean $assertionsDisabled = false;
    public static int delete_count;
    public static boolean isDeleteInProgress;
    private ActionModeController mActionModeController;
    private ActionHandler mActions;
    private BaseActivity mActivity;
    private DocumentsAdapter mAdapter;
    private final DocumentsAdapter.Environment mAdapterEnv;
    private Runnable mBandSelectStartedCallback;
    private BandSelectionHelper mBandSelector;
    private DocumentClipper mClipper;
    private ItemDetailsLookup mDetailsLookup;
    private DragHoverListener mDragHoverListener;
    private FocusManager mFocusManager;
    private IconHelper mIconHelper;
    private Injector<?> mInjector;
    private KeyInputHandler mKeyListener;
    private GridLayoutManager mLayout;
    private DirectoryState mLocalState;
    private int mMode;
    private Model mModel;
    private final EventListener<Model.Update> mModelUpdateListener;
    private View mProgressBar;
    private RecyclerView mRecView;
    private SwipeRefreshLayout mRefreshLayout;
    private SelectionMetadata mSelectionMetadata;
    private SelectionHelper mSelectionMgr;
    private State mState;
    private int mColumnCount = 1;
    private float mLiveScale = 1.0f;
    private ContentLock mContentLock = new ContentLock();
    private Selection mRestoredSelection = null;
    private SortModel.UpdateListener mSortListener = new SortModel.UpdateListener() {
        @Override
        public final void onModelUpdate(SortModel sortModel, int i) {
            DirectoryFragment.lambda$new$0(this.f$0, sortModel, i);
        }
    };
    private final Runnable mOnDisplayStateChanged = new Runnable() {
        @Override
        public final void run() {
            this.f$0.onDisplayStateChanged();
        }
    };

    public DirectoryFragment() {
        this.mModelUpdateListener = new ModelUpdateListener();
        this.mAdapterEnv = new AdapterEnvironment();
    }

    public static void lambda$new$0(DirectoryFragment directoryFragment, SortModel sortModel, int i) {
        if ((i & 2) != 0) {
            directoryFragment.mActions.loadDocumentsForCurrentStack();
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mActivity = (BaseActivity) getActivity();
        View viewInflate = layoutInflater.inflate(R.layout.fragment_directory, viewGroup, false);
        this.mProgressBar = viewInflate.findViewById(R.id.progressbar);
        this.mRecView = (RecyclerView) viewInflate.findViewById(R.id.dir_list);
        this.mRecView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
                DirectoryFragment.this.cancelThumbnailTask(viewHolder.itemView);
            }
        });
        this.mRefreshLayout = (SwipeRefreshLayout) viewInflate.findViewById(R.id.refresh_layout);
        this.mRefreshLayout.setOnRefreshListener(this);
        this.mRecView.setItemAnimator(new DirectoryItemAnimator(this.mActivity));
        this.mInjector = this.mActivity.getInjector();
        this.mModel = this.mInjector.getModel();
        this.mModel.reset();
        this.mInjector.actions.registerDisplayStateChangedListener(this.mOnDisplayStateChanged);
        this.mClipper = DocumentsApplication.getDocumentClipper(getContext());
        if (this.mInjector.config.dragAndDropEnabled()) {
            this.mDragHoverListener = DragHoverListener.create(new DirectoryDragListener(new DragHost(this.mActivity, DocumentsApplication.getDragAndDropManager(this.mActivity), this.mInjector.selectionMgr, this.mInjector.actions, this.mActivity.getDisplayState(), this.mInjector.dialogs, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return DirectoryFragment.lambda$onCreateView$1(this.f$0, (View) obj);
                }
            }, new Lookup() {
                @Override
                public final Object lookup(Object obj) {
                    return this.f$0.getDocumentHolder((View) obj);
                }
            }, new Lookup() {
                @Override
                public final Object lookup(Object obj) {
                    return this.f$0.getDestination((View) obj);
                }
            })), this.mRecView);
        }
        this.mRecView.setOnDragListener(this.mDragHoverListener);
        return viewInflate;
    }

    public static boolean lambda$onCreateView$1(DirectoryFragment directoryFragment, View view) {
        return directoryFragment.getModelId(view) != null;
    }

    @Override
    public void onDestroyView() {
        this.mSelectionMgr.clearSelection();
        this.mInjector.actions.unregisterDisplayStateChangedListener(this.mOnDisplayStateChanged);
        int childCount = this.mRecView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            cancelThumbnailTask(this.mRecView.getChildAt(i));
        }
        this.mModel.removeUpdateListener(this.mModelUpdateListener);
        this.mModel.removeUpdateListener(this.mAdapter.getModelUpdateListener());
        if (this.mBandSelector != null) {
            this.mBandSelector.removeOnBandStartedListener(this.mBandSelectStartedCallback);
        }
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        DragStartListener dragStartListenerCreate;
        int i;
        super.onActivityCreated(bundle);
        this.mActivity = (BaseActivity) getActivity();
        this.mState = this.mActivity.getDisplayState();
        if (bundle == null) {
            bundle = getArguments();
        }
        this.mLocalState = new DirectoryState();
        this.mLocalState.restore(bundle);
        BaseActivity.RetainedState retainedState = this.mActivity.getRetainedState();
        if (retainedState != null && retainedState.hasSelection()) {
            this.mRestoredSelection = retainedState.selection;
            retainedState.selection = null;
        }
        this.mIconHelper = new IconHelper(this.mActivity, 2);
        this.mClipper = DocumentsApplication.getDocumentClipper(getContext());
        this.mAdapter = new DirectoryAddonsAdapter(this.mAdapterEnv, new ModelBackedDocumentsAdapter(this.mAdapterEnv, this.mIconHelper, this.mInjector.fileTypeLookup));
        this.mRecView.setAdapter(this.mAdapter);
        this.mLayout = new GridLayoutManager(getContext(), this.mColumnCount) {
            @Override
            public void onLayoutCompleted(RecyclerView.State state) {
                super.onLayoutCompleted(state);
                DirectoryFragment.this.mFocusManager.onLayoutCompleted();
            }
        };
        GridLayoutManager.SpanSizeLookup spanSizeLookupCreateSpanSizeLookup = this.mAdapter.createSpanSizeLookup();
        if (spanSizeLookupCreateSpanSizeLookup != null) {
            this.mLayout.setSpanSizeLookup(spanSizeLookupCreateSpanSizeLookup);
        }
        this.mRecView.setLayoutManager(this.mLayout);
        this.mModel.addUpdateListener(this.mAdapter.getModelUpdateListener());
        this.mModel.addUpdateListener(this.mModelUpdateListener);
        DocsSelectionPredicate docsSelectionPredicate = new DocsSelectionPredicate(this.mInjector.config, this.mState, this.mModel, this.mRecView);
        this.mSelectionMgr = this.mInjector.getSelectionManager(this.mAdapter, docsSelectionPredicate);
        this.mFocusManager = this.mInjector.getFocusManager(this.mRecView, this.mModel);
        this.mActions = this.mInjector.getActionHandler(this.mContentLock);
        this.mRecView.setAccessibilityDelegateCompat(new AccessibilityEventRouter(this.mRecView, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Boolean.valueOf(this.f$0.onAccessibilityClick((View) obj));
            }
        }));
        final Model model = this.mModel;
        Objects.requireNonNull(model);
        this.mSelectionMetadata = new SelectionMetadata(new Function() {
            @Override
            public final Object apply(Object obj) {
                return model.getItem((String) obj);
            }
        });
        this.mSelectionMgr.addObserver(this.mSelectionMetadata);
        this.mDetailsLookup = new DocsItemDetailsLookup(this.mRecView);
        GestureSelectionHelper gestureSelectionHelperCreate = GestureSelectionHelper.create(this.mSelectionMgr, this.mRecView, this.mContentLock, this.mDetailsLookup);
        if (this.mState.allowMultiple) {
            this.mBandSelector = new BandSelectionHelper(new DefaultBandHost(this.mRecView, R.drawable.band_select_overlay), this.mAdapter, new DocsStableIdProvider(this.mAdapter), this.mSelectionMgr, docsSelectionPredicate, new DefaultBandPredicate(this.mDetailsLookup), this.mContentLock);
            final FocusManager focusManager = this.mFocusManager;
            Objects.requireNonNull(focusManager);
            this.mBandSelectStartedCallback = new Runnable() {
                @Override
                public final void run() {
                    focusManager.clearFocus();
                }
            };
            this.mBandSelector.addOnBandStartedListener(this.mBandSelectStartedCallback);
        }
        if (this.mInjector.config.dragAndDropEnabled()) {
            IconHelper iconHelper = this.mIconHelper;
            Model model2 = this.mModel;
            SelectionHelper selectionHelper = this.mSelectionMgr;
            SelectionMetadata selectionMetadata = this.mSelectionMetadata;
            State state = this.mState;
            ItemDetailsLookup itemDetailsLookup = this.mDetailsLookup;
            Function function = new Function() {
                @Override
                public final Object apply(Object obj) {
                    return this.f$0.getModelId((View) obj);
                }
            };
            final RecyclerView recyclerView = this.mRecView;
            Objects.requireNonNull(recyclerView);
            dragStartListenerCreate = DragStartListener.create(iconHelper, model2, selectionHelper, selectionMetadata, state, itemDetailsLookup, function, new DragStartListener.ViewFinder() {
                @Override
                public final View findView(float f, float f2) {
                    return recyclerView.findChildViewUnder(f, f2);
                }
            }, DocumentsApplication.getDragAndDropManager(this.mActivity));
        } else {
            dragStartListenerCreate = DragStartListener.DUMMY;
        }
        final DragStartListener dragStartListener = dragStartListenerCreate;
        InputHandlers inputHandlers = new InputHandlers(this.mActions, this.mSelectionMgr, docsSelectionPredicate, this.mDetailsLookup, this.mFocusManager, this.mRecView, this.mState);
        MouseInputHandler mouseInputHandlerCreateMouseHandler = inputHandlers.createMouseHandler(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.onContextMenuClick((MotionEvent) obj);
            }
        });
        GestureRouter gestureRouter = new GestureRouter(inputHandlers.createTouchHandler(gestureSelectionHelperCreate, dragStartListener));
        gestureRouter.register(3, mouseInputHandlerCreateMouseHandler);
        this.mKeyListener = inputHandlers.createKeyHandler();
        if (Build.IS_DEBUGGABLE) {
            new ScaleHelper(getContext(), this.mInjector.features, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.scaleLayout(((Float) obj).floatValue());
                }
            }).attach(this.mRecView);
        }
        final SwipeRefreshLayout swipeRefreshLayout = this.mRefreshLayout;
        Objects.requireNonNull(swipeRefreshLayout);
        new RefreshHelper(new BooleanConsumer() {
            @Override
            public final void accept(boolean z) {
                swipeRefreshLayout.setEnabled(z);
            }
        }).attach(this.mRecView);
        TouchEventRouter touchEventRouter = new TouchEventRouter(new GestureDetector(getContext(), gestureRouter), gestureSelectionHelperCreate);
        ItemDetailsLookup itemDetailsLookup2 = this.mDetailsLookup;
        Objects.requireNonNull(dragStartListener);
        touchEventRouter.register(3, new MouseDragEventInterceptor(itemDetailsLookup2, new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return dragStartListener.onMouseDragEvent((MotionEvent) obj);
            }
        }, this.mBandSelector));
        this.mRecView.addOnItemTouchListener(touchEventRouter);
        this.mActionModeController = this.mInjector.getActionModeController(this.mSelectionMetadata, new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.handleMenuItemClick((MenuItem) obj);
            }
        });
        if (this.mActionModeController != null) {
            Log.d("DirectoryFragment", "mActionModeController is set and callback added");
            this.mSelectionMgr.addObserver(this.mActionModeController);
        }
        this.mIconHelper.setThumbnailsEnabled(!(((ActivityManager) this.mActivity.getSystemService("activity")).isLowRamDevice() && this.mState.stack.isRecents()));
        boolean z = this.mLocalState.mDocument == null || this.mLocalState.mDocument.prefersSortByLastModified();
        SortModel sortModel = this.mState.sortModel;
        if (z) {
            i = R.id.date;
        } else {
            i = android.R.id.title;
        }
        sortModel.setDefaultDimension(i);
        this.mActions.loadDocumentsForCurrentStack();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mState.sortModel.addListener(this.mSortListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mState.sortModel.removeListener(this.mSortListener);
        SparseArray<Parcelable> sparseArray = new SparseArray<>();
        getView().saveHierarchyState(sparseArray);
        this.mState.dirConfigs.put(this.mLocalState.getConfigKey(), sparseArray);
    }

    public void retainState(BaseActivity.RetainedState retainedState) {
        retainedState.selection = new Selection();
        this.mSelectionMgr.copySelection(retainedState.selection);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mLocalState.save(bundle);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
        MenuInflater menuInflater = getActivity().getMenuInflater();
        if (getModelId(view) == null) {
            this.mInjector.menuManager.inflateContextMenuForContainer(contextMenu, menuInflater);
        } else {
            this.mInjector.menuManager.inflateContextMenuForDocs(contextMenu, menuInflater, this.mSelectionMetadata);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        return handleMenuItemClick(menuItem);
    }

    private void onCopyDestinationPicked(int i, Intent intent) {
        FileOperation fileOperationClaimPendingOperation = this.mLocalState.claimPendingOperation();
        if (i == 0 || intent == null) {
            fileOperationClaimPendingOperation.dispose();
            return;
        }
        fileOperationClaimPendingOperation.setDestination((DocumentStack) intent.getParcelableExtra("com.android.documentsui.STACK"));
        String strCreateJobId = FileOperations.createJobId();
        this.mInjector.dialogs.showProgressDialog(strCreateJobId, fileOperationClaimPendingOperation);
        BaseActivity baseActivity = this.mActivity;
        DialogController dialogController = this.mInjector.dialogs;
        Objects.requireNonNull(dialogController);
        FileOperations.start(baseActivity, fileOperationClaimPendingOperation, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController), strCreateJobId);
    }

    protected boolean onContextMenuClick(MotionEvent motionEvent) {
        if (this.mDetailsLookup.overStableItem(motionEvent)) {
            this.mInjector.menuManager.showContextMenu(this, this.mRecView.getChildViewHolder(this.mRecView.findChildViewUnder(motionEvent.getX(), motionEvent.getY())).itemView, motionEvent.getX() - r0.getLeft(), motionEvent.getY() - r0.getTop());
            return true;
        }
        this.mInjector.menuManager.showContextMenu(this, this.mRecView, motionEvent.getX(), motionEvent.getY());
        return true;
    }

    public void onViewModeChanged() {
        onDisplayStateChanged();
    }

    private void onDisplayStateChanged() {
        updateLayout(this.mState.derivedMode);
        this.mRecView.setAdapter(this.mAdapter);
    }

    private void updateLayout(int i) {
        this.mMode = i;
        int directoryPadding = getDirectoryPadding(i);
        this.mRecView.setPadding(directoryPadding, directoryPadding, directoryPadding, directoryPadding);
        this.mColumnCount = calculateColumnCount(i);
        if (this.mLayout != null) {
            this.mLayout.setSpanCount(this.mColumnCount);
        }
        this.mRecView.requestLayout();
        if (this.mBandSelector != null) {
            this.mBandSelector.reset();
        }
        this.mIconHelper.setViewMode(i);
    }

    private void scaleLayout(float f) {
        if (SharedMinimal.VERBOSE) {
            Log.v("DirectoryFragment", "Handling scale event: " + f + ", existing scale: " + this.mLiveScale);
        }
        if (this.mMode == 2) {
            float fraction = getFraction(R.fraction.grid_scale_min);
            float fraction2 = getFraction(R.fraction.grid_scale_max);
            float f2 = this.mLiveScale * f;
            if (SharedMinimal.VERBOSE) {
                Log.v("DirectoryFragment", "Next scale " + f2 + ", Min/max scale " + fraction + "/" + fraction2);
            }
            if (f2 > fraction && f2 < fraction2) {
                if (SharedMinimal.DEBUG) {
                    Log.d("DirectoryFragment", "Updating grid scale: " + f);
                }
                this.mLiveScale = f2;
                updateLayout(this.mMode);
                return;
            }
            return;
        }
        if (SharedMinimal.DEBUG) {
            Log.d("DirectoryFragment", "List mode, ignoring scale: " + f);
        }
        this.mLiveScale = 1.0f;
    }

    private int calculateColumnCount(int i) {
        if (i == 1) {
            return 1;
        }
        int scaledSize = getScaledSize(R.dimen.grid_width);
        int scaledSize2 = getScaledSize(R.dimen.grid_item_margin) * 2;
        return Math.max(1, Math.round(Math.max(2, (this.mRecView.getWidth() - ((int) ((this.mRecView.getPaddingLeft() + this.mRecView.getPaddingRight()) * this.mLiveScale))) / (scaledSize + scaledSize2)) / this.mLiveScale));
    }

    private float getFraction(int i) {
        return getResources().getFraction(i, 1, 0);
    }

    private int getScaledSize(int i) {
        return (int) (getResources().getDimensionPixelSize(i) * this.mLiveScale);
    }

    private int getDirectoryPadding(int i) {
        switch (i) {
            case 1:
                return getResources().getDimensionPixelSize(R.dimen.list_container_padding);
            case 2:
                return getResources().getDimensionPixelSize(R.dimen.grid_container_padding);
            default:
                throw new IllegalArgumentException("Unsupported layout mode: " + i);
        }
    }

    private boolean handleMenuItemClick(MenuItem menuItem) {
        DocumentInfo currentDirectory;
        Selection selection = new Selection();
        this.mSelectionMgr.copySelection(selection);
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.action_menu_compress:
                transferDocuments(selection, this.mState.stack, 3);
                this.mActionModeController.finishActionMode();
                return true;
            case R.id.action_menu_copy_to:
                if (!selection.isEmpty()) {
                    transferDocuments(selection, null, 1);
                }
                this.mActionModeController.finishActionMode();
                return true;
            case R.id.action_menu_delete:
                break;
            default:
                switch (itemId) {
                    case R.id.action_menu_extract_to:
                        transferDocuments(selection, null, 2);
                        this.mActionModeController.finishActionMode();
                        return true;
                    case R.id.action_menu_inspect:
                        this.mActionModeController.finishActionMode();
                        if (selection.isEmpty()) {
                            currentDirectory = this.mActivity.getCurrentDirectory();
                        } else {
                            currentDirectory = this.mModel.getDocuments(selection).get(0);
                        }
                        this.mActions.showInspector(currentDirectory);
                        return true;
                    case R.id.action_menu_move_to:
                        if (this.mModel.hasDocuments(selection, DocumentFilters.NOT_MOVABLE)) {
                            this.mInjector.dialogs.showOperationUnsupported();
                            return true;
                        }
                        this.mActionModeController.finishActionMode();
                        transferDocuments(selection, null, 4);
                        return true;
                    case R.id.action_menu_open:
                        openDocuments(selection);
                        this.mActionModeController.finishActionMode();
                        return true;
                    case R.id.action_menu_open_with:
                        showChooserForDoc(selection);
                        return true;
                    default:
                        switch (itemId) {
                            case R.id.action_menu_rename:
                                this.mActionModeController.finishActionMode();
                                renameDocuments(selection);
                                return true;
                            case R.id.action_menu_select_all:
                                this.mActions.selectAllFiles();
                                return true;
                            case R.id.action_menu_share:
                                if (!selection.isEmpty()) {
                                    this.mActions.shareSelectedDocuments();
                                }
                                return true;
                            default:
                                switch (itemId) {
                                    case R.id.dir_menu_copy_to_clipboard:
                                        this.mActions.copyToClipboard();
                                        return true;
                                    case R.id.dir_menu_create_dir:
                                        this.mActions.showCreateDirectoryDialog();
                                        return true;
                                    case R.id.dir_menu_cut_to_clipboard:
                                        this.mActions.cutToClipboard();
                                        return true;
                                    case R.id.dir_menu_delete:
                                        break;
                                    case R.id.dir_menu_inspect:
                                        break;
                                    case R.id.dir_menu_open:
                                        break;
                                    case R.id.dir_menu_open_in_new_window:
                                        this.mActions.openSelectedInNewWindow();
                                        return true;
                                    case R.id.dir_menu_open_with:
                                        break;
                                    case R.id.dir_menu_paste_from_clipboard:
                                        pasteFromClipboard();
                                        return true;
                                    case R.id.dir_menu_paste_into_folder:
                                        pasteIntoFolder();
                                        return true;
                                    case R.id.dir_menu_rename:
                                        break;
                                    case R.id.dir_menu_select_all:
                                        break;
                                    case R.id.dir_menu_share:
                                        break;
                                    case R.id.dir_menu_view_in_owner:
                                        this.mActions.viewInOwner();
                                        return true;
                                    default:
                                        if (SharedMinimal.DEBUG) {
                                            Log.d("DirectoryFragment", "Unhandled menu item selected: " + menuItem);
                                        }
                                        return false;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
        this.mActions.deleteSelectedDocuments();
        return true;
    }

    private boolean onAccessibilityClick(View view) {
        this.mActions.openItem(getDocumentHolder(view).getItemDetails(), 2, 1);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAdapter.notifyDataSetChanged();
    }

    private void cancelThumbnailTask(View view) {
        ImageView imageView = (ImageView) view.findViewById(R.id.icon_thumb);
        if (imageView != null) {
            this.mIconHelper.stopLoading(imageView);
        }
    }

    private void openDocuments(Selection selection) {
        Metrics.logUserAction(getContext(), 18);
        List<DocumentInfo> documents = this.mModel.getDocuments(selection);
        if (documents.size() > 1) {
            this.mActivity.onDocumentsPicked(documents);
            return;
        }
        if (documents.size() == 1) {
            this.mActivity.onDocumentPicked(documents.get(0));
            return;
        }
        Log.d("DirectoryFragment", "openDocuments list size is invalid = " + documents.size());
    }

    private void showChooserForDoc(Selection selection) {
        Metrics.logUserAction(getContext(), 18);
        DocumentInfo documentInfoFromDirectoryCursor = DocumentInfo.fromDirectoryCursor(this.mModel.getItem(selection.iterator().next()));
        if (documentInfoFromDirectoryCursor.mimeType.equals("application/vnd.android.package-archive")) {
            showToast(R.string.toast_no_application);
        } else {
            this.mActions.showChooserForDoc(documentInfoFromDirectoryCursor);
        }
    }

    private void transferDocuments(Selection selection, DocumentStack documentStack, int i) {
        int i2;
        switch (i) {
            case 1:
                Metrics.logUserAction(getContext(), 11);
                break;
            case 2:
                Metrics.logUserAction(getContext(), 28);
                break;
            case 3:
                Metrics.logUserAction(getContext(), 27);
                break;
            case 4:
                Metrics.logUserAction(getContext(), 12);
                break;
        }
        try {
            ClipStore clipStore = DocumentsApplication.getClipStore(getContext());
            final Model model = this.mModel;
            Objects.requireNonNull(model);
            UrisSupplier urisSupplierCreate = UrisSupplier.create(selection, new Function() {
                @Override
                public final Object apply(Object obj) {
                    return model.getItemUri((String) obj);
                }
            }, clipStore);
            DocumentInfo currentDirectory = this.mActivity.getCurrentDirectory();
            FileOperation fileOperationBuild = new FileOperation.Builder().withOpType(i).withSrcParent(currentDirectory == null ? null : currentDirectory.derivedUri).withSrcs(urisSupplierCreate).build();
            if (documentStack != null) {
                fileOperationBuild.setDestination(documentStack);
                String strCreateJobId = FileOperations.createJobId();
                this.mInjector.dialogs.showProgressDialog(strCreateJobId, fileOperationBuild);
                BaseActivity baseActivity = this.mActivity;
                DialogController dialogController = this.mInjector.dialogs;
                Objects.requireNonNull(dialogController);
                FileOperations.start(baseActivity, fileOperationBuild, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController), strCreateJobId);
                return;
            }
            this.mLocalState.mPendingOperation = fileOperationBuild;
            Intent intent = new Intent("com.android.documentsui.PICK_COPY_DESTINATION", Uri.EMPTY, getActivity(), PickActivity.class);
            switch (i) {
                case 1:
                    i2 = R.string.menu_copy;
                    break;
                case 2:
                    i2 = R.string.menu_extract;
                    break;
                case 3:
                    i2 = R.string.menu_compress;
                    break;
                case 4:
                    i2 = R.string.menu_move;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown mode: " + i);
            }
            intent.putExtra("android.provider.extra.PROMPT", getResources().getString(i2));
            List<DocumentInfo> documents = this.mModel.getDocuments(selection);
            for (DocumentInfo documentInfo : documents) {
                if (documentInfo.isDrm && documentInfo.drmMethod >= 0) {
                    if (i == 1) {
                        showToast(R.string.drm_file_cannot_copy);
                    } else if (i == 4) {
                        showToast(R.string.drm_file_cannot_move);
                    }
                    Log.d("DirectoryFragment", "Choose drm file '" + documentInfo.displayName + "' is " + documentInfo.drmMethod + " cann't shared!");
                    return;
                }
            }
            intent.putExtra("com.android.documentsui.DIRECTORY_COPY", hasDirectory(documents));
            intent.putExtra("com.android.documentsui.OPERATION_TYPE", i);
            startActivityForResult(intent, 1);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create uri supplier.", e);
        }
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            onCopyDestinationPicked(i2, intent);
            return;
        }
        throw new UnsupportedOperationException("Unknown request code: " + i);
    }

    private static boolean hasDirectory(List<DocumentInfo> list) {
        Iterator<DocumentInfo> it = list.iterator();
        while (it.hasNext()) {
            if ("vnd.android.document/directory".equals(it.next().mimeType)) {
                return true;
            }
        }
        return false;
    }

    private void renameDocuments(Selection selection) {
        Metrics.logUserAction(getContext(), 14);
        Log.d("DirectoryFragment", "renameDocuments selected.size = " + selection.size());
        if (selection.size() != 1) {
            return;
        }
        RenameDocumentFragment.show(getChildFragmentManager(), this.mModel.getDocuments(selection).get(0));
    }

    public void pasteFromClipboard() {
        Metrics.logUserAction(getContext(), 22);
        DocumentClipper documentClipper = this.mClipper;
        DocumentStack documentStack = this.mState.stack;
        DialogController dialogController = this.mInjector.dialogs;
        Objects.requireNonNull(dialogController);
        documentClipper.copyFromClipboard(documentStack, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController));
        getActivity().invalidateOptionsMenu();
    }

    public void pasteIntoFolder() {
        String next = this.mSelectionMgr.getSelection().iterator().next();
        Cursor item = this.mModel.getItem(next);
        if (item == null) {
            Log.w("DirectoryFragment", "Invalid destination. Can't obtain cursor for modelId: " + next);
            return;
        }
        DocumentInfo documentInfoFromDirectoryCursor = DocumentInfo.fromDirectoryCursor(item);
        DocumentClipper documentClipper = this.mClipper;
        DocumentStack documentStack = this.mState.stack;
        DialogController dialogController = this.mInjector.dialogs;
        Objects.requireNonNull(dialogController);
        documentClipper.copyFromClipboard(documentInfoFromDirectoryCursor, documentStack, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController));
        getActivity().invalidateOptionsMenu();
    }

    private void setupDragAndDropOnDocumentView(View view, Cursor cursor) {
        if ("vnd.android.document/directory".equals(DocumentInfo.getCursorString(cursor, "mime_type"))) {
            view.setOnDragListener(this.mDragHoverListener);
        }
    }

    private DocumentInfo getDestination(View view) {
        String modelId = getModelId(view);
        if (modelId != null) {
            Cursor item = this.mModel.getItem(modelId);
            if (item == null) {
                Log.w("DirectoryFragment", "Invalid destination. Can't obtain cursor for modelId: " + modelId);
                return null;
            }
            return DocumentInfo.fromDirectoryCursor(item);
        }
        if (view != this.mRecView) {
            return null;
        }
        return this.mActivity.getCurrentDirectory();
    }

    private String getModelId(View view) {
        View viewFindContainingItemView = this.mRecView.findContainingItemView(view);
        if (viewFindContainingItemView != null) {
            RecyclerView.ViewHolder childViewHolder = this.mRecView.getChildViewHolder(viewFindContainingItemView);
            if (childViewHolder instanceof DocumentHolder) {
                return ((DocumentHolder) childViewHolder).getModelId();
            }
            return null;
        }
        return null;
    }

    private DocumentHolder getDocumentHolder(View view) {
        RecyclerView.ViewHolder childViewHolder = this.mRecView.getChildViewHolder(view);
        if (childViewHolder instanceof DocumentHolder) {
            return (DocumentHolder) childViewHolder;
        }
        return null;
    }

    public static void showDirectory(FragmentManager fragmentManager, RootInfo rootInfo, DocumentInfo documentInfo, int i) {
        if (SharedMinimal.DEBUG) {
            Log.d("DirectoryFragment", "Showing directory: " + DocumentInfo.debugString(documentInfo));
        }
        create(fragmentManager, rootInfo, documentInfo, i);
    }

    public static void showRecentsOpen(FragmentManager fragmentManager, int i) {
        create(fragmentManager, null, null, i);
    }

    public static void create(FragmentManager fragmentManager, RootInfo rootInfo, DocumentInfo documentInfo, int i) {
        if (SharedMinimal.DEBUG) {
            if (documentInfo == null) {
                Log.d("DirectoryFragment", "Creating new fragment null directory");
            } else {
                Log.d("DirectoryFragment", "Creating new fragment for directory: " + DocumentInfo.debugString(documentInfo));
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("root", rootInfo);
        bundle.putParcelable("document", documentInfo);
        bundle.putParcelable("selection", new Selection());
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        AnimationView.setupAnimations(fragmentTransactionBeginTransaction, i, bundle);
        DirectoryFragment directoryFragment = new DirectoryFragment();
        directoryFragment.setArguments(bundle);
        fragmentTransactionBeginTransaction.replace(getFragmentId(), directoryFragment);
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public static DirectoryFragment get(FragmentManager fragmentManager) {
        Fragment fragmentFindFragmentById = fragmentManager.findFragmentById(getFragmentId());
        if (fragmentFindFragmentById instanceof DirectoryFragment) {
            return (DirectoryFragment) fragmentFindFragmentById;
        }
        return null;
    }

    private static int getFragmentId() {
        return R.id.container_directory;
    }

    @Override
    public void onRefresh() {
        ThumbnailCache thumbnailCache = DocumentsApplication.getThumbnailCache(getContext());
        String[] modelIds = this.mModel.getModelIds();
        int iMin = Math.min(modelIds.length, 100);
        for (int i = 0; i < iMin; i++) {
            thumbnailCache.removeUri(this.mModel.getItemUri(modelIds[i]));
        }
        this.mActions.refreshDocument(this.mActivity.getCurrentDirectory(), new BooleanConsumer() {
            @Override
            public final void accept(boolean z) {
                DirectoryFragment.lambda$onRefresh$3(this.f$0, z);
            }
        });
    }

    public static void lambda$onRefresh$3(DirectoryFragment directoryFragment, boolean z) {
        if (z) {
            directoryFragment.mRefreshLayout.setRefreshing(false);
        } else {
            directoryFragment.mActions.loadDocumentsForCurrentStack();
        }
    }

    private final class ModelUpdateListener implements EventListener<Model.Update> {
        private ModelUpdateListener() {
        }

        @Override
        public void accept(Model.Update update) {
            if (SharedMinimal.DEBUG) {
                Log.d("DirectoryFragment", "Received model update. Loading=" + DirectoryFragment.this.mModel.isLoading());
            }
            DirectoryFragment.this.mProgressBar.setVisibility(DirectoryFragment.this.mModel.isLoading() ? 0 : 8);
            DirectoryFragment.this.updateLayout(DirectoryFragment.this.mState.derivedMode);
            DirectoryFragment.this.mAdapter.notifyDataSetChanged();
            if (DirectoryFragment.this.mRestoredSelection != null) {
                DirectoryFragment.this.mSelectionMgr.restoreSelection(DirectoryFragment.this.mRestoredSelection);
                DirectoryFragment.this.mRestoredSelection = null;
            }
            SparseArray<Parcelable> sparseArrayRemove = DirectoryFragment.this.mState.dirConfigs.remove(DirectoryFragment.this.mLocalState.getConfigKey());
            SortDimension dimensionById = DirectoryFragment.this.mState.sortModel.getDimensionById(DirectoryFragment.this.mState.sortModel.getSortedDimensionId());
            if (sparseArrayRemove == null || DirectoryFragment.this.getArguments().getBoolean("ignoreState", false)) {
                if (DirectoryFragment.this.mLocalState.mLastSortDimensionId != dimensionById.getId() || DirectoryFragment.this.mLocalState.mLastSortDimensionId == 0 || DirectoryFragment.this.mLocalState.mLastSortDirection != dimensionById.getSortDirection()) {
                    DirectoryFragment.this.mRecView.smoothScrollToPosition(0);
                }
            } else {
                DirectoryFragment.this.getView().restoreHierarchyState(sparseArrayRemove);
            }
            DirectoryFragment.this.mLocalState.mLastSortDimensionId = dimensionById.getId();
            DirectoryFragment.this.mLocalState.mLastSortDirection = dimensionById.getSortDirection();
            if (DirectoryFragment.this.mRefreshLayout.isRefreshing()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public final void run() {
                        DirectoryFragment.this.mRefreshLayout.setRefreshing(false);
                    }
                }, 500L);
            }
            try {
                if (!DirectoryFragment.this.mModel.isLoading()) {
                    DirectoryFragment.this.mActivity.notifyDirectoryLoaded(DirectoryFragment.this.mModel.doc != null ? DirectoryFragment.this.mModel.doc.derivedUri : null);
                }
            } catch (NullPointerException e) {
                Log.e("DirectoryFragment", "Activity is null");
                e.printStackTrace();
            }
        }
    }

    private final class AdapterEnvironment implements DocumentsAdapter.Environment {
        private AdapterEnvironment() {
        }

        @Override
        public Features getFeatures() {
            return DirectoryFragment.this.mInjector.features;
        }

        @Override
        public Context getContext() {
            return DirectoryFragment.this.mActivity;
        }

        @Override
        public State getDisplayState() {
            return DirectoryFragment.this.mState;
        }

        @Override
        public boolean isInSearchMode() {
            return DirectoryFragment.this.mInjector.searchManager.isSearching();
        }

        @Override
        public Model getModel() {
            return DirectoryFragment.this.mModel;
        }

        @Override
        public int getColumnCount() {
            return DirectoryFragment.this.mColumnCount;
        }

        @Override
        public boolean isSelected(String str) {
            return DirectoryFragment.this.mSelectionMgr.isSelected(str);
        }

        @Override
        public boolean isDocumentEnabled(String str, int i) {
            return DirectoryFragment.this.mInjector.config.isDocumentEnabled(str, i, DirectoryFragment.this.mState);
        }

        @Override
        public void initDocumentHolder(DocumentHolder documentHolder) {
            documentHolder.addKeyEventListener(DirectoryFragment.this.mKeyListener);
            documentHolder.itemView.setOnFocusChangeListener(DirectoryFragment.this.mFocusManager);
        }

        @Override
        public void onBindDocumentHolder(DocumentHolder documentHolder, Cursor cursor) {
            DirectoryFragment.this.setupDragAndDropOnDocumentView(documentHolder.itemView, cursor);
        }

        @Override
        public ActionHandler getActionHandler() {
            return DirectoryFragment.this.mActions;
        }
    }

    private void showToast(int i) {
        ((BaseActivity) getActivity()).showToast(i);
    }
}
