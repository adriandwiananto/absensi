package com.emoney.absensi;

import nfc.emoney.proto.misc.Network;
import nfc.emoney.proto.userdata.AppData;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class Register extends Activity implements OnClickListener{

	private final static String TAG = "{class} Register";
	
	private AppData appdata;
	private String ACCN;
	private ProgressBar pSpinner;
	
	private EditText eACCN;
	private Button bConfirm, bCancel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);
		
		//set IMEI in appdata
		//ONLY SET IMEI ONCE IN REGISTRATION!
		appdata = new AppData(this);
		appdata.setIMEI();
		
		eACCN = (EditText)findViewById(R.id.eRegACCN);
		bConfirm = (Button)findViewById(R.id.bRegConfirm);
		bConfirm.setOnClickListener(this);
		bCancel = (Button)findViewById(R.id.bRegCancel);
		bCancel.setOnClickListener(this);
		pSpinner = (ProgressBar)findViewById(R.id.pRegSpinner);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.bRegConfirm:
				Log.d(TAG,"Starts register");
				
				//hide soft keyboard
				InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
				
				//get string from edit text
				ACCN = eACCN.getText().toString();
	
				//check if data from edit text is correct
				//ACCN must have length of 14
				if(ACCN.length() < 14){
					Toast.makeText(getApplicationContext(), "Incorrect Account ID length" , Toast.LENGTH_SHORT).show();
					return;
				}
	
				//UI purpose
				bConfirm.setEnabled(false);
				bCancel.setEnabled(false);
				pSpinner.setVisibility(View.VISIBLE);
				
				//create JSON object of REGISTRATION
				long lACCN = Long.parseLong(ACCN);
				JSONObject json = new JSONObject();
				try {
					json.put("HWID", appdata.getIMEI());
					json.put("ACCN", lACCN);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				//do HTTP POST in separate thread (async task)
				Log.d(TAG,"Create asynctask");
				Network net = new Network(Register.this ,getApplicationContext(), json, ACCN);
				net.execute();
				Log.d(TAG,"Finish main thread");
				break;
			case R.id.bRegCancel:
				finish();
				break;
		}
	}
}
