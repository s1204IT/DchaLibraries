package jp.co.benesse.dcha.systemsettings;

import android.content.ContentResolver;
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

public class NetworkSettingActivity extends ParentSettingActivity implements WifiTracker.WifiListener, AccessPoint.AccessPointListener, WifiDialog.WifiDialogListener, View.OnClickListener, AdapterView.OnItemClickListener, View.OnTouchListener {
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
    private int mMoveCountJudgingTouch = 4;
    private final int scrollAbleAmountOfBar = 173;
    private final int heightOfElement = 92;
    private final int HeightOfListView = 337;
    private final int scrollAmountListOfOneClick = 65;
    private final int aboveOffset = 86;
    private final int benethOffset = -87;
    private final int scrollBarWidth = 40;
    private final int scrollBarHeight = 60;

    @Override
    protected void onCreate(Bundle icicle) {
        Logger.d("NetworkSettingActivity", "onCreate 0001");
        super.onCreate(icicle);
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
        this.mBgThread = new HandlerThread("NetworkSettingActivity", 10);
        this.mBgThread.start();
        onActivityCreated(icicle);
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

    public void onActivityCreated(Bundle savedInstanceState) {
        Logger.d("NetworkSettingActivity", "onActivityCreated 0001");
        this.mWifiTracker = new WifiTracker(this, this, this.mBgThread.getLooper(), true, true, false);
        this.mWifiManager = this.mWifiTracker.getManager();
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
        if (savedInstanceState != null) {
            Logger.d("NetworkSettingActivity", "onActivityCreated 0002");
            this.mDialogMode = savedInstanceState.getInt("dialog_mode");
            if (savedInstanceState.containsKey("wifi_ap_state")) {
                Logger.d("NetworkSettingActivity", "onActivityCreated 0003");
                this.mAccessPointSavedState = savedInstanceState.getBundle("wifi_ap_state");
            }
            if (savedInstanceState.containsKey("wifi_nfc_dlg_state")) {
                Logger.d("NetworkSettingActivity", "onActivityCreated 0004");
                this.mWifiNfcDialogSavedState = savedInstanceState.getBundle("wifi_nfc_dlg_state");
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
    public void onSaveInstanceState(Bundle outState) {
        Logger.d("NetworkSettingActivity", "onSaveInstanceState 0001");
        super.onSaveInstanceState(outState);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            Logger.d("NetworkSettingActivity", "onSaveInstanceState 0002");
            outState.putInt("dialog_mode", this.mDialogMode);
            if (this.mDlgAccessPoint != null) {
                Logger.d("NetworkSettingActivity", "onSaveInstanceState 0003");
                this.mAccessPointSavedState = new Bundle();
                this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
                outState.putBundle("wifi_ap_state", this.mAccessPointSavedState);
            }
        }
        Logger.d("NetworkSettingActivity", "onSaveInstanceState 0004");
    }

    private void showDialog(AccessPoint accessPoint, int dialogMode) {
        Logger.d("NetworkSettingActivity", "showDialog 0001");
        if (this.mDialog != null) {
            Logger.d("NetworkSettingActivity", "showDialog 0002");
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint;
        this.mDialogMode = dialogMode;
        AccessPoint ap = this.mDlgAccessPoint;
        if (ap == null) {
            Logger.d("NetworkSettingActivity", "showDialog 0003");
            if (this.mAccessPointSavedState != null) {
                Logger.d("NetworkSettingActivity", "showDialog 0004");
                ap = new AccessPoint(this, this.mAccessPointSavedState);
                this.mDlgAccessPoint = ap;
                this.mAccessPointSavedState = null;
            }
        }
        this.mSelectedAccessPoint = ap;
        this.mDialog = new WifiDialog(this, this, accessPoint, dialogMode);
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
        List<AccessPoint> constructAccessPoints = this.mWifiTracker.getAccessPoints();
        ArrayList<AccessPoint> arrayList = new ArrayList();
        for (AccessPoint constructAccessPoint : constructAccessPoints) {
            if (constructAccessPoint != null) {
                boolean existFlg = false;
                for (AccessPoint accessPoint : arrayList) {
                    if (accessPoint != null && constructAccessPoint.getSsidStr().equals(accessPoint.getSsidStr())) {
                        existFlg = true;
                    }
                }
                if (!existFlg) {
                    arrayList.add(constructAccessPoint);
                }
            }
        }
        boolean refresh = arrayList.size() != this.apSize;
        this.apSize = arrayList.size();
        if (arrayList.size() > 3) {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0003");
            this.mScrollHandleLayout.setVisibility(0);
            this.scrollAbleAmountOfList = (this.apSize * 92) - 337;
        } else {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0004");
            this.mScrollHandleLayout.setVisibility(4);
            this.scrollAbleAmountOfList = 0;
        }
        if (this.scrollAbleAmountOfList > 0) {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0005");
            this.scrollAmountBarOfOneClick = 11245 / this.scrollAbleAmountOfList;
        }
        if (refresh) {
            Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0006");
            this.mScrollHandleNetwork.scrollTo(0, 86);
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
                for (AccessPoint accessPoint2 : arrayList) {
                    if (accessPoint2.getLevel() != -1) {
                        if (this.mOpenSsid != null && this.mOpenSsid.equals(accessPoint2.getSsidStr()) && !accessPoint2.isSaved() && accessPoint2.getSecurity() != 0) {
                            this.mOpenSsid = null;
                        }
                        accessPoint2.setListener(this);
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
                    ArrayAdapter adapter = (ArrayAdapter) this.mApList.getAdapter();
                    if (adapter == null || refresh) {
                        Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0011");
                        this.mApList.setAdapter((ListAdapter) new ApListAdapter(this, R.layout.wifi_ap_list, arrayList, getOnAccessSsid(this.mWifiManager)));
                    } else {
                        Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0012");
                        adapter.clear();
                        adapter.addAll(arrayList);
                        adapter.notifyDataSetChanged();
                    }
                    this.mApList.setOnItemClickListener(this);
                }
                break;
        }
        Logger.d("NetworkSettingActivity", "onAccessPointsChanged 0017");
    }

    @Override
    public void onWifiStateChanged(int state) {
        Logger.d("NetworkSettingActivity", "onWifiStateChanged 0001");
        if (!this.mTracking) {
            Logger.d("NetworkSettingActivity", "onWifiStateChanged 0002");
            return;
        }
        switch (state) {
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
    public void onForget(WifiDialog dialog) {
        Logger.d("NetworkSettingActivity", "onForget 0001");
        forget();
        Logger.d("NetworkSettingActivity", "onForget 0002");
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        Logger.d("NetworkSettingActivity", "onSubmit 0001");
        if (this.mDialog != null) {
            Logger.d("NetworkSettingActivity", "onSubmit 0002");
            submit(this.mDialog.getController());
        }
        Logger.d("NetworkSettingActivity", "onSubmit 0003");
    }

    void submit(WifiConfigController configController) {
        Logger.d("NetworkSettingActivity", "submit 0001");
        WifiConfiguration config = configController.getConfig();
        if (config == null) {
            Logger.d("NetworkSettingActivity", "submit 0002");
            if (this.mSelectedAccessPoint != null && this.mSelectedAccessPoint.isSaved()) {
                Logger.d("NetworkSettingActivity", "submit 0003");
                connect(this.mSelectedAccessPoint.getConfig());
            }
        } else if (configController.getMode() == 2) {
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

    protected void connect(WifiConfiguration config) {
        Logger.d("NetworkSettingActivity", "connect 0001");
        this.mWifiManager.connect(config, this.mConnectListener);
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
        ApListAdapter adapter = (ApListAdapter) this.mApList.getAdapter();
        if (adapter != null) {
            adapter.updateAccessPoint(accessPoint);
        }
        Logger.d("NetworkSettingActivity", "onAccessPointChanged 0002");
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        Logger.d("NetworkSettingActivity", "onLevelChanged 0001");
        ApListAdapter adapter = (ApListAdapter) this.mApList.getAdapter();
        if (adapter != null) {
            adapter.updateAccessPoint(accessPoint);
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
        int dialogMode;
        Logger.d("NetworkSettingActivity", "onItemClick 0001");
        this.mSelectedAccessPoint = (AccessPoint) this.mApList.getItemAtPosition(position);
        if (this.mSelectedAccessPoint.isSaved()) {
            Logger.d("NetworkSettingActivity", "onItemClick 0002");
            dialogMode = 0;
        } else {
            Logger.d("NetworkSettingActivity", "onItemClick 0003");
            dialogMode = 1;
        }
        showDialog(this.mSelectedAccessPoint, dialogMode);
        Logger.d("NetworkSettingActivity", "onItemClick 0004");
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
                Logger.d("NetworkSettingActivity", "onTouch 0005");
                if (this.mListTouchActionMove > this.mMoveCountJudgingTouch) {
                    Logger.d("NetworkSettingActivity", "onTouch 0006");
                    this.mListTouchActionMove = 0;
                    return true;
                }
                this.mListTouchActionMove = 0;
            }
            if (event.getX() > this.mScrollBaseNetwork.getLeft() - this.mApList.getLeft()) {
                Logger.d("NetworkSettingActivity", "onTouch 0007");
                return true;
            }
        }
        switch (event.getAction()) {
            case 0:
                Logger.d("NetworkSettingActivity", "onTouch 0008");
                this.eventStopFlg = false;
                this.mTouchBeforeX = event.getX();
                this.mTouchBeforeY = event.getY();
                Rect rect = new Rect(this.mScrollHandleNetwork.getScrollX(), 86 - this.mScrollHandleNetwork.getScrollY(), this.mScrollHandleNetwork.getScrollX() + 40, 86 - (this.mScrollHandleNetwork.getScrollY() - 60));
                if (!rect.contains((int) event.getX(), (int) event.getY())) {
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
                scrollImageAndList(event.getX(), event.getY(), event);
                break;
                break;
            case 2:
                Logger.d("NetworkSettingActivity", "onTouch 0010");
                if (this.eventStopFlg) {
                    Logger.d("NetworkSettingActivity", "onTouch 0011");
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
        Logger.d("NetworkSettingActivity", "onTouch 0014");
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

    private void backWifiActivity() {
        Logger.d("NetworkSettingActivity", "backWifiActivity 0001");
        Intent intent = new Intent();
        intent.setClassName("jp.co.benesse.dcha.systemsettings", "jp.co.benesse.dcha.systemsettings.WifiSettingActivity");
        intent.putExtra("first_flg", this.mIsFirstFlow);
        startActivity(intent);
        finish();
        Logger.d("NetworkSettingActivity", "backWifiActivity 0002");
    }

    private String getMacAddress(WifiManager wifiManager) {
        Logger.d("NetworkSettingActivity", "getMacAddress 0001");
        String macAddress = "";
        try {
            ContentResolver cr = getContentResolver();
            macAddress = Settings.System.getString(cr, "bc:mac_address");
        } catch (Exception e) {
            Logger.d("NetworkSettingActivity", "getMacAddress 0002");
            Logger.d("NetworkSettingActivity", "Exception", e);
        }
        if (TextUtils.isEmpty(macAddress)) {
            try {
                Logger.d("NetworkSettingActivity", "getMacAddress 0003");
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    Logger.d("NetworkSettingActivity", "getMacAddress 0004");
                    macAddress = wifiInfo.getMacAddress();
                }
            } catch (Exception e2) {
                Logger.d("NetworkSettingActivity", "getMacAddress 0005");
                Logger.d("NetworkSettingActivity", "Exception", e2);
            }
        }
        Logger.d("NetworkSettingActivity", "getMacAddress 0006 return:", macAddress);
        return macAddress;
    }

    private String getOnAccessSsid(WifiManager manager) {
        Logger.d("NetworkSettingActivity", "getOnAccessSsid 0001");
        WifiInfo info = manager.getConnectionInfo();
        Logger.d("NetworkSettingActivity", "getOnAccessSsid 0002");
        return info.getSSID();
    }
}
