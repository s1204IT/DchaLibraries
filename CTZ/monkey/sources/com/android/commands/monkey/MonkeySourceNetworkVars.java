package com.android.commands.monkey;

import android.hardware.display.DisplayManagerGlobal;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import com.android.commands.monkey.MonkeySourceNetwork;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MonkeySourceNetworkVars {
    private static final Map<String, VarGetter> VAR_MAP = new TreeMap();

    private interface VarGetter {
        String get();
    }

    private static class StaticVarGetter implements VarGetter {
        private final String value;

        public StaticVarGetter(String str) {
            this.value = str;
        }

        @Override
        public String get() {
            return this.value;
        }
    }

    static {
        VAR_MAP.put("build.board", new StaticVarGetter(Build.BOARD));
        VAR_MAP.put("build.brand", new StaticVarGetter(Build.BRAND));
        VAR_MAP.put("build.device", new StaticVarGetter(Build.DEVICE));
        VAR_MAP.put("build.display", new StaticVarGetter(Build.DISPLAY));
        VAR_MAP.put("build.fingerprint", new StaticVarGetter(Build.FINGERPRINT));
        VAR_MAP.put("build.host", new StaticVarGetter(Build.HOST));
        VAR_MAP.put("build.id", new StaticVarGetter(Build.ID));
        VAR_MAP.put("build.model", new StaticVarGetter(Build.MODEL));
        VAR_MAP.put("build.product", new StaticVarGetter(Build.PRODUCT));
        VAR_MAP.put("build.tags", new StaticVarGetter(Build.TAGS));
        VAR_MAP.put("build.brand", new StaticVarGetter(Long.toString(Build.TIME)));
        VAR_MAP.put("build.type", new StaticVarGetter(Build.TYPE));
        VAR_MAP.put("build.user", new StaticVarGetter(Build.USER));
        VAR_MAP.put("build.cpu_abi", new StaticVarGetter(Build.CPU_ABI));
        VAR_MAP.put("build.manufacturer", new StaticVarGetter(Build.MANUFACTURER));
        VAR_MAP.put("build.version.incremental", new StaticVarGetter(Build.VERSION.INCREMENTAL));
        VAR_MAP.put("build.version.release", new StaticVarGetter(Build.VERSION.RELEASE));
        VAR_MAP.put("build.version.sdk", new StaticVarGetter(Integer.toString(Build.VERSION.SDK_INT)));
        VAR_MAP.put("build.version.codename", new StaticVarGetter(Build.VERSION.CODENAME));
        Display realDisplay = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        VAR_MAP.put("display.width", new StaticVarGetter(Integer.toString(realDisplay.getWidth())));
        VAR_MAP.put("display.height", new StaticVarGetter(Integer.toString(realDisplay.getHeight())));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        realDisplay.getMetrics(displayMetrics);
        VAR_MAP.put("display.density", new StaticVarGetter(Float.toString(displayMetrics.density)));
        VAR_MAP.put("am.current.package", new VarGetter() {
            @Override
            public String get() {
                return Monkey.currentPackage;
            }
        });
        VAR_MAP.put("am.current.action", new VarGetter() {
            @Override
            public String get() {
                if (Monkey.currentIntent == null) {
                    return null;
                }
                return Monkey.currentIntent.getAction();
            }
        });
        VAR_MAP.put("am.current.comp.class", new VarGetter() {
            @Override
            public String get() {
                if (Monkey.currentIntent == null) {
                    return null;
                }
                return Monkey.currentIntent.getComponent().getClassName();
            }
        });
        VAR_MAP.put("am.current.comp.package", new VarGetter() {
            @Override
            public String get() {
                if (Monkey.currentIntent == null) {
                    return null;
                }
                return Monkey.currentIntent.getComponent().getPackageName();
            }
        });
        VAR_MAP.put("am.current.data", new VarGetter() {
            @Override
            public String get() {
                if (Monkey.currentIntent == null) {
                    return null;
                }
                return Monkey.currentIntent.getDataString();
            }
        });
        VAR_MAP.put("am.current.categories", new VarGetter() {
            @Override
            public String get() {
                if (Monkey.currentIntent == null) {
                    return null;
                }
                StringBuffer stringBuffer = new StringBuffer();
                Iterator<String> it = Monkey.currentIntent.getCategories().iterator();
                while (it.hasNext()) {
                    stringBuffer.append(it.next());
                    stringBuffer.append(" ");
                }
                return stringBuffer.toString();
            }
        });
        VAR_MAP.put("clock.realtime", new VarGetter() {
            @Override
            public String get() {
                return Long.toString(SystemClock.elapsedRealtime());
            }
        });
        VAR_MAP.put("clock.uptime", new VarGetter() {
            @Override
            public String get() {
                return Long.toString(SystemClock.uptimeMillis());
            }
        });
        VAR_MAP.put("clock.millis", new VarGetter() {
            @Override
            public String get() {
                return Long.toString(System.currentTimeMillis());
            }
        });
        VAR_MAP.put("monkey.version", new VarGetter() {
            @Override
            public String get() {
                return Integer.toString(2);
            }
        });
    }

    public static class ListVarCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> list, MonkeySourceNetwork.CommandQueue commandQueue) {
            Set setKeySet = MonkeySourceNetworkVars.VAR_MAP.keySet();
            StringBuffer stringBuffer = new StringBuffer();
            Iterator it = setKeySet.iterator();
            while (it.hasNext()) {
                stringBuffer.append((String) it.next());
                stringBuffer.append(" ");
            }
            return new MonkeySourceNetwork.MonkeyCommandReturn(true, stringBuffer.toString());
        }
    }

    public static class GetVarCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> list, MonkeySourceNetwork.CommandQueue commandQueue) {
            if (list.size() == 2) {
                VarGetter varGetter = (VarGetter) MonkeySourceNetworkVars.VAR_MAP.get(list.get(1));
                if (varGetter == null) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(false, "unknown var");
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, varGetter.get());
            }
            return MonkeySourceNetwork.EARG;
        }
    }
}
