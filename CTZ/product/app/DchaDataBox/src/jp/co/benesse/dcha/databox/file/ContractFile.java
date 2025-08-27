package jp.co.benesse.dcha.databox.file;

import android.net.Uri;
import java.util.Locale;

/* loaded from: classes.dex */
public enum ContractFile {
    TOP_DIR;

    final String pathName = name().toLowerCase(Locale.JAPAN);
    final int codeForMany = ordinal() * 10;
    public final Uri contentUri = Uri.parse("content://" + FileProvider.AUTHORITY + "/" + this.pathName);

    ContractFile() {
    }
}
