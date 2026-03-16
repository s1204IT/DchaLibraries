package com.android.gallery3d.filtershow;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.print.PrintHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.category.Action;
import com.android.gallery3d.filtershow.category.CategoryAdapter;
import com.android.gallery3d.filtershow.category.CategorySelected;
import com.android.gallery3d.filtershow.category.CategoryView;
import com.android.gallery3d.filtershow.category.MainPanel;
import com.android.gallery3d.filtershow.category.SwipableView;
import com.android.gallery3d.filtershow.data.UserPresetsManager;
import com.android.gallery3d.filtershow.editors.BasicEditor;
import com.android.gallery3d.filtershow.editors.Editor;
import com.android.gallery3d.filtershow.editors.EditorChanSat;
import com.android.gallery3d.filtershow.editors.EditorColorBorder;
import com.android.gallery3d.filtershow.editors.EditorCrop;
import com.android.gallery3d.filtershow.editors.EditorDraw;
import com.android.gallery3d.filtershow.editors.EditorGrad;
import com.android.gallery3d.filtershow.editors.EditorManager;
import com.android.gallery3d.filtershow.editors.EditorMirror;
import com.android.gallery3d.filtershow.editors.EditorPanel;
import com.android.gallery3d.filtershow.editors.EditorRedEye;
import com.android.gallery3d.filtershow.editors.EditorRotate;
import com.android.gallery3d.filtershow.editors.EditorStraighten;
import com.android.gallery3d.filtershow.editors.EditorTinyPlanet;
import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.history.HistoryItem;
import com.android.gallery3d.filtershow.history.HistoryManager;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.imageshow.Spline;
import com.android.gallery3d.filtershow.info.InfoPanel;
import com.android.gallery3d.filtershow.pipeline.CachingPipeline;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.pipeline.ProcessingService;
import com.android.gallery3d.filtershow.presets.PresetManagementDialog;
import com.android.gallery3d.filtershow.presets.UserPresetsAdapter;
import com.android.gallery3d.filtershow.provider.SharedImageProvider;
import com.android.gallery3d.filtershow.state.StateAdapter;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.android.gallery3d.filtershow.tools.XmpPresets;
import com.android.gallery3d.filtershow.ui.ExportDialog;
import com.android.gallery3d.filtershow.ui.FramedTextButton;
import com.android.gallery3d.util.GalleryUtils;
import com.android.photos.data.GalleryBitmapPool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Vector;

