package com.android.browser;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.browser.TabControl;
import com.android.browser.UI;

/* loaded from: classes.dex */
public class BottomBar extends LinearLayout {
    private BaseUi mBaseUi;
    protected LinearLayout mBottomBar;
    private Animator mBottomBarAnimator;
    protected ImageView mBottomBarBack;
    protected ImageView mBottomBarBookmarks;
    protected ImageView mBottomBarForward;
    protected TextView mBottomBarTabCount;
    protected ImageView mBottomBarTabs;
    private FrameLayout mContentView;
    private Context mContext;
    private Animator.AnimatorListener mHideBottomBarAnimatorListener;
    private boolean mShowing;
    private TabControl mTabControl;
    private UiController mUiController;
    private boolean mUseFullScreen;
    private boolean mUseQuickControls;

    public BottomBar(Context context, UiController uiController, BaseUi baseUi, TabControl tabControl, FrameLayout frameLayout) {
        super(context, null);
        this.mHideBottomBarAnimatorListener = new Animator.AnimatorListener() { // from class: com.android.browser.BottomBar.10
            AnonymousClass10() {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                BottomBar.this.onScrollChanged();
                BottomBar.this.setLayerType(0, null);
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }
        };
        this.mContext = context;
        this.mUiController = uiController;
        this.mBaseUi = baseUi;
        this.mTabControl = tabControl;
        this.mContentView = frameLayout;
        initLayout(context);
        setupBottomBar();
    }

