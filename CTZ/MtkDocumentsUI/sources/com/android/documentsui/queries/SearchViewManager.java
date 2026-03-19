package com.android.documentsui.queries;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.queries.SearchViewManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Timer;
import java.util.TimerTask;

public class SearchViewManager implements MenuItem.OnActionExpandListener, View.OnClickListener, View.OnFocusChangeListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener {
    static final boolean $assertionsDisabled = false;
    private final EventHandler<String> mCommandProcessor;
    private String mCurrentSearch;
    private boolean mFullBar;
    private boolean mIgnoreNextClose;
    private final SearchManagerListener mListener;
    private Menu mMenu;
    private MenuItem mMenuItem;

    @GuardedBy("mSearchLock")
    private Runnable mQueuedSearchRunnable;

    @GuardedBy("mSearchLock")
    private TimerTask mQueuedSearchTask;
    private boolean mSearchExpanded;
    private final Object mSearchLock;
    private SearchView mSearchView;
    private final Timer mTimer;
    private final Handler mUiHandler;

    public interface SearchManagerListener {
        void onSearchChanged(String str);

        void onSearchFinished();

        void onSearchViewChanged(boolean z);
    }

    public SearchViewManager(SearchManagerListener searchManagerListener, EventHandler<String> eventHandler, Bundle bundle) {
        this(searchManagerListener, eventHandler, bundle, new Timer(), new Handler(Looper.getMainLooper()));
    }

    @VisibleForTesting
    protected SearchViewManager(SearchManagerListener searchManagerListener, EventHandler<String> eventHandler, Bundle bundle, Timer timer, Handler handler) {
        this.mSearchLock = new Object();
        this.mListener = searchManagerListener;
        this.mCommandProcessor = eventHandler;
        this.mTimer = timer;
        this.mUiHandler = handler;
        this.mCurrentSearch = bundle != null ? bundle.getString("query") : null;
    }

    public void install(Menu menu, boolean z) {
        this.mMenu = menu;
        this.mMenuItem = this.mMenu.findItem(R.id.option_menu_search);
        this.mSearchView = (SearchView) this.mMenuItem.getActionView();
        this.mSearchView.setOnQueryTextListener(this);
        this.mSearchView.setOnCloseListener(this);
        this.mSearchView.setOnSearchClickListener(this);
        this.mSearchView.setOnQueryTextFocusChangeListener(this);
        this.mFullBar = z;
        if (this.mFullBar) {
            this.mMenuItem.setShowAsActionFlags(10);
            this.mMenuItem.setOnActionExpandListener(this);
            this.mSearchView.setMaxWidth(Integer.MAX_VALUE);
        }
        restoreSearch();
    }

    public void updateMenu() {
        if (isSearching() && this.mFullBar) {
            this.mMenu.setGroupVisible(R.id.group_hide_when_searching, false);
        }
    }

    public void update(DocumentStack documentStack) {
        if (this.mMenuItem == null) {
            if (SharedMinimal.DEBUG) {
                Log.d("SearchManager", "update called before Search MenuItem installed.");
                return;
            }
            return;
        }
        if (this.mCurrentSearch != null) {
            this.mMenuItem.expandActionView();
            this.mSearchView.setIconified(false);
            this.mSearchView.clearFocus();
            this.mSearchView.setQuery(this.mCurrentSearch, false);
        } else {
            this.mSearchView.clearFocus();
            if (!this.mSearchView.isIconified()) {
                this.mIgnoreNextClose = true;
                this.mSearchView.setIconified(true);
            }
            if (this.mMenuItem.isActionViewExpanded()) {
                this.mMenuItem.collapseActionView();
            }
        }
        showMenu(documentStack);
    }

    public void showMenu(DocumentStack documentStack) {
        DocumentInfo documentInfoPeek = documentStack != null ? documentStack.peek() : null;
        boolean z = true;
        if (documentInfoPeek != null && documentInfoPeek.isInArchive()) {
            z = false;
        }
        RootInfo root = documentStack != null ? documentStack.getRoot() : null;
        if (root == null || (root.flags & 8) == 0) {
            z = false;
        }
        if (this.mMenuItem == null) {
            if (SharedMinimal.DEBUG) {
                Log.d("SearchManager", "showMenu called before Search MenuItem installed.");
            }
        } else {
            if (!z) {
                this.mCurrentSearch = null;
            }
            this.mMenuItem.setVisible(z);
        }
    }

