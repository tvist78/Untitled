/*******************************************************************************
 * Copyright (c) 2010 Eugene Vorobkalo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Eugene Vorobkalo - initial API and implementation
 *     Andrey Fedorov
 ******************************************************************************/
package org.droidpres.activity;

import java.text.DecimalFormat;
import java.util.UUID;

import android.content.*;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.droidpres.BaseApplication;

import android.app.Activity;
import android.database.Cursor;
import org.droidpres.R;
import org.droidpres.db.DB;
import org.droidpres.utils.Const;
import org.droidpres.utils.Utils;

import android.app.AlertDialog;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int MSG_SETUP = 1;

    public static int versionCode;
    private static int mAccuracy;
    public static String versionName;
    public static String appName;

    private boolean mAgentID = false;
    private boolean mCustomTitleFlag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCustomTitleFlag = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main_new);

        checkGPS();
    }

    private void UpdateForm(boolean firstInit){

        SQLiteDatabase db = DB.get().getReadableDatabase();

        int countToSend = 0;
        int countNotSended = 0;
        int countOrders = 0;
        try{
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM "+DB.TABLE_DOCUMENT+" WHERE number<>0",null);
            if (cursor.moveToFirst()) {
                countOrders = cursor.getInt(0);
            }
            cursor.close();

            cursor = db.rawQuery("SELECT COUNT(*) FROM "+DB.TABLE_DOCUMENT+
                    " WHERE number<>0 and docstate="+Const.DOCSTATE_PREPARE_SEND,null);
            if (cursor.moveToFirst()) countToSend = cursor.getInt(0);
            cursor.close();
            cursor = db.rawQuery("SELECT COUNT(*) FROM "+DB.TABLE_DOCUMENT+
                    " WHERE number<>0 and docstate="+Const.DOCSTATE_EDIT,null);
            if (cursor.moveToFirst()) countNotSended = countToSend + cursor.getInt(0);
            cursor.close();
        }catch (Exception ex){
            Log.e("MainActivity",ex.getMessage());
        }

        ((TextView) findViewById(R.id.subtextListOrders)).setText("Всего заказов: "+countOrders);
        ((TextView) findViewById(R.id.subtextExchange)).setText("К отправке: "+countToSend+"/"+countNotSended);

        db.close();
    }

    public void butOrdersList_Click(View v){
        Bundle intent_extras = new Bundle();
        intent_extras.putLong(Const.EXTRA_CLIENT_ID, 0);
        intent_extras.putString(Const.EXTRA_CLIENT_NAME, getString(R.string.orders_all_clients));

        Intent intent_document = new Intent(this, DocumentListActivity.class);
        intent_document.putExtras(intent_extras);
        startActivity(intent_document);
    }

    public void butNewOrder_Click(View v){
        Bundle intent_extras = new Bundle();
        intent_extras.putLong(Const.EXTRA_DOC_ID, 0);
        intent_extras.putInt(Const.EXTRA_DOC_PRESVEN, 0);
        intent_extras.putInt(Const.EXTRA_DOC_TYPE, 1);
        intent_extras.putString(Const.EXTRA_DOC_DESC, "");

        Intent DocIntent = new Intent(this, DocActivity.class);
        DocIntent.putExtras(intent_extras);
        startActivity(DocIntent);
    }

    public void butSettings_Click(View v){
        mAccuracy = SetupRootActivity.getGPSAccuracy(getBaseContext());
        startActivityForResult(new Intent(MainActivity.this, SetupActivity.class), MSG_SETUP);
    }

    public void butExchange_Click(View v){
        startActivity(new Intent(this, TransferActivity.class));
    }

    public void butExit_Click(View v){
        quit();
    }

    @Override
    protected void onResume(){
        super.onResume();
        UpdateForm(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        DecimalFormat nf = SetupRootActivity.getQtyFormat(this, getString(R.string.lb_qty));
        DecimalFormat cf = SetupRootActivity.getCurrencyFormat(this);
        mAgentID = SetupRootActivity.getAgentID(this).length() > 0;
        if (!mAgentID) Utils.ToastMsg(this, R.string.err_NoSetAgentID);

        final String id = SetupRootActivity.getGUID(this);
        if (id == null) {
            final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
            final String tmDevice, tmSerial, androidId;
            tmDevice = "" + tm.getDeviceId();
            androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32)); // | tmSerial.hashCode()
            SetupRootActivity.setSeting(this,getString(R.string.DEVICE_GUID),deviceUuid.toString());
        }
    }

    @Override
    protected void onStop(){
        unregisterReceiver(mBatInfoReceiver);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkGPS();
        if (requestCode == MSG_SETUP) {
            BaseApplication.schedule(this);
        }
    }

    private void customTitleBar(String left, String right) {
        if (right.length() > 20) right = right.substring(0, 20);
        if (mCustomTitleFlag) {
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
            ((TextView) findViewById(R.id.tvTitleLeft)).setText(left);
            ((TextView) findViewById(R.id.tvTitleRight)).setText(right);
        }
    }

    private void checkGPS() {
        if (!SetupRootActivity.getNoStartGPS(this)) return;

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (! manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.setup_nostartwithoutgps)
                    .setMessage(R.string.msg_NoOnGPS)
                    .setCancelable(false)
                    .setPositiveButton(R.string.lb_on_gps, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                    MSG_SETUP);
                        }
                    })
                    .setNegativeButton(R.string.lb_setup, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(MainActivity.this, SetupActivity.class),
                                    MSG_SETUP);
                        }
                    })
                    .show();
        }
    }

    private void quit() {
        finish();
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            customTitleBar(String.format("%s v%s", getString(R.string.app_name),BaseApplication.FULL_VERSION),
                    String.format("Заряд: %d%%", intent.getIntExtra("level", 0)));
        }
    };
}