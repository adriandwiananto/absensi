package nfc.emoney.proto.userdata;

import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AppData {
	private final static String TAG = "{class} AppData";
	private static final String PREF1_NAME = "Pref1";
	private static final String PREF2_NAME = "Pref2";
	private SharedPreferences Pref, Pref1, Pref2;
	private Context ctx;
	private String IMEI;
	private long lIMEI;
	private boolean error = false;
	
	public AppData(Context context) {
		ctx = context;
		Pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		Pref1 = ctx.getSharedPreferences(PREF1_NAME, Context.MODE_PRIVATE);
		Pref2 = ctx.getSharedPreferences(PREF2_NAME, Context.MODE_PRIVATE);
		
		int retCheck = checkDuplicate();
		Log.d(TAG,"return check duplicate: "+retCheck);
		
		if(retCheck != 0)error = true;
	}

	@SuppressLint("NewApi")
	private int checkDuplicate(){
		//Cycle through all the entries in the sp
		for(Entry<String,?> entry : Pref.getAll().entrySet()){ 
			Object v = entry.getValue(); 
			String key = entry.getKey();
			//Now we just figure out what type it is, so we can copy it.
			// Note that i am using Boolean and Integer instead of boolean and int.
			// That's because the Entry class can only hold objects and int and boolean are primatives.
			if(v instanceof Boolean){ 
				// Also note that i have to cast the object to a Boolean 
				// and then use .booleanValue to get the boolean
			    if(Pref.getBoolean(key, false) != Pref1.getBoolean(key, false) != Pref2.getBoolean(key, false))
			    	return 1;

			    Log.d(TAG,"(bool)Key: "+key+" Value: "+Pref.getBoolean(key, false));
			} else if(v instanceof Float) {
				float pref = Pref.getFloat(key, (float) 0.0);
				float pref1 = Pref1.getFloat(key, (float) 0.0);
				float pref2 = Pref2.getFloat(key, (float) 0.0);
				
				if(Float.compare(pref, pref1) != 0){
					return 2;
				}
				if(Float.compare(pref, pref2) != 0){
					return 3;
				}
				if(Float.compare(pref1, pref2) != 0){
					return 4;
				}
				Log.d(TAG,"(float)Key: "+key+" Value: "+pref);
			} else if(v instanceof Integer) {
				int pref = Pref.getInt(key, 0);
				int pref1 = Pref1.getInt(key, 0);
				int pref2 = Pref2.getInt(key, 0);
				
				if(Integer.compare(pref, pref1) != 0){
					return 5;
				}
				if(Integer.compare(pref, pref2) != 0){
					return 6;
				}
				if(Integer.compare(pref1, pref2) != 0){
					return 7;
				}
				Log.d(TAG,"(int)Key: "+key+" Value: "+pref);
			}
			else if(v instanceof Long) {
				long pref = Pref.getLong(key, 0);
				long pref1 = Pref1.getLong(key, 0);
				long pref2 = Pref2.getLong(key, 0);
				
				if(Long.compare(pref, pref1) != 0){
					return 8;
				}
				if(Long.compare(pref, pref2) != 0){
					return 9;
				}
				if(Long.compare(pref1, pref2) != 0){
					return 10;
				}
				Log.d(TAG,"(long)Key: "+key+" Value: "+pref);
			}
			else if(v instanceof String) {
				String pref = Pref.getString(key, "");         
				String pref1 = Pref1.getString(key, "");         
				String pref2 = Pref2.getString(key, "");
				
				if(pref.compareTo(pref1) != 0){
					return 11;
				}
				if(pref.compareTo(pref2) != 0 ){
					return 12;
				}
				if(pref1.compareTo(pref2) != 0 ){
					return 13;
				}
				Log.d(TAG,"(str)Key: "+key+" Value: "+pref);
			}
		}
		return 0;
	}
	
	private void saveDuplicate(){
		//Pref1,Pref2 is the shared pref to copy to
		SharedPreferences.Editor ed1 = Pref1.edit(); 
		SharedPreferences.Editor ed2 = Pref2.edit();
		
		SharedPreferences sp = Pref; //The shared preferences to copy from
		ed1.clear(); // This clears the one we are copying to, but you don't necessarily need to do that.
		ed2.clear(); // This clears the one we are copying to, but you don't necessarily need to do that.
		
		//Cycle through all the entries in the sp
		for(Entry<String,?> entry : sp.getAll().entrySet()){ 
			Object v = entry.getValue(); 
			String key = entry.getKey();
			//Now we just figure out what type it is, so we can copy it.
			// Note that i am using Boolean and Integer instead of boolean and int.
			// That's because the Entry class can only hold objects and int and boolean are primatives.
			if(v instanceof Boolean){ 
				// Also note that i have to cast the object to a Boolean 
				// and then use .booleanValue to get the boolean
			    ed1.putBoolean(key, ((Boolean)v).booleanValue());
				ed2.putBoolean(key, ((Boolean)v).booleanValue());
			} else if(v instanceof Float) {
				ed1.putFloat(key, ((Float)v).floatValue());
				ed2.putFloat(key, ((Float)v).floatValue());
			} else if(v instanceof Integer) {
				ed1.putInt(key, ((Integer)v).intValue());
				ed2.putInt(key, ((Integer)v).intValue());
			}
			else if(v instanceof Long) {
				ed1.putLong(key, ((Long)v).longValue());
				ed2.putLong(key, ((Long)v).longValue());
			}
			else if(v instanceof String) {
				ed1.putString(key, ((String)v));         
				ed2.putString(key, ((String)v));         
			}
		}
		ed1.commit(); //save it.	
		ed2.commit(); //save it.	
	}
	
	public boolean getError(){
		return error;
	}
	
	public void setACCN(long lACCN) {
		Editor edit = Pref.edit();
		edit.putLong("ACCN", lACCN);
		edit.commit();
		saveDuplicate();
	}
	
	public long getACCN() {
		return Pref.getLong("ACCN", 0);
	}

	public void setIMEI() {
		TelephonyManager T = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
		IMEI = T.getDeviceId();
		lIMEI = Long.parseLong(IMEI);
		Editor edit = Pref.edit();
		edit.putLong("HWID", lIMEI);
		edit.commit();
		saveDuplicate();
	}
	
	public long getIMEI(){
		return Pref.getLong("HWID", 0);
	}
		
	public void deleteAppData(){
		Editor edit = Pref.edit();
		edit.clear();
		edit.commit();
		
		edit = Pref2.edit();
		edit.clear();
		edit.commit();
		
		edit = Pref1.edit();
		edit.clear();
		edit.commit();
	}
}
