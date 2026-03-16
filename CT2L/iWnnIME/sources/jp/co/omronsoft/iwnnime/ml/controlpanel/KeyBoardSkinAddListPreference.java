package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnArrayAdapter;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;

public class KeyBoardSkinAddListPreference extends ListPreference {
    private static final int GOOGLE_PLAY_LINK_BUTTON_ICON = 2130837655;
    private static final String GOOGLE_PLAY_LINK_BUTTON_NAME = "";
    private static final String GOOGLE_PLAY_LINK_URL = "";
    public static final String KEYBOARDSKINADD_ACTION = "jp.co.omronsoft.wnnext.ADD_SKIN2";
    private static final String KEYBOARD_IMAGE_EDITION = "standard";
    private static final String KEYBOARD_IMAGE_URL = "http://cloudwnn.wnnlab.com/contents/keyboardskin/";
    private static final String LOCALE_JA = "ja";
    private static final String LOCALE_NOT_JA = "en";
    private int mClickedDialogEntryIndex;
    private CharSequence mStandardEntry;
    private CharSequence mStandardEntryValue;
    public static boolean GOOGLE_PLAY_LINK_BUTTON = false;
    private static final String[] GOOGLE_PLAY_LINK_DISABLE_PACKAGES = {LoggingEvents.EXTRA_CALLING_APP_NAME, LoggingEvents.EXTRA_CALLING_APP_NAME};
    public static boolean KEYBOARD_IMAGE_LINK_BUTTON = true;

    public KeyBoardSkinAddListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mClickedDialogEntryIndex = 0;
        CharSequence[] standardEntries = getEntries();
        CharSequence[] standardEntryValues = getEntryValues();
        this.mStandardEntry = standardEntries[0];
        this.mStandardEntryValue = standardEntryValues[0];
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        Context context = getContext();
        Resources res = context.getResources();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = WnnUtility.getPackageInfo(context, KEYBOARDSKINADD_ACTION);
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> entryValues = new ArrayList<>();
        ArrayList<Drawable> entriesImage = new ArrayList<>();
        entries.add(this.mStandardEntry);
        entryValues.add(this.mStandardEntryValue);
        entriesImage.add(res.getDrawable(R.drawable.standard_skin));
        if (isEnableGooglePlayLinkButton(context, resolveInfo)) {
            Drawable icon = res.getDrawable(R.drawable.standard_skin);
            entryValues.add(LoggingEvents.EXTRA_CALLING_APP_NAME);
            entries.add(LoggingEvents.EXTRA_CALLING_APP_NAME);
            entriesImage.add(icon);
        }
        for (ResolveInfo info : resolveInfo) {
            ActivityInfo actInfo = info.activityInfo;
            CharSequence label = info.loadLabel(pm);
            if (label == null) {
                if (actInfo != null) {
                    label = ((PackageItemInfo) actInfo).name;
                } else {
                    label = LoggingEvents.EXTRA_CALLING_APP_NAME;
                }
            }
            entries.add(label);
            entriesImage.add(info.loadIcon(pm));
            if (actInfo != null) {
                entryValues.add(((PackageItemInfo) actInfo).name);
            } else {
                entryValues.add(LoggingEvents.EXTRA_CALLING_APP_NAME);
            }
        }
        CharSequence[] tmpEntries = (CharSequence[]) entries.toArray(new CharSequence[entries.size()]);
        CharSequence[] tmpEntryValues = (CharSequence[]) entryValues.toArray(new CharSequence[entryValues.size()]);
        setEntries(tmpEntries);
        setEntryValues(tmpEntryValues);
        SharedPreferences sharedPref = getSharedPreferences();
        String set = LoggingEvents.EXTRA_CALLING_APP_NAME;
        if (sharedPref != null) {
            set = sharedPref.getString(getKey(), LoggingEvents.EXTRA_CALLING_APP_NAME);
        }
        setValue(set);
        this.mClickedDialogEntryIndex = findIndexOfValue(set);
        WnnArrayAdapter<CharSequence> adapter = new WnnArrayAdapter<>(context, 0, tmpEntries);
        adapter.setEntriesImage(entriesImage);
        adapter.setCheckIndex(this.mClickedDialogEntryIndex);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                KeyBoardSkinAddListPreference.this.mClickedDialogEntryIndex = position;
                Dialog dialog = KeyBoardSkinAddListPreference.this.getDialog();
                KeyBoardSkinAddListPreference.this.onClick(dialog, -1);
                dialog.dismiss();
            }
        };
        ListView listView = WnnUtility.makeSingleChoiceListView(context, adapter, this.mClickedDialogEntryIndex, listener);
        builder.setView(listView);
        if (KEYBOARD_IMAGE_LINK_BUTTON) {
            builder.setNeutralButton(R.string.ti_preference_download_summary_txt, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    KeyBoardSkinAddListPreference.this.callDownloadPage(KeyBoardSkinAddListPreference.this.getWnnKeyboardLabSkinUrl());
                }
            });
        }
        builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            CharSequence[] values = getEntryValues();
            if (this.mClickedDialogEntryIndex >= 0 && values != null) {
                String value = values[this.mClickedDialogEntryIndex].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                }
            }
            IWnnIME wnn = IWnnIME.getCurrentIme();
            if (wnn != null) {
                IWnnImeEvent ev = new IWnnImeEvent(IWnnImeEvent.CHANGE_INPUT_CANDIDATE_VIEW);
                wnn.onEvent(ev);
            }
        }
    }

    @Override
    public void setValue(String value) {
        if (value.equals(LoggingEvents.EXTRA_CALLING_APP_NAME) && GOOGLE_PLAY_LINK_BUTTON) {
            callDownloadPage(LoggingEvents.EXTRA_CALLING_APP_NAME);
        } else {
            super.setValue(value);
        }
    }

    private boolean isEnableGooglePlayLinkButton(Context context, List<ResolveInfo> resolveInfo) {
        if (!GOOGLE_PLAY_LINK_BUTTON) {
            return false;
        }
        for (ResolveInfo info : resolveInfo) {
            ActivityInfo actInfo = info.activityInfo;
            String packagename = actInfo.packageName;
            if (packagename.equals(GOOGLE_PLAY_LINK_DISABLE_PACKAGES[0]) || packagename.equals(GOOGLE_PLAY_LINK_DISABLE_PACKAGES[1])) {
                return false;
            }
        }
        return true;
    }

    private void callDownloadPage(String url) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getWnnKeyboardLabSkinUrl() {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(KEYBOARD_IMAGE_URL);
        Locale locale = Locale.getDefault();
        if (locale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
            urlBuilder.append("ja/");
        } else {
            urlBuilder.append("en/");
        }
        PackageManager pm = getContext().getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null) {
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        String deviceName = Build.MODEL.replaceAll(" ", LoggingEvents.EXTRA_CALLING_APP_NAME);
        urlBuilder.append("standard_" + packageInfo.versionCode + "_" + deviceName + "/");
        return urlBuilder.toString();
    }
}
