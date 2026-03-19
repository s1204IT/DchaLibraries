package com.android.storagemanager.deletionhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.storagemanager.ButtonBarProvider;
import com.android.storagemanager.R;
import com.android.storagemanager.deletionhelper.DeletionType;
import com.android.storagemanager.overlay.DeletionHelperFeatureProvider;
import com.android.storagemanager.overlay.FeatureFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DeletionHelperSettings extends PreferenceFragment implements View.OnClickListener, DeletionType.FreeableChangedListener {
    AppDeletionType mAppBackend;
    private AppDeletionPreferenceGroup mApps;
    private Button mCancel;
    private List<DeletionType> mDeletableContentList;
    private DownloadsDeletionType mDownloadsDeletion;
    private DownloadsDeletionPreferenceGroup mDownloadsPreference;
    private Button mFree;
    private Preference mGaugePreference;
    private LoadingSpinnerController mLoadingController;
    private PhotosDeletionPreference mPhotoPreference;
    private DeletionType mPhotoVideoDeletion;
    private DeletionHelperFeatureProvider mProvider;
    private int mThresholdType;

    public static DeletionHelperSettings newInstance(int i) {
        DeletionHelperSettings deletionHelperSettings = new DeletionHelperSettings();
        Bundle bundle = new Bundle(1);
        bundle.putInt("threshold_key", i);
        deletionHelperSettings.setArguments(bundle);
        return deletionHelperSettings;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        Intent intent;
        addPreferencesFromResource(R.xml.deletion_helper_list);
        this.mThresholdType = getArguments().getInt("threshold_key", 0);
        this.mApps = (AppDeletionPreferenceGroup) findPreference("apps_group");
        this.mPhotoPreference = (PhotosDeletionPreference) findPreference("delete_photos");
        this.mProvider = FeatureFactory.getFactory(getActivity()).getDeletionHelperFeatureProvider();
        this.mLoadingController = new LoadingSpinnerController((DeletionHelperActivity) getActivity());
        if (this.mProvider != null) {
            this.mPhotoVideoDeletion = this.mProvider.createPhotoVideoDeletionType(getContext(), this.mThresholdType);
        }
        HashSet hashSet = null;
        if (bundle != null) {
            hashSet = (HashSet) bundle.getSerializable("checkedSet");
        }
        this.mAppBackend = new AppDeletionType(this, hashSet, this.mThresholdType);
        this.mAppBackend.registerView(this.mApps);
        this.mAppBackend.registerFreeableChangedListener(this);
        this.mApps.setDeletionType(this.mAppBackend);
        this.mDeletableContentList = new ArrayList(3);
        this.mGaugePreference = findPreference("deletion_gauge");
        Activity activity = getActivity();
        if (activity != null && this.mGaugePreference != null && (intent = activity.getIntent()) != null) {
            CharSequence gaugeString = getGaugeString(getContext(), intent, activity.getCallingPackage());
            if (gaugeString != null) {
                this.mGaugePreference.setTitle(gaugeString);
            } else {
                getPreferenceScreen().removePreference(this.mGaugePreference);
            }
        }
    }

    protected static CharSequence getGaugeString(Context context, Intent intent, String str) {
        CharSequence applicationLabel;
        Preconditions.checkNotNull(intent);
        long longExtra = intent.getLongExtra("android.os.storage.extra.REQUESTED_BYTES", -1L);
        if (longExtra <= 0 || (applicationLabel = AppUtils.getApplicationLabel(context.getPackageManager(), str)) == null) {
            return null;
        }
        return context.getString(R.string.app_requesting_space, applicationLabel, Formatter.formatFileSize(context, longExtra));
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        initializeButtons();
        setHasOptionsMenu(true);
        Activity activity = getActivity();
        if (activity.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            activity.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 0);
        }
        if (this.mProvider != null && this.mPhotoVideoDeletion != null) {
            this.mPhotoPreference.setDaysToKeep(this.mProvider.getDaysToKeep(this.mThresholdType));
            this.mPhotoPreference.registerFreeableChangedListener(this);
            this.mPhotoPreference.registerDeletionService(this.mPhotoVideoDeletion);
            this.mDeletableContentList.add(this.mPhotoVideoDeletion);
        } else {
            getPreferenceScreen().removePreference(this.mPhotoPreference);
            this.mPhotoPreference.setEnabled(false);
        }
        String[] stringArray = null;
        if (bundle != null) {
            stringArray = bundle.getStringArray("uncheckedFiles");
        }
        this.mDownloadsPreference = (DownloadsDeletionPreferenceGroup) findPreference("delete_downloads");
        this.mDownloadsDeletion = new DownloadsDeletionType(getActivity(), stringArray);
        this.mDownloadsPreference.registerFreeableChangedListener(this);
        this.mDownloadsPreference.registerDeletionService(this.mDownloadsDeletion);
        this.mDeletableContentList.add(this.mDownloadsDeletion);
        if (isEmptyState()) {
            setupEmptyState();
        }
        this.mDeletableContentList.add(this.mAppBackend);
        updateFreeButtonText();
    }

    void setupEmptyState() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (this.mDownloadsPreference != null) {
            this.mDownloadsPreference.setChecked(false);
            preferenceScreen.removePreference(this.mDownloadsPreference);
        }
        preferenceScreen.removePreference(this.mApps);
        this.mDownloadsDeletion = null;
        this.mDownloadsPreference = null;
    }

    private boolean isEmptyState() {
        return this.mThresholdType == 1;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mLoadingController.initializeLoading(getListView());
        int size = this.mDeletableContentList.size();
        for (int i = 0; i < size; i++) {
            this.mDeletableContentList.get(i).onResume();
        }
        if (this.mDownloadsDeletion != null && getActivity().checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == 0) {
            getLoaderManager().initLoader(1, new Bundle(), this.mDownloadsDeletion);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        int size = this.mDeletableContentList.size();
        for (int i = 0; i < size; i++) {
            this.mDeletableContentList.get(i).onPause();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        int size = this.mDeletableContentList.size();
        for (int i = 0; i < size; i++) {
            this.mDeletableContentList.get(i).onSaveInstanceStateBundle(bundle);
        }
    }

    @Override
    public void onFreeableChanged(int i, long j) {
        if ((i > 0 || j > 0 || allTypesEmpty()) && this.mLoadingController != null) {
            this.mLoadingController.onCategoryLoad();
        }
        if (this.mFree != null) {
            this.mFree.setEnabled((j == 0 && getTotalFreeableSpace(false) == 0) ? false : true);
        }
        updateFreeButtonText();
        if (allTypesEmpty() && !isEmptyState()) {
            startEmptyState();
        }
    }

    private boolean allTypesEmpty() {
        return this.mAppBackend.isEmpty() && (this.mDownloadsDeletion == null || this.mDownloadsDeletion.isEmpty()) && (this.mPhotoVideoDeletion == null || this.mPhotoVideoDeletion.isEmpty());
    }

    private void startEmptyState() {
        if (getActivity() instanceof DeletionHelperActivity) {
            ((DeletionHelperActivity) getActivity()).setIsEmptyState(true);
        }
    }

    protected void clearData() {
        if (this.mPhotoPreference != null && this.mPhotoPreference.isChecked()) {
            this.mPhotoVideoDeletion.clearFreeableData(getActivity());
        }
        if (this.mDownloadsPreference != null) {
            this.mDownloadsDeletion.clearFreeableData(getActivity());
        }
        if (this.mAppBackend != null) {
            this.mAppBackend.clearFreeableData(getActivity());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.next_button) {
            ConfirmDeletionDialog confirmDeletionDialogNewInstance = ConfirmDeletionDialog.newInstance(getTotalFreeableSpace(false));
            confirmDeletionDialogNewInstance.setTargetFragment(this, 0);
            confirmDeletionDialogNewInstance.show(getFragmentManager(), "ConfirmDeletionDialog");
            MetricsLogger.action(getContext(), 467);
            return;
        }
        MetricsLogger.action(getContext(), 468);
        getActivity().finish();
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 0 && iArr.length > 0 && iArr[0] == 0) {
            this.mDownloadsDeletion.onResume();
            getLoaderManager().initLoader(1, new Bundle(), this.mDownloadsDeletion);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Activity activity = getActivity();
        String string = getResources().getString(R.string.help_uri_deletion_helper);
        if (string != null && activity != null) {
            HelpUtils.prepareHelpMenuItem(activity, menu, string, getClass().getName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    void setDownloadsDeletionType(DownloadsDeletionType downloadsDeletionType) {
        this.mDownloadsDeletion = downloadsDeletionType;
    }

    private void initializeButtons() {
        ButtonBarProvider buttonBarProvider = (ButtonBarProvider) getActivity();
        buttonBarProvider.getButtonBar().setVisibility(0);
        this.mCancel = buttonBarProvider.getSkipButton();
        this.mCancel.setText(R.string.cancel);
        this.mCancel.setOnClickListener(this);
        this.mCancel.setVisibility(0);
        this.mFree = buttonBarProvider.getNextButton();
        this.mFree.setText(R.string.storage_menu_free);
        this.mFree.setOnClickListener(this);
        this.mFree.setEnabled(false);
    }

    private void updateFreeButtonText() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        this.mFree.setText(String.format(activity.getString(R.string.deletion_helper_free_button), Formatter.formatFileSize(activity, getTotalFreeableSpace(false))));
    }

    private long getTotalFreeableSpace(boolean z) {
        long totalAppsFreeableSpace = 0 + this.mAppBackend.getTotalAppsFreeableSpace(z);
        if (this.mPhotoPreference != null) {
            totalAppsFreeableSpace += this.mPhotoPreference.getFreeableBytes(z);
        }
        if (this.mDownloadsPreference != null) {
            return totalAppsFreeableSpace + this.mDownloadsDeletion.getFreeableBytes(z);
        }
        return totalAppsFreeableSpace;
    }
}