    private void initLayout(Context context) {
        LayoutInflater.from(context).inflate(R.layout.bottom_bar, this);
        this.mBottomBar = (LinearLayout) findViewById(R.id.bottombar);
        this.mBottomBarBack = (ImageView) findViewById(R.id.back);
        this.mBottomBarForward = (ImageView) findViewById(R.id.forward);
        this.mBottomBarTabs = (ImageView) findViewById(R.id.tabs);
        this.mBottomBarBookmarks = (ImageView) findViewById(R.id.bookmarks);
        this.mBottomBarTabCount = (TextView) findViewById(R.id.tabcount);
        this.mBottomBarBack.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.BottomBar.1
            AnonymousClass1() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ((Controller) BottomBar.this.mUiController).onBackKey();
            }
        });
        this.mBottomBarBack.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.android.browser.BottomBar.2
            AnonymousClass2() {
            }

            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.back), 0).show();
                return false;
            }
        });
        this.mBottomBarForward.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.BottomBar.3
            AnonymousClass3() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (BottomBar.this.mUiController != null && BottomBar.this.mUiController.getCurrentTab() != null) {
                    BottomBar.this.mUiController.getCurrentTab().goForward();
                }
            }
        });
        this.mBottomBarForward.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.android.browser.BottomBar.4
            AnonymousClass4() {
            }

            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.forward), 0).show();
                return false;
            }
        });
        this.mBottomBarTabs.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.BottomBar.5
            AnonymousClass5() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ((PhoneUi) BottomBar.this.mBaseUi).toggleNavScreen();
            }
        });
        this.mBottomBarTabs.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.android.browser.BottomBar.6
            AnonymousClass6() {
            }

            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.tabs), 0).show();
                return false;
            }
        });
        this.mBottomBarBookmarks.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.BottomBar.7
            AnonymousClass7() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                BottomBar.this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
            }
        });
        this.mBottomBarBookmarks.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.android.browser.BottomBar.8
            AnonymousClass8() {
            }

            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.bookmarks), 0).show();
                return false;
            }
        });
        this.mBottomBarTabCount.setText(Integer.toString(this.mUiController.getTabControl().getTabCount()));
        this.mTabControl.setOnTabCountChangedListener(new TabControl.OnTabCountChangedListener() { // from class: com.android.browser.BottomBar.9
            AnonymousClass9() {
            }

            @Override // com.android.browser.TabControl.OnTabCountChangedListener
            public void onTabCountChanged() {
                BottomBar.this.mBottomBarTabCount.setText(Integer.toString(BottomBar.this.mTabControl.getTabCount()));
            }
        });
    }

    /* renamed from: com.android.browser.BottomBar$1 */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            ((Controller) BottomBar.this.mUiController).onBackKey();
        }
    }

    /* renamed from: com.android.browser.BottomBar$2 */
    class AnonymousClass2 implements View.OnLongClickListener {
        AnonymousClass2() {
        }

        @Override // android.view.View.OnLongClickListener
        public boolean onLongClick(View view) {
            Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.back), 0).show();
            return false;
        }
    }

    /* renamed from: com.android.browser.BottomBar$3 */
    class AnonymousClass3 implements View.OnClickListener {
        AnonymousClass3() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (BottomBar.this.mUiController != null && BottomBar.this.mUiController.getCurrentTab() != null) {
                BottomBar.this.mUiController.getCurrentTab().goForward();
            }
        }
    }

    /* renamed from: com.android.browser.BottomBar$4 */
    class AnonymousClass4 implements View.OnLongClickListener {
        AnonymousClass4() {
        }

        @Override // android.view.View.OnLongClickListener
        public boolean onLongClick(View view) {
            Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.forward), 0).show();
            return false;
        }
    }

    /* renamed from: com.android.browser.BottomBar$5 */
    class AnonymousClass5 implements View.OnClickListener {
        AnonymousClass5() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            ((PhoneUi) BottomBar.this.mBaseUi).toggleNavScreen();
        }
    }

    /* renamed from: com.android.browser.BottomBar$6 */
    class AnonymousClass6 implements View.OnLongClickListener {
        AnonymousClass6() {
        }

        @Override // android.view.View.OnLongClickListener
        public boolean onLongClick(View view) {
            Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.tabs), 0).show();
            return false;
        }
    }

    /* renamed from: com.android.browser.BottomBar$7 */
    class AnonymousClass7 implements View.OnClickListener {
        AnonymousClass7() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            BottomBar.this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
        }
    }

    /* renamed from: com.android.browser.BottomBar$8 */
    class AnonymousClass8 implements View.OnLongClickListener {
        AnonymousClass8() {
        }

        @Override // android.view.View.OnLongClickListener
        public boolean onLongClick(View view) {
            Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.bookmarks), 0).show();
            return false;
        }
    }

    /* renamed from: com.android.browser.BottomBar$9 */
    class AnonymousClass9 implements TabControl.OnTabCountChangedListener {
        AnonymousClass9() {
        }

        @Override // com.android.browser.TabControl.OnTabCountChangedListener
        public void onTabCountChanged() {
            BottomBar.this.mBottomBarTabCount.setText(Integer.toString(BottomBar.this.mTabControl.getTabCount()));
        }
    }

    private void setupBottomBar() {
        ViewGroup viewGroup = (ViewGroup) getParent();
        show();
        if (viewGroup != null) {
            viewGroup.removeView(this);
        }
        this.mContentView.addView(this, makeLayoutParams());
        this.mBaseUi.setContentViewMarginBottom(0);
    }

    public void setFullScreen(boolean z) {
        this.mUseFullScreen = z;
        if (z) {
            setVisibility(8);
        } else {
            setVisibility(0);
        }
    }

    public void setUseQuickControls(boolean z) {
        this.mUseQuickControls = z;
        if (z) {
            setVisibility(8);
        } else {
            setVisibility(0);
        }
    }

    void setupBottomBarAnimator(Animator animator) throws Resources.NotFoundException {
        int integer = this.mContext.getResources().getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(2.5f));
        animator.setDuration(integer);
    }

    void show() {
        cancelBottomBarAnimation();
        if (this.mUseQuickControls) {
            setVisibility(8);
            this.mShowing = false;
        } else if (!this.mShowing) {
            setVisibility(0);
            int visibleBottomHeight = getVisibleBottomHeight();
            float translationY = getTranslationY();
            setLayerType(2, null);
            this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", translationY, translationY - visibleBottomHeight);
            setupBottomBarAnimator(this.mBottomBarAnimator);
            this.mBottomBarAnimator.start();
            this.mShowing = true;
        }
    }

    void hide() throws Resources.NotFoundException {
        if (this.mUseQuickControls || this.mUseFullScreen) {
            cancelBottomBarAnimation();
            int visibleBottomHeight = getVisibleBottomHeight();
            float translationY = getTranslationY();
            setLayerType(2, null);
            this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", translationY, translationY + visibleBottomHeight);
            this.mBottomBarAnimator.addListener(this.mHideBottomBarAnimatorListener);
            setupBottomBarAnimator(this.mBottomBarAnimator);
            this.mBottomBarAnimator.start();
            setVisibility(8);
            this.mShowing = false;
            return;
        }
        setVisibility(0);
        cancelBottomBarAnimation();
        int visibleBottomHeight2 = getVisibleBottomHeight();
        float translationY2 = getTranslationY();
        setLayerType(2, null);
        this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", translationY2, translationY2 + visibleBottomHeight2);
        this.mBottomBarAnimator.addListener(this.mHideBottomBarAnimatorListener);
        setupBottomBarAnimator(this.mBottomBarAnimator);
        this.mBottomBarAnimator.start();
        this.mShowing = false;
    }

    boolean isShowing() {
        return this.mShowing;
    }

    void cancelBottomBarAnimation() {
        if (this.mBottomBarAnimator != null) {
            this.mBottomBarAnimator.cancel();
            this.mBottomBarAnimator = null;
        }
    }

    /* renamed from: com.android.browser.BottomBar$10 */
    class AnonymousClass10 implements Animator.AnimatorListener {
        AnonymousClass10() {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            BottomBar.this.onScrollChanged();
            BottomBar.this.setLayerType(0, null);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }
    }

    private int getVisibleBottomHeight() {
        return this.mBottomBar.getHeight();
    }

    private ViewGroup.MarginLayoutParams makeLayoutParams() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2);
        layoutParams.gravity = 80;
        return layoutParams;
    }

    public void onScrollChanged() {
        if (!this.mShowing) {
            setTranslationY(getVisibleBottomHeight());
        }
    }

    public void changeBottomBarState(boolean z, boolean z2) {
        this.mBottomBarBack.setEnabled(z);
        this.mBottomBarForward.setEnabled(z2);
    }
}
