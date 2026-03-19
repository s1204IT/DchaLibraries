package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.ConfigMessageParams;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;

public class ConfigurationClientModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "ConfigurationClientModel";

    public ConfigurationClientModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 4);
    }

    @Override
    public void setConfigMessageHeader(int i, int i2, int i3, int i4, int i5) {
        Log.d(TAG, "setConfigMessageHeader");
        super.setConfigMessageHeader(i, i2, i3, i4, i5);
    }

    public void configBeaconGet() {
        Log.d(TAG, "configBeaconGet");
        super.modelSendConfigMessage();
    }

    public void configBeaconSet(ConfigMessageParams configMessageParams) {
    }

    public void configCompositionDataGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configCompositionDataGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configDefaultTTLGet() {
        Log.d(TAG, "configDefaultTTLGet");
        super.modelSendConfigMessage();
    }

    public void configDefaultTTLSet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configDefaultTTLSet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configGattProxyGet() {
        Log.d(TAG, "configGattProxyGet");
        super.modelSendConfigMessage();
    }

    public void configGattProxySet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configGattProxySet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configFriendGet() {
        Log.d(TAG, "configFriendGet");
        super.modelSendConfigMessage();
    }

    public void configFriendSet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configFriendSet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configRelayGet() {
        Log.d(TAG, "configRelayGet");
        super.modelSendConfigMessage();
    }

    public void configRelaySet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configRelaySet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelPubGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelPubGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelPubSet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelPubSet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelSubAdd(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelSubAdd");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelSubDel(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelSubDel");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelSubOw(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelSubOw");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelSubDelAll(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelSubDelAll");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configSigModelSubGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configSigModelSubGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configVendorModelSubGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configVendorModelSubGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNetkeyAdd(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configNetkeyAdd");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNetkeyUpdate(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configNetkeyUpdate");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNetkeyDel(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configNetkeyDel");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNetkeyGet() {
        Log.d(TAG, "configNetkeyGet");
        super.modelSendConfigMessage();
    }

    public void configAppkeyAdd(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configAppkeyAdd");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configAppkeyUpdate(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configAppkeyUpdate");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configAppkeyDel(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configAppkeyDel");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configAppkeyGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configAppkeyGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelAppBind(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelAppBind");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configModelAppUnbind(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configModelAppUnbind");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configSigModelAppGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configSigModelAppGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configVendorModelAppGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configVendorModelAppGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNodeIdentityGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configNodeIdentityGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNodeIdentitySet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configNodeIdentitySet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNodeReset() {
        Log.d(TAG, "configNodeReset");
        super.modelSendConfigMessage();
    }

    public void configKeyRefreshPhaseGet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configKeyRefreshPhaseGet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configKeyRefreshPhaseSet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configKeyRefreshPhaseSet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configHbPubGet() {
        Log.d(TAG, "configHbPubGet");
        super.modelSendConfigMessage();
    }

    public void configHbPubSet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configHbPubSet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configHbSubGet() {
        Log.d(TAG, "configHbSubGet");
        super.modelSendConfigMessage();
    }

    public void configHbSubSet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configHbSubSet");
        super.modelSendConfigMessage(configMessageParams);
    }

    public void configNetworkTransmitGet() {
        Log.d(TAG, "configNetworkTransmitGet");
        super.modelSendConfigMessage();
    }

    public void configNetworkTransmitSet(ConfigMessageParams configMessageParams) {
        Log.d(TAG, "configNetworkTransmitSet");
        super.modelSendConfigMessage(configMessageParams);
    }
}
