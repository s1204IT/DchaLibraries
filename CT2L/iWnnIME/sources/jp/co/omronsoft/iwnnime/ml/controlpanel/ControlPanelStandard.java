package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import com.android.inputmethodcommon.InputMethodSettingsActivity;
import java.util.List;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnCore;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class ControlPanelStandard extends InputMethodSettingsActivity {
    private static final String FRAGMENT_NAME_USERDIC_LIST = "jp.co.omronsoft.iwnnime.ml.controlpanel.UserDictionaryToolsList";
    private static ControlPanelStandard mCurrentControlPanel = null;
    private int mOrientation = 0;
    private PreferenceActivity.Header mDefaultSelectedHeader = null;
    private boolean mNotInit = true;
    private OnRestartConpaneListener mOnRestartConpaneListener = null;

    public interface OnRestartConpaneListener {
        void OnRestartConpane();
    }

    public static ControlPanelStandard getCurrentControlPanel() {
        return mCurrentControlPanel;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!(this instanceof SubControlPanelStandard)) {
            mCurrentControlPanel = this;
            IWnnIME.updateTabletMode(this);
            initInputMethodSettings(R.string.ti_preference_lang_setting_keyboard_title_txt);
            iWnnEngine engine = iWnnEngine.getEngine();
            engine.setFilesDirPath(IWnnIME.getFilesDirPath(this));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!IWnnCore.hasLibrary()) {
            Toast.makeText(getApplicationContext(), R.string.ti_message_not_work_txt, 0).show();
            finish();
        } else {
            updateInputMethodSettings();
            if (!(this instanceof SubControlPanelStandard)) {
                invalidateHeaders();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this == mCurrentControlPanel) {
            mCurrentControlPanel = null;
        }
    }

    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.iwnnime_pref_headers, target);
        updateHeaderList(target);
    }

    @Override
    public PreferenceActivity.Header onGetNewHeader() {
        if (!isMultiPaneMode() || (!isStartSettings() && !this.mNotInit)) {
            return null;
        }
        this.mNotInit = false;
        clearIsStartSettings();
        return this.mDefaultSelectedHeader;
    }

    private void updateHeaderList(List<PreferenceActivity.Header> target) {
        for (int cnt = 0; cnt < target.size(); cnt++) {
            PreferenceActivity.Header header = target.get(cnt);
            int id = (int) header.id;
            if (id == R.id.header_display) {
                this.mDefaultSelectedHeader = header;
            }
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isMultiPaneMode();
    }

    public boolean isMultiPaneMode() {
        if (IWnnIME.isTabletMode() || getResources().getConfiguration().orientation != 1) {
            return true;
        }
        return false;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args, int titleRes, int shortTitleRes) {
        if (FRAGMENT_NAME_USERDIC_LIST.equals(fragmentName)) {
            titleRes = R.string.ti_preference_dictionary_menu_standard_txt;
        }
        Intent intent = super.onBuildStartFragmentIntent(fragmentName, args, titleRes, shortTitleRes);
        intent.setClass(this, SubControlPanelStandard.class);
        return intent;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        if (FRAGMENT_NAME_USERDIC_LIST.equals(pref.getFragment())) {
            startPreferencePanel(pref.getFragment(), pref.getExtras(), R.string.ti_preference_dictionary_menu_standard_txt, null, null, 0);
            return true;
        }
        super.onPreferenceStartFragment(caller, pref);
        return true;
    }

    @Override
    public void showBreadCrumbs(CharSequence title, CharSequence shortTitle) {
        super.showBreadCrumbs(title, shortTitle);
        if (isMultiPaneMode()) {
            setTitle(R.string.ti_ime_name_setting_title_standard_txt);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mOrientation != newConfig.orientation) {
            this.mOrientation = newConfig.orientation;
            restart();
        }
    }

    public void restart() {
        if (this.mOnRestartConpaneListener != null) {
            this.mOnRestartConpaneListener.OnRestartConpane();
        }
        finish();
        if (mCurrentControlPanel == this) {
            Intent intent = new Intent();
            intent.setClass(this, ControlPanelStandard.class);
            startActivity(intent);
        }
    }

    public void moveToRootActivity() {
        Intent intent = new Intent();
        intent.setClass(this, ControlPanelStandard.class);
        intent.setFlags(iWnnEngine.WNNWORD_ATTRIBUTE_NEXT_BUTTON);
        startActivity(intent);
    }

    public void setOnRestartConpaneListener(OnRestartConpaneListener listener) {
        this.mOnRestartConpaneListener = listener;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
}
