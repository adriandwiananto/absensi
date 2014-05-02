package com.emoney.absensi;

import java.util.Arrays;
import java.util.Random;

import nfc.emoney.proto.misc.Converter;
import nfc.emoney.proto.misc.Network;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class AbsenNfc extends Activity implements OnClickListener{
	
	private final static String TAG = "{class} AbsenNfc";
	
	private Button bCancel;
	private TextView tSESN;
	private NfcAdapter nfcAdapter;
	private PendingIntent mNfcPendingIntent;
	private ProgressBar pSpinner;

	private int sesnInt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.absen_nfc);
		
		// Init NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // no nfc device
    	if (nfcAdapter == null){
    		Toast.makeText(this, "No NFC found!", Toast.LENGTH_LONG).show();
        	finish();
        }
    	nfcAdapter.setNdefPushMessage(null, this);
    	
		bCancel = (Button) findViewById(R.id.bNfcCancel);
		bCancel.setOnClickListener(this);
		tSESN = (TextView)findViewById(R.id.tNfcRand);
		pSpinner = (ProgressBar)findViewById(R.id.pNfcSpinner);
		pSpinner.setVisibility(View.GONE);
		
		Random r = new Random();
		int Low = 100; //inclusive
		int High = 1000; //exclusive
		sesnInt = r.nextInt(High-Low) + Low;

		tSESN.setText(String.valueOf(sesnInt));
		
		//to prevent new activity creation after receiving NFC intent
		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(AbsenNfc.this, AbsenNfc.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        Log.d(TAG, "action intent:"+ getIntent().getAction());
        Log.d(TAG, "data intent:"+ getIntent().getDataString());
        
        //check if nfc enabled. if nfc is disabled, create dialog to offer enabling nfc in wireless setting
        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
        }
        
        nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, null, null);
    	//if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) processIntent(getIntent());
    }
	
	@Override
	public void onNewIntent(Intent intent){
		Log.d(TAG,"onNewIntent");
        Log.d(TAG, "action intent:"+ intent.getAction());
    	if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) processIntent(intent);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.bNfcCancel:
				finish();
				break;
		}
	}
	
	private void processIntent(Intent intent) {
		//debugging purpose
		Log.d(TAG,"process intent");
		pSpinner.setVisibility(View.VISIBLE);
		
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] msgs;
		if (rawMsgs != null) {
			//get payload of received NFC data
			msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				msgs[i] = (NdefMessage) rawMsgs[i];
			}
			byte[] receivedPacket = msgs[0].getRecords()[0].getPayload();
			Log.d(TAG,"received packet: "+Converter.byteArrayToHexString(receivedPacket));
			
			boolean valid_chk = true;
			if(receivedPacket[0] != 17) valid_chk = false;
			if(receivedPacket[1] != 2) valid_chk = false;
			if(receivedPacket[2] != 2) valid_chk = false;
			if(receivedPacket[3] != Converter.integerToByteArray(sesnInt)[2]) valid_chk = false;
			if(receivedPacket[4] != Converter.integerToByteArray(sesnInt)[3]) valid_chk = false;
			if(receivedPacket[5] != 0) valid_chk = false;
			if(receivedPacket[6] != 0) valid_chk = false;
			
			if(valid_chk == false){
				pSpinner.setVisibility(View.GONE);
				Toast.makeText(getApplicationContext(), "Invalid Absen Data", Toast.LENGTH_SHORT).show();
				return;
			}
			
			byte[] accnpArray = Arrays.copyOfRange(receivedPacket, 7, 13);
			byte[] tsArray = Arrays.copyOfRange(receivedPacket, 13, 17);
			
			Network sync = new Network(AbsenNfc.this, getApplicationContext(), Converter.byteArrayToLong(accnpArray), Converter.byteArrayToInteger(tsArray));
			sync.execute();
        }
	}
	
	private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("NFC is disabled. Would you like to enable it?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
    }
}
