package com.mediatek.camera.common.mode;

import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import java.util.ArrayList;

public class DeviceUsage {
    private final CameraDeviceManagerFactory.CameraApi mCameraApi;
    private String mDeviceType;
    private final ArrayList<String> mNeedOpenedCameraIdList;

    public DeviceUsage(String str, CameraDeviceManagerFactory.CameraApi cameraApi, ArrayList<String> arrayList) {
        this.mDeviceType = str;
        this.mCameraApi = cameraApi;
        this.mNeedOpenedCameraIdList = new ArrayList<>(arrayList);
    }

    public String getDeviceType() {
        return this.mDeviceType;
    }

    public CameraDeviceManagerFactory.CameraApi getCameraApi() {
        return this.mCameraApi;
    }

    public ArrayList<String> getCameraIdList() {
        return new ArrayList<>(this.mNeedOpenedCameraIdList);
    }

    public ArrayList<String> getNeedClosedCameraIds(DeviceUsage deviceUsage) {
        ArrayList<String> cameraIdList = getCameraIdList();
        if (deviceUsage == null || !this.mCameraApi.equals(deviceUsage.getCameraApi()) || !this.mDeviceType.equals(deviceUsage.getDeviceType())) {
            return cameraIdList;
        }
        boolean z = false;
        if (!(cameraIdList.size() == deviceUsage.getCameraIdList().size())) {
            return cameraIdList;
        }
        int i = 0;
        while (true) {
            if (i < cameraIdList.size()) {
                if (!deviceUsage.getCameraIdList().contains(cameraIdList.get(i))) {
                    break;
                }
                i++;
            } else {
                z = true;
                break;
            }
        }
        if (z) {
            return new ArrayList<>();
        }
        return cameraIdList;
    }
}
