package com.android.internal.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.Slog;
import com.android.internal.R;

public class ChooserActivity extends ResolverActivity {
    private static final String TAG = "ChooserActivity";
    private IntentSender mChosenComponentSender;
    private Bundle mReplacementExtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (!(targetParcelable instanceof Intent)) {
            Log.w(TAG, "Target is not an intent: " + targetParcelable);
            finish();
            super.onCreate(null);
            return;
        }
        Intent target = (Intent) targetParcelable;
        if (target != null) {
            modifyTargetIntent(target);
        }
        this.mReplacementExtras = intent.getBundleExtra(Intent.EXTRA_REPLACEMENT_EXTRAS);
        CharSequence title = intent.getCharSequenceExtra(Intent.EXTRA_TITLE);
        int defaultTitleRes = 0;
        if (title == null) {
            defaultTitleRes = R.string.chooseActivity;
        }
        Parcelable[] pa = intent.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
        Intent[] initialIntents = null;
        if (pa != null) {
            initialIntents = new Intent[pa.length];
            for (int i = 0; i < pa.length; i++) {
                if (!(pa[i] instanceof Intent)) {
                    Log.w(TAG, "Initial intent #" + i + " not an Intent: " + pa[i]);
                    finish();
                    super.onCreate(null);
                    return;
                } else {
                    Intent in = (Intent) pa[i];
                    modifyTargetIntent(in);
                    initialIntents[i] = in;
                }
            }
        }
        this.mChosenComponentSender = (IntentSender) intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER);
        setSafeForwardingMode(true);
        super.onCreate(savedInstanceState, target, title, defaultTitleRes, initialIntents, null, false);
    }

    @Override
    public Intent getReplacementIntent(ActivityInfo aInfo, Intent defIntent) {
        Bundle replExtras;
        Intent result = defIntent;
        if (this.mReplacementExtras != null && (replExtras = this.mReplacementExtras.getBundle(aInfo.packageName)) != null) {
            result = new Intent(defIntent);
            result.putExtras(replExtras);
        }
        if (aInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_USER_OWNER) || aInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            return Intent.createChooser(result, getIntent().getCharSequenceExtra(Intent.EXTRA_TITLE));
        }
        return result;
    }

    @Override
    public void onActivityStarted(Intent intent) {
        ComponentName target;
        if (this.mChosenComponentSender != null && (target = intent.getComponent()) != null) {
            Intent fillIn = new Intent().putExtra(Intent.EXTRA_CHOSEN_COMPONENT, target);
            try {
                this.mChosenComponentSender.sendIntent(this, -1, fillIn, null, null);
            } catch (IntentSender.SendIntentException e) {
                Slog.e(TAG, "Unable to launch supplied IntentSender to report the chosen component: " + e);
            }
        }
    }

    private void modifyTargetIntent(Intent in) {
        String action = in.getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            in.addFlags(134742016);
        }
    }
}
