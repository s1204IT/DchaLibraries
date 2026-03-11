package com.android.browser;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
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
        this.mHideBottomBarAnimatorListener = new Animator.AnimatorListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                this.this$0.onScrollChanged();
                this.this$0.setLayerType(0, null);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
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

    private int getVisibleBottomHeight() {
        return this.mBottomBar.getHeight();
    }

    private void initLayout(Context context) {
        LayoutInflater.from(context).inflate(2130968593, this);
        this.mBottomBar = (LinearLayout) findViewById(2131558440);
        this.mBottomBarBack = (ImageView) findViewById(2131558441);
        this.mBottomBarForward = (ImageView) findViewById(2131558442);
        this.mBottomBarTabs = (ImageView) findViewById(2131558443);
        this.mBottomBarBookmarks = (ImageView) findViewById(2131558445);
        this.mBottomBarTabCount = (TextView) findViewById(2131558444);
        this.mBottomBarBack.setOnClickListener(new View.OnClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                ((Controller) this.this$0.mUiController).onBackKey();
            }
        });
        this.mBottomBarBack.setOnLongClickListener(new View.OnLongClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(this.this$0.mUiController.getActivity(), this.this$0.mUiController.getActivity().getResources().getString(2131492984), 0).show();
                return false;
            }
        });
        this.mBottomBarForward.setOnClickListener(new View.OnClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                if (this.this$0.mUiController == null || this.this$0.mUiController.getCurrentTab() == null) {
                    return;
                }
                this.this$0.mUiController.getCurrentTab().goForward();
            }
        });
        this.mBottomBarForward.setOnLongClickListener(new View.OnLongClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(this.this$0.mUiController.getActivity(), this.this$0.mUiController.getActivity().getResources().getString(2131492985), 0).show();
                return false;
            }
        });
        this.mBottomBarTabs.setOnClickListener(new View.OnClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                ((PhoneUi) this.this$0.mBaseUi).toggleNavScreen();
            }
        });
        this.mBottomBarTabs.setOnLongClickListener(new View.OnLongClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(this.this$0.mUiController.getActivity(), this.this$0.mUiController.getActivity().getResources().getString(2131492917), 0).show();
                return false;
            }
        });
        this.mBottomBarBookmarks.setOnClickListener(new View.OnClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                this.this$0.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
            }
        });
        this.mBottomBarBookmarks.setOnLongClickListener(new View.OnLongClickListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(this.this$0.mUiController.getActivity(), this.this$0.mUiController.getActivity().getResources().getString(2131493026), 0).show();
                return false;
            }
        });
        this.mBottomBarTabCount.setText(Integer.toString(this.mUiController.getTabControl().getTabCount()));
        this.mTabControl.setOnTabCountChangedListener(new TabControl.OnTabCountChangedListener(this) {
            final BottomBar this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onTabCountChanged() {
                this.this$0.mBottomBarTabCount.setText(Integer.toString(this.this$0.mTabControl.getTabCount()));
            }
        });
    }

    private ViewGroup.MarginLayoutParams makeLayoutParams() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2);
        layoutParams.gravity = 80;
        return layoutParams;
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

    void cancelBottomBarAnimation() {
        if (this.mBottomBarAnimator != null) {
            this.mBottomBarAnimator.cancel();
            this.mBottomBarAnimator = null;
        }
    }

    public void changeBottomBarState(boolean z, boolean z2) {
        this.mBottomBarBack.setEnabled(z);
        this.mBottomBarForward.setEnabled(z2);
    }

    void hide() {
        if (this.mUseQuickControls || this.mUseFullScreen) {
            cancelBottomBarAnimation();
            int visibleBottomHeight = getVisibleBottomHeight();
            float translationY = getTranslationY();
            setLayerType(2, null);
            this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", translationY, visibleBottomHeight + translationY);
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
        this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", translationY2, visibleBottomHeight2 + translationY2);
        this.mBottomBarAnimator.addListener(this.mHideBottomBarAnimatorListener);
        setupBottomBarAnimator(this.mBottomBarAnimator);
        this.mBottomBarAnimator.start();
        this.mShowing = false;
    }

    boolean isShowing() {
        return this.mShowing;
    }

    public void onScrollChanged() {
        if (this.mShowing) {
            return;
        }
        setTranslationY(getVisibleBottomHeight());
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

    void setupBottomBarAnimator(Animator animator) {
        int integer = this.mContext.getResources().getInteger(2131623944);
        animator.setInterpolator(new DecelerateInterpolator(2.5f));
        animator.setDuration(integer);
    }

    void show() {
        cancelBottomBarAnimation();
        if (this.mUseQuickControls) {
            setVisibility(8);
            this.mShowing = false;
        } else {
            if (this.mShowing) {
                return;
            }
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
}
