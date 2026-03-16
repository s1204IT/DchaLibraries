package android.net;

import android.content.Context;
import android.net.SamplingDataTracker;
import android.os.Handler;
import android.os.Messenger;
import com.android.internal.util.Preconditions;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseNetworkStateTracker implements NetworkStateTracker {
    public static final String PROP_TCP_BUFFER_UNKNOWN = "net.tcp.buffersize.unknown";
    public static final String PROP_TCP_BUFFER_WIFI = "net.tcp.buffersize.wifi";
    protected Context mContext;
    private AtomicBoolean mDefaultRouteSet;
    protected LinkProperties mLinkProperties;
    protected Network mNetwork;
    protected NetworkCapabilities mNetworkCapabilities;
    protected NetworkInfo mNetworkInfo;
    private AtomicBoolean mPrivateDnsRouteSet;
    private Handler mTarget;
    private AtomicBoolean mTeardownRequested;

    public BaseNetworkStateTracker(int networkType) {
        this.mNetwork = new Network(0);
        this.mTeardownRequested = new AtomicBoolean(false);
        this.mPrivateDnsRouteSet = new AtomicBoolean(false);
        this.mDefaultRouteSet = new AtomicBoolean(false);
        this.mNetworkInfo = new NetworkInfo(networkType, -1, ConnectivityManager.getNetworkTypeName(networkType), null);
        this.mLinkProperties = new LinkProperties();
        this.mNetworkCapabilities = new NetworkCapabilities();
    }

    protected BaseNetworkStateTracker() {
        this.mNetwork = new Network(0);
        this.mTeardownRequested = new AtomicBoolean(false);
        this.mPrivateDnsRouteSet = new AtomicBoolean(false);
        this.mDefaultRouteSet = new AtomicBoolean(false);
    }

    @Deprecated
    protected Handler getTargetHandler() {
        return this.mTarget;
    }

    protected final void dispatchStateChanged() {
        this.mTarget.obtainMessage(458752, getNetworkInfo()).sendToTarget();
    }

    protected final void dispatchConfigurationChanged() {
        this.mTarget.obtainMessage(NetworkStateTracker.EVENT_CONFIGURATION_CHANGED, getNetworkInfo()).sendToTarget();
    }

    @Override
    public void startMonitoring(Context context, Handler target) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mTarget = (Handler) Preconditions.checkNotNull(target);
        startMonitoringInternal();
    }

    protected void startMonitoringInternal() {
    }

    @Override
    public NetworkInfo getNetworkInfo() {
        return new NetworkInfo(this.mNetworkInfo);
    }

    @Override
    public LinkProperties getLinkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    @Override
    public NetworkCapabilities getNetworkCapabilities() {
        return new NetworkCapabilities(this.mNetworkCapabilities);
    }

    @Override
    public LinkQualityInfo getLinkQualityInfo() {
        return null;
    }

    @Override
    public void captivePortalCheckCompleted(boolean isCaptivePortal) {
    }

    @Override
    public boolean setRadio(boolean turnOn) {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return this.mNetworkInfo.isAvailable();
    }

    @Override
    public void setUserDataEnable(boolean enabled) {
    }

    @Override
    public void setPolicyDataEnable(boolean enabled) {
    }

    @Override
    public boolean isPrivateDnsRouteSet() {
        return this.mPrivateDnsRouteSet.get();
    }

    @Override
    public void privateDnsRouteSet(boolean enabled) {
        this.mPrivateDnsRouteSet.set(enabled);
    }

    @Override
    public boolean isDefaultRouteSet() {
        return this.mDefaultRouteSet.get();
    }

    @Override
    public void defaultRouteSet(boolean enabled) {
        this.mDefaultRouteSet.set(enabled);
    }

    @Override
    public boolean isTeardownRequested() {
        return this.mTeardownRequested.get();
    }

    @Override
    public void setTeardownRequested(boolean isRequested) {
        this.mTeardownRequested.set(isRequested);
    }

    @Override
    public void setDependencyMet(boolean met) {
    }

    @Override
    public void supplyMessenger(Messenger messenger) {
    }

    @Override
    public String getNetworkInterfaceName() {
        if (this.mLinkProperties != null) {
            return this.mLinkProperties.getInterfaceName();
        }
        return null;
    }

    @Override
    public void startSampling(SamplingDataTracker.SamplingSnapshot s) {
    }

    @Override
    public void stopSampling(SamplingDataTracker.SamplingSnapshot s) {
    }

    @Override
    public void setNetId(int netId) {
        this.mNetwork = new Network(netId);
    }

    @Override
    public Network getNetwork() {
        return this.mNetwork;
    }
}
