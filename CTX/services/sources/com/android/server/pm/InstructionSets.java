package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArraySet;
import dalvik.system.VMRuntime;
import java.util.ArrayList;
import java.util.List;

public class InstructionSets {
    private static final String PREFERRED_INSTRUCTION_SET = VMRuntime.getInstructionSet(Build.SUPPORTED_ABIS[0]);

    public static String[] getAppDexInstructionSets(ApplicationInfo applicationInfo) {
        if (applicationInfo.primaryCpuAbi != null) {
            if (applicationInfo.secondaryCpuAbi != null) {
                return new String[]{VMRuntime.getInstructionSet(applicationInfo.primaryCpuAbi), VMRuntime.getInstructionSet(applicationInfo.secondaryCpuAbi)};
            }
            return new String[]{VMRuntime.getInstructionSet(applicationInfo.primaryCpuAbi)};
        }
        return new String[]{getPreferredInstructionSet()};
    }

    public static String[] getAppDexInstructionSets(PackageSetting packageSetting) {
        if (packageSetting.primaryCpuAbiString != null) {
            if (packageSetting.secondaryCpuAbiString != null) {
                return new String[]{VMRuntime.getInstructionSet(packageSetting.primaryCpuAbiString), VMRuntime.getInstructionSet(packageSetting.secondaryCpuAbiString)};
            }
            return new String[]{VMRuntime.getInstructionSet(packageSetting.primaryCpuAbiString)};
        }
        return new String[]{getPreferredInstructionSet()};
    }

    public static String getPreferredInstructionSet() {
        return PREFERRED_INSTRUCTION_SET;
    }

    public static String getDexCodeInstructionSet(String str) {
        String str2 = SystemProperties.get("ro.dalvik.vm.isa." + str);
        return TextUtils.isEmpty(str2) ? str : str2;
    }

    public static String[] getDexCodeInstructionSets(String[] strArr) {
        ArraySet arraySet = new ArraySet(strArr.length);
        for (String str : strArr) {
            arraySet.add(getDexCodeInstructionSet(str));
        }
        return (String[]) arraySet.toArray(new String[arraySet.size()]);
    }

    public static String[] getAllDexCodeInstructionSets() {
        String[] strArr = new String[Build.SUPPORTED_ABIS.length];
        for (int i = 0; i < strArr.length; i++) {
            strArr[i] = VMRuntime.getInstructionSet(Build.SUPPORTED_ABIS[i]);
        }
        return getDexCodeInstructionSets(strArr);
    }

    public static List<String> getAllInstructionSets() {
        String[] strArr = Build.SUPPORTED_ABIS;
        ArrayList arrayList = new ArrayList(strArr.length);
        for (String str : strArr) {
            String instructionSet = VMRuntime.getInstructionSet(str);
            if (!arrayList.contains(instructionSet)) {
                arrayList.add(instructionSet);
            }
        }
        return arrayList;
    }

    public static String getPrimaryInstructionSet(ApplicationInfo applicationInfo) {
        if (applicationInfo.primaryCpuAbi == null) {
            return getPreferredInstructionSet();
        }
        return VMRuntime.getInstructionSet(applicationInfo.primaryCpuAbi);
    }
}
