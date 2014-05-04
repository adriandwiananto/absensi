package com.emoney.absensi;

import nfc.emoney.proto.userdata.AppData;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{

	//private final static String TAG = "{class} MainActivity";

	ImageButton ibQr, ibNfc;
	
	private long lIMEI;
	private AppData appdata;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ibQr = (ImageButton)findViewById(R.id.ibQr);
		ibQr.setOnClickListener(this);
		ibNfc = (ImageButton)findViewById(R.id.ibNfc);
		ibNfc.setOnClickListener(this);
		
		//get device IMEI
    	TelephonyManager T = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
		String IMEI = T.getDeviceId();
		lIMEI = Long.parseLong(IMEI);
		
		appdata = new AppData(getApplicationContext());
        if(appdata.getError() == true){
			Toast.makeText(this, "APPDATA ERROR!", Toast.LENGTH_LONG).show();
			finish();
		}

        if(appdata.getACCN() == 0){
        	startActivity(new Intent(this, Register.class)); 
        	finish();
        }
        else{
        	//check if registered IMEI in appdata is same with current IMEI
        	if(appdata.getIMEI() != lIMEI){
        		Toast.makeText(getApplicationContext(), "Registered device not same with current device", Toast.LENGTH_LONG).show();
        		finish();
        	}
        }
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.ibNfc:
				startActivity(new Intent(this, AbsenNfc.class));
				break;
			case R.id.ibQr:
				startActivity(new Intent(this, AbsenQr.class));
				break;
		}
	}
}
