package android.transition;

import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class TransitionManager {
    private static String LOG_TAG = "TransitionManager";
    private static Transition sDefaultTransition = new AutoTransition();
    private static final String[] EMPTY_STRINGS = new String[0];
    private static ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>> sRunningTransitions = new ThreadLocal<>();
    private static ArrayList<ViewGroup> sPendingTransitions = new ArrayList<>();
    ArrayMap<Scene, Transition> mSceneTransitions = new ArrayMap<>();
    ArrayMap<Scene, ArrayMap<Scene, Transition>> mScenePairTransitions = new ArrayMap<>();

    public void setDefaultTransition(Transition transition) {
        sDefaultTransition = transition;
    }

    public static Transition getDefaultTransition() {
        return sDefaultTransition;
    }

    public void setTransition(Scene scene, Transition transition) {
        this.mSceneTransitions.put(scene, transition);
    }

    public void setTransition(Scene scene, Scene scene2, Transition transition) {
        ArrayMap<Scene, Transition> arrayMap = this.mScenePairTransitions.get(scene2);
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>();
            this.mScenePairTransitions.put(scene2, arrayMap);
        }
        arrayMap.put(scene, transition);
    }

    public Transition getTransition(Scene scene) {
        Scene currentScene;
        ArrayMap<Scene, Transition> arrayMap;
        Transition transition;
        ViewGroup sceneRoot = scene.getSceneRoot();
        if (sceneRoot != null && (currentScene = Scene.getCurrentScene(sceneRoot)) != null && (arrayMap = this.mScenePairTransitions.get(scene)) != null && (transition = arrayMap.get(currentScene)) != null) {
            return transition;
        }
        Transition transition2 = this.mSceneTransitions.get(scene);
        return transition2 != null ? transition2 : sDefaultTransition;
    }

    private static void changeScene(Scene scene, Transition transition) {
        ViewGroup sceneRoot = scene.getSceneRoot();
        if (!sPendingTransitions.contains(sceneRoot)) {
            if (transition == null) {
                scene.enter();
                return;
            }
            sPendingTransitions.add(sceneRoot);
            Transition transitionMo30clone = transition.mo30clone();
            transitionMo30clone.setSceneRoot(sceneRoot);
            Scene currentScene = Scene.getCurrentScene(sceneRoot);
            if (currentScene != null && currentScene.isCreatedFromLayoutResource()) {
                transitionMo30clone.setCanRemoveViews(true);
            }
            sceneChangeSetup(sceneRoot, transitionMo30clone);
            scene.enter();
            sceneChangeRunTransition(sceneRoot, transitionMo30clone);
        }
    }

    private static ArrayMap<ViewGroup, ArrayList<Transition>> getRunningTransitions() {
        WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>> weakReference = sRunningTransitions.get();
        if (weakReference == null || weakReference.get() == null) {
            WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>> weakReference2 = new WeakReference<>(new ArrayMap());
            sRunningTransitions.set(weakReference2);
            weakReference = weakReference2;
        }
        return weakReference.get();
    }

    private static void sceneChangeRunTransition(ViewGroup viewGroup, Transition transition) {
        if (transition != null && viewGroup != null) {
            MultiListener multiListener = new MultiListener(transition, viewGroup);
            viewGroup.addOnAttachStateChangeListener(multiListener);
            viewGroup.getViewTreeObserver().addOnPreDrawListener(multiListener);
        }
    }

    private static class MultiListener implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        ViewGroup mSceneRoot;
        Transition mTransition;
        final ViewTreeObserver mViewTreeObserver;

        MultiListener(Transition transition, ViewGroup viewGroup) {
            this.mTransition = transition;
            this.mSceneRoot = viewGroup;
            this.mViewTreeObserver = this.mSceneRoot.getViewTreeObserver();
        }

        private void removeListeners() {
            if (this.mViewTreeObserver.isAlive()) {
                this.mViewTreeObserver.removeOnPreDrawListener(this);
            } else {
                this.mSceneRoot.getViewTreeObserver().removeOnPreDrawListener(this);
            }
            this.mSceneRoot.removeOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            removeListeners();
            TransitionManager.sPendingTransitions.remove(this.mSceneRoot);
            ArrayList arrayList = (ArrayList) TransitionManager.getRunningTransitions().get(this.mSceneRoot);
            if (arrayList != null && arrayList.size() > 0) {
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    ((Transition) it.next()).resume(this.mSceneRoot);
                }
            }
            this.mTransition.clearValues(true);
        }

        @Override
        public boolean onPreDraw() {
            removeListeners();
            if (!TransitionManager.sPendingTransitions.remove(this.mSceneRoot)) {
                return true;
            }
            final ArrayMap runningTransitions = TransitionManager.getRunningTransitions();
            ArrayList arrayList = (ArrayList) runningTransitions.get(this.mSceneRoot);
            ArrayList arrayList2 = null;
            if (arrayList == null) {
                arrayList = new ArrayList();
                runningTransitions.put(this.mSceneRoot, arrayList);
            } else if (arrayList.size() > 0) {
                arrayList2 = new ArrayList(arrayList);
            }
            arrayList.add(this.mTransition);
            this.mTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    ((ArrayList) runningTransitions.get(MultiListener.this.mSceneRoot)).remove(transition);
                }
            });
            this.mTransition.captureValues(this.mSceneRoot, false);
            if (arrayList2 != null) {
                Iterator it = arrayList2.iterator();
                while (it.hasNext()) {
                    ((Transition) it.next()).resume(this.mSceneRoot);
                }
            }
            this.mTransition.playTransition(this.mSceneRoot);
            return true;
        }
    }

    private static void sceneChangeSetup(ViewGroup viewGroup, Transition transition) {
        ArrayList<Transition> arrayList = getRunningTransitions().get(viewGroup);
        if (arrayList != null && arrayList.size() > 0) {
            Iterator<Transition> it = arrayList.iterator();
            while (it.hasNext()) {
                it.next().pause(viewGroup);
            }
        }
        if (transition != null) {
            transition.captureValues(viewGroup, true);
        }
        Scene currentScene = Scene.getCurrentScene(viewGroup);
        if (currentScene != null) {
            currentScene.exit();
        }
    }

    public void transitionTo(Scene scene) {
        changeScene(scene, getTransition(scene));
    }

    public static void go(Scene scene) {
        changeScene(scene, sDefaultTransition);
    }

    public static void go(Scene scene, Transition transition) {
        changeScene(scene, transition);
    }

    public static void beginDelayedTransition(ViewGroup viewGroup) {
        beginDelayedTransition(viewGroup, null);
    }

    public static void beginDelayedTransition(ViewGroup viewGroup, Transition transition) {
        if (!sPendingTransitions.contains(viewGroup) && viewGroup.isLaidOut()) {
            sPendingTransitions.add(viewGroup);
            if (transition == null) {
                transition = sDefaultTransition;
            }
            Transition transitionMo30clone = transition.mo30clone();
            sceneChangeSetup(viewGroup, transitionMo30clone);
            Scene.setCurrentScene(viewGroup, null);
            sceneChangeRunTransition(viewGroup, transitionMo30clone);
        }
    }

    public static void endTransitions(ViewGroup viewGroup) {
        sPendingTransitions.remove(viewGroup);
        ArrayList<Transition> arrayList = getRunningTransitions().get(viewGroup);
        if (arrayList != null && !arrayList.isEmpty()) {
            ArrayList arrayList2 = new ArrayList(arrayList);
            for (int size = arrayList2.size() - 1; size >= 0; size--) {
                ((Transition) arrayList2.get(size)).forceToEnd(viewGroup);
            }
        }
    }
}
