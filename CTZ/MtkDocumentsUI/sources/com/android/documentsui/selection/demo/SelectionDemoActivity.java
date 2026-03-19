package com.android.documentsui.selection.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import com.android.documentsui.R;
import com.android.documentsui.selection.BandSelectionHelper;
import com.android.documentsui.selection.ContentLock;
import com.android.documentsui.selection.DefaultBandHost;
import com.android.documentsui.selection.DefaultBandPredicate;
import com.android.documentsui.selection.DefaultSelectionHelper;
import com.android.documentsui.selection.GestureRouter;
import com.android.documentsui.selection.GestureSelectionHelper;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MouseInputHandler;
import com.android.documentsui.selection.MutableSelection;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.selection.TouchEventRouter;
import com.android.documentsui.selection.TouchInputHandler;
import com.android.documentsui.selection.demo.SelectionDemoAdapter;

public class SelectionDemoActivity extends AppCompatActivity {
    private SelectionDemoAdapter mAdapter;
    private int mColumnCount = 1;
    private GridLayoutManager mLayout;
    private RecyclerView mRecView;
    private SelectionHelper mSelectionHelper;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.selection_demo_layout);
        this.mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(this.mToolbar);
        this.mRecView = (RecyclerView) findViewById(R.id.list);
        this.mLayout = new GridLayoutManager(this, this.mColumnCount);
        this.mRecView.setLayoutManager(this.mLayout);
        this.mAdapter = new SelectionDemoAdapter(this);
        this.mRecView.setAdapter(this.mAdapter);
        DemoStableIdProvider demoStableIdProvider = new DemoStableIdProvider(this.mAdapter);
        SelectionHelper.SelectionPredicate selectionPredicate = new SelectionHelper.SelectionPredicate() {
            @Override
            public boolean canSetStateForId(String str, boolean z) {
                return true;
            }

            @Override
            public boolean canSetStateAtPosition(int i, boolean z) {
                return true;
            }
        };
        this.mSelectionHelper = new DefaultSelectionHelper(0, this.mAdapter, demoStableIdProvider, selectionPredicate);
        this.mAdapter.addOnBindCallback(new SelectionDemoAdapter.OnBindCallback() {
            @Override
            void onBound(DemoHolder demoHolder, int i) {
                demoHolder.setSelected(SelectionDemoActivity.this.mSelectionHelper.isSelected(SelectionDemoActivity.this.mAdapter.getStableId(i)));
            }
        });
        DemoDetailsLookup demoDetailsLookup = new DemoDetailsLookup(this.mRecView);
        GestureRouter gestureRouter = new GestureRouter();
        TouchEventRouter touchEventRouter = new TouchEventRouter(new GestureDetector(this, gestureRouter));
        ContentLock contentLock = new ContentLock();
        GestureSelectionHelper gestureSelectionHelperCreate = GestureSelectionHelper.create(this.mSelectionHelper, this.mRecView, contentLock, demoDetailsLookup);
        this.mRecView.addOnItemTouchListener(touchEventRouter);
        TouchInputHandler touchInputHandler = new TouchInputHandler(this.mSelectionHelper, demoDetailsLookup, selectionPredicate, gestureSelectionHelperCreate, new TouchCallbacks(this, this.mRecView));
        touchEventRouter.register(1, gestureSelectionHelperCreate);
        touchEventRouter.register(0, gestureSelectionHelperCreate);
        gestureRouter.register(1, touchInputHandler);
        gestureRouter.register(0, touchInputHandler);
        MouseInputHandler mouseInputHandler = new MouseInputHandler(this.mSelectionHelper, demoDetailsLookup, new MouseCallbacks(this, this.mRecView));
        BandSelectionHelper bandSelectionHelper = new BandSelectionHelper(new DefaultBandHost(this.mRecView, R.drawable.selection_demo_band_overlay), this.mAdapter, demoStableIdProvider, this.mSelectionHelper, selectionPredicate, new DefaultBandPredicate(demoDetailsLookup), contentLock);
        touchEventRouter.register(3, bandSelectionHelper);
        touchEventRouter.register(2, bandSelectionHelper);
        gestureRouter.register(3, mouseInputHandler);
        gestureRouter.register(2, mouseInputHandler);
        updateFromSavedState(bundle);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        MutableSelection mutableSelection = new MutableSelection();
        this.mSelectionHelper.copySelection(mutableSelection);
        bundle.putParcelable("demo-saved-selection", mutableSelection);
        bundle.putInt("demo-column-count", this.mColumnCount);
    }

    private void updateFromSavedState(Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey("demo-saved-selection")) {
                Selection selection = (Selection) bundle.getParcelable("demo-saved-selection");
                if (!selection.isEmpty()) {
                    this.mSelectionHelper.restoreSelection(selection);
                    Toast.makeText(this, "Selection restored.", 0).show();
                }
            }
            if (bundle.containsKey("demo-column-count")) {
                this.mColumnCount = bundle.getInt("demo-column-count");
                this.mLayout.setSpanCount(this.mColumnCount);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean zOnCreateOptionsMenu = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.selection_demo_actions, menu);
        return zOnCreateOptionsMenu;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.option_menu_add_column).setEnabled(this.mColumnCount <= 3);
        menu.findItem(R.id.option_menu_remove_column).setEnabled(this.mColumnCount > 1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.option_menu_add_column) {
            GridLayoutManager gridLayoutManager = this.mLayout;
            int i = this.mColumnCount + 1;
            this.mColumnCount = i;
            gridLayoutManager.setSpanCount(i);
            return true;
        }
        if (itemId == R.id.option_menu_remove_column) {
            GridLayoutManager gridLayoutManager2 = this.mLayout;
            int i2 = this.mColumnCount - 1;
            this.mColumnCount = i2;
            gridLayoutManager2.setSpanCount(i2);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        if (this.mSelectionHelper.hasSelection()) {
            this.mSelectionHelper.clearSelection();
            this.mSelectionHelper.clearProvisionalSelection();
        } else {
            super.onBackPressed();
        }
    }

    private static void toast(Context context, String str) {
        Toast.makeText(context, str, 0).show();
    }

    @Override
    protected void onDestroy() {
        this.mSelectionHelper.clearSelection();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mAdapter.loadData();
    }

    private static final class MouseCallbacks extends MouseInputHandler.Callbacks {
        private final Context mContext;
        private final RecyclerView mRecView;

        MouseCallbacks(Context context, RecyclerView recyclerView) {
            this.mContext = context;
            this.mRecView = recyclerView;
        }

        @Override
        public boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, MotionEvent motionEvent) {
            SelectionDemoActivity.toast(this.mContext, "Activate item: " + itemDetails.getStableId());
            return true;
        }

        @Override
        public boolean onContextClick(MotionEvent motionEvent) {
            SelectionDemoActivity.toast(this.mContext, "Context click received.");
            return true;
        }

        @Override
        public void onPerformHapticFeedback() {
            this.mRecView.performHapticFeedback(0);
        }
    }

    private static final class TouchCallbacks extends TouchInputHandler.Callbacks {
        private final Context mContext;
        private final RecyclerView mRecView;

        private TouchCallbacks(Context context, RecyclerView recyclerView) {
            this.mContext = context;
            this.mRecView = recyclerView;
        }

        @Override
        public boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, MotionEvent motionEvent) {
            SelectionDemoActivity.toast(this.mContext, "Activate item: " + itemDetails.getStableId());
            return true;
        }

        @Override
        public void onPerformHapticFeedback() {
            this.mRecView.performHapticFeedback(0);
        }
    }
}
