package com.android.settings.password;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.password.ChooseLockPassword;
import com.android.settings.password.ChooseLockTypeDialogFragment;

public class SetupChooseLockPassword extends ChooseLockPassword {
    public static Intent modifyIntentForSetup(Context context, Intent intent) {
        intent.setClass(context, SetupChooseLockPassword.class);
        intent.putExtra("extra_prefs_show_button_bar", false);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return SetupChooseLockPasswordFragment.class.getName().equals(str);
    }

    @Override
    Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }

    public static class SetupChooseLockPasswordFragment extends ChooseLockPassword.ChooseLockPasswordFragment implements ChooseLockTypeDialogFragment.OnLockTypeSelectedListener {
        private Button mOptionsButton;

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            Activity activity = getActivity();
            boolean z = new ChooseLockGenericController(activity, this.mUserId).getVisibleScreenLockTypes(65536, false).size() > 0;
            boolean booleanExtra = activity.getIntent().getBooleanExtra("show_options_button", false);
            if (!z) {
                Log.w("SetupChooseLockPassword", "Visible screen lock types is empty!");
            }
            if (booleanExtra && z) {
                this.mOptionsButton = (Button) view.findViewById(R.id.screen_lock_options);
                this.mOptionsButton.setVisibility(0);
                this.mOptionsButton.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.screen_lock_options) {
                ChooseLockTypeDialogFragment.newInstance(this.mUserId).show(getChildFragmentManager(), (String) null);
            } else if (id == R.id.skip_button) {
                SetupSkipDialog.newInstance(getActivity().getIntent().getBooleanExtra(":settings:frp_supported", false)).show(getFragmentManager());
            } else {
                super.onClick(view);
            }
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            SetupRedactionInterstitial.setEnabled(context, true);
            return null;
        }

        @Override
        public void onLockTypeSelected(ScreenLockType screenLockType) {
            if (screenLockType == (this.mIsAlphaMode ? ScreenLockType.PASSWORD : ScreenLockType.PIN)) {
                return;
            }
            startChooseLockActivity(screenLockType, getActivity());
        }

        @Override
        protected void updateUi() {
            super.updateUi();
            this.mSkipButton.setVisibility(this.mForFingerprint ? 8 : 0);
            if (this.mOptionsButton != null) {
                this.mOptionsButton.setVisibility(this.mUiStage != ChooseLockPassword.ChooseLockPasswordFragment.Stage.Introduction ? 8 : 0);
            }
        }
    }
}
