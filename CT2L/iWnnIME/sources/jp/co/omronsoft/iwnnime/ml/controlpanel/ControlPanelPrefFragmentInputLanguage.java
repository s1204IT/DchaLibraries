package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.os.Bundle;
import com.android.inputmethodcommon.InputMethodSettingsFragment;
import jp.co.omronsoft.iwnnime.ml.R;

public class ControlPanelPrefFragmentInputLanguage extends InputMethodSettingsFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initInputMethodSettings(ControlPanelPrefFragment.CATEGORY_ROOT_KEY, R.string.ti_preference_lang_setting_menu_txt, R.string.ti_preference_lang_setting_keyboard_title_txt);
    }
}
