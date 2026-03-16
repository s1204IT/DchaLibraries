package android.widget;

import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.android.internal.R;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import libcore.util.Objects;

public class RemoteViews implements Parcelable, LayoutInflater.Filter {
    static final String EXTRA_REMOTEADAPTER_APPWIDGET_ID = "remoteAdapterAppWidgetId";
    private static final String LOG_TAG = "RemoteViews";
    private static final int MODE_HAS_LANDSCAPE_AND_PORTRAIT = 1;
    private static final int MODE_NORMAL = 0;
    private ArrayList<Action> mActions;
    private ApplicationInfo mApplication;
    private BitmapCache mBitmapCache;
    private boolean mIsRoot;
    private boolean mIsWidgetCollectionChild;
    private RemoteViews mLandscape;
    private final int mLayoutId;
    private MemoryUsageCounter mMemoryUsageCounter;
    private final MutablePair<String, Class<?>> mPair;
    private RemoteViews mPortrait;
    private static final OnClickHandler DEFAULT_ON_CLICK_HANDLER = new OnClickHandler();
    private static final Object[] sMethodsLock = new Object[0];
    private static final ArrayMap<Class<? extends View>, ArrayMap<MutablePair<String, Class<?>>, Method>> sMethods = new ArrayMap<>();
    private static final ThreadLocal<Object[]> sInvokeArgsTls = new ThreadLocal<Object[]>() {
        @Override
        protected Object[] initialValue() {
            return new Object[1];
        }
    };
    public static final Parcelable.Creator<RemoteViews> CREATOR = new Parcelable.Creator<RemoteViews>() {
        @Override
        public RemoteViews createFromParcel(Parcel parcel) {
            return new RemoteViews(parcel);
        }

        @Override
        public RemoteViews[] newArray(int size) {
            return new RemoteViews[size];
        }
    };

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RemoteView {
    }

    static class MutablePair<F, S> {
        F first;
        S second;

        MutablePair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public boolean equals(Object o) {
            if (!(o instanceof MutablePair)) {
                return false;
            }
            MutablePair<?, ?> p = (MutablePair) o;
            return Objects.equal(p.first, this.first) && Objects.equal(p.second, this.second);
        }

        public int hashCode() {
            return (this.first == null ? 0 : this.first.hashCode()) ^ (this.second != null ? this.second.hashCode() : 0);
        }
    }

    public static class ActionException extends RuntimeException {
        public ActionException(Exception ex) {
            super(ex);
        }

        public ActionException(String message) {
            super(message);
        }
    }

