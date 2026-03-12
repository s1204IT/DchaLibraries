#!/system/bin/sh

#mlanutl wlan0 hostcmd /res/wifi/ed_mac_ctrl_8801.conf ed_mac_ctrl
mlanutl wlan0 hscfg 0
mlanutl wlan0 httxcfg 0x20
mlanutl wlan0 htcapinfo 0x00800000