public class FilterShowActivity extends FragmentActivity implements DialogInterface.OnDismissListener, DialogInterface.OnShowListener, AdapterView.OnItemClickListener, PopupMenu.OnDismissListener, ShareActionProvider.OnShareTargetSelectedListener {
    private ProcessingService mBoundService;
    private LoadBitmapTask mLoadBitmapTask;
    private Menu mMenu;
    private WeakReference<ProgressDialog> mSavingProgressDialog;
    private ShareActionProvider mShareActionProvider;
    private String mAction = "";
    MasterImage mMasterImage = null;
    private ImageShow mImageShow = null;
    private View mSaveButton = null;
    private EditorPlaceHolder mEditorPlaceHolder = new EditorPlaceHolder(this);
    private Editor mCurrentEditor = null;
    private boolean mShowingTinyPlanet = false;
    private boolean mShowingImageStatePanel = false;
    private boolean mShowingVersionsPanel = false;
    private final Vector<ImageShow> mImageViews = new Vector<>();
    private File mSharedOutputFile = null;
    private boolean mSharingImage = false;
    private Uri mOriginalImageUri = null;
    private ImagePreset mOriginalPreset = null;
    private Uri mSelectedImageUri = null;
    private ArrayList<Action> mActions = new ArrayList<>();
    private UserPresetsManager mUserPresetsManager = null;
    private UserPresetsAdapter mUserPresetsAdapter = null;
    private CategoryAdapter mCategoryLooksAdapter = null;
    private CategoryAdapter mCategoryBordersAdapter = null;
    private CategoryAdapter mCategoryGeometryAdapter = null;
    private CategoryAdapter mCategoryFiltersAdapter = null;
    private CategoryAdapter mCategoryVersionsAdapter = null;
    private int mCurrentPanel = 0;
    private Vector<FilterUserPresetRepresentation> mVersions = new Vector<>();
    private int mVersionsCounter = 0;
    private boolean mHandlingSwipeButton = false;
    private View mHandledSwipeView = null;
    private float mHandledSwipeViewLastDelta = 0.0f;
    private float mSwipeStartX = 0.0f;
    private float mSwipeStartY = 0.0f;
    private boolean mIsBound = false;
    private DialogInterface mCurrentDialog = null;
    private PopupMenu mCurrentMenu = null;
    private boolean mLoadingVisible = true;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            FilterShowActivity.this.mBoundService = ((ProcessingService.LocalBinder) service).getService();
            FilterShowActivity.this.mBoundService.setFiltershowActivity(FilterShowActivity.this);
            FilterShowActivity.this.mBoundService.onStart();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            FilterShowActivity.this.mBoundService = null;
        }
    };
    public Point mHintTouchPoint = new Point();

    public ProcessingService getProcessingService() {
        return this.mBoundService;
    }

    public boolean isSimpleEditAction() {
        return !"action_nextgen_edit".equalsIgnoreCase(this.mAction);
    }

    void doBindService() {
        bindService(new Intent(this, (Class<?>) ProcessingService.class), this.mConnection, 1);
        this.mIsBound = true;
    }

    void doUnbindService() {
        if (this.mIsBound) {
            unbindService(this.mConnection);
            this.mIsBound = false;
        }
    }

    public void updateUIAfterServiceStarted() {
        MasterImage.setMaster(this.mMasterImage);
        ImageFilter.setActivityForMemoryToasts(this);
        this.mUserPresetsManager = new UserPresetsManager(this);
        this.mUserPresetsAdapter = new UserPresetsAdapter(this);
        setupMasterImage();
        setupMenu();
        setDefaultValues();
        fillEditors();
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        loadXML();
        fillCategories();
        loadMainPanel();
        extractXMPData();
        processIntent();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean onlyUsePortrait = getResources().getBoolean(R.bool.only_use_portrait);
        if (onlyUsePortrait) {
            setRequestedOrientation(1);
        }
        clearGalleryBitmapPool();
        doBindService();
        getWindow().setBackgroundDrawable(new ColorDrawable(-7829368));
        setContentView(R.layout.filtershow_splashscreen);
    }

    public boolean isShowingImageStatePanel() {
        return this.mShowingImageStatePanel;
    }

    public void loadMainPanel() {
        if (findViewById(R.id.main_panel_container) != null) {
            MainPanel panel = new MainPanel();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_panel_container, panel, "MainPanel");
            transaction.commitAllowingStateLoss();
        }
    }

    public void loadEditorPanel(FilterRepresentation representation, Editor currentEditor) {
        if (representation.getEditorId() == R.id.imageOnlyEditor) {
            currentEditor.reflectCurrentFilter();
            return;
        }
        final int currentId = currentEditor.getID();
        Runnable showEditor = new Runnable() {
            @Override
            public void run() {
                EditorPanel panel = new EditorPanel();
                panel.setEditor(currentId);
                FragmentTransaction transaction = FilterShowActivity.this.getSupportFragmentManager().beginTransaction();
                transaction.remove(FilterShowActivity.this.getSupportFragmentManager().findFragmentByTag("MainPanel"));
                transaction.replace(R.id.main_panel_container, panel, "MainPanel");
                transaction.commit();
            }
        };
        Fragment main = getSupportFragmentManager().findFragmentByTag("MainPanel");
        boolean doAnimation = false;
        if (this.mShowingImageStatePanel && getResources().getConfiguration().orientation == 1) {
            doAnimation = true;
        }
        if (doAnimation && main != null && (main instanceof MainPanel)) {
            MainPanel mainPanel = (MainPanel) main;
            View container = mainPanel.getView().findViewById(R.id.category_panel_container);
            View bottom = mainPanel.getView().findViewById(R.id.bottom_panel);
            int panelHeight = container.getHeight() + bottom.getHeight();
            ViewPropertyAnimator anim = mainPanel.getView().animate();
            anim.translationY(panelHeight).start();
            Handler handler = new Handler();
            handler.postDelayed(showEditor, anim.getDuration());
            return;
        }
        showEditor.run();
    }

    public void toggleInformationPanel() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        InfoPanel panel = new InfoPanel();
        panel.show(transaction, "InfoPanel");
    }

    private void loadXML() {
        setContentView(R.layout.filtershow_activity);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(16);
        actionBar.setCustomView(R.layout.filtershow_actionbar);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_screen)));
        this.mSaveButton = actionBar.getCustomView();
        this.mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterShowActivity.this.saveImage();
            }
        });
        this.mImageShow = (ImageShow) findViewById(R.id.imageShow);
        this.mImageViews.add(this.mImageShow);
        setupEditors();
        this.mEditorPlaceHolder.hide();
        this.mImageShow.attach();
        setupStatePanel();
    }

    public void fillCategories() {
        fillLooks();
        loadUserPresets();
        fillBorders();
        fillTools();
        fillEffects();
        fillVersions();
    }

    public void setupStatePanel() {
        MasterImage.getImage().setHistoryManager(this.mMasterImage.getHistory());
    }

    private void fillVersions() {
        if (this.mCategoryVersionsAdapter != null) {
            this.mCategoryVersionsAdapter.clear();
        }
        this.mCategoryVersionsAdapter = new CategoryAdapter(this);
        this.mCategoryVersionsAdapter.setShowAddButton(true);
    }

    public void registerAction(Action action) {
        if (!this.mActions.contains(action)) {
            this.mActions.add(action);
        }
    }

    private void loadActions() {
        for (int i = 0; i < this.mActions.size(); i++) {
            Action action = this.mActions.get(i);
            action.setImageFrame(new Rect(0, 0, 96, 96), 0);
        }
    }

    public void updateVersions() {
        this.mCategoryVersionsAdapter.clear();
        FilterUserPresetRepresentation originalRep = new FilterUserPresetRepresentation(getString(R.string.filtershow_version_original), new ImagePreset(), -1);
        this.mCategoryVersionsAdapter.add(new Action(this, originalRep, 0));
        ImagePreset current = new ImagePreset(MasterImage.getImage().getPreset());
        FilterUserPresetRepresentation currentRep = new FilterUserPresetRepresentation(getString(R.string.filtershow_version_current), current, -1);
        this.mCategoryVersionsAdapter.add(new Action(this, currentRep, 0));
        if (this.mVersions.size() > 0) {
            this.mCategoryVersionsAdapter.add(new Action(this, 3));
        }
        for (FilterUserPresetRepresentation rep : this.mVersions) {
            this.mCategoryVersionsAdapter.add(new Action(this, rep, 0, true));
        }
        this.mCategoryVersionsAdapter.notifyDataSetInvalidated();
    }

    public void addCurrentVersion() {
        ImagePreset current = new ImagePreset(MasterImage.getImage().getPreset());
        this.mVersionsCounter++;
        FilterUserPresetRepresentation rep = new FilterUserPresetRepresentation("" + this.mVersionsCounter, current, -1);
        this.mVersions.add(rep);
        updateVersions();
    }

    public void removeVersion(Action action) {
        this.mVersions.remove(action.getRepresentation());
        updateVersions();
    }

    public void removeLook(Action action) {
        FilterUserPresetRepresentation rep = (FilterUserPresetRepresentation) action.getRepresentation();
        if (rep != null) {
            this.mUserPresetsManager.delete(rep.getId());
            updateUserPresetsFromManager();
        }
    }

    private void fillEffects() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getEffects();
        if (this.mCategoryFiltersAdapter != null) {
            this.mCategoryFiltersAdapter.clear();
        }
        this.mCategoryFiltersAdapter = new CategoryAdapter(this);
        for (FilterRepresentation representation : filtersRepresentations) {
            if (representation.getTextId() != 0) {
                representation.setName(getString(representation.getTextId()));
            }
            this.mCategoryFiltersAdapter.add(new Action(this, representation));
        }
    }

    private void fillTools() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getTools();
        if (this.mCategoryGeometryAdapter != null) {
            this.mCategoryGeometryAdapter.clear();
        }
        this.mCategoryGeometryAdapter = new CategoryAdapter(this);
        boolean found = false;
        for (FilterRepresentation representation : filtersRepresentations) {
            this.mCategoryGeometryAdapter.add(new Action(this, representation));
            if (representation instanceof FilterDrawRepresentation) {
                found = true;
            }
        }
        if (!found) {
            Action action = new Action(this, new FilterDrawRepresentation());
            action.setIsDoubleAction(true);
            this.mCategoryGeometryAdapter.add(action);
        }
    }

    private void processIntent() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra("launch-fullscreen", false)) {
            getWindow().addFlags(1024);
        }
        this.mAction = intent.getAction();
        this.mSelectedImageUri = intent.getData();
        Uri loadUri = this.mSelectedImageUri;
        if (this.mOriginalImageUri != null) {
            loadUri = this.mOriginalImageUri;
        }
        if (loadUri != null) {
            startLoadBitmap(loadUri);
        } else {
            pickImage();
        }
    }

    private void setupEditors() {
        this.mEditorPlaceHolder.setContainer((FrameLayout) findViewById(R.id.editorContainer));
        EditorManager.addEditors(this.mEditorPlaceHolder);
        this.mEditorPlaceHolder.setOldViews(this.mImageViews);
    }

    private void fillEditors() {
        this.mEditorPlaceHolder.addEditor(new EditorChanSat());
        this.mEditorPlaceHolder.addEditor(new EditorGrad());
        this.mEditorPlaceHolder.addEditor(new EditorDraw());
        this.mEditorPlaceHolder.addEditor(new EditorColorBorder());
        this.mEditorPlaceHolder.addEditor(new BasicEditor());
        this.mEditorPlaceHolder.addEditor(new ImageOnlyEditor());
        this.mEditorPlaceHolder.addEditor(new EditorTinyPlanet());
        this.mEditorPlaceHolder.addEditor(new EditorRedEye());
        this.mEditorPlaceHolder.addEditor(new EditorCrop());
        this.mEditorPlaceHolder.addEditor(new EditorMirror());
        this.mEditorPlaceHolder.addEditor(new EditorRotate());
        this.mEditorPlaceHolder.addEditor(new EditorStraighten());
    }

    private void setDefaultValues() {
        Resources res = getResources();
        FramedTextButton.setTextSize((int) getPixelsFromDip(14.0f));
        FramedTextButton.setTrianglePadding((int) getPixelsFromDip(4.0f));
        FramedTextButton.setTriangleSize((int) getPixelsFromDip(10.0f));
        Drawable curveHandle = res.getDrawable(R.drawable.camera_crop);
        int curveHandleSize = (int) res.getDimension(R.dimen.crop_indicator_size);
        Spline.setCurveHandle(curveHandle, curveHandleSize);
        Spline.setCurveWidth((int) getPixelsFromDip(3.0f));
        this.mOriginalImageUri = null;
    }

    private void startLoadBitmap(Uri uri) {
        View imageShow = findViewById(R.id.imageShow);
        imageShow.setVisibility(4);
        startLoadingIndicator();
        this.mShowingTinyPlanet = false;
        this.mLoadBitmapTask = new LoadBitmapTask();
        this.mLoadBitmapTask.execute(uri);
    }

    private void fillBorders() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> borders = filtersManager.getBorders();
        for (int i = 0; i < borders.size(); i++) {
            FilterRepresentation filter = borders.get(i);
            filter.setName(getString(R.string.borders));
            if (i == 0) {
                filter.setName(getString(R.string.none));
            }
        }
        if (this.mCategoryBordersAdapter != null) {
            this.mCategoryBordersAdapter.clear();
        }
        this.mCategoryBordersAdapter = new CategoryAdapter(this);
        for (FilterRepresentation representation : borders) {
            if (representation.getTextId() != 0) {
                representation.setName(getString(representation.getTextId()));
            }
            this.mCategoryBordersAdapter.add(new Action(this, representation, 0));
        }
    }

    public UserPresetsAdapter getUserPresetsAdapter() {
        return this.mUserPresetsAdapter;
    }

    public CategoryAdapter getCategoryLooksAdapter() {
        return this.mCategoryLooksAdapter;
    }

    public CategoryAdapter getCategoryBordersAdapter() {
        return this.mCategoryBordersAdapter;
    }

    public CategoryAdapter getCategoryGeometryAdapter() {
        return this.mCategoryGeometryAdapter;
    }

    public CategoryAdapter getCategoryFiltersAdapter() {
        return this.mCategoryFiltersAdapter;
    }

    public CategoryAdapter getCategoryVersionsAdapter() {
        return this.mCategoryVersionsAdapter;
    }

    public void removeFilterRepresentation(FilterRepresentation filterRepresentation) {
        if (filterRepresentation != null) {
            ImagePreset oldPreset = MasterImage.getImage().getPreset();
            ImagePreset copy = new ImagePreset(oldPreset);
            copy.removeFilter(filterRepresentation);
            MasterImage.getImage().setPreset(copy, copy.getLastRepresentation(), true);
            if (MasterImage.getImage().getCurrentFilterRepresentation() == filterRepresentation) {
                FilterRepresentation lastRepresentation = copy.getLastRepresentation();
                MasterImage.getImage().setCurrentFilterRepresentation(lastRepresentation);
            }
        }
    }

    public void useFilterRepresentation(FilterRepresentation filterRepresentation) {
        if (filterRepresentation != null) {
            if ((filterRepresentation instanceof FilterRotateRepresentation) || (filterRepresentation instanceof FilterMirrorRepresentation) || MasterImage.getImage().getCurrentFilterRepresentation() != filterRepresentation) {
                if ((filterRepresentation instanceof FilterUserPresetRepresentation) || (filterRepresentation instanceof FilterRotateRepresentation) || (filterRepresentation instanceof FilterMirrorRepresentation)) {
                    MasterImage.getImage().onNewLook(filterRepresentation);
                }
                ImagePreset oldPreset = MasterImage.getImage().getPreset();
                ImagePreset copy = new ImagePreset(oldPreset);
                FilterRepresentation representation = copy.getRepresentation(filterRepresentation);
                if (representation == null) {
                    filterRepresentation = filterRepresentation.copy();
                    copy.addFilter(filterRepresentation);
                } else if (filterRepresentation.allowsSingleInstanceOnly() && !representation.equals(filterRepresentation)) {
                    copy.removeFilter(representation);
                    copy.addFilter(filterRepresentation);
                }
                MasterImage.getImage().setPreset(copy, filterRepresentation, true);
                MasterImage.getImage().setCurrentFilterRepresentation(filterRepresentation);
            }
        }
    }

    public void showRepresentation(FilterRepresentation representation) {
        if (representation != null) {
            if (representation instanceof FilterRotateRepresentation) {
                FilterRotateRepresentation r = (FilterRotateRepresentation) representation;
                r.rotateCW();
            }
            if (representation instanceof FilterMirrorRepresentation) {
                FilterMirrorRepresentation r2 = (FilterMirrorRepresentation) representation;
                r2.cycle();
            }
            if (representation.isBooleanFilter()) {
                ImagePreset preset = MasterImage.getImage().getPreset();
                if (preset.getRepresentation(representation) != null) {
                    ImagePreset copy = new ImagePreset(preset);
                    copy.removeFilter(representation);
                    FilterRepresentation filterRepresentation = representation.copy();
                    MasterImage.getImage().setPreset(copy, filterRepresentation, true);
                    MasterImage.getImage().setCurrentFilterRepresentation(null);
                    return;
                }
            }
            useFilterRepresentation(representation);
            if (this.mCurrentEditor != null) {
                this.mCurrentEditor.detach();
            }
            this.mCurrentEditor = this.mEditorPlaceHolder.showEditor(representation.getEditorId());
            if (this.mCurrentEditor != null) {
                loadEditorPanel(representation, this.mCurrentEditor);
            }
        }
    }

    public Editor getEditor(int editorID) {
        return this.mEditorPlaceHolder.getEditor(editorID);
    }

    public void setCurrentPanel(int currentPanel) {
        this.mCurrentPanel = currentPanel;
    }

    public int getCurrentPanel() {
        return this.mCurrentPanel;
    }

    public void updateCategories() {
        if (this.mMasterImage != null) {
            ImagePreset preset = this.mMasterImage.getPreset();
            this.mCategoryLooksAdapter.reflectImagePreset(preset);
            this.mCategoryBordersAdapter.reflectImagePreset(preset);
        }
    }

    public View getMainStatePanelContainer(int id) {
        return findViewById(id);
    }

    public void onShowMenu(PopupMenu menu) {
        this.mCurrentMenu = menu;
        menu.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(PopupMenu popupMenu) {
        if (this.mCurrentMenu != null) {
            this.mCurrentMenu.setOnDismissListener(null);
            this.mCurrentMenu = null;
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        this.mCurrentDialog = dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        this.mCurrentDialog = null;
    }

    private class LoadHighresBitmapTask extends AsyncTask<Void, Void, Boolean> {
        private LoadHighresBitmapTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            MasterImage master = MasterImage.getImage();
            Rect originalBounds = master.getOriginalBounds();
            if (master.supportsHighRes()) {
                int highresPreviewSize = master.getOriginalBitmapLarge().getWidth() * 2;
                if (highresPreviewSize > originalBounds.width()) {
                    highresPreviewSize = originalBounds.width();
                }
                Rect bounds = new Rect();
                Bitmap originalHires = ImageLoader.loadOrientedConstrainedBitmap(master.getUri(), master.getActivity(), highresPreviewSize, master.getOrientation(), bounds);
                master.setOriginalBounds(bounds);
                master.setOriginalBitmapHighres(originalHires);
                FilterShowActivity.this.mBoundService.setOriginalBitmapHighres(originalHires);
                master.warnListeners();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Bitmap highresBitmap = MasterImage.getImage().getOriginalBitmapHighres();
            if (highresBitmap != null) {
                float highResPreviewScale = highresBitmap.getWidth() / MasterImage.getImage().getOriginalBounds().width();
                FilterShowActivity.this.mBoundService.setHighresPreviewScaleFactor(highResPreviewScale);
            }
            MasterImage.getImage().warnListeners();
        }
    }

    public boolean isLoadingVisible() {
        return this.mLoadingVisible;
    }

    public void startLoadingIndicator() {
        View loading = findViewById(R.id.loading);
        this.mLoadingVisible = true;
        loading.setVisibility(0);
    }

    public void stopLoadingIndicator() {
        View loading = findViewById(R.id.loading);
        loading.setVisibility(8);
        this.mLoadingVisible = false;
    }

    private class LoadBitmapTask extends AsyncTask<Uri, Boolean, Boolean> {
        int mBitmapSize;

        public LoadBitmapTask() {
            this.mBitmapSize = FilterShowActivity.this.getScreenImageSize();
        }

        @Override
        protected Boolean doInBackground(Uri... params) {
            if (!MasterImage.getImage().loadBitmap(params[0], this.mBitmapSize)) {
                return false;
            }
            publishProgress(Boolean.valueOf(ImageLoader.queryLightCycle360(MasterImage.getImage().getActivity())));
            return true;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            super.onProgressUpdate((Object[]) values);
            if (!isCancelled() && values[0].booleanValue()) {
                FilterShowActivity.this.mShowingTinyPlanet = true;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            MasterImage.setMaster(FilterShowActivity.this.mMasterImage);
            if (!isCancelled()) {
                if (!result.booleanValue()) {
                    if (FilterShowActivity.this.mOriginalImageUri != null && !FilterShowActivity.this.mOriginalImageUri.equals(FilterShowActivity.this.mSelectedImageUri)) {
                        FilterShowActivity.this.mOriginalImageUri = FilterShowActivity.this.mSelectedImageUri;
                        FilterShowActivity.this.mOriginalPreset = null;
                        Toast.makeText(FilterShowActivity.this, R.string.cannot_edit_original, 0).show();
                        FilterShowActivity.this.startLoadBitmap(FilterShowActivity.this.mOriginalImageUri);
                        return;
                    }
                    FilterShowActivity.this.cannotLoadImage();
                    return;
                }
                if (CachingPipeline.getRenderScriptContext() == null) {
                    Log.v("FilterShowActivity", "RenderScript context destroyed during load");
                    return;
                }
                View imageShow = FilterShowActivity.this.findViewById(R.id.imageShow);
                imageShow.setVisibility(0);
                Bitmap largeBitmap = MasterImage.getImage().getOriginalBitmapLarge();
                FilterShowActivity.this.mBoundService.setOriginalBitmap(largeBitmap);
                float previewScale = largeBitmap.getWidth() / MasterImage.getImage().getOriginalBounds().width();
                FilterShowActivity.this.mBoundService.setPreviewScaleFactor(previewScale);
                if (!FilterShowActivity.this.mShowingTinyPlanet) {
                    FilterShowActivity.this.mCategoryFiltersAdapter.removeTinyPlanet();
                }
                FilterShowActivity.this.mCategoryLooksAdapter.imageLoaded();
                FilterShowActivity.this.mCategoryBordersAdapter.imageLoaded();
                FilterShowActivity.this.mCategoryGeometryAdapter.imageLoaded();
                FilterShowActivity.this.mCategoryFiltersAdapter.imageLoaded();
                FilterShowActivity.this.mLoadBitmapTask = null;
                MasterImage.getImage().warnListeners();
                FilterShowActivity.this.loadActions();
                if (FilterShowActivity.this.mOriginalPreset != null) {
                    MasterImage.getImage().setLoadedPreset(FilterShowActivity.this.mOriginalPreset);
                    MasterImage.getImage().setPreset(FilterShowActivity.this.mOriginalPreset, FilterShowActivity.this.mOriginalPreset.getLastRepresentation(), true);
                    FilterShowActivity.this.mOriginalPreset = null;
                } else {
                    FilterShowActivity.this.setDefaultPreset();
                }
                MasterImage.getImage().resetGeometryImages(true);
                if (FilterShowActivity.this.mAction == "com.android.camera.action.TINY_PLANET") {
                    FilterShowActivity.this.showRepresentation(FilterShowActivity.this.mCategoryFiltersAdapter.getTinyPlanet());
                }
                LoadHighresBitmapTask highresLoad = new LoadHighresBitmapTask();
                highresLoad.execute(new Void[0]);
                MasterImage.getImage().warnListeners();
                super.onPostExecute(result);
            }
        }
    }

    private void clearGalleryBitmapPool() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GalleryBitmapPool.getInstance().clear();
                return null;
            }
        }.execute(new Void[0]);
    }

    @Override
    protected void onDestroy() {
        if (this.mLoadBitmapTask != null) {
            this.mLoadBitmapTask.cancel(false);
        }
        this.mUserPresetsManager.close();
        doUnbindService();
        super.onDestroy();
    }

    private int getScreenImageSize() {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        return Math.max(outMetrics.heightPixels, outMetrics.widthPixels);
    }

    private void showSavingProgress(String albumName) {
        String progressText;
        ProgressDialog progress;
        if (this.mSavingProgressDialog != null && (progress = this.mSavingProgressDialog.get()) != null) {
            progress.show();
            return;
        }
        if (albumName == null) {
            progressText = getString(R.string.saving_image);
        } else {
            progressText = getString(R.string.filtershow_saving_image, new Object[]{albumName});
        }
        this.mSavingProgressDialog = new WeakReference<>(ProgressDialog.show(this, "", progressText, true, false));
    }

    private void hideSavingProgress() {
        ProgressDialog progress;
        if (this.mSavingProgressDialog != null && (progress = this.mSavingProgressDialog.get()) != null) {
            progress.dismiss();
        }
    }

    public void completeSaveImage(Uri saveUri) {
        if (this.mSharingImage && this.mSharedOutputFile != null) {
            Uri uri = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI, Uri.encode(this.mSharedOutputFile.getAbsolutePath()));
            ContentValues values = new ContentValues();
            values.put("prepare", (Boolean) false);
            getContentResolver().insert(uri, values);
        }
        setResult(-1, new Intent().setData(saveUri));
        hideSavingProgress();
        finish();
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider arg0, Intent arg1) {
        Uri uri = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI, Uri.encode(this.mSharedOutputFile.getAbsolutePath()));
        ContentValues values = new ContentValues();
        values.put("prepare", (Boolean) true);
        getContentResolver().insert(uri, values);
        this.mSharingImage = true;
        showSavingProgress(null);
        this.mImageShow.saveImage(this, this.mSharedOutputFile);
        return true;
    }

    private Intent getDefaultShareIntent() {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.addFlags(524288);
        intent.addFlags(1);
        intent.setType("image/jpeg");
        this.mSharedOutputFile = SaveImage.getNewFile(this, MasterImage.getImage().getUri());
        Uri uri = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI, Uri.encode(this.mSharedOutputFile.getAbsolutePath()));
        intent.putExtra("android.intent.extra.STREAM", uri);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filtershow_activity_menu, menu);
        MenuItem showState = menu.findItem(R.id.showImageStateButton);
        if (this.mShowingImageStatePanel) {
            showState.setTitle(R.string.hide_imagestate_panel);
        } else {
            showState.setTitle(R.string.show_imagestate_panel);
        }
        this.mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
        this.mShareActionProvider.setShareIntent(getDefaultShareIntent());
        this.mShareActionProvider.setOnShareTargetSelectedListener(this);
        this.mMenu = menu;
        setupMenu();
        return true;
    }

    private void setupMenu() {
        if (this.mMenu != null && this.mMasterImage != null) {
            MenuItem undoItem = this.mMenu.findItem(R.id.undoButton);
            MenuItem redoItem = this.mMenu.findItem(R.id.redoButton);
            MenuItem resetItem = this.mMenu.findItem(R.id.resetHistoryButton);
            MenuItem printItem = this.mMenu.findItem(R.id.printButton);
            if (!PrintHelper.systemSupportsPrint()) {
                printItem.setVisible(false);
            }
            this.mMasterImage.getHistory().setMenuItems(undoItem, redoItem, resetItem);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mShareActionProvider != null) {
            this.mShareActionProvider.setOnShareTargetSelectedListener(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mShareActionProvider != null) {
            this.mShareActionProvider.setOnShareTargetSelectedListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                saveImage();
                break;
            case R.id.undoButton:
                HistoryManager adapter = this.mMasterImage.getHistory();
                int position = adapter.undo();
                this.mMasterImage.onHistoryItemClick(position);
                backToMain();
                invalidateViews();
                break;
            case R.id.redoButton:
                HistoryManager adapter2 = this.mMasterImage.getHistory();
                int position2 = adapter2.redo();
                this.mMasterImage.onHistoryItemClick(position2);
                invalidateViews();
                break;
            case R.id.resetHistoryButton:
                resetHistory();
                break;
            case R.id.showInfoPanel:
                toggleInformationPanel();
                break;
            case R.id.showImageStateButton:
                toggleImageStatePanel();
                break;
            case R.id.manageUserPresets:
                manageUserPresets();
                break;
            case R.id.exportFlattenButton:
                showExportOptionsDialog();
                break;
            case R.id.printButton:
                print();
                break;
        }
        return true;
    }

    public void print() {
        Bitmap bitmap = MasterImage.getImage().getHighresImage();
        PrintHelper printer = new PrintHelper(this);
        printer.printBitmap("ImagePrint", bitmap);
    }

    public void addNewPreset() {
        DialogFragment dialog = new PresetManagementDialog();
        dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
    }

    private void manageUserPresets() {
        DialogFragment dialog = new PresetManagementDialog();
        dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
    }

    private void showExportOptionsDialog() {
        DialogFragment dialog = new ExportDialog();
        dialog.show(getSupportFragmentManager(), "ExportDialogFragment");
    }

    public void updateUserPresetsFromAdapter(UserPresetsAdapter adapter) {
        ArrayList<FilterUserPresetRepresentation> representations = adapter.getDeletedRepresentations();
        for (FilterUserPresetRepresentation representation : representations) {
            deletePreset(representation.getId());
        }
        ArrayList<FilterUserPresetRepresentation> changedRepresentations = adapter.getChangedRepresentations();
        for (FilterUserPresetRepresentation representation2 : changedRepresentations) {
            updatePreset(representation2);
        }
        adapter.clearDeletedRepresentations();
        adapter.clearChangedRepresentations();
        loadUserPresets();
    }

    public void loadUserPresets() {
        this.mUserPresetsManager.load();
        updateUserPresetsFromManager();
    }

    public void updateUserPresetsFromManager() {
        ArrayList<FilterUserPresetRepresentation> presets = this.mUserPresetsManager.getRepresentations();
        if (presets != null) {
            if (this.mCategoryLooksAdapter != null) {
                fillLooks();
            }
            if (presets.size() > 0) {
                this.mCategoryLooksAdapter.add(new Action(this, 3));
            }
            this.mUserPresetsAdapter.clear();
            for (int i = 0; i < presets.size(); i++) {
                FilterUserPresetRepresentation representation = presets.get(i);
                this.mCategoryLooksAdapter.add(new Action(this, representation, 0, true));
                this.mUserPresetsAdapter.add(new Action(this, representation, 0));
            }
            if (presets.size() > 0) {
                this.mCategoryLooksAdapter.add(new Action(this, 2));
            }
            this.mCategoryLooksAdapter.notifyDataSetChanged();
            this.mCategoryLooksAdapter.notifyDataSetInvalidated();
        }
    }

    public void saveCurrentImagePreset(String name) {
        this.mUserPresetsManager.save(MasterImage.getImage().getPreset(), name);
    }

    private void deletePreset(int id) {
        this.mUserPresetsManager.delete(id);
    }

    private void updatePreset(FilterUserPresetRepresentation representation) {
        this.mUserPresetsManager.update(representation);
    }

    public void enableSave(boolean enable) {
        if (this.mSaveButton != null) {
            this.mSaveButton.setEnabled(enable);
        }
    }

    private void fillLooks() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getLooks();
        if (this.mCategoryLooksAdapter != null) {
            this.mCategoryLooksAdapter.clear();
        }
        this.mCategoryLooksAdapter = new CategoryAdapter(this);
        int verticalItemHeight = (int) getResources().getDimension(R.dimen.action_item_height);
        this.mCategoryLooksAdapter.setItemHeight(verticalItemHeight);
        for (FilterRepresentation representation : filtersRepresentations) {
            this.mCategoryLooksAdapter.add(new Action(this, representation, 0));
        }
        if (this.mUserPresetsManager.getRepresentations() == null || this.mUserPresetsManager.getRepresentations().size() == 0) {
            this.mCategoryLooksAdapter.add(new Action(this, 2));
        }
        Fragment panel = getSupportFragmentManager().findFragmentByTag("MainPanel");
        if (panel != null && (panel instanceof MainPanel)) {
            MainPanel mainPanel = (MainPanel) panel;
            mainPanel.loadCategoryLookPanel(true);
        }
    }

    public void setDefaultPreset() {
        ImagePreset preset = new ImagePreset();
        this.mMasterImage.setPreset(preset, preset.getLastRepresentation(), true);
    }

    public void invalidateViews() {
        for (ImageShow views : this.mImageViews) {
            views.updateImage();
        }
    }

    public void toggleImageStatePanel() {
        invalidateOptionsMenu();
        this.mShowingImageStatePanel = !this.mShowingImageStatePanel;
        Fragment panel = getSupportFragmentManager().findFragmentByTag("MainPanel");
        if (panel != null) {
            if (panel instanceof EditorPanel) {
                EditorPanel editorPanel = (EditorPanel) panel;
                editorPanel.showImageStatePanel(this.mShowingImageStatePanel);
            } else if (panel instanceof MainPanel) {
                MainPanel mainPanel = (MainPanel) panel;
                mainPanel.showImageStatePanel(this.mShowingImageStatePanel);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setDefaultValues();
        if (this.mMasterImage != null) {
            loadXML();
            fillCategories();
            loadMainPanel();
            if (this.mCurrentMenu != null) {
                this.mCurrentMenu.dismiss();
                this.mCurrentMenu = null;
            }
            if (this.mCurrentDialog != null) {
                this.mCurrentDialog.dismiss();
                this.mCurrentDialog = null;
            }
            if (!this.mShowingTinyPlanet && this.mLoadBitmapTask == null) {
                this.mCategoryFiltersAdapter.removeTinyPlanet();
            }
            stopLoadingIndicator();
        }
    }

    public void setupMasterImage() {
        HistoryManager historyManager = new HistoryManager();
        StateAdapter imageStateAdapter = new StateAdapter(this, 0);
        MasterImage.reset();
        this.mMasterImage = MasterImage.getImage();
        this.mMasterImage.setHistoryManager(historyManager);
        this.mMasterImage.setStateAdapter(imageStateAdapter);
        this.mMasterImage.setActivity(this);
        if (Runtime.getRuntime().maxMemory() > 134217728) {
            this.mMasterImage.setSupportsHighRes(true);
        } else {
            this.mMasterImage.setSupportsHighRes(false);
        }
    }

    void resetHistory() {
        HistoryManager adapter = this.mMasterImage.getHistory();
        adapter.reset();
        HistoryItem historyItem = adapter.getItem(0);
        ImagePreset original = new ImagePreset();
        FilterRepresentation rep = null;
        if (historyItem != null) {
            rep = historyItem.getFilterRepresentation();
        }
        this.mMasterImage.setPreset(original, rep, true);
        invalidateViews();
        backToMain();
    }

    public void showDefaultImageView() {
        this.mEditorPlaceHolder.hide();
        this.mImageShow.setVisibility(0);
        MasterImage.getImage().setCurrentFilter(null);
        MasterImage.getImage().setCurrentFilterRepresentation(null);
    }

    public void backToMain() {
        Fragment currentPanel = getSupportFragmentManager().findFragmentByTag("MainPanel");
        if (!(currentPanel instanceof MainPanel)) {
            loadMainPanel();
            showDefaultImageView();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment currentPanel = getSupportFragmentManager().findFragmentByTag("MainPanel");
        if (currentPanel instanceof MainPanel) {
            if (!this.mImageShow.hasModifications()) {
                done();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.unsaved).setTitle(R.string.save_before_exit);
            builder.setPositiveButton(R.string.save_and_exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    FilterShowActivity.this.saveImage();
                }
            });
            builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    FilterShowActivity.this.done();
                }
            });
            builder.show();
            return;
        }
        backToMain();
    }

    public void cannotLoadImage() {
        Toast.makeText(this, R.string.cannot_load_image, 0).show();
        finish();
    }

    public float getPixelsFromDip(float value) {
        Resources r = getResources();
        return TypedValue.applyDimension(1, value, r.getDisplayMetrics());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.mMasterImage.onHistoryItemClick(position);
        invalidateViews();
    }

    public void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction("android.intent.action.GET_CONTENT");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1 && requestCode == 1) {
            Uri selectedImageUri = data.getData();
            startLoadBitmap(selectedImageUri);
        }
    }

    public void saveImage() {
        if (this.mImageShow.hasModifications()) {
            InputStream is = null;
            boolean error = false;
            try {
                is = getContentResolver().openInputStream(this.mSelectedImageUri);
            } catch (FileNotFoundException e) {
                Log.w("FilterShowActivity", "FileNotFoundException ");
                error = true;
            } finally {
                Utils.closeSilently(is);
            }
            if (error) {
                Toast.makeText(this, R.string.edit_save_error, 0).show();
                done();
                return;
            }
            File saveDir = SaveImage.getFinalSaveDirectory(this, this.mSelectedImageUri);
            int bucketId = GalleryUtils.getBucketId(saveDir.getPath());
            String albumName = LocalAlbum.getLocalizedName(getResources(), bucketId, null);
            showSavingProgress(albumName);
            File mod_filename = SaveImage.getNewFile(this, this.mSelectedImageUri);
            this.mImageShow.saveImage(this, mod_filename);
            return;
        }
        done();
    }

    public void done() {
        hideSavingProgress();
        if (this.mLoadBitmapTask != null) {
            this.mLoadBitmapTask.cancel(false);
        }
        finish();
    }

    private void extractXMPData() {
        XmpPresets.XMresults res = XmpPresets.extractXMPData(getBaseContext(), this.mMasterImage, getIntent().getData());
        if (res != null) {
            this.mOriginalImageUri = res.originalimage;
            this.mOriginalPreset = res.preset;
        }
    }

    public Uri getSelectedImageUri() {
        return this.mSelectedImageUri;
    }

    public void setHandlesSwipeForView(View view, float startX, float startY) {
        if (view != null) {
            this.mHandlingSwipeButton = true;
        } else {
            this.mHandlingSwipeButton = false;
        }
        this.mHandledSwipeView = view;
        int[] location = new int[2];
        view.getLocationInWindow(location);
        this.mSwipeStartX = location[0] + startX;
        this.mSwipeStartY = location[1] + startY;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mHandlingSwipeButton) {
            int direction = 1;
            if (this.mHandledSwipeView instanceof CategoryView) {
                direction = ((CategoryView) this.mHandledSwipeView).getOrientation();
            }
            if (ev.getActionMasked() == 2) {
                float delta = ev.getY() - this.mSwipeStartY;
                float distance = this.mHandledSwipeView.getHeight();
                if (direction == 0) {
                    delta = ev.getX() - this.mSwipeStartX;
                    this.mHandledSwipeView.setTranslationX(delta);
                    distance = this.mHandledSwipeView.getWidth();
                } else {
                    this.mHandledSwipeView.setTranslationY(delta);
                }
                float delta2 = Math.abs(delta);
                float transparency = Math.min(1.0f, delta2 / distance);
                this.mHandledSwipeView.setAlpha(1.0f - transparency);
                this.mHandledSwipeViewLastDelta = delta2;
            }
            if (ev.getActionMasked() == 3 || ev.getActionMasked() == 1) {
                this.mHandledSwipeView.setTranslationX(0.0f);
                this.mHandledSwipeView.setTranslationY(0.0f);
                this.mHandledSwipeView.setAlpha(1.0f);
                this.mHandlingSwipeButton = false;
                float distance2 = this.mHandledSwipeView.getHeight();
                if (direction == 0) {
                    distance2 = this.mHandledSwipeView.getWidth();
                }
                if (this.mHandledSwipeViewLastDelta > distance2) {
                    ((SwipableView) this.mHandledSwipeView).delete();
                }
            }
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    public Point hintTouchPoint(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = this.mHintTouchPoint.x - location[0];
        int y = this.mHintTouchPoint.y - location[1];
        return new Point(x, y);
    }

    public void startTouchAnimation(View target, float x, float y) {
        final CategorySelected hint = (CategorySelected) findViewById(R.id.categorySelectedIndicator);
        int[] location = new int[2];
        target.getLocationOnScreen(location);
        this.mHintTouchPoint.x = (int) (location[0] + x);
        this.mHintTouchPoint.y = (int) (location[1] + y);
        int[] locationHint = new int[2];
        ((View) hint.getParent()).getLocationOnScreen(locationHint);
        int dx = (int) (x - (hint.getWidth() / 2));
        int dy = (int) (y - (hint.getHeight() / 2));
        hint.setTranslationX((location[0] - locationHint[0]) + dx);
        hint.setTranslationY((location[1] - locationHint[1]) + dy);
        hint.setVisibility(0);
        hint.animate().scaleX(2.0f).scaleY(2.0f).alpha(0.0f).withEndAction(new Runnable() {
            @Override
            public void run() {
                hint.setVisibility(4);
                hint.setScaleX(1.0f);
                hint.setScaleY(1.0f);
                hint.setAlpha(1.0f);
            }
        });
    }
}
