package com.android.browser;

import android.content.Context;
import com.mediatek.browser.ext.IBrowserBookmarkExt;
import com.mediatek.browser.ext.IBrowserDownloadExt;
import com.mediatek.browser.ext.IBrowserHistoryExt;
import com.mediatek.browser.ext.IBrowserMiscExt;
import com.mediatek.browser.ext.IBrowserRegionalPhoneExt;
import com.mediatek.browser.ext.IBrowserSettingExt;
import com.mediatek.browser.ext.IBrowserSiteNavigationExt;
import com.mediatek.browser.ext.IBrowserUrlExt;
import com.mediatek.browser.ext.INetworkStateHandlerExt;
import com.mediatek.browser.ext.OpBrowserCustomizationFactoryBase;

public class Extensions {
    private static volatile IBrowserBookmarkExt sBookmarkPlugin = null;
    private static volatile IBrowserDownloadExt sDownloadPlugin = null;
    private static volatile IBrowserHistoryExt sHistoryPlugin = null;
    private static volatile IBrowserMiscExt sMiscPlugin = null;
    private static volatile IBrowserRegionalPhoneExt sRegionalPhonePlugin = null;
    private static volatile IBrowserSettingExt sSettingPlugin = null;
    private static volatile IBrowserSiteNavigationExt sSiteNavigationPlugin = null;
    private static volatile IBrowserUrlExt sUrlPlugin = null;
    private static volatile INetworkStateHandlerExt sNetworkPlugin = null;

    private Extensions() {
    }

    public static IBrowserBookmarkExt getBookmarkPlugin(Context context) {
        if (sBookmarkPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sBookmarkPlugin == null) {
                        sBookmarkPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserBookmarkExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sBookmarkPlugin;
    }

    public static IBrowserDownloadExt getDownloadPlugin(Context context) {
        if (sDownloadPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sDownloadPlugin == null) {
                        sDownloadPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserDownloadExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sDownloadPlugin;
    }

    public static IBrowserHistoryExt getHistoryPlugin(Context context) {
        if (sHistoryPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sHistoryPlugin == null) {
                        sHistoryPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserHistoryExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sHistoryPlugin;
    }

    public static IBrowserMiscExt getMiscPlugin(Context context) {
        if (sMiscPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sMiscPlugin == null) {
                        sMiscPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserMiscExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sMiscPlugin;
    }

    public static INetworkStateHandlerExt getNetworkStateHandlerPlugin(Context context) {
        if (sNetworkPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sNetworkPlugin == null) {
                        sNetworkPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeNetworkStateHandlerExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sNetworkPlugin;
    }

    public static IBrowserRegionalPhoneExt getRegionalPhonePlugin(Context context) {
        if (sRegionalPhonePlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sRegionalPhonePlugin == null) {
                        sRegionalPhonePlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserRegionalPhoneExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sRegionalPhonePlugin;
    }

    public static IBrowserSettingExt getSettingPlugin(Context context) {
        if (sSettingPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sSettingPlugin == null) {
                        sSettingPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserSettingExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sSettingPlugin;
    }

    public static IBrowserSiteNavigationExt getSiteNavigationPlugin(Context context) {
        if (sSiteNavigationPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sSiteNavigationPlugin == null) {
                        sSiteNavigationPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserSiteNavigationExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sSiteNavigationPlugin;
    }

    public static IBrowserUrlExt getUrlPlugin(Context context) {
        if (sUrlPlugin == null) {
            synchronized (Extensions.class) {
                try {
                    if (sUrlPlugin == null) {
                        sUrlPlugin = OpBrowserCustomizationFactoryBase.getOpFactory(context).makeBrowserUrlExt();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return sUrlPlugin;
    }

    static void resetPlugins() {
        synchronized (Extensions.class) {
            try {
                sBookmarkPlugin = null;
                sDownloadPlugin = null;
                sHistoryPlugin = null;
                sMiscPlugin = null;
                sRegionalPhonePlugin = null;
                sSettingPlugin = null;
                sSiteNavigationPlugin = null;
                sUrlPlugin = null;
                sNetworkPlugin = null;
                OpBrowserCustomizationFactoryBase.resetOpFactory();
            } catch (Throwable th) {
                throw th;
            }
        }
    }
}
