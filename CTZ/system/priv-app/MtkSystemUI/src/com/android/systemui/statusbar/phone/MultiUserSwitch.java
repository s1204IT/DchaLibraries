package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserSwitcherController;

/* loaded from: classes.dex */
public class MultiUserSwitch extends FrameLayout implements View.OnClickListener {
    private boolean mKeyguardMode;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    protected QSPanel mQsPanel;
    private final int[] mTmpInt2;
    private UserSwitcherController.BaseUserAdapter mUserListener;
    final UserManager mUserManager;
    protected UserSwitcherController mUserSwitcherController;

    public MultiUserSwitch(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTmpInt2 = new int[2];
        this.mUserManager = UserManager.get(getContext());
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
        refreshContentDescription();
    }

    public void setQsPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
        setUserSwitcherController((UserSwitcherController) Dependency.get(UserSwitcherController.class));
    }

    public void setUserSwitcherController(UserSwitcherController userSwitcherController) {
        this.mUserSwitcherController = userSwitcherController;
        registerListener();
        refreshContentDescription();
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void setKeyguardMode(boolean z) {
        this.mKeyguardMode = z;
        registerListener();
    }

    private void registerListener() {
        UserSwitcherController userSwitcherController;
        if (this.mUserManager.isUserSwitcherEnabled() && this.mUserListener == null && (userSwitcherController = this.mUserSwitcherController) != null) {
            this.mUserListener = new UserSwitcherController.BaseUserAdapter(userSwitcherController) { // from class: com.android.systemui.statusbar.phone.MultiUserSwitch.1
                AnonymousClass1(UserSwitcherController userSwitcherController2) {
                    super(userSwitcherController2);
                }

                @Override // android.widget.BaseAdapter
                public void notifyDataSetChanged() {
                    MultiUserSwitch.this.refreshContentDescription();
                }

                @Override // android.widget.Adapter
                public View getView(int i, View view, ViewGroup viewGroup) {
                    return null;
                }
            };
            refreshContentDescription();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.MultiUserSwitch$1 */
    class AnonymousClass1 extends UserSwitcherController.BaseUserAdapter {
        AnonymousClass1(UserSwitcherController userSwitcherController2) {
            super(userSwitcherController2);
        }

        @Override // android.widget.BaseAdapter
        public void notifyDataSetChanged() {
            MultiUserSwitch.this.refreshContentDescription();
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            return null;
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (this.mUserManager.isUserSwitcherEnabled()) {
            if (this.mKeyguardMode) {
                if (this.mKeyguardUserSwitcher != null) {
                    this.mKeyguardUserSwitcher.show(true);
                    return;
                }
                return;
            } else {
                if (this.mQsPanel != null && this.mUserSwitcherController != null) {
                    View childAt = getChildCount() > 0 ? getChildAt(0) : this;
                    childAt.getLocationInWindow(this.mTmpInt2);
                    int[] iArr = this.mTmpInt2;
                    iArr[0] = iArr[0] + (childAt.getWidth() / 2);
                    int[] iArr2 = this.mTmpInt2;
                    iArr2[1] = iArr2[1] + (childAt.getHeight() / 2);
                    this.mQsPanel.showDetailAdapter(true, getUserDetailAdapter(), this.mTmpInt2);
                    return;
                }
                return;
            }
        }
        if (this.mQsPanel != null) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(ContactsContract.QuickContact.composeQuickContactsIntent(getContext(), view, ContactsContract.Profile.CONTENT_URI, 3, null), 0);
        }
    }

    @Override // android.view.View
    public void setClickable(boolean z) {
        super.setClickable(z);
        refreshContentDescription();
    }

    private void refreshContentDescription() {
        String currentUserName;
        if (this.mUserManager.isUserSwitcherEnabled() && this.mUserSwitcherController != null) {
            currentUserName = this.mUserSwitcherController.getCurrentUserName(this.mContext);
        } else {
            currentUserName = null;
        }
        String string = TextUtils.isEmpty(currentUserName) ? null : this.mContext.getString(R.string.accessibility_quick_settings_user, currentUserName);
        if (!TextUtils.equals(getContentDescription(), string)) {
            setContentDescription(string);
        }
    }

    @Override // android.view.View
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(Button.class.getName());
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(Button.class.getName());
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    protected DetailAdapter getUserDetailAdapter() {
        return this.mUserSwitcherController.userDetailAdapter;
    }
}
