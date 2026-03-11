package android.support.v4.app;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.v4.util.Preconditions;
import android.view.LayoutInflater;
import android.view.View;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class FragmentHostCallback<E> extends FragmentContainer {
    private final Activity mActivity;
    private final Context mContext;
    final FragmentManagerImpl mFragmentManager;
    private final Handler mHandler;
    private final int mWindowAnimations;

    FragmentHostCallback(Activity activity, Context context, Handler handler, int i) {
        this.mFragmentManager = new FragmentManagerImpl();
        this.mActivity = activity;
        this.mContext = (Context) Preconditions.checkNotNull(context, "context == null");
        this.mHandler = (Handler) Preconditions.checkNotNull(handler, "handler == null");
        this.mWindowAnimations = i;
    }

    FragmentHostCallback(FragmentActivity fragmentActivity) {
        this(fragmentActivity, fragmentActivity, fragmentActivity.mHandler, 0);
    }

    Activity getActivity() {
        return this.mActivity;
    }

    Context getContext() {
        return this.mContext;
    }

    FragmentManagerImpl getFragmentManagerImpl() {
        return this.mFragmentManager;
    }

    Handler getHandler() {
        return this.mHandler;
    }

    void onAttachFragment(Fragment fragment) {
    }

    public void onDump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    @Override
    public View onFindViewById(int i) {
        return null;
    }

    public LayoutInflater onGetLayoutInflater() {
        return LayoutInflater.from(this.mContext);
    }

    public int onGetWindowAnimations() {
        return this.mWindowAnimations;
    }

    @Override
    public boolean onHasView() {
        return true;
    }

    public boolean onHasWindowAnimations() {
        return true;
    }

    public boolean onShouldSaveFragmentState(Fragment fragment) {
        return true;
    }

    public void onSupportInvalidateOptionsMenu() {
    }
}
