package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.text.TextUtils;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import jp.co.benesse.dcha.util.FileUtils;
import jp.co.benesse.dcha.util.Logger;

public class HealthCheckLogic {
    public void getMacAddress(WifiInfo wifiInfo, HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckLogic", "onCreate 0001");
        if (wifiInfo != null) {
            Logger.d("HealthCheckLogic", "onCreate 0002");
            healthCheckDto.myMacaddress = wifiInfo.getMacAddress();
        } else {
            Logger.d("HealthCheckLogic", "onCreate 0003");
            healthCheckDto.myMacaddress = "";
        }
        Logger.d("HealthCheckLogic", "onCreate 0004");
    }

    public void checkSsid(Context context, List<WifiConfiguration> cfgList, HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckLogic", "checkSsid 0001");
        String getSsid = null;
        if (cfgList != null) {
            Logger.d("HealthCheckLogic", "checkSsid 0002");
            Iterator<WifiConfiguration> it = cfgList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                WifiConfiguration wifiConfiguration = it.next();
                if (wifiConfiguration.status == 0) {
                    Logger.d("HealthCheckLogic", "checkSsid 0003");
                    getSsid = wifiConfiguration.SSID;
                    break;
                }
                getSsid = wifiConfiguration.SSID;
            }
            getSsid = parseSsid(getSsid);
        }
        if (TextUtils.isEmpty(getSsid) || context.getString(R.string.unknown_ssid).equals(getSsid)) {
            Logger.d("HealthCheckLogic", "checkSsid 0004");
            healthCheckDto.isCheckedSsid = R.string.health_check_ng;
            healthCheckDto.mySsid = context.getString(R.string.health_check_ng);
        } else {
            Logger.d("HealthCheckLogic", "checkSsid 0005");
            healthCheckDto.mySsid = getSsid;
            healthCheckDto.isCheckedSsid = R.string.health_check_ok;
        }
        Logger.d("HealthCheckLogic", "checkSsid 0006");
    }

    public void checkWifi(WifiInfo wifiInf, HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckLogic", "checkWifi 0001");
        if (wifiInf != null && SupplicantState.COMPLETED.equals(wifiInf.getSupplicantState())) {
            Logger.d("HealthCheckLogic", "checkWifi 0002");
            healthCheckDto.isCheckedWifi = R.string.health_check_ok;
        } else {
            Logger.d("HealthCheckLogic", "checkWifi 0003");
            healthCheckDto.isCheckedWifi = R.string.health_check_ng;
        }
        Logger.d("HealthCheckLogic", "checkWifi 0004");
    }

    public void checkIpAddress(Context context, DhcpInfo dhcpInfo, HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckLogic", "checkIpAddress 0001");
        if (dhcpInfo != null && dhcpInfo.ipAddress != 0) {
            Logger.d("HealthCheckLogic", "checkIpAddress 0002");
            healthCheckDto.myIpAddress = parseAddress(dhcpInfo.ipAddress);
            healthCheckDto.mySubnetMask = parseAddress(getNetmask(context, dhcpInfo.ipAddress));
            healthCheckDto.myDefaultGateway = parseAddress(dhcpInfo.gateway);
            healthCheckDto.myDns1 = parseAddress(dhcpInfo.dns1);
            healthCheckDto.myDns2 = parseAddress(dhcpInfo.dns2);
            healthCheckDto.isCheckedIpAddress = R.string.health_check_ok;
        } else {
            Logger.d("HealthCheckLogic", "checkIpAddress 0003");
            healthCheckDto.myIpAddress = context.getString(R.string.health_check_ng);
            healthCheckDto.isCheckedIpAddress = R.string.health_check_ng;
        }
        Logger.d("HealthCheckLogic", "checkIpAddress 0004");
    }

    private int getNetmask(Context context, int ipAddress) {
        Logger.d("HealthCheckLogic", "getNetmask 0001");
        int ret = 0;
        try {
            byte[] ip = ByteBuffer.allocate(4).putInt(ipAddress).array();
            int j = ip.length - 1;
            for (int i = 0; j > i; i++) {
                byte tmp = ip[j];
                ip[j] = ip[i];
                ip[i] = tmp;
                j--;
            }
            InetAddress inetAddress = InetAddress.getByAddress(ip);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                Logger.d("HealthCheckLogic", "getNetmask 0002");
                short netPrefix = address.getNetworkPrefixLength();
                if (netPrefix >= 0 && netPrefix <= 32) {
                    Logger.d("HealthCheckLogic", "getNetmask 0003");
                    ret = Integer.reverseBytes((-1) << (32 - netPrefix));
                }
            }
        } catch (Exception e) {
            Logger.e("HealthCheckLogic", "getNetmask 0004");
        }
        Logger.e("HealthCheckLogic", "getNetmask 0005");
        return ret;
    }

    public void checkNetConnection(HealthChkMngDto healthChkMngDto, HealthCheckDto healthCheckDto) {
        Logger.d("HealthCheckLogic", "checkNetConnection 0001");
        if (healthChkMngDto != null) {
            Logger.d("HealthCheckLogic", "checkNetConnection 0002");
            String url = healthChkMngDto.url;
            int timeout = healthChkMngDto.timeout;
            ExecuteHttpTask executeHttpTask = getExecuteHttpTask(url, timeout);
            executeHttpTask.execute();
            HttpResponse response = executeHttpTask.getResponse();
            if (response != null) {
                Logger.d("HealthCheckLogic", "checkNetConnection 0003");
                healthCheckDto.isCheckedNetConnection = R.string.health_check_ok;
            } else {
                Logger.d("HealthCheckLogic", "checkNetConnection 0004");
                healthCheckDto.isCheckedNetConnection = R.string.health_check_ng;
            }
        } else {
            Logger.d("HealthCheckLogic", "checkNetConnection 0005");
            healthCheckDto.isCheckedNetConnection = R.string.health_check_ng;
        }
        Logger.d("HealthCheckLogic", "checkNetConnection 0006");
    }

    public void checkDownloadSpeed(Context context, HealthChkMngDto healthChkMngDto, HealthCheckDto healthCheckDto) throws Throwable {
        BufferedInputStream bufferedInputStream;
        Logger.d("HealthCheckLogic", "checkDownloadSpeed 0001");
        long lastSuccessTime = 0;
        long lastSuccessSize = 0;
        String[] urlList = getUrlList(healthChkMngDto);
        if (urlList != null) {
            Logger.d("HealthCheckLogic", "checkDownloadSpeed 0002");
            long processBegin = System.currentTimeMillis();
            int currentIndex = healthChkMngDto.url.lastIndexOf("/");
            String currentUrl = healthChkMngDto.url.substring(0, currentIndex + 1);
            InputStream inputStream = null;
            BufferedInputStream bufferedInputStream2 = null;
            byte[] buffer = new byte[1024];
            int timeout = Integer.parseInt(urlList[0]) * 1000;
            try {
                try {
                    int numberOfLine = urlList.length;
                    int i = 1;
                    loop0: while (true) {
                        BufferedInputStream bufferedInputStream3 = bufferedInputStream2;
                        if (i >= numberOfLine) {
                            bufferedInputStream = bufferedInputStream3;
                            break;
                        }
                        try {
                            URL url = new URL(currentUrl + urlList[i]);
                            URLConnection connection = url.openConnection();
                            connection.setConnectTimeout(timeout);
                            connection.setReadTimeout(timeout);
                            connection.connect();
                            inputStream = connection.getInputStream();
                            bufferedInputStream = new BufferedInputStream(inputStream, 1024);
                            long startTime = System.currentTimeMillis();
                            long fileSize = 0;
                            while (true) {
                                int len = bufferedInputStream.read(buffer);
                                if (len == -1) {
                                    break;
                                }
                                fileSize += (long) len;
                                if (healthCheckDto.isCancel()) {
                                    Logger.d("HealthCheckLogic", "checkDownloadSpeed 0003");
                                    break loop0;
                                } else if (timeout < ((int) (System.currentTimeMillis() - processBegin))) {
                                    Logger.d("HealthCheckLogic", "checkDownloadSpeed 0004");
                                    break loop0;
                                }
                            }
                        } catch (Exception e) {
                            e = e;
                            bufferedInputStream2 = bufferedInputStream3;
                            Logger.d("HealthCheckLogic", "checkDownloadSpeed 0005", e);
                            FileUtils.close(inputStream);
                            FileUtils.close(bufferedInputStream2);
                        } catch (Throwable th) {
                            th = th;
                            bufferedInputStream2 = bufferedInputStream3;
                            FileUtils.close(inputStream);
                            FileUtils.close(bufferedInputStream2);
                            throw th;
                        }
                        bufferedInputStream2 = null;
                        i++;
                    }
                    FileUtils.close(inputStream);
                    FileUtils.close(bufferedInputStream);
                } catch (Exception e2) {
                    e = e2;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        getDSpeedResult(context, healthCheckDto, lastSuccessTime, lastSuccessSize);
        Logger.d("HealthCheckLogic", "checkDownloadSpeed 0006");
    }

    public String parseSsid(String string) {
        Logger.d("HealthCheckLogic", "parseSsid 0001");
        if (string != null) {
            int length = string.length();
            if (string.startsWith("0x")) {
                Logger.d("HealthCheckLogic", "parseSsid 0001");
                return string.replaceFirst("0x", "");
            }
            if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
                Logger.d("HealthCheckLogic", "parseSsid 0002");
                return string.substring(1, length - 1);
            }
        }
        Logger.d("HealthCheckLogic", "parseSsid 0003");
        return string;
    }

    public String parseAddress(int address) {
        Logger.d("HealthCheckLogic", "parseAddress 0001");
        String returnAddress = "";
        if (address != 0) {
            Logger.d("HealthCheckLogic", "parseAddress 0002");
            int classA = (address >> 0) & 255;
            int classB = (address >> 8) & 255;
            int classC = (address >> 16) & 255;
            int classD = (address >> 24) & 255;
            returnAddress = classA + "." + classB + "." + classC + "." + classD;
        }
        Logger.d("HealthCheckLogic", "parseAddress 0003");
        return returnAddress;
    }

    public String[] getUrlList(HealthChkMngDto healthChkMngDto) {
        Logger.d("HealthCheckLogic", "getUrlList 0001");
        String[] urlList = null;
        if (healthChkMngDto != null) {
            Logger.d("HealthCheckLogic", "getUrlList 0002");
            String url = healthChkMngDto.url;
            int timeout = healthChkMngDto.timeout;
            ExecuteHttpTask executeHttpTask = getExecuteHttpTask(url, timeout);
            executeHttpTask.execute();
            HttpResponse response = executeHttpTask.getResponse();
            if (response != null) {
                Logger.d("HealthCheckLogic", "getUrlList 0003");
                try {
                    urlList = response.getEntity().split("\n");
                    Integer.parseInt(urlList[0]);
                    if (urlList.length < 2) {
                        Logger.d("HealthCheckLogic", "getUrlList 0004");
                        urlList = null;
                    }
                } catch (Exception e) {
                    Logger.d("HealthCheckLogic", "getUrlList 0005", e);
                    urlList = null;
                }
            }
        }
        Logger.d("HealthCheckLogic", "getUrlList 0006");
        return urlList;
    }

    public void getDSpeedResult(Context context, HealthCheckDto healthCheckDto, long lastSuccessTime, long lastSuccessSize) {
        Logger.d("HealthCheckLogic", "getDSpeedResult 0001");
        if (lastSuccessTime == 0) {
            Logger.d("HealthCheckLogic", "getDSpeedResult 0002");
            lastSuccessTime = 1;
        }
        long downloadSpeed = (8 * lastSuccessSize) / lastSuccessTime;
        if (downloadSpeed < Integer.parseInt(context.getString(R.string.mast_download_speed))) {
            Logger.d("HealthCheckLogic", "getDSpeedResult 0003");
            healthCheckDto.myDownloadSpeed = R.string.h_check_low_speed;
            healthCheckDto.myDSpeedImage = R.drawable.health_check_speed_low;
        } else if (downloadSpeed < Integer.parseInt(context.getString(R.string.recommended_d_speed))) {
            Logger.d("HealthCheckLogic", "getDSpeedResult 0004");
            healthCheckDto.myDownloadSpeed = R.string.h_check_middle_speed;
            healthCheckDto.myDSpeedImage = R.drawable.health_check_speed_middle;
        } else {
            Logger.d("HealthCheckLogic", "getDSpeedResult 0005");
            healthCheckDto.myDownloadSpeed = R.string.h_check_high_speed;
            healthCheckDto.myDSpeedImage = R.drawable.health_check_speed_high;
        }
        healthCheckDto.isCheckedDSpeed = R.string.health_check_ok;
        Logger.d("HealthCheckLogic", "getDSpeedResult 0006");
    }

    public ExecuteHttpTask getExecuteHttpTask(String url, int timeout) {
        Logger.d("HealthCheckLogic", "getExecuteHttpTask 0001");
        ExecuteHttpTask executeHttpTask = new ExecuteHttpTask(url, timeout);
        return executeHttpTask;
    }
}
