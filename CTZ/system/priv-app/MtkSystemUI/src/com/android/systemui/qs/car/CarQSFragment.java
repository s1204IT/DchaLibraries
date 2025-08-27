package com.android.systemui.qs.car;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.qs.QSFooter;
import com.android.systemui.statusbar.car.UserGridRecyclerView;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class CarQSFragment extends Fragment implements QS {
    private AnimatorSet mAnimatorSet;
    private CarQSFooter mFooter;
    private View mFooterExpandIcon;
    private View mFooterUserName;
    private View mHeader;
    private UserGridRecyclerView mUserGridView;
    private UserSwitchCallback mUserSwitchCallback;
    private View mUserSwitcherContainer;

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.car_qs_panel, viewGroup, false);
    }

    @Override // android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mHeader = view.findViewById(R.id.header);
        this.mFooter = (CarQSFooter) view.findViewById(R.id.qs_footer);
        this.mFooterUserName = this.mFooter.findViewById(R.id.user_name);
        this.mFooterExpandIcon = this.mFooter.findViewById(R.id.user_switch_expand_icon);
        this.mUserSwitcherContainer = view.findViewById(R.id.user_switcher_container);
        updateUserSwitcherHeight(0);
        Context context = getContext();
        this.mUserGridView = (UserGridRecyclerView) this.mUserSwitcherContainer.findViewById(R.id.user_grid);
        this.mUserGridView.getRecyclerView().setLayoutManager(new GridLayoutManager(context, context.getResources().getInteger(R.integer.user_fullscreen_switcher_num_col)));
        this.mUserGridView.buildAdapter();
        this.mUserSwitchCallback = new UserSwitchCallback();
        this.mFooter.setUserSwitchCallback(this.mUserSwitchCallback);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void hideImmediately() {
        getView().setVisibility(4);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setQsExpansion(float f, float f2) {
        getView().setVisibility(f2 == 0.0f ? 0 : 4);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public View getHeader() {
        return this.mHeader;
    }

    QSFooter getFooter() {
        return this.mFooter;
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setHeaderListening(boolean z) {
        this.mFooter.setListening(z);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setListening(boolean z) {
        this.mFooter.setListening(z);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public int getQsMinExpansionHeight() {
        return getView().getHeight();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public int getDesiredHeight() {
        return getView().getHeight();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setPanelView(QS.HeightListener heightListener) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setHeightOverride(int i) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setHeaderClickable(boolean z) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public boolean isCustomizing() {
        return false;
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setOverscrolling(boolean z) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setExpanded(boolean z) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public boolean isShowingDetail() {
        return false;
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void closeDetail() {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setKeyguardShowing(boolean z) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void animateHeaderSlidingIn(long j) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void animateHeaderSlidingOut() {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void notifyCustomizeChanged() {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setContainer(ViewGroup viewGroup) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setExpandClickListener(View.OnClickListener onClickListener) {
    }

    public class UserSwitchCallback {
        private boolean mShowing;

        public UserSwitchCallback() {
        }

        public boolean isShowing() {
            return this.mShowing;
        }

        public void show() throws Resources.NotFoundException {
            this.mShowing = true;
            CarQSFragment.this.animateHeightChange(true);
        }

        public void hide() throws Resources.NotFoundException {
            this.mShowing = false;
            CarQSFragment.this.animateHeightChange(false);
        }
    }

    private void updateUserSwitcherHeight(int i) {
        this.mUserSwitcherContainer.getLayoutParams().height = i;
        this.mUserSwitcherContainer.requestLayout();
    }

    private void animateHeightChange(boolean z) throws Resources.NotFoundException {
        if (this.mAnimatorSet != null) {
            this.mAnimatorSet.cancel();
        }
        ArrayList arrayList = new ArrayList();
        ValueAnimator valueAnimator = (ValueAnimator) AnimatorInflater.loadAnimator(getContext(), z ? R.anim.car_user_switcher_open_animation : R.anim.car_user_switcher_close_animation);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.qs.car.-$$Lambda$CarQSFragment$F8Wi9wqWb5vO5dq5W60LmH4nDcg
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                this.f$0.updateUserSwitcherHeight(((Integer) valueAnimator2.getAnimatedValue()).intValue());
            }
        });
        arrayList.add(valueAnimator);
        Animator animatorLoadAnimator = AnimatorInflater.loadAnimator(getContext(), z ? R.anim.car_user_switcher_open_name_animation : R.anim.car_user_switcher_close_name_animation);
        animatorLoadAnimator.setTarget(this.mFooterUserName);
        arrayList.add(animatorLoadAnimator);
        Animator animatorLoadAnimator2 = AnimatorInflater.loadAnimator(getContext(), z ? R.anim.car_user_switcher_open_icon_animation : R.anim.car_user_switcher_close_icon_animation);
        animatorLoadAnimator2.setTarget(this.mFooterExpandIcon);
        arrayList.add(animatorLoadAnimator2);
        this.mAnimatorSet = new AnimatorSet();
        this.mAnimatorSet.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.qs.car.CarQSFragment.1
            AnonymousClass1() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                CarQSFragment.this.mAnimatorSet = null;
            }
        });
        this.mAnimatorSet.playTogether((Animator[]) arrayList.toArray(new Animator[0]));
        setupInitialValues(this.mAnimatorSet);
        this.mAnimatorSet.start();
    }

    /* renamed from: com.android.systemui.qs.car.CarQSFragment$1 */
    class AnonymousClass1 extends AnimatorListenerAdapter {
        AnonymousClass1() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            CarQSFragment.this.mAnimatorSet = null;
        }
    }

    private void setupInitialValues(Animator animator) {
        if (animator instanceof AnimatorSet) {
            Iterator<Animator> it = ((AnimatorSet) animator).getChildAnimations().iterator();
            while (it.hasNext()) {
                setupInitialValues(it.next());
            }
        } else if (animator instanceof ObjectAnimator) {
            ((ObjectAnimator) animator).setCurrentFraction(0.0f);
        }
    }
}
