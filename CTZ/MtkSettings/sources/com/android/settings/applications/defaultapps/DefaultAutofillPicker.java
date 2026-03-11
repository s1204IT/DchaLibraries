package com.android.settings.applications.defaultapps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.service.autofill.AutofillServiceInfo;
import android.support.v7.preference.Preference;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.CandidateInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultAutofillPicker extends DefaultAppPickerFragment {
    static final Intent AUTOFILL_PROBE = new Intent("android.service.autofill.AutofillService");
    private DialogInterface.OnClickListener mCancelListener;
    private final PackageMonitor mSettingsPackageMonitor = new AnonymousClass1();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final Activity activity = getActivity();
        if (activity != null && activity.getIntent().getStringExtra("package_name") != null) {
            this.mCancelListener = new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    DefaultAutofillPicker.lambda$onCreate$0(activity, dialogInterface, i);
                }
            };
        }
        this.mSettingsPackageMonitor.register(activity, activity.getMainLooper(), false);
        update();
    }

    static void lambda$onCreate$0(Activity activity, DialogInterface dialogInterface, int i) {
        activity.setResult(0);
        activity.finish();
    }

    @Override
    protected DefaultAppPickerFragment.ConfirmationDialogFragment newConfirmationDialogFragment(String str, CharSequence charSequence) {
        AutofillPickerConfirmationDialogFragment autofillPickerConfirmationDialogFragment = new AutofillPickerConfirmationDialogFragment();
        autofillPickerConfirmationDialogFragment.init(this, str, charSequence);
        return autofillPickerConfirmationDialogFragment;
    }

    public static class AutofillPickerConfirmationDialogFragment extends DefaultAppPickerFragment.ConfirmationDialogFragment {
        @Override
        public void onCreate(Bundle bundle) {
            setCancelListener(((DefaultAutofillPicker) getTargetFragment()).mCancelListener);
            super.onCreate(bundle);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_autofill_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 792;
    }

    @Override
    protected boolean shouldShowItemNone() {
        return true;
    }

    class AnonymousClass1 extends PackageMonitor {
        AnonymousClass1() {
        }

        public void onPackageAdded(String str, int i) {
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    DefaultAutofillPicker.this.update();
                }
            });
        }

        public void onPackageModified(String str) {
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    DefaultAutofillPicker.this.update();
                }
            });
        }

        public void onPackageRemoved(String str, int i) {
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    DefaultAutofillPicker.this.update();
                }
            });
        }
    }

    public void update() {
        updateCandidates();
        addAddServicePreference();
    }

    @Override
    public void onDestroy() {
        this.mSettingsPackageMonitor.unregister();
        super.onDestroy();
    }

    private Preference newAddServicePreferenceOrNull() {
        String string = Settings.Secure.getString(getActivity().getContentResolver(), "autofill_service_search_uri");
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(string));
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.print_menu_item_add_service);
        preference.setIcon(R.drawable.ic_menu_add);
        preference.setOrder(2147483646);
        preference.setIntent(intent);
        preference.setPersistent(false);
        return preference;
    }

    private void addAddServicePreference() {
        Preference preferenceNewAddServicePreferenceOrNull = newAddServicePreferenceOrNull();
        if (preferenceNewAddServicePreferenceOrNull != null) {
            getPreferenceScreen().addPreference(preferenceNewAddServicePreferenceOrNull);
        }
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        List<ResolveInfo> listQueryIntentServices = this.mPm.queryIntentServices(AUTOFILL_PROBE, 128);
        Context context = getContext();
        for (ResolveInfo resolveInfo : listQueryIntentServices) {
            String str = resolveInfo.serviceInfo.permission;
            if ("android.permission.BIND_AUTOFILL_SERVICE".equals(str)) {
                arrayList.add(new DefaultAppInfo(context, this.mPm, this.mUserId, new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)));
            }
            if ("android.permission.BIND_AUTOFILL".equals(str)) {
                Log.w("DefaultAutofillPicker", "AutofillService from '" + resolveInfo.serviceInfo.packageName + "' uses unsupported permission android.permission.BIND_AUTOFILL. It works for now, but might not be supported on future releases");
                arrayList.add(new DefaultAppInfo(context, this.mPm, this.mUserId, new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)));
            }
        }
        return arrayList;
    }

    public static String getDefaultKey(Context context) {
        ComponentName componentNameUnflattenFromString;
        String string = Settings.Secure.getString(context.getContentResolver(), "autofill_service");
        if (string != null && (componentNameUnflattenFromString = ComponentName.unflattenFromString(string)) != null) {
            return componentNameUnflattenFromString.flattenToString();
        }
        return null;
    }

    @Override
    protected String getDefaultKey() {
        return getDefaultKey(getContext());
    }

    @Override
    protected CharSequence getConfirmationMessage(CandidateInfo candidateInfo) {
        if (candidateInfo == null) {
            return null;
        }
        return Html.fromHtml(getContext().getString(R.string.autofill_confirmation_message, candidateInfo.loadLabel()));
    }

    @Override
    protected boolean setDefaultKey(String str) {
        String stringExtra;
        Settings.Secure.putString(getContext().getContentResolver(), "autofill_service", str);
        Activity activity = getActivity();
        if (activity != null && (stringExtra = activity.getIntent().getStringExtra("package_name")) != null) {
            activity.setResult((str == null || !str.startsWith(stringExtra)) ? 0 : -1);
            activity.finish();
            return true;
        }
        return true;
    }

    static final class AutofillSettingIntentProvider {
        private final Context mContext;
        private final String mSelectedKey;

        public AutofillSettingIntentProvider(Context context, String str) {
            this.mSelectedKey = str;
            this.mContext = context;
        }

        public Intent getIntent() {
            Iterator<ResolveInfo> it = this.mContext.getPackageManager().queryIntentServices(DefaultAutofillPicker.AUTOFILL_PROBE, 128).iterator();
            while (it.hasNext()) {
                ServiceInfo serviceInfo = it.next().serviceInfo;
                if (TextUtils.equals(this.mSelectedKey, new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString())) {
                    try {
                        String settingsActivity = new AutofillServiceInfo(this.mContext, serviceInfo).getSettingsActivity();
                        if (TextUtils.isEmpty(settingsActivity)) {
                            return null;
                        }
                        return new Intent("android.intent.action.MAIN").setComponent(new ComponentName(serviceInfo.packageName, settingsActivity));
                    } catch (SecurityException e) {
                        Log.w("DefaultAutofillPicker", "Error getting info for " + serviceInfo + ": " + e);
                        return null;
                    }
                }
            }
            return null;
        }
    }
}
