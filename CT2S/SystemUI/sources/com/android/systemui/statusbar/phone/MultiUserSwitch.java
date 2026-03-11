package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class MultiUserSwitch extends FrameLayout implements View.OnClickListener {
    private boolean mKeyguardMode;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private QSPanel mQsPanel;
    final UserManager mUserManager;

    public MultiUserSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mUserManager = UserManager.get(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
    }

    public void setQsPanel(QSPanel qsPanel) {
        this.mQsPanel = qsPanel;
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void setKeyguardMode(boolean keyguardShowing) {
        this.mKeyguardMode = keyguardShowing;
    }

    @Override
    public void onClick(View v) {
        UserSwitcherController userSwitcherController;
        if (UserSwitcherController.isUserSwitcherAvailable(this.mUserManager)) {
            if (this.mKeyguardMode) {
                if (this.mKeyguardUserSwitcher != null) {
                    this.mKeyguardUserSwitcher.show(true);
                    return;
                }
                return;
            } else {
                if (this.mQsPanel != null && (userSwitcherController = this.mQsPanel.getHost().getUserSwitcherController()) != null) {
                    this.mQsPanel.showDetailAdapter(true, userSwitcherController.userDetailAdapter);
                    return;
                }
                return;
            }
        }
        Intent intent = ContactsContract.QuickContact.composeQuickContactsIntent(getContext(), v, ContactsContract.Profile.CONTENT_URI, 3, null);
        getContext().startActivityAsUser(intent, new UserHandle(-2));
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        String text;
        UserSwitcherController controller;
        super.onPopulateAccessibilityEvent(event);
        if (isClickable()) {
            if (UserSwitcherController.isUserSwitcherAvailable(this.mUserManager)) {
                String currentUser = null;
                if (this.mQsPanel != null && (controller = this.mQsPanel.getHost().getUserSwitcherController()) != null) {
                    currentUser = controller.getCurrentUserName(this.mContext);
                }
                if (TextUtils.isEmpty(currentUser)) {
                    text = this.mContext.getString(R.string.accessibility_multi_user_switch_switcher);
                } else {
                    text = this.mContext.getString(R.string.accessibility_multi_user_switch_switcher_with_current, currentUser);
                }
            } else {
                text = this.mContext.getString(R.string.accessibility_multi_user_switch_quick_contact);
            }
            if (!TextUtils.isEmpty(text)) {
                event.getText().add(text);
            }
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
