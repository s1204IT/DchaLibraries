package jp.co.benesse.dcha.databox.file;

import android.net.Uri;
import java.util.Locale;

public enum ContractFile {
    TOP_DIR;

    final String pathName = name().toLowerCase(Locale.JAPAN);
    final int codeForMany = ordinal() * 10;
    public final Uri contentUri = Uri.parse("content://" + FileProvider.AUTHORITY + "/" + this.pathName);

    public static ContractFile[] valuesCustom() {
        ContractFile[] contractFileArrValuesCustom = values();
        int length = contractFileArrValuesCustom.length;
        ContractFile[] contractFileArr = new ContractFile[length];
        System.arraycopy(contractFileArrValuesCustom, 0, contractFileArr, 0, length);
        return contractFileArr;
    }

    ContractFile() {
    }
}
