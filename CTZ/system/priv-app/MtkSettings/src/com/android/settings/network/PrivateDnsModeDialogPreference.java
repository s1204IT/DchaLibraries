package com.android.settings.network;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;
import android.system.Os;
import android.system.OsConstants;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.CustomDialogPreference;
import com.android.settingslib.HelpUtils;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class PrivateDnsModeDialogPreference extends CustomDialogPreference implements DialogInterface.OnClickListener, TextWatcher, RadioGroup.OnCheckedChangeListener {
    private static final int[] ADDRESS_FAMILIES;
    static final String HOSTNAME_KEY = "private_dns_specifier";
    static final String MODE_KEY = "private_dns_mode";
    private static final Map<String, Integer> PRIVATE_DNS_MAP = new HashMap();
    EditText mEditText;
    String mMode;
    RadioGroup mRadioGroup;
    private final AnnotationSpan.LinkInfo mUrlLinkInfo;

    static {
        PRIVATE_DNS_MAP.put("off", Integer.valueOf(R.id.private_dns_mode_off));
        PRIVATE_DNS_MAP.put("opportunistic", Integer.valueOf(R.id.private_dns_mode_opportunistic));
        PRIVATE_DNS_MAP.put("hostname", Integer.valueOf(R.id.private_dns_mode_provider));
        ADDRESS_FAMILIES = new int[]{OsConstants.AF_INET, OsConstants.AF_INET6};
    }

    public static String getModeFromSettings(ContentResolver contentResolver) {
        String string = Settings.Global.getString(contentResolver, MODE_KEY);
        if (!PRIVATE_DNS_MAP.containsKey(string)) {
            string = Settings.Global.getString(contentResolver, "private_dns_default_mode");
        }
        return PRIVATE_DNS_MAP.containsKey(string) ? string : "opportunistic";
    }

    public static String getHostnameFromSettings(ContentResolver contentResolver) {
        return Settings.Global.getString(contentResolver, HOSTNAME_KEY);
    }

    public PrivateDnsModeDialogPreference(Context context) {
        super(context);
        this.mUrlLinkInfo = new AnnotationSpan.LinkInfo("url", $$Lambda$PrivateDnsModeDialogPreference$I1bK8FTmQSNCcqXqZ0usMONEsU.INSTANCE);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mUrlLinkInfo = new AnnotationSpan.LinkInfo("url", $$Lambda$PrivateDnsModeDialogPreference$I1bK8FTmQSNCcqXqZ0usMONEsU.INSTANCE);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mUrlLinkInfo = new AnnotationSpan.LinkInfo("url", $$Lambda$PrivateDnsModeDialogPreference$I1bK8FTmQSNCcqXqZ0usMONEsU.INSTANCE);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mUrlLinkInfo = new AnnotationSpan.LinkInfo("url", $$Lambda$PrivateDnsModeDialogPreference$I1bK8FTmQSNCcqXqZ0usMONEsU.INSTANCE);
    }

    static /* synthetic */ void lambda$new$0(View view) throws Resources.NotFoundException, URISyntaxException {
        Context context = view.getContext();
        Intent helpIntent = HelpUtils.getHelpIntent(context, context.getString(R.string.help_uri_private_dns), context.getClass().getName());
        if (helpIntent != null) {
            try {
                view.startActivityForResult(helpIntent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w("PrivateDnsModeDialog", "Activity was not found for intent, " + helpIntent.toString());
            }
        }
    }

    @Override // com.android.settingslib.CustomDialogPreference
    protected void onBindDialogView(View view) {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        this.mMode = getModeFromSettings(context.getContentResolver());
        this.mEditText = (EditText) view.findViewById(R.id.private_dns_mode_provider_hostname);
        this.mEditText.addTextChangedListener(this);
        this.mEditText.setText(getHostnameFromSettings(contentResolver));
        this.mRadioGroup = (RadioGroup) view.findViewById(R.id.private_dns_radio_group);
        this.mRadioGroup.setOnCheckedChangeListener(this);
        this.mRadioGroup.check(PRIVATE_DNS_MAP.getOrDefault(this.mMode, Integer.valueOf(R.id.private_dns_mode_opportunistic)).intValue());
        TextView textView = (TextView) view.findViewById(R.id.private_dns_help_info);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(context, "url", HelpUtils.getHelpIntent(context, context.getString(R.string.help_uri_private_dns), context.getClass().getName()));
        if (linkInfo.isActionable()) {
            textView.setText(AnnotationSpan.linkify(context.getText(R.string.private_dns_help_message), linkInfo));
        }
    }

    @Override // com.android.settingslib.CustomDialogPreference
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            Context context = getContext();
            if (this.mMode.equals("hostname")) {
                Settings.Global.putString(context.getContentResolver(), HOSTNAME_KEY, this.mEditText.getText().toString());
            }
            FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, 1249, this.mMode, new Pair[0]);
            Settings.Global.putString(context.getContentResolver(), MODE_KEY, this.mMode);
        }
    }

    @Override // android.widget.RadioGroup.OnCheckedChangeListener
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch (i) {
            case R.id.private_dns_mode_off /* 2131362416 */:
                this.mMode = "off";
                break;
            case R.id.private_dns_mode_opportunistic /* 2131362417 */:
                this.mMode = "opportunistic";
                break;
            case R.id.private_dns_mode_provider /* 2131362418 */:
                this.mMode = "hostname";
                break;
        }
        updateDialogInfo();
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        updateDialogInfo();
    }

    private boolean isWeaklyValidatedHostname(String str) {
        if (!str.matches("^[a-zA-Z0-9_.-]+$")) {
            return false;
        }
        for (int i : ADDRESS_FAMILIES) {
            if (Os.inet_pton(i, str) != null) {
                return false;
            }
        }
        return true;
    }

    private Button getSaveButton() {
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog == null) {
            return null;
        }
        return alertDialog.getButton(-1);
    }

    private void updateDialogInfo() {
        boolean zIsWeaklyValidatedHostname;
        boolean zEquals = "hostname".equals(this.mMode);
        if (this.mEditText != null) {
            this.mEditText.setEnabled(zEquals);
        }
        Button saveButton = getSaveButton();
        if (saveButton != null) {
            if (zEquals) {
                zIsWeaklyValidatedHostname = isWeaklyValidatedHostname(this.mEditText.getText().toString());
            } else {
                zIsWeaklyValidatedHostname = true;
            }
            saveButton.setEnabled(zIsWeaklyValidatedHostname);
        }
    }
}
