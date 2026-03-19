package android.widget;

import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.Application;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.os.UserHandle;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.RemoteViewsAdapter;
import com.android.internal.R;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.util.Preconditions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class RemoteViews implements Parcelable, LayoutInflater.Filter {
    private static final int BITMAP_REFLECTION_ACTION_TAG = 12;
    static final String EXTRA_REMOTEADAPTER_APPWIDGET_ID = "remoteAdapterAppWidgetId";
    private static final int LAYOUT_PARAM_ACTION_TAG = 19;
    private static final String LOG_TAG = "RemoteViews";
    private static final int MAX_NESTED_VIEWS = 10;
    private static final int MODE_HAS_LANDSCAPE_AND_PORTRAIT = 1;
    private static final int MODE_NORMAL = 0;
    private static final int OVERRIDE_TEXT_COLORS_TAG = 20;
    private static final int REFLECTION_ACTION_TAG = 2;
    private static final int SET_DRAWABLE_TINT_TAG = 3;
    private static final int SET_EMPTY_VIEW_ACTION_TAG = 6;
    private static final int SET_ON_CLICK_FILL_IN_INTENT_TAG = 9;
    private static final int SET_ON_CLICK_PENDING_INTENT_TAG = 1;
    private static final int SET_PENDING_INTENT_TEMPLATE_TAG = 8;
    private static final int SET_REMOTE_INPUTS_ACTION_TAG = 18;
    private static final int SET_REMOTE_VIEW_ADAPTER_INTENT_TAG = 10;
    private static final int SET_REMOTE_VIEW_ADAPTER_LIST_TAG = 15;
    private static final int TEXT_VIEW_DRAWABLE_ACTION_TAG = 11;
    private static final int TEXT_VIEW_SIZE_ACTION_TAG = 13;
    private static final int VIEW_CONTENT_NAVIGATION_TAG = 5;
    private static final int VIEW_GROUP_ACTION_ADD_TAG = 4;
    private static final int VIEW_GROUP_ACTION_REMOVE_TAG = 7;
    private static final int VIEW_PADDING_ACTION_TAG = 14;
    private ArrayList<Action> mActions;
    public ApplicationInfo mApplication;
    private int mApplyThemeResId;
    private BitmapCache mBitmapCache;
    private final Map<Class, Object> mClassCookies;
    private boolean mIsRoot;
    private boolean mIsWidgetCollectionChild;
    private RemoteViews mLandscape;
    private final int mLayoutId;
    private RemoteViews mPortrait;
    private boolean mReapplyDisallowed;
    private static final OnClickHandler DEFAULT_ON_CLICK_HANDLER = new OnClickHandler();
    private static final ArrayMap<MethodKey, MethodArgs> sMethods = new ArrayMap<>();
    private static final MethodKey sLookupKey = new MethodKey();
    private static final Action ACTION_NOOP = new RuntimeAction() {
        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
        }
    };
    public static final Parcelable.Creator<RemoteViews> CREATOR = new Parcelable.Creator<RemoteViews>() {
        @Override
        public RemoteViews createFromParcel(Parcel parcel) {
            return new RemoteViews(parcel);
        }

        @Override
        public RemoteViews[] newArray(int i) {
            return new RemoteViews[i];
        }
    };

    public interface OnViewAppliedListener {
        void onError(Exception exc);

        void onViewApplied(View view);
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RemoteView {
    }

    public void setRemoteInputs(int i, RemoteInput[] remoteInputArr) {
        this.mActions.add(new SetRemoteInputsAction(i, remoteInputArr));
    }

    public void reduceImageSizes(int i, int i2) {
        ArrayList<Bitmap> arrayList = this.mBitmapCache.mBitmaps;
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            arrayList.set(i3, Icon.scaleDownIfNecessary(arrayList.get(i3), i, i2));
        }
    }

    public void overrideTextColors(int i) {
        addAction(new OverrideTextColorsAction(i));
    }

    public void setReapplyDisallowed() {
        this.mReapplyDisallowed = true;
    }

    public boolean isReapplyDisallowed() {
        return this.mReapplyDisallowed;
    }

    static class MethodKey {
        public String methodName;
        public Class paramClass;
        public Class targetClass;

        MethodKey() {
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof MethodKey)) {
                return false;
            }
            MethodKey methodKey = (MethodKey) obj;
            return Objects.equals(methodKey.targetClass, this.targetClass) && Objects.equals(methodKey.paramClass, this.paramClass) && Objects.equals(methodKey.methodName, this.methodName);
        }

        public int hashCode() {
            return (Objects.hashCode(this.targetClass) ^ Objects.hashCode(this.paramClass)) ^ Objects.hashCode(this.methodName);
        }

        public void set(Class cls, Class cls2, String str) {
            this.targetClass = cls;
            this.paramClass = cls2;
            this.methodName = str;
        }
    }

    static class MethodArgs {
        public MethodHandle asyncMethod;
        public String asyncMethodName;
        public MethodHandle syncMethod;

        MethodArgs() {
        }
    }

    public static class ActionException extends RuntimeException {
        public ActionException(Exception exc) {
            super(exc);
        }

        public ActionException(String str) {
            super(str);
        }

        public ActionException(Throwable th) {
            super(th);
        }
    }

    public static class OnClickHandler {
        private int mEnterAnimationId;

        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent intent) {
            return onClickHandler(view, pendingIntent, intent, 0);
        }

        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent intent, int i) {
            ActivityOptions activityOptionsMakeBasic;
            try {
                Context context = view.getContext();
                if (this.mEnterAnimationId != 0) {
                    activityOptionsMakeBasic = ActivityOptions.makeCustomAnimation(context, this.mEnterAnimationId, 0);
                } else {
                    activityOptionsMakeBasic = ActivityOptions.makeBasic();
                }
                if (i != 0) {
                    activityOptionsMakeBasic.setLaunchWindowingMode(i);
                }
                context.startIntentSender(pendingIntent.getIntentSender(), intent, 268435456, 268435456, 0, activityOptionsMakeBasic.toBundle());
                return true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(RemoteViews.LOG_TAG, "Cannot send pending intent: ", e);
                return false;
            } catch (Exception e2) {
                Log.e(RemoteViews.LOG_TAG, "Cannot send pending intent due to unknown exception: ", e2);
                return false;
            }
        }

        public void setEnterAnimationId(int i) {
            this.mEnterAnimationId = i;
        }
    }

    private static abstract class Action implements Parcelable {
        public static final int MERGE_APPEND = 1;
        public static final int MERGE_IGNORE = 2;
        public static final int MERGE_REPLACE = 0;
        int viewId;

        public abstract void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) throws ActionException;

        public abstract int getActionTag();

        private Action() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public void setBitmapCache(BitmapCache bitmapCache) {
        }

        public int mergeBehavior() {
            return 0;
        }

        public String getUniqueKey() {
            return getActionTag() + Session.SESSION_SEPARATION_CHAR_CHILD + this.viewId;
        }

        public Action initActionAsync(ViewTree viewTree, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            return this;
        }

        public boolean prefersAsyncApply() {
            return false;
        }

        public boolean hasSameAppInfo(ApplicationInfo applicationInfo) {
            return true;
        }

        public void visitUris(Consumer<Uri> consumer) {
        }
    }

    private static abstract class RuntimeAction extends Action {
        private RuntimeAction() {
            super();
        }

        @Override
        public final int getActionTag() {
            return 0;
        }

        @Override
        public final void writeToParcel(Parcel parcel, int i) {
            throw new UnsupportedOperationException();
        }
    }

    public void mergeRemoteViews(RemoteViews remoteViews) {
        if (remoteViews == null) {
            return;
        }
        RemoteViews remoteViews2 = new RemoteViews(remoteViews);
        HashMap map = new HashMap();
        if (this.mActions == null) {
            this.mActions = new ArrayList<>();
        }
        int size = this.mActions.size();
        for (int i = 0; i < size; i++) {
            Action action = this.mActions.get(i);
            map.put(action.getUniqueKey(), action);
        }
        ArrayList<Action> arrayList = remoteViews2.mActions;
        if (arrayList == null) {
            return;
        }
        int size2 = arrayList.size();
        for (int i2 = 0; i2 < size2; i2++) {
            Action action2 = arrayList.get(i2);
            String uniqueKey = arrayList.get(i2).getUniqueKey();
            int iMergeBehavior = arrayList.get(i2).mergeBehavior();
            if (map.containsKey(uniqueKey) && iMergeBehavior == 0) {
                this.mActions.remove(map.get(uniqueKey));
                map.remove(uniqueKey);
            }
            if (iMergeBehavior == 0 || iMergeBehavior == 1) {
                this.mActions.add(action2);
            }
        }
        this.mBitmapCache = new BitmapCache();
        setBitmapCache(this.mBitmapCache);
    }

    public void visitUris(Consumer<Uri> consumer) {
        if (this.mActions != null) {
            for (int i = 0; i < this.mActions.size(); i++) {
                this.mActions.get(i).visitUris(consumer);
            }
        }
    }

    private static void visitIconUri(Icon icon, Consumer<Uri> consumer) {
        if (icon != null && icon.getType() == 4) {
            consumer.accept(icon.getUri());
        }
    }

    private static class RemoteViewsContextWrapper extends ContextWrapper {
        private final Context mContextForResources;

        RemoteViewsContextWrapper(Context context, Context context2) {
            super(context);
            this.mContextForResources = context2;
        }

        @Override
        public Resources getResources() {
            return this.mContextForResources.getResources();
        }

        @Override
        public Resources.Theme getTheme() {
            return this.mContextForResources.getTheme();
        }

        @Override
        public String getPackageName() {
            return this.mContextForResources.getPackageName();
        }
    }

    private class SetEmptyView extends Action {
        int emptyViewId;

        SetEmptyView(int i, int i2) {
            super();
            this.viewId = i;
            this.emptyViewId = i2;
        }

        SetEmptyView(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.emptyViewId = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.emptyViewId);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById instanceof AdapterView) {
                AdapterView adapterView = (AdapterView) viewFindViewById;
                View viewFindViewById2 = view.findViewById(this.emptyViewId);
                if (viewFindViewById2 == null) {
                    return;
                }
                adapterView.setEmptyView(viewFindViewById2);
            }
        }

        @Override
        public int getActionTag() {
            return 6;
        }
    }

    private class SetOnClickFillInIntent extends Action {
        Intent fillInIntent;

        public SetOnClickFillInIntent(int i, Intent intent) {
            super();
            this.viewId = i;
            this.fillInIntent = intent;
        }

        public SetOnClickFillInIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.fillInIntent = (Intent) parcel.readTypedObject(Intent.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeTypedObject(this.fillInIntent, 0);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, final OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            if (!RemoteViews.this.mIsWidgetCollectionChild) {
                Log.e(RemoteViews.LOG_TAG, "The method setOnClickFillInIntent is available only from RemoteViewsFactory (ie. on collection items).");
            } else if (viewFindViewById == view) {
                viewFindViewById.setTagInternal(R.id.fillInIntent, this.fillInIntent);
            } else if (this.fillInIntent != null) {
                viewFindViewById.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        View view3 = (View) view2.getParent();
                        while (view3 != null && !(view3 instanceof AdapterView) && (!(view3 instanceof AppWidgetHostView) || (view3 instanceof RemoteViewsAdapter.RemoteViewsFrameLayout))) {
                            view3 = (View) view3.getParent();
                        }
                        if (!(view3 instanceof AdapterView)) {
                            Log.e(RemoteViews.LOG_TAG, "Collection item doesn't have AdapterView parent");
                            return;
                        }
                        if (!(view3.getTag() instanceof PendingIntent)) {
                            Log.e(RemoteViews.LOG_TAG, "Attempting setOnClickFillInIntent without calling setPendingIntentTemplate on parent.");
                            return;
                        }
                        PendingIntent pendingIntent = (PendingIntent) view3.getTag();
                        SetOnClickFillInIntent.this.fillInIntent.setSourceBounds(RemoteViews.getSourceBounds(view2));
                        onClickHandler.onClickHandler(view2, pendingIntent, SetOnClickFillInIntent.this.fillInIntent);
                    }
                });
            }
        }

        @Override
        public int getActionTag() {
            return 9;
        }
    }

    private class SetPendingIntentTemplate extends Action {
        PendingIntent pendingIntentTemplate;

        public SetPendingIntentTemplate(int i, PendingIntent pendingIntent) {
            super();
            this.viewId = i;
            this.pendingIntentTemplate = pendingIntent;
        }

        public SetPendingIntentTemplate(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.pendingIntentTemplate = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            PendingIntent.writePendingIntentOrNullToParcel(this.pendingIntentTemplate, parcel);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, final OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            if (viewFindViewById instanceof AdapterView) {
                AdapterView adapterView = (AdapterView) viewFindViewById;
                adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView2, View view2, int i, long j) {
                        if (view2 instanceof ViewGroup) {
                            ViewGroup viewGroup2 = (ViewGroup) view2;
                            int i2 = 0;
                            if (adapterView2 instanceof AdapterViewAnimator) {
                                viewGroup2 = (ViewGroup) viewGroup2.getChildAt(0);
                            }
                            if (viewGroup2 == null) {
                                return;
                            }
                            Intent intent = null;
                            int childCount = viewGroup2.getChildCount();
                            while (true) {
                                if (i2 >= childCount) {
                                    break;
                                }
                                Object tag = viewGroup2.getChildAt(i2).getTag(R.id.fillInIntent);
                                if (!(tag instanceof Intent)) {
                                    i2++;
                                } else {
                                    intent = (Intent) tag;
                                    break;
                                }
                            }
                            if (intent == null) {
                                return;
                            }
                            new Intent().setSourceBounds(RemoteViews.getSourceBounds(view2));
                            onClickHandler.onClickHandler(view2, SetPendingIntentTemplate.this.pendingIntentTemplate, intent);
                        }
                    }
                });
                adapterView.setTag(this.pendingIntentTemplate);
            } else {
                Log.e(RemoteViews.LOG_TAG, "Cannot setPendingIntentTemplate on a view which is notan AdapterView (id: " + this.viewId + ")");
            }
        }

        @Override
        public int getActionTag() {
            return 8;
        }
    }

    private class SetRemoteViewsAdapterList extends Action {
        ArrayList<RemoteViews> list;
        int viewTypeCount;

        public SetRemoteViewsAdapterList(int i, ArrayList<RemoteViews> arrayList, int i2) {
            super();
            this.viewId = i;
            this.list = arrayList;
            this.viewTypeCount = i2;
        }

        public SetRemoteViewsAdapterList(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.viewTypeCount = parcel.readInt();
            this.list = parcel.createTypedArrayList(RemoteViews.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.viewTypeCount);
            parcel.writeTypedList(this.list, i);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            if (!(viewGroup instanceof AppWidgetHostView)) {
                Log.e(RemoteViews.LOG_TAG, "SetRemoteViewsAdapterIntent action can only be used for AppWidgets (root id: " + this.viewId + ")");
                return;
            }
            boolean z = viewFindViewById instanceof AbsListView;
            if (!z && !(viewFindViewById instanceof AdapterViewAnimator)) {
                Log.e(RemoteViews.LOG_TAG, "Cannot setRemoteViewsAdapter on a view which is not an AbsListView or AdapterViewAnimator (id: " + this.viewId + ")");
                return;
            }
            if (z) {
                AbsListView absListView = (AbsListView) viewFindViewById;
                ListAdapter adapter = absListView.getAdapter();
                if ((adapter instanceof RemoteViewsListAdapter) && this.viewTypeCount <= adapter.getViewTypeCount()) {
                    ((RemoteViewsListAdapter) adapter).setViewsList(this.list);
                    return;
                } else {
                    absListView.setAdapter((ListAdapter) new RemoteViewsListAdapter(absListView.getContext(), this.list, this.viewTypeCount));
                    return;
                }
            }
            if (viewFindViewById instanceof AdapterViewAnimator) {
                AdapterViewAnimator adapterViewAnimator = (AdapterViewAnimator) viewFindViewById;
                Adapter adapter2 = adapterViewAnimator.getAdapter();
                if ((adapter2 instanceof RemoteViewsListAdapter) && this.viewTypeCount <= adapter2.getViewTypeCount()) {
                    ((RemoteViewsListAdapter) adapter2).setViewsList(this.list);
                } else {
                    adapterViewAnimator.setAdapter(new RemoteViewsListAdapter(adapterViewAnimator.getContext(), this.list, this.viewTypeCount));
                }
            }
        }

        @Override
        public int getActionTag() {
            return 15;
        }
    }

    private class SetRemoteViewsAdapterIntent extends Action {
        Intent intent;
        boolean isAsync;

        public SetRemoteViewsAdapterIntent(int i, Intent intent) {
            super();
            this.isAsync = false;
            this.viewId = i;
            this.intent = intent;
        }

        public SetRemoteViewsAdapterIntent(Parcel parcel) {
            super();
            this.isAsync = false;
            this.viewId = parcel.readInt();
            this.intent = (Intent) parcel.readTypedObject(Intent.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeTypedObject(this.intent, i);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            if (!(viewGroup instanceof AppWidgetHostView)) {
                Log.e(RemoteViews.LOG_TAG, "SetRemoteViewsAdapterIntent action can only be used for AppWidgets (root id: " + this.viewId + ")");
                return;
            }
            boolean z = viewFindViewById instanceof AbsListView;
            if (!z && !(viewFindViewById instanceof AdapterViewAnimator)) {
                Log.e(RemoteViews.LOG_TAG, "Cannot setRemoteViewsAdapter on a view which is not an AbsListView or AdapterViewAnimator (id: " + this.viewId + ")");
                return;
            }
            this.intent.putExtra(RemoteViews.EXTRA_REMOTEADAPTER_APPWIDGET_ID, ((AppWidgetHostView) viewGroup).getAppWidgetId());
            if (z) {
                AbsListView absListView = (AbsListView) viewFindViewById;
                absListView.setRemoteViewsAdapter(this.intent, this.isAsync);
                absListView.setRemoteViewsOnClickHandler(onClickHandler);
            } else if (viewFindViewById instanceof AdapterViewAnimator) {
                AdapterViewAnimator adapterViewAnimator = (AdapterViewAnimator) viewFindViewById;
                adapterViewAnimator.setRemoteViewsAdapter(this.intent, this.isAsync);
                adapterViewAnimator.setRemoteViewsOnClickHandler(onClickHandler);
            }
        }

        @Override
        public Action initActionAsync(ViewTree viewTree, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            SetRemoteViewsAdapterIntent setRemoteViewsAdapterIntent = RemoteViews.this.new SetRemoteViewsAdapterIntent(this.viewId, this.intent);
            setRemoteViewsAdapterIntent.isAsync = true;
            return setRemoteViewsAdapterIntent;
        }

        @Override
        public int getActionTag() {
            return 10;
        }
    }

    private class SetOnClickPendingIntent extends Action {
        PendingIntent pendingIntent;

        public SetOnClickPendingIntent(int i, PendingIntent pendingIntent) {
            super();
            this.viewId = i;
            this.pendingIntent = pendingIntent;
        }

        public SetOnClickPendingIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.pendingIntent = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            PendingIntent.writePendingIntentOrNullToParcel(this.pendingIntent, parcel);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, final OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            if (RemoteViews.this.mIsWidgetCollectionChild) {
                Log.w(RemoteViews.LOG_TAG, "Cannot setOnClickPendingIntent for collection item (id: " + this.viewId + ")");
                ApplicationInfo applicationInfo = view.getContext().getApplicationInfo();
                if (applicationInfo != null && applicationInfo.targetSdkVersion >= 16) {
                    return;
                }
            }
            View.OnClickListener onClickListener = null;
            if (this.pendingIntent != null) {
                onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        Rect sourceBounds = RemoteViews.getSourceBounds(view2);
                        Intent intent = new Intent();
                        intent.setSourceBounds(sourceBounds);
                        onClickHandler.onClickHandler(view2, SetOnClickPendingIntent.this.pendingIntent, intent);
                    }
                };
            }
            viewFindViewById.setTagInternal(R.id.pending_intent_tag, this.pendingIntent);
            viewFindViewById.setOnClickListener(onClickListener);
        }

        @Override
        public int getActionTag() {
            return 1;
        }
    }

    private static Rect getSourceBounds(View view) {
        float f = view.getContext().getResources().getCompatibilityInfo().applicationScale;
        view.getLocationOnScreen(new int[2]);
        Rect rect = new Rect();
        rect.left = (int) ((r1[0] * f) + 0.5f);
        rect.top = (int) ((r1[1] * f) + 0.5f);
        rect.right = (int) (((r1[0] + view.getWidth()) * f) + 0.5f);
        rect.bottom = (int) (((r1[1] + view.getHeight()) * f) + 0.5f);
        return rect;
    }

    private MethodHandle getMethod(View view, String str, Class<?> cls, boolean z) {
        Method method;
        Class<?> cls2 = view.getClass();
        synchronized (sMethods) {
            sLookupKey.set(cls2, cls, str);
            MethodArgs methodArgs = sMethods.get(sLookupKey);
            if (methodArgs == null) {
                try {
                    if (cls == null) {
                        method = cls2.getMethod(str, new Class[0]);
                    } else {
                        method = cls2.getMethod(str, cls);
                    }
                    if (!method.isAnnotationPresent(RemotableViewMethod.class)) {
                        throw new ActionException("view: " + cls2.getName() + " can't use method with RemoteViews: " + str + getParameters(cls));
                    }
                    MethodArgs methodArgs2 = new MethodArgs();
                    methodArgs2.syncMethod = MethodHandles.publicLookup().unreflect(method);
                    methodArgs2.asyncMethodName = ((RemotableViewMethod) method.getAnnotation(RemotableViewMethod.class)).asyncImpl();
                    MethodKey methodKey = new MethodKey();
                    methodKey.set(cls2, cls, str);
                    sMethods.put(methodKey, methodArgs2);
                    methodArgs = methodArgs2;
                    if (z) {
                        return methodArgs.syncMethod;
                    }
                    if (methodArgs.asyncMethodName.isEmpty()) {
                        return null;
                    }
                    if (methodArgs.asyncMethod == null) {
                        MethodType methodTypeChangeReturnType = methodArgs.syncMethod.type().dropParameterTypes(0, 1).changeReturnType(Runnable.class);
                        try {
                            methodArgs.asyncMethod = MethodHandles.publicLookup().findVirtual(cls2, methodArgs.asyncMethodName, methodTypeChangeReturnType);
                        } catch (IllegalAccessException | NoSuchMethodException e) {
                            throw new ActionException("Async implementation declared as " + methodArgs.asyncMethodName + " but not defined for " + str + ": public Runnable " + methodArgs.asyncMethodName + " (" + TextUtils.join(",", methodTypeChangeReturnType.parameterArray()) + ")");
                        }
                    }
                    return methodArgs.asyncMethod;
                } catch (IllegalAccessException | NoSuchMethodException e2) {
                    throw new ActionException("view: " + cls2.getName() + " doesn't have method: " + str + getParameters(cls));
                }
            }
            if (z) {
            }
        }
    }

    private static String getParameters(Class<?> cls) {
        if (cls == null) {
            return "()";
        }
        return "(" + cls + ")";
    }

    private class SetDrawableTint extends Action {
        int colorFilter;
        PorterDuff.Mode filterMode;
        boolean targetBackground;

        SetDrawableTint(int i, boolean z, int i2, PorterDuff.Mode mode) {
            super();
            this.viewId = i;
            this.targetBackground = z;
            this.colorFilter = i2;
            this.filterMode = mode;
        }

        SetDrawableTint(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.targetBackground = parcel.readInt() != 0;
            this.colorFilter = parcel.readInt();
            this.filterMode = PorterDuff.intToMode(parcel.readInt());
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.targetBackground ? 1 : 0);
            parcel.writeInt(this.colorFilter);
            parcel.writeInt(PorterDuff.modeToInt(this.filterMode));
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            Drawable drawable = null;
            if (this.targetBackground) {
                drawable = viewFindViewById.getBackground();
            } else if (viewFindViewById instanceof ImageView) {
                drawable = ((ImageView) viewFindViewById).getDrawable();
            }
            if (drawable != null) {
                drawable.mutate().setColorFilter(this.colorFilter, this.filterMode);
            }
        }

        @Override
        public int getActionTag() {
            return 3;
        }
    }

    private final class ViewContentNavigation extends Action {
        final boolean mNext;

        ViewContentNavigation(int i, boolean z) {
            super();
            this.viewId = i;
            this.mNext = z;
        }

        ViewContentNavigation(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.mNext = parcel.readBoolean();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeBoolean(this.mNext);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            try {
                (void) RemoteViews.this.getMethod(viewFindViewById, this.mNext ? "showNext" : "showPrevious", null, false).invoke(viewFindViewById);
            } catch (Throwable th) {
                throw new ActionException(th);
            }
        }

        @Override
        public int mergeBehavior() {
            return 2;
        }

        @Override
        public int getActionTag() {
            return 5;
        }
    }

    private static class BitmapCache {
        int mBitmapMemory;
        ArrayList<Bitmap> mBitmaps;

        public BitmapCache() {
            this.mBitmapMemory = -1;
            this.mBitmaps = new ArrayList<>();
        }

        public BitmapCache(Parcel parcel) {
            this.mBitmapMemory = -1;
            this.mBitmaps = parcel.createTypedArrayList(Bitmap.CREATOR);
        }

        public int getBitmapId(Bitmap bitmap) {
            if (bitmap == null) {
                return -1;
            }
            if (this.mBitmaps.contains(bitmap)) {
                return this.mBitmaps.indexOf(bitmap);
            }
            this.mBitmaps.add(bitmap);
            this.mBitmapMemory = -1;
            return this.mBitmaps.size() - 1;
        }

        public Bitmap getBitmapForId(int i) {
            if (i == -1 || i >= this.mBitmaps.size()) {
                return null;
            }
            return this.mBitmaps.get(i);
        }

        public void writeBitmapsToParcel(Parcel parcel, int i) {
            parcel.writeTypedList(this.mBitmaps, i);
        }

        public int getBitmapMemory() {
            if (this.mBitmapMemory < 0) {
                this.mBitmapMemory = 0;
                int size = this.mBitmaps.size();
                for (int i = 0; i < size; i++) {
                    this.mBitmapMemory += this.mBitmaps.get(i).getAllocationByteCount();
                }
            }
            return this.mBitmapMemory;
        }
    }

    private class BitmapReflectionAction extends Action {
        Bitmap bitmap;
        int bitmapId;
        String methodName;

        BitmapReflectionAction(int i, String str, Bitmap bitmap) {
            super();
            this.bitmap = bitmap;
            this.viewId = i;
            this.methodName = str;
            this.bitmapId = RemoteViews.this.mBitmapCache.getBitmapId(bitmap);
        }

        BitmapReflectionAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.methodName = parcel.readString();
            this.bitmapId = parcel.readInt();
            this.bitmap = RemoteViews.this.mBitmapCache.getBitmapForId(this.bitmapId);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeString(this.methodName);
            parcel.writeInt(this.bitmapId);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) throws ActionException {
            RemoteViews.this.new ReflectionAction(this.viewId, this.methodName, 12, this.bitmap).apply(view, viewGroup, onClickHandler);
        }

        @Override
        public void setBitmapCache(BitmapCache bitmapCache) {
            this.bitmapId = bitmapCache.getBitmapId(this.bitmap);
        }

        @Override
        public int getActionTag() {
            return 12;
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
        static final int ICON = 16;
        static final int INT = 4;
        static final int INTENT = 14;
        static final int LONG = 5;
        static final int SHORT = 3;
        static final int STRING = 9;
        static final int URI = 11;
        String methodName;
        int type;
        Object value;

        ReflectionAction(int i, String str, int i2, Object obj) {
            super();
            this.viewId = i;
            this.methodName = str;
            this.type = i2;
            this.value = obj;
        }

        ReflectionAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.methodName = parcel.readString();
            this.type = parcel.readInt();
            switch (this.type) {
                case 1:
                    this.value = Boolean.valueOf(parcel.readBoolean());
                    break;
                case 2:
                    this.value = Byte.valueOf(parcel.readByte());
                    break;
                case 3:
                    this.value = Short.valueOf((short) parcel.readInt());
                    break;
                case 4:
                    this.value = Integer.valueOf(parcel.readInt());
                    break;
                case 5:
                    this.value = Long.valueOf(parcel.readLong());
                    break;
                case 6:
                    this.value = Float.valueOf(parcel.readFloat());
                    break;
                case 7:
                    this.value = Double.valueOf(parcel.readDouble());
                    break;
                case 8:
                    this.value = Character.valueOf((char) parcel.readInt());
                    break;
                case 9:
                    this.value = parcel.readString();
                    break;
                case 10:
                    this.value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                    break;
                case 11:
                    this.value = parcel.readTypedObject(Uri.CREATOR);
                    break;
                case 12:
                    this.value = parcel.readTypedObject(Bitmap.CREATOR);
                    break;
                case 13:
                    this.value = parcel.readBundle();
                    break;
                case 14:
                    this.value = parcel.readTypedObject(Intent.CREATOR);
                    break;
                case 15:
                    this.value = parcel.readTypedObject(ColorStateList.CREATOR);
                    break;
                case 16:
                    this.value = parcel.readTypedObject(Icon.CREATOR);
                    break;
            }
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeString(this.methodName);
            parcel.writeInt(this.type);
            switch (this.type) {
                case 1:
                    parcel.writeBoolean(((Boolean) this.value).booleanValue());
                    break;
                case 2:
                    parcel.writeByte(((Byte) this.value).byteValue());
                    break;
                case 3:
                    parcel.writeInt(((Short) this.value).shortValue());
                    break;
                case 4:
                    parcel.writeInt(((Integer) this.value).intValue());
                    break;
                case 5:
                    parcel.writeLong(((Long) this.value).longValue());
                    break;
                case 6:
                    parcel.writeFloat(((Float) this.value).floatValue());
                    break;
                case 7:
                    parcel.writeDouble(((Double) this.value).doubleValue());
                    break;
                case 8:
                    parcel.writeInt(((Character) this.value).charValue());
                    break;
                case 9:
                    parcel.writeString((String) this.value);
                    break;
                case 10:
                    TextUtils.writeToParcel((CharSequence) this.value, parcel, i);
                    break;
                case 11:
                case 12:
                case 14:
                case 15:
                case 16:
                    parcel.writeTypedObject((Parcelable) this.value, i);
                    break;
                case 13:
                    parcel.writeBundle((Bundle) this.value);
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
                case 16:
                    return Icon.class;
                default:
                    return null;
            }
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            Class<?> parameterType = getParameterType();
            if (parameterType != null) {
                try {
                    (void) RemoteViews.this.getMethod(viewFindViewById, this.methodName, parameterType, false).invoke(viewFindViewById, this.value);
                } catch (Throwable th) {
                    throw new ActionException(th);
                }
            } else {
                throw new ActionException("bad type: " + this.type);
            }
        }

        @Override
        public Action initActionAsync(ViewTree viewTree, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = viewTree.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return RemoteViews.ACTION_NOOP;
            }
            Class<?> parameterType = getParameterType();
            if (parameterType != null) {
                try {
                    MethodHandle method = RemoteViews.this.getMethod(viewFindViewById, this.methodName, parameterType, true);
                    if (method != null) {
                        Runnable runnableInvoke = (Runnable) method.invoke(viewFindViewById, this.value);
                        if (runnableInvoke == null) {
                            return RemoteViews.ACTION_NOOP;
                        }
                        if (runnableInvoke instanceof ViewStub.ViewReplaceRunnable) {
                            viewTree.createTree();
                            viewTree.findViewTreeById(this.viewId).replaceView(((ViewStub.ViewReplaceRunnable) runnableInvoke).view);
                        }
                        return new RunnableAction(runnableInvoke);
                    }
                    return this;
                } catch (Throwable th) {
                    throw new ActionException(th);
                }
            }
            throw new ActionException("bad type: " + this.type);
        }

        @Override
        public int mergeBehavior() {
            if (this.methodName.equals("smoothScrollBy")) {
                return 1;
            }
            return 0;
        }

        @Override
        public int getActionTag() {
            return 2;
        }

        @Override
        public String getUniqueKey() {
            return super.getUniqueKey() + this.methodName + this.type;
        }

        @Override
        public boolean prefersAsyncApply() {
            return this.type == 11 || this.type == 16;
        }

        @Override
        public void visitUris(Consumer<Uri> consumer) {
            int i = this.type;
            if (i == 11) {
                consumer.accept((Uri) this.value);
            } else if (i == 16) {
                RemoteViews.visitIconUri((Icon) this.value, consumer);
            }
        }
    }

    private static final class RunnableAction extends RuntimeAction {
        private final Runnable mRunnable;

        RunnableAction(Runnable runnable) {
            super();
            this.mRunnable = runnable;
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            this.mRunnable.run();
        }
    }

    private void configureRemoteViewsAsChild(RemoteViews remoteViews) {
        remoteViews.setBitmapCache(this.mBitmapCache);
        remoteViews.setNotRoot();
    }

    void setNotRoot() {
        this.mIsRoot = false;
    }

    private class ViewGroupActionAdd extends Action {
        private int mIndex;
        private RemoteViews mNestedViews;

        ViewGroupActionAdd(RemoteViews remoteViews, int i, RemoteViews remoteViews2) {
            this(i, remoteViews2, -1);
        }

        ViewGroupActionAdd(int i, RemoteViews remoteViews, int i2) {
            super();
            this.viewId = i;
            this.mNestedViews = remoteViews;
            this.mIndex = i2;
            if (remoteViews != null) {
                RemoteViews.this.configureRemoteViewsAsChild(remoteViews);
            }
        }

        ViewGroupActionAdd(Parcel parcel, BitmapCache bitmapCache, ApplicationInfo applicationInfo, int i, Map<Class, Object> map) {
            super();
            this.viewId = parcel.readInt();
            this.mIndex = parcel.readInt();
            this.mNestedViews = new RemoteViews(parcel, bitmapCache, applicationInfo, i, map);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.mIndex);
            this.mNestedViews.writeToParcel(parcel, i);
        }

        @Override
        public boolean hasSameAppInfo(ApplicationInfo applicationInfo) {
            return this.mNestedViews.hasSameAppInfo(applicationInfo);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            Context context = view.getContext();
            ViewGroup viewGroup2 = (ViewGroup) view.findViewById(this.viewId);
            if (viewGroup2 == null) {
                return;
            }
            viewGroup2.addView(this.mNestedViews.apply(context, viewGroup2, onClickHandler), this.mIndex);
        }

        @Override
        public Action initActionAsync(ViewTree viewTree, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            viewTree.createTree();
            ViewTree viewTreeFindViewTreeById = viewTree.findViewTreeById(this.viewId);
            if (viewTreeFindViewTreeById == null || !(viewTreeFindViewTreeById.mRoot instanceof ViewGroup)) {
                return RemoteViews.ACTION_NOOP;
            }
            final ViewGroup viewGroup2 = (ViewGroup) viewTreeFindViewTreeById.mRoot;
            final AsyncApplyTask asyncApplyTask = this.mNestedViews.getAsyncApplyTask(viewTree.mRoot.getContext(), viewGroup2, null, onClickHandler);
            final ViewTree viewTreeDoInBackground = asyncApplyTask.doInBackground(new Void[0]);
            if (viewTreeDoInBackground == null) {
                throw new ActionException(asyncApplyTask.mError);
            }
            viewTreeFindViewTreeById.addChild(viewTreeDoInBackground, this.mIndex);
            return new RuntimeAction() {
                {
                    super();
                }

                @Override
                public void apply(View view, ViewGroup viewGroup3, OnClickHandler onClickHandler2) throws ActionException {
                    asyncApplyTask.onPostExecute(viewTreeDoInBackground);
                    viewGroup2.addView(asyncApplyTask.mResult, ViewGroupActionAdd.this.mIndex);
                }
            };
        }

        @Override
        public void setBitmapCache(BitmapCache bitmapCache) {
            this.mNestedViews.setBitmapCache(bitmapCache);
        }

        @Override
        public int mergeBehavior() {
            return 1;
        }

        @Override
        public boolean prefersAsyncApply() {
            return this.mNestedViews.prefersAsyncApply();
        }

        @Override
        public int getActionTag() {
            return 4;
        }
    }

    private class ViewGroupActionRemove extends Action {
        private static final int REMOVE_ALL_VIEWS_ID = -2;
        private int mViewIdToKeep;

        ViewGroupActionRemove(RemoteViews remoteViews, int i) {
            this(i, -2);
        }

        ViewGroupActionRemove(int i, int i2) {
            super();
            this.viewId = i;
            this.mViewIdToKeep = i2;
        }

        ViewGroupActionRemove(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.mViewIdToKeep = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.mViewIdToKeep);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            ViewGroup viewGroup2 = (ViewGroup) view.findViewById(this.viewId);
            if (viewGroup2 == null) {
                return;
            }
            if (this.mViewIdToKeep == -2) {
                viewGroup2.removeAllViews();
            } else {
                removeAllViewsExceptIdToKeep(viewGroup2);
            }
        }

        @Override
        public Action initActionAsync(ViewTree viewTree, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            viewTree.createTree();
            ViewTree viewTreeFindViewTreeById = viewTree.findViewTreeById(this.viewId);
            if (viewTreeFindViewTreeById == null || !(viewTreeFindViewTreeById.mRoot instanceof ViewGroup)) {
                return RemoteViews.ACTION_NOOP;
            }
            final ViewGroup viewGroup2 = (ViewGroup) viewTreeFindViewTreeById.mRoot;
            viewTreeFindViewTreeById.mChildren = null;
            return new RuntimeAction() {
                {
                    super();
                }

                @Override
                public void apply(View view, ViewGroup viewGroup3, OnClickHandler onClickHandler2) throws ActionException {
                    if (ViewGroupActionRemove.this.mViewIdToKeep != -2) {
                        ViewGroupActionRemove.this.removeAllViewsExceptIdToKeep(viewGroup2);
                    } else {
                        viewGroup2.removeAllViews();
                    }
                }
            };
        }

        private void removeAllViewsExceptIdToKeep(ViewGroup viewGroup) {
            for (int childCount = viewGroup.getChildCount() - 1; childCount >= 0; childCount--) {
                if (viewGroup.getChildAt(childCount).getId() != this.mViewIdToKeep) {
                    viewGroup.removeViewAt(childCount);
                }
            }
        }

        @Override
        public int getActionTag() {
            return 7;
        }

        @Override
        public int mergeBehavior() {
            return 1;
        }
    }

    private class TextViewDrawableAction extends Action {
        int d1;
        int d2;
        int d3;
        int d4;
        boolean drawablesLoaded;
        Icon i1;
        Icon i2;
        Icon i3;
        Icon i4;
        Drawable id1;
        Drawable id2;
        Drawable id3;
        Drawable id4;
        boolean isRelative;
        boolean useIcons;

        public TextViewDrawableAction(int i, boolean z, int i2, int i3, int i4, int i5) {
            super();
            this.isRelative = false;
            this.useIcons = false;
            this.drawablesLoaded = false;
            this.viewId = i;
            this.isRelative = z;
            this.useIcons = false;
            this.d1 = i2;
            this.d2 = i3;
            this.d3 = i4;
            this.d4 = i5;
        }

        public TextViewDrawableAction(int i, boolean z, Icon icon, Icon icon2, Icon icon3, Icon icon4) {
            super();
            this.isRelative = false;
            this.useIcons = false;
            this.drawablesLoaded = false;
            this.viewId = i;
            this.isRelative = z;
            this.useIcons = true;
            this.i1 = icon;
            this.i2 = icon2;
            this.i3 = icon3;
            this.i4 = icon4;
        }

        public TextViewDrawableAction(Parcel parcel) {
            super();
            this.isRelative = false;
            this.useIcons = false;
            this.drawablesLoaded = false;
            this.viewId = parcel.readInt();
            this.isRelative = parcel.readInt() != 0;
            this.useIcons = parcel.readInt() != 0;
            if (this.useIcons) {
                this.i1 = (Icon) parcel.readTypedObject(Icon.CREATOR);
                this.i2 = (Icon) parcel.readTypedObject(Icon.CREATOR);
                this.i3 = (Icon) parcel.readTypedObject(Icon.CREATOR);
                this.i4 = (Icon) parcel.readTypedObject(Icon.CREATOR);
                return;
            }
            this.d1 = parcel.readInt();
            this.d2 = parcel.readInt();
            this.d3 = parcel.readInt();
            this.d4 = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.isRelative ? 1 : 0);
            parcel.writeInt(this.useIcons ? 1 : 0);
            if (this.useIcons) {
                parcel.writeTypedObject(this.i1, 0);
                parcel.writeTypedObject(this.i2, 0);
                parcel.writeTypedObject(this.i3, 0);
                parcel.writeTypedObject(this.i4, 0);
                return;
            }
            parcel.writeInt(this.d1);
            parcel.writeInt(this.d2);
            parcel.writeInt(this.d3);
            parcel.writeInt(this.d4);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) throws Throwable {
            Drawable drawableLoadDrawable;
            Drawable drawableLoadDrawable2;
            Drawable drawableLoadDrawable3;
            TextView textView = (TextView) view.findViewById(this.viewId);
            if (textView == null) {
                return;
            }
            if (this.drawablesLoaded) {
                if (this.isRelative) {
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(this.id1, this.id2, this.id3, this.id4);
                    return;
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(this.id1, this.id2, this.id3, this.id4);
                    return;
                }
            }
            if (this.useIcons) {
                Context context = textView.getContext();
                if (this.i1 != null) {
                    drawableLoadDrawable = this.i1.loadDrawable(context);
                } else {
                    drawableLoadDrawable = null;
                }
                if (this.i2 != null) {
                    drawableLoadDrawable2 = this.i2.loadDrawable(context);
                } else {
                    drawableLoadDrawable2 = null;
                }
                if (this.i3 != null) {
                    drawableLoadDrawable3 = this.i3.loadDrawable(context);
                } else {
                    drawableLoadDrawable3 = null;
                }
                Drawable drawableLoadDrawable4 = this.i4 != null ? this.i4.loadDrawable(context) : null;
                if (this.isRelative) {
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawableLoadDrawable, drawableLoadDrawable2, drawableLoadDrawable3, drawableLoadDrawable4);
                    return;
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(drawableLoadDrawable, drawableLoadDrawable2, drawableLoadDrawable3, drawableLoadDrawable4);
                    return;
                }
            }
            if (this.isRelative) {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(this.d1, this.d2, this.d3, this.d4);
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(this.d1, this.d2, this.d3, this.d4);
            }
        }

        @Override
        public Action initActionAsync(ViewTree viewTree, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            TextViewDrawableAction textViewDrawableAction;
            TextView textView = (TextView) viewTree.findViewById(this.viewId);
            if (textView == null) {
                return RemoteViews.ACTION_NOOP;
            }
            if (this.useIcons) {
                textViewDrawableAction = RemoteViews.this.new TextViewDrawableAction(this.viewId, this.isRelative, this.i1, this.i2, this.i3, this.i4);
            } else {
                textViewDrawableAction = RemoteViews.this.new TextViewDrawableAction(this.viewId, this.isRelative, this.d1, this.d2, this.d3, this.d4);
            }
            textViewDrawableAction.drawablesLoaded = true;
            Context context = textView.getContext();
            if (this.useIcons) {
                textViewDrawableAction.id1 = this.i1 == null ? null : this.i1.loadDrawable(context);
                textViewDrawableAction.id2 = this.i2 == null ? null : this.i2.loadDrawable(context);
                textViewDrawableAction.id3 = this.i3 == null ? null : this.i3.loadDrawable(context);
                textViewDrawableAction.id4 = this.i4 != null ? this.i4.loadDrawable(context) : null;
            } else {
                textViewDrawableAction.id1 = this.d1 == 0 ? null : context.getDrawable(this.d1);
                textViewDrawableAction.id2 = this.d2 == 0 ? null : context.getDrawable(this.d2);
                textViewDrawableAction.id3 = this.d3 == 0 ? null : context.getDrawable(this.d3);
                textViewDrawableAction.id4 = this.d4 != 0 ? context.getDrawable(this.d4) : null;
            }
            return textViewDrawableAction;
        }

        @Override
        public boolean prefersAsyncApply() {
            return this.useIcons;
        }

        @Override
        public int getActionTag() {
            return 11;
        }

        @Override
        public void visitUris(Consumer<Uri> consumer) {
            if (this.useIcons) {
                RemoteViews.visitIconUri(this.i1, consumer);
                RemoteViews.visitIconUri(this.i2, consumer);
                RemoteViews.visitIconUri(this.i3, consumer);
                RemoteViews.visitIconUri(this.i4, consumer);
            }
        }
    }

    private class TextViewSizeAction extends Action {
        float size;
        int units;

        public TextViewSizeAction(int i, int i2, float f) {
            super();
            this.viewId = i;
            this.units = i2;
            this.size = f;
        }

        public TextViewSizeAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.units = parcel.readInt();
            this.size = parcel.readFloat();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.units);
            parcel.writeFloat(this.size);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            TextView textView = (TextView) view.findViewById(this.viewId);
            if (textView == null) {
                return;
            }
            textView.setTextSize(this.units, this.size);
        }

        @Override
        public int getActionTag() {
            return 13;
        }
    }

    private class ViewPaddingAction extends Action {
        int bottom;
        int left;
        int right;
        int top;

        public ViewPaddingAction(int i, int i2, int i3, int i4, int i5) {
            super();
            this.viewId = i;
            this.left = i2;
            this.top = i3;
            this.right = i4;
            this.bottom = i5;
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
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.left);
            parcel.writeInt(this.top);
            parcel.writeInt(this.right);
            parcel.writeInt(this.bottom);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            viewFindViewById.setPadding(this.left, this.top, this.right, this.bottom);
        }

        @Override
        public int getActionTag() {
            return 14;
        }
    }

    private static class LayoutParamAction extends Action {
        public static final int LAYOUT_MARGIN_BOTTOM_DIMEN = 3;
        public static final int LAYOUT_MARGIN_END = 4;
        public static final int LAYOUT_MARGIN_END_DIMEN = 1;
        public static final int LAYOUT_WIDTH = 2;
        final int mProperty;
        final int mValue;

        public LayoutParamAction(int i, int i2, int i3) {
            super();
            this.viewId = i;
            this.mProperty = i2;
            this.mValue = i3;
        }

        public LayoutParamAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.mProperty = parcel.readInt();
            this.mValue = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeInt(this.mProperty);
            parcel.writeInt(this.mValue);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            ViewGroup.LayoutParams layoutParams;
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null || (layoutParams = viewFindViewById.getLayoutParams()) == null) {
                return;
            }
            int iResolveDimenPixelOffset = this.mValue;
            switch (this.mProperty) {
                case 1:
                    iResolveDimenPixelOffset = resolveDimenPixelOffset(viewFindViewById, this.mValue);
                    break;
                case 2:
                    layoutParams.width = this.mValue;
                    viewFindViewById.setLayoutParams(layoutParams);
                    return;
                case 3:
                    if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = resolveDimenPixelOffset(viewFindViewById, this.mValue);
                        viewFindViewById.setLayoutParams(layoutParams);
                        return;
                    }
                    return;
                case 4:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown property " + this.mProperty);
            }
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) layoutParams).setMarginEnd(iResolveDimenPixelOffset);
                viewFindViewById.setLayoutParams(layoutParams);
            }
        }

        private static int resolveDimenPixelOffset(View view, int i) {
            if (i == 0) {
                return 0;
            }
            return view.getContext().getResources().getDimensionPixelOffset(i);
        }

        @Override
        public int getActionTag() {
            return 19;
        }

        @Override
        public String getUniqueKey() {
            return super.getUniqueKey() + this.mProperty;
        }
    }

    private class SetRemoteInputsAction extends Action {
        final Parcelable[] remoteInputs;

        public SetRemoteInputsAction(int i, RemoteInput[] remoteInputArr) {
            super();
            this.viewId = i;
            this.remoteInputs = remoteInputArr;
        }

        public SetRemoteInputsAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.remoteInputs = (Parcelable[]) parcel.createTypedArray(RemoteInput.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.viewId);
            parcel.writeTypedArray(this.remoteInputs, i);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            View viewFindViewById = view.findViewById(this.viewId);
            if (viewFindViewById == null) {
                return;
            }
            viewFindViewById.setTagInternal(R.id.remote_input_tag, this.remoteInputs);
        }

        @Override
        public int getActionTag() {
            return 18;
        }
    }

    private class OverrideTextColorsAction extends Action {
        private final int textColor;

        public OverrideTextColorsAction(int i) {
            super();
            this.textColor = i;
        }

        public OverrideTextColorsAction(Parcel parcel) {
            super();
            this.textColor = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.textColor);
        }

        @Override
        public void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
            Stack stack = new Stack();
            stack.add(view);
            while (!stack.isEmpty()) {
                View view2 = (View) stack.pop();
                if (view2 instanceof TextView) {
                    TextView textView = (TextView) view2;
                    textView.setText(NotificationColorUtil.clearColorSpans(textView.getText()));
                    textView.setTextColor(this.textColor);
                }
                if (view2 instanceof ViewGroup) {
                    ViewGroup viewGroup2 = (ViewGroup) view2;
                    for (int i = 0; i < viewGroup2.getChildCount(); i++) {
                        stack.push(viewGroup2.getChildAt(i));
                    }
                }
            }
        }

        @Override
        public int getActionTag() {
            return 20;
        }
    }

    public RemoteViews(String str, int i) {
        this(getApplicationInfo(str, UserHandle.myUserId()), i);
    }

    public RemoteViews(String str, int i, int i2) {
        this(getApplicationInfo(str, i), i2);
    }

    protected RemoteViews(ApplicationInfo applicationInfo, int i) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mApplication = applicationInfo;
        this.mLayoutId = i;
        this.mBitmapCache = new BitmapCache();
        this.mClassCookies = null;
    }

    private boolean hasLandscapeAndPortraitLayouts() {
        return (this.mLandscape == null || this.mPortrait == null) ? false : true;
    }

    public RemoteViews(RemoteViews remoteViews, RemoteViews remoteViews2) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        if (remoteViews == null || remoteViews2 == null) {
            throw new RuntimeException("Both RemoteViews must be non-null");
        }
        if (!remoteViews.hasSameAppInfo(remoteViews2.mApplication)) {
            throw new RuntimeException("Both RemoteViews must share the same package and user");
        }
        this.mApplication = remoteViews2.mApplication;
        this.mLayoutId = remoteViews2.getLayoutId();
        this.mLandscape = remoteViews;
        this.mPortrait = remoteViews2;
        this.mBitmapCache = new BitmapCache();
        configureRemoteViewsAsChild(remoteViews);
        configureRemoteViewsAsChild(remoteViews2);
        this.mClassCookies = remoteViews2.mClassCookies != null ? remoteViews2.mClassCookies : remoteViews.mClassCookies;
    }

    public RemoteViews(RemoteViews remoteViews) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mBitmapCache = remoteViews.mBitmapCache;
        this.mApplication = remoteViews.mApplication;
        this.mIsRoot = remoteViews.mIsRoot;
        this.mLayoutId = remoteViews.mLayoutId;
        this.mIsWidgetCollectionChild = remoteViews.mIsWidgetCollectionChild;
        this.mReapplyDisallowed = remoteViews.mReapplyDisallowed;
        this.mClassCookies = remoteViews.mClassCookies;
        if (remoteViews.hasLandscapeAndPortraitLayouts()) {
            this.mLandscape = new RemoteViews(remoteViews.mLandscape);
            this.mPortrait = new RemoteViews(remoteViews.mPortrait);
        }
        if (remoteViews.mActions != null) {
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.putClassCookies(this.mClassCookies);
            remoteViews.writeActionsToParcel(parcelObtain);
            parcelObtain.setDataPosition(0);
            readActionsFromParcel(parcelObtain, 0);
            parcelObtain.recycle();
        }
        setBitmapCache(new BitmapCache());
    }

    public RemoteViews(Parcel parcel) {
        this(parcel, null, null, 0, null);
    }

    private RemoteViews(Parcel parcel, BitmapCache bitmapCache, ApplicationInfo applicationInfo, int i, Map<Class, Object> map) {
        boolean z;
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        if (i > 10 && UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new IllegalArgumentException("Too many nested views.");
        }
        int i2 = i + 1;
        int i3 = parcel.readInt();
        if (bitmapCache == null) {
            this.mBitmapCache = new BitmapCache(parcel);
            this.mClassCookies = parcel.copyClassCookies();
        } else {
            setBitmapCache(bitmapCache);
            this.mClassCookies = map;
            setNotRoot();
        }
        if (i3 == 0) {
            this.mApplication = parcel.readInt() != 0 ? ApplicationInfo.CREATOR.createFromParcel(parcel) : applicationInfo;
            this.mLayoutId = parcel.readInt();
            if (parcel.readInt() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.mIsWidgetCollectionChild = z;
            readActionsFromParcel(parcel, i2);
        } else {
            this.mLandscape = new RemoteViews(parcel, this.mBitmapCache, applicationInfo, i2, this.mClassCookies);
            this.mPortrait = new RemoteViews(parcel, this.mBitmapCache, this.mLandscape.mApplication, i2, this.mClassCookies);
            this.mApplication = this.mPortrait.mApplication;
            this.mLayoutId = this.mPortrait.getLayoutId();
        }
        this.mReapplyDisallowed = parcel.readInt() == 0;
    }

    private void readActionsFromParcel(Parcel parcel, int i) {
        int i2 = parcel.readInt();
        if (i2 > 0) {
            this.mActions = new ArrayList<>(i2);
            for (int i3 = 0; i3 < i2; i3++) {
                this.mActions.add(getActionFromParcel(parcel, i));
            }
        }
    }

    private Action getActionFromParcel(Parcel parcel, int i) {
        int i2 = parcel.readInt();
        switch (i2) {
            case 1:
                return new SetOnClickPendingIntent(parcel);
            case 2:
                return new ReflectionAction(parcel);
            case 3:
                return new SetDrawableTint(parcel);
            case 4:
                return new ViewGroupActionAdd(parcel, this.mBitmapCache, this.mApplication, i, this.mClassCookies);
            case 5:
                return new ViewContentNavigation(parcel);
            case 6:
                return new SetEmptyView(parcel);
            case 7:
                return new ViewGroupActionRemove(parcel);
            case 8:
                return new SetPendingIntentTemplate(parcel);
            case 9:
                return new SetOnClickFillInIntent(parcel);
            case 10:
                return new SetRemoteViewsAdapterIntent(parcel);
            case 11:
                return new TextViewDrawableAction(parcel);
            case 12:
                return new BitmapReflectionAction(parcel);
            case 13:
                return new TextViewSizeAction(parcel);
            case 14:
                return new ViewPaddingAction(parcel);
            case 15:
                return new SetRemoteViewsAdapterList(parcel);
            case 16:
            case 17:
            default:
                throw new ActionException("Tag " + i2 + " not found");
            case 18:
                return new SetRemoteInputsAction(parcel);
            case 19:
                return new LayoutParamAction(parcel);
            case 20:
                return new OverrideTextColorsAction(parcel);
        }
    }

    @Override
    @Deprecated
    public RemoteViews mo11clone() {
        Preconditions.checkState(this.mIsRoot, "RemoteView has been attached to another RemoteView. May only clone the root of a RemoteView hierarchy.");
        return new RemoteViews(this);
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

    void setIsWidgetCollectionChild(boolean z) {
        this.mIsWidgetCollectionChild = z;
    }

    private void setBitmapCache(BitmapCache bitmapCache) {
        this.mBitmapCache = bitmapCache;
        if (!hasLandscapeAndPortraitLayouts()) {
            if (this.mActions != null) {
                int size = this.mActions.size();
                for (int i = 0; i < size; i++) {
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
        return this.mBitmapCache.getBitmapMemory();
    }

    private void addAction(Action action) {
        if (hasLandscapeAndPortraitLayouts()) {
            throw new RuntimeException("RemoteViews specifying separate landscape and portrait layouts cannot be modified. Instead, fully configure the landscape and portrait layouts individually before constructing the combined layout.");
        }
        if (this.mActions == null) {
            this.mActions = new ArrayList<>();
        }
        this.mActions.add(action);
    }

    public void addView(int i, RemoteViews remoteViews) {
        Action viewGroupActionAdd;
        if (remoteViews == null) {
            viewGroupActionAdd = new ViewGroupActionRemove(this, i);
        } else {
            viewGroupActionAdd = new ViewGroupActionAdd(this, i, remoteViews);
        }
        addAction(viewGroupActionAdd);
    }

    public void addView(int i, RemoteViews remoteViews, int i2) {
        addAction(new ViewGroupActionAdd(i, remoteViews, i2));
    }

    public void removeAllViews(int i) {
        addAction(new ViewGroupActionRemove(this, i));
    }

    public void removeAllViewsExceptId(int i, int i2) {
        addAction(new ViewGroupActionRemove(i, i2));
    }

    public void showNext(int i) {
        addAction(new ViewContentNavigation(i, true));
    }

    public void showPrevious(int i) {
        addAction(new ViewContentNavigation(i, false));
    }

    public void setDisplayedChild(int i, int i2) {
        setInt(i, "setDisplayedChild", i2);
    }

    public void setViewVisibility(int i, int i2) {
        setInt(i, "setVisibility", i2);
    }

    public void setTextViewText(int i, CharSequence charSequence) {
        setCharSequence(i, "setText", charSequence);
    }

    public void setTextViewTextSize(int i, int i2, float f) {
        addAction(new TextViewSizeAction(i, i2, f));
    }

    public void setTextViewCompoundDrawables(int i, int i2, int i3, int i4, int i5) {
        addAction(new TextViewDrawableAction(i, false, i2, i3, i4, i5));
    }

    public void setTextViewCompoundDrawablesRelative(int i, int i2, int i3, int i4, int i5) {
        addAction(new TextViewDrawableAction(i, true, i2, i3, i4, i5));
    }

    public void setTextViewCompoundDrawables(int i, Icon icon, Icon icon2, Icon icon3, Icon icon4) {
        addAction(new TextViewDrawableAction(i, false, icon, icon2, icon3, icon4));
    }

    public void setTextViewCompoundDrawablesRelative(int i, Icon icon, Icon icon2, Icon icon3, Icon icon4) {
        addAction(new TextViewDrawableAction(i, true, icon, icon2, icon3, icon4));
    }

    public void setImageViewResource(int i, int i2) {
        setInt(i, "setImageResource", i2);
    }

    public void setImageViewUri(int i, Uri uri) {
        setUri(i, "setImageURI", uri);
    }

    public void setImageViewBitmap(int i, Bitmap bitmap) {
        setBitmap(i, "setImageBitmap", bitmap);
    }

    public void setImageViewIcon(int i, Icon icon) {
        setIcon(i, "setImageIcon", icon);
    }

    public void setEmptyView(int i, int i2) {
        addAction(new SetEmptyView(i, i2));
    }

    public void setChronometer(int i, long j, String str, boolean z) {
        setLong(i, "setBase", j);
        setString(i, "setFormat", str);
        setBoolean(i, "setStarted", z);
    }

    public void setChronometerCountDown(int i, boolean z) {
        setBoolean(i, "setCountDown", z);
    }

    public void setProgressBar(int i, int i2, int i3, boolean z) {
        setBoolean(i, "setIndeterminate", z);
        if (!z) {
            setInt(i, "setMax", i2);
            setInt(i, "setProgress", i3);
        }
    }

    public void setOnClickPendingIntent(int i, PendingIntent pendingIntent) {
        addAction(new SetOnClickPendingIntent(i, pendingIntent));
    }

    public void setPendingIntentTemplate(int i, PendingIntent pendingIntent) {
        addAction(new SetPendingIntentTemplate(i, pendingIntent));
    }

    public void setOnClickFillInIntent(int i, Intent intent) {
        addAction(new SetOnClickFillInIntent(i, intent));
    }

    public void setDrawableTint(int i, boolean z, int i2, PorterDuff.Mode mode) {
        addAction(new SetDrawableTint(i, z, i2, mode));
    }

    public void setProgressTintList(int i, ColorStateList colorStateList) {
        addAction(new ReflectionAction(i, "setProgressTintList", 15, colorStateList));
    }

    public void setProgressBackgroundTintList(int i, ColorStateList colorStateList) {
        addAction(new ReflectionAction(i, "setProgressBackgroundTintList", 15, colorStateList));
    }

    public void setProgressIndeterminateTintList(int i, ColorStateList colorStateList) {
        addAction(new ReflectionAction(i, "setIndeterminateTintList", 15, colorStateList));
    }

    public void setTextColor(int i, int i2) {
        setInt(i, "setTextColor", i2);
    }

    public void setTextColor(int i, ColorStateList colorStateList) {
        addAction(new ReflectionAction(i, "setTextColor", 15, colorStateList));
    }

    @Deprecated
    public void setRemoteAdapter(int i, int i2, Intent intent) {
        setRemoteAdapter(i2, intent);
    }

    public void setRemoteAdapter(int i, Intent intent) {
        addAction(new SetRemoteViewsAdapterIntent(i, intent));
    }

    public void setRemoteAdapter(int i, ArrayList<RemoteViews> arrayList, int i2) {
        addAction(new SetRemoteViewsAdapterList(i, arrayList, i2));
    }

    public void setScrollPosition(int i, int i2) {
        setInt(i, "smoothScrollToPosition", i2);
    }

    public void setRelativeScrollPosition(int i, int i2) {
        setInt(i, "smoothScrollByOffset", i2);
    }

    public void setViewPadding(int i, int i2, int i3, int i4, int i5) {
        addAction(new ViewPaddingAction(i, i2, i3, i4, i5));
    }

    public void setViewLayoutMarginEndDimen(int i, int i2) {
        addAction(new LayoutParamAction(i, 1, i2));
    }

    public void setViewLayoutMarginEnd(int i, int i2) {
        addAction(new LayoutParamAction(i, 4, i2));
    }

    public void setViewLayoutMarginBottomDimen(int i, int i2) {
        addAction(new LayoutParamAction(i, 3, i2));
    }

    public void setViewLayoutWidth(int i, int i2) {
        if (i2 != 0 && i2 != -1 && i2 != -2) {
            throw new IllegalArgumentException("Only supports 0, WRAP_CONTENT and MATCH_PARENT");
        }
        this.mActions.add(new LayoutParamAction(i, 2, i2));
    }

    public void setBoolean(int i, String str, boolean z) {
        addAction(new ReflectionAction(i, str, 1, Boolean.valueOf(z)));
    }

    public void setByte(int i, String str, byte b) {
        addAction(new ReflectionAction(i, str, 2, Byte.valueOf(b)));
    }

    public void setShort(int i, String str, short s) {
        addAction(new ReflectionAction(i, str, 3, Short.valueOf(s)));
    }

    public void setInt(int i, String str, int i2) {
        addAction(new ReflectionAction(i, str, 4, Integer.valueOf(i2)));
    }

    public void setColorStateList(int i, String str, ColorStateList colorStateList) {
        addAction(new ReflectionAction(i, str, 15, colorStateList));
    }

    public void setLong(int i, String str, long j) {
        addAction(new ReflectionAction(i, str, 5, Long.valueOf(j)));
    }

    public void setFloat(int i, String str, float f) {
        addAction(new ReflectionAction(i, str, 6, Float.valueOf(f)));
    }

    public void setDouble(int i, String str, double d) {
        addAction(new ReflectionAction(i, str, 7, Double.valueOf(d)));
    }

    public void setChar(int i, String str, char c) {
        addAction(new ReflectionAction(i, str, 8, Character.valueOf(c)));
    }

    public void setString(int i, String str, String str2) {
        addAction(new ReflectionAction(i, str, 9, str2));
    }

    public void setCharSequence(int i, String str, CharSequence charSequence) {
        addAction(new ReflectionAction(i, str, 10, charSequence));
    }

    public void setUri(int i, String str, Uri uri) {
        if (uri != null) {
            uri = uri.getCanonicalUri();
            if (StrictMode.vmFileUriExposureEnabled()) {
                uri.checkFileUriExposed("RemoteViews.setUri()");
            }
        }
        addAction(new ReflectionAction(i, str, 11, uri));
    }

    public void setBitmap(int i, String str, Bitmap bitmap) {
        addAction(new BitmapReflectionAction(i, str, bitmap));
    }

    public void setBundle(int i, String str, Bundle bundle) {
        addAction(new ReflectionAction(i, str, 13, bundle));
    }

    public void setIntent(int i, String str, Intent intent) {
        addAction(new ReflectionAction(i, str, 14, intent));
    }

    public void setIcon(int i, String str, Icon icon) {
        addAction(new ReflectionAction(i, str, 16, icon));
    }

    public void setContentDescription(int i, CharSequence charSequence) {
        setCharSequence(i, "setContentDescription", charSequence);
    }

    public void setAccessibilityTraversalBefore(int i, int i2) {
        setInt(i, "setAccessibilityTraversalBefore", i2);
    }

    public void setAccessibilityTraversalAfter(int i, int i2) {
        setInt(i, "setAccessibilityTraversalAfter", i2);
    }

    public void setLabelFor(int i, int i2) {
        setInt(i, "setLabelFor", i2);
    }

    private RemoteViews getRemoteViewsToApply(Context context) {
        if (hasLandscapeAndPortraitLayouts()) {
            if (context.getResources().getConfiguration().orientation == 2) {
                return this.mLandscape;
            }
            return this.mPortrait;
        }
        return this;
    }

    public void setApplyTheme(int i) {
        this.mApplyThemeResId = i;
    }

    public View apply(Context context, ViewGroup viewGroup) {
        return apply(context, viewGroup, null);
    }

    public View apply(Context context, ViewGroup viewGroup, OnClickHandler onClickHandler) {
        RemoteViews remoteViewsToApply = getRemoteViewsToApply(context);
        View viewInflateView = inflateView(context, remoteViewsToApply, viewGroup);
        loadTransitionOverride(context, onClickHandler);
        remoteViewsToApply.performApply(viewInflateView, viewGroup, onClickHandler);
        return viewInflateView;
    }

    private View inflateView(Context context, RemoteViews remoteViews, ViewGroup viewGroup) {
        Context contextThemeWrapper;
        RemoteViewsContextWrapper remoteViewsContextWrapper = new RemoteViewsContextWrapper(context, getContextForResources(context));
        if (this.mApplyThemeResId != 0) {
            contextThemeWrapper = new ContextThemeWrapper(remoteViewsContextWrapper, this.mApplyThemeResId);
        } else {
            contextThemeWrapper = remoteViewsContextWrapper;
        }
        LayoutInflater layoutInflaterCloneInContext = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).cloneInContext(contextThemeWrapper);
        layoutInflaterCloneInContext.setFilter(this);
        View viewInflate = layoutInflaterCloneInContext.inflate(remoteViews.getLayoutId(), viewGroup, false);
        viewInflate.setTagInternal(16908312, Integer.valueOf(remoteViews.getLayoutId()));
        return viewInflate;
    }

    private static void loadTransitionOverride(Context context, OnClickHandler onClickHandler) {
        if (onClickHandler != null && context.getResources().getBoolean(R.bool.config_overrideRemoteViewsActivityTransition)) {
            TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(R.styleable.Window);
            TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(typedArrayObtainStyledAttributes.getResourceId(8, 0), R.styleable.WindowAnimation);
            onClickHandler.setEnterAnimationId(typedArrayObtainStyledAttributes2.getResourceId(26, 0));
            typedArrayObtainStyledAttributes.recycle();
            typedArrayObtainStyledAttributes2.recycle();
        }
    }

    public CancellationSignal applyAsync(Context context, ViewGroup viewGroup, Executor executor, OnViewAppliedListener onViewAppliedListener) {
        return applyAsync(context, viewGroup, executor, onViewAppliedListener, null);
    }

    private CancellationSignal startTaskOnExecutor(AsyncApplyTask asyncApplyTask, Executor executor) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(asyncApplyTask);
        if (executor == null) {
            executor = AsyncTask.THREAD_POOL_EXECUTOR;
        }
        asyncApplyTask.executeOnExecutor(executor, new Void[0]);
        return cancellationSignal;
    }

    public CancellationSignal applyAsync(Context context, ViewGroup viewGroup, Executor executor, OnViewAppliedListener onViewAppliedListener, OnClickHandler onClickHandler) {
        return startTaskOnExecutor(getAsyncApplyTask(context, viewGroup, onViewAppliedListener, onClickHandler), executor);
    }

    private AsyncApplyTask getAsyncApplyTask(Context context, ViewGroup viewGroup, OnViewAppliedListener onViewAppliedListener, OnClickHandler onClickHandler) {
        return new AsyncApplyTask(getRemoteViewsToApply(context), viewGroup, context, onViewAppliedListener, onClickHandler, null);
    }

    private class AsyncApplyTask extends AsyncTask<Void, Void, ViewTree> implements CancellationSignal.OnCancelListener {
        private Action[] mActions;
        final Context mContext;
        private Exception mError;
        final OnClickHandler mHandler;
        final OnViewAppliedListener mListener;
        final ViewGroup mParent;
        final RemoteViews mRV;
        private View mResult;
        private ViewTree mTree;

        private AsyncApplyTask(RemoteViews remoteViews, ViewGroup viewGroup, Context context, OnViewAppliedListener onViewAppliedListener, OnClickHandler onClickHandler, View view) {
            this.mRV = remoteViews;
            this.mParent = viewGroup;
            this.mContext = context;
            this.mListener = onViewAppliedListener;
            this.mHandler = onClickHandler;
            this.mResult = view;
            RemoteViews.loadTransitionOverride(context, onClickHandler);
        }

        @Override
        protected ViewTree doInBackground(Void... voidArr) {
            try {
                if (this.mResult == null) {
                    this.mResult = RemoteViews.this.inflateView(this.mContext, this.mRV, this.mParent);
                }
                this.mTree = new ViewTree(this.mResult);
                if (this.mRV.mActions != null) {
                    int size = this.mRV.mActions.size();
                    this.mActions = new Action[size];
                    for (int i = 0; i < size && !isCancelled(); i++) {
                        this.mActions[i] = ((Action) this.mRV.mActions.get(i)).initActionAsync(this.mTree, this.mParent, this.mHandler);
                    }
                } else {
                    this.mActions = null;
                }
                return this.mTree;
            } catch (Exception e) {
                this.mError = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ViewTree viewTree) {
            if (this.mError == null) {
                try {
                    if (this.mActions != null) {
                        OnClickHandler onClickHandler = this.mHandler == null ? RemoteViews.DEFAULT_ON_CLICK_HANDLER : this.mHandler;
                        for (Action action : this.mActions) {
                            action.apply(viewTree.mRoot, this.mParent, onClickHandler);
                        }
                    }
                } catch (Exception e) {
                    this.mError = e;
                }
            }
            if (this.mListener != null) {
                if (this.mError != null) {
                    this.mListener.onError(this.mError);
                    return;
                } else {
                    this.mListener.onViewApplied(viewTree.mRoot);
                    return;
                }
            }
            if (this.mError != null) {
                if (this.mError instanceof ActionException) {
                    throw ((ActionException) this.mError);
                }
                throw new ActionException(this.mError);
            }
        }

        @Override
        public void onCancel() {
            cancel(true);
        }
    }

    public void reapply(Context context, View view) {
        reapply(context, view, null);
    }

    public void reapply(Context context, View view, OnClickHandler onClickHandler) {
        RemoteViews remoteViewsToApply = getRemoteViewsToApply(context);
        if (hasLandscapeAndPortraitLayouts() && ((Integer) view.getTag(16908312)).intValue() != remoteViewsToApply.getLayoutId()) {
            throw new RuntimeException("Attempting to re-apply RemoteViews to a view that that does not share the same root layout id.");
        }
        remoteViewsToApply.performApply(view, (ViewGroup) view.getParent(), onClickHandler);
    }

    public CancellationSignal reapplyAsync(Context context, View view, Executor executor, OnViewAppliedListener onViewAppliedListener) {
        return reapplyAsync(context, view, executor, onViewAppliedListener, null);
    }

    public CancellationSignal reapplyAsync(Context context, View view, Executor executor, OnViewAppliedListener onViewAppliedListener, OnClickHandler onClickHandler) {
        RemoteViews remoteViewsToApply = getRemoteViewsToApply(context);
        if (hasLandscapeAndPortraitLayouts() && ((Integer) view.getTag(16908312)).intValue() != remoteViewsToApply.getLayoutId()) {
            throw new RuntimeException("Attempting to re-apply RemoteViews to a view that that does not share the same root layout id.");
        }
        return startTaskOnExecutor(new AsyncApplyTask(remoteViewsToApply, (ViewGroup) view.getParent(), context, onViewAppliedListener, onClickHandler, view), executor);
    }

    private void performApply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) {
        if (this.mActions != null) {
            if (onClickHandler == null) {
                onClickHandler = DEFAULT_ON_CLICK_HANDLER;
            }
            int size = this.mActions.size();
            for (int i = 0; i < size; i++) {
                this.mActions.get(i).apply(view, viewGroup, onClickHandler);
            }
        }
    }

    public boolean prefersAsyncApply() {
        if (this.mActions != null) {
            int size = this.mActions.size();
            for (int i = 0; i < size; i++) {
                if (this.mActions.get(i).prefersAsyncApply()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Context getContextForResources(Context context) {
        if (this.mApplication != null) {
            if (context.getUserId() == UserHandle.getUserId(this.mApplication.uid) && context.getPackageName().equals(this.mApplication.packageName)) {
                return context;
            }
            try {
                return context.createApplicationContext(this.mApplication, 4);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "Package name " + this.mApplication.packageName + " not found");
            }
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
    public boolean onLoadClass(Class cls) {
        return cls.isAnnotationPresent(RemoteView.class);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (!hasLandscapeAndPortraitLayouts()) {
            parcel.writeInt(0);
            if (this.mIsRoot) {
                this.mBitmapCache.writeBitmapsToParcel(parcel, i);
            }
            if (!this.mIsRoot && (i & 2) != 0) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                this.mApplication.writeToParcel(parcel, i);
            }
            parcel.writeInt(this.mLayoutId);
            parcel.writeInt(this.mIsWidgetCollectionChild ? 1 : 0);
            writeActionsToParcel(parcel);
        } else {
            parcel.writeInt(1);
            if (this.mIsRoot) {
                this.mBitmapCache.writeBitmapsToParcel(parcel, i);
            }
            this.mLandscape.writeToParcel(parcel, i);
            this.mPortrait.writeToParcel(parcel, i | 2);
        }
        parcel.writeInt(this.mReapplyDisallowed ? 1 : 0);
    }

    private void writeActionsToParcel(Parcel parcel) {
        int size;
        int i;
        if (this.mActions != null) {
            size = this.mActions.size();
        } else {
            size = 0;
        }
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            Action action = this.mActions.get(i2);
            parcel.writeInt(action.getActionTag());
            if (!action.hasSameAppInfo(this.mApplication)) {
                i = 0;
            } else {
                i = 2;
            }
            action.writeToParcel(parcel, i);
        }
    }

    private static ApplicationInfo getApplicationInfo(String str, int i) {
        if (str == null) {
            return null;
        }
        Application applicationCurrentApplication = ActivityThread.currentApplication();
        if (applicationCurrentApplication == null) {
            throw new IllegalStateException("Cannot create remote views out of an aplication.");
        }
        ApplicationInfo applicationInfo = applicationCurrentApplication.getApplicationInfo();
        if (UserHandle.getUserId(applicationInfo.uid) != i || !applicationInfo.packageName.equals(str)) {
            try {
                return applicationCurrentApplication.getBaseContext().createPackageContextAsUser(str, 0, new UserHandle(i)).getApplicationInfo();
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("No such package " + str);
            }
        }
        return applicationInfo;
    }

    public boolean hasSameAppInfo(ApplicationInfo applicationInfo) {
        return this.mApplication.packageName.equals(applicationInfo.packageName) && this.mApplication.uid == applicationInfo.uid;
    }

    private static class ViewTree {
        private static final int INSERT_AT_END_INDEX = -1;
        private ArrayList<ViewTree> mChildren;
        private View mRoot;

        private ViewTree(View view) {
            this.mRoot = view;
        }

        public void createTree() {
            if (this.mChildren != null) {
                return;
            }
            this.mChildren = new ArrayList<>();
            if (this.mRoot instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) this.mRoot;
                int childCount = viewGroup.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    addViewChild(viewGroup.getChildAt(i));
                }
            }
        }

        public ViewTree findViewTreeById(int i) {
            if (this.mRoot.getId() == i) {
                return this;
            }
            if (this.mChildren == null) {
                return null;
            }
            Iterator<ViewTree> it = this.mChildren.iterator();
            while (it.hasNext()) {
                ViewTree viewTreeFindViewTreeById = it.next().findViewTreeById(i);
                if (viewTreeFindViewTreeById != null) {
                    return viewTreeFindViewTreeById;
                }
            }
            return null;
        }

        public void replaceView(View view) {
            this.mRoot = view;
            this.mChildren = null;
            createTree();
        }

        public <T extends View> T findViewById(int i) {
            if (this.mChildren == null) {
                return (T) this.mRoot.findViewById(i);
            }
            ViewTree viewTreeFindViewTreeById = findViewTreeById(i);
            if (viewTreeFindViewTreeById == null) {
                return null;
            }
            return (T) viewTreeFindViewTreeById.mRoot;
        }

        public void addChild(ViewTree viewTree) {
            addChild(viewTree, -1);
        }

        public void addChild(ViewTree viewTree, int i) {
            if (this.mChildren == null) {
                this.mChildren = new ArrayList<>();
            }
            viewTree.createTree();
            if (i == -1) {
                this.mChildren.add(viewTree);
            } else {
                this.mChildren.add(i, viewTree);
            }
        }

        private void addViewChild(View view) {
            ViewTree viewTree;
            if (view.isRootNamespace()) {
                return;
            }
            if (view.getId() != 0) {
                viewTree = new ViewTree(view);
                this.mChildren.add(viewTree);
            } else {
                viewTree = this;
            }
            if ((view instanceof ViewGroup) && viewTree.mChildren == null) {
                viewTree.mChildren = new ArrayList<>();
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    viewTree.addViewChild(viewGroup.getChildAt(i));
                }
            }
        }
    }
}
