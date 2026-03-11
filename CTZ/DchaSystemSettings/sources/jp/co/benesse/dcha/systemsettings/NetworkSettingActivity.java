package jp.co.benesse.dcha.systemsettings;

import android.content.Intent;
import android.graphics.Rect;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import jp.co.benesse.dcha.systemsettings.AccessPoint;
import jp.co.benesse.dcha.systemsettings.WifiDialog;
import jp.co.benesse.dcha.systemsettings.WifiTracker;
import jp.co.benesse.dcha.util.Logger;

public class NetworkSettingActivity extends ParentSettingActivity implements View.OnClickListener, View.OnTouchListener, AdapterView.OnItemClickListener, AccessPoint.AccessPointListener, WifiDialog.WifiDialogListener, WifiTracker.WifiListener {
    private int apSize;
    private boolean downEnableFlg;
    private boolean eventStopFlg;
    private int locationY;
    private Bundle mAccessPointSavedState;
    private ImageView mAddNetworkBtn;
    private ListView mApList;
    private ImageView mBackBtn;
    private HandlerThread mBgThread;
    private WifiManager.ActionListener mConnectListener;
    private WifiDialog mDialog;
    private int mDialogMode;
    private AccessPoint mDlgAccessPoint;
    private WifiManager.ActionListener mForgetListener;
    private int mListTouchActionMove;
    private TextView mMacAddressTextView;
    private String mOpenSsid;
    private ImageView mPankuzu;
    private WifiManager.ActionListener mSaveListener;
    private MenuItem mScanMenuItem;
    private ImageView mScrollAboveBtn;
    private ImageView mScrollBaseNetwork;
    private ImageView mScrollBenethBtn;
    private RelativeLayout mScrollHandleLayout;
    private ImageView mScrollHandleNetwork;
    private AccessPoint mSelectedAccessPoint;
    private float mTouchBeforeX;
    private float mTouchBeforeY;
    private float mTouchDiffBeforeActionY;
    private boolean mTracking;
    protected WifiManager mWifiManager;
    private Bundle mWifiNfcDialogSavedState;
    private WifiTracker mWifiTracker;
    private int scrollAbleAmountOfList;
    private int scrollAmountBarOfOneClick;
    private boolean upEnableFlg;
    private final String TAG = "NetworkSettingActivity";
    private boolean mBackBtnfree = true;
    private int mMoveCountJudgingTouch = 8;
    private final int scrollAbleAmountOfBar = 259;
    private final int heightOfElement = 138;
    private final int HeightOfListView = 505;
    private final int scrollAmountListOfOneClick = 97;
    private final int aboveOffset = 129;
    private final int benethOffset = -130;
    private final int scrollBarWidth = 60;
    private final int scrollBarHeight = 90;

    @Override
    protected void onCreate(Bundle bundle) {
        Logger.d("NetworkSettingActivity", "onCreate 0001");
        super.onCreate(bundle);
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
        this.mScrollHandleNetwork.scrollTo(0, 129);
        this.mIsFirstFlow = getFirstFlg();
        if (!this.mIsFirstFlow) {
            Logger.d("NetworkSettingActivity", "onCreate 0002");
            this.mPankuzu.setVisibility(4);
        } else {
            Logger.d("NetworkSettingActivity", "onCreate 0003");
            getWindow().addFlags(128);
        }
        this.mBgThread = new HandlerThread("NetworkSettingActivity", 10);
        this.mBgThread.start();
        onActivityCreated(bundle);
        Logger.d("NetworkSettingActivity", "onCreate 0004");
    }

