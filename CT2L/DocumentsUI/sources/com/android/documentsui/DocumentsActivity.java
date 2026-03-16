package com.android.documentsui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import com.android.documentsui.model.DocumentInfo;
import com.android.documentsui.model.DocumentStack;
import com.android.documentsui.model.DurableUtils;
import com.android.documentsui.model.RootInfo;
import com.google.common.collect.Maps;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import libcore.io.IoUtils;

public class DocumentsActivity extends Activity {
    private DirectoryContainerView mDirectoryContainer;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIgnoreNextClose;
    private boolean mIgnoreNextCollapse;
    private boolean mIgnoreNextNavigation;
    private RootsCache mRoots;
    private View mRootsDrawer;
    private Toolbar mRootsToolbar;
    private boolean mSearchExpanded;
    private SearchView mSearchView;
    private boolean mShowAsDialog;
    private State mState;
    private Toolbar mToolbar;
    private Spinner mToolbarStack;
    private DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            DocumentsActivity.this.mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            DocumentsActivity.this.mDrawerToggle.onDrawerOpened(drawerView);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            DocumentsActivity.this.mDrawerToggle.onDrawerClosed(drawerView);
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            DocumentsActivity.this.mDrawerToggle.onDrawerStateChanged(newState);
        }
    };
    private BaseAdapter mStackAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return DocumentsActivity.this.mState.stack.size();
        }

        @Override
        public DocumentInfo getItem(int position) {
            return DocumentsActivity.this.mState.stack.get((DocumentsActivity.this.mState.stack.size() - position) - 1);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subdir_title, parent, false);
            }
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            DocumentInfo doc = getItem(position);
            if (position == 0) {
                RootInfo root = DocumentsActivity.this.getCurrentRoot();
                title.setText(root.title);
            } else {
                title.setText(doc.displayName);
            }
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subdir, parent, false);
            }
            ImageView subdir = (ImageView) convertView.findViewById(R.id.subdir);
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            DocumentInfo doc = getItem(position);
            if (position == 0) {
                RootInfo root = DocumentsActivity.this.getCurrentRoot();
                title.setText(root.title);
                subdir.setVisibility(8);
            } else {
                title.setText(doc.displayName);
                subdir.setVisibility(0);
            }
            return convertView;
        }
    };
    private AdapterView.OnItemSelectedListener mStackListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (DocumentsActivity.this.mIgnoreNextNavigation) {
                DocumentsActivity.this.mIgnoreNextNavigation = false;
                return;
            }
            while (DocumentsActivity.this.mState.stack.size() > position + 1) {
                DocumentsActivity.this.mState.stackTouched = true;
                DocumentsActivity.this.mState.stack.pop();
            }
            DocumentsActivity.this.onCurrentDirectoryChanged(4);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mRoots = DocumentsApplication.getRootsCache(this);
        setResult(0);
        setContentView(R.layout.activity);
        Resources res = getResources();
        this.mShowAsDialog = res.getBoolean(R.bool.show_as_dialog);
        if (this.mShowAsDialog) {
            WindowManager.LayoutParams a = getWindow().getAttributes();
            Point size = new Point();
            getWindowManager().getDefaultDisplay().getSize(size);
            a.width = (int) res.getFraction(R.dimen.dialog_width, size.x, size.x);
            getWindow().setAttributes(a);
        } else {
            this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            this.mDrawerToggle = new ActionBarDrawerToggle(this, this.mDrawerLayout, R.drawable.ic_hamburger, R.string.drawer_open, R.string.drawer_close);
            this.mDrawerLayout.setDrawerListener(this.mDrawerListener);
            this.mRootsDrawer = findViewById(R.id.drawer_roots);
        }
        this.mDirectoryContainer = (DirectoryContainerView) findViewById(R.id.container_directory);
        if (icicle != null) {
            this.mState = (State) icicle.getParcelable("state");
        } else {
            buildDefaultState();
        }
        this.mToolbar = (Toolbar) findViewById(R.id.toolbar);
        this.mToolbar.setTitleTextAppearance(this, android.R.style.TextAppearance.DeviceDefault.Widget.ActionBar.Title);
        this.mToolbarStack = (Spinner) findViewById(R.id.stack);
        this.mToolbarStack.setOnItemSelectedListener(this.mStackListener);
        this.mRootsToolbar = (Toolbar) findViewById(R.id.roots_toolbar);
        if (this.mRootsToolbar != null) {
            this.mRootsToolbar.setTitleTextAppearance(this, android.R.style.TextAppearance.DeviceDefault.Widget.ActionBar.Title);
        }
        setActionBar(this.mToolbar);
        if (this.mState.action == 5) {
            if (this.mShowAsDialog) {
                findViewById(R.id.container_roots).setVisibility(8);
            } else {
                this.mDrawerLayout.setDrawerLockMode(1);
            }
        }
        if (this.mState.action == 2) {
            String mimeType = getIntent().getType();
            String title = getIntent().getStringExtra("android.intent.extra.TITLE");
            SaveFragment.show(getFragmentManager(), mimeType, title);
        } else if (this.mState.action == 4) {
            PickFragment.show(getFragmentManager());
        }
        if (this.mState.action == 3) {
            Intent moreApps = new Intent(getIntent());
            moreApps.setComponent(null);
            moreApps.setPackage(null);
            RootsFragment.show(getFragmentManager(), moreApps);
        } else if (this.mState.action == 1 || this.mState.action == 2 || this.mState.action == 4) {
            RootsFragment.show(getFragmentManager(), null);
        }
        if (!this.mState.restored) {
            if (this.mState.action == 5) {
                Uri rootUri = getIntent().getData();
                new RestoreRootTask(rootUri).executeOnExecutor(getCurrentExecutor(), new Void[0]);
                return;
            } else {
                new RestoreStackTask().execute(new Void[0]);
                return;
            }
        }
        onCurrentDirectoryChanged(1);
    }

    private void buildDefaultState() {
        this.mState = new State();
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("android.intent.action.OPEN_DOCUMENT".equals(action)) {
            this.mState.action = 1;
        } else if ("android.intent.action.CREATE_DOCUMENT".equals(action)) {
            this.mState.action = 2;
        } else if ("android.intent.action.GET_CONTENT".equals(action)) {
            this.mState.action = 3;
        } else if ("android.intent.action.OPEN_DOCUMENT_TREE".equals(action)) {
            this.mState.action = 4;
        } else if ("android.provider.action.MANAGE_ROOT".equals(action)) {
            this.mState.action = 5;
        }
        if (this.mState.action == 1 || this.mState.action == 3) {
            this.mState.allowMultiple = intent.getBooleanExtra("android.intent.extra.ALLOW_MULTIPLE", false);
        }
        if (this.mState.action == 5) {
            this.mState.acceptMimes = new String[]{"*/*"};
            this.mState.allowMultiple = true;
        } else if (intent.hasExtra("android.intent.extra.MIME_TYPES")) {
            this.mState.acceptMimes = intent.getStringArrayExtra("android.intent.extra.MIME_TYPES");
        } else {
            this.mState.acceptMimes = new String[]{intent.getType()};
        }
        this.mState.localOnly = intent.getBooleanExtra("android.intent.extra.LOCAL_ONLY", false);
        this.mState.forceAdvanced = intent.getBooleanExtra("android.content.extra.SHOW_ADVANCED", false);
        this.mState.showAdvanced = this.mState.forceAdvanced | LocalPreferences.getDisplayAdvancedDevices(this);
        if (this.mState.action == 5) {
            this.mState.showSize = true;
        } else {
            this.mState.showSize = LocalPreferences.getDisplayFileSize(this);
        }
    }

    private class RestoreRootTask extends AsyncTask<Void, Void, RootInfo> {
        private Uri mRootUri;

        public RestoreRootTask(Uri rootUri) {
            this.mRootUri = rootUri;
        }

        @Override
        protected RootInfo doInBackground(Void... params) {
            String rootId = DocumentsContract.getRootId(this.mRootUri);
            return DocumentsActivity.this.mRoots.getRootOneshot(this.mRootUri.getAuthority(), rootId);
        }

        @Override
        protected void onPostExecute(RootInfo root) {
            if (!DocumentsActivity.this.isDestroyed()) {
                DocumentsActivity.this.mState.restored = true;
                if (root != null) {
                    DocumentsActivity.this.onRootPicked(root, true);
                } else {
                    Log.w("Documents", "Failed to find root: " + this.mRootUri);
                    DocumentsActivity.this.finish();
                }
            }
        }
    }

    private class RestoreStackTask extends AsyncTask<Void, Void, Void> {
        private volatile boolean mExternal;
        private volatile boolean mRestoredStack;

        private RestoreStackTask() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            String packageName = DocumentsActivity.this.getCallingPackageMaybeExtra();
            Cursor cursor = DocumentsActivity.this.getContentResolver().query(RecentsProvider.buildResume(packageName), null, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    this.mExternal = cursor.getInt(cursor.getColumnIndex("external")) != 0;
                    byte[] rawStack = cursor.getBlob(cursor.getColumnIndex("stack"));
                    DurableUtils.readFromArray(rawStack, DocumentsActivity.this.mState.stack);
                    this.mRestoredStack = true;
                }
            } catch (IOException e) {
                Log.w("Documents", "Failed to resume: " + e);
            } finally {
                IoUtils.closeQuietly(cursor);
            }
            if (this.mRestoredStack) {
                Collection<RootInfo> matchingRoots = DocumentsActivity.this.mRoots.getMatchingRootsBlocking(DocumentsActivity.this.mState);
                try {
                    DocumentsActivity.this.mState.stack.updateRoot(matchingRoots);
                    DocumentsActivity.this.mState.stack.updateDocuments(DocumentsActivity.this.getContentResolver());
                } catch (FileNotFoundException e2) {
                    Log.w("Documents", "Failed to restore stack: " + e2);
                    DocumentsActivity.this.mState.stack.reset();
                    this.mRestoredStack = false;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!DocumentsActivity.this.isDestroyed()) {
                DocumentsActivity.this.mState.restored = true;
                boolean showDrawer = false;
                if (!this.mRestoredStack) {
                    showDrawer = true;
                }
                if (MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, DocumentsActivity.this.mState.acceptMimes)) {
                    showDrawer = false;
                }
                if (this.mExternal && DocumentsActivity.this.mState.action == 3) {
                    showDrawer = true;
                }
                if (showDrawer) {
                    DocumentsActivity.this.setRootsDrawerOpen(true);
                }
                DocumentsActivity.this.onCurrentDirectoryChanged(1);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (this.mDrawerToggle != null) {
            this.mDrawerToggle.syncState();
        }
        updateActionBar();
    }

    public void setRootsDrawerOpen(boolean open) {
        if (!this.mShowAsDialog) {
            if (open) {
                this.mDrawerLayout.openDrawer(this.mRootsDrawer);
            } else {
                this.mDrawerLayout.closeDrawer(this.mRootsDrawer);
            }
        }
    }

    private boolean isRootsDrawerOpen() {
        if (this.mShowAsDialog) {
            return false;
        }
        return this.mDrawerLayout.isDrawerOpen(this.mRootsDrawer);
    }

    public void updateActionBar() {
        if (this.mRootsToolbar != null) {
            if (this.mState.action == 1 || this.mState.action == 3 || this.mState.action == 4) {
                this.mRootsToolbar.setTitle(R.string.title_open);
            } else if (this.mState.action == 2) {
                this.mRootsToolbar.setTitle(R.string.title_save);
            }
        }
        RootInfo root = getCurrentRoot();
        boolean showRootIcon = this.mShowAsDialog || this.mState.action == 5;
        if (showRootIcon) {
            this.mToolbar.setNavigationIcon(root != null ? root.loadToolbarIcon(this.mToolbar.getContext()) : null);
            this.mToolbar.setNavigationContentDescription(R.string.drawer_open);
            this.mToolbar.setNavigationOnClickListener(null);
        } else {
            this.mToolbar.setNavigationIcon(R.drawable.ic_hamburger);
            this.mToolbar.setNavigationContentDescription(R.string.drawer_open);
            this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DocumentsActivity.this.setRootsDrawerOpen(true);
                }
            });
        }
        if (this.mSearchExpanded) {
            this.mToolbar.setTitle((CharSequence) null);
            this.mToolbarStack.setVisibility(8);
            this.mToolbarStack.setAdapter((SpinnerAdapter) null);
        } else if (this.mState.stack.size() <= 1) {
            this.mToolbar.setTitle(root.title);
            this.mToolbarStack.setVisibility(8);
            this.mToolbarStack.setAdapter((SpinnerAdapter) null);
        } else {
            this.mToolbar.setTitle((CharSequence) null);
            this.mToolbarStack.setVisibility(0);
            this.mToolbarStack.setAdapter((SpinnerAdapter) this.mStackAdapter);
            this.mIgnoreNextNavigation = true;
            this.mToolbarStack.setSelection(this.mStackAdapter.getCount() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity, menu);
        if (this.mShowAsDialog) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                switch (item.getItemId()) {
                    case R.id.menu_advanced:
                    case R.id.menu_file_size:
                        break;
                    default:
                        item.setShowAsAction(2);
                        break;
                }
            }
        }
        MenuItem searchMenu = menu.findItem(R.id.menu_search);
        this.mSearchView = (SearchView) searchMenu.getActionView();
        this.mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                DocumentsActivity.this.mSearchExpanded = true;
                DocumentsActivity.this.mState.currentSearch = query;
                DocumentsActivity.this.mSearchView.clearFocus();
                DocumentsActivity.this.onCurrentDirectoryChanged(1);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item2) {
                DocumentsActivity.this.mSearchExpanded = true;
                DocumentsActivity.this.updateActionBar();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item2) {
                DocumentsActivity.this.mSearchExpanded = false;
                if (DocumentsActivity.this.mIgnoreNextCollapse) {
                    DocumentsActivity.this.mIgnoreNextCollapse = false;
                } else {
                    DocumentsActivity.this.mState.currentSearch = null;
                    DocumentsActivity.this.onCurrentDirectoryChanged(1);
                }
                return true;
            }
        });
        this.mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                DocumentsActivity.this.mSearchExpanded = false;
                if (DocumentsActivity.this.mIgnoreNextClose) {
                    DocumentsActivity.this.mIgnoreNextClose = false;
                } else {
                    DocumentsActivity.this.mState.currentSearch = null;
                    DocumentsActivity.this.onCurrentDirectoryChanged(1);
                }
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean searchVisible;
        super.onPrepareOptionsMenu(menu);
        FragmentManager fm = getFragmentManager();
        RootInfo root = getCurrentRoot();
        DocumentInfo cwd = getCurrentDirectory();
        MenuItem createDir = menu.findItem(R.id.menu_create_dir);
        MenuItem search = menu.findItem(R.id.menu_search);
        MenuItem sort = menu.findItem(R.id.menu_sort);
        MenuItem sortSize = menu.findItem(R.id.menu_sort_size);
        MenuItem grid = menu.findItem(R.id.menu_grid);
        MenuItem list = menu.findItem(R.id.menu_list);
        MenuItem advanced = menu.findItem(R.id.menu_advanced);
        MenuItem fileSize = menu.findItem(R.id.menu_file_size);
        sort.setVisible(cwd != null);
        grid.setVisible(this.mState.derivedMode != 2);
        list.setVisible(this.mState.derivedMode != 1);
        if (this.mState.currentSearch != null) {
            sort.setVisible(false);
            search.expandActionView();
            this.mSearchView.setIconified(false);
            this.mSearchView.clearFocus();
            this.mSearchView.setQuery(this.mState.currentSearch, false);
        } else {
            this.mIgnoreNextClose = true;
            this.mSearchView.setIconified(true);
            this.mSearchView.clearFocus();
            this.mIgnoreNextCollapse = true;
            search.collapseActionView();
        }
        sortSize.setVisible(this.mState.showSize);
        boolean fileSizeVisible = this.mState.action != 5;
        if (this.mState.action == 2 || this.mState.action == 4) {
            createDir.setVisible(cwd != null && cwd.isCreateSupported());
            searchVisible = false;
            if (cwd == null) {
                grid.setVisible(false);
                list.setVisible(false);
                fileSizeVisible = false;
            }
            if (this.mState.action == 2) {
                SaveFragment.get(fm).setSaveEnabled(cwd != null && cwd.isCreateSupported());
            }
        } else {
            createDir.setVisible(false);
            searchVisible = (root == null || (root.flags & 8) == 0) ? false : true;
        }
        search.setVisible(searchVisible);
        advanced.setTitle(LocalPreferences.getDisplayAdvancedDevices(this) ? R.string.menu_advanced_hide : R.string.menu_advanced_show);
        fileSize.setTitle(LocalPreferences.getDisplayFileSize(this) ? R.string.menu_file_size_hide : R.string.menu_file_size_show);
        advanced.setVisible(this.mState.action != 5);
        fileSize.setVisible(fileSizeVisible);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.mDrawerToggle != null && this.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        if (id == 16908332) {
            onBackPressed();
            return true;
        }
        if (id == R.id.menu_create_dir) {
            CreateDirectoryFragment.show(getFragmentManager());
            return true;
        }
        if (id == R.id.menu_search) {
            return false;
        }
        if (id == R.id.menu_sort_name) {
            setUserSortOrder(1);
            return true;
        }
        if (id == R.id.menu_sort_date) {
            setUserSortOrder(2);
            return true;
        }
        if (id == R.id.menu_sort_size) {
            setUserSortOrder(3);
            return true;
        }
        if (id == R.id.menu_grid) {
            setUserMode(2);
            return true;
        }
        if (id == R.id.menu_list) {
            setUserMode(1);
            return true;
        }
        if (id == R.id.menu_advanced) {
            setDisplayAdvancedDevices(LocalPreferences.getDisplayAdvancedDevices(this) ? false : true);
            return true;
        }
        if (id == R.id.menu_file_size) {
            setDisplayFileSize(LocalPreferences.getDisplayFileSize(this) ? false : true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setDisplayAdvancedDevices(boolean display) {
        LocalPreferences.setDisplayAdvancedDevices(this, display);
        this.mState.showAdvanced = this.mState.forceAdvanced | display;
        RootsFragment.get(getFragmentManager()).onDisplayStateChanged();
        invalidateOptionsMenu();
    }

    private void setDisplayFileSize(boolean display) {
        LocalPreferences.setDisplayFileSize(this, display);
        this.mState.showSize = display;
        DirectoryFragment.get(getFragmentManager()).onDisplayStateChanged();
        invalidateOptionsMenu();
    }

    public void onStateChanged() {
        invalidateOptionsMenu();
    }

    private void setUserSortOrder(int sortOrder) {
        this.mState.userSortOrder = sortOrder;
        DirectoryFragment.get(getFragmentManager()).onUserSortOrderChanged();
    }

    private void setUserMode(int mode) {
        this.mState.userMode = mode;
        DirectoryFragment.get(getFragmentManager()).onUserModeChanged();
    }

    public void setPending(boolean pending) {
        SaveFragment save = SaveFragment.get(getFragmentManager());
        if (save != null) {
            save.setPending(pending);
        }
    }

    @Override
    public void onBackPressed() {
        if (!this.mState.stackTouched) {
            super.onBackPressed();
            return;
        }
        int size = this.mState.stack.size();
        if (size > 1) {
            this.mState.stack.pop();
            onCurrentDirectoryChanged(4);
        } else if (size == 1 && !isRootsDrawerOpen()) {
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelable("state", this.mState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    public RootInfo getCurrentRoot() {
        return this.mState.stack.root != null ? this.mState.stack.root : this.mRoots.getRecentsRoot();
    }

    public DocumentInfo getCurrentDirectory() {
        return this.mState.stack.peek();
    }

    private String getCallingPackageMaybeExtra() {
        String extra = getIntent().getStringExtra("android.content.extra.PACKAGE_NAME");
        return extra != null ? extra : getCallingPackage();
    }

    public Executor getCurrentExecutor() {
        DocumentInfo cwd = getCurrentDirectory();
        return (cwd == null || cwd.authority == null) ? AsyncTask.THREAD_POOL_EXECUTOR : ProviderExecutor.forAuthority(cwd.authority);
    }

    public State getDisplayState() {
        return this.mState;
    }

    private void onCurrentDirectoryChanged(int anim) {
        PickFragment pick;
        SaveFragment save;
        FragmentManager fm = getFragmentManager();
        RootInfo root = getCurrentRoot();
        DocumentInfo cwd = getCurrentDirectory();
        this.mDirectoryContainer.setDrawDisappearingFirst(anim == 3);
        if (cwd == null) {
            if (this.mState.action == 2 || this.mState.action == 4) {
                RecentsCreateFragment.show(fm);
            } else {
                DirectoryFragment.showRecentsOpen(fm, anim);
                boolean visualMimes = MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, this.mState.acceptMimes);
                this.mState.userMode = visualMimes ? 2 : 1;
                this.mState.derivedMode = this.mState.userMode;
            }
        } else if (this.mState.currentSearch != null) {
            DirectoryFragment.showSearch(fm, root, this.mState.currentSearch, anim);
        } else {
            DirectoryFragment.showNormal(fm, root, cwd, anim);
        }
        if (this.mState.action == 2 && (save = SaveFragment.get(fm)) != null) {
            save.setReplaceTarget(null);
        }
        if (this.mState.action == 4 && (pick = PickFragment.get(fm)) != null) {
            CharSequence displayName = this.mState.stack.size() <= 1 ? root.title : cwd.displayName;
            pick.setPickTarget(cwd, displayName);
        }
        RootsFragment roots = RootsFragment.get(fm);
        if (roots != null) {
            roots.onCurrentRootChanged();
        }
        updateActionBar();
        invalidateOptionsMenu();
        dumpStack();
    }

    public void onStackPicked(DocumentStack stack) {
        try {
            stack.updateDocuments(getContentResolver());
            this.mState.stack = stack;
            this.mState.stackTouched = true;
            onCurrentDirectoryChanged(2);
        } catch (FileNotFoundException e) {
            Log.w("Documents", "Failed to restore stack: " + e);
        }
    }

    public void onRootPicked(RootInfo root, boolean closeDrawer) {
        this.mState.stack.root = root;
        this.mState.stack.clear();
        this.mState.stackTouched = true;
        if (!this.mRoots.isRecentsRoot(root)) {
            new PickRootTask(root).executeOnExecutor(getCurrentExecutor(), new Void[0]);
        } else {
            onCurrentDirectoryChanged(2);
        }
        if (closeDrawer) {
            setRootsDrawerOpen(false);
        }
    }

    private class PickRootTask extends AsyncTask<Void, Void, DocumentInfo> {
        private RootInfo mRoot;

        public PickRootTask(RootInfo root) {
            this.mRoot = root;
        }

        @Override
        protected DocumentInfo doInBackground(Void... params) {
            try {
                Uri uri = DocumentsContract.buildDocumentUri(this.mRoot.authority, this.mRoot.documentId);
                return DocumentInfo.fromUri(DocumentsActivity.this.getContentResolver(), uri);
            } catch (FileNotFoundException e) {
                Log.w("Documents", "Failed to find root", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            if (result != null) {
                DocumentsActivity.this.mState.stack.push(result);
                DocumentsActivity.this.mState.stackTouched = true;
                DocumentsActivity.this.onCurrentDirectoryChanged(2);
            }
        }
    }

    public void onAppPicked(ResolveInfo info) {
        Intent intent = new Intent(getIntent());
        intent.setFlags(intent.getFlags() & (-33554433));
        intent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
        startActivityForResult(intent, 42);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Documents", "onActivityResult() code=" + resultCode);
        if (requestCode == 42 && resultCode != 0) {
            String packageName = getCallingPackageMaybeExtra();
            ContentValues values = new ContentValues();
            values.put("external", (Integer) 1);
            getContentResolver().insert(RecentsProvider.buildResume(packageName), values);
            setResult(resultCode, data);
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onDocumentPicked(DocumentInfo doc) {
        FragmentManager fm = getFragmentManager();
        if (doc.isDirectory()) {
            this.mState.stack.push(doc);
            this.mState.stackTouched = true;
            onCurrentDirectoryChanged(3);
            return;
        }
        if (this.mState.action == 1 || this.mState.action == 3) {
            new ExistingFinishTask(doc.derivedUri).executeOnExecutor(getCurrentExecutor(), new Void[0]);
            return;
        }
        if (this.mState.action == 2) {
            SaveFragment.get(fm).setReplaceTarget(doc);
            return;
        }
        if (this.mState.action == 5) {
            Intent manage = new Intent("android.provider.action.MANAGE_DOCUMENT");
            manage.setData(doc.derivedUri);
            try {
                startActivity(manage);
            } catch (ActivityNotFoundException e) {
                Intent view = new Intent("android.intent.action.VIEW");
                view.setFlags(1);
                view.setData(doc.derivedUri);
                try {
                    startActivity(view);
                } catch (ActivityNotFoundException e2) {
                    Toast.makeText(this, R.string.toast_no_application, 0).show();
                }
            }
        }
    }

    public void onDocumentsPicked(List<DocumentInfo> docs) {
        if (this.mState.action == 1 || this.mState.action == 3) {
            int size = docs.size();
            Uri[] uris = new Uri[size];
            for (int i = 0; i < size; i++) {
                uris[i] = docs.get(i).derivedUri;
            }
            new ExistingFinishTask(uris).executeOnExecutor(getCurrentExecutor(), new Void[0]);
        }
    }

    public void onSaveRequested(DocumentInfo replaceTarget) {
        new ExistingFinishTask(replaceTarget.derivedUri).executeOnExecutor(getCurrentExecutor(), new Void[0]);
    }

    public void onSaveRequested(String mimeType, String displayName) {
        new CreateFinishTask(mimeType, displayName).executeOnExecutor(getCurrentExecutor(), new Void[0]);
    }

    public void onPickRequested(DocumentInfo pickTarget) {
        Uri viaUri = DocumentsContract.buildTreeDocumentUri(pickTarget.authority, pickTarget.documentId);
        new PickFinishTask(viaUri).executeOnExecutor(getCurrentExecutor(), new Void[0]);
    }

    private void saveStackBlocking() {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        byte[] rawStack = DurableUtils.writeToArrayOrNull(this.mState.stack);
        if (this.mState.action == 2 || this.mState.action == 4) {
            values.clear();
            values.put("key", this.mState.stack.buildKey());
            values.put("stack", rawStack);
            resolver.insert(RecentsProvider.buildRecent(), values);
        }
        String packageName = getCallingPackageMaybeExtra();
        values.clear();
        values.put("stack", rawStack);
        values.put("external", (Integer) 0);
        resolver.insert(RecentsProvider.buildResume(packageName), values);
    }

    private void onFinished(Uri... uris) {
        Log.d("Documents", "onFinished() " + Arrays.toString(uris));
        Intent intent = new Intent();
        if (uris.length == 1) {
            intent.setData(uris[0]);
        } else if (uris.length > 1) {
            ClipData clipData = new ClipData(null, this.mState.acceptMimes, new ClipData.Item(uris[0]));
            for (int i = 1; i < uris.length; i++) {
                clipData.addItem(new ClipData.Item(uris[i]));
            }
            intent.setClipData(clipData);
        }
        if (this.mState.action == 3) {
            intent.addFlags(1);
        } else if (this.mState.action == 4) {
            intent.addFlags(195);
        } else {
            intent.addFlags(67);
        }
        setResult(-1, intent);
        finish();
    }

    private class CreateFinishTask extends AsyncTask<Void, Void, Uri> {
        private final String mDisplayName;
        private final String mMimeType;

        public CreateFinishTask(String mimeType, String displayName) {
            this.mMimeType = mimeType;
            this.mDisplayName = displayName;
        }

        @Override
        protected void onPreExecute() {
            DocumentsActivity.this.setPending(true);
        }

        @Override
        protected Uri doInBackground(Void... params) {
            ContentResolver resolver = DocumentsActivity.this.getContentResolver();
            DocumentInfo cwd = DocumentsActivity.this.getCurrentDirectory();
            ContentProviderClient client = null;
            Uri childUri = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, cwd.derivedUri.getAuthority());
                childUri = DocumentsContract.createDocument(client, cwd.derivedUri, this.mMimeType, this.mDisplayName);
            } catch (Exception e) {
                Log.w("Documents", "Failed to create document", e);
            } finally {
                ContentProviderClient.releaseQuietly(client);
            }
            if (childUri != null) {
                DocumentsActivity.this.saveStackBlocking();
            }
            return childUri;
        }

        @Override
        protected void onPostExecute(Uri result) {
            if (result != null) {
                DocumentsActivity.this.onFinished(result);
            } else {
                Toast.makeText(DocumentsActivity.this, R.string.save_error, 0).show();
            }
            DocumentsActivity.this.setPending(false);
        }
    }

    private class ExistingFinishTask extends AsyncTask<Void, Void, Void> {
        private final Uri[] mUris;

        public ExistingFinishTask(Uri... uris) {
            this.mUris = uris;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DocumentsActivity.this.saveStackBlocking();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            DocumentsActivity.this.onFinished(this.mUris);
        }
    }

    private class PickFinishTask extends AsyncTask<Void, Void, Void> {
        private final Uri mUri;

        public PickFinishTask(Uri uri) {
            this.mUri = uri;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DocumentsActivity.this.saveStackBlocking();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            DocumentsActivity.this.onFinished(this.mUri);
        }
    }

    public static class State implements Parcelable {
        public static final Parcelable.Creator<State> CREATOR = new Parcelable.Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                State state = new State();
                state.action = in.readInt();
                state.userMode = in.readInt();
                state.acceptMimes = in.readStringArray();
                state.userSortOrder = in.readInt();
                state.allowMultiple = in.readInt() != 0;
                state.showSize = in.readInt() != 0;
                state.localOnly = in.readInt() != 0;
                state.forceAdvanced = in.readInt() != 0;
                state.showAdvanced = in.readInt() != 0;
                state.stackTouched = in.readInt() != 0;
                state.restored = in.readInt() != 0;
                DurableUtils.readFromParcel(in, state.stack);
                state.currentSearch = in.readString();
                in.readMap(state.dirState, null);
                return state;
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
        public String[] acceptMimes;
        public int action;
        public String currentSearch;
        public int userMode = 0;
        public int derivedMode = 1;
        public int userSortOrder = 0;
        public int derivedSortOrder = 1;
        public boolean allowMultiple = false;
        public boolean showSize = false;
        public boolean localOnly = false;
        public boolean forceAdvanced = false;
        public boolean showAdvanced = false;
        public boolean stackTouched = false;
        public boolean restored = false;
        public DocumentStack stack = new DocumentStack();
        public HashMap<String, SparseArray<Parcelable>> dirState = Maps.newHashMap();

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(this.action);
            out.writeInt(this.userMode);
            out.writeStringArray(this.acceptMimes);
            out.writeInt(this.userSortOrder);
            out.writeInt(this.allowMultiple ? 1 : 0);
            out.writeInt(this.showSize ? 1 : 0);
            out.writeInt(this.localOnly ? 1 : 0);
            out.writeInt(this.forceAdvanced ? 1 : 0);
            out.writeInt(this.showAdvanced ? 1 : 0);
            out.writeInt(this.stackTouched ? 1 : 0);
            out.writeInt(this.restored ? 1 : 0);
            DurableUtils.writeToParcel(out, this.stack);
            out.writeString(this.currentSearch);
            out.writeMap(this.dirState);
        }
    }

    private void dumpStack() {
        Log.d("Documents", "Current stack: ");
        Log.d("Documents", " * " + this.mState.stack.root);
        for (DocumentInfo doc : this.mState.stack) {
            Log.d("Documents", " +-- " + doc);
        }
    }

    public static DocumentsActivity get(Fragment fragment) {
        return (DocumentsActivity) fragment.getActivity();
    }
}
