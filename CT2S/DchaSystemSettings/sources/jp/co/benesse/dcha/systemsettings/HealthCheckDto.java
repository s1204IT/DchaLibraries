package jp.co.benesse.dcha.systemsettings;

import java.io.Serializable;
import jp.co.benesse.dcha.util.Logger;

public class HealthCheckDto implements Serializable {
    private static final long serialVersionUID = 4031444211707627400L;
    public int isHealthChecked = R.string.health_check_pending;
    private boolean cancelFlag = false;
    public int isCheckedSsid = R.string.health_check_pending;
    public String myMacaddress = "";
    public String mySsid = "";
    public int isCheckedWifi = R.string.health_check_pending;
    public int isCheckedIpAddress = R.string.health_check_pending;
    public String myIpAddress = "";
    public String mySubnetMask = "";
    public String myDefaultGateway = "";
    public String myDns1 = "";
    public String myDns2 = "";
    public int isCheckedNetConnection = R.string.health_check_pending;
    public int isCheckedDSpeed = R.string.health_check_pending;
    public int myDownloadSpeed = R.string.h_check_low_speed;
    public int myDSpeedImage = R.drawable.health_check_speed_low;

    public synchronized boolean isCancel() {
        Logger.d("HealthCheckDto", "isCancel 0001");
        return this.cancelFlag;
    }

    public synchronized void cancel() {
        Logger.d("HealthCheckDto", "cancel 0001");
        this.cancelFlag = true;
    }
}
