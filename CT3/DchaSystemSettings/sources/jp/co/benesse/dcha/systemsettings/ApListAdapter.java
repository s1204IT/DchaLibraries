package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import jp.co.benesse.dcha.util.Logger;

public class ApListAdapter extends ArrayAdapter<AccessPoint> {
    private static Typeface mTypeFace;
    private TextView mApName;
    private TextView mApNameTitle;
    private ImageView mApRssi;
    private TextView mApSecurity;
    private Context mContext;
    private ImageView mIconLocked;
    private LayoutInflater mInflater;
    private String mOnAccessSsid;
    private ImageView mWifinetworkChecked;

    public ApListAdapter(Context context, int resource, List<AccessPoint> accessPoints, String onAccessSsid) {
        super(context, resource, accessPoints);
        Logger.d("ApListAdapter", "ApListAdapter 0001");
        this.mOnAccessSsid = onAccessSsid;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mContext = context;
        Logger.d("ApListAdapter", "ApListAdapter 0002");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Logger.d("ApListAdapter", "getView 0001");
        View v = convertView;
        if (convertView == null) {
            Logger.d("ApListAdapter", "getView 0002");
            v = this.mInflater.inflate(R.layout.wifi_ap_list, (ViewGroup) null);
        }
        AccessPoint accessPoint = getItem(position);
        String ssid = accessPoint.getSsidStr();
        String securityMsg = accessPoint.getSummary();
        this.mApNameTitle = (TextView) v.findViewById(R.id.tv_ap_name_title);
        this.mApName = (TextView) v.findViewById(R.id.tv_ap_name);
        this.mApSecurity = (TextView) v.findViewById(R.id.tv_ap_security);
        this.mWifinetworkChecked = (ImageView) v.findViewById(R.id.wifinetwork_checked);
        this.mApName.setText(ssid);
        this.mApSecurity.setText(securityMsg);
        try {
            if (ParentSettingActivity.canReadSystemFont()) {
                if (mTypeFace == null) {
                    mTypeFace = Typeface.createFromFile("system/fonts/gjsgm.ttf");
                }
                this.mApNameTitle.setTypeface(mTypeFace);
                this.mApName.setTypeface(mTypeFace);
                this.mApSecurity.setTypeface(mTypeFace);
            }
        } catch (RuntimeException e) {
        }
        if (ssid.equals(this.mOnAccessSsid.substring(1, this.mOnAccessSsid.length() - 1)) && ((NetworkSettingActivity) this.mContext).getResources().getString(R.string.wifi_status_connected).equals(this.mApSecurity.getText().toString())) {
            Logger.d("ApListAdapter", "getView 0003");
            v.setBackgroundResource(R.drawable.background);
            this.mApNameTitle.setTextColor(Color.parseColor("#0056a2"));
            this.mApName.setTextColor(Color.parseColor("#0056a2"));
            this.mApSecurity.setTextColor(Color.parseColor("#0056a2"));
            this.mWifinetworkChecked.setVisibility(0);
        } else {
            Logger.d("ApListAdapter", "getView 0004");
            v.setBackgroundColor(-1);
            this.mApNameTitle.setTextColor(-16777216);
            this.mApName.setTextColor(-16777216);
            this.mApSecurity.setTextColor(-16777216);
            this.mWifinetworkChecked.setVisibility(4);
        }
        this.mApRssi = (ImageView) v.findViewById(R.id.img_wifi_level);
        this.mIconLocked = (ImageView) v.findViewById(R.id.icon_locked);
        int level = accessPoint.getLevel();
        if (level < 0) {
            Logger.d("ApListAdapter", "getView 0005");
            this.mApRssi.setImageDrawable(null);
            this.mIconLocked.setImageDrawable(null);
        } else {
            Logger.d("ApListAdapter", "getView 0006");
            this.mApRssi.setImageLevel(level);
            this.mApRssi.setImageResource(R.drawable.wifi_signal);
            if (accessPoint.getSecurity() != 0) {
                Logger.d("ApListAdapter", "getView 0007");
                this.mIconLocked.setImageResource(R.drawable.icon_locked);
            } else {
                Logger.d("ApListAdapter", "getView 0008");
                this.mIconLocked.setImageDrawable(null);
            }
        }
        Logger.d("ApListAdapter", "getView 0009");
        return v;
    }

    public boolean updateAccessPoint(AccessPoint src) {
        Logger.d("ApListAdapter", "updateAccessPoint 0001");
        String ssid = src.getSsidStr();
        int count = getCount();
        for (int index = 0; index < count; index++) {
            AccessPoint target = getItem(index);
            if (TextUtils.equals(ssid, target.getSsidStr())) {
                Logger.d("ApListAdapter", "updateAccessPoint 0002");
                return updateAccessPoint(src, target);
            }
        }
        Logger.d("ApListAdapter", "updateAccessPoint 0003");
        return false;
    }

    private boolean updateAccessPoint(AccessPoint src, AccessPoint target) {
        Logger.d("ApListAdapter", "updateAccessPoint 0004");
        if (target.equals(src)) {
            Logger.d("ApListAdapter", "updateAccessPoint 0005");
            return true;
        }
        WifiConfiguration config = src.getConfig();
        WifiInfo wifi = src.getInfo();
        NetworkInfo network = src.getNetworkInfo();
        if (target.update(config, wifi, network)) {
            Logger.d("ApListAdapter", "updateAccessPoint 0006");
            notifyDataSetChanged();
            return true;
        }
        Logger.d("ApListAdapter", "updateAccessPoint 0007");
        return false;
    }
}
