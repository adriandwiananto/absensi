package com.emoney.absensi;

import java.util.Arrays;
import java.util.Random;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import nfc.emoney.proto.misc.CameraPreview;
import nfc.emoney.proto.misc.Converter;
import nfc.emoney.proto.misc.Network;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class AbsenQr extends Activity implements OnClickListener{
	
	private final static String TAG = "{class} AbsenNfc";
	
	private Button bCancel;
	private TextView tSESN;
	private NfcAdapter nfcAdapter;
	private PendingIntent mNfcPendingIntent;
	private ProgressBar pSpinner;

	private int sesnInt;
	
	// scanner
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    
    private FrameLayout preview;
    ImageScanner scanner;
    
    private boolean barcodeScanned = false;
    private boolean previewing = true;
    static {
        System.loadLibrary("iconv");
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.absen_qr);
		
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
		
		preview = (FrameLayout)findViewById(R.id.camera_preview);
		
		autoFocusHandler = new Handler();
		mCamera = getCameraInstance();
//		
		scanner = new ImageScanner();
//		
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
        
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        preview.addView(mPreview);
        preview.setVisibility(View.VISIBLE);
		
		//to prevent new activity creation after receiving NFC intent
		//mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(AbsenQr.this, AbsenQr.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}
	
	 /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }
    
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);
            
            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                
                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    Log.d(TAG, sym.getData());
                    barcodeScanned = true;
					processQr(sym.getData());
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
    
    private void processQr(String qr){
		if (qr != null){
			byte[] receivedPacket = Converter.hexStringToByteArray(qr);
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
			
			Network sync = new Network(AbsenQr.this, getApplicationContext(), Converter.byteArrayToLong(accnpArray), Converter.byteArrayToInteger(tsArray));
			sync.execute();
			releaseCamera();
			
		}
    }
    
    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
	
	@Override
	public void onNewIntent(Intent intent){
		Log.d(TAG,"onNewIntent");
        Log.d(TAG, "action intent:"+ intent.getAction());
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.bNfcCancel:
				finish();
				break;
		}
	}
}
