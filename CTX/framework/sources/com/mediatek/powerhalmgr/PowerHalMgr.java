package com.mediatek.powerhalmgr;

public class PowerHalMgr {
    public static final int CMD_GET_BOOST_TIMEOUT = 16;
    public static final int CMD_GET_CLUSTER_CPU_FREQ_MAX = 4;
    public static final int CMD_GET_CLUSTER_CPU_FREQ_MIN = 3;
    public static final int CMD_GET_CLUSTER_CPU_NUM = 2;
    public static final int CMD_GET_CLUSTER_NUM = 1;
    public static final int CMD_GET_CPU_TOPOLOGY = 18;
    public static final int CMD_GET_DEBUG_DUMP_ALL = 20;
    public static final int CMD_GET_DEBUG_SET_LVL = 19;
    public static final int CMD_GET_EXT_LAUNCH_DURATION = 9;
    public static final int CMD_GET_EXT_LAUNCH_DURATION_1 = 11;
    public static final int CMD_GET_EXT_LAUNCH_DURATION_2 = 13;
    public static final int CMD_GET_EXT_LAUNCH_DURATION_3 = 15;
    public static final int CMD_GET_EXT_LAUNCH_FOLLOW = 8;
    public static final int CMD_GET_EXT_LAUNCH_FOLLOW_1 = 10;
    public static final int CMD_GET_EXT_LAUNCH_FOLLOW_2 = 12;
    public static final int CMD_GET_EXT_LAUNCH_FOLLOW_3 = 14;
    public static final int CMD_GET_EXT_PEAK_PERIOD = 17;
    public static final int CMD_GET_FOREGROUND_PID = 6;
    public static final int CMD_GET_FOREGROUND_TYPE = 7;
    public static final int CMD_GET_GPU_FREQ_COUNT = 5;
    public static final int CMD_GET_LOAD_TRACKING = 21;
    public static final int CMD_SET_APU_BOOST_VAL = 87;
    public static final int CMD_SET_APU_BW_THRES = 86;
    public static final int CMD_SET_APU_CORE_MAX = 84;
    public static final int CMD_SET_APU_CORE_MIN = 83;
    public static final int CMD_SET_APU_FREQ_MAX = 82;
    public static final int CMD_SET_APU_FREQ_MIN = 81;
    public static final int CMD_SET_APU_PERF_MODE = 85;
    public static final int CMD_SET_BG_BOOST_VALUE = 37;
    public static final int CMD_SET_CLUSTER_CPU_CORE_MAX = 2;
    public static final int CMD_SET_CLUSTER_CPU_CORE_MIN = 1;
    public static final int CMD_SET_CLUSTER_CPU_FREQ_MAX = 4;
    public static final int CMD_SET_CLUSTER_CPU_FREQ_MAX_HL = 94;
    public static final int CMD_SET_CLUSTER_CPU_FREQ_MIN = 3;
    public static final int CMD_SET_CLUSTER_CPU_FREQ_MIN_HL = 93;
    public static final int CMD_SET_CM_MGR = 80;
    public static final int CMD_SET_CPUFREQ_ABOVE_HISPEED_DELAY = 14;
    public static final int CMD_SET_CPUFREQ_HISPEED_FREQ = 12;
    public static final int CMD_SET_CPUFREQ_MIN_SAMPLE_TIME = 13;
    public static final int CMD_SET_CPU_PERF_MODE = 5;
    public static final int CMD_SET_DCM_MODE = 52;
    public static final int CMD_SET_DCS_MODE = 53;
    public static final int CMD_SET_DFPS = 40;
    public static final int CMD_SET_DISP_DECOUPLE = 68;
    public static final int CMD_SET_DISP_IDLE_TIME = 79;
    public static final int CMD_SET_DVFS_POWER_MODE = 15;
    public static final int CMD_SET_END_JUMP = 99;
    public static final int CMD_SET_EXT_LAUNCH_FOLLOW = 88;
    public static final int CMD_SET_EXT_LAUNCH_FOLLOW_1 = 89;
    public static final int CMD_SET_EXT_LAUNCH_FOLLOW_2 = 90;
    public static final int CMD_SET_EXT_LAUNCH_FOLLOW_3 = 91;
    public static final int CMD_SET_EXT_LAUNCH_MON = 63;
    public static final int CMD_SET_EXT_PEAK_PERIOD = 92;
    public static final int CMD_SET_FBT_BHR_OPP = 72;
    public static final int CMD_SET_FBT_FLOOR_BOUND = 59;
    public static final int CMD_SET_FBT_KMIN = 60;
    public static final int CMD_SET_FG_BOOST_VALUE = 36;
    public static final int CMD_SET_FPSGO_ENABLE = 55;
    public static final int CMD_SET_FSBT_SOFT_FPS = 71;
    public static final int CMD_SET_FSTB_FORCE_VAG = 56;
    public static final int CMD_SET_FSTB_FPS = 54;
    public static final int CMD_SET_GED_BENCHMARK_ON = 57;
    public static final int CMD_SET_GPU_FREQ_MAX = 7;
    public static final int CMD_SET_GPU_FREQ_MAX_HL = 96;
    public static final int CMD_SET_GPU_FREQ_MIN = 6;
    public static final int CMD_SET_GPU_FREQ_MIN_HL = 95;
    public static final int CMD_SET_GX_BOOST = 58;
    public static final int CMD_SET_HPS_DOWN_THRESHOLD = 17;
    public static final int CMD_SET_HPS_DOWN_TIMES = 19;
    public static final int CMD_SET_HPS_HEAVY_TASK = 21;
    public static final int CMD_SET_HPS_POWER_MODE = 22;
    public static final int CMD_SET_HPS_RBOOST_THRESH = 49;
    public static final int CMD_SET_HPS_RUSH_BOOST = 20;
    public static final int CMD_SET_HPS_UP_THRESHOLD = 16;
    public static final int CMD_SET_HPS_UP_TIMES = 18;
    public static final int CMD_SET_IDLE_PREFER = 31;
    public static final int CMD_SET_IO_BOOST_VALUE = 69;
    public static final int CMD_SET_LOAD_TRACKING = 97;
    public static final int CMD_SET_MTK_PREFER_IDLE = 65;
    public static final int CMD_SET_OPP_DDR = 62;
    public static final int CMD_SET_PACK_BOOST_MODE = 38;
    public static final int CMD_SET_PACK_BOOST_TIMEOUT = 39;
    public static final int CMD_SET_PPM_HICA_VAR = 26;
    public static final int CMD_SET_PPM_HOLD_TIME_LL_ONLY = 48;
    public static final int CMD_SET_PPM_HOLD_TIME_L_ONLY = 42;
    public static final int CMD_SET_PPM_LIMIT_BIG = 46;
    public static final int CMD_SET_PPM_MODE = 25;
    public static final int CMD_SET_PPM_ROOT_CLUSTER = 23;
    public static final int CMD_SET_PPM_SPORTS_MODE = 47;
    public static final int CMD_SET_ROOT_BOOST_VALUE = 34;
    public static final int CMD_SET_SCHED_AFFINITY = 76;
    public static final int CMD_SET_SCHED_AVG_HTASK_AC = 28;
    public static final int CMD_SET_SCHED_AVG_HTASK_THRESH = 29;
    public static final int CMD_SET_SCHED_BOOST = 73;
    public static final int CMD_SET_SCHED_HTASK_THRESH = 27;
    public static final int CMD_SET_SCHED_ISO = 78;
    public static final int CMD_SET_SCHED_LB_ENABLE = 32;
    public static final int CMD_SET_SCHED_LOAD_LIGHT_FILTER = 77;
    public static final int CMD_SET_SCHED_MIGR_COST = 74;
    public static final int CMD_SET_SCHED_MODE = 30;
    public static final int CMD_SET_SCHED_TUNE_BOOST = 75;
    public static final int CMD_SET_SCREEN_OFF_STATE = 11;
    public static final int CMD_SET_SMART_FORCE_ISOLATE = 50;
    public static final int CMD_SET_SPORTS_MODE = 41;
    public static final int CMD_SET_STUNE_FG_PERFER_IDLE = 67;
    public static final int CMD_SET_STUNE_TA_PERFER_IDLE = 66;
    public static final int CMD_SET_STUNE_THRESH = 51;
    public static final int CMD_SET_TA_BOOST_VALUE = 35;
    public static final int CMD_SET_THERMAL_POLICY = 98;
    public static final int CMD_SET_UNUSED_1 = 24;
    public static final int CMD_SET_UNUSED_2 = 33;
    public static final int CMD_SET_UNUSED_3 = 64;
    public static final int CMD_SET_VCORE = 45;
    public static final int CMD_SET_VCORE_BW_ENABLED = 9;
    public static final int CMD_SET_VCORE_BW_THRES = 8;
    public static final int CMD_SET_VCORE_BW_THRES_DDR3 = 43;
    public static final int CMD_SET_VCORE_MIN = 10;
    public static final int CMD_SET_VCORE_MIN_DDR3 = 44;
    public static final int CMD_SET_VIDEO_MODE = 61;
    public static final int CMD_SET_WIPHY_CAM = 70;
    public static final int DFPS_MODE_ARR = 2;
    public static final int DFPS_MODE_DEFAULT = 0;
    public static final int DFPS_MODE_FRR = 1;
    public static final int DFPS_MODE_INTERNAL_SW = 3;
    public static final int DFPS_MODE_MAXIMUM = 4;
    public static final int DISP_MODE_DEFAULT = 0;
    public static final int DISP_MODE_EN = 1;
    public static final int DISP_MODE_NUM = 2;
    public static final int MTK_HINT_ALWAYS_ENABLE = 268435455;
    public static final int SCREEN_OFF_DISABLE = 0;
    public static final int SCREEN_OFF_ENABLE = 1;
    public static final int SCREEN_OFF_WAIT_RESTORE = 2;
    public static final int STATE_DEAD = 3;
    public static final int STATE_DESTORYED = 2;
    public static final int STATE_PAUSED = 0;
    public static final int STATE_RESUMED = 1;
    public static final int STATE_STOPPED = 4;

    public int scnReg() {
        return -1;
    }

    public void scnConfig(int i, int i2, int i3, int i4, int i5, int i6) {
    }

    public void scnUnreg(int i) {
    }

    public void scnEnable(int i, int i2) {
    }

    public void scnDisable(int i) {
    }

    public void scnUltraCfg(int i, int i2, int i3, int i4, int i5, int i6) {
    }

    public void mtkCusPowerHint(int i, int i2) {
    }
}