    @Override
    protected void onDestroy() {
        Logger.d("NetworkSettingActivity", "onDestroy 0001");
        this.mBgThread.quit();
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
        if (this.mDialog != null) {
            Logger.d("NetworkSettingActivity", "onDestroy 0002");
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        Logger.d("NetworkSettingActivity", "onDestroy 0003");
        super.onDestroy();
    }

    public void onActivityCreated(Bundle bundle) {
        Logger.d("NetworkSettingActivity", "onActivityCreated 0001");
        this.mWifiTracker = new WifiTracker(this, this, this.mBgThread.getLooper(), true, true, false);
        this.mWifiManager = this.mWifiTracker.getManager();
        this.mConnectListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                Logger.d("NetworkSettingActivity", "onSuccess 0001");
            }

            public void onFailure(int i) {
                Logger.d("NetworkSettingActivity", "onFailure 0001");
            }
        };
        this.mSaveListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                Logger.d("NetworkSettingActivity", "onSuccess 0002");
            }

            public void onFailure(int i) {
                Logger.d("NetworkSettingActivity", "onFailure 0002");
            }
        };
        this.mForgetListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                Logger.d("NetworkSettingActivity", "onSuccess 0003");
            }

            public void onFailure(int i) {
                Logger.d("NetworkSettingActivity", "onFailure 0003");
            }
        };
        if (bundle != null) {
            Logger.d("NetworkSettingActivity", "onActivityCreated 0002");
            this.mDialogMode = bundle.getInt("dialog_mode");
            if (bundle.containsKey("wifi_ap_state")) {
                Logger.d("NetworkSettingActivity", "onActivityCreated 0003");
                this.mAccessPointSavedState = bundle.getBundle("wifi_ap_state");
            }
            if (bundle.containsKey("wifi_nfc_dlg_state")) {
                Logger.d("NetworkSettingActivity", "onActivityCreated 0004");
                this.mWifiNfcDialogSavedState = bundle.getBundle("wifi_nfc_dlg_state");
            }
        }
        Intent intent = getIntent();
        if (intent.hasExtra("wifi_start_connect_ssid")) {
            Logger.d("NetworkSettingActivity", "onActivityCreated 0005");
            this.mOpenSsid = intent.getStringExtra("wifi_start_connect_ssid");
            onAccessPointsChanged();
        }
        if (this.mWifiManager != null) {
            Logger.d("NetworkSettingActivity", "onActivityCreated 0006");
            this.mWifiManager.setWifiEnabled(true);
        }
        Logger.d("NetworkSettingActivity", "onActivityCreated 0007");
    }

    @Override
    protected void onStart() {
        Logger.d("NetworkSettingActivity", "onStart 0001");
        super.onStart();
        Logger.d("NetworkSettingActivity", "onStart 0002");
    }

    @Override
    protected void onResume() {
        Logger.d("NetworkSettingActivity", "onResume 0001");
        super.onResume();
        this.mWifiTracker.startTracking();
        this.mTracking = true;
        invalidateOptionsMenu();
        this.mMacAddressTextView.setText(getMacAddress(this.mWifiManager));
        Logger.d("NetworkSettingActivity", "onResume 0002");
    }

    @Override
    protected void onPause() {
        Logger.d("NetworkSettingActivity", "onPause 0001");
        super.onPause();
        this.mTracking = false;
        this.mWifiTracker.stopTracking();
        Logger.d("NetworkSettingActivity", "onPause 0002");
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        Logger.d("NetworkSettingActivity", "onSaveInstanceState 0001");
        super.onSaveInstanceState(bundle);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            Logger.d("NetworkSettingActivity", "onSaveInstanceState 0002");
            bundle.putInt("dialog_mode", this.mDialogMode);
            if (this.mDlgAccessPoint != null) {
                Logger.d("NetworkSettingActivity", "onSaveInstanceState 0003");
                this.mAccessPointSavedState = new Bundle();
                this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
                bundle.putBundle("wifi_ap_state", this.mAccessPointSavedState);
            }
        }
        Logger.d("NetworkSettingActivity", "onSaveInstanceState 0004");
    }

    private void showDialog(AccessPoint accessPoint, int i) {
        Logger.d("NetworkSettingActivity", "showDialog 0001");
        if (this.mDialog != null) {
            Logger.d("NetworkSettingActivity", "showDialog 0002");
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint;
        this.mDialogMode = i;
        AccessPoint accessPoint2 = this.mDlgAccessPoint;
        if (accessPoint2 == null) {
            Logger.d("NetworkSettingActivity", "showDialog 0003");
            if (this.mAccessPointSavedState != null) {
                Logger.d("NetworkSettingActivity", "showDialog 0004");
                accessPoint2 = new AccessPoint(this, this.mAccessPointSavedState);
                this.mDlgAccessPoint = accessPoint2;
                this.mAccessPointSavedState = null;
            }
        }
        this.mSelectedAccessPoint = accessPoint2;
        this.mDialog = new WifiDialog(this, this, accessPoint, i);
        this.mDialog.show();
        Logger.d("NetworkSettingActivity", "showDialog 0005");
    }

    @Override
    public void onAccessPointsChanged() {
        Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0001");
        if (!this.mTracking) {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0002");
            return;
        }
        int wifiState = this.mWifiManager.getWifiState();
        List<AccessPoint> accessPoints = this.mWifiTracker.getAccessPoints();
        ArrayList<AccessPoint> arrayList = new ArrayList();
        for (AccessPoint accessPoint : accessPoints) {
            if (accessPoint != null) {
                boolean z = false;
                for (AccessPoint accessPoint2 : arrayList) {
                    if (accessPoint2 != null && accessPoint.getSsidStr().equals(accessPoint2.getSsidStr())) {
                        z = true;
                    }
                }
                if (!z) {
                    arrayList.add(accessPoint);
                }
            }
        }
        boolean z2 = arrayList.size() != this.apSize;
        this.apSize = arrayList.size();
        if (arrayList.size() > 3) {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0003");
            this.mScrollHandleLayout.setVisibility(0);
            this.scrollAbleAmountOfList = (this.apSize * 138) - 505;
        } else {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0004");
            this.mScrollHandleLayout.setVisibility(4);
            this.scrollAbleAmountOfList = 0;
        }
        if (this.scrollAbleAmountOfList > 0) {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0005");
            this.scrollAmountBarOfOneClick = 25123 / this.scrollAbleAmountOfList;
        }
        if (z2) {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0006");
            this.mScrollHandleNetwork.scrollTo(0, 129);
        }
        switch (wifiState) {
            case 0:
                Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0014");
                this.mApList.setVisibility(8);
                break;
            case 1:
                Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0015");
                if (this.mScanMenuItem != null) {
                    Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0016");
                    this.mScanMenuItem.setEnabled(false);
                }
                this.mApList.setVisibility(8);
                break;
            case 2:
                Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0013");
                this.mApList.setVisibility(8);
                break;
            case 3:
                Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0007");
                for (AccessPoint accessPoint3 : arrayList) {
                    if (accessPoint3.getLevel() != -1) {
                        if (this.mOpenSsid != null && this.mOpenSsid.equals(accessPoint3.getSsidStr()) && !accessPoint3.isSaved() && accessPoint3.getSecurity() != 0) {
                            this.mOpenSsid = null;
                        }
                        accessPoint3.setListener(this);
                    }
                }
                if (this.mScanMenuItem != null) {
                    Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0008");
                    this.mScanMenuItem.setEnabled(true);
                }
                if (arrayList.size() == 0) {
                    Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0009");
                    this.mApList.setVisibility(8);
                } else {
                    Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0010");
                    this.mApList.setVisibility(0);
                    ArrayAdapter arrayAdapter = (ArrayAdapter) this.mApList.getAdapter();
                    if (arrayAdapter == null || z2) {
                        Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0011");
                        this.mApList.setAdapter((ListAdapter) new ApListAdapter(this, R.layout.wifi_ap_list, arrayList, getOnAccessSsid(this.mWifiManager)));
                    } else {
                        Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0012");
                        arrayAdapter.clear();
                        arrayAdapter.addAll(arrayList);
                        arrayAdapter.notifyDataSetChanged();
                    }
                    this.mApList.setOnItemClickListener(this);
                }
                break;
        }
        Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0017");
    }

    @Override
    public void onWifiStateChanged(int i) {
        Logger.d("NetworkSettingActivity", "onWifiStateChanged 0001");
        if (!this.mTracking) {
            Logger.d("NetworkSettingActivity", "onWifiStateChanged 0002");
            return;
        }
        switch (i) {
            case 1:
                Logger.d("NetworkSettingActivity", "onWifiStateChanged 0005");
                this.mApList.setVisibility(8);
                this.mAddNetworkBtn.setEnabled(false);
                this.mMacAddressTextView.setText("");
                break;
            case 2:
                Logger.d("NetworkSettingActivity", "onWifiStateChanged 0004");
                this.mApList.setVisibility(8);
                break;
            case 3:
                Logger.d("NetworkSettingActivity", "onWifiStateChanged 0003");
                this.mAddNetworkBtn.setEnabled(true);
                this.mMacAddressTextView.setText(getMacAddress(this.mWifiManager));
                return;
        }
        Logger.d("NetworkSettingActivity", "onWifiStateChanged 0006");
    }

    @Override
    public void onConnectedChanged() {
        Logger.d("NetworkSettingActivity", "onConnectedChanged 0001");
    }

    @Override
    public void onForget(WifiDialog wifiDialog) {
        Logger.d("NetworkSettingActivity", "onForget 0001");
        forget();
        Logger.d("NetworkSettingActivity", "onForget 0002");
    }

    @Override
    public void onSubmit(WifiDialog wifiDialog) {
        Logger.d("NetworkSettingActivity", "onSubmit 0001");
        if (this.mDialog != null) {
            Logger.d("NetworkSettingActivity", "onSubmit 0002");
            submit(this.mDialog.getController());
        }
        Logger.d("NetworkSettingActivity", "onSubmit 0003");
    }

    void submit(WifiConfigController wifiConfigController) {
        Logger.d("NetworkSettingActivity", "submit 0001");
        WifiConfiguration config = wifiConfigController.getConfig();
        if (config == null) {
            Logger.d("NetworkSettingActivity", "submit 0002");
            if (this.mSelectedAccessPoint != null && this.mSelectedAccessPoint.isSaved()) {
                Logger.d("NetworkSettingActivity", "submit 0003");
                connect(this.mSelectedAccessPoint.getConfig());
            }
        } else if (wifiConfigController.getMode() == 2) {
            Logger.d("NetworkSettingActivity", "submit 0004");
            this.mWifiManager.save(config, this.mSaveListener);
        } else {
            Logger.d("NetworkSettingActivity", "submit 0005");
            this.mWifiManager.save(config, this.mSaveListener);
            if (this.mSelectedAccessPoint != null) {
                Logger.d("NetworkSettingActivity", "submit 0006");
                connect(config);
            }
        }
        this.mWifiTracker.resumeScanning();
        Logger.d("NetworkSettingActivity", "submit 0007");
    }

    void forget() {
        Logger.d("NetworkSettingActivity", "forget 0001");
        if (!this.mSelectedAccessPoint.isSaved()) {
            Logger.d("NetworkSettingActivity", "forget 0002");
            if (this.mSelectedAccessPoint.getNetworkInfo() != null && this.mSelectedAccessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED) {
                Logger.d("NetworkSettingActivity", "forget 0003");
                this.mWifiManager.disableEphemeralNetwork(AccessPoint.convertToQuotedString(this.mSelectedAccessPoint.getSsidStr()));
            } else {
                Logger.d("NetworkSettingActivity", "forget 0004");
                Logger.e("NetworkSettingActivity", "Failed to forget invalid network " + this.mSelectedAccessPoint.getConfig());
                return;
            }
        } else {
            Logger.d("NetworkSettingActivity", "forget 0005");
            this.mWifiManager.forget(this.mSelectedAccessPoint.getConfig().networkId, this.mForgetListener);
        }
        this.mWifiTracker.resumeScanning();
        Logger.d("NetworkSettingActivity", "forget 0006");
    }

    protected void connect(WifiConfiguration wifiConfiguration) {
        Logger.d("NetworkSettingActivity", "connect 0001");
        this.mWifiManager.connect(wifiConfiguration, this.mConnectListener);
        Logger.d("NetworkSettingActivity", "connect 0002");
    }

    void onAddNetworkPressed() {
        Logger.d("NetworkSettingActivity", "onAddNetworkPressed 0001");
        this.mSelectedAccessPoint = null;
        showDialog((AccessPoint) null, 1);
        Logger.d("NetworkSettingActivity", "onAddNetworkPressed 0002");
    }

    @Override
    public void onAccessPointChanged(AccessPoint accessPoint) {
        Logger.d("NetworkSettingActivity", "onAccessPointChanged 0001");
        ApListAdapter apListAdapter = (ApListAdapter) this.mApList.getAdapter();
        if (apListAdapter != null) {
            apListAdapter.updateAccessPoint(accessPoint);
        }
        Logger.d("NetworkSettingActivity", "onAccessPointChanged 0002");
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        Logger.d("NetworkSettingActivity", "onLevelChanged 0001");
        ApListAdapter apListAdapter = (ApListAdapter) this.mApList.getAdapter();
        if (apListAdapter != null) {
            apListAdapter.updateAccessPoint(accessPoint);
        }
        Logger.d("NetworkSettingActivity", "onLevelChanged 0002");
    }

    @Override
    public void onClick(View view) {
        Logger.d("NetworkSettingActivity", "onClick 0001");
        int id = view.getId();
        if (id == this.mAddNetworkBtn.getId()) {
            Logger.d("NetworkSettingActivity", "onClick 0002");
            onAddNetworkPressed();
        } else if (id == this.mScrollAboveBtn.getId()) {
            Logger.d("NetworkSettingActivity", "onClick 0003");
            this.mApList.smoothScrollBy(-97, 100);
            if (129 > this.mScrollHandleNetwork.getScrollY() + this.scrollAmountBarOfOneClick) {
                Logger.d("NetworkSettingActivity", "onClick 0004");
                this.mScrollHandleNetwork.scrollBy(0, this.scrollAmountBarOfOneClick);
            } else {
                Logger.d("NetworkSettingActivity", "onClick 0005");
                this.mScrollHandleNetwork.scrollTo(0, 129);
            }
        } else if (id == this.mScrollBenethBtn.getId()) {
            Logger.d("NetworkSettingActivity", "onClick 0006");
            this.mApList.smoothScrollBy(97, 100);
            if (-130 < this.mScrollHandleNetwork.getScrollY() - this.scrollAmountBarOfOneClick) {
                Logger.d("NetworkSettingActivity", "onClick 0007");
                this.mScrollHandleNetwork.scrollBy(0, -this.scrollAmountBarOfOneClick);
            } else {
                Logger.d("NetworkSettingActivity", "onClick 0008");
                this.mScrollHandleNetwork.scrollTo(0, -130);
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
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        int i2;
        Logger.d("NetworkSettingActivity", "onItemClick 0001");
        this.mSelectedAccessPoint = (AccessPoint) this.mApList.getItemAtPosition(i);
        if (this.mSelectedAccessPoint.isSaved()) {
            Logger.d("NetworkSettingActivity", "onItemClick 0002");
            i2 = 0;
        } else {
            Logger.d("NetworkSettingActivity", "onItemClick 0003");
            i2 = 1;
        }
        showDialog(this.mSelectedAccessPoint, i2);
        Logger.d("NetworkSettingActivity", "onItemClick 0004");
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Logger.d("NetworkSettingActivity", "onTouch 0001");
        if (view.getId() == this.mApList.getId()) {
            Logger.d("NetworkSettingActivity", "onTouch 0002");
            if (motionEvent.getAction() == 2) {
                Logger.d("NetworkSettingActivity", "onTouch 0003");
                this.mApList.scrollBy(0, 0);
                this.mListTouchActionMove++;
                Logger.d("NetworkSettingActivity", "onTouch 0004");
                return true;
            }
            if (motionEvent.getAction() == 1) {
                Logger.d("NetworkSettingActivity", "onTouch 0005");
                if (this.mListTouchActionMove > this.mMoveCountJudgingTouch) {
                    Logger.d("NetworkSettingActivity", "onTouch 0006");
                    this.mListTouchActionMove = 0;
                    return true;
                }
                this.mListTouchActionMove = 0;
            }
            if (motionEvent.getX() > this.mScrollBaseNetwork.getLeft() - this.mApList.getLeft()) {
                Logger.d("NetworkSettingActivity", "onTouch 0007");
                return true;
            }
        }
        switch (motionEvent.getAction()) {
            case 0:
                Logger.d("NetworkSettingActivity", "onTouch 0008");
                this.eventStopFlg = false;
                this.mTouchBeforeX = motionEvent.getX();
                this.mTouchBeforeY = motionEvent.getY();
                if (!new Rect(this.mScrollHandleNetwork.getScrollX(), 129 - this.mScrollHandleNetwork.getScrollY(), this.mScrollHandleNetwork.getScrollX() + 60, 129 - (this.mScrollHandleNetwork.getScrollY() - 90)).contains((int) motionEvent.getX(), (int) motionEvent.getY())) {
                    Logger.d("NetworkSettingActivity", "onTouch 0009");
                    this.eventStopFlg = true;
                    return true;
                }
                break;
            case 1:
                Logger.d("NetworkSettingActivity", "onTouch 0012");
                if (this.eventStopFlg) {
                    Logger.d("NetworkSettingActivity", "onTouch 0013");
                    return false;
                }
                scrollImageAndList(motionEvent.getX(), motionEvent.getY(), motionEvent);
                break;
                break;
            case 2:
                Logger.d("NetworkSettingActivity", "onTouch 0010");
                if (this.eventStopFlg) {
                    Logger.d("NetworkSettingActivity", "onTouch 0011");
                    return false;
                }
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                scrollImageAndList(x, y, motionEvent);
                this.mTouchBeforeX = x;
                this.mTouchBeforeY = y;
                break;
                break;
        }
        Logger.d("NetworkSettingActivity", "onTouch 0014");
        return true;
    }

    private void scrollImageAndList(float f, float f2, MotionEvent motionEvent) {
        Logger.d("NetworkSettingActivity", "scrollImageAndList 0001");
        if (this.upEnableFlg && this.mTouchBeforeY > f2) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0002");
            return;
        }
        if (this.downEnableFlg && f2 > this.mTouchBeforeY) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0003");
            return;
        }
        this.upEnableFlg = false;
        this.downEnableFlg = false;
        this.mTouchDiffBeforeActionY = (int) (this.mTouchBeforeY - f2);
        this.mScrollHandleNetwork.scrollBy(0, (int) this.mTouchDiffBeforeActionY);
        int scrollY = ((129 - this.mScrollHandleNetwork.getScrollY()) * this.scrollAbleAmountOfList) / 259;
        this.mApList.smoothScrollToPositionFromTop((scrollY / 138) + 1, 138 - (scrollY % 138), 0);
        this.locationY = this.mScrollHandleNetwork.getScrollY();
        if (this.locationY >= 129) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0004");
            this.mScrollHandleNetwork.scrollTo(0, 129);
            this.mApList.smoothScrollToPosition(0);
            this.upEnableFlg = true;
        }
        if (this.locationY <= -130) {
            Logger.d("NetworkSettingActivity", "scrollImageAndList 0005");
            this.mScrollHandleNetwork.scrollTo(0, -130);
            this.mApList.smoothScrollToPosition(this.apSize);
            this.downEnableFlg = true;
        }
        Logger.d("NetworkSettingActivity", "scrollImageAndList 0006");
    }

    private void backWifiActivity() {
        Logger.d("NetworkSettingActivity", "backWifiActivity 0001");
        Intent intent = new Intent();
        intent.setClassName("jp.co.benesse.dcha.systemsettings", "jp.co.benesse.dcha.systemsettings.WifiSettingActivity");
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        Logger.d("NetworkSettingActivity", "backWifiActivity 0002");
    }

    private String getMacAddress(WifiManager wifiManager) {
        Logger.d("NetworkSettingActivity", "getMacAddress 0001");
        String macAddress = "";
        try {
            macAddress = Settings.System.getString(getContentResolver(), "bc:mac_address");
        } catch (Exception e) {
            Logger.d("NetworkSettingActivity", "getMacAddress 0002");
            Logger.d("NetworkSettingActivity", "Exception", e);
        }
        if (TextUtils.isEmpty(macAddress)) {
            try {
                Logger.d("NetworkSettingActivity", "getMacAddress 0003");
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    Logger.d("NetworkSettingActivity", "getMacAddress 0004");
                    macAddress = connectionInfo.getMacAddress();
                }
            } catch (Exception e2) {
                Logger.d("NetworkSettingActivity", "getMacAddress 0005");
                Logger.d("NetworkSettingActivity", "Exception", e2);
            }
        }
        Logger.d("NetworkSettingActivity", "getMacAddress 0006 return:", macAddress);
        return macAddress;
    }

    private String getOnAccessSsid(WifiManager wifiManager) {
        Logger.d("NetworkSettingActivity", "getOnAccessSsid 0001");
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        Logger.d("NetworkSettingActivity", "getOnAccessSsid 0002");
        return connectionInfo.getSSID();
    }
}
