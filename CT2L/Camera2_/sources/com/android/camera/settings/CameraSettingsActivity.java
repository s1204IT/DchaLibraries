package com.android.camera.settings;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import com.android.camera.debug.Log;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.util.CameraSettingsActivityHelper;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.Size;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CameraSettingsActivity extends FragmentActivity {
    public static final String PREF_SCREEN_EXTRA = "pref_screen_extra";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mode_settings);
        String prefKey = getIntent().getStringExtra(PREF_SCREEN_EXTRA);
        CameraSettingsFragment dialog = new CameraSettingsFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString(PREF_SCREEN_EXTRA, prefKey);
        dialog.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(android.R.id.content, dialog).commit();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == 16908332) {
            finish();
        }
        return true;
    }

    public static class CameraSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public static final String PREF_CATEGORY_ADVANCED = "pref_category_advanced";
        public static final String PREF_CATEGORY_RESOLUTION = "pref_category_resolution";
        private static final Log.Tag TAG = new Log.Tag("SettingsFragment");
        private static DecimalFormat sMegaPixelFormat = new DecimalFormat("##0.0");
        private String[] mCamcorderProfileNames;
        private boolean mGetSubPrefAsRoot = true;
        private CameraDeviceInfo mInfos;
        private SettingsUtil.SelectedPictureSizes mOldPictureSizesBack;
        private SettingsUtil.SelectedPictureSizes mOldPictureSizesFront;
        private List<Size> mPictureSizesBack;
        private List<Size> mPictureSizesFront;
        private String mPrefKey;
        private SettingsUtil.SelectedVideoQualities mVideoQualitiesBack;
        private SettingsUtil.SelectedVideoQualities mVideoQualitiesFront;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle arguments = getArguments();
            if (arguments != null) {
                this.mPrefKey = arguments.getString(CameraSettingsActivity.PREF_SCREEN_EXTRA);
            }
            Context context = getActivity().getApplicationContext();
            addPreferencesFromResource(R.xml.camera_preferences);
            this.mGetSubPrefAsRoot = false;
            CameraSettingsActivityHelper.addAdditionalPreferences(this, context);
            this.mGetSubPrefAsRoot = true;
            this.mCamcorderProfileNames = getResources().getStringArray(R.array.camcorder_profile_names);
            this.mInfos = CameraAgentFactory.getAndroidCameraAgent(context, CameraAgentFactory.CameraApi.API_1).getCameraDeviceInfo();
        }

        @Override
        public void onResume() {
            super.onResume();
            getActivity();
            loadSizes();
            setVisibilities();
            PreferenceScreen resolutionScreen = (PreferenceScreen) findPreference(PREF_CATEGORY_RESOLUTION);
            fillEntriesAndSummaries(resolutionScreen);
            setPreferenceScreenIntent(resolutionScreen);
            PreferenceScreen advancedScreen = (PreferenceScreen) findPreference(PREF_CATEGORY_ADVANCED);
            setPreferenceScreenIntent(advancedScreen);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        private void setPreferenceScreenIntent(PreferenceScreen preferenceScreen) {
            Intent intent = new Intent(getActivity(), (Class<?>) CameraSettingsActivity.class);
            intent.putExtra(CameraSettingsActivity.PREF_SCREEN_EXTRA, preferenceScreen.getKey());
            preferenceScreen.setIntent(intent);
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            PreferenceScreen root = super.getPreferenceScreen();
            if (!this.mGetSubPrefAsRoot || this.mPrefKey == null || root == null) {
                return root;
            }
            PreferenceScreen match = findByKey(root, this.mPrefKey);
            if (match != null) {
                return match;
            }
            throw new RuntimeException("key " + this.mPrefKey + " not found");
        }

        private PreferenceScreen findByKey(PreferenceScreen parent, String key) {
            PreferenceScreen match;
            if (!key.equals(parent.getKey())) {
                for (int i = 0; i < parent.getPreferenceCount(); i++) {
                    Preference child = parent.getPreference(i);
                    if ((child instanceof PreferenceScreen) && (match = findByKey((PreferenceScreen) child, key)) != null) {
                        return match;
                    }
                }
                return null;
            }
            return parent;
        }

        private void setVisibilities() {
            PreferenceGroup resolutions = (PreferenceGroup) findPreference(PREF_CATEGORY_RESOLUTION);
            if (this.mPictureSizesBack == null) {
                recursiveDelete(resolutions, findPreference(Keys.KEY_PICTURE_SIZE_BACK));
                recursiveDelete(resolutions, findPreference(Keys.KEY_VIDEO_QUALITY_BACK));
            }
            if (this.mPictureSizesFront == null) {
                recursiveDelete(resolutions, findPreference(Keys.KEY_PICTURE_SIZE_FRONT));
                recursiveDelete(resolutions, findPreference(Keys.KEY_VIDEO_QUALITY_FRONT));
            }
        }

        private void fillEntriesAndSummaries(PreferenceGroup group) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference pref = group.getPreference(i);
                if (pref instanceof PreferenceGroup) {
                    fillEntriesAndSummaries((PreferenceGroup) pref);
                }
                setSummary(pref);
                setEntries(pref);
            }
        }

        private boolean recursiveDelete(PreferenceGroup group, Preference preference) {
            if (group == null) {
                Log.d(TAG, "attempting to delete from null preference group");
                return false;
            }
            if (preference == null) {
                Log.d(TAG, "attempting to delete null preference");
                return false;
            }
            if (group.removePreference(preference)) {
                return true;
            }
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference pref = group.getPreference(i);
                if ((pref instanceof PreferenceGroup) && recursiveDelete((PreferenceGroup) pref, preference)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setSummary(findPreference(key));
        }

        private void setEntries(Preference preference) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
                    setEntriesForSelection(this.mPictureSizesBack, listPreference);
                    return;
                }
                if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
                    setEntriesForSelection(this.mPictureSizesFront, listPreference);
                } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
                    setEntriesForSelection(this.mVideoQualitiesBack, listPreference);
                } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
                    setEntriesForSelection(this.mVideoQualitiesFront, listPreference);
                }
            }
        }

        private void setSummary(Preference preference) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
                    setSummaryForSelection(this.mOldPictureSizesBack, this.mPictureSizesBack, listPreference);
                    return;
                }
                if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
                    setSummaryForSelection(this.mOldPictureSizesFront, this.mPictureSizesFront, listPreference);
                    return;
                }
                if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
                    setSummaryForSelection(this.mVideoQualitiesBack, listPreference);
                } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
                    setSummaryForSelection(this.mVideoQualitiesFront, listPreference);
                } else {
                    listPreference.setSummary(listPreference.getEntry());
                }
            }
        }

        private void setEntriesForSelection(List<Size> selectedSizes, ListPreference preference) {
            if (selectedSizes != null) {
                String[] entries = new String[selectedSizes.size()];
                String[] entryValues = new String[selectedSizes.size()];
                for (int i = 0; i < selectedSizes.size(); i++) {
                    Size size = selectedSizes.get(i);
                    entries[i] = getSizeSummaryString(size);
                    entryValues[i] = SettingsUtil.sizeToSetting(size);
                }
                preference.setEntries(entries);
                preference.setEntryValues(entryValues);
            }
        }

        private void setEntriesForSelection(SettingsUtil.SelectedVideoQualities selectedQualities, ListPreference preference) {
            if (selectedQualities != null) {
                ArrayList<String> entries = new ArrayList<>();
                entries.add(this.mCamcorderProfileNames[selectedQualities.large]);
                if (selectedQualities.medium != selectedQualities.large) {
                    entries.add(this.mCamcorderProfileNames[selectedQualities.medium]);
                }
                if (selectedQualities.small != selectedQualities.medium) {
                    entries.add(this.mCamcorderProfileNames[selectedQualities.small]);
                }
                preference.setEntries((CharSequence[]) entries.toArray(new String[0]));
            }
        }

        private void setSummaryForSelection(SettingsUtil.SelectedPictureSizes oldPictureSizes, List<Size> displayableSizes, ListPreference preference) {
            if (oldPictureSizes != null) {
                String setting = preference.getValue();
                Size selectedSize = oldPictureSizes.getFromSetting(setting, displayableSizes);
                preference.setSummary(getSizeSummaryString(selectedSize));
            }
        }

        private void setSummaryForSelection(SettingsUtil.SelectedVideoQualities selectedQualities, ListPreference preference) {
            if (selectedQualities != null) {
                int selectedQuality = selectedQualities.getFromSetting(preference.getValue());
                preference.setSummary(this.mCamcorderProfileNames[selectedQuality]);
            }
        }

        private void loadSizes() {
            if (this.mInfos == null) {
                Log.w(TAG, "null deviceInfo, cannot display resolution sizes");
                return;
            }
            int backCameraId = SettingsUtil.getCameraId(this.mInfos, SettingsUtil.CAMERA_FACING_BACK);
            if (backCameraId >= 0) {
                List<Size> sizes = CameraPictureSizesCacher.getSizesForCamera(backCameraId, getActivity().getApplicationContext());
                if (sizes != null) {
                    this.mOldPictureSizesBack = SettingsUtil.getSelectedCameraPictureSizes(sizes, backCameraId);
                    this.mPictureSizesBack = ResolutionUtil.getDisplayableSizesFromSupported(sizes, true);
                }
                this.mVideoQualitiesBack = SettingsUtil.getSelectedVideoQualities(backCameraId);
            } else {
                this.mPictureSizesBack = null;
                this.mVideoQualitiesBack = null;
            }
            int frontCameraId = SettingsUtil.getCameraId(this.mInfos, SettingsUtil.CAMERA_FACING_FRONT);
            if (frontCameraId >= 0) {
                List<Size> sizes2 = CameraPictureSizesCacher.getSizesForCamera(frontCameraId, getActivity().getApplicationContext());
                if (sizes2 != null) {
                    this.mOldPictureSizesFront = SettingsUtil.getSelectedCameraPictureSizes(sizes2, frontCameraId);
                    this.mPictureSizesFront = ResolutionUtil.getDisplayableSizesFromSupported(sizes2, false);
                }
                this.mVideoQualitiesFront = SettingsUtil.getSelectedVideoQualities(frontCameraId);
                return;
            }
            this.mPictureSizesFront = null;
            this.mVideoQualitiesFront = null;
        }

        private String getSizeSummaryString(Size size) {
            Size approximateSize = ResolutionUtil.getApproximateSize(size);
            String megaPixels = sMegaPixelFormat.format(((double) (size.width() * size.height())) / 1000000.0d);
            int numerator = ResolutionUtil.aspectRatioNumerator(approximateSize);
            int denominator = ResolutionUtil.aspectRatioDenominator(approximateSize);
            String result = getResources().getString(R.string.setting_summary_aspect_ratio_and_megapixels, Integer.valueOf(numerator), Integer.valueOf(denominator), megaPixels);
            return result;
        }
    }
}