    public static class OnClickHandler {
        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            try {
                Context context = view.getContext();
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                context.startIntentSender(pendingIntent.getIntentSender(), fillInIntent, 268435456, 268435456, 0, opts.toBundle());
                return true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(RemoteViews.LOG_TAG, "Cannot send pending intent: ", e);
                return false;
            } catch (Exception e2) {
                Log.e(RemoteViews.LOG_TAG, "Cannot send pending intent due to unknown exception: ", e2);
                return false;
            }
        }
    }

    private static abstract class Action implements Parcelable {
        public static final int MERGE_APPEND = 1;
        public static final int MERGE_IGNORE = 2;
        public static final int MERGE_REPLACE = 0;
        int viewId;

        public abstract void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) throws ActionException;

        public abstract String getActionName();

        private Action() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public void updateMemoryUsageEstimate(MemoryUsageCounter counter) {
        }

        public void setBitmapCache(BitmapCache bitmapCache) {
        }

        public int mergeBehavior() {
            return 0;
        }

        public String getUniqueKey() {
            return getActionName() + this.viewId;
        }
    }

    public void mergeRemoteViews(RemoteViews newRv) {
        if (newRv != null) {
            RemoteViews copy = newRv.mo11clone();
            HashMap<String, Action> map = new HashMap<>();
            if (this.mActions == null) {
                this.mActions = new ArrayList<>();
            }
            int count = this.mActions.size();
            for (int i = 0; i < count; i++) {
                Action a = this.mActions.get(i);
                map.put(a.getUniqueKey(), a);
            }
            ArrayList<Action> newActions = copy.mActions;
            if (newActions != null) {
                int count2 = newActions.size();
                for (int i2 = 0; i2 < count2; i2++) {
                    Action a2 = newActions.get(i2);
                    String key = newActions.get(i2).getUniqueKey();
                    int mergeBehavior = newActions.get(i2).mergeBehavior();
                    if (map.containsKey(key) && mergeBehavior == 0) {
                        this.mActions.remove(map.get(key));
                        map.remove(key);
                    }
                    if (mergeBehavior == 0 || mergeBehavior == 1) {
                        this.mActions.add(a2);
                    }
                }
                this.mBitmapCache = new BitmapCache();
                setBitmapCache(this.mBitmapCache);
            }
        }
    }

    private class SetEmptyView extends Action {
        public static final int TAG = 6;
        int emptyViewId;
        int viewId;

        SetEmptyView(int viewId, int emptyViewId) {
            super();
            this.viewId = viewId;
            this.emptyViewId = emptyViewId;
        }

        SetEmptyView(Parcel in) {
            super();
            this.viewId = in.readInt();
            this.emptyViewId = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(6);
            out.writeInt(this.viewId);
            out.writeInt(this.emptyViewId);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View view = root.findViewById(this.viewId);
            if (view instanceof AdapterView) {
                AdapterView<?> adapterView = (AdapterView) view;
                View emptyView = root.findViewById(this.emptyViewId);
                if (emptyView != null) {
                    adapterView.setEmptyView(emptyView);
                }
            }
        }

        @Override
        public String getActionName() {
            return "SetEmptyView";
        }
    }

    private class SetOnClickFillInIntent extends Action {
        public static final int TAG = 9;
        Intent fillInIntent;

        public SetOnClickFillInIntent(int id, Intent fillInIntent) {
            super();
            this.viewId = id;
            this.fillInIntent = fillInIntent;
        }

        public SetOnClickFillInIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.fillInIntent = Intent.CREATOR.createFromParcel(parcel);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(9);
            dest.writeInt(this.viewId);
            this.fillInIntent.writeToParcel(dest, 0);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, final OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (!RemoteViews.this.mIsWidgetCollectionChild) {
                    Log.e(RemoteViews.LOG_TAG, "The method setOnClickFillInIntent is available only from RemoteViewsFactory (ie. on collection items).");
                    return;
                }
                if (target == root) {
                    target.setTagInternal(R.id.fillInIntent, this.fillInIntent);
                } else if (this.fillInIntent != null) {
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            View parent = (View) v.getParent();
                            while (parent != null && !(parent instanceof AdapterView) && !(parent instanceof AppWidgetHostView)) {
                                parent = (View) parent.getParent();
                            }
                            if ((parent instanceof AppWidgetHostView) || parent == null) {
                                Log.e(RemoteViews.LOG_TAG, "Collection item doesn't have AdapterView parent");
                                return;
                            }
                            if (!(parent.getTag() instanceof PendingIntent)) {
                                Log.e(RemoteViews.LOG_TAG, "Attempting setOnClickFillInIntent without calling setPendingIntentTemplate on parent.");
                                return;
                            }
                            PendingIntent pendingIntent = (PendingIntent) parent.getTag();
                            Rect rect = RemoteViews.getSourceBounds(v);
                            SetOnClickFillInIntent.this.fillInIntent.setSourceBounds(rect);
                            handler.onClickHandler(v, pendingIntent, SetOnClickFillInIntent.this.fillInIntent);
                        }
                    };
                    target.setOnClickListener(listener);
                }
            }
        }

        @Override
        public String getActionName() {
            return "SetOnClickFillInIntent";
        }
    }

    private class SetPendingIntentTemplate extends Action {
        public static final int TAG = 8;
        PendingIntent pendingIntentTemplate;

        public SetPendingIntentTemplate(int id, PendingIntent pendingIntentTemplate) {
            super();
            this.viewId = id;
            this.pendingIntentTemplate = pendingIntentTemplate;
        }

        public SetPendingIntentTemplate(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.pendingIntentTemplate = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(8);
            dest.writeInt(this.viewId);
            this.pendingIntentTemplate.writeToParcel(dest, 0);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, final OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (target instanceof AdapterView) {
                    AdapterView<?> av = (AdapterView) target;
                    AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (view instanceof ViewGroup) {
                                ViewGroup vg = (ViewGroup) view;
                                if (parent instanceof AdapterViewAnimator) {
                                    vg = (ViewGroup) vg.getChildAt(0);
                                }
                                if (vg != null) {
                                    Intent fillInIntent = null;
                                    int childCount = vg.getChildCount();
                                    int i = 0;
                                    while (true) {
                                        if (i >= childCount) {
                                            break;
                                        }
                                        Object tag = vg.getChildAt(i).getTag(R.id.fillInIntent);
                                        if (!(tag instanceof Intent)) {
                                            i++;
                                        } else {
                                            fillInIntent = (Intent) tag;
                                            break;
                                        }
                                    }
                                    if (fillInIntent != null) {
                                        Rect rect = RemoteViews.getSourceBounds(view);
                                        Intent intent = new Intent();
                                        intent.setSourceBounds(rect);
                                        handler.onClickHandler(view, SetPendingIntentTemplate.this.pendingIntentTemplate, fillInIntent);
                                    }
                                }
                            }
                        }
                    };
                    av.setOnItemClickListener(listener);
                    av.setTag(this.pendingIntentTemplate);
                    return;
                }
                Log.e(RemoteViews.LOG_TAG, "Cannot setPendingIntentTemplate on a view which is notan AdapterView (id: " + this.viewId + ")");
            }
        }

        @Override
        public String getActionName() {
            return "SetPendingIntentTemplate";
        }
    }

    private class SetRemoteViewsAdapterList extends Action {
        public static final int TAG = 15;
        ArrayList<RemoteViews> list;
        int viewTypeCount;

        public SetRemoteViewsAdapterList(int id, ArrayList<RemoteViews> list, int viewTypeCount) {
            super();
            this.viewId = id;
            this.list = list;
            this.viewTypeCount = viewTypeCount;
        }

        public SetRemoteViewsAdapterList(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.viewTypeCount = parcel.readInt();
            int count = parcel.readInt();
            this.list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                RemoteViews rv = RemoteViews.CREATOR.createFromParcel(parcel);
                this.list.add(rv);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(15);
            dest.writeInt(this.viewId);
            dest.writeInt(this.viewTypeCount);
            if (this.list == null || this.list.size() == 0) {
                dest.writeInt(0);
                return;
            }
            int count = this.list.size();
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                RemoteViews rv = this.list.get(i);
                rv.writeToParcel(dest, flags);
            }
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (!(rootParent instanceof AppWidgetHostView)) {
                    Log.e(RemoteViews.LOG_TAG, "SetRemoteViewsAdapterIntent action can only be used for AppWidgets (root id: " + this.viewId + ")");
                    return;
                }
                if (!(target instanceof AbsListView) && !(target instanceof AdapterViewAnimator)) {
                    Log.e(RemoteViews.LOG_TAG, "Cannot setRemoteViewsAdapter on a view which is not an AbsListView or AdapterViewAnimator (id: " + this.viewId + ")");
                    return;
                }
                if (target instanceof AbsListView) {
                    AbsListView v = (AbsListView) target;
                    Adapter a = v.getAdapter();
                    if ((a instanceof RemoteViewsListAdapter) && this.viewTypeCount <= a.getViewTypeCount()) {
                        ((RemoteViewsListAdapter) a).setViewsList(this.list);
                        return;
                    } else {
                        v.setAdapter((ListAdapter) new RemoteViewsListAdapter(v.getContext(), this.list, this.viewTypeCount));
                        return;
                    }
                }
                if (target instanceof AdapterViewAnimator) {
                    AdapterViewAnimator v2 = (AdapterViewAnimator) target;
                    Adapter a2 = v2.getAdapter();
                    if ((a2 instanceof RemoteViewsListAdapter) && this.viewTypeCount <= a2.getViewTypeCount()) {
                        ((RemoteViewsListAdapter) a2).setViewsList(this.list);
                    } else {
                        v2.setAdapter(new RemoteViewsListAdapter(v2.getContext(), this.list, this.viewTypeCount));
                    }
                }
            }
        }

        @Override
        public String getActionName() {
            return "SetRemoteViewsAdapterList";
        }
    }

    private class SetRemoteViewsAdapterIntent extends Action {
        public static final int TAG = 10;
        Intent intent;

        public SetRemoteViewsAdapterIntent(int id, Intent intent) {
            super();
            this.viewId = id;
            this.intent = intent;
        }

        public SetRemoteViewsAdapterIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.intent = Intent.CREATOR.createFromParcel(parcel);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(10);
            dest.writeInt(this.viewId);
            this.intent.writeToParcel(dest, flags);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (!(rootParent instanceof AppWidgetHostView)) {
                    Log.e(RemoteViews.LOG_TAG, "SetRemoteViewsAdapterIntent action can only be used for AppWidgets (root id: " + this.viewId + ")");
                    return;
                }
                if (!(target instanceof AbsListView) && !(target instanceof AdapterViewAnimator)) {
                    Log.e(RemoteViews.LOG_TAG, "Cannot setRemoteViewsAdapter on a view which is not an AbsListView or AdapterViewAnimator (id: " + this.viewId + ")");
                    return;
                }
                AppWidgetHostView host = (AppWidgetHostView) rootParent;
                this.intent.putExtra(RemoteViews.EXTRA_REMOTEADAPTER_APPWIDGET_ID, host.getAppWidgetId());
                if (target instanceof AbsListView) {
                    AbsListView v = (AbsListView) target;
                    v.setRemoteViewsAdapter(this.intent);
                    v.setRemoteViewsOnClickHandler(handler);
                } else if (target instanceof AdapterViewAnimator) {
                    AdapterViewAnimator v2 = (AdapterViewAnimator) target;
                    v2.setRemoteViewsAdapter(this.intent);
                    v2.setRemoteViewsOnClickHandler(handler);
                }
            }
        }

        @Override
        public String getActionName() {
            return "SetRemoteViewsAdapterIntent";
        }
    }

    private class SetOnClickPendingIntent extends Action {
        public static final int TAG = 1;
        PendingIntent pendingIntent;

        public SetOnClickPendingIntent(int id, PendingIntent pendingIntent) {
            super();
            this.viewId = id;
            this.pendingIntent = pendingIntent;
        }

        public SetOnClickPendingIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            if (parcel.readInt() != 0) {
                this.pendingIntent = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(1);
            dest.writeInt(this.viewId);
            dest.writeInt(this.pendingIntent == null ? 0 : 1);
            if (this.pendingIntent != null) {
                this.pendingIntent.writeToParcel(dest, 0);
            }
        }

        @Override
        public void apply(View root, ViewGroup rootParent, final OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (RemoteViews.this.mIsWidgetCollectionChild) {
                    Log.w(RemoteViews.LOG_TAG, "Cannot setOnClickPendingIntent for collection item (id: " + this.viewId + ")");
                    ApplicationInfo appInfo = root.getContext().getApplicationInfo();
                    if (appInfo != null && appInfo.targetSdkVersion >= 16) {
                        return;
                    }
                }
                View.OnClickListener listener = null;
                if (this.pendingIntent != null) {
                    listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Rect rect = RemoteViews.getSourceBounds(v);
                            Intent intent = new Intent();
                            intent.setSourceBounds(rect);
                            handler.onClickHandler(v, SetOnClickPendingIntent.this.pendingIntent, intent);
                        }
                    };
                }
                target.setOnClickListener(listener);
            }
        }

        @Override
        public String getActionName() {
            return "SetOnClickPendingIntent";
        }
    }

    private static Rect getSourceBounds(View v) {
        float appScale = v.getContext().getResources().getCompatibilityInfo().applicationScale;
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        Rect rect = new Rect();
        rect.left = (int) ((pos[0] * appScale) + 0.5f);
        rect.top = (int) ((pos[1] * appScale) + 0.5f);
        rect.right = (int) (((pos[0] + v.getWidth()) * appScale) + 0.5f);
        rect.bottom = (int) (((pos[1] + v.getHeight()) * appScale) + 0.5f);
        return rect;
    }

    private Method getMethod(View view, String str, Class<?> cls) {
        Method method;
        Class<?> cls2 = view.getClass();
        synchronized (sMethodsLock) {
            ArrayMap<MutablePair<String, Class<?>>, Method> methods = sMethods.get(cls2);
            if (methods == null) {
                methods = new ArrayMap<>();
                sMethods.put(cls2, methods);
            }
            this.mPair.first = str;
            this.mPair.second = cls;
            method = methods.get(this.mPair);
            if (method == null) {
                try {
                    if (cls == 0) {
                        method = cls2.getMethod(str, new Class[0]);
                    } else {
                        method = cls2.getMethod(str, cls);
                    }
                    if (!method.isAnnotationPresent(RemotableViewMethod.class)) {
                        throw new ActionException("view: " + cls2.getName() + " can't use method with RemoteViews: " + str + getParameters(cls));
                    }
                    methods.put(new MutablePair<>(str, cls), method);
                } catch (NoSuchMethodException e) {
                    throw new ActionException("view: " + cls2.getName() + " doesn't have method: " + str + getParameters(cls));
                }
            }
        }
        return method;
    }

    private static String getParameters(Class<?> paramType) {
        return paramType == null ? "()" : "(" + paramType + ")";
    }

    private static Object[] wrapArg(Object value) {
        Object[] args = sInvokeArgsTls.get();
        args[0] = value;
        return args;
    }

    private class SetDrawableParameters extends Action {
        public static final int TAG = 3;
        int alpha;
        int colorFilter;
        PorterDuff.Mode filterMode;
        int level;
        boolean targetBackground;

        public SetDrawableParameters(int id, boolean targetBackground, int alpha, int colorFilter, PorterDuff.Mode mode, int level) {
            super();
            this.viewId = id;
            this.targetBackground = targetBackground;
            this.alpha = alpha;
            this.colorFilter = colorFilter;
            this.filterMode = mode;
            this.level = level;
        }

        public SetDrawableParameters(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.targetBackground = parcel.readInt() != 0;
            this.alpha = parcel.readInt();
            this.colorFilter = parcel.readInt();
            boolean hasMode = parcel.readInt() != 0;
            if (hasMode) {
                this.filterMode = PorterDuff.Mode.valueOf(parcel.readString());
            } else {
                this.filterMode = null;
            }
            this.level = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(3);
            dest.writeInt(this.viewId);
            dest.writeInt(this.targetBackground ? 1 : 0);
            dest.writeInt(this.alpha);
            dest.writeInt(this.colorFilter);
            if (this.filterMode != null) {
                dest.writeInt(1);
                dest.writeString(this.filterMode.toString());
            } else {
                dest.writeInt(0);
            }
            dest.writeInt(this.level);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                Drawable targetDrawable = null;
                if (this.targetBackground) {
                    targetDrawable = target.getBackground();
                } else if (target instanceof ImageView) {
                    ImageView imageView = (ImageView) target;
                    targetDrawable = imageView.getDrawable();
                }
                if (targetDrawable != null) {
                    if (this.alpha != -1) {
                        targetDrawable.setAlpha(this.alpha);
                    }
                    if (this.filterMode != null) {
                        targetDrawable.setColorFilter(this.colorFilter, this.filterMode);
                    }
                    if (this.level != -1) {
                        targetDrawable.setLevel(this.level);
                    }
                }
            }
        }

        @Override
        public String getActionName() {
            return "SetDrawableParameters";
        }
    }

    private final class ReflectionActionWithoutParams extends Action {
        public static final int TAG = 5;
        final String methodName;

        ReflectionActionWithoutParams(int viewId, String methodName) {
            super();
            this.viewId = viewId;
            this.methodName = methodName;
        }

        ReflectionActionWithoutParams(Parcel in) {
            super();
            this.viewId = in.readInt();
            this.methodName = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(5);
            out.writeInt(this.viewId);
            out.writeString(this.methodName);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View view = root.findViewById(this.viewId);
            if (view != null) {
                try {
                    RemoteViews.this.getMethod(view, this.methodName, null).invoke(view, new Object[0]);
                } catch (ActionException e) {
                    throw e;
                } catch (Exception ex) {
                    throw new ActionException(ex);
                }
            }
        }

        @Override
        public int mergeBehavior() {
            return (this.methodName.equals("showNext") || this.methodName.equals("showPrevious")) ? 2 : 0;
        }

        @Override
        public String getActionName() {
            return "ReflectionActionWithoutParams";
        }
    }

    private static class BitmapCache {
        ArrayList<Bitmap> mBitmaps;

        public BitmapCache() {
            this.mBitmaps = new ArrayList<>();
        }

        public BitmapCache(Parcel source) {
            int count = source.readInt();
            this.mBitmaps = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Bitmap b = Bitmap.CREATOR.createFromParcel(source);
                this.mBitmaps.add(b);
            }
        }

        public int getBitmapId(Bitmap b) {
            if (b == null) {
                return -1;
            }
            if (this.mBitmaps.contains(b)) {
                return this.mBitmaps.indexOf(b);
            }
            this.mBitmaps.add(b);
            return this.mBitmaps.size() - 1;
        }

        public Bitmap getBitmapForId(int id) {
            if (id == -1 || id >= this.mBitmaps.size()) {
                return null;
            }
            return this.mBitmaps.get(id);
        }

        public void writeBitmapsToParcel(Parcel dest, int flags) {
            int count = this.mBitmaps.size();
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                this.mBitmaps.get(i).writeToParcel(dest, flags);
            }
        }

        public void assimilate(BitmapCache bitmapCache) {
            ArrayList<Bitmap> bitmapsToBeAdded = bitmapCache.mBitmaps;
            int count = bitmapsToBeAdded.size();
            for (int i = 0; i < count; i++) {
                Bitmap b = bitmapsToBeAdded.get(i);
                if (!this.mBitmaps.contains(b)) {
                    this.mBitmaps.add(b);
                }
            }
        }

        public void addBitmapMemory(MemoryUsageCounter memoryCounter) {
            for (int i = 0; i < this.mBitmaps.size(); i++) {
                memoryCounter.addBitmapMemory(this.mBitmaps.get(i));
            }
        }
    }

    private class BitmapReflectionAction extends Action {
        public static final int TAG = 12;
        Bitmap bitmap;
        int bitmapId;
        String methodName;

        BitmapReflectionAction(int viewId, String methodName, Bitmap bitmap) {
            super();
            this.bitmap = bitmap;
            this.viewId = viewId;
            this.methodName = methodName;
            this.bitmapId = RemoteViews.this.mBitmapCache.getBitmapId(bitmap);
        }

        BitmapReflectionAction(Parcel in) {
            super();
            this.viewId = in.readInt();
            this.methodName = in.readString();
            this.bitmapId = in.readInt();
            this.bitmap = RemoteViews.this.mBitmapCache.getBitmapForId(this.bitmapId);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(12);
            dest.writeInt(this.viewId);
            dest.writeString(this.methodName);
            dest.writeInt(this.bitmapId);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) throws ActionException {
            ReflectionAction ra = RemoteViews.this.new ReflectionAction(this.viewId, this.methodName, 12, this.bitmap);
            ra.apply(root, rootParent, handler);
        }

        @Override
        public void setBitmapCache(BitmapCache bitmapCache) {
            this.bitmapId = bitmapCache.getBitmapId(this.bitmap);
        }

        @Override
        public String getActionName() {
            return "BitmapReflectionAction";
        }
    }

    private final class ReflectionAction extends Action {
        static final int BITMAP = 12;
        static final int BOOLEAN = 1;
        static final int BUNDLE = 13;
        static final int BYTE = 2;
        static final int CHAR = 8;
        static final int CHAR_SEQUENCE = 10;
        static final int COLOR_STATE_LIST = 15;
        static final int DOUBLE = 7;
        static final int FLOAT = 6;
        static final int INT = 4;
        static final int INTENT = 14;
        static final int LONG = 5;
        static final int SHORT = 3;
        static final int STRING = 9;
        static final int TAG = 2;
        static final int URI = 11;
        String methodName;
        int type;
        Object value;

        ReflectionAction(int viewId, String methodName, int type, Object value) {
            super();
            this.viewId = viewId;
            this.methodName = methodName;
            this.type = type;
            this.value = value;
        }

        ReflectionAction(Parcel in) {
            super();
            this.viewId = in.readInt();
            this.methodName = in.readString();
            this.type = in.readInt();
            switch (this.type) {
                case 1:
                    this.value = Boolean.valueOf(in.readInt() != 0);
                    break;
                case 2:
                    this.value = Byte.valueOf(in.readByte());
                    break;
                case 3:
                    this.value = Short.valueOf((short) in.readInt());
                    break;
                case 4:
                    this.value = Integer.valueOf(in.readInt());
                    break;
                case 5:
                    this.value = Long.valueOf(in.readLong());
                    break;
                case 6:
                    this.value = Float.valueOf(in.readFloat());
                    break;
                case 7:
                    this.value = Double.valueOf(in.readDouble());
                    break;
                case 8:
                    this.value = Character.valueOf((char) in.readInt());
                    break;
                case 9:
                    this.value = in.readString();
                    break;
                case 10:
                    this.value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    break;
                case 11:
                    if (in.readInt() != 0) {
                        this.value = Uri.CREATOR.createFromParcel(in);
                    }
                    break;
                case 12:
                    if (in.readInt() != 0) {
                        this.value = Bitmap.CREATOR.createFromParcel(in);
                    }
                    break;
                case 13:
                    this.value = in.readBundle();
                    break;
                case 14:
                    if (in.readInt() != 0) {
                        this.value = Intent.CREATOR.createFromParcel(in);
                    }
                    break;
                case 15:
                    if (in.readInt() != 0) {
                        this.value = ColorStateList.CREATOR.createFromParcel(in);
                    }
                    break;
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(2);
            out.writeInt(this.viewId);
            out.writeString(this.methodName);
            out.writeInt(this.type);
            switch (this.type) {
                case 1:
                    out.writeInt(((Boolean) this.value).booleanValue() ? 1 : 0);
                    break;
                case 2:
                    out.writeByte(((Byte) this.value).byteValue());
                    break;
                case 3:
                    out.writeInt(((Short) this.value).shortValue());
                    break;
                case 4:
                    out.writeInt(((Integer) this.value).intValue());
                    break;
                case 5:
                    out.writeLong(((Long) this.value).longValue());
                    break;
                case 6:
                    out.writeFloat(((Float) this.value).floatValue());
                    break;
                case 7:
                    out.writeDouble(((Double) this.value).doubleValue());
                    break;
                case 8:
                    out.writeInt(((Character) this.value).charValue());
                    break;
                case 9:
                    out.writeString((String) this.value);
                    break;
                case 10:
                    TextUtils.writeToParcel((CharSequence) this.value, out, flags);
                    break;
                case 11:
                    out.writeInt(this.value == null ? 0 : 1);
                    if (this.value != null) {
                        ((Uri) this.value).writeToParcel(out, flags);
                    }
                    break;
                case 12:
                    out.writeInt(this.value == null ? 0 : 1);
                    if (this.value != null) {
                        ((Bitmap) this.value).writeToParcel(out, flags);
                    }
                    break;
                case 13:
                    out.writeBundle((Bundle) this.value);
                    break;
                case 14:
                    out.writeInt(this.value == null ? 0 : 1);
                    if (this.value != null) {
                        ((Intent) this.value).writeToParcel(out, flags);
                    }
                    break;
                case 15:
                    out.writeInt(this.value == null ? 0 : 1);
                    if (this.value != null) {
                        ((ColorStateList) this.value).writeToParcel(out, flags);
                    }
                    break;
            }
        }

        private Class<?> getParameterType() {
            switch (this.type) {
                case 1:
                    return Boolean.TYPE;
                case 2:
                    return Byte.TYPE;
                case 3:
                    return Short.TYPE;
                case 4:
                    return Integer.TYPE;
                case 5:
                    return Long.TYPE;
                case 6:
                    return Float.TYPE;
                case 7:
                    return Double.TYPE;
                case 8:
                    return Character.TYPE;
                case 9:
                    return String.class;
                case 10:
                    return CharSequence.class;
                case 11:
                    return Uri.class;
                case 12:
                    return Bitmap.class;
                case 13:
                    return Bundle.class;
                case 14:
                    return Intent.class;
                case 15:
                    return ColorStateList.class;
                default:
                    return null;
            }
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View view = root.findViewById(this.viewId);
            if (view != null) {
                Class<?> param = getParameterType();
                if (param != null) {
                    try {
                        RemoteViews.this.getMethod(view, this.methodName, param).invoke(view, RemoteViews.wrapArg(this.value));
                        return;
                    } catch (ActionException e) {
                        throw e;
                    } catch (Exception ex) {
                        throw new ActionException(ex);
                    }
                }
                throw new ActionException("bad type: " + this.type);
            }
        }

        @Override
        public int mergeBehavior() {
            return this.methodName.equals("smoothScrollBy") ? 1 : 0;
        }

        @Override
        public String getActionName() {
            return "ReflectionAction" + this.methodName + this.type;
        }
    }

    private void configureRemoteViewsAsChild(RemoteViews rv) {
        this.mBitmapCache.assimilate(rv.mBitmapCache);
        rv.setBitmapCache(this.mBitmapCache);
        rv.setNotRoot();
    }

    void setNotRoot() {
        this.mIsRoot = false;
    }

    private class ViewGroupAction extends Action {
        public static final int TAG = 4;
        RemoteViews nestedViews;

        public ViewGroupAction(int viewId, RemoteViews nestedViews) {
            super();
            this.viewId = viewId;
            this.nestedViews = nestedViews;
            if (nestedViews != null) {
                RemoteViews.this.configureRemoteViewsAsChild(nestedViews);
            }
        }

        public ViewGroupAction(RemoteViews remoteViews, Parcel parcel, BitmapCache bitmapCache) {
            RemoteViews.this = remoteViews;
            super();
            this.viewId = parcel.readInt();
            boolean nestedViewsNull = parcel.readInt() == 0;
            if (!nestedViewsNull) {
                this.nestedViews = new RemoteViews(parcel, bitmapCache);
            } else {
                this.nestedViews = null;
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(4);
            dest.writeInt(this.viewId);
            if (this.nestedViews != null) {
                dest.writeInt(1);
                this.nestedViews.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            Context context = root.getContext();
            ViewGroup target = (ViewGroup) root.findViewById(this.viewId);
            if (target != null) {
                if (this.nestedViews != null) {
                    target.addView(this.nestedViews.apply(context, target, handler));
                } else {
                    target.removeAllViews();
                }
            }
        }

        @Override
        public void updateMemoryUsageEstimate(MemoryUsageCounter counter) {
            if (this.nestedViews != null) {
                counter.increment(this.nestedViews.estimateMemoryUsage());
            }
        }

        @Override
        public void setBitmapCache(BitmapCache bitmapCache) {
            if (this.nestedViews != null) {
                this.nestedViews.setBitmapCache(bitmapCache);
            }
        }

        @Override
        public String getActionName() {
            return "ViewGroupAction" + (this.nestedViews == null ? "Remove" : "Add");
        }

        @Override
        public int mergeBehavior() {
            return 1;
        }
    }

    private class TextViewDrawableAction extends Action {
        public static final int TAG = 11;
        int d1;
        int d2;
        int d3;
        int d4;
        boolean isRelative;

        public TextViewDrawableAction(int viewId, boolean isRelative, int d1, int d2, int d3, int d4) {
            super();
            this.isRelative = false;
            this.viewId = viewId;
            this.isRelative = isRelative;
            this.d1 = d1;
            this.d2 = d2;
            this.d3 = d3;
            this.d4 = d4;
        }

        public TextViewDrawableAction(Parcel parcel) {
            super();
            this.isRelative = false;
            this.viewId = parcel.readInt();
            this.isRelative = parcel.readInt() != 0;
            this.d1 = parcel.readInt();
            this.d2 = parcel.readInt();
            this.d3 = parcel.readInt();
            this.d4 = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(11);
            dest.writeInt(this.viewId);
            dest.writeInt(this.isRelative ? 1 : 0);
            dest.writeInt(this.d1);
            dest.writeInt(this.d2);
            dest.writeInt(this.d3);
            dest.writeInt(this.d4);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            TextView target = (TextView) root.findViewById(this.viewId);
            if (target != null) {
                if (this.isRelative) {
                    target.setCompoundDrawablesRelativeWithIntrinsicBounds(this.d1, this.d2, this.d3, this.d4);
                } else {
                    target.setCompoundDrawablesWithIntrinsicBounds(this.d1, this.d2, this.d3, this.d4);
                }
            }
        }

        @Override
        public String getActionName() {
            return "TextViewDrawableAction";
        }
    }

    private class TextViewSizeAction extends Action {
        public static final int TAG = 13;
        float size;
        int units;

        public TextViewSizeAction(int viewId, int units, float size) {
            super();
            this.viewId = viewId;
            this.units = units;
            this.size = size;
        }

        public TextViewSizeAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.units = parcel.readInt();
            this.size = parcel.readFloat();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(13);
            dest.writeInt(this.viewId);
            dest.writeInt(this.units);
            dest.writeFloat(this.size);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            TextView target = (TextView) root.findViewById(this.viewId);
            if (target != null) {
                target.setTextSize(this.units, this.size);
            }
        }

        @Override
        public String getActionName() {
            return "TextViewSizeAction";
        }
    }

    private class ViewPaddingAction extends Action {
        public static final int TAG = 14;
        int bottom;
        int left;
        int right;
        int top;

        public ViewPaddingAction(int viewId, int left, int top, int right, int bottom) {
            super();
            this.viewId = viewId;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public ViewPaddingAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.left = parcel.readInt();
            this.top = parcel.readInt();
            this.right = parcel.readInt();
            this.bottom = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(14);
            dest.writeInt(this.viewId);
            dest.writeInt(this.left);
            dest.writeInt(this.top);
            dest.writeInt(this.right);
            dest.writeInt(this.bottom);
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                target.setPadding(this.left, this.top, this.right, this.bottom);
            }
        }

        @Override
        public String getActionName() {
            return "ViewPaddingAction";
        }
    }

    private class TextViewDrawableColorFilterAction extends Action {
        public static final int TAG = 17;
        final int color;
        final int index;
        final boolean isRelative;
        final PorterDuff.Mode mode;

        public TextViewDrawableColorFilterAction(int viewId, boolean isRelative, int index, int color, PorterDuff.Mode mode) {
            super();
            this.viewId = viewId;
            this.isRelative = isRelative;
            this.index = index;
            this.color = color;
            this.mode = mode;
        }

        public TextViewDrawableColorFilterAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.isRelative = parcel.readInt() != 0;
            this.index = parcel.readInt();
            this.color = parcel.readInt();
            this.mode = readPorterDuffMode(parcel);
        }

        private PorterDuff.Mode readPorterDuffMode(Parcel parcel) {
            int mode = parcel.readInt();
            return (mode < 0 || mode >= PorterDuff.Mode.values().length) ? PorterDuff.Mode.CLEAR : PorterDuff.Mode.values()[mode];
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(17);
            dest.writeInt(this.viewId);
            dest.writeInt(this.isRelative ? 1 : 0);
            dest.writeInt(this.index);
            dest.writeInt(this.color);
            dest.writeInt(this.mode.ordinal());
        }

        @Override
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            TextView target = (TextView) root.findViewById(this.viewId);
            if (target != null) {
                Drawable[] drawables = this.isRelative ? target.getCompoundDrawablesRelative() : target.getCompoundDrawables();
                if (this.index < 0 || this.index >= 4) {
                    throw new IllegalStateException("index must be in range [0, 3].");
                }
                Drawable d = drawables[this.index];
                if (d != null) {
                    d.mutate();
                    d.setColorFilter(this.color, this.mode);
                }
            }
        }

        @Override
        public String getActionName() {
            return "TextViewDrawableColorFilterAction";
        }
    }

    private class MemoryUsageCounter {
        int mMemoryUsage;

        private MemoryUsageCounter() {
        }

        public void clear() {
            this.mMemoryUsage = 0;
        }

        public void increment(int numBytes) {
            this.mMemoryUsage += numBytes;
        }

        public int getMemoryUsage() {
            return this.mMemoryUsage;
        }

        public void addBitmapMemory(Bitmap b) {
            Bitmap.Config c = b.getConfig();
            int bpp = 4;
            if (c != null) {
                switch (c) {
                    case ALPHA_8:
                        bpp = 1;
                        break;
                    case RGB_565:
                    case ARGB_4444:
                        bpp = 2;
                        break;
                    case ARGB_8888:
                        bpp = 4;
                        break;
                }
            }
            increment(b.getWidth() * b.getHeight() * bpp);
        }
    }

    public RemoteViews(String packageName, int layoutId) {
        this(getApplicationInfo(packageName, UserHandle.myUserId()), layoutId);
    }

    public RemoteViews(String packageName, int userId, int layoutId) {
        this(getApplicationInfo(packageName, userId), layoutId);
    }

    protected RemoteViews(ApplicationInfo application, int layoutId) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mPair = new MutablePair<>(null, null);
        this.mApplication = application;
        this.mLayoutId = layoutId;
        this.mBitmapCache = new BitmapCache();
        this.mMemoryUsageCounter = new MemoryUsageCounter();
        recalculateMemoryUsage();
    }

    private boolean hasLandscapeAndPortraitLayouts() {
        return (this.mLandscape == null || this.mPortrait == null) ? false : true;
    }

    public RemoteViews(RemoteViews landscape, RemoteViews portrait) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mPair = new MutablePair<>(null, null);
        if (landscape == null || portrait == null) {
            throw new RuntimeException("Both RemoteViews must be non-null");
        }
        if (landscape.mApplication.uid != portrait.mApplication.uid || !landscape.mApplication.packageName.equals(portrait.mApplication.packageName)) {
            throw new RuntimeException("Both RemoteViews must share the same package and user");
        }
        this.mApplication = portrait.mApplication;
        this.mLayoutId = portrait.getLayoutId();
        this.mLandscape = landscape;
        this.mPortrait = portrait;
        this.mMemoryUsageCounter = new MemoryUsageCounter();
        this.mBitmapCache = new BitmapCache();
        configureRemoteViewsAsChild(landscape);
        configureRemoteViewsAsChild(portrait);
        recalculateMemoryUsage();
    }

    public RemoteViews(Parcel parcel) {
        this(parcel, (BitmapCache) null);
    }

    private RemoteViews(Parcel parcel, BitmapCache bitmapCache) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mPair = new MutablePair<>(null, null);
        int mode = parcel.readInt();
        if (bitmapCache == null) {
            this.mBitmapCache = new BitmapCache(parcel);
        } else {
            setBitmapCache(bitmapCache);
            setNotRoot();
        }
        if (mode == 0) {
            this.mApplication = (ApplicationInfo) parcel.readParcelable(null);
            this.mLayoutId = parcel.readInt();
            this.mIsWidgetCollectionChild = parcel.readInt() == 1;
            int count = parcel.readInt();
            if (count > 0) {
                this.mActions = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int tag = parcel.readInt();
                    switch (tag) {
                        case 1:
                            this.mActions.add(new SetOnClickPendingIntent(parcel));
                            break;
                        case 2:
                            this.mActions.add(new ReflectionAction(parcel));
                            break;
                        case 3:
                            this.mActions.add(new SetDrawableParameters(parcel));
                            break;
                        case 4:
                            this.mActions.add(new ViewGroupAction(this, parcel, this.mBitmapCache));
                            break;
                        case 5:
                            this.mActions.add(new ReflectionActionWithoutParams(parcel));
                            break;
                        case 6:
                            this.mActions.add(new SetEmptyView(parcel));
                            break;
                        case 7:
                        case 16:
                        default:
                            throw new ActionException("Tag " + tag + " not found");
                        case 8:
                            this.mActions.add(new SetPendingIntentTemplate(parcel));
                            break;
                        case 9:
                            this.mActions.add(new SetOnClickFillInIntent(parcel));
                            break;
                        case 10:
                            this.mActions.add(new SetRemoteViewsAdapterIntent(parcel));
                            break;
                        case 11:
                            this.mActions.add(new TextViewDrawableAction(parcel));
                            break;
                        case 12:
                            this.mActions.add(new BitmapReflectionAction(parcel));
                            break;
                        case 13:
                            this.mActions.add(new TextViewSizeAction(parcel));
                            break;
                        case 14:
                            this.mActions.add(new ViewPaddingAction(parcel));
                            break;
                        case 15:
                            this.mActions.add(new SetRemoteViewsAdapterList(parcel));
                            break;
                        case 17:
                            this.mActions.add(new TextViewDrawableColorFilterAction(parcel));
                            break;
                    }
                }
            }
        } else {
            this.mLandscape = new RemoteViews(parcel, this.mBitmapCache);
            this.mPortrait = new RemoteViews(parcel, this.mBitmapCache);
            this.mApplication = this.mPortrait.mApplication;
            this.mLayoutId = this.mPortrait.getLayoutId();
        }
        this.mMemoryUsageCounter = new MemoryUsageCounter();
        recalculateMemoryUsage();
    }

    @Override
    public RemoteViews mo11clone() {
        Parcel p = Parcel.obtain();
        writeToParcel(p, 0);
        p.setDataPosition(0);
        RemoteViews rv = new RemoteViews(p);
        p.recycle();
        return rv;
    }

    public String getPackage() {
        if (this.mApplication != null) {
            return this.mApplication.packageName;
        }
        return null;
    }

    public int getLayoutId() {
        return this.mLayoutId;
    }

    void setIsWidgetCollectionChild(boolean isWidgetCollectionChild) {
        this.mIsWidgetCollectionChild = isWidgetCollectionChild;
    }

    private void recalculateMemoryUsage() {
        this.mMemoryUsageCounter.clear();
        if (!hasLandscapeAndPortraitLayouts()) {
            if (this.mActions != null) {
                int count = this.mActions.size();
                for (int i = 0; i < count; i++) {
                    this.mActions.get(i).updateMemoryUsageEstimate(this.mMemoryUsageCounter);
                }
            }
            if (this.mIsRoot) {
                this.mBitmapCache.addBitmapMemory(this.mMemoryUsageCounter);
                return;
            }
            return;
        }
        this.mMemoryUsageCounter.increment(this.mLandscape.estimateMemoryUsage());
        this.mMemoryUsageCounter.increment(this.mPortrait.estimateMemoryUsage());
        this.mBitmapCache.addBitmapMemory(this.mMemoryUsageCounter);
    }

    private void setBitmapCache(BitmapCache bitmapCache) {
        this.mBitmapCache = bitmapCache;
        if (!hasLandscapeAndPortraitLayouts()) {
            if (this.mActions != null) {
                int count = this.mActions.size();
                for (int i = 0; i < count; i++) {
                    this.mActions.get(i).setBitmapCache(bitmapCache);
                }
                return;
            }
            return;
        }
        this.mLandscape.setBitmapCache(bitmapCache);
        this.mPortrait.setBitmapCache(bitmapCache);
    }

    public int estimateMemoryUsage() {
        return this.mMemoryUsageCounter.getMemoryUsage();
    }

    private void addAction(Action a) {
        if (hasLandscapeAndPortraitLayouts()) {
            throw new RuntimeException("RemoteViews specifying separate landscape and portrait layouts cannot be modified. Instead, fully configure the landscape and portrait layouts individually before constructing the combined layout.");
        }
        if (this.mActions == null) {
            this.mActions = new ArrayList<>();
        }
        this.mActions.add(a);
        a.updateMemoryUsageEstimate(this.mMemoryUsageCounter);
    }

    public void addView(int viewId, RemoteViews nestedView) {
        addAction(new ViewGroupAction(viewId, nestedView));
    }

    public void removeAllViews(int viewId) {
        addAction(new ViewGroupAction(viewId, (RemoteViews) null));
    }

    public void showNext(int viewId) {
        addAction(new ReflectionActionWithoutParams(viewId, "showNext"));
    }

    public void showPrevious(int viewId) {
        addAction(new ReflectionActionWithoutParams(viewId, "showPrevious"));
    }

    public void setDisplayedChild(int viewId, int childIndex) {
        setInt(viewId, "setDisplayedChild", childIndex);
    }

    public void setViewVisibility(int viewId, int visibility) {
        setInt(viewId, "setVisibility", visibility);
    }

    public void setTextViewText(int viewId, CharSequence text) {
        setCharSequence(viewId, "setText", text);
    }

    public void setTextViewTextSize(int viewId, int units, float size) {
        addAction(new TextViewSizeAction(viewId, units, size));
    }

    public void setTextViewCompoundDrawables(int viewId, int left, int top, int right, int bottom) {
        addAction(new TextViewDrawableAction(viewId, false, left, top, right, bottom));
    }

    public void setTextViewCompoundDrawablesRelative(int viewId, int start, int top, int end, int bottom) {
        addAction(new TextViewDrawableAction(viewId, true, start, top, end, bottom));
    }

    public void setTextViewCompoundDrawablesRelativeColorFilter(int viewId, int index, int color, PorterDuff.Mode mode) {
        if (index < 0 || index >= 4) {
            throw new IllegalArgumentException("index must be in range [0, 3].");
        }
        addAction(new TextViewDrawableColorFilterAction(viewId, true, index, color, mode));
    }

    public void setImageViewResource(int viewId, int srcId) {
        setInt(viewId, "setImageResource", srcId);
    }

    public void setImageViewUri(int viewId, Uri uri) {
        setUri(viewId, "setImageURI", uri);
    }

    public void setImageViewBitmap(int viewId, Bitmap bitmap) {
        setBitmap(viewId, "setImageBitmap", bitmap);
    }

    public void setEmptyView(int viewId, int emptyViewId) {
        addAction(new SetEmptyView(viewId, emptyViewId));
    }

    public void setChronometer(int viewId, long base, String format, boolean started) {
        setLong(viewId, "setBase", base);
        setString(viewId, "setFormat", format);
        setBoolean(viewId, "setStarted", started);
    }

    public void setProgressBar(int viewId, int max, int progress, boolean indeterminate) {
        setBoolean(viewId, "setIndeterminate", indeterminate);
        if (!indeterminate) {
            setInt(viewId, "setMax", max);
            setInt(viewId, "setProgress", progress);
        }
    }

    public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent) {
        addAction(new SetOnClickPendingIntent(viewId, pendingIntent));
    }

    public void setPendingIntentTemplate(int viewId, PendingIntent pendingIntentTemplate) {
        addAction(new SetPendingIntentTemplate(viewId, pendingIntentTemplate));
    }

    public void setOnClickFillInIntent(int viewId, Intent fillInIntent) {
        addAction(new SetOnClickFillInIntent(viewId, fillInIntent));
    }

    public void setDrawableParameters(int viewId, boolean targetBackground, int alpha, int colorFilter, PorterDuff.Mode mode, int level) {
        addAction(new SetDrawableParameters(viewId, targetBackground, alpha, colorFilter, mode, level));
    }

    public void setProgressTintList(int viewId, ColorStateList tint) {
        addAction(new ReflectionAction(viewId, "setProgressTintList", 15, tint));
    }

    public void setProgressBackgroundTintList(int viewId, ColorStateList tint) {
        addAction(new ReflectionAction(viewId, "setProgressBackgroundTintList", 15, tint));
    }

    public void setProgressIndeterminateTintList(int viewId, ColorStateList tint) {
        addAction(new ReflectionAction(viewId, "setIndeterminateTintList", 15, tint));
    }

    public void setTextColor(int viewId, int color) {
        setInt(viewId, "setTextColor", color);
    }

    @Deprecated
    public void setRemoteAdapter(int appWidgetId, int viewId, Intent intent) {
        setRemoteAdapter(viewId, intent);
    }

    public void setRemoteAdapter(int viewId, Intent intent) {
        addAction(new SetRemoteViewsAdapterIntent(viewId, intent));
    }

    public void setRemoteAdapter(int viewId, ArrayList<RemoteViews> list, int viewTypeCount) {
        addAction(new SetRemoteViewsAdapterList(viewId, list, viewTypeCount));
    }

    public void setScrollPosition(int viewId, int position) {
        setInt(viewId, "smoothScrollToPosition", position);
    }

    public void setRelativeScrollPosition(int viewId, int offset) {
        setInt(viewId, "smoothScrollByOffset", offset);
    }

    public void setViewPadding(int viewId, int left, int top, int right, int bottom) {
        addAction(new ViewPaddingAction(viewId, left, top, right, bottom));
    }

    public void setBoolean(int viewId, String methodName, boolean value) {
        addAction(new ReflectionAction(viewId, methodName, 1, Boolean.valueOf(value)));
    }

    public void setByte(int viewId, String methodName, byte value) {
        addAction(new ReflectionAction(viewId, methodName, 2, Byte.valueOf(value)));
    }

    public void setShort(int viewId, String methodName, short value) {
        addAction(new ReflectionAction(viewId, methodName, 3, Short.valueOf(value)));
    }

    public void setInt(int viewId, String methodName, int value) {
        addAction(new ReflectionAction(viewId, methodName, 4, Integer.valueOf(value)));
    }

    public void setLong(int viewId, String methodName, long value) {
        addAction(new ReflectionAction(viewId, methodName, 5, Long.valueOf(value)));
    }

    public void setFloat(int viewId, String methodName, float value) {
        addAction(new ReflectionAction(viewId, methodName, 6, Float.valueOf(value)));
    }

    public void setDouble(int viewId, String methodName, double value) {
        addAction(new ReflectionAction(viewId, methodName, 7, Double.valueOf(value)));
    }

    public void setChar(int viewId, String methodName, char value) {
        addAction(new ReflectionAction(viewId, methodName, 8, Character.valueOf(value)));
    }

    public void setString(int viewId, String methodName, String value) {
        addAction(new ReflectionAction(viewId, methodName, 9, value));
    }

    public void setCharSequence(int viewId, String methodName, CharSequence value) {
        addAction(new ReflectionAction(viewId, methodName, 10, value));
    }

    public void setUri(int viewId, String methodName, Uri value) {
        if (value != null) {
            value = value.getCanonicalUri();
            if (StrictMode.vmFileUriExposureEnabled()) {
                value.checkFileUriExposed("RemoteViews.setUri()");
            }
        }
        addAction(new ReflectionAction(viewId, methodName, 11, value));
    }

    public void setBitmap(int viewId, String methodName, Bitmap value) {
        addAction(new BitmapReflectionAction(viewId, methodName, value));
    }

    public void setBundle(int viewId, String methodName, Bundle value) {
        addAction(new ReflectionAction(viewId, methodName, 13, value));
    }

    public void setIntent(int viewId, String methodName, Intent value) {
        addAction(new ReflectionAction(viewId, methodName, 14, value));
    }

    public void setContentDescription(int viewId, CharSequence contentDescription) {
        setCharSequence(viewId, "setContentDescription", contentDescription);
    }

    public void setAccessibilityTraversalBefore(int viewId, int nextId) {
        setInt(viewId, "setAccessibilityTraversalBefore", nextId);
    }

    public void setAccessibilityTraversalAfter(int viewId, int nextId) {
        setInt(viewId, "setAccessibilityTraversalAfter", nextId);
    }

    public void setLabelFor(int viewId, int labeledId) {
        setInt(viewId, "setLabelFor", labeledId);
    }

    private RemoteViews getRemoteViewsToApply(Context context) {
        if (hasLandscapeAndPortraitLayouts()) {
            int orientation = context.getResources().getConfiguration().orientation;
            if (orientation == 2) {
                return this.mLandscape;
            }
            return this.mPortrait;
        }
        return this;
    }

    public View apply(Context context, ViewGroup parent) {
        return apply(context, parent, null);
    }

    public View apply(Context context, ViewGroup parent, OnClickHandler handler) {
        RemoteViews rvToApply = getRemoteViewsToApply(context);
        final Context contextForResources = getContextForResources(context);
        Context inflationContext = new ContextWrapper(context) {
            @Override
            public Resources getResources() {
                return contextForResources.getResources();
            }

            @Override
            public Resources.Theme getTheme() {
                return contextForResources.getTheme();
            }
        };
        LayoutInflater inflater = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).cloneInContext(inflationContext);
        inflater.setFilter(this);
        View result = inflater.inflate(rvToApply.getLayoutId(), parent, false);
        rvToApply.performApply(result, parent, handler);
        return result;
    }

    public void reapply(Context context, View v) {
        reapply(context, v, null);
    }

    public void reapply(Context context, View v, OnClickHandler handler) {
        RemoteViews rvToApply = getRemoteViewsToApply(context);
        if (hasLandscapeAndPortraitLayouts() && v.getId() != rvToApply.getLayoutId()) {
            throw new RuntimeException("Attempting to re-apply RemoteViews to a view that that does not share the same root layout id.");
        }
        rvToApply.performApply(v, (ViewGroup) v.getParent(), handler);
    }

    private void performApply(View v, ViewGroup parent, OnClickHandler handler) {
        if (this.mActions != null) {
            if (handler == null) {
                handler = DEFAULT_ON_CLICK_HANDLER;
            }
            int count = this.mActions.size();
            for (int i = 0; i < count; i++) {
                Action a = this.mActions.get(i);
                a.apply(v, parent, handler);
            }
        }
    }

    private Context getContextForResources(Context context) {
        if (this.mApplication != null) {
            if (context.getUserId() != UserHandle.getUserId(this.mApplication.uid) || !context.getPackageName().equals(this.mApplication.packageName)) {
                try {
                    return context.createApplicationContext(this.mApplication, 4);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_TAG, "Package name " + this.mApplication.packageName + " not found");
                    return context;
                }
            }
            return context;
        }
        return context;
    }

    public int getSequenceNumber() {
        if (this.mActions == null) {
            return 0;
        }
        return this.mActions.size();
    }

    @Override
    public boolean onLoadClass(Class clazz) {
        return clazz.isAnnotationPresent(RemoteView.class);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int count;
        if (!hasLandscapeAndPortraitLayouts()) {
            dest.writeInt(0);
            if (this.mIsRoot) {
                this.mBitmapCache.writeBitmapsToParcel(dest, flags);
            }
            dest.writeParcelable(this.mApplication, flags);
            dest.writeInt(this.mLayoutId);
            dest.writeInt(this.mIsWidgetCollectionChild ? 1 : 0);
            if (this.mActions != null) {
                count = this.mActions.size();
            } else {
                count = 0;
            }
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                Action a = this.mActions.get(i);
                a.writeToParcel(dest, 0);
            }
            return;
        }
        dest.writeInt(1);
        if (this.mIsRoot) {
            this.mBitmapCache.writeBitmapsToParcel(dest, flags);
        }
        this.mLandscape.writeToParcel(dest, flags);
        this.mPortrait.writeToParcel(dest, flags);
    }

    private static ApplicationInfo getApplicationInfo(String packageName, int userId) {
        if (packageName == null) {
            return null;
        }
        Application application = ActivityThread.currentApplication();
        if (application == null) {
            throw new IllegalStateException("Cannot create remote views out of an aplication.");
        }
        ApplicationInfo applicationInfo = application.getApplicationInfo();
        if (UserHandle.getUserId(applicationInfo.uid) != userId || !applicationInfo.packageName.equals(packageName)) {
            try {
                Context context = application.getBaseContext().createPackageContextAsUser(packageName, 0, new UserHandle(userId));
                return context.getApplicationInfo();
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("No such package " + packageName);
            }
        }
        return applicationInfo;
    }
}
