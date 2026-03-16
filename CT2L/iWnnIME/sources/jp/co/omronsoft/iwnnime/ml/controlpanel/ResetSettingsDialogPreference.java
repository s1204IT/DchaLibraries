package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import jp.co.omronsoft.iwnnime.ml.DecoEmojiListener;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class ResetSettingsDialogPreference extends DialogPreference {
    public ResetSettingsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(LoggingEvents.EXTRA_CALLING_APP_NAME);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Context context = getContext();
            IWnnIME.getIntFromNotResetSettingsPreference(context, DecoEmojiListener.PREF_KEY, -1);
            IWnnIME.getStringFromNotResetSettingsPreference(context, iWnnEngine.DICTIONARY_VERSION_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
            for (int languageType = 0; languageType < 19; languageType++) {
                for (int index = 1; index <= 10; index++) {
                    String key = AdditionalDictionaryPreference.createAdditionalDictionaryKey(languageType, index);
                    IWnnIME.getStringFromNotResetSettingsPreference(context, key, null);
                }
            }
            for (int index2 = 0; index2 < 10; index2++) {
                IWnnIME.getStringFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_NAME), null);
                IWnnIME.getStringFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_FILE), null);
                IWnnIME.getIntFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_CONVERT_HIGH), 0);
                IWnnIME.getIntFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_CONVERT_BASE), 0);
                IWnnIME.getIntFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_PREDICT_HIGH), 0);
                IWnnIME.getIntFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_PREDICT_BASE), 0);
                IWnnIME.getIntFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_MORPHO_HIGH), 0);
                IWnnIME.getIntFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_MORPHO_BASE), 0);
                IWnnIME.getBooleanFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_CACHE_FLAG), false);
                IWnnIME.getIntFromNotResetSettingsPreference(context, DownloadDictionaryPreference.createSharedPrefKey(index2, DownloadDictionaryPreference.QUERY_PARAM_LIMIT), 0);
            }
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor sharedPrefeditor = sharedPref.edit();
            sharedPrefeditor.clear();
            sharedPrefeditor.commit();
            IWnnIME wnn = IWnnIME.getCurrentIme();
            if (wnn != null) {
                IWnnImeEvent ev = new IWnnImeEvent(IWnnImeEvent.CHANGE_INPUT_CANDIDATE_VIEW);
                wnn.onEvent(ev);
            }
            Toast.makeText(context, R.string.ti_dialog_reset_setting_done_txt, 0).show();
        }
    }
}
