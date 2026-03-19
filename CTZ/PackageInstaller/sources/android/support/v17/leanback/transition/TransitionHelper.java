package android.support.v17.leanback.transition;

import android.app.Fragment;
import android.graphics.Rect;
import android.os.Build;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

public final class TransitionHelper {

    private static class TransitionStub {
        ArrayList<TransitionListener> mTransitionListeners;

        TransitionStub() {
        }
    }

    public static Object createChangeBounds(boolean reparent) {
        if (Build.VERSION.SDK_INT >= 19) {
            CustomChangeBounds changeBounds = new CustomChangeBounds();
            changeBounds.setReparent(reparent);
            return changeBounds;
        }
        return new TransitionStub();
    }

    public static Object createChangeTransform() {
        if (Build.VERSION.SDK_INT >= 21) {
            return new ChangeTransform();
        }
        return new TransitionStub();
    }

    public static Object createTransitionSet(boolean z) {
        if (Build.VERSION.SDK_INT >= 19) {
            TransitionSet transitionSet = new TransitionSet();
            transitionSet.setOrdering(z ? 1 : 0);
            return transitionSet;
        }
        return new TransitionStub();
    }

    public static void addTransition(Object transitionSet, Object transition) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((TransitionSet) transitionSet).addTransition((Transition) transition);
        }
    }

    public static void exclude(Object transition, int targetId, boolean exclude) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).excludeTarget(targetId, exclude);
        }
    }

    public static void exclude(Object transition, View targetView, boolean exclude) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).excludeTarget(targetView, exclude);
        }
    }

    public static void include(Object transition, int targetId) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).addTarget(targetId);
        }
    }

    public static void include(Object transition, View targetView) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).addTarget(targetView);
        }
    }

    public static void setStartDelay(Object transition, long startDelay) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).setStartDelay(startDelay);
        }
    }

    public static Object createFadeTransition(int fadeMode) {
        if (Build.VERSION.SDK_INT >= 19) {
            return new Fade(fadeMode);
        }
        return new TransitionStub();
    }

    public static void addTransitionListener(Object transition, final TransitionListener listener) {
        if (listener == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            Transition t = (Transition) transition;
            listener.mImpl = new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition11) {
                    listener.onTransitionStart(transition11);
                }

                @Override
                public void onTransitionResume(Transition transition11) {
                    listener.onTransitionResume(transition11);
                }

                @Override
                public void onTransitionPause(Transition transition11) {
                    listener.onTransitionPause(transition11);
                }

                @Override
                public void onTransitionEnd(Transition transition11) {
                    listener.onTransitionEnd(transition11);
                }

                @Override
                public void onTransitionCancel(Transition transition11) {
                    listener.onTransitionCancel(transition11);
                }
            };
            t.addListener((Transition.TransitionListener) listener.mImpl);
        } else {
            TransitionStub stub = (TransitionStub) transition;
            if (stub.mTransitionListeners == null) {
                stub.mTransitionListeners = new ArrayList<>();
            }
            stub.mTransitionListeners.add(listener);
        }
    }

    public static void setEnterTransition(Fragment fragment, Object transition) {
        if (Build.VERSION.SDK_INT >= 21) {
            fragment.setEnterTransition((Transition) transition);
        }
    }

    public static void setExitTransition(Fragment fragment, Object transition) {
        if (Build.VERSION.SDK_INT >= 21) {
            fragment.setExitTransition((Transition) transition);
        }
    }

    public static void setSharedElementEnterTransition(Fragment fragment, Object transition) {
        if (Build.VERSION.SDK_INT >= 21) {
            fragment.setSharedElementEnterTransition((Transition) transition);
        }
    }

    public static Object createFadeAndShortSlide(int edge) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new FadeAndShortSlide(edge);
        }
        return new TransitionStub();
    }

    public static Object createFadeAndShortSlide(int edge, float distance) {
        if (Build.VERSION.SDK_INT >= 21) {
            FadeAndShortSlide slide = new FadeAndShortSlide(edge);
            slide.setDistance(distance);
            return slide;
        }
        return new TransitionStub();
    }

    public static void beginDelayedTransition(ViewGroup sceneRoot, Object transitionObject) {
        if (Build.VERSION.SDK_INT >= 21) {
            Transition transition = (Transition) transitionObject;
            TransitionManager.beginDelayedTransition(sceneRoot, transition);
        }
    }

    public static void setEpicenterCallback(Object transition, final TransitionEpicenterCallback callback) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (callback == null) {
                ((Transition) transition).setEpicenterCallback(null);
            } else {
                ((Transition) transition).setEpicenterCallback(new Transition.EpicenterCallback() {
                    @Override
                    public Rect onGetEpicenter(Transition transition11) {
                        return callback.onGetEpicenter(transition11);
                    }
                });
            }
        }
    }
}
