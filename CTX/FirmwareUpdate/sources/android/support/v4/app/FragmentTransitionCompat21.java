package android.support.v4.app;

import android.graphics.Rect;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

class FragmentTransitionCompat21 extends FragmentTransitionImpl {
    FragmentTransitionCompat21() {
    }

    private static boolean hasSimpleTarget(Transition transition) {
        return (isNullOrEmpty(transition.getTargetIds()) && isNullOrEmpty(transition.getTargetNames()) && isNullOrEmpty(transition.getTargetTypes())) ? false : true;
    }

    @Override
    public void addTarget(Object obj, View view) {
        if (obj != null) {
            ((Transition) obj).addTarget(view);
        }
    }

    @Override
    public void addTargets(Object obj, ArrayList<View> arrayList) {
        Transition transition = (Transition) obj;
        if (transition == null) {
            return;
        }
        if (transition instanceof TransitionSet) {
            TransitionSet transitionSet = (TransitionSet) transition;
            int transitionCount = transitionSet.getTransitionCount();
            for (int i = 0; i < transitionCount; i++) {
                addTargets(transitionSet.getTransitionAt(i), arrayList);
            }
            return;
        }
        if (hasSimpleTarget(transition) || !isNullOrEmpty(transition.getTargets())) {
            return;
        }
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            transition.addTarget(arrayList.get(i2));
        }
    }

    @Override
    public void beginDelayedTransition(ViewGroup viewGroup, Object obj) {
        TransitionManager.beginDelayedTransition(viewGroup, (Transition) obj);
    }

    @Override
    public boolean canHandle(Object obj) {
        return obj instanceof Transition;
    }

    @Override
    public Object cloneTransition(Object obj) {
        if (obj != null) {
            return ((Transition) obj).clone();
        }
        return null;
    }

    @Override
    public Object mergeTransitionsInSequence(Object obj, Object obj2, Object obj3) {
        Transition ordering = null;
        Transition transition = (Transition) obj;
        Transition transition2 = (Transition) obj2;
        Transition transition3 = (Transition) obj3;
        if (transition != null && transition2 != null) {
            ordering = new TransitionSet().addTransition(transition).addTransition(transition2).setOrdering(1);
        } else if (transition != null) {
            ordering = transition;
        } else if (transition2 != null) {
            ordering = transition2;
        }
        if (transition3 == null) {
            return ordering;
        }
        TransitionSet transitionSet = new TransitionSet();
        if (ordering != null) {
            transitionSet.addTransition(ordering);
        }
        transitionSet.addTransition(transition3);
        return transitionSet;
    }

    @Override
    public Object mergeTransitionsTogether(Object obj, Object obj2, Object obj3) {
        TransitionSet transitionSet = new TransitionSet();
        if (obj != null) {
            transitionSet.addTransition((Transition) obj);
        }
        if (obj2 != null) {
            transitionSet.addTransition((Transition) obj2);
        }
        if (obj3 != null) {
            transitionSet.addTransition((Transition) obj3);
        }
        return transitionSet;
    }

    @Override
    public void removeTarget(Object obj, View view) {
        if (obj != null) {
            ((Transition) obj).removeTarget(view);
        }
    }

    @Override
    public void replaceTargets(Object obj, ArrayList<View> arrayList, ArrayList<View> arrayList2) {
        List<View> targets;
        int size;
        int i;
        Transition transition = (Transition) obj;
        if (transition instanceof TransitionSet) {
            TransitionSet transitionSet = (TransitionSet) transition;
            int transitionCount = transitionSet.getTransitionCount();
            for (int i2 = 0; i2 < transitionCount; i2++) {
                replaceTargets(transitionSet.getTransitionAt(i2), arrayList, arrayList2);
            }
            return;
        }
        if (hasSimpleTarget(transition) || (targets = transition.getTargets()) == null || targets.size() != arrayList.size() || !targets.containsAll(arrayList)) {
            return;
        }
        if (arrayList2 == null) {
            size = 0;
            i = 0;
        } else {
            size = arrayList2.size();
            i = 0;
        }
        while (i < size) {
            transition.addTarget(arrayList2.get(i));
            i++;
        }
        for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
            transition.removeTarget(arrayList.get(size2));
        }
    }

    @Override
    public void scheduleHideFragmentView(Object obj, View view, ArrayList<View> arrayList) {
        ((Transition) obj).addListener(new Transition.TransitionListener(this, view, arrayList) {
            final FragmentTransitionCompat21 this$0;
            final ArrayList val$exitingViews;
            final View val$fragmentView;

            {
                this.this$0 = this;
                this.val$fragmentView = view;
                this.val$exitingViews = arrayList;
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                transition.removeListener(this);
                this.val$fragmentView.setVisibility(8);
                int size = this.val$exitingViews.size();
                for (int i = 0; i < size; i++) {
                    ((View) this.val$exitingViews.get(i)).setVisibility(0);
                }
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }

            @Override
            public void onTransitionStart(Transition transition) {
            }
        });
    }

    @Override
    public void scheduleRemoveTargets(Object obj, Object obj2, ArrayList<View> arrayList, Object obj3, ArrayList<View> arrayList2, Object obj4, ArrayList<View> arrayList3) {
        ((Transition) obj).addListener(new Transition.TransitionListener(this, obj2, arrayList, obj3, arrayList2, obj4, arrayList3) {
            final FragmentTransitionCompat21 this$0;
            final Object val$enterTransition;
            final ArrayList val$enteringViews;
            final Object val$exitTransition;
            final ArrayList val$exitingViews;
            final Object val$sharedElementTransition;
            final ArrayList val$sharedElementsIn;

            {
                this.this$0 = this;
                this.val$enterTransition = obj2;
                this.val$enteringViews = arrayList;
                this.val$exitTransition = obj3;
                this.val$exitingViews = arrayList2;
                this.val$sharedElementTransition = obj4;
                this.val$sharedElementsIn = arrayList3;
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }

            @Override
            public void onTransitionStart(Transition transition) {
                if (this.val$enterTransition != null) {
                    this.this$0.replaceTargets(this.val$enterTransition, this.val$enteringViews, null);
                }
                if (this.val$exitTransition != null) {
                    this.this$0.replaceTargets(this.val$exitTransition, this.val$exitingViews, null);
                }
                if (this.val$sharedElementTransition != null) {
                    this.this$0.replaceTargets(this.val$sharedElementTransition, this.val$sharedElementsIn, null);
                }
            }
        });
    }

    @Override
    public void setEpicenter(Object obj, Rect rect) {
        if (obj != null) {
            ((Transition) obj).setEpicenterCallback(new Transition.EpicenterCallback(this, rect) {
                final FragmentTransitionCompat21 this$0;
                final Rect val$epicenter;

                {
                    this.this$0 = this;
                    this.val$epicenter = rect;
                }

                @Override
                public Rect onGetEpicenter(Transition transition) {
                    if (this.val$epicenter == null || this.val$epicenter.isEmpty()) {
                        return null;
                    }
                    return this.val$epicenter;
                }
            });
        }
    }

    @Override
    public void setEpicenter(Object obj, View view) {
        if (view != null) {
            Rect rect = new Rect();
            getBoundsOnScreen(view, rect);
            ((Transition) obj).setEpicenterCallback(new Transition.EpicenterCallback(this, rect) {
                final FragmentTransitionCompat21 this$0;
                final Rect val$epicenter;

                {
                    this.this$0 = this;
                    this.val$epicenter = rect;
                }

                @Override
                public Rect onGetEpicenter(Transition transition) {
                    return this.val$epicenter;
                }
            });
        }
    }

    @Override
    public void setSharedElementTargets(Object obj, View view, ArrayList<View> arrayList) {
        TransitionSet transitionSet = (TransitionSet) obj;
        List<View> targets = transitionSet.getTargets();
        targets.clear();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            bfsAddViewChildren(targets, arrayList.get(i));
        }
        targets.add(view);
        arrayList.add(view);
        addTargets(transitionSet, arrayList);
    }

    @Override
    public void swapSharedElementTargets(Object obj, ArrayList<View> arrayList, ArrayList<View> arrayList2) {
        TransitionSet transitionSet = (TransitionSet) obj;
        if (transitionSet != null) {
            transitionSet.getTargets().clear();
            transitionSet.getTargets().addAll(arrayList2);
            replaceTargets(transitionSet, arrayList, arrayList2);
        }
    }

    @Override
    public Object wrapTransitionInSet(Object obj) {
        if (obj == null) {
            return null;
        }
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition((Transition) obj);
        return transitionSet;
    }
}
