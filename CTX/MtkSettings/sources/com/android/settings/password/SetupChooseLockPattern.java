package com.android.settings.password;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.password.ChooseLockTypeDialogFragment;
import com.android.settings.password.SetupChooseLockPattern;

public class SetupChooseLockPattern extends ChooseLockPattern {
    public static Intent modifyIntentForSetup(Context context, Intent intent) {
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return SetupChooseLockPatternFragment.class.getName().equals(str);
    }

    @Override
    Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPatternFragment.class;
    }

    public static class SetupChooseLockPatternFragment extends ChooseLockPattern.ChooseLockPatternFragment implements ChooseLockTypeDialogFragment.OnLockTypeSelectedListener {
        private Button mOptionsButton;

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
            if (!getResources().getBoolean(R.bool.config_lock_pattern_minimal_ui)) {
                this.mOptionsButton = (Button) viewOnCreateView.findViewById(R.id.screen_lock_options);
                this.mOptionsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        SetupChooseLockPattern.SetupChooseLockPatternFragment setupChooseLockPatternFragment = this.f$0;
                        ChooseLockTypeDialogFragment.newInstance(setupChooseLockPatternFragment.mUserId).show(setupChooseLockPatternFragment.getChildFragmentManager(), (String) null);
                    }
                });
            }
            if (!this.mForFingerprint) {
                Button button = (Button) viewOnCreateView.findViewById(R.id.skip_button);
                button.setVisibility(0);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        SetupChooseLockPattern.SetupChooseLockPatternFragment setupChooseLockPatternFragment = this.f$0;
                        SetupSkipDialog.newInstance(setupChooseLockPatternFragment.getActivity().getIntent().getBooleanExtra(":settings:frp_supported", false)).show(setupChooseLockPatternFragment.getFragmentManager());
                    }
                });
            }
            return viewOnCreateView;
        }

        @Override
        public void onLockTypeSelected(ScreenLockType screenLockType) {
            if (ScreenLockType.PATTERN == screenLockType) {
                return;
            }
            startChooseLockActivity(screenLockType, getActivity());
        }

        @Override
        protected void updateStage(ChooseLockPattern.ChooseLockPatternFragment.Stage stage) {
            super.updateStage(stage);
            if (!getResources().getBoolean(R.bool.config_lock_pattern_minimal_ui) && this.mOptionsButton != null) {
                this.mOptionsButton.setVisibility(stage == ChooseLockPattern.ChooseLockPatternFragment.Stage.Introduction ? 0 : 4);
            }
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            SetupRedactionInterstitial.setEnabled(context, true);
            return null;
        }
    }
}
