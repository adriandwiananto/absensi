package nfc.emoney.proto.misc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nfc.emoney.proto.userdata.AppData;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
//import nfc.emoney.proto.R;

import com.emoney.absensi.MainActivity;
import com.emoney.absensi.R;

public class Network extends AsyncTask<Void, Void, JSONObject> {

	private final static String TAG = "{class} Network";
	private final static int REGISTRATION_MODE = 99;
	private final static int ABSEN_SYNC_MODE = 49;
	private final static String REG_SERVER = "https://emoney-server.herokuapp.com/register.json";
	private final static String ABSEN_SERVER = "https://emoney-server.herokuapp.com/presence.json";
			
	private String hostname;
	private String data; //registration https post param
	private String newACCN;
	
	private long accnpLong;
	private int tsInt;
	
	private JSONObject jobj_response;
	
	private int param_mode;
	private int error;
	private String errorMessage;
	
	private Context ctx;
	private AppData appdata;
	private Activity parentActivity;
	
	/**
	 * USE THIS CONSTRUCTOR TO SEND HTTP POST REGISTRATION DATA
	 * @param parent parent activity
	 * @param context caller context
	 * @param jobj JSON object of registration data
	 * @param NewPass new password inputted in registration activity
	 * @param ACCNtoSend ACCN inputted in registration activity
	 * @param HWID phone IMEI
	 */
	public Network(Activity parent, Context context, JSONObject jobj, String ACCNtoSend){
		ctx = context;
		parentActivity = parent;
		hostname = REG_SERVER;
		newACCN = ACCNtoSend;
		error = 0;
		
		param_mode = REGISTRATION_MODE;
		appdata = new AppData(ctx);
		
		data = jobj.toString();
	}
	
	/**
	 * USE THIS CONSTRUCTOR TO SEND HTTP POST ABSEN DATA
	 * @param parent parent activity
	 * @param context caller context
	 * @param keyEncryptionKey key encryption key
	 * @param logKey log key
	 * @param balanceKey balance key
	 */
	public Network(Activity parent, Context context, long ACCNP, int TS) {
		ctx = context;
		parentActivity = parent;
		param_mode = ABSEN_SYNC_MODE;
		hostname = ABSEN_SERVER;
		error = 0;
		accnpLong = ACCNP;
		tsInt = TS;
		
		appdata = new AppData(ctx);
	}

	@Override
	protected JSONObject doInBackground(Void... params) {
		try {
			//HTTP POST preparation
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(hostname);

			// Add data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			if(param_mode == ABSEN_SYNC_MODE){
				//add JSON object of SYNC header to header param of HTTP POST
				//add JSON array of SYNC logs to logs param of HTTP POST
				nameValuePairs.add(new BasicNameValuePair("ACCN-P", String.valueOf(accnpLong)));
				nameValuePairs.add(new BasicNameValuePair("ACCN-M", String.valueOf(appdata.getACCN())));
				nameValuePairs.add(new BasicNameValuePair("timestamp", String.valueOf(tsInt)));
				nameValuePairs.add(new BasicNameValuePair("HWID", String.valueOf(appdata.getIMEI())));
			}
			else if(param_mode == REGISTRATION_MODE){
				//add JSON object of REGISTRATION to data param of HTTP POST
				nameValuePairs.add(new BasicNameValuePair("data", data));
			}
			else{
				error = 5; //unknown param_mode
				return null;
			}
			
			Log.d(TAG,"post: "+nameValuePairs.toString());
			
			//add name value pairs List to HttpPost 
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			
			// Get response and return it in JSON Object type
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			String json = reader.readLine();
			JSONTokener tokener = new JSONTokener(json);
			JSONObject finalResult = new JSONObject(tokener);
			Log.d(TAG, "return:"+finalResult.toString());
			
			return finalResult;
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG,"error:"+e.getMessage());
			error = 1;
			return null;
		}
	}

	@Override
	protected void onPreExecute() {
		//Toast this message before doInBackground starts
		if(param_mode == REGISTRATION_MODE){
			Toast.makeText(ctx, "Registration Starts", Toast.LENGTH_SHORT).show();	
		} else {
			Toast.makeText(ctx, "Sync Starts", Toast.LENGTH_SHORT).show();
		}
	}
	 
	@Override
	protected void onPostExecute(JSONObject result) {
		if (error == 0) {
			jobj_response = result;

			if(param_mode == REGISTRATION_MODE){
				String responseStatus;
				try {
					responseStatus = jobj_response.getString("result");
					
					//if result from HTTP POST response error
					if((responseStatus.compareTo("Error") == 0) || (responseStatus.compareTo("error") == 0)){
						String errorMessage = jobj_response.getString("message");
						Toast.makeText(ctx, "Registration failed!! "+errorMessage, Toast.LENGTH_LONG).show();
						appdata.deleteAppData();
						error = 3;
					} else { 
						//if no error
						//get aes key from HTTP POST response and convert it to byte array
						String responseKey = jobj_response.getString("key");
						byte[] aesKey = new byte[responseKey.length()/2];
						aesKey = Converter.hexStringToByteArray(responseKey.toString());
						Log.d(TAG,"aesKey byte array:"+Arrays.toString(aesKey));
						
						//write ACCN to appdata
						//write password to appdata (hashed)
						Log.d(TAG,"Start writing shared pref");
						appdata.setACCN(Long.parseLong(newACCN));
						
						Toast.makeText(ctx, "Registration Success", Toast.LENGTH_LONG).show();
						
						//close registration activity and start login activity
						((ProgressBar)parentActivity.findViewById(R.id.pRegSpinner)).setVisibility(View.INVISIBLE);
						ctx.startActivity((new Intent(ctx, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						parentActivity.finish();
					}
				} catch (JSONException e) {
					e.printStackTrace();
					appdata.deleteAppData();
				}
			}
			else if(param_mode == ABSEN_SYNC_MODE){
				String responseStatus;
				try {
					//if result in http post response is error, set error value
					responseStatus = jobj_response.getString("result");
					if(responseStatus.compareTo("Error") == 0){
						error = 3;
						errorMessage = jobj_response.getString("message");
					}
					if(responseStatus.compareTo("error") == 0){
						error = 3;
						errorMessage = jobj_response.getString("message");
					}
				} catch (Exception e) {
					e.printStackTrace();
					error = 4;
				}
			}
			else{
				error = 99;
				Log.d(TAG,"WTF -- What a Terible Failure. Param_mode not LOG or REG!");
			}
			
		} else {
			// error occured
			error = 1;
			Log.d(TAG,"Response is empty JSON Object");
			(parentActivity.findViewById(R.id.pNfcSpinner)).setVisibility(View.GONE);
			//appdata.deleteAppData();
		}
		
		
		if(param_mode == REGISTRATION_MODE){
			//if error in registration mode, new activity isn't called then execute bellow code
			//change in UI
			(parentActivity.findViewById(R.id.bRegConfirm)).setEnabled(true);
			(parentActivity.findViewById(R.id.bRegCancel)).setEnabled(true);
			(parentActivity.findViewById(R.id.pRegSpinner)).setVisibility(View.GONE);
		} else if (param_mode == ABSEN_SYNC_MODE) { 
			//in log sync mode, error or no error, this will be executed
			if(error == 0){
				Toast.makeText(ctx, "Absen success", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(ctx, "Absen failed", Toast.LENGTH_LONG).show();
				Log.d(TAG,"error:"+error);
				Log.d(TAG,"error message:"+errorMessage);
			}
			
			//change in UI
			parentActivity.finish();
		}
	}
}
