package com.android.internal.widget;

import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class ViewClippingUtil {
    private static final int CLIP_CHILDREN_TAG = 16908799;
    private static final int CLIP_CLIPPING_SET = 16908798;
    private static final int CLIP_TO_PADDING = 16908801;

    public static void setClippingDeactivated(View view, boolean z, ClippingParameters clippingParameters) {
        if ((!z && !clippingParameters.isClippingEnablingAllowed(view)) || !(view.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        while (true) {
            if (!z && !clippingParameters.isClippingEnablingAllowed(view)) {
                return;
            }
            ArraySet arraySet = (ArraySet) viewGroup.getTag(16908798);
            if (arraySet == null) {
                arraySet = new ArraySet();
                viewGroup.setTagInternal(16908798, arraySet);
            }
            Boolean boolValueOf = (Boolean) viewGroup.getTag(16908799);
            if (boolValueOf == null) {
                boolValueOf = Boolean.valueOf(viewGroup.getClipChildren());
                viewGroup.setTagInternal(16908799, boolValueOf);
            }
            Boolean boolValueOf2 = (Boolean) viewGroup.getTag(16908801);
            if (boolValueOf2 == null) {
                boolValueOf2 = Boolean.valueOf(viewGroup.getClipToPadding());
                viewGroup.setTagInternal(16908801, boolValueOf2);
            }
            if (!z) {
                arraySet.remove(view);
                if (arraySet.isEmpty()) {
                    viewGroup.setClipChildren(boolValueOf.booleanValue());
                    viewGroup.setClipToPadding(boolValueOf2.booleanValue());
                    viewGroup.setTagInternal(16908798, null);
                    clippingParameters.onClippingStateChanged(viewGroup, true);
                }
            } else {
                arraySet.add(view);
                viewGroup.setClipChildren(false);
                viewGroup.setClipToPadding(false);
                clippingParameters.onClippingStateChanged(viewGroup, false);
            }
            if (clippingParameters.shouldFinish(viewGroup)) {
                return;
            }
            ViewParent parent = viewGroup.getParent();
            if (parent instanceof ViewGroup) {
                viewGroup = (ViewGroup) parent;
            } else {
                return;
            }
        }
    }

    public interface ClippingParameters {
        boolean shouldFinish(View view);

        default boolean isClippingEnablingAllowed(View view) {
            return !MessagingPropertyAnimator.isAnimatingTranslation(view);
        }

        default void onClippingStateChanged(View view, boolean z) {
        }
    }
}
