#!/system/bin/sh

mlanutl wlan0 hostcmd /res/wifi/ed_mac_ctrl.conf ed_mac_ctrl_v2
mlanutl wlan0 hscfg 0
mlanutl wlan0 httxcfg 0x62
mlanutl wlan0 htcapinfo 0x01820000