    public boolean cancelSearch() {
        if (!isExpanded() && !isSearching()) {
            return false;
        }
        cancelQueuedSearch();
        this.mSearchView.setQuery("", false);
        if (this.mFullBar) {
            onClose();
        } else {
            this.mSearchView.setIconified(true);
        }
        return true;
    }

    private void cancelQueuedSearch() {
        synchronized (this.mSearchLock) {
            if (this.mQueuedSearchTask != null) {
                this.mQueuedSearchTask.cancel();
            }
            this.mQueuedSearchTask = null;
            this.mUiHandler.removeCallbacks(this.mQueuedSearchRunnable);
            this.mQueuedSearchRunnable = null;
        }
    }

    private void restoreSearch() {
        if (isSearching()) {
            if (this.mFullBar) {
                this.mMenuItem.expandActionView();
            } else {
                this.mSearchView.setIconified(false);
            }
            onSearchExpanded();
            this.mSearchView.setQuery(this.mCurrentSearch, false);
            this.mSearchView.clearFocus();
        }
    }

    private void onSearchExpanded() {
        this.mSearchExpanded = true;
        if (this.mFullBar) {
            this.mMenu.setGroupVisible(R.id.group_hide_when_searching, false);
        }
        this.mListener.onSearchViewChanged(true);
    }

    @Override
    public boolean onClose() {
        this.mSearchExpanded = false;
        if (this.mIgnoreNextClose) {
            this.mIgnoreNextClose = false;
            return false;
        }
        if (this.mCurrentSearch != null) {
            this.mCurrentSearch = null;
            this.mListener.onSearchChanged(this.mCurrentSearch);
        }
        if (this.mFullBar) {
            this.mMenuItem.collapseActionView();
        }
        this.mListener.onSearchFinished();
        this.mListener.onSearchViewChanged(false);
        return false;
    }

    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("query", this.mCurrentSearch);
    }

    @Override
    public void onClick(View view) {
        onSearchExpanded();
    }

    @Override
    public boolean onQueryTextSubmit(String str) {
        if (this.mCommandProcessor.accept(str)) {
            this.mSearchView.setQuery("", false);
            return true;
        }
        cancelQueuedSearch();
        if (this.mCurrentSearch != str) {
            this.mCurrentSearch = str;
            this.mListener.onSearchChanged(this.mCurrentSearch);
        }
        this.mSearchView.clearFocus();
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        if (!z) {
            if (this.mCurrentSearch == null) {
                this.mSearchView.setIconified(true);
            } else if (TextUtils.isEmpty(this.mSearchView.getQuery())) {
                cancelSearch();
            }
        }
    }

    class AnonymousClass1 extends TimerTask {
        final String val$newText;

        AnonymousClass1(String str) {
            this.val$newText = str;
        }

        @Override
        public void run() {
            synchronized (SearchViewManager.this.mSearchLock) {
                SearchViewManager searchViewManager = SearchViewManager.this;
                final String str = this.val$newText;
                searchViewManager.mQueuedSearchRunnable = new Runnable() {
                    @Override
                    public final void run() {
                        SearchViewManager.AnonymousClass1.lambda$run$0(this.f$0, str);
                    }
                };
                SearchViewManager.this.mUiHandler.post(SearchViewManager.this.mQueuedSearchRunnable);
            }
        }

        public static void lambda$run$0(AnonymousClass1 anonymousClass1, String str) {
            SearchViewManager.this.mCurrentSearch = str;
            if (SearchViewManager.this.mCurrentSearch != null && SearchViewManager.this.mCurrentSearch.isEmpty()) {
                SearchViewManager.this.mCurrentSearch = null;
            }
            SearchViewManager.this.mListener.onSearchChanged(SearchViewManager.this.mCurrentSearch);
        }
    }

    @VisibleForTesting
    protected TimerTask createSearchTask(String str) {
        return new AnonymousClass1(str);
    }

    @Override
    public boolean onQueryTextChange(String str) {
        cancelQueuedSearch();
        synchronized (this.mSearchLock) {
            this.mQueuedSearchTask = createSearchTask(str);
            this.mTimer.schedule(this.mQueuedSearchTask, 750L);
        }
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        this.mMenu.setGroupVisible(R.id.group_hide_when_searching, true);
        if (!isExpanded() && !isSearching()) {
            return true;
        }
        cancelSearch();
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem menuItem) {
        return true;
    }

    public String getCurrentSearch() {
        return this.mCurrentSearch;
    }

    public boolean isSearching() {
        return this.mCurrentSearch != null;
    }

    public boolean isExpanded() {
        return this.mSearchExpanded;
    }
}
