package com.android.systemui.statusbar.notification;

import android.util.Pools;
import android.view.View;
import com.android.internal.widget.MessagingGroup;
import com.android.internal.widget.MessagingImageMessage;
import com.android.internal.widget.MessagingLayout;
import com.android.internal.widget.MessagingLinearLayout;
import com.android.internal.widget.MessagingMessage;
import com.android.internal.widget.MessagingPropertyAnimator;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.notification.TransformState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessagingLayoutTransformState extends TransformState {
    private static Pools.SimplePool<MessagingLayoutTransformState> sInstancePool = new Pools.SimplePool<>(40);
    private HashMap<MessagingGroup, MessagingGroup> mGroupMap = new HashMap<>();
    private MessagingLinearLayout mMessageContainer;
    private MessagingLayout mMessagingLayout;
    private float mRelativeTranslationOffset;

    public static MessagingLayoutTransformState obtain() {
        MessagingLayoutTransformState messagingLayoutTransformState = (MessagingLayoutTransformState) sInstancePool.acquire();
        if (messagingLayoutTransformState != null) {
            return messagingLayoutTransformState;
        }
        return new MessagingLayoutTransformState();
    }

    @Override
    public void initFrom(View view, TransformState.TransformInfo transformInfo) {
        super.initFrom(view, transformInfo);
        if (this.mTransformedView instanceof MessagingLinearLayout) {
            this.mMessageContainer = this.mTransformedView;
            this.mMessagingLayout = this.mMessageContainer.getMessagingLayout();
            this.mRelativeTranslationOffset = view.getContext().getResources().getDisplayMetrics().density * 8.0f;
        }
    }

    @Override
    public boolean transformViewTo(TransformState transformState, float f) {
        if (transformState instanceof MessagingLayoutTransformState) {
            transformViewInternal((MessagingLayoutTransformState) transformState, f, true);
            return true;
        }
        return super.transformViewTo(transformState, f);
    }

    @Override
    public void transformViewFrom(TransformState transformState, float f) {
        if (transformState instanceof MessagingLayoutTransformState) {
            transformViewInternal((MessagingLayoutTransformState) transformState, f, false);
        } else {
            super.transformViewFrom(transformState, f);
        }
    }

    private void transformViewInternal(MessagingLayoutTransformState messagingLayoutTransformState, float f, boolean z) {
        float fMax;
        ensureVisible();
        ArrayList<MessagingGroup> arrayListFilterHiddenGroups = filterHiddenGroups(this.mMessagingLayout.getMessagingGroups());
        HashMap<MessagingGroup, MessagingGroup> mapFindPairs = findPairs(arrayListFilterHiddenGroups, filterHiddenGroups(messagingLayoutTransformState.mMessagingLayout.getMessagingGroups()));
        MessagingGroup messagingGroup = null;
        float translationY = 0.0f;
        float f2 = 0.0f;
        for (int size = arrayListFilterHiddenGroups.size() - 1; size >= 0; size--) {
            MessagingGroup messagingGroup2 = arrayListFilterHiddenGroups.get(size);
            MessagingGroup messagingGroup3 = mapFindPairs.get(messagingGroup2);
            if (!isGone(messagingGroup2)) {
                if (messagingGroup3 != null) {
                    transformGroups(messagingGroup2, messagingGroup3, f, z);
                    if (messagingGroup == null) {
                        if (z) {
                            float top = messagingGroup2.getTop() - messagingGroup3.getTop();
                            float translationY2 = messagingGroup3.getAvatar().getTranslationY();
                            f2 = translationY2;
                            translationY = translationY2 - top;
                        } else {
                            float top2 = messagingGroup3.getTop() - messagingGroup2.getTop();
                            translationY = messagingGroup2.getAvatar().getTranslationY();
                            f2 = translationY - top2;
                        }
                        messagingGroup = messagingGroup2;
                    }
                } else {
                    if (messagingGroup != null) {
                        adaptGroupAppear(messagingGroup2, f, translationY, z);
                        int top3 = messagingGroup.getTop() - messagingGroup2.getTop();
                        float height = this.mTransformInfo.isAnimating() ? top3 : messagingGroup2.getHeight() * 0.75f;
                        fMax = Math.max(0.0f, Math.min(1.0f, (f2 - (top3 - height)) / height));
                        if (z) {
                            fMax = 1.0f - fMax;
                        }
                    } else {
                        fMax = f;
                    }
                    if (z) {
                        disappear(messagingGroup2, fMax);
                    } else {
                        appear(messagingGroup2, fMax);
                    }
                }
            }
        }
    }

    private void appear(MessagingGroup messagingGroup, float f) {
        MessagingLinearLayout messageContainer = messagingGroup.getMessageContainer();
        for (int i = 0; i < messageContainer.getChildCount(); i++) {
            View childAt = messageContainer.getChildAt(i);
            if (!isGone(childAt)) {
                appear(childAt, f);
                setClippingDeactivated(childAt, true);
            }
        }
        appear(messagingGroup.getAvatar(), f);
        appear(messagingGroup.getSenderView(), f);
        appear((View) messagingGroup.getIsolatedMessage(), f);
        setClippingDeactivated(messagingGroup.getSenderView(), true);
        setClippingDeactivated(messagingGroup.getAvatar(), true);
    }

    private void adaptGroupAppear(MessagingGroup messagingGroup, float f, float f2, boolean z) {
        float f3;
        if (z) {
            f3 = f * this.mRelativeTranslationOffset;
        } else {
            f3 = this.mRelativeTranslationOffset * (1.0f - f);
        }
        if (messagingGroup.getSenderView().getVisibility() != 8) {
            f3 *= 0.5f;
        }
        messagingGroup.getMessageContainer().setTranslationY(f3);
        messagingGroup.setTranslationY(f2 * 0.85f);
    }

    private void disappear(MessagingGroup messagingGroup, float f) {
        MessagingLinearLayout messageContainer = messagingGroup.getMessageContainer();
        for (int i = 0; i < messageContainer.getChildCount(); i++) {
            View childAt = messageContainer.getChildAt(i);
            if (!isGone(childAt)) {
                disappear(childAt, f);
                setClippingDeactivated(childAt, true);
            }
        }
        disappear(messagingGroup.getAvatar(), f);
        disappear(messagingGroup.getSenderView(), f);
        disappear((View) messagingGroup.getIsolatedMessage(), f);
        setClippingDeactivated(messagingGroup.getSenderView(), true);
        setClippingDeactivated(messagingGroup.getAvatar(), true);
    }

    private void appear(View view, float f) {
        if (view == null || view.getVisibility() == 8) {
            return;
        }
        TransformState transformStateCreateFrom = TransformState.createFrom(view, this.mTransformInfo);
        transformStateCreateFrom.appear(f, null);
        transformStateCreateFrom.recycle();
    }

    private void disappear(View view, float f) {
        if (view == null || view.getVisibility() == 8) {
            return;
        }
        TransformState transformStateCreateFrom = TransformState.createFrom(view, this.mTransformInfo);
        transformStateCreateFrom.disappear(f, null);
        transformStateCreateFrom.recycle();
    }

    private ArrayList<MessagingGroup> filterHiddenGroups(ArrayList<MessagingGroup> arrayList) {
        ArrayList<MessagingGroup> arrayList2 = new ArrayList<>(arrayList);
        int i = 0;
        while (i < arrayList2.size()) {
            if (isGone(arrayList2.get(i))) {
                arrayList2.remove(i);
                i--;
            }
            i++;
        }
        return arrayList2;
    }

    private void transformGroups(MessagingGroup messagingGroup, MessagingGroup messagingGroup2, float f, boolean z) {
        float f2;
        ?? r1;
        int i = 0;
        ?? r9 = 1;
        boolean z2 = messagingGroup2.getIsolatedMessage() == null && !this.mTransformInfo.isAnimating();
        boolean z3 = z2;
        transformView(f, z, messagingGroup.getSenderView(), messagingGroup2.getSenderView(), true, z3);
        transformView(f, z, messagingGroup.getAvatar(), messagingGroup2.getAvatar(), true, z3);
        List messages = messagingGroup.getMessages();
        List messages2 = messagingGroup2.getMessages();
        float fMax = f;
        float translationY = 0.0f;
        while (i < messages.size()) {
            View view = ((MessagingMessage) messages.get((messages.size() - r9) - i)).getView();
            if (isGone(view)) {
                f2 = fMax;
                r1 = r9;
            } else {
                int size = (messages2.size() - r9) - i;
                if (size >= 0) {
                    View view2 = ((MessagingMessage) messages2.get(size)).getView();
                    View view3 = isGone(view2) ? null : view2;
                    if (view3 == null) {
                        fMax = Math.max(0.0f, Math.min(1.0f, ((view.getTop() + view.getHeight()) + translationY) / view.getHeight()));
                        f2 = z ? 1.0f - fMax : fMax;
                        View view4 = view3;
                        transformView(f2, z, view, view3, false, z2);
                        if (f2 == 0.0f && messagingGroup2.getIsolatedMessage() == view4) {
                            r1 = 1;
                            messagingGroup.setTransformingImages(true);
                        } else {
                            r1 = 1;
                        }
                        if (view4 == null) {
                            view.setTranslationY(translationY);
                            setClippingDeactivated(view, r1);
                        } else {
                            translationY = z ? view4.getTranslationY() - (((view.getTop() + messagingGroup.getTop()) - view4.getTop()) - view4.getTop()) : view.getTranslationY();
                        }
                    }
                }
            }
            i++;
            r9 = r1;
            fMax = f2;
        }
        messagingGroup.updateClipRect();
    }

    private void transformView(float f, boolean z, View view, View view2, boolean z2, boolean z3) {
        TransformState transformStateCreateFrom = TransformState.createFrom(view, this.mTransformInfo);
        if (z3) {
            transformStateCreateFrom.setDefaultInterpolator(Interpolators.LINEAR);
        }
        transformStateCreateFrom.setIsSameAsAnyView(z2);
        if (z) {
            if (view2 != null) {
                TransformState transformStateCreateFrom2 = TransformState.createFrom(view2, this.mTransformInfo);
                transformStateCreateFrom.transformViewTo(transformStateCreateFrom2, f);
                transformStateCreateFrom2.recycle();
            } else {
                transformStateCreateFrom.disappear(f, null);
            }
        } else if (view2 != null) {
            TransformState transformStateCreateFrom3 = TransformState.createFrom(view2, this.mTransformInfo);
            transformStateCreateFrom.transformViewFrom(transformStateCreateFrom3, f);
            transformStateCreateFrom3.recycle();
        } else {
            transformStateCreateFrom.appear(f, null);
        }
        transformStateCreateFrom.recycle();
    }

    private HashMap<MessagingGroup, MessagingGroup> findPairs(ArrayList<MessagingGroup> arrayList, ArrayList<MessagingGroup> arrayList2) {
        this.mGroupMap.clear();
        int i = Integer.MAX_VALUE;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            MessagingGroup messagingGroup = arrayList.get(size);
            MessagingGroup messagingGroup2 = null;
            int i2 = 0;
            for (int iMin = Math.min(arrayList2.size(), i) - 1; iMin >= 0; iMin--) {
                MessagingGroup messagingGroup3 = arrayList2.get(iMin);
                int iCalculateGroupCompatibility = messagingGroup.calculateGroupCompatibility(messagingGroup3);
                if (iCalculateGroupCompatibility > i2) {
                    i = iMin;
                    messagingGroup2 = messagingGroup3;
                    i2 = iCalculateGroupCompatibility;
                }
            }
            if (messagingGroup2 != null) {
                this.mGroupMap.put(messagingGroup, messagingGroup2);
            }
        }
        return this.mGroupMap;
    }

    private boolean isGone(View view) {
        if (view.getVisibility() == 8) {
            return true;
        }
        MessagingLinearLayout.LayoutParams layoutParams = view.getLayoutParams();
        return (layoutParams instanceof MessagingLinearLayout.LayoutParams) && layoutParams.hide;
    }

    @Override
    public void setVisible(boolean z, boolean z2) {
        super.setVisible(z, z2);
        resetTransformedView();
        ArrayList messagingGroups = this.mMessagingLayout.getMessagingGroups();
        for (int i = 0; i < messagingGroups.size(); i++) {
            MessagingGroup messagingGroup = (MessagingGroup) messagingGroups.get(i);
            if (!isGone(messagingGroup)) {
                MessagingLinearLayout messageContainer = messagingGroup.getMessageContainer();
                for (int i2 = 0; i2 < messageContainer.getChildCount(); i2++) {
                    setVisible(messageContainer.getChildAt(i2), z, z2);
                }
                setVisible(messagingGroup.getAvatar(), z, z2);
                setVisible(messagingGroup.getSenderView(), z, z2);
                MessagingImageMessage isolatedMessage = messagingGroup.getIsolatedMessage();
                if (isolatedMessage != null) {
                    setVisible(isolatedMessage, z, z2);
                }
            }
        }
    }

    private void setVisible(View view, boolean z, boolean z2) {
        if (isGone(view) || MessagingPropertyAnimator.isAnimatingAlpha(view)) {
            return;
        }
        TransformState transformStateCreateFrom = TransformState.createFrom(view, this.mTransformInfo);
        transformStateCreateFrom.setVisible(z, z2);
        transformStateCreateFrom.recycle();
    }

    @Override
    protected void resetTransformedView() {
        super.resetTransformedView();
        ArrayList messagingGroups = this.mMessagingLayout.getMessagingGroups();
        for (int i = 0; i < messagingGroups.size(); i++) {
            MessagingGroup messagingGroup = (MessagingGroup) messagingGroups.get(i);
            if (!isGone(messagingGroup)) {
                MessagingLinearLayout messageContainer = messagingGroup.getMessageContainer();
                for (int i2 = 0; i2 < messageContainer.getChildCount(); i2++) {
                    View childAt = messageContainer.getChildAt(i2);
                    if (!isGone(childAt)) {
                        resetTransformedView(childAt);
                        setClippingDeactivated(childAt, false);
                    }
                }
                resetTransformedView(messagingGroup.getAvatar());
                resetTransformedView(messagingGroup.getSenderView());
                MessagingImageMessage isolatedMessage = messagingGroup.getIsolatedMessage();
                if (isolatedMessage != null) {
                    resetTransformedView(isolatedMessage);
                }
                setClippingDeactivated(messagingGroup.getAvatar(), false);
                setClippingDeactivated(messagingGroup.getSenderView(), false);
                messagingGroup.setTranslationY(0.0f);
                messagingGroup.getMessageContainer().setTranslationY(0.0f);
            }
            messagingGroup.setTransformingImages(false);
            messagingGroup.updateClipRect();
        }
    }

    @Override
    public void prepareFadeIn() {
        super.prepareFadeIn();
        setVisible(true, false);
    }

    private void resetTransformedView(View view) {
        TransformState transformStateCreateFrom = TransformState.createFrom(view, this.mTransformInfo);
        transformStateCreateFrom.resetTransformedView();
        transformStateCreateFrom.recycle();
    }

    @Override
    protected void reset() {
        super.reset();
        this.mMessageContainer = null;
        this.mMessagingLayout = null;
    }

    @Override
    public void recycle() {
        super.recycle();
        this.mGroupMap.clear();
        sInstancePool.release(this);
    }
}
