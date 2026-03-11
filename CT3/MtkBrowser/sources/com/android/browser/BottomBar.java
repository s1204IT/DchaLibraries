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

    public BottomBar(Context context, UiController controller, BaseUi ui, TabControl tabControl, FrameLayout contentView) {
        super(context, null);
        this.mHideBottomBarAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                BottomBar.this.onScrollChanged();
                BottomBar.this.setLayerType(0, null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        };
        this.mContext = context;
        this.mUiController = controller;
        this.mBaseUi = ui;
        this.mTabControl = tabControl;
        this.mContentView = contentView;
        initLayout(context);
        setupBottomBar();
    }

    private void initLayout(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.bottom_bar, this);
        this.mBottomBar = (LinearLayout) findViewById(R.id.bottombar);
        this.mBottomBarBack = (ImageView) findViewById(R.id.back);
        this.mBottomBarForward = (ImageView) findViewById(R.id.forward);
        this.mBottomBarTabs = (ImageView) findViewById(R.id.tabs);
        this.mBottomBarBookmarks = (ImageView) findViewById(R.id.bookmarks);
        this.mBottomBarTabCount = (TextView) findViewById(R.id.tabcount);
        this.mBottomBarBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ((Controller) BottomBar.this.mUiController).onBackKey();
            }
        });
        this.mBottomBarBack.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.back), 0).show();
                return false;
            }
        });
        this.mBottomBarForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (BottomBar.this.mUiController == null || BottomBar.this.mUiController.getCurrentTab() == null) {
                    return;
                }
                BottomBar.this.mUiController.getCurrentTab().goForward();
            }
        });
        this.mBottomBarForward.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.forward), 0).show();
                return false;
            }
        });
        this.mBottomBarTabs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ((PhoneUi) BottomBar.this.mBaseUi).toggleNavScreen();
            }
        });
        this.mBottomBarTabs.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.tabs), 0).show();
                return false;
            }
        });
        this.mBottomBarBookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                BottomBar.this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
            }
        });
        this.mBottomBarBookmarks.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Toast.makeText(BottomBar.this.mUiController.getActivity(), BottomBar.this.mUiController.getActivity().getResources().getString(R.string.bookmarks), 0).show();
                return false;
            }
        });
        this.mBottomBarTabCount.setText(Integer.toString(this.mUiController.getTabControl().getTabCount()));
        this.mTabControl.setOnTabCountChangedListener(new TabControl.OnTabCountChangedListener() {
            @Override
            public void onTabCountChanged() {
                BottomBar.this.mBottomBarTabCount.setText(Integer.toString(BottomBar.this.mTabControl.getTabCount()));
            }
        });
    }

    private void setupBottomBar() {
        ViewGroup parent = (ViewGroup) getParent();
        show();
        if (parent != null) {
            parent.removeView(this);
        }
        this.mContentView.addView(this, makeLayoutParams());
        this.mBaseUi.setContentViewMarginBottom(0);
    }

    public void setFullScreen(boolean use) {
        this.mUseFullScreen = use;
        if (use) {
            setVisibility(8);
        } else {
            setVisibility(0);
        }
    }

    public void setUseQuickControls(boolean use) {
        this.mUseQuickControls = use;
        if (use) {
            setVisibility(8);
        } else {
            setVisibility(0);
        }
    }

    void setupBottomBarAnimator(Animator animator) {
        Resources res = this.mContext.getResources();
        int duration = res.getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(2.5f));
        animator.setDuration(duration);
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
            int visibleHeight = getVisibleBottomHeight();
            float startPos = getTranslationY();
            setLayerType(2, null);
            this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos, startPos - visibleHeight);
            setupBottomBarAnimator(this.mBottomBarAnimator);
            this.mBottomBarAnimator.start();
            this.mShowing = true;
        }
    }

    void hide() {
        if (this.mUseQuickControls || this.mUseFullScreen) {
            cancelBottomBarAnimation();
            int visibleHeight = getVisibleBottomHeight();
            float startPos = getTranslationY();
            setLayerType(2, null);
            this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos, visibleHeight + startPos);
            this.mBottomBarAnimator.addListener(this.mHideBottomBarAnimatorListener);
            setupBottomBarAnimator(this.mBottomBarAnimator);
            this.mBottomBarAnimator.start();
            setVisibility(8);
            this.mShowing = false;
            return;
        }
        setVisibility(0);
        cancelBottomBarAnimation();
        int visibleHeight2 = getVisibleBottomHeight();
        float startPos2 = getTranslationY();
        setLayerType(2, null);
        this.mBottomBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos2, visibleHeight2 + startPos2);
        this.mBottomBarAnimator.addListener(this.mHideBottomBarAnimatorListener);
        setupBottomBarAnimator(this.mBottomBarAnimator);
        this.mBottomBarAnimator.start();
        this.mShowing = false;
    }

    boolean isShowing() {
        return this.mShowing;
    }

    void cancelBottomBarAnimation() {
        if (this.mBottomBarAnimator == null) {
            return;
        }
        this.mBottomBarAnimator.cancel();
        this.mBottomBarAnimator = null;
    }

    private int getVisibleBottomHeight() {
        return this.mBottomBar.getHeight();
    }

    private ViewGroup.MarginLayoutParams makeLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2);
        params.gravity = 80;
        return params;
    }

    public void onScrollChanged() {
        if (this.mShowing) {
            return;
        }
        setTranslationY(getVisibleBottomHeight());
    }

    public void changeBottomBarState(boolean back, boolean forward) {
        this.mBottomBarBack.setEnabled(back);
        this.mBottomBarForward.setEnabled(forward);
    }
}
