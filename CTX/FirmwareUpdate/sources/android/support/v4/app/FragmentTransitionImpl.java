package android.support.v4.app;

import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class FragmentTransitionImpl {
    protected static void bfsAddViewChildren(List<View> list, View view) {
        int size = list.size();
        if (containedBeforeIndex(list, view, size)) {
            return;
        }
        list.add(view);
        for (int i = size; i < list.size(); i++) {
            View view2 = list.get(i);
            if (view2 instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view2;
                int childCount = viewGroup.getChildCount();
                for (int i2 = 0; i2 < childCount; i2++) {
                    View childAt = viewGroup.getChildAt(i2);
                    if (!containedBeforeIndex(list, childAt, size)) {
                        list.add(childAt);
                    }
                }
            }
        }
    }

    private static boolean containedBeforeIndex(List<View> list, View view, int i) {
        for (int i2 = 0; i2 < i; i2++) {
            if (list.get(i2) == view) {
                return true;
            }
        }
        return false;
    }

    static String findKeyForValue(Map<String, String> map, String str) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (str.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    protected static boolean isNullOrEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public abstract void addTarget(Object obj, View view);

    public abstract void addTargets(Object obj, ArrayList<View> arrayList);

    public abstract void beginDelayedTransition(ViewGroup viewGroup, Object obj);

    public abstract boolean canHandle(Object obj);

    void captureTransitioningViews(ArrayList<View> arrayList, View view) {
        if (view.getVisibility() == 0) {
            if (!(view instanceof ViewGroup)) {
                arrayList.add(view);
                return;
            }
            ViewGroup viewGroup = (ViewGroup) view;
            if (ViewGroupCompat.isTransitionGroup(viewGroup)) {
                arrayList.add(viewGroup);
                return;
            }
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                captureTransitioningViews(arrayList, viewGroup.getChildAt(i));
            }
        }
    }

    public abstract Object cloneTransition(Object obj);

    void findNamedViews(Map<String, View> map, View view) {
        if (view.getVisibility() == 0) {
            String transitionName = ViewCompat.getTransitionName(view);
            if (transitionName != null) {
                map.put(transitionName, view);
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    findNamedViews(map, viewGroup.getChildAt(i));
                }
            }
        }
    }

    protected void getBoundsOnScreen(View view, Rect rect) {
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        rect.set(iArr[0], iArr[1], iArr[0] + view.getWidth(), iArr[1] + view.getHeight());
    }

    public abstract Object mergeTransitionsInSequence(Object obj, Object obj2, Object obj3);

    public abstract Object mergeTransitionsTogether(Object obj, Object obj2, Object obj3);

    ArrayList<String> prepareSetNameOverridesReordered(ArrayList<View> arrayList) {
        ArrayList<String> arrayList2 = new ArrayList<>();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            View view = arrayList.get(i);
            arrayList2.add(ViewCompat.getTransitionName(view));
            ViewCompat.setTransitionName(view, null);
        }
        return arrayList2;
    }

    public abstract void removeTarget(Object obj, View view);

    public abstract void replaceTargets(Object obj, ArrayList<View> arrayList, ArrayList<View> arrayList2);

    public abstract void scheduleHideFragmentView(Object obj, View view, ArrayList<View> arrayList);

    void scheduleNameReset(ViewGroup viewGroup, ArrayList<View> arrayList, Map<String, String> map) {
        OneShotPreDrawListener.add(viewGroup, new Runnable(this, arrayList, map) {
            final FragmentTransitionImpl this$0;
            final Map val$nameOverrides;
            final ArrayList val$sharedElementsIn;

            {
                this.this$0 = this;
                this.val$sharedElementsIn = arrayList;
                this.val$nameOverrides = map;
            }

            @Override
            public void run() {
                int size = this.val$sharedElementsIn.size();
                for (int i = 0; i < size; i++) {
                    View view = (View) this.val$sharedElementsIn.get(i);
                    ViewCompat.setTransitionName(view, (String) this.val$nameOverrides.get(ViewCompat.getTransitionName(view)));
                }
            }
        });
    }

    public abstract void scheduleRemoveTargets(Object obj, Object obj2, ArrayList<View> arrayList, Object obj3, ArrayList<View> arrayList2, Object obj4, ArrayList<View> arrayList3);

    public abstract void setEpicenter(Object obj, Rect rect);

    public abstract void setEpicenter(Object obj, View view);

    void setNameOverridesOrdered(View view, ArrayList<View> arrayList, Map<String, String> map) {
        OneShotPreDrawListener.add(view, new Runnable(this, arrayList, map) {
            final FragmentTransitionImpl this$0;
            final Map val$nameOverrides;
            final ArrayList val$sharedElementsIn;

            {
                this.this$0 = this;
                this.val$sharedElementsIn = arrayList;
                this.val$nameOverrides = map;
            }

            @Override
            public void run() {
                int size = this.val$sharedElementsIn.size();
                for (int i = 0; i < size; i++) {
                    View view2 = (View) this.val$sharedElementsIn.get(i);
                    String transitionName = ViewCompat.getTransitionName(view2);
                    if (transitionName != null) {
                        ViewCompat.setTransitionName(view2, FragmentTransitionImpl.findKeyForValue(this.val$nameOverrides, transitionName));
                    }
                }
            }
        });
    }

    void setNameOverridesReordered(View view, ArrayList<View> arrayList, ArrayList<View> arrayList2, ArrayList<String> arrayList3, Map<String, String> map) {
        int size = arrayList2.size();
        ArrayList arrayList4 = new ArrayList();
        for (int i = 0; i < size; i++) {
            View view2 = arrayList.get(i);
            String transitionName = ViewCompat.getTransitionName(view2);
            arrayList4.add(transitionName);
            if (transitionName != null) {
                ViewCompat.setTransitionName(view2, null);
                String str = map.get(transitionName);
                int i2 = 0;
                while (true) {
                    if (i2 >= size) {
                        break;
                    }
                    if (str.equals(arrayList3.get(i2))) {
                        ViewCompat.setTransitionName(arrayList2.get(i2), transitionName);
                        break;
                    }
                    i2++;
                }
            }
        }
        OneShotPreDrawListener.add(view, new Runnable(this, size, arrayList2, arrayList3, arrayList, arrayList4) {
            final FragmentTransitionImpl this$0;
            final ArrayList val$inNames;
            final int val$numSharedElements;
            final ArrayList val$outNames;
            final ArrayList val$sharedElementsIn;
            final ArrayList val$sharedElementsOut;

            {
                this.this$0 = this;
                this.val$numSharedElements = size;
                this.val$sharedElementsIn = arrayList2;
                this.val$inNames = arrayList3;
                this.val$sharedElementsOut = arrayList;
                this.val$outNames = arrayList4;
            }

            @Override
            public void run() {
                int i3 = 0;
                while (true) {
                    int i4 = i3;
                    if (i4 >= this.val$numSharedElements) {
                        return;
                    }
                    ViewCompat.setTransitionName((View) this.val$sharedElementsIn.get(i4), (String) this.val$inNames.get(i4));
                    ViewCompat.setTransitionName((View) this.val$sharedElementsOut.get(i4), (String) this.val$outNames.get(i4));
                    i3 = i4 + 1;
                }
            }
        });
    }

    public abstract void setSharedElementTargets(Object obj, View view, ArrayList<View> arrayList);

    public abstract void swapSharedElementTargets(Object obj, ArrayList<View> arrayList, ArrayList<View> arrayList2);

    public abstract Object wrapTransitionInSet(Object obj);
}
