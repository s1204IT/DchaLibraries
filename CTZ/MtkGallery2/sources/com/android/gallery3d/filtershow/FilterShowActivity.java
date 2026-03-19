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
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.print.PrintHelper;
import android.util.DisplayMetrics;
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
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.util.PermissionHelper;
import com.mediatek.galleryportable.SystemPropertyUtils;
import com.mediatek.plugin.preload.SoOperater;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class FilterShowActivity extends FragmentActivity implements DialogInterface.OnDismissListener, DialogInterface.OnShowListener, AdapterView.OnItemClickListener, PopupMenu.OnDismissListener, ShareActionProvider.OnShareTargetSelectedListener {
    private String mAlbumNameForSaving;
    private ProcessingService mBoundService;
    private LoadBitmapTask mLoadBitmapTask;
    private Menu mMenu;
    private MainPanel mPreviousPanel;
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
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FilterShowActivity.this.mBoundService = ((ProcessingService.LocalBinder) iBinder).getService();
            FilterShowActivity.this.mBoundService.setFiltershowActivity(FilterShowActivity.this);
            FilterShowActivity.this.mBoundService.onStart();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            FilterShowActivity.this.mBoundService = null;
        }
    };
    public Point mHintTouchPoint = new Point();
    private boolean mIsNoneFilter = true;

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
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!PermissionHelper.checkForFilterShow(this)) {
            return;
        }
        if (getResources().getBoolean(R.bool.only_use_portrait)) {
            setRequestedOrientation(1);
        } else {
            setRequestedOrientation(14);
        }
        clearGalleryBitmapPool();
        doBindService();
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        setContentView(R.layout.filtershow_splashscreen);
    }

    public boolean isShowingImageStatePanel() {
        return this.mShowingImageStatePanel;
    }

    public void loadMainPanel() {
        if (findViewById(R.id.main_panel_container) == null) {
            return;
        }
        MainPanel mainPanel = new MainPanel();
        if (this.mPreviousPanel != null) {
            mainPanel.setPreviousToggleVersion(this.mPreviousPanel.getPreviousToggleVersion());
        }
        this.mPreviousPanel = mainPanel;
        FragmentTransaction fragmentTransactionBeginTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.main_panel_container, mainPanel, "MainPanel");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public void loadEditorPanel(FilterRepresentation filterRepresentation, Editor editor) {
        if (filterRepresentation.getEditorId() == R.id.imageOnlyEditor) {
            editor.reflectCurrentFilter();
            return;
        }
        final int id = editor.getID();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                EditorPanel editorPanel = new EditorPanel();
                editorPanel.setEditor(id);
                FragmentTransaction fragmentTransactionBeginTransaction = FilterShowActivity.this.getSupportFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.remove(FilterShowActivity.this.getSupportFragmentManager().findFragmentByTag("MainPanel"));
                fragmentTransactionBeginTransaction.replace(R.id.main_panel_container, editorPanel, "MainPanel");
                fragmentTransactionBeginTransaction.commitAllowingStateLoss();
            }
        };
        Fragment fragmentFindFragmentByTag = getSupportFragmentManager().findFragmentByTag("MainPanel");
        boolean z = false;
        if (this.mShowingImageStatePanel && getResources().getConfiguration().orientation == 1) {
            z = true;
        }
        if (z && fragmentFindFragmentByTag != null && (fragmentFindFragmentByTag instanceof MainPanel)) {
            int height = fragmentFindFragmentByTag.getView().findViewById(R.id.category_panel_container).getHeight() + fragmentFindFragmentByTag.getView().findViewById(R.id.bottom_panel).getHeight();
            ViewPropertyAnimator viewPropertyAnimatorAnimate = fragmentFindFragmentByTag.getView().animate();
            viewPropertyAnimatorAnimate.translationY(height).start();
            new Handler().postDelayed(runnable, viewPropertyAnimatorAnimate.getDuration());
            return;
        }
        runnable.run();
    }

    public void toggleInformationPanel() {
        if (MasterImage.getImage().getFilteredImage() == null) {
            return;
        }
        FragmentTransaction fragmentTransactionBeginTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        new InfoPanel().show(fragmentTransactionBeginTransaction, "InfoPanel");
    }

    private void loadXML() {
        View viewFindViewById;
        setContentView(R.layout.filtershow_activity);
        if (SystemPropertyUtils.get("prop.filtershow.imagetest").equals(SchemaSymbols.ATTVAL_TRUE_1) && (viewFindViewById = findViewById(R.id.mainView)) != null) {
            int i = SystemPropertyUtils.getInt("prop.filtershow.imagetest.color", 0);
            viewFindViewById.setBackgroundColor(Color.rgb(i, i, i));
        }
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
        if (this.mActions.contains(action)) {
            return;
        }
        this.mActions.add(action);
    }

    private void loadActions() {
        for (int i = 0; i < this.mActions.size(); i++) {
            this.mActions.get(i).setImageFrame(new Rect(0, 0, 96, 96), 0);
        }
    }

    public void updateVersions() {
        this.mCategoryVersionsAdapter.clear();
        this.mCategoryVersionsAdapter.add(new Action(this, new FilterUserPresetRepresentation(getString(R.string.filtershow_version_original), new ImagePreset(), -1), 0));
        this.mCategoryVersionsAdapter.add(new Action(this, new FilterUserPresetRepresentation(getString(R.string.filtershow_version_current), new ImagePreset(MasterImage.getImage().getPreset()), -1), 0));
        if (this.mVersions.size() > 0) {
            this.mCategoryVersionsAdapter.add(new Action(this, 3));
        }
        Vector vector = new Vector();
        Iterator<FilterUserPresetRepresentation> it = this.mVersions.iterator();
        while (it.hasNext()) {
            Action action = new Action(this, it.next(), 0, true);
            action.setAdapter(this.mCategoryVersionsAdapter);
            vector.add(action);
        }
        this.mCategoryVersionsAdapter.addAll(vector);
        this.mCategoryVersionsAdapter.notifyDataSetInvalidated();
    }

    public void addCurrentVersion() {
        ImagePreset imagePreset = new ImagePreset(MasterImage.getImage().getPreset());
        this.mVersionsCounter++;
        this.mVersions.add(new FilterUserPresetRepresentation("" + this.mVersionsCounter, imagePreset, -1));
        updateVersions();
    }

    public void removeVersion(Action action) {
        this.mVersions.remove(action.getRepresentation());
        updateVersions();
    }

    public void removeLook(Action action) {
        FilterUserPresetRepresentation filterUserPresetRepresentation = (FilterUserPresetRepresentation) action.getRepresentation();
        if (filterUserPresetRepresentation == null) {
            return;
        }
        this.mUserPresetsManager.delete(filterUserPresetRepresentation.getId());
        updateUserPresetsFromManager();
    }

    private void fillEffects() {
        ArrayList<FilterRepresentation> effects = FiltersManager.getManager().getEffects();
        if (this.mCategoryFiltersAdapter != null) {
            this.mCategoryFiltersAdapter.clear();
        }
        this.mCategoryFiltersAdapter = new CategoryAdapter(this);
        for (FilterRepresentation filterRepresentation : effects) {
            if (filterRepresentation.getTextId() != 0) {
                filterRepresentation.setName(getString(filterRepresentation.getTextId()));
            }
            this.mCategoryFiltersAdapter.add(new Action(this, filterRepresentation));
        }
    }

    private void fillTools() {
        ArrayList<FilterRepresentation> tools = FiltersManager.getManager().getTools();
        if (this.mCategoryGeometryAdapter != null) {
            this.mCategoryGeometryAdapter.clear();
        }
        this.mCategoryGeometryAdapter = new CategoryAdapter(this);
        boolean z = false;
        for (FilterRepresentation filterRepresentation : tools) {
            this.mCategoryGeometryAdapter.add(new Action(this, filterRepresentation));
            if (filterRepresentation instanceof FilterDrawRepresentation) {
                z = true;
            }
        }
        if (!z) {
            Action action = new Action(this, new FilterDrawRepresentation());
            action.setIsDoubleAction(true);
            this.mCategoryGeometryAdapter.add(action);
        }
    }

    private void processIntent() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra("launch-fullscreen", false)) {
            getWindow().addFlags(SoOperater.STEP);
        }
        this.mAction = intent.getAction();
        this.mSelectedImageUri = intent.getData();
        Uri uri = this.mSelectedImageUri;
        if (this.mOriginalImageUri != null) {
            uri = this.mOriginalImageUri;
        }
        if (uri != null) {
            startLoadBitmap(uri);
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
        Resources resources = getResources();
        FramedTextButton.setTextSize((int) getPixelsFromDip(14.0f));
        FramedTextButton.setTrianglePadding((int) getPixelsFromDip(4.0f));
        FramedTextButton.setTriangleSize((int) getPixelsFromDip(10.0f));
        Spline.setCurveHandle(resources.getDrawable(R.drawable.camera_crop), (int) resources.getDimension(R.dimen.crop_indicator_size));
        Spline.setCurveWidth((int) getPixelsFromDip(3.0f));
        this.mOriginalImageUri = null;
    }

    private void startLoadBitmap(Uri uri) {
        findViewById(R.id.imageShow).setVisibility(4);
        startLoadingIndicator();
        this.mShowingTinyPlanet = false;
        this.mLoadBitmapTask = new LoadBitmapTask();
        this.mLoadBitmapTask.execute(uri);
    }

    private void fillBorders() {
        ArrayList<FilterRepresentation> borders = FiltersManager.getManager().getBorders();
        if (this.mCategoryBordersAdapter != null) {
            this.mCategoryBordersAdapter.clear();
        }
        this.mCategoryBordersAdapter = new CategoryAdapter(this);
        for (FilterRepresentation filterRepresentation : borders) {
            filterRepresentation.setName("");
            this.mCategoryBordersAdapter.add(new Action(this, filterRepresentation, 0));
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
        if (filterRepresentation == null) {
            return;
        }
        ImagePreset imagePreset = new ImagePreset(MasterImage.getImage().getPreset());
        imagePreset.removeFilter(filterRepresentation);
        MasterImage.getImage().setPreset(imagePreset, imagePreset.getLastRepresentation(), true);
        if (MasterImage.getImage().getCurrentFilterRepresentation() == filterRepresentation) {
            MasterImage.getImage().setCurrentFilterRepresentation(imagePreset.getLastRepresentation());
        }
    }

    public void useFilterRepresentation(FilterRepresentation filterRepresentation) {
        if (filterRepresentation == null) {
            return;
        }
        boolean z = filterRepresentation instanceof FilterRotateRepresentation;
        if (!z && !(filterRepresentation instanceof FilterMirrorRepresentation) && MasterImage.getImage().getCurrentFilterRepresentation() == filterRepresentation) {
            return;
        }
        if ((filterRepresentation instanceof FilterUserPresetRepresentation) || z || (filterRepresentation instanceof FilterMirrorRepresentation)) {
            MasterImage.getImage().onNewLook(filterRepresentation);
        }
        ImagePreset imagePreset = new ImagePreset(MasterImage.getImage().getPreset());
        FilterRepresentation representation = imagePreset.getRepresentation(filterRepresentation);
        if (representation == null) {
            filterRepresentation = filterRepresentation.copy();
            imagePreset.addFilter(filterRepresentation);
        } else if (filterRepresentation.allowsSingleInstanceOnly() && !representation.equals(filterRepresentation)) {
            imagePreset.removeFilter(representation);
            imagePreset.addFilter(filterRepresentation.copy());
        }
        MasterImage.getImage().setPreset(imagePreset, filterRepresentation, true);
        MasterImage.getImage().setCurrentFilterRepresentation(filterRepresentation);
    }

    public void showRepresentation(FilterRepresentation filterRepresentation) {
        if (filterRepresentation == 0) {
            return;
        }
        this.mIsNoneFilter = false;
        if (filterRepresentation instanceof FilterRotateRepresentation) {
            filterRepresentation.rotateCW();
        }
        if (filterRepresentation instanceof FilterMirrorRepresentation) {
            filterRepresentation.cycle();
        }
        ImagePreset preset = MasterImage.getImage().getPreset();
        if (preset == null) {
            return;
        }
        if (filterRepresentation.isBooleanFilter() && preset.getRepresentation(filterRepresentation) != null) {
            ImagePreset imagePreset = new ImagePreset(preset);
            imagePreset.removeFilter(filterRepresentation);
            MasterImage.getImage().setPreset(imagePreset, filterRepresentation.copy(), true);
            MasterImage.getImage().setCurrentFilterRepresentation(null);
            return;
        }
        useFilterRepresentation(filterRepresentation);
        if (this.mCurrentEditor != null) {
            this.mCurrentEditor.detach();
        }
        this.mCurrentEditor = this.mEditorPlaceHolder.showEditor(filterRepresentation.getEditorId());
        loadEditorPanel(filterRepresentation, this.mCurrentEditor);
    }

    public Editor getEditor(int i) {
        return this.mEditorPlaceHolder.getEditor(i);
    }

    public void setCurrentPanel(int i) {
        this.mCurrentPanel = i;
    }

    public int getCurrentPanel() {
        return this.mCurrentPanel;
    }

    public void updateCategories() {
        if (this.mMasterImage == null) {
            return;
        }
        ImagePreset preset = this.mMasterImage.getPreset();
        this.mCategoryLooksAdapter.reflectImagePreset(preset);
        this.mCategoryBordersAdapter.reflectImagePreset(preset);
    }

    public View getMainStatePanelContainer(int i) {
        return findViewById(i);
    }

    public void onShowMenu(PopupMenu popupMenu) {
        this.mCurrentMenu = popupMenu;
        popupMenu.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(PopupMenu popupMenu) {
        if (this.mCurrentMenu == null) {
            return;
        }
        this.mCurrentMenu.setOnDismissListener(null);
        this.mCurrentMenu = null;
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        this.mCurrentDialog = dialogInterface;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        this.mCurrentDialog = null;
    }

    private class LoadHighresBitmapTask extends AsyncTask<Void, Void, Boolean> {
        private LoadHighresBitmapTask() {
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            int iHeight;
            MasterImage image = MasterImage.getImage();
            Rect originalBounds = image.getOriginalBounds();
            if (image.supportsHighRes()) {
                int width = image.getOriginalBitmapLarge().getWidth() * 2;
                if (width > originalBounds.width()) {
                    width = originalBounds.width();
                }
                int height = image.getOriginalBitmapLarge().getHeight() * 2;
                if (height > originalBounds.height()) {
                    iHeight = originalBounds.height();
                } else {
                    iHeight = height;
                }
                if (iHeight <= width) {
                    iHeight = width;
                }
                Rect rect = new Rect();
                Bitmap bitmapLoadOrientedConstrainedBitmap = ImageLoader.loadOrientedConstrainedBitmap(image.getUri(), image.getActivity(), iHeight, image.getOrientation(), rect);
                image.setOriginalBounds(rect);
                image.setOriginalBitmapHighres(bitmapLoadOrientedConstrainedBitmap);
                FilterShowActivity.this.mBoundService.setOriginalBitmapHighres(bitmapLoadOrientedConstrainedBitmap);
                image.warnListeners();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (MasterImage.getImage().getOriginalBitmapHighres() != null) {
                FilterShowActivity.this.mBoundService.setHighresPreviewScaleFactor(r2.getWidth() / MasterImage.getImage().getOriginalBounds().width());
            }
            MasterImage.getImage().warnListeners();
        }
    }

    public boolean isLoadingVisible() {
        return this.mLoadingVisible;
    }

    public void startLoadingIndicator() {
        View viewFindViewById = findViewById(R.id.loading);
        this.mLoadingVisible = true;
        viewFindViewById.setVisibility(0);
    }

    public void stopLoadingIndicator() {
        findViewById(R.id.loading).setVisibility(8);
        this.mLoadingVisible = false;
    }

    private class LoadBitmapTask extends AsyncTask<Uri, Boolean, Boolean> {
        int mBitmapSize;

        public LoadBitmapTask() {
            this.mBitmapSize = FilterShowActivity.this.getScreenImageSize();
        }

        @Override
        protected Boolean doInBackground(Uri... uriArr) {
            if (!MasterImage.getImage().loadBitmap(uriArr[0], this.mBitmapSize)) {
                return false;
            }
            publishProgress(Boolean.valueOf(ImageLoader.queryLightCycle360(MasterImage.getImage().getActivity())));
            return true;
        }

        @Override
        protected void onProgressUpdate(Boolean... boolArr) {
            super.onProgressUpdate((Object[]) boolArr);
            if (!isCancelled() && boolArr[0].booleanValue()) {
                FilterShowActivity.this.mShowingTinyPlanet = true;
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            MasterImage.setMaster(FilterShowActivity.this.mMasterImage);
            if (isCancelled()) {
                return;
            }
            if (!bool.booleanValue()) {
                if (FilterShowActivity.this.mOriginalImageUri != null && !FilterShowActivity.this.mOriginalImageUri.equals(FilterShowActivity.this.mSelectedImageUri)) {
                    FilterShowActivity.this.mOriginalImageUri = FilterShowActivity.this.mSelectedImageUri;
                    FilterShowActivity.this.mOriginalPreset = null;
                    Toast.makeText(FilterShowActivity.this, R.string.cannot_edit_original, 0).show();
                    FilterShowActivity.this.startLoadBitmap(FilterShowActivity.this.mOriginalImageUri);
                    FilterShowActivity.this.mAlbumNameForSaving = FilterShowActivity.this.loadAlbumNameForSaving();
                    return;
                }
                FilterShowActivity.this.cannotLoadImage();
                return;
            }
            if (CachingPipeline.getRenderScriptContext() == null) {
                Log.v("FilterShowActivity", "RenderScript context destroyed during load");
                return;
            }
            FilterShowActivity.this.findViewById(R.id.imageShow).setVisibility(0);
            FilterShowActivity.this.mBoundService.setOriginalBitmap(MasterImage.getImage().getOriginalBitmapLarge());
            FilterShowActivity.this.mBoundService.setPreviewScaleFactor(r0.getWidth() / MasterImage.getImage().getOriginalBounds().width());
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
            new LoadHighresBitmapTask().execute(new Void[0]);
            MasterImage.getImage().warnListeners();
            FilterShowActivity.this.mAlbumNameForSaving = FilterShowActivity.this.loadAlbumNameForSaving();
            super.onPostExecute(bool);
        }
    }

    private void clearGalleryBitmapPool() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                GalleryBitmapPool.getInstance().clear();
                return null;
            }
        }.execute(new Void[0]);
    }

    @Override
    protected void onDestroy() {
        hideSavingProgress();
        if (this.mLoadBitmapTask != null) {
            this.mLoadBitmapTask.cancel(false);
        }
        if (this.mUserPresetsManager != null) {
            this.mUserPresetsManager.close();
        }
        doUnbindService();
        super.onDestroy();
    }

    private int getScreenImageSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels);
    }

    private void showSavingProgress(String str) {
        String string;
        ProgressDialog progressDialog;
        if (this.mSavingProgressDialog != null && (progressDialog = this.mSavingProgressDialog.get()) != null) {
            progressDialog.show();
            return;
        }
        if (str == null) {
            string = getString(R.string.saving_image);
        } else {
            string = getString(R.string.filtershow_saving_image, str);
        }
        this.mSavingProgressDialog = new WeakReference<>(ProgressDialog.show(this, "", string, true, false));
    }

    private void hideSavingProgress() {
        ProgressDialog progressDialog;
        if (this.mSavingProgressDialog != null && (progressDialog = this.mSavingProgressDialog.get()) != null) {
            progressDialog.dismiss();
        }
    }

    public void completeSaveImage(Uri uri) {
        if (this.mSharingImage && this.mSharedOutputFile != null) {
            Uri uriWithAppendedPath = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI, Uri.encode(this.mSharedOutputFile.getAbsolutePath()));
            ContentValues contentValues = new ContentValues();
            contentValues.put("prepare", (Boolean) false);
            getContentResolver().insert(uriWithAppendedPath, contentValues);
        }
        setResult(-1, new Intent().setData(uri));
        hideSavingProgress();
        finish();
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        Uri uriWithAppendedPath = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI, Uri.encode(this.mSharedOutputFile.getAbsolutePath()));
        ContentValues contentValues = new ContentValues();
        contentValues.put("prepare", (Boolean) true);
        getContentResolver().insert(uriWithAppendedPath, contentValues);
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
        intent.putExtra("android.intent.extra.STREAM", Uri.withAppendedPath(SharedImageProvider.CONTENT_URI, Uri.encode(this.mSharedOutputFile.getAbsolutePath())));
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filtershow_activity_menu, menu);
        MenuItem menuItemFindItem = menu.findItem(R.id.showImageStateButton);
        if (this.mShowingImageStatePanel) {
            menuItemFindItem.setTitle(R.string.hide_imagestate_panel);
        } else {
            menuItemFindItem.setTitle(R.string.show_imagestate_panel);
        }
        this.mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
        this.mShareActionProvider.setShareIntent(getDefaultShareIntent());
        this.mShareActionProvider.setOnShareTargetSelectedListener(this);
        this.mMenu = menu;
        setupMenu();
        return true;
    }

    private void setupMenu() {
        if (this.mMenu == null || this.mMasterImage == null) {
            return;
        }
        MenuItem menuItemFindItem = this.mMenu.findItem(R.id.undoButton);
        MenuItem menuItemFindItem2 = this.mMenu.findItem(R.id.redoButton);
        MenuItem menuItemFindItem3 = this.mMenu.findItem(R.id.resetHistoryButton);
        MenuItem menuItemFindItem4 = this.mMenu.findItem(R.id.printButton);
        if (!PrintHelper.systemSupportsPrint()) {
            menuItemFindItem4.setVisible(false);
        }
        this.mMasterImage.getHistory().setMenuItems(menuItemFindItem, menuItemFindItem2, menuItemFindItem3);
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
        Fragment fragmentFindFragmentByTag;
        super.onResume();
        if (this.mIsNoneFilter && (fragmentFindFragmentByTag = getSupportFragmentManager().findFragmentByTag("MainPanel")) != null && (fragmentFindFragmentByTag instanceof MainPanel) && this.mCurrentPanel == 0) {
            backToMain();
            Log.d("Gallery2/FilterShowActivity", "loadCategoryLookPanel on resuming use backToMain~");
        }
        if (this.mShareActionProvider != null) {
            this.mShareActionProvider.setOnShareTargetSelectedListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mLoadingVisible) {
            Log.d("Gallery2/FilterShowActivity", "<onOptionsItemSelected> mLoadingVisible = " + this.mLoadingVisible);
            return false;
        }
        int itemId = menuItem.getItemId();
        if (itemId != 16908332) {
            switch (itemId) {
                case R.id.undoButton:
                    if (this.mMasterImage != null) {
                        this.mMasterImage.onHistoryItemClick(this.mMasterImage.getHistory().undo());
                        backToMain();
                        invalidateViews();
                        break;
                    }
                    break;
                case R.id.redoButton:
                    if (this.mMasterImage != null) {
                        this.mMasterImage.onHistoryItemClick(this.mMasterImage.getHistory().redo());
                        invalidateViews();
                        break;
                    }
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
            return false;
        }
        saveImage();
        return true;
    }

    public void print() {
        new PrintHelper(this).printBitmap("ImagePrint", MasterImage.getImage().getHighresImage());
    }

    public void addNewPreset() {
        new PresetManagementDialog().show(getSupportFragmentManager(), "NoticeDialogFragment");
    }

    private void manageUserPresets() {
        new PresetManagementDialog().show(getSupportFragmentManager(), "NoticeDialogFragment");
    }

    private void showExportOptionsDialog() {
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        ImagePreset preset = MasterImage.getImage().getPreset();
        if (originalBounds == null || preset == null) {
            Log.d("Gallery2/FilterShowActivity", "  (mOriginalBounds == null || preset == null)");
        } else {
            new ExportDialog().show(getSupportFragmentManager(), "ExportDialogFragment");
        }
    }

    public void updateUserPresetsFromAdapter(UserPresetsAdapter userPresetsAdapter) {
        Iterator<FilterUserPresetRepresentation> it = userPresetsAdapter.getDeletedRepresentations().iterator();
        while (it.hasNext()) {
            deletePreset(it.next().getId());
        }
        Iterator<FilterUserPresetRepresentation> it2 = userPresetsAdapter.getChangedRepresentations().iterator();
        while (it2.hasNext()) {
            updatePreset(it2.next());
        }
        userPresetsAdapter.clearDeletedRepresentations();
        userPresetsAdapter.clearChangedRepresentations();
        loadUserPresets();
    }

    public void loadUserPresets() {
        this.mUserPresetsManager.load();
        updateUserPresetsFromManager();
    }

    public void updateUserPresetsFromManager() {
        ArrayList<FilterUserPresetRepresentation> representations = this.mUserPresetsManager.getRepresentations();
        if (representations == null) {
            return;
        }
        if (this.mCategoryLooksAdapter != null) {
            fillLooks();
        }
        if (representations.size() > 0) {
            this.mCategoryLooksAdapter.add(new Action(this, 3));
        }
        this.mUserPresetsAdapter.clear();
        for (int i = 0; i < representations.size(); i++) {
            FilterUserPresetRepresentation filterUserPresetRepresentation = representations.get(i);
            this.mCategoryLooksAdapter.add(new Action(this, filterUserPresetRepresentation, 0, true));
            this.mUserPresetsAdapter.add(new Action(this, filterUserPresetRepresentation, 0));
        }
        if (representations.size() > 0) {
            this.mCategoryLooksAdapter.add(new Action(this, 2));
        }
        this.mCategoryLooksAdapter.notifyDataSetChanged();
        this.mCategoryLooksAdapter.notifyDataSetInvalidated();
    }

    public void saveCurrentImagePreset(String str) {
        this.mUserPresetsManager.save(MasterImage.getImage().getPreset(), str);
    }

    private void deletePreset(int i) {
        this.mUserPresetsManager.delete(i);
    }

    private void updatePreset(FilterUserPresetRepresentation filterUserPresetRepresentation) {
        this.mUserPresetsManager.update(filterUserPresetRepresentation);
    }

    public void enableSave(boolean z) {
        if (this.mSaveButton != null) {
            this.mSaveButton.setEnabled(z);
        }
    }

    private void fillLooks() {
        ArrayList<FilterRepresentation> looks = FiltersManager.getManager().getLooks();
        if (this.mCategoryLooksAdapter != null) {
            this.mCategoryLooksAdapter.clear();
        }
        this.mCategoryLooksAdapter = new CategoryAdapter(this);
        this.mCategoryLooksAdapter.setItemHeight((int) getResources().getDimension(R.dimen.action_item_height));
        Iterator<FilterRepresentation> it = looks.iterator();
        while (it.hasNext()) {
            this.mCategoryLooksAdapter.add(new Action(this, it.next(), 0));
        }
        if (this.mUserPresetsManager.getRepresentations() == null || this.mUserPresetsManager.getRepresentations().size() == 0) {
            this.mCategoryLooksAdapter.add(new Action(this, 2));
        }
        ?? FindFragmentByTag = getSupportFragmentManager().findFragmentByTag("MainPanel");
        if (FindFragmentByTag != 0 && (FindFragmentByTag instanceof MainPanel)) {
            FindFragmentByTag.loadCategoryLookPanel(true);
        }
    }

    public void setDefaultPreset() {
        ImagePreset imagePreset = new ImagePreset();
        this.mMasterImage.setPreset(imagePreset, imagePreset.getLastRepresentation(), true);
    }

    public void invalidateViews() {
        Iterator<ImageShow> it = this.mImageViews.iterator();
        while (it.hasNext()) {
            it.next().updateImage();
        }
    }

    public void toggleImageStatePanel() {
        invalidateOptionsMenu();
        this.mShowingImageStatePanel = !this.mShowingImageStatePanel;
        ?? FindFragmentByTag = getSupportFragmentManager().findFragmentByTag("MainPanel");
        if (FindFragmentByTag != 0) {
            if (FindFragmentByTag instanceof EditorPanel) {
                FindFragmentByTag.showImageStatePanel(this.mShowingImageStatePanel);
            } else if (FindFragmentByTag instanceof MainPanel) {
                FindFragmentByTag.showImageStatePanel(this.mShowingImageStatePanel);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        setDefaultValues();
        if (this.mMasterImage == null) {
            return;
        }
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

    public void setupMasterImage() {
        HistoryManager historyManager = new HistoryManager();
        StateAdapter stateAdapter = new StateAdapter(this, 0);
        MasterImage.reset();
        this.mMasterImage = MasterImage.getImage();
        this.mMasterImage.setHistoryManager(historyManager);
        this.mMasterImage.setStateAdapter(stateAdapter);
        this.mMasterImage.setActivity(this);
        if (Runtime.getRuntime().maxMemory() > 134217728) {
            this.mMasterImage.setSupportsHighRes(true);
        } else {
            this.mMasterImage.setSupportsHighRes(false);
        }
        if (Runtime.getRuntime().maxMemory() <= 268435456 && getScreenImageSize() <= 1280) {
            this.mMasterImage.setSupportsHighRes(false);
        }
    }

    void resetHistory() {
        for (FilterRepresentation filterRepresentation : FiltersManager.getManager().getTools()) {
            if (filterRepresentation instanceof FilterRotateRepresentation) {
                filterRepresentation.setRotation(FilterRotateRepresentation.Rotation.ZERO);
            }
            if (filterRepresentation instanceof FilterMirrorRepresentation) {
                filterRepresentation.setMirror(FilterMirrorRepresentation.Mirror.NONE);
            }
        }
        HistoryManager history = this.mMasterImage.getHistory();
        history.reset();
        HistoryItem item = history.getItem(0);
        ImagePreset imagePreset = new ImagePreset();
        FilterRepresentation filterRepresentation2 = null;
        if (item != null) {
            filterRepresentation2 = item.getFilterRepresentation();
        }
        this.mMasterImage.setPreset(imagePreset, filterRepresentation2, true);
        invalidateViews();
        backToMain();
    }

    public void showDefaultImageView() {
        this.mEditorPlaceHolder.hide();
        if (this.mImageShow != null) {
            this.mImageShow.setVisibility(0);
        }
        MasterImage.getImage().setCurrentFilter(null);
        MasterImage.getImage().setCurrentFilterRepresentation(null);
    }

    public void backToMain() {
        if (getSupportFragmentManager().findFragmentByTag("MainPanel") instanceof MainPanel) {
            return;
        }
        loadMainPanel();
        showDefaultImageView();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().findFragmentByTag("MainPanel") instanceof MainPanel) {
            if (this.mImageShow == null || !this.mImageShow.hasModifications()) {
                done();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.unsaved).setTitle(R.string.save_before_exit);
            builder.setPositiveButton(R.string.save_and_exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    FilterShowActivity.this.saveImage();
                }
            });
            builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    FilterShowActivity.this.done();
                }
            });
            builder.show();
            return;
        }
        if (this.mCurrentEditor instanceof EditorCrop) {
            undoWhenBackToMain();
        }
        backToMain();
    }

    public void cannotLoadImage() {
        Toast.makeText(this, R.string.cannot_load_image, 0).show();
        finish();
    }

    public float getPixelsFromDip(float f) {
        return TypedValue.applyDimension(1, f, getResources().getDisplayMetrics());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        this.mMasterImage.onHistoryItemClick(i);
        invalidateViews();
    }

    public void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction("android.intent.action.GET_CONTENT");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), 1);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i2 == -1 && i == 1) {
            startLoadBitmap(intent.getData());
        }
    }

    public void saveImage() {
        if (this.mImageShow.hasModifications()) {
            showSavingProgress(this.mAlbumNameForSaving);
            this.mImageShow.saveImage(this, null);
        } else {
            done();
        }
    }

    public void done() {
        hideSavingProgress();
        if (this.mLoadBitmapTask != null) {
            this.mLoadBitmapTask.cancel(false);
        }
        finish();
    }

    private void extractXMPData() {
        XmpPresets.XMresults xMresultsExtractXMPData = XmpPresets.extractXMPData(getBaseContext(), this.mMasterImage, getIntent().getData());
        if (xMresultsExtractXMPData == null) {
            return;
        }
        this.mOriginalImageUri = xMresultsExtractXMPData.originalimage;
        this.mOriginalPreset = xMresultsExtractXMPData.preset;
    }

    public Uri getSelectedImageUri() {
        return this.mSelectedImageUri;
    }

    public void setHandlesSwipeForView(View view, float f, float f2) {
        if (view != null) {
            this.mHandlingSwipeButton = true;
        } else {
            this.mHandlingSwipeButton = false;
        }
        this.mHandledSwipeView = view;
        view.getLocationInWindow(new int[2]);
        this.mSwipeStartX = r2[0] + f;
        this.mSwipeStartY = r2[1] + f2;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        int orientation;
        if (this.mHandlingSwipeButton) {
            if (this.mHandledSwipeView instanceof CategoryView) {
                orientation = ((CategoryView) this.mHandledSwipeView).getOrientation();
            } else {
                orientation = 1;
            }
            if (motionEvent.getActionMasked() == 2) {
                float y = motionEvent.getY() - this.mSwipeStartY;
                float height = this.mHandledSwipeView.getHeight();
                if (orientation == 0) {
                    y = motionEvent.getX() - this.mSwipeStartX;
                    this.mHandledSwipeView.setTranslationX(y);
                    height = this.mHandledSwipeView.getWidth();
                } else {
                    this.mHandledSwipeView.setTranslationY(y);
                }
                float fAbs = Math.abs(y);
                this.mHandledSwipeView.setAlpha(1.0f - Math.min(1.0f, fAbs / height));
                this.mHandledSwipeViewLastDelta = fAbs;
            }
            if (motionEvent.getActionMasked() == 3 || motionEvent.getActionMasked() == 1) {
                this.mHandledSwipeView.setTranslationX(0.0f);
                this.mHandledSwipeView.setTranslationY(0.0f);
                this.mHandledSwipeView.setAlpha(1.0f);
                this.mHandlingSwipeButton = false;
                float height2 = this.mHandledSwipeView.getHeight();
                if (orientation == 0) {
                    height2 = this.mHandledSwipeView.getWidth();
                }
                if (this.mHandledSwipeViewLastDelta > height2) {
                    ((SwipableView) this.mHandledSwipeView).delete();
                }
            }
            return true;
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    public Point hintTouchPoint(View view) {
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        return new Point(this.mHintTouchPoint.x - iArr[0], this.mHintTouchPoint.y - iArr[1]);
    }

    public void startTouchAnimation(View view, float f, float f2) {
        final CategorySelected categorySelected = (CategorySelected) findViewById(R.id.categorySelectedIndicator);
        view.getLocationOnScreen(new int[2]);
        this.mHintTouchPoint.x = (int) (r2[0] + f);
        this.mHintTouchPoint.y = (int) (r2[1] + f2);
        ((View) categorySelected.getParent()).getLocationOnScreen(new int[2]);
        int width = (int) (f - (categorySelected.getWidth() / 2));
        int height = (int) (f2 - (categorySelected.getHeight() / 2));
        categorySelected.setTranslationX((r2[0] - r7[0]) + width);
        categorySelected.setTranslationY((r2[1] - r7[1]) + height);
        categorySelected.setVisibility(0);
        categorySelected.animate().scaleX(2.0f).scaleY(2.0f).alpha(0.0f).withEndAction(new Runnable() {
            @Override
            public void run() {
                categorySelected.setVisibility(4);
                categorySelected.setScaleX(1.0f);
                categorySelected.setScaleY(1.0f);
                categorySelected.setAlpha(1.0f);
            }
        });
    }

    public void addLabel() {
        int count = this.mCategoryVersionsAdapter.getCount();
        for (int i = 0; i < count; i++) {
            Action item = this.mCategoryVersionsAdapter.getItem(i);
            if (item.getType() == 0) {
                item.mHasFinishAppliedFilterOperation = false;
                item.mIsAddVersionOperation = true;
            }
        }
    }

    public boolean hasFinishApplyVersionOperation() {
        if (this.mVersions.size() <= 0) {
            return true;
        }
        int count = this.mCategoryVersionsAdapter.getCount();
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < count) {
                Action item = this.mCategoryVersionsAdapter.getItem(i);
                item.getRepresentation();
                if (item.getType() != 0 || item.mHasFinishAppliedFilterOperation || !item.mIsAddVersionOperation) {
                    i++;
                } else {
                    Log.d("Gallery2/FilterShowActivity", "has not finish Apply VersionOperation i====" + i);
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        if (i == count) {
            Log.d("Gallery2/FilterShowActivity", "has Finish ApplyVersionOperation i == size  " + count);
            return true;
        }
        return z;
    }

    private void undoWhenBackToMain() {
        HistoryManager history;
        Log.d("Gallery2/FilterShowActivity", "<undoWhenBackToMain>");
        if (this.mMasterImage == null || (history = this.mMasterImage.getHistory()) == null) {
            return;
        }
        this.mMasterImage.onHistoryItemClick(history.undo());
        history.removeLast();
    }

    private String loadAlbumNameForSaving() {
        if (this.mSelectedImageUri == null) {
            Log.d("Gallery2/FilterShowActivity", "<loadAlbumNameForSaving> mSelectedImageUri is null");
            return null;
        }
        return LocalAlbum.getLocalizedName(getResources(), GalleryUtils.getBucketId(SaveImage.getFinalSaveDirectory(this, this.mSelectedImageUri).getPath()), null);
    }
}
