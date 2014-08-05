package cw.kop.autobackground.accounts;

import android.content.Intent;

import com.google.android.gms.common.AccountPicker;

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 8/4/2014.
 */
public class GoogleAccount {

    public static final int GOOGLE_ACCOUNT_SIGN_IN = 0;
    public static final int GOOGLE_AUTH_CODE = 1;

    public static Intent getPickerIntent() {
        return AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"}, false, null, null, null, null);
    }


    public static void setResult() {}

    public static void deleteAccount() {
        AppSettings.setGoogleAccountName("");
        AppSettings.setGoogleAccountToken("");
    }

}
