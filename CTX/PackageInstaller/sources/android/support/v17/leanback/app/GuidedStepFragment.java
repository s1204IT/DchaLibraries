package android.support.v17.leanback.app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionAdapter;
import android.support.v17.leanback.widget.GuidedActionAdapterGroup;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.support.v17.leanback.widget.NonOverlappingLinearLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class GuidedStepFragment extends Fragment implements GuidedActionAdapter.FocusListener {
    private GuidedActionAdapter mAdapter;
    private GuidedActionAdapterGroup mAdapterGroup;
    private GuidedActionAdapter mButtonAdapter;
    private GuidedActionAdapter mSubAdapter;
    private ContextThemeWrapper mThemeWrapper;
    private List<GuidedAction> mActions = new ArrayList();
    private List<GuidedAction> mButtonActions = new ArrayList();
    private int entranceTransitionType = 0;
    private GuidanceStylist mGuidanceStylist = onCreateGuidanceStylist();
    GuidedActionsStylist mActionsStylist = onCreateActionsStylist();
    private GuidedActionsStylist mButtonActionsStylist = onCreateButtonActionsStylist();

    public GuidedStepFragment() {
        onProvideFragmentTransitions();
    }

    public GuidanceStylist onCreateGuidanceStylist() {
        return new GuidanceStylist();
    }

    public GuidedActionsStylist onCreateActionsStylist() {
        return new GuidedActionsStylist();
    }

    public GuidedActionsStylist onCreateButtonActionsStylist() {
        GuidedActionsStylist stylist = new GuidedActionsStylist();
        stylist.setAsButtonActions();
        return stylist;
    }

    public int onProvideTheme() {
        return -1;
    }

    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance("", "", "", null);
    }

    public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
    }

    public void onCreateButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
    }

    public void onGuidedActionClicked(GuidedAction action) {
    }

    public boolean onSubGuidedActionClicked(GuidedAction action) {
        return true;
    }

    public boolean isExpanded() {
        return this.mActionsStylist.isExpanded();
    }

    public void expandAction(GuidedAction action, boolean withTransition) {
        this.mActionsStylist.expandAction(action, withTransition);
    }

    public void collapseSubActions() {
        collapseAction(true);
    }

    public void collapseAction(boolean withTransition) {
        if (this.mActionsStylist != null && this.mActionsStylist.getActionsGridView() != null) {
            this.mActionsStylist.collapseAction(withTransition);
        }
    }

    @Override
    public void onGuidedActionFocused(GuidedAction action) {
    }

    @Deprecated
    public void onGuidedActionEdited(GuidedAction action) {
    }

    public void onGuidedActionEditCanceled(GuidedAction action) {
        onGuidedActionEdited(action);
    }

    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        onGuidedActionEdited(action);
        return -2L;
    }

    public void setButtonActions(List<GuidedAction> actions) {
        this.mButtonActions = actions;
        if (this.mButtonAdapter != null) {
            this.mButtonAdapter.setActions(this.mButtonActions);
        }
    }

    public void setActions(List<GuidedAction> actions) {
        this.mActions = actions;
        if (this.mAdapter != null) {
            this.mAdapter.setActions(this.mActions);
        }
    }

    protected void onProvideFragmentTransitions() {
        if (Build.VERSION.SDK_INT >= 21) {
            int uiStyle = getUiStyle();
            if (uiStyle == 0) {
                Object enterTransition = TransitionHelper.createFadeAndShortSlide(8388613);
                TransitionHelper.exclude(enterTransition, R.id.guidedstep_background, true);
                TransitionHelper.exclude(enterTransition, R.id.guidedactions_sub_list_background, true);
                TransitionHelper.setEnterTransition(this, enterTransition);
                Object fade = TransitionHelper.createFadeTransition(3);
                TransitionHelper.include(fade, R.id.guidedactions_sub_list_background);
                Object changeBounds = TransitionHelper.createChangeBounds(false);
                Object sharedElementTransition = TransitionHelper.createTransitionSet(false);
                TransitionHelper.addTransition(sharedElementTransition, fade);
                TransitionHelper.addTransition(sharedElementTransition, changeBounds);
                TransitionHelper.setSharedElementEnterTransition(this, sharedElementTransition);
            } else if (uiStyle == 1) {
                if (this.entranceTransitionType == 0) {
                    Object fade2 = TransitionHelper.createFadeTransition(3);
                    TransitionHelper.include(fade2, R.id.guidedstep_background);
                    Object slideFromSide = TransitionHelper.createFadeAndShortSlide(8388615);
                    TransitionHelper.include(slideFromSide, R.id.content_fragment);
                    TransitionHelper.include(slideFromSide, R.id.action_fragment_root);
                    Object enterTransition2 = TransitionHelper.createTransitionSet(false);
                    TransitionHelper.addTransition(enterTransition2, fade2);
                    TransitionHelper.addTransition(enterTransition2, slideFromSide);
                    TransitionHelper.setEnterTransition(this, enterTransition2);
                } else {
                    Object slideFromBottom = TransitionHelper.createFadeAndShortSlide(80);
                    TransitionHelper.include(slideFromBottom, R.id.guidedstep_background_view_root);
                    Object enterTransition3 = TransitionHelper.createTransitionSet(false);
                    TransitionHelper.addTransition(enterTransition3, slideFromBottom);
                    TransitionHelper.setEnterTransition(this, enterTransition3);
                }
                TransitionHelper.setSharedElementEnterTransition(this, null);
            } else if (uiStyle == 2) {
                TransitionHelper.setEnterTransition(this, null);
                TransitionHelper.setSharedElementEnterTransition(this, null);
            }
            Object exitTransition = TransitionHelper.createFadeAndShortSlide(8388611);
            TransitionHelper.exclude(exitTransition, R.id.guidedstep_background, true);
            TransitionHelper.exclude(exitTransition, R.id.guidedactions_sub_list_background, true);
            TransitionHelper.setExitTransition(this, exitTransition);
        }
    }

    public View onCreateBackgroundView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lb_guidedstep_background, container, false);
    }

    public int getUiStyle() {
        Bundle b = getArguments();
        if (b == null) {
            return 1;
        }
        return b.getInt("uiStyle", 1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onProvideFragmentTransitions();
        ArrayList<GuidedAction> actions = new ArrayList<>();
        onCreateActions(actions, savedInstanceState);
        if (savedInstanceState != null) {
            onRestoreActions(actions, savedInstanceState);
        }
        setActions(actions);
        ArrayList<GuidedAction> buttonActions = new ArrayList<>();
        onCreateButtonActions(buttonActions, savedInstanceState);
        if (savedInstanceState != null) {
            onRestoreButtonActions(buttonActions, savedInstanceState);
        }
        setButtonActions(buttonActions);
    }

    @Override
    public void onDestroyView() {
        this.mGuidanceStylist.onDestroyView();
        this.mActionsStylist.onDestroyView();
        this.mButtonActionsStylist.onDestroyView();
        this.mAdapter = null;
        this.mSubAdapter = null;
        this.mButtonAdapter = null;
        this.mAdapterGroup = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        resolveTheme();
        LayoutInflater inflater2 = getThemeInflater(inflater);
        GuidedStepRootLayout root = (GuidedStepRootLayout) inflater2.inflate(R.layout.lb_guidedstep_fragment, container, false);
        root.setFocusOutStart(isFocusOutStartAllowed());
        root.setFocusOutEnd(isFocusOutEndAllowed());
        ViewGroup guidanceContainer = (ViewGroup) root.findViewById(R.id.content_fragment);
        ViewGroup actionContainer = (ViewGroup) root.findViewById(R.id.action_fragment);
        ((NonOverlappingLinearLayout) actionContainer).setFocusableViewAvailableFixEnabled(true);
        GuidanceStylist.Guidance guidance = onCreateGuidance(savedInstanceState);
        View guidanceView = this.mGuidanceStylist.onCreateView(inflater2, guidanceContainer, guidance);
        guidanceContainer.addView(guidanceView);
        View actionsView = this.mActionsStylist.onCreateView(inflater2, actionContainer);
        actionContainer.addView(actionsView);
        View buttonActionsView = this.mButtonActionsStylist.onCreateView(inflater2, actionContainer);
        actionContainer.addView(buttonActionsView);
        GuidedActionAdapter.EditListener editListener = new GuidedActionAdapter.EditListener() {
            @Override
            public void onImeOpen() {
                GuidedStepFragment.this.runImeAnimations(true);
            }

            @Override
            public void onImeClose() {
                GuidedStepFragment.this.runImeAnimations(false);
            }

            @Override
            public long onGuidedActionEditedAndProceed(GuidedAction action) {
                return GuidedStepFragment.this.onGuidedActionEditedAndProceed(action);
            }

            @Override
            public void onGuidedActionEditCanceled(GuidedAction action) {
                GuidedStepFragment.this.onGuidedActionEditCanceled(action);
            }
        };
        this.mAdapter = new GuidedActionAdapter(this.mActions, new GuidedActionAdapter.ClickListener() {
            @Override
            public void onGuidedActionClicked(GuidedAction action) {
                GuidedStepFragment.this.onGuidedActionClicked(action);
                if (GuidedStepFragment.this.isExpanded()) {
                    GuidedStepFragment.this.collapseAction(true);
                } else if (action.hasSubActions() || action.hasEditableActivatorView()) {
                    GuidedStepFragment.this.expandAction(action, true);
                }
            }
        }, this, this.mActionsStylist, false);
        this.mButtonAdapter = new GuidedActionAdapter(this.mButtonActions, new GuidedActionAdapter.ClickListener() {
            @Override
            public void onGuidedActionClicked(GuidedAction action) {
                GuidedStepFragment.this.onGuidedActionClicked(action);
            }
        }, this, this.mButtonActionsStylist, false);
        this.mSubAdapter = new GuidedActionAdapter(null, new GuidedActionAdapter.ClickListener() {
            @Override
            public void onGuidedActionClicked(GuidedAction action) {
                if (!GuidedStepFragment.this.mActionsStylist.isInExpandTransition() && GuidedStepFragment.this.onSubGuidedActionClicked(action)) {
                    GuidedStepFragment.this.collapseSubActions();
                }
            }
        }, this, this.mActionsStylist, true);
        this.mAdapterGroup = new GuidedActionAdapterGroup();
        this.mAdapterGroup.addAdpter(this.mAdapter, this.mButtonAdapter);
        this.mAdapterGroup.addAdpter(this.mSubAdapter, null);
        this.mAdapterGroup.setEditListener(editListener);
        this.mActionsStylist.setEditListener(editListener);
        this.mActionsStylist.getActionsGridView().setAdapter(this.mAdapter);
        if (this.mActionsStylist.getSubActionsGridView() != null) {
            this.mActionsStylist.getSubActionsGridView().setAdapter(this.mSubAdapter);
        }
        this.mButtonActionsStylist.getActionsGridView().setAdapter(this.mButtonAdapter);
        if (this.mButtonActions.size() == 0) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) buttonActionsView.getLayoutParams();
            lp.weight = 0.0f;
            buttonActionsView.setLayoutParams(lp);
        } else {
            Context ctx = this.mThemeWrapper != null ? this.mThemeWrapper : FragmentUtil.getContext(this);
            TypedValue typedValue = new TypedValue();
            if (ctx.getTheme().resolveAttribute(R.attr.guidedActionContentWidthWeightTwoPanels, typedValue, true)) {
                View actionsRoot = root.findViewById(R.id.action_fragment_root);
                float weight = typedValue.getFloat();
                LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) actionsRoot.getLayoutParams();
                lp2.weight = weight;
                actionsRoot.setLayoutParams(lp2);
            }
        }
        View backgroundView = onCreateBackgroundView(inflater2, root, savedInstanceState);
        if (backgroundView != null) {
            FrameLayout backgroundViewRoot = (FrameLayout) root.findViewById(R.id.guidedstep_background_view_root);
            backgroundViewRoot.addView(backgroundView, 0);
        }
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().findViewById(R.id.action_fragment).requestFocus();
    }

    final String getAutoRestoreKey(GuidedAction action) {
        return "action_" + action.getId();
    }

    final String getButtonAutoRestoreKey(GuidedAction action) {
        return "buttonaction_" + action.getId();
    }

    static boolean isSaveEnabled(GuidedAction action) {
        return action.isAutoSaveRestoreEnabled() && action.getId() != -1;
    }

    final void onRestoreActions(List<GuidedAction> actions, Bundle savedInstanceState) {
        int size = actions.size();
        for (int i = 0; i < size; i++) {
            GuidedAction action = actions.get(i);
            if (isSaveEnabled(action)) {
                action.onRestoreInstanceState(savedInstanceState, getAutoRestoreKey(action));
            }
        }
    }

    final void onRestoreButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
        int size = actions.size();
        for (int i = 0; i < size; i++) {
            GuidedAction action = actions.get(i);
            if (isSaveEnabled(action)) {
                action.onRestoreInstanceState(savedInstanceState, getButtonAutoRestoreKey(action));
            }
        }
    }

    final void onSaveActions(List<GuidedAction> actions, Bundle outState) {
        int size = actions.size();
        for (int i = 0; i < size; i++) {
            GuidedAction action = actions.get(i);
            if (isSaveEnabled(action)) {
                action.onSaveInstanceState(outState, getAutoRestoreKey(action));
            }
        }
    }

    final void onSaveButtonActions(List<GuidedAction> actions, Bundle outState) {
        int size = actions.size();
        for (int i = 0; i < size; i++) {
            GuidedAction action = actions.get(i);
            if (isSaveEnabled(action)) {
                action.onSaveInstanceState(outState, getButtonAutoRestoreKey(action));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        onSaveActions(this.mActions, outState);
        onSaveButtonActions(this.mButtonActions, outState);
    }

    private static boolean isGuidedStepTheme(Context context) {
        int resId = R.attr.guidedStepThemeFlag;
        TypedValue typedValue = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(resId, typedValue, true);
        return found && typedValue.type == 18 && typedValue.data != 0;
    }

    public boolean isFocusOutStartAllowed() {
        return false;
    }

    public boolean isFocusOutEndAllowed() {
        return false;
    }

    private void resolveTheme() {
        Context context = FragmentUtil.getContext(this);
        int theme = onProvideTheme();
        if (theme != -1 || isGuidedStepTheme(context)) {
            if (theme != -1) {
                this.mThemeWrapper = new ContextThemeWrapper(context, theme);
                return;
            }
            return;
        }
        int resId = R.attr.guidedStepTheme;
        TypedValue typedValue = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(resId, typedValue, true);
        if (found) {
            ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, typedValue.resourceId);
            if (isGuidedStepTheme(themeWrapper)) {
                this.mThemeWrapper = themeWrapper;
            } else {
                found = false;
                this.mThemeWrapper = null;
            }
        }
        if (!found) {
            Log.e("GuidedStepF", "GuidedStepFragment does not have an appropriate theme set.");
        }
    }

    private LayoutInflater getThemeInflater(LayoutInflater inflater) {
        if (this.mThemeWrapper == null) {
            return inflater;
        }
        return inflater.cloneInContext(this.mThemeWrapper);
    }

    void runImeAnimations(boolean entering) {
        ArrayList<Animator> animators = new ArrayList<>();
        if (entering) {
            this.mGuidanceStylist.onImeAppearing(animators);
            this.mActionsStylist.onImeAppearing(animators);
            this.mButtonActionsStylist.onImeAppearing(animators);
        } else {
            this.mGuidanceStylist.onImeDisappearing(animators);
            this.mActionsStylist.onImeDisappearing(animators);
            this.mButtonActionsStylist.onImeDisappearing(animators);
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.start();
    }
}
