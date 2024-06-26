package com.android.systemui.tuner;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.widget.EditText;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.NavigationBarInflaterView;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class NavBarTuner extends TunerPreferenceFragment {
    private static final int[][] ICONS = {new int[]{R.drawable.ic_qs_circle, R.string.tuner_circle}, new int[]{R.drawable.ic_add, R.string.tuner_plus}, new int[]{R.drawable.ic_remove, R.string.tuner_minus}, new int[]{R.drawable.ic_left, R.string.tuner_left}, new int[]{R.drawable.ic_right, R.string.tuner_right}, new int[]{R.drawable.ic_menu, R.string.tuner_menu}};
    private Handler mHandler;
    private final ArrayList<TunerService.Tunable> mTunables = new ArrayList<>();

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        this.mHandler = new Handler();
        super.onCreate(bundle);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override // android.support.v14.preference.PreferenceFragment
    public void onCreatePreferences(Bundle bundle, String str) {
        addPreferencesFromResource(R.xml.nav_bar_tuner);
        bindLayout((ListPreference) findPreference("layout"));
        bindButton("sysui_nav_bar_left", "space", "left");
        bindButton("sysui_nav_bar_right", "menu_ime", "right");
    }

    @Override // android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        this.mTunables.forEach(new Consumer() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$tsKQ8HfwaDSvc3iDCsgHsW954hc
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((TunerService) Dependency.get(TunerService.class)).removeTunable((TunerService.Tunable) obj);
            }
        });
    }

    private void addTunable(TunerService.Tunable tunable, String... strArr) {
        this.mTunables.add(tunable);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(tunable, strArr);
    }

    private void bindLayout(final ListPreference listPreference) {
        addTunable(new TunerService.Tunable() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$S2jLo9__CXt1xH-Y0l__zpA7S6Y
            @Override // com.android.systemui.tuner.TunerService.Tunable
            public final void onTuningChanged(String str, String str2) {
                NavBarTuner.this.mHandler.post(new Runnable() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$4yjdjEjuBBM9Uve2djbFEJblmmM
                    @Override // java.lang.Runnable
                    public final void run() {
                        NavBarTuner.lambda$bindLayout$1(str2, r2);
                    }
                });
            }
        }, "sysui_nav_bar");
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$xKs-3UbChwmRyuUfQ6nrgLk_wz4
            @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                return NavBarTuner.lambda$bindLayout$3(preference, obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$bindLayout$1(String str, ListPreference listPreference) {
        if (str == null) {
            str = "default";
        }
        listPreference.setValue(str);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$bindLayout$3(Preference preference, Object obj) {
        String str = (String) obj;
        if ("default".equals(str)) {
            str = null;
        }
        ((TunerService) Dependency.get(TunerService.class)).setValue("sysui_nav_bar", str);
        return true;
    }

    private void bindButton(final String str, final String str2, String str3) {
        final ListPreference listPreference = (ListPreference) findPreference("type_" + str3);
        final Preference findPreference = findPreference("keycode_" + str3);
        final ListPreference listPreference2 = (ListPreference) findPreference("icon_" + str3);
        setupIcons(listPreference2);
        addTunable(new TunerService.Tunable() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$OO9apf0OQ_k5f52-B3z0sPWxq4Y
            @Override // com.android.systemui.tuner.TunerService.Tunable
            public final void onTuningChanged(String str4, String str5) {
                r0.mHandler.post(new Runnable() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$SHMBahM6FQXBhKLgycxQEP9MKcg
                    @Override // java.lang.Runnable
                    public final void run() {
                        NavBarTuner.lambda$bindButton$4(NavBarTuner.this, str5, r3, r4, r5, r6);
                    }
                });
            }
        }, str);
        Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$addObFvLTzuE04K9maj0keVenxg
            @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                return NavBarTuner.lambda$bindButton$7(NavBarTuner.this, str, listPreference, findPreference, listPreference2, preference, obj);
            }
        };
        listPreference.setOnPreferenceChangeListener(onPreferenceChangeListener);
        listPreference2.setOnPreferenceChangeListener(onPreferenceChangeListener);
        findPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$L8cT3y_Xey3yV0gzNMgaCqxduDs
            @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
            public final boolean onPreferenceClick(Preference preference) {
                return NavBarTuner.lambda$bindButton$9(NavBarTuner.this, findPreference, str, listPreference, listPreference2, preference);
            }
        });
    }

    public static /* synthetic */ void lambda$bindButton$4(NavBarTuner navBarTuner, String str, String str2, ListPreference listPreference, ListPreference listPreference2, Preference preference) {
        if (str == null) {
            str = str2;
        }
        String extractButton = NavigationBarInflaterView.extractButton(str);
        if (extractButton.startsWith("key")) {
            listPreference.setValue("key");
            String extractImage = NavigationBarInflaterView.extractImage(extractButton);
            int extractKeycode = NavigationBarInflaterView.extractKeycode(extractButton);
            listPreference2.setValue(extractImage);
            navBarTuner.updateSummary(listPreference2);
            preference.setSummary(extractKeycode + "");
            preference.setVisible(true);
            listPreference2.setVisible(true);
            return;
        }
        listPreference.setValue(extractButton);
        preference.setVisible(false);
        listPreference2.setVisible(false);
    }

    public static /* synthetic */ boolean lambda$bindButton$7(final NavBarTuner navBarTuner, final String str, final ListPreference listPreference, final Preference preference, final ListPreference listPreference2, Preference preference2, Object obj) {
        navBarTuner.mHandler.post(new Runnable() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$LY1SqGQukLZwVkxUGh5npeYUH_E
            @Override // java.lang.Runnable
            public final void run() {
                NavBarTuner.lambda$bindButton$6(NavBarTuner.this, str, listPreference, preference, listPreference2);
            }
        });
        return true;
    }

    public static /* synthetic */ void lambda$bindButton$6(NavBarTuner navBarTuner, String str, ListPreference listPreference, Preference preference, ListPreference listPreference2) {
        navBarTuner.setValue(str, listPreference, preference, listPreference2);
        navBarTuner.updateSummary(listPreference2);
    }

    public static /* synthetic */ boolean lambda$bindButton$9(final NavBarTuner navBarTuner, final Preference preference, final String str, final ListPreference listPreference, final ListPreference listPreference2, Preference preference2) {
        final EditText editText = new EditText(navBarTuner.getContext());
        new AlertDialog.Builder(navBarTuner.getContext()).setTitle(preference2.getTitle()).setView(editText).setNegativeButton(17039360, (DialogInterface.OnClickListener) null).setPositiveButton(17039370, new DialogInterface.OnClickListener() { // from class: com.android.systemui.tuner.-$$Lambda$NavBarTuner$f5iTNvW7hFZFMmuMznH0p1eCwlI
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                NavBarTuner.lambda$bindButton$8(NavBarTuner.this, editText, preference, str, listPreference, listPreference2, dialogInterface, i);
            }
        }).show();
        return true;
    }

    public static /* synthetic */ void lambda$bindButton$8(NavBarTuner navBarTuner, EditText editText, Preference preference, String str, ListPreference listPreference, ListPreference listPreference2, DialogInterface dialogInterface, int i) {
        int i2;
        try {
            i2 = Integer.parseInt(editText.getText().toString());
        } catch (Exception e) {
            i2 = 66;
        }
        preference.setSummary(i2 + "");
        navBarTuner.setValue(str, listPreference, preference, listPreference2);
    }

    private void updateSummary(ListPreference listPreference) {
        try {
            int applyDimension = (int) TypedValue.applyDimension(1, 14.0f, getContext().getResources().getDisplayMetrics());
            String str = listPreference.getValue().split("/")[0];
            int parseInt = Integer.parseInt(listPreference.getValue().split("/")[1]);
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            Drawable loadDrawable = Icon.createWithResource(str, parseInt).loadDrawable(getContext());
            loadDrawable.setTint(-16777216);
            loadDrawable.setBounds(0, 0, applyDimension, applyDimension);
            spannableStringBuilder.append("  ", new ImageSpan(loadDrawable, 1), 0);
            spannableStringBuilder.append((CharSequence) " ");
            for (int i = 0; i < ICONS.length; i++) {
                if (ICONS[i][0] == parseInt) {
                    spannableStringBuilder.append((CharSequence) getString(ICONS[i][1]));
                }
            }
            listPreference.setSummary(spannableStringBuilder);
        } catch (Exception e) {
            Log.d("NavButton", "Problem with summary", e);
            listPreference.setSummary((CharSequence) null);
        }
    }

    private void setValue(String str, ListPreference listPreference, Preference preference, ListPreference listPreference2) {
        int i;
        String value = listPreference.getValue();
        if ("key".equals(value)) {
            String value2 = listPreference2.getValue();
            try {
                i = Integer.parseInt(preference.getSummary().toString());
            } catch (Exception e) {
                i = 66;
            }
            value = value + "(" + i + ":" + value2 + ")";
        }
        ((TunerService) Dependency.get(TunerService.class)).setValue(str, value);
    }

    private void setupIcons(ListPreference listPreference) {
        CharSequence[] charSequenceArr = new CharSequence[ICONS.length];
        CharSequence[] charSequenceArr2 = new CharSequence[ICONS.length];
        int applyDimension = (int) TypedValue.applyDimension(1, 14.0f, getContext().getResources().getDisplayMetrics());
        for (int i = 0; i < ICONS.length; i++) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            Drawable loadDrawable = Icon.createWithResource(getContext().getPackageName(), ICONS[i][0]).loadDrawable(getContext());
            loadDrawable.setTint(-16777216);
            loadDrawable.setBounds(0, 0, applyDimension, applyDimension);
            spannableStringBuilder.append("  ", new ImageSpan(loadDrawable, 1), 0);
            spannableStringBuilder.append((CharSequence) " ");
            spannableStringBuilder.append((CharSequence) getString(ICONS[i][1]));
            charSequenceArr[i] = spannableStringBuilder;
            charSequenceArr2[i] = getContext().getPackageName() + "/" + ICONS[i][0];
        }
        listPreference.setEntries(charSequenceArr);
        listPreference.setEntryValues(charSequenceArr2);
    }
}
