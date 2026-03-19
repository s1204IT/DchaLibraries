package com.android.storagemanager.deletionhelper;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settingslib.widget.LinkTextView;
import com.android.storagemanager.ButtonBarProvider;
import com.android.storagemanager.R;
import com.android.storagemanager.utils.Utils;

public class DeletionHelperActivity extends Activity implements ButtonBarProvider {
    private ViewGroup mButtonBar;
    private DeletionHelperSettings mFragment;
    private boolean mIsShowingInterstitial;
    private Button mNextButton;
    private Button mSkipButton;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.settings_main_prefs);
        setIsEmptyState(false);
        if (bundle == null) {
            FragmentManager fragmentManager = getFragmentManager();
            this.mFragment = DeletionHelperSettings.newInstance(0);
            fragmentManager.beginTransaction().replace(R.id.main_content, this.mFragment).commit();
        }
        ((LinkTextView) findViewById(R.id.all_items_link)).setText(NoThresholdSpan.linkify(new SpannableString(getString(R.string.empty_state_review_items_link).toUpperCase()), this), TextView.BufferType.SPANNABLE);
        this.mButtonBar = (ViewGroup) findViewById(R.id.button_bar);
        this.mNextButton = (Button) findViewById(R.id.next_button);
        this.mSkipButton = (Button) findViewById(R.id.skip_button);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        this.mFragment.onRequestPermissionsResult(i, strArr, iArr);
    }

    void setIsEmptyState(boolean z) {
        View viewFindViewById = findViewById(R.id.empty_state);
        boolean z2 = false;
        findViewById(R.id.main_content).setVisibility(z ? 8 : 0);
        viewFindViewById.setVisibility(z ? 0 : 8);
        findViewById(R.id.button_bar).setVisibility(z ? 8 : 0);
        setTitle(z ? R.string.empty_state_title : R.string.deletion_helper_title);
        if (z && viewFindViewById.getVisibility() != 0) {
            z2 = true;
        }
        this.mIsShowingInterstitial = z2;
        invalidateOptionsMenu();
    }

    public boolean isLoadingVisible() {
        View viewFindViewById = findViewById(R.id.loading_container);
        return viewFindViewById != null && viewFindViewById.getVisibility() == 0;
    }

    public void setLoading(View view, boolean z, boolean z2) {
        Utils.handleLoadingContainer(findViewById(R.id.loading_container), view, !z, z2);
        getButtonBar().setVisibility(z ? 8 : 0);
    }

    @Override
    public ViewGroup getButtonBar() {
        return this.mButtonBar;
    }

    @Override
    public Button getNextButton() {
        return this.mNextButton;
    }

    @Override
    public Button getSkipButton() {
        return this.mSkipButton;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Settings.Global.getInt(getContentResolver(), "enable_deletion_helper_no_threshold_toggle", 1) < 1 || this.mIsShowingInterstitial) {
            return false;
        }
        getMenuInflater().inflate(R.menu.deletion_helper_settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int i;
        FragmentManager fragmentManager = getFragmentManager();
        int itemId = menuItem.getItemId();
        if (itemId == R.id.default_threshold) {
            i = 0;
        } else {
            if (itemId != R.id.no_threshold) {
                return super.onOptionsItemSelected(menuItem);
            }
            i = 1;
        }
        this.mFragment = DeletionHelperSettings.newInstance(i);
        fragmentManager.beginTransaction().replace(R.id.main_content, this.mFragment).commit();
        return true;
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    private static class NoThresholdSpan extends ClickableSpan {
        private final DeletionHelperActivity mParent;

        public NoThresholdSpan(DeletionHelperActivity deletionHelperActivity) {
            this.mParent = deletionHelperActivity;
        }

        @Override
        public void onClick(View view) {
            FragmentManager fragmentManager = this.mParent.getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.main_content, DeletionHelperSettings.newInstance(1)).commit();
            this.mParent.setIsEmptyState(false);
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            super.updateDrawState(textPaint);
            textPaint.setUnderlineText(false);
        }

        public static SpannableString linkify(SpannableString spannableString, DeletionHelperActivity deletionHelperActivity) {
            spannableString.setSpan(new NoThresholdSpan(deletionHelperActivity), 0, spannableString.length(), 33);
            return spannableString;
        }
    }
}
