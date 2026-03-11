package com.android.settings.fingerprint;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settingslib.HelpUtils;
import com.android.setupwizardlib.SetupWizardRecyclerLayout;
import com.android.setupwizardlib.items.IItem;
import com.android.setupwizardlib.items.Item;
import com.android.setupwizardlib.items.RecyclerItemAdapter;

public class FingerprintEnrollIntroduction extends FingerprintEnrollBase implements RecyclerItemAdapter.OnItemSelectedListener {
    private boolean mHasPassword;
    private UserManager mUserManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_introduction);
        setHeaderText(R.string.security_settings_fingerprint_enroll_introduction_title);
        SetupWizardRecyclerLayout layout = (SetupWizardRecyclerLayout) findViewById(R.id.setup_wizard_layout);
        this.mUserManager = UserManager.get(this);
        RecyclerItemAdapter adapter = (RecyclerItemAdapter) layout.getAdapter();
        adapter.setOnItemSelectedListener(this);
        Item item = (Item) adapter.findItemById(R.id.fingerprint_introduction_message);
        String linkUrl = getString(R.string.help_url_fingerprint);
        if (Settings.Global.getInt(getContentResolver(), "device_provisioned", 0) == 0) {
            linkUrl = "";
        }
        item.setTitle(LearnMoreSpan.linkify(getText(R.string.security_settings_fingerprint_enroll_introduction_message), linkUrl));
        layout.setDividerInset(0);
        updatePasswordQuality();
    }

    private void updatePasswordQuality() {
        int passwordQuality = new ChooseLockSettingsHelper(this).utils().getActivePasswordQuality(this.mUserManager.getCredentialOwnerProfile(this.mUserId));
        this.mHasPassword = passwordQuality != 0;
    }

    @Override
    protected void onNextButtonClick() {
        if (!this.mHasPassword) {
            launchChooseLock();
        } else {
            launchFindSensor(null);
        }
    }

    private void launchChooseLock() {
        Intent intent = getChooseLockIntent();
        long challenge = ((FingerprintManager) getSystemService(FingerprintManager.class)).preEnroll();
        intent.putExtra("minimum_quality", 65536);
        intent.putExtra("hide_disabled_prefs", true);
        intent.putExtra("has_challenge", true);
        intent.putExtra("challenge", challenge);
        intent.putExtra("for_fingerprint", true);
        if (this.mUserId != -10000) {
            intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
        }
        startActivityForResult(intent, 1);
    }

    private void launchFindSensor(byte[] token) {
        Intent intent = getFindSensorIntent();
        if (token != null) {
            intent.putExtra("hw_auth_token", token);
        }
        if (this.mUserId != -10000) {
            intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
        }
        startActivityForResult(intent, 2);
    }

    protected Intent getChooseLockIntent() {
        return new Intent(this, (Class<?>) ChooseLockGeneric.class);
    }

    protected Intent getFindSensorIntent() {
        Intent intent = new Intent(this, (Class<?>) FingerprintEnrollFindSensor.class);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean isResultFinished = resultCode == 1;
        if (requestCode == 2) {
            if (isResultFinished || resultCode == 2) {
                int result = isResultFinished ? -1 : 2;
                setResult(result, data);
                finish();
                return;
            }
        } else if (requestCode == 1 && isResultFinished) {
            updatePasswordQuality();
            byte[] token = data.getByteArrayExtra("hw_auth_token");
            launchFindSensor(token);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemSelected(IItem item) {
        switch (((Item) item).getId()) {
            case R.id.cancel_button:
                onCancelButtonClick();
                break;
            case R.id.next_button:
                onNextButtonClick();
                break;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 243;
    }

    protected void onCancelButtonClick() {
        finish();
    }

    private static class LearnMoreSpan extends URLSpan {
        private static final Typeface TYPEFACE_MEDIUM = Typeface.create("sans-serif-medium", 0);

        private LearnMoreSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
            if (intent == null) {
                Log.w("LearnMoreSpan", "Null help intent.");
                return;
            }
            try {
                widget.startActivityForResult(intent, 3);
            } catch (ActivityNotFoundException e) {
                Log.w("LearnMoreSpan", "Actvity was not found for intent, " + intent.toString());
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = (Annotation[]) msg.getSpans(0, msg.length(), Annotation.class);
            if (TextUtils.isEmpty(uri)) {
                CharSequence ret = rawText;
                for (Annotation annotation : spans) {
                    int start = msg.getSpanStart(annotation);
                    int end = msg.getSpanEnd(annotation);
                    ret = TextUtils.concat(ret.subSequence(0, start), msg.subSequence(end, msg.length()));
                }
                return ret;
            }
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            for (Annotation annotation2 : spans) {
                int start2 = msg.getSpanStart(annotation2);
                int end2 = msg.getSpanEnd(annotation2);
                LearnMoreSpan link = new LearnMoreSpan(uri);
                builder.setSpan(link, start2, end2, msg.getSpanFlags(link));
            }
            return builder;
        }
    }
}
