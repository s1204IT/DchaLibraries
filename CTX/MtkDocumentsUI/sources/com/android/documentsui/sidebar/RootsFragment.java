package com.android.documentsui.sidebar;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.BaseActivity;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.Injector;
import com.android.documentsui.ItemDragListener;
import com.android.documentsui.R;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Events;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.Shared;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.roots.ProvidersCache;
import com.android.documentsui.roots.RootsLoader;
import com.android.documentsui.sidebar.RootsFragment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RootsFragment extends Fragment {
    static final boolean $assertionsDisabled = false;
    private ActionHandler mActionHandler;
    private RootsAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<Collection<RootInfo>> mCallbacks;
    private View.OnDragListener mDragListener;
    private Injector<?> mInjector;
    private final AdapterView.OnItemClickListener mItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            RootsFragment.this.mAdapter.getItem(i).open();
            RootsFragment.this.getBaseActivity().setRootsDrawerOpen(false);
        }
    };
    private final AdapterView.OnItemLongClickListener mItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long j) {
            return RootsFragment.this.mAdapter.getItem(i).showAppDetails();
        }
    };
    private ListView mList;

    @FunctionalInterface
    interface RootUpdater {
        void updateDocInfoForRoot(DocumentInfo documentInfo);
    }

    public static RootsFragment show(FragmentManager fragmentManager, Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("includeApps", intent);
        RootsFragment rootsFragment = new RootsFragment();
        rootsFragment.setArguments(bundle);
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.container_roots, rootsFragment);
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        return rootsFragment;
    }

    public static RootsFragment get(FragmentManager fragmentManager) {
        return (RootsFragment) fragmentManager.findFragmentById(R.id.container_roots);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mInjector = getBaseActivity().getInjector();
        View viewInflate = layoutInflater.inflate(R.layout.fragment_roots, viewGroup, false);
        this.mList = (ListView) viewInflate.findViewById(R.id.roots_list);
        this.mList.setOnItemClickListener(this.mItemListener);
        this.mList.setOnGenericMotionListener(new AnonymousClass3());
        this.mList.setChoiceMode(1);
        return viewInflate;
    }

    class AnonymousClass3 implements View.OnGenericMotionListener {
        AnonymousClass3() {
        }

        @Override
        public boolean onGenericMotion(final View view, MotionEvent motionEvent) {
            if (Events.isMouseEvent(motionEvent) && motionEvent.getButtonState() == 2) {
                final int x = (int) motionEvent.getX();
                final int y = (int) motionEvent.getY();
                return RootsFragment.this.onRightClick(view, x, y, new Runnable() {
                    @Override
                    public final void run() {
                        RootsFragment.AnonymousClass3 anonymousClass3 = this.f$0;
                        RootsFragment.this.mInjector.menuManager.showContextMenu(RootsFragment.this, view, x, y);
                    }
                });
            }
            return false;
        }
    }

    private boolean onRightClick(View view, int i, int i2, final Runnable runnable) {
        Item item = this.mAdapter.getItem(this.mList.pointToPosition(i, i2));
        if (!(item instanceof RootItem)) {
            return false;
        }
        final RootItem rootItem = (RootItem) item;
        if (!rootItem.root.supportsCreate()) {
            return false;
        }
        getRootDocument(rootItem, new RootUpdater() {
            @Override
            public final void updateDocInfoForRoot(DocumentInfo documentInfo) {
                RootsFragment.lambda$onRightClick$0(rootItem, runnable, documentInfo);
            }
        });
        return true;
    }

    static void lambda$onRightClick$0(RootItem rootItem, Runnable runnable, DocumentInfo documentInfo) {
        rootItem.docInfo = documentInfo;
        runnable.run();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        final BaseActivity baseActivity = getBaseActivity();
        final ProvidersCache providersCache = DocumentsApplication.getProvidersCache(baseActivity);
        final State displayState = baseActivity.getDisplayState();
        this.mActionHandler = this.mInjector.actions;
        if (this.mInjector.config.dragAndDropEnabled()) {
            this.mDragListener = new ItemDragListener<DragHost>(new DragHost(baseActivity, DocumentsApplication.getDragAndDropManager(baseActivity), new Lookup() {
                @Override
                public final Object lookup(Object obj) {
                    return this.f$0.getItem((View) obj);
                }
            }, this.mActionHandler)) {
                static final boolean $assertionsDisabled = false;

                @Override
                public boolean handleDropEventChecked(View view, DragEvent dragEvent) {
                    return RootsFragment.this.getItem(view).dropOn(dragEvent);
                }
            };
        }
        this.mCallbacks = new LoaderManager.LoaderCallbacks<Collection<RootInfo>>() {
            @Override
            public Loader<Collection<RootInfo>> onCreateLoader(int i, Bundle bundle2) {
                return new RootsLoader(baseActivity, providersCache, displayState);
            }

            @Override
            public void onLoadFinished(Loader<Collection<RootInfo>> loader, Collection<RootInfo> collection) {
                if (!RootsFragment.this.isAdded()) {
                    return;
                }
                RootsFragment.this.mAdapter = new RootsAdapter(baseActivity, RootsFragment.this.sortLoadResult(collection, baseActivity.getIntent().getBooleanExtra("android.provider.extra.EXCLUDE_SELF", false) ? baseActivity.getCallingPackage() : null, (Intent) RootsFragment.this.getArguments().getParcelable("includeApps")), RootsFragment.this.mDragListener);
                RootsFragment.this.mList.setAdapter((ListAdapter) RootsFragment.this.mAdapter);
                if (RootsFragment.this.mInjector.features != null && RootsFragment.this.mInjector.features.isLauncherEnabled()) {
                    RootsFragment.this.mInjector.shortcutsUpdater.accept(collection);
                } else if (SharedMinimal.DEBUG) {
                    Log.d("RootsFragment", "Launcher is disabled");
                }
                RootsFragment.this.onCurrentRootChanged();
            }

            @Override
            public void onLoaderReset(Loader<Collection<RootInfo>> loader) {
                RootsFragment.this.mAdapter = null;
                RootsFragment.this.mList.setAdapter((ListAdapter) null);
            }
        };
    }

    private List<Item> sortLoadResult(Collection<RootInfo> collection, String str, Intent intent) {
        List<Item> arrayList = new ArrayList<>();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        for (RootInfo rootInfo : collection) {
            RootItem rootItem = new RootItem(rootInfo, this.mActionHandler);
            Activity activity = getActivity();
            if (!rootInfo.isHome() || Shared.shouldShowDocumentsRoot(activity)) {
                if (rootInfo.isLibrary()) {
                    arrayList2.add(rootItem);
                } else {
                    arrayList3.add(rootItem);
                }
            }
        }
        RootComparator rootComparator = new RootComparator();
        Collections.sort(arrayList2, rootComparator);
        Collections.sort(arrayList3, rootComparator);
        if (SharedMinimal.VERBOSE) {
            Log.v("RootsFragment", "Adding library roots: " + arrayList2);
        }
        arrayList.addAll(arrayList2);
        if (!arrayList2.isEmpty() && !arrayList3.isEmpty()) {
            arrayList.add(new SpacerItem());
        }
        if (SharedMinimal.VERBOSE) {
            Log.v("RootsFragment", "Adding plain roots: " + arrayList2);
        }
        arrayList.addAll(arrayList3);
        if (intent != null) {
            includeHandlerApps(intent, str, arrayList);
        }
        return arrayList;
    }

    private void includeHandlerApps(Intent intent, String str, List<Item> list) {
        if (SharedMinimal.VERBOSE) {
            Log.v("RootsFragment", "Adding handler apps for intent: " + intent);
        }
        Context context = getContext();
        List<ResolveInfo> listQueryIntentActivities = context.getPackageManager().queryIntentActivities(intent, 65536);
        ArrayList arrayList = new ArrayList();
        for (ResolveInfo resolveInfo : listQueryIntentActivities) {
            if (!context.getPackageName().equals(resolveInfo.activityInfo.packageName) && !TextUtils.equals(str, resolveInfo.activityInfo.packageName)) {
                AppItem appItem = new AppItem(resolveInfo, this.mActionHandler);
                if (SharedMinimal.VERBOSE) {
                    Log.v("RootsFragment", "Adding handler app: " + appItem);
                }
                arrayList.add(appItem);
            }
        }
        if (arrayList.size() > 0) {
            list.add(new SpacerItem());
            list.addAll(arrayList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        onDisplayStateChanged();
    }

    public void onDisplayStateChanged() {
        if (((BaseActivity) getActivity()).getDisplayState().action == 5) {
            this.mList.setOnItemLongClickListener(this.mItemLongClickListener);
        } else {
            this.mList.setOnItemLongClickListener(null);
            this.mList.setLongClickable(false);
        }
        getLoaderManager().restartLoader(2, null, this.mCallbacks);
    }

    public void onCurrentRootChanged() {
        if (this.mAdapter == null) {
            return;
        }
        RootInfo currentRoot = ((BaseActivity) getActivity()).getCurrentRoot();
        for (int i = 0; i < this.mAdapter.getCount(); i++) {
            Item item = this.mAdapter.getItem(i);
            if ((item instanceof RootItem) && Objects.equals(((RootItem) item).root, currentRoot)) {
                this.mList.setItemChecked(i, true);
                return;
            }
        }
    }

    public boolean requestFocus() {
        return this.mList.requestFocus();
    }

    private BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
        this.mAdapter.getItem(((AdapterView.AdapterContextMenuInfo) contextMenuInfo).position).createContextMenu(contextMenu, getBaseActivity().getMenuInflater(), this.mInjector.menuManager);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        if (adapterContextMenuInfo == null) {
            return false;
        }
        RootItem rootItem = (RootItem) this.mAdapter.getItem(adapterContextMenuInfo.position);
        switch (menuItem.getItemId()) {
            case R.id.root_menu_eject_root:
                ejectClicked(adapterContextMenuInfo.targetView.findViewById(R.id.eject_icon), rootItem.root, this.mActionHandler);
                return true;
            case R.id.root_menu_open_in_new_window:
                this.mActionHandler.openInNewWindow(new DocumentStack(rootItem.root, new DocumentInfo[0]));
                return true;
            case R.id.root_menu_paste_into_folder:
                this.mActionHandler.pasteIntoFolder(rootItem.root);
                return true;
            case R.id.root_menu_settings:
                this.mActionHandler.openSettings(rootItem.root);
                return true;
            default:
                if (SharedMinimal.DEBUG) {
                    Log.d("RootsFragment", "Unhandled menu item selected: " + menuItem);
                }
                return false;
        }
    }

    private void getRootDocument(RootItem rootItem, final RootUpdater rootUpdater) {
        this.mActionHandler.getRootDocument(rootItem.root, 500, new Consumer() {
            @Override
            public final void accept(Object obj) {
                rootUpdater.updateDocInfoForRoot((DocumentInfo) obj);
            }
        });
    }

    private Item getItem(View view) {
        return this.mAdapter.getItem(((Integer) view.getTag(R.id.item_position_tag)).intValue());
    }

    static void ejectClicked(final View view, final RootInfo rootInfo, ActionHandler actionHandler) {
        view.setEnabled(false);
        rootInfo.ejecting = true;
        actionHandler.ejectRoot(rootInfo, new BooleanConsumer() {
            @Override
            public void accept(boolean z) {
                rootInfo.ejecting = false;
                if (view.getVisibility() == 0) {
                    view.setEnabled(!z);
                }
            }
        });
    }

    private static class RootComparator implements Comparator<RootItem> {
        private RootComparator() {
        }

        @Override
        public int compare(RootItem rootItem, RootItem rootItem2) {
            return rootItem.root.compareTo(rootItem2.root);
        }
    }
}
