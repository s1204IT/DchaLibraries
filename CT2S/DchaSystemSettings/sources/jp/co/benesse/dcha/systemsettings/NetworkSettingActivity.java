package jp.co.benesse.dcha.systemsettings;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.security.KeyStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import jp.co.benesse.dcha.util.Logger;

public class NetworkSettingActivity extends ParentSettingActivity implements DialogInterface.OnClickListener, View.OnClickListener, View.OnTouchListener, AdapterView.OnItemClickListener {
    private int apSize;
    private boolean downEnableFlg;
    private boolean eventStopFlg;
    private int locationY;
    private ImageView mAddNetworkBtn;
    private ListView mApList;
    private ImageView mBackBtn;
    private WifiManager.ActionListener mConnectListener;
    private WifiDialog mDialog;
    private final IntentFilter mFilter;
    private WifiManager.ActionListener mForgetListener;
    private WifiInfo mLastInfo;
    private NetworkInfo.DetailedState mLastState;
    private int mListTouchActionMove;
    private String mMacAddress;
    private TextView mMacAddressTextView;
    private String mOnAccessSsid;
    private ImageView mPankuzu;
    private WifiManager.ActionListener mSaveListener;
    private ImageView mScrollAboveBtn;
    private ImageView mScrollBaseNetwork;
    private ImageView mScrollBenethBtn;
    private RelativeLayout mScrollHandleLayout;
    private ImageView mScrollHandleNetwork;
    private AccessPoint mSelectedAccessPoint;
    private float mTouchBeforeX;
    private float mTouchBeforeY;
    private float mTouchDiffBeforeActionY;
    private WifiManager mWifiManager;
    private int scrollAbleAmountOfList;
    private int scrollAmountBarOfOneClick;
    private boolean upEnableFlg;
    private final String TAG = "NetworkSettingActivity";
    private boolean mBackBtnfree = true;
    private int mKeyStoreNetworkId = -1;
    private AtomicBoolean mConnected = new AtomicBoolean(false);
    private int mMoveCountJudgingTouch = 4;
    private final int scrollAbleAmountOfBar = 173;
    private final int heightOfElement = 92;
    private final int HeightOfListView = 337;
    private final int scrollAmountListOfOneClick = 65;
    private final int aboveOffset = 86;
    private final int benethOffset = -87;
    private final int scrollBarWidth = 40;
    private final int scrollBarHeight = 60;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d("NetworkSettingActivity", "onReceive 0001");
            NetworkSettingActivity.this.handleEvent(context, intent);
            Logger.d("NetworkSettingActivity", "onReceive 0002");
        }
    };

    public NetworkSettingActivity() {
        Logger.d("NetworkSettingActivity", "NetworkSettingActivity 0001");
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mFilter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
        this.mFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.RSSI_CHANGED");
        Logger.d("NetworkSettingActivity", "NetworkSettingActivity 0002");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("NetworkSettingActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_network);
        this.mApList = (ListView) findViewById(R.id.wifinetwork_list);
        this.mApList.setScrollingCacheEnabled(false);
        this.mScrollHandleLayout = (RelativeLayout) findViewById(R.id.scroll_handle_layout);
        this.mMacAddressTextView = (TextView) findViewById(R.id.mac_address);
        setFont(this.mMacAddressTextView);
        this.mAddNetworkBtn = (ImageView) findViewById(R.id.add_network_btn);
        this.mScrollBaseNetwork = (ImageView) findViewById(R.id.scroll_base_network);
        this.mScrollAboveBtn = (ImageView) findViewById(R.id.wifinetwork_scroll_above_btn);
        this.mScrollBenethBtn = (ImageView) findViewById(R.id.wifinetwork_scroll_beneth_btn);
        this.mScrollHandleNetwork = (ImageView) findViewById(R.id.scroll_handle_network);
        this.mBackBtn = (ImageView) findViewById(R.id.back_btn);
        this.mPankuzu = (ImageView) findViewById(R.id.pankuzu);
        this.mAddNetworkBtn.setOnClickListener(this);
        this.mScrollAboveBtn.setOnClickListener(this);
        this.mScrollBenethBtn.setOnClickListener(this);
        this.mBackBtn.setOnClickListener(this);
        this.mScrollHandleNetwork.setOnTouchListener(this);
        this.mApList.setOnTouchListener(this);
        this.mScrollHandleNetwork.scrollTo(0, 86);
        this.mIsFirstFlow = getFirstFlg();
        if (!this.mIsFirstFlow) {
            Logger.d("NetworkSettingActivity", "onCreate 0002");
            this.mPankuzu.setVisibility(4);
        } else {
            Logger.d("NetworkSettingActivity", "onCreate 0003");
            getWindow().addFlags(128);
        }
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mWifiManager.setWifiEnabled(true);
        this.mLastInfo = this.mWifiManager.getConnectionInfo();
        this.mOnAccessSsid = this.mLastInfo.getSSID();
        this.mMacAddress = this.mLastInfo.getMacAddress();
        this.mConnectListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                Logger.d("NetworkSettingActivity", "onSuccess 0001");
            }

            public void onFailure(int reason) {
                Logger.d("NetworkSettingActivity", "onFailure 0001");
            }
        };
        this.mSaveListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                Logger.d("NetworkSettingActivity", "onSuccess 0002");
            }

            public void onFailure(int reason) {
                Logger.d("NetworkSettingActivity", "onFailure 0002");
            }
        };
        this.mForgetListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                Logger.d("NetworkSettingActivity", "onSuccess 0003");
            }

            public void onFailure(int reason) {
                Logger.d("NetworkSettingActivity", "onFailure 0003");
            }
        };
        Logger.d("NetworkSettingActivity", "onCreate 0004");
    }

    @Override
    protected void onStart() {
        Logger.d("NetworkSettingActivity", "onStart 0001");
        super.onStart();
        registerReceiver(this.mReceiver, this.mFilter);
        Logger.d("NetworkSettingActivity", "onStart 0002");
    }

    @Override
    protected void onResume() {
        Logger.d("NetworkSettingActivity", "onResume 0001");
        super.onResume();
        this.mMacAddressTextView.setText(this.mMacAddress);
        if (this.mKeyStoreNetworkId != -1 && KeyStore.getInstance().state() == KeyStore.State.UNLOCKED) {
            Logger.d("NetworkSettingActivity", "onResume 0002");
            this.mWifiManager.connect(this.mKeyStoreNetworkId, this.mConnectListener);
        }
        this.mKeyStoreNetworkId = -1;
        updateAccessPoints();
        Logger.d("NetworkSettingActivity", "onResume 0003");
    }

    @Override
    protected void onStop() {
        Logger.d("NetworkSettingActivity", "onStop 0001");
        super.onStop();
        if (this.mReceiver != null) {
            Logger.d("NetworkSettingActivity", "onStop 0002");
            unregisterReceiver(this.mReceiver);
        }
        Logger.d("NetworkSettingActivity", "onStop 0003");
    }

    @Override
    protected void onDestroy() {
        Logger.d("NetworkSettingActivity", "onDestroy 0001");
        super.onDestroy();
        this.mAddNetworkBtn.setOnClickListener(null);
        this.mScrollAboveBtn.setOnClickListener(null);
        this.mScrollBenethBtn.setOnClickListener(null);
        this.mBackBtn.setOnClickListener(null);
        this.mApList = null;
        this.mScrollHandleLayout = null;
        this.mMacAddressTextView = null;
        this.mAddNetworkBtn = null;
        this.mScrollBaseNetwork = null;
        this.mScrollAboveBtn = null;
        this.mScrollBenethBtn = null;
        this.mBackBtn = null;
        this.mPankuzu = null;
        this.mReceiver = null;
        if (this.mDialog != null) {
            Logger.d("NetworkSettingActivity", "onDestroy 0002");
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        Logger.d("NetworkSettingActivity", "onDestroy 0003");
    }

    @Override
    public void onClick(View view) {
        Logger.d("NetworkSettingActivity", "onClick 0001");
        int id = view.getId();
        if (id == this.mAddNetworkBtn.getId()) {
            Logger.d("NetworkSettingActivity", "onClick 0002");
            this.mSelectedAccessPoint = null;
            onCreateDialog((AccessPoint) null, true).show();
        } else if (id == this.mScrollAboveBtn.getId()) {
            Logger.d("NetworkSettingActivity", "onClick 0003");
            this.mApList.smoothScrollBy(-65, 100);
            if (86 > this.mScrollHandleNetwork.getScrollY() + this.scrollAmountBarOfOneClick) {
                Logger.d("NetworkSettingActivity", "onClick 0004");
                this.mScrollHandleNetwork.scrollBy(0, this.scrollAmountBarOfOneClick);
            } else {
                Logger.d("NetworkSettingActivity", "onClick 0005");
                this.mScrollHandleNetwork.scrollTo(0, 86);
            }
        } else if (id == this.mScrollBenethBtn.getId()) {
            Logger.d("NetworkSettingActivity", "onClick 0006");
            this.mApList.smoothScrollBy(65, 100);
            if (-87 < this.mScrollHandleNetwork.getScrollY() - this.scrollAmountBarOfOneClick) {
                Logger.d("NetworkSettingActivity", "onClick 0007");
                this.mScrollHandleNetwork.scrollBy(0, -this.scrollAmountBarOfOneClick);
            } else {
                Logger.d("NetworkSettingActivity", "onClick 0008");
                this.mScrollHandleNetwork.scrollTo(0, -87);
            }
        } else if (id == this.mBackBtn.getId()) {
            Logger.d("NetworkSettingActivity", "onClick 0009");
            if (this.mBackBtnfree) {
                Logger.d("NetworkSettingActivity", "onClick 0010");
                this.mBackBtnfree = false;
                this.mBackBtn.setClickable(false);
                backWifiActivity();
            }
        }
        Logger.d("NetworkSettingActivity", "onClick 0011");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Logger.d("NetworkSettingActivity", "onItemClick 0001");
        this.mSelectedAccessPoint = (AccessPoint) this.mApList.getItemAtPosition(position);
        onCreateDialog(this.mSelectedAccessPoint, false).show();
        Logger.d("NetworkSettingActivity", "onItemClick 0002");
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        Logger.d("NetworkSettingActivity", "onClick 0011");
        if (button == -3 && this.mSelectedAccessPoint != null) {
            Logger.d("NetworkSettingActivity", "onClick 0012");
            forget();
        } else if (button == -1) {
            Logger.d("NetworkSettingActivity", "onClick 0013");
            submit(this.mDialog.getController());
        }
        Logger.d("NetworkSettingActivity", "onClick 0014");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Logger.d("NetworkSettingActivity", "onTouch 0001");
        if (v.getId() == this.mApList.getId()) {
            Logger.d("NetworkSettingActivity", "onTouch 0002");
            if (event.getAction() == 2) {
                Logger.d("NetworkSettingActivity", "onTouch 0003");
                this.mApList.scrollBy(0, 0);
                this.mListTouchActionMove++;
                Logger.d("NetworkSettingActivity", "onTouch 0004");
                return true;
            }
            if (event.getAction() == 1) {
                if (this.mListTouchActionMove > this.mMoveCountJudgingTouch) {
                    this.mListTouchActionMove = 0;
                    return true;
                }
                this.mListTouchActionMove = 0;
            }
            if (event.getX() > this.mScrollBaseNetwork.getLeft() - this.mApList.getLeft()) {
                Logger.d("NetworkSettingActivity", "onTouch 0005");
                return true;
            }
        }
        switch (event.getAction()) {
            case 0:
                Logger.d("NetworkSettingActivity", "onTouch 0006");
                this.eventStopFlg = false;
                this.mTouchBeforeX = event.getX();
                this.mTouchBeforeY = event.getY();
                Rect rect = new Rect(this.mScrollHandleNetwork.getScrollX(), 86 - this.mScrollHandleNetwork.getScrollY(), this.mScrollHandleNetwork.getScrollX() + 40, 86 - (this.mScrollHandleNetwork.getScrollY() - 60));
                if (!rect.contains((int) event.getX(), (int) event.getY())) {
                    Logger.d("NetworkSettingActivity", "onTouch 0007");
                    this.eventStopFlg = true;
                    Logger.d("NetworkSettingActivity", "onTouch 0008");
                    return true;
                }
                break;
            case 1:
                Logger.d("NetworkSettingActivity", "onTouch 0011");
                if (this.eventStopFlg) {
                    Logger.d("NetworkSettingActivity", "onTouch 0012");
                    return false;
                }
                scrollImageAndList(event.getX(), event.getY(), event);
                break;
                break;
            case 2:
                Logger.d("NetworkSettingActivity", "onTouch 0009");
                if (this.eventStopFlg) {
                    Logger.d("NetworkSettingActivity", "onTouch 0010");
                    return false;
                }
                float x = event.getX();
                float y = event.getY();
                scrollImageAndList(x, y, event);
                this.mTouchBeforeX = x;
                this.mTouchBeforeY = y;
                break;
                break;
        }
        Logger.d("NetworkSettingActivity", "onTouch 0013");
        return true;
    }

    private void scrollImageAndList(float mTouchX, float mTouchY, MotionEvent event) {
        Logger.d("NetworkSettingActivity", "scrollImageAndList 0001");
        if (this.upEnableFlg && this.mTouchBeforeY > mTouchY) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0002");
            return;
        }
        if (this.downEnableFlg && mTouchY > this.mTouchBeforeY) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0003");
            return;
        }
        this.upEnableFlg = false;
        this.downEnableFlg = false;
        this.mTouchDiffBeforeActionY = (int) (this.mTouchBeforeY - mTouchY);
        this.mScrollHandleNetwork.scrollBy(0, (int) this.mTouchDiffBeforeActionY);
        int listLocation = ((86 - this.mScrollHandleNetwork.getScrollY()) * this.scrollAbleAmountOfList) / 173;
        int indexOfList = listLocation / 92;
        int locationOfElement = listLocation % 92;
        this.mApList.smoothScrollToPositionFromTop(indexOfList + 1, 92 - locationOfElement, 0);
        this.locationY = this.mScrollHandleNetwork.getScrollY();
        if (this.locationY >= 86) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0004");
            this.mScrollHandleNetwork.scrollTo(0, 86);
            this.mApList.smoothScrollToPosition(0);
            this.upEnableFlg = true;
        }
        if (this.locationY <= -87) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0005");
            this.mScrollHandleNetwork.scrollTo(0, -87);
            this.mApList.smoothScrollToPosition(this.apSize);
            this.downEnableFlg = true;
        }
        Logger.d("NetworkSettingActivity", "scrollImageAndList 0006");
    }

    public void updateAccessPoints() {
        Logger.d("NetworkSettingActivity", "updateAccessPoints 0001");
        if (this == null) {
            Logger.d("NetworkSettingActivity", "updateAccessPoints 0002");
            return;
        }
        int wifiState = this.mWifiManager.getWifiState();
        List<AccessPoint> constructAccessPoints = constructAccessPoints();
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();
        for (AccessPoint constructAccessPoint : constructAccessPoints) {
            Logger.d("NetworkSettingActivity", "updateAccessPoints 0003");
            if (constructAccessPoint != null) {
                Logger.d("NetworkSettingActivity", "updateAccessPoints 0004");
                boolean existFlg = false;
                for (AccessPoint accessPoint : accessPoints) {
                    Logger.d("NetworkSettingActivity", "updateAccessPoints 0005");
                    if (accessPoint != null && constructAccessPoint.ssid.equals(accessPoint.ssid)) {
                        Logger.d("NetworkSettingActivity", "updateAccessPoints 0006");
                        existFlg = true;
                    }
                }
                if (!existFlg) {
                    Logger.d("NetworkSettingActivity", "updateAccessPoints 0007");
                    accessPoints.add(constructAccessPoint);
                }
            }
        }
        this.apSize = accessPoints.size();
        if (accessPoints.size() > 3) {
            Logger.d("NetworkSettingActivity", "updateAccessPoints 0008");
            this.mScrollHandleLayout.setVisibility(0);
            this.scrollAbleAmountOfList = (this.apSize * 92) - 337;
        } else {
            Logger.d("NetworkSettingActivity", "updateAccessPoints 0009");
            this.mScrollHandleLayout.setVisibility(4);
            this.scrollAbleAmountOfList = 0;
        }
        if (this.scrollAbleAmountOfList > 0) {
            Logger.d("NetworkSettingActivity", "updateAccessPoints 0010");
            this.scrollAmountBarOfOneClick = 11245 / this.scrollAbleAmountOfList;
        }
        this.mScrollHandleNetwork.scrollTo(0, 86);
        switch (wifiState) {
            case 0:
                Logger.d("NetworkSettingActivity", "updateAccessPoints 0015");
                this.mApList.setVisibility(8);
                break;
            case 1:
                Logger.d("NetworkSettingActivity", "updateAccessPoints 0016");
                this.mApList.setVisibility(8);
                break;
            case 2:
                Logger.d("NetworkSettingActivity", "updateAccessPoints 0014");
                this.mApList.setVisibility(8);
                break;
            case 3:
                Logger.d("NetworkSettingActivity", "updateAccessPoints 0011");
                if (accessPoints.size() == 0) {
                    Logger.d("NetworkSettingActivity", "updateAccessPoints 0012");
                    this.mApList.setVisibility(8);
                } else {
                    Logger.d("NetworkSettingActivity", "updateAccessPoints 0013");
                    this.mApList.setVisibility(0);
                    this.mApList.setAdapter((ListAdapter) new ApListAdapter(this, R.layout.wifi_ap_list, accessPoints, this.mOnAccessSsid));
                    this.mApList.setOnItemClickListener(this);
                }
                break;
        }
        Logger.d("NetworkSettingActivity", "updateAccessPoints 0017");
    }

    private List<AccessPoint> constructAccessPoints() {
        Logger.d("NetworkSettingActivity", "constructAccessPoints 0001");
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();
        Multimap<String, AccessPoint> apMap = new Multimap<>();
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            Logger.d("NetworkSettingActivity", "constructAccessPoints 0002");
            for (WifiConfiguration config : configs) {
                Logger.d("NetworkSettingActivity", "constructAccessPoints 0003");
                AccessPoint accessPoint = new AccessPoint(this, config);
                accessPoint.update(this.mLastInfo, this.mLastState);
                accessPoints.add(accessPoint);
                apMap.put(accessPoint.ssid, accessPoint);
                Logger.i("NetworkSettingActivity", "test accessPoint = " + accessPoint.ssid);
            }
        }
        List<ScanResult> results = this.mWifiManager.getScanResults();
        if (results != null) {
            Logger.d("NetworkSettingActivity", "constructAccessPoints 0004");
            for (ScanResult result : results) {
                Logger.d("NetworkSettingActivity", "constructAccessPoints 0005");
                if (result.SSID == null || result.SSID.length() == 0 || result.capabilities.contains("[IBSS]")) {
                    Logger.d("NetworkSettingActivity", "constructAccessPoints 0006");
                } else {
                    boolean found = false;
                    for (AccessPoint accessPoint2 : apMap.getAll(result.SSID)) {
                        Logger.d("NetworkSettingActivity", "constructAccessPoints 0007");
                        if (accessPoint2.update(result)) {
                            Logger.d("NetworkSettingActivity", "constructAccessPoints 0008");
                            Logger.i("NetworkSettingActivity", "constructAccessPoints loop result loop accessPoint : success update accessPoint.ssid =" + accessPoint2.ssid);
                            found = true;
                        }
                    }
                    if (!found) {
                        Logger.d("NetworkSettingActivity", "constructAccessPoints 0009");
                        AccessPoint accessPoint3 = new AccessPoint(this, result);
                        accessPoints.add(accessPoint3);
                        apMap.put(accessPoint3.ssid, accessPoint3);
                    }
                }
            }
        }
        Collections.sort(accessPoints);
        Logger.d("NetworkSettingActivity", "constructAccessPoints 0010");
        return accessPoints;
    }

    private class Multimap<K, V> {
        private HashMap<K, List<V>> store;

        private Multimap() {
            this.store = new HashMap<>();
        }

        List<V> getAll(K key) {
            Logger.d("NetworkSettingActivity", "getAll 0001");
            List<V> values = this.store.get(key);
            Logger.d("NetworkSettingActivity", "getAll 0002");
            return values != null ? values : Collections.emptyList();
        }

        void put(K key, V val) {
            Logger.d("NetworkSettingActivity", "put 0001");
            List<V> curVals = this.store.get(key);
            if (curVals == null) {
                Logger.d("NetworkSettingActivity", "put 0002");
                curVals = new ArrayList<>(3);
                this.store.put(key, curVals);
            }
            curVals.add(val);
            Logger.d("NetworkSettingActivity", "put 0003");
        }
    }

    private void updateConnectionState(NetworkInfo.DetailedState state) {
        Logger.d("NetworkSettingActivity", "updateConnectionState 0001");
        Logger.i("NetworkSettingActivity", "updateConnectionState state = " + state);
        if (!this.mWifiManager.isWifiEnabled()) {
            Logger.d("NetworkSettingActivity", "updateConnectionState 0002");
            return;
        }
        this.mLastInfo = this.mWifiManager.getConnectionInfo();
        this.mOnAccessSsid = this.mLastInfo.getSSID();
        if (state != null) {
            Logger.d("NetworkSettingActivity", "updateConnectionState 0003");
            this.mLastState = state;
            List<AccessPoint> accessPoints = constructAccessPoints();
            for (AccessPoint accessPoint : accessPoints) {
                Logger.d("NetworkSettingActivity", "updateConnectionState 0004");
                accessPoint.update(this.mLastInfo, this.mLastState);
            }
        }
        Logger.d("NetworkSettingActivity", "updateConnectionState 0005");
    }

    private void updateWifiState(int state) {
        Logger.d("NetworkSettingActivity", "updateWifiState 0001");
        switch (state) {
            case 1:
                Logger.d("NetworkSettingActivity", "updateWifiState 0004");
                this.mApList.setVisibility(8);
                break;
            case 2:
                Logger.d("NetworkSettingActivity", "updateWifiState 0003");
                this.mApList.setVisibility(8);
                break;
            case 3:
                Logger.d("NetworkSettingActivity", "updateWifiState 0002");
                return;
        }
        this.mLastInfo = null;
        this.mLastState = null;
        Logger.d("NetworkSettingActivity", "updateWifiState 0005");
    }

    public void handleEvent(Context context, Intent intent) {
        Logger.d("NetworkSettingActivity", "handleEvent 0001");
        String action = intent.getAction();
        if (this.mWifiManager.isWifiEnabled()) {
            Logger.d("NetworkSettingActivity", "handleEvent 0002");
            this.mAddNetworkBtn.setEnabled(true);
            this.mLastInfo = this.mWifiManager.getConnectionInfo();
            this.mMacAddress = this.mLastInfo.getMacAddress();
            this.mMacAddressTextView.setText(this.mMacAddress);
        } else {
            Logger.d("NetworkSettingActivity", "handleEvent 0003");
            this.mAddNetworkBtn.setEnabled(false);
            this.mMacAddressTextView.setText("");
        }
        if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
            Logger.d("NetworkSettingActivity", "handleEvent 0004");
            updateWifiState(intent.getIntExtra("wifi_state", 4));
        } else if ("android.net.wifi.SCAN_RESULTS".equals(action) || "android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action) || "android.net.wifi.LINK_CONFIGURATION_CHANGED".equals(action)) {
            Logger.d("NetworkSettingActivity", "handleEvent 0005");
            updateAccessPoints();
        } else if ("android.net.wifi.supplicant.STATE_CHANGE".equals(action)) {
            Logger.d("NetworkSettingActivity", "handleEvent 0006");
            SupplicantState state = (SupplicantState) intent.getParcelableExtra("newState");
            if (!this.mConnected.get() && SupplicantState.isHandshakeState(state)) {
                Logger.d("NetworkSettingActivity", "handleEvent 0007");
                updateConnectionState(WifiInfo.getDetailedStateOf(state));
                updateAccessPoints();
            }
        } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
            Logger.d("NetworkSettingActivity", "handleEvent 0008");
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            this.mConnected.set(info.isConnected());
            updateConnectionState(info.getDetailedState());
        } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
            Logger.d("NetworkSettingActivity", "handleEvent 0009");
            updateConnectionState(null);
            updateAccessPoints();
        }
        Logger.d("NetworkSettingActivity", "handleEvent 0010");
    }

    public Dialog onCreateDialog(AccessPoint ap, boolean edit) {
        Logger.d("NetworkSettingActivity", "onCreateDialog 0001");
        if (this.mDialog != null) {
            Logger.d("NetworkSettingActivity", "onCreateDialog 0002");
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        this.mDialog = new WifiDialog(this, this, ap, edit);
        Logger.d("NetworkSettingActivity", "onCreateDialog 0003");
        return this.mDialog;
    }

    private void backWifiActivity() {
        Logger.d("NetworkSettingActivity", "backWifiActivity 0001");
        Intent intent = new Intent();
        intent.setClassName("jp.co.benesse.dcha.systemsettings", "jp.co.benesse.dcha.systemsettings.WifiSettingActivity");
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        finish();
        Logger.d("NetworkSettingActivity", "backWifiActivity 0002");
    }

    void submit(WifiConfigController configController) {
        Logger.d("NetworkSettingActivity", "submit 0001");
        WifiConfiguration config = configController.getConfig();
        if (config == null) {
            Logger.d("NetworkSettingActivity", "submit 0002");
            if (this.mSelectedAccessPoint != null && this.mSelectedAccessPoint.networkId != -1) {
                Logger.d("NetworkSettingActivity", "submit 0003");
                this.mWifiManager.connect(this.mSelectedAccessPoint.networkId, this.mConnectListener);
            }
        } else if (config.networkId != -1) {
            Logger.d("NetworkSettingActivity", "submit 0004");
            if (this.mSelectedAccessPoint != null) {
                Logger.d("NetworkSettingActivity", "submit 0005");
                this.mWifiManager.save(config, this.mSaveListener);
            }
        } else if (configController.isEdit()) {
            Logger.d("NetworkSettingActivity", "submit 0006");
            this.mWifiManager.save(config, this.mSaveListener);
        } else {
            Logger.d("NetworkSettingActivity", "submit 0007");
            this.mWifiManager.connect(config, this.mConnectListener);
        }
        if (this.mWifiManager.isWifiEnabled()) {
            Logger.d("NetworkSettingActivity", "submit 0008");
        }
        updateAccessPoints();
        Logger.d("NetworkSettingActivity", "submit 0009");
    }

    void forget() {
        Logger.d("NetworkSettingActivity", "forget 0001");
        if (this.mSelectedAccessPoint.networkId == -1) {
            Logger.d("NetworkSettingActivity", "forget 0002");
            return;
        }
        this.mWifiManager.forget(this.mSelectedAccessPoint.networkId, this.mForgetListener);
        if (this.mWifiManager.isWifiEnabled()) {
            Logger.d("NetworkSettingActivity", "forget 0003");
        }
        updateAccessPoints();
        Logger.d("NetworkSettingActivity", "forget 0004");
    }
}
