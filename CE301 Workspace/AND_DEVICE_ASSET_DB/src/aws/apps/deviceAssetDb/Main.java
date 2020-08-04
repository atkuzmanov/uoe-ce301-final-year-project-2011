package aws.apps.deviceAssetDb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import aws.apps.deviceAssetDb.R;
import aws.util.deviceAssetDb.CurrentState;
import aws.util.deviceAssetDb.DownloadThread;
import aws.util.deviceAssetDb.UsefulBits;

enum MENU_BUTTONS {
	ABOUT, UPDATE_DB, QUIT, EXPORT;

	public static MENU_BUTTONS lookUpByOrdinal(int i) {
		return MENU_BUTTONS.values()[i];
	}
}

public class Main extends Activity {
	final String TAG =  this.getClass().getName();

	final int UPDATE_DB = 0;

	private UsefulBits uB;
	private ListView myList;
	private TextView tvResults;

	private EditText fldInventoryNo;
	private EditText fldBarcode;
	private EditText fldBrand;
	private EditText fldModel;
	private EditText fldHostname;
	private EditText fldRoomName;
	private EditText fldMacAddress;
	private EditText fldIpAddress;

	private Button btnClear;
	private Button btnSearch;
	private Button btnHideShow;

	private int visibilityState = View.VISIBLE;

	private ProgressDialog progressDialog;
	private DownloadThread externalIpThread;
	private String localDbLocation = "";
	private String localOldDbFullPath = "";
	private String localDbFullPath = "";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "^ Intent started");

		setContentView(R.layout.main);
		setTitle(getString(R.string.main_title));

		uB = new UsefulBits(this);

		btnHideShow = (Button) findViewById(R.id.btnHideShow);
		btnClear = (Button) findViewById(R.id.btnClear);
		btnSearch = (Button) findViewById(R.id.btnSearch);
		tvResults = (TextView) findViewById(R.id.tv_results);

		fldInventoryNo = (EditText) findViewById(R.id.editInventoryNo);
		fldBarcode = (EditText) findViewById(R.id.editBarcode);
		fldBrand = (EditText) findViewById(R.id.editBrand);
		fldModel = (EditText) findViewById(R.id.editModel);
		fldHostname = (EditText) findViewById(R.id.editHostname);
		fldRoomName = (EditText) findViewById(R.id.editRoomName);
		fldMacAddress = (EditText) findViewById(R.id.editMacAddress);
		fldIpAddress = (EditText) findViewById(R.id.editIpAddress);

		localDbLocation = Environment.getExternalStorageDirectory() + getString(R.string.sd_db_location);
		localOldDbFullPath = localDbLocation + getString(R.string.sd_db_name_old);
		localDbFullPath = localDbLocation + getString(R.string.sd_db_name);

		final Object data = getLastNonConfigurationInstance();


		if (data==null){
			myList = (ListView) findViewById(R.id.main_list);
		} else{
			restoreState(data);
		}

		myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				listOnClick(v, pos, id);
			}
		});

		btnHideShow.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(visibilityState == View.VISIBLE){
					visibilityState = View.GONE;
				} else {
					visibilityState = View.VISIBLE;
				}
				setVisibility(visibilityState);
			}
		});

		btnClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tvResults.setText("0");
				fldInventoryNo.setText("");
				fldBarcode.setText("");
				fldBrand.setText("");
				fldModel.setText("");
				fldHostname.setText("");
				myList.setAdapter(null);
			}
		});

		btnSearch.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				populateList();
			}
		});

		// Rename DB
		if (((!new File(localDbFullPath).exists()) && (new File(localOldDbFullPath).exists()))){
			if(new File(localOldDbFullPath).renameTo(new File(localDbFullPath))==true)
			{
				Log.d(TAG, "^ Renaming succesfull!");
			}
		} else {
			Log.d(TAG, "^ Renaming not neeed.");
		}

		// Prompt user to DL DB if it is missing.
		if (!new File(localDbFullPath).exists()){
			uB.ShowAlert(getString(R.string.alert_db_not_found_title), getString(R.string.alert_db_not_found_instructions), getString(R.string.ok));
			Log.e(TAG, "^ Database not found: " + localDbFullPath);
			return;
		}

		setVisibility(visibilityState);
	}

	private void setVisibility(int visibilityState){
		TableLayout tlInput = (TableLayout) findViewById(R.id.main_table_input);
		TableLayout tlButtons = (TableLayout) findViewById(R.id.main_table_buttons);

		tlInput.setVisibility(visibilityState);
		tlButtons.setVisibility(visibilityState);

		if(visibilityState == TableLayout.VISIBLE){
			btnHideShow.setText(getString(R.string.label_hide));
		} else {
			btnHideShow.setText(getString(R.string.label_show));
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		CurrentState cr = new CurrentState();

		cr.setInventoryNo((fldInventoryNo.getText().toString()));
		cr.setBarcode((fldBarcode.getText().toString()));

		cr.setVisibility(visibilityState);
		cr.setResults(tvResults.getText().toString());

		cr.setAdapter(myList.getAdapter());

		return cr;
	}

	private void restoreState(Object data){
		if(myList==null){
			myList = (ListView) findViewById(R.id.main_list);
		} 
		if (!(data==null)){
			CurrentState cr = (CurrentState) data;

			fldInventoryNo.setText(cr.getInventoryNo());
			fldBarcode.setText(cr.getBarcode());

			visibilityState = cr.getVisibility();
			tvResults.setText(cr.getResults());

			myList.setAdapter(cr.getAdapter());
		}
	}

	@SuppressWarnings("unchecked")
	private void listOnClick(View v, int pos, long id){
		try{
			HashMap<String, Object> selection = (HashMap<String, Object>) myList.getItemAtPosition(pos);
			String itemid = selection.get("inventorynumberid").toString();

			Intent myIntent = new Intent();
			myIntent.putExtra("item_id",  itemid);
			myIntent.setClassName(getPackageName(),getPackageName() + ".AssetInfo");
			startActivity(myIntent); 


		}catch(Exception e){
			Log.e(TAG, "^ listOnClick error: " + e.getMessage());
		}
	}

	private void populateList(){
		myList.setAdapter(null);	

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			Log.d(TAG, "^ SD card not available.");
			uB.ShowAlert(getString(R.string.sd_error), getString(R.string.sd_not_available), getString(R.string.ok));
			return;
		}

		if (!new File(localDbFullPath).exists()){
			uB.ShowAlert(getString(R.string.alert_db_not_found_title), getString(R.string.alert_db_not_found_instructions), getString(R.string.ok));
			Log.e(TAG, "^ Database not found: " + localDbFullPath);
			return;
		}
		try {
			List<Map<String, Object>> drInfo = new ArrayList<Map<String, Object>>();

			SQLiteDatabase db = SQLiteDatabase.openDatabase(localDbFullPath, null, SQLiteDatabase.OPEN_READONLY);

			if(!db.isOpen()){
				Log.e(TAG, "^ DB was not opened!");
				uB.showToast(getString(R.string.error_could_not_open_db), Toast.LENGTH_SHORT, Gravity.TOP,0,0);
				return;
			}


			


			Cursor cur = db.rawQuery(getSQLStatement(), null);

			cur.moveToFirst();

			while (cur.isAfterLast() == false) {
				Map<String, Object> map = new HashMap<String, Object>();
				for(int i=0; i < cur.getColumnCount() ;i++){
					map.put(cur.getColumnName(i), setTextIfBlank(cur.getString(i).trim().replace("''", "'"), 1, '-'));
				}

				drInfo.add(map);
				cur.moveToNext();
			}

			cur.close();
			db.close();

//			myList.setAdapter(new SimpleAdapter(this, drInfo, R.layout.listitem,
//					cur.getColumnNames(), 
//					new int[] { R.id.inventory_no_id, R.id.inventory_no, R.id.barcode, R.id.brand, R.id.model, R.id.hostname, R.id.roomName }));

			myList.setAdapter(new SimpleAdapter(this, drInfo, R.layout.listitem,
					cur.getColumnNames(), 
					new int[] { R.id.inventory_no_id, R.id.inventory_no, R.id.barcode, R.id.brand, R.id.model, R.id.hostname, R.id.roomName }));

			
			tvResults.setText(cur.getCount()+"");
		} catch (Exception e) {
			Log.e(TAG,"^ populateList(): " + e.getMessage());
			uB.showToast(e.getMessage(), Toast.LENGTH_SHORT, Gravity.TOP, 0,0);
		}	
	}

	private String setTextIfBlank(String str, int count, char ch){
		if(str.equals("")){
			char[] chars = new char[count];
			while (count>0) chars[--count] = ch;
			return String.valueOf(chars);
		} else {
			return str;
		}
	}

	private String getSQLStatement(){
		
		String sSelect = "SELECT d.inventorynumberid, d.deviceinventorynumber, d.devicebarcodenumber, d.devicebrand, d.devicemodel, d.devicehostname, r.roomname";
		String sFrom = " FROM tbldevices d, tblrooms r";
		String sWhere = " WHERE d.roomid=r.roomid";
		String sOrder = " ORDER BY d.inventorynumberid ASC";
		
		String inventoryNo = fldInventoryNo.getText().toString().trim();
		String barcode = fldBarcode.getText().toString().trim();
		String brand = fldBrand.getText().toString().trim();
		String model = fldModel.getText().toString().trim();
		String hostname = fldHostname.getText().toString().trim();
		String roomname = fldRoomName.getText().toString().trim();
		String macaddress = fldMacAddress.getText().toString().trim();
		String ipaddress = fldIpAddress.getText().toString().trim();

		// Finish building the FROM statement
		if (macaddress.length()>0 || ipaddress.length()>0){
			sFrom += " tblnics n";}
				
		// Start building the WHERE statement		
		
		if (macaddress.length()>0 || ipaddress.length()>0){
			sWhere += " AND d.inventorynumberid=n.inventorynumberid";}
		
		sWhere = getWhereCompononent(sWhere, "d.deviceinventorynumber", inventoryNo);
		sWhere = getWhereCompononent(sWhere, "d.devicebarcodenumber", barcode);
		sWhere = getWhereCompononent(sWhere, "d.devicebrand", brand);
		sWhere = getWhereCompononent(sWhere, "d.devicemodel", model);
		sWhere = getWhereCompononent(sWhere, "d.devicehostname", hostname);
		sWhere = getWhereCompononent(sWhere, "r.roomname", roomname);
		sWhere = getWhereCompononent(sWhere, "n.nicmac", macaddress);
		sWhere = getWhereCompononent(sWhere, "n.nicip", ipaddress);
				
		if (sSelect.length()>0){
			Log.d(TAG, "^ " + sSelect);}
		if (sFrom.length()>0){
				Log.d(TAG, "^ " + sFrom);}			
		if (sWhere.length()>0){
			Log.d(TAG, "^ " + sWhere);}
		if (sOrder.length()>0){
			Log.d(TAG, "^ " + sOrder);}
		
		return sSelect + sFrom + sWhere + sOrder;
	}

	private String getWhereCompononent(String currentWhereStatement, String fieldName, String fieldValue){
		String statement = "";
		if (fieldValue.length()>0){

			fieldValue = fieldValue.replaceAll("'", "''");
			if (currentWhereStatement.length() > 0){
				statement += " AND ";
			}
			statement += fieldName + " LIKE '" + fieldValue +"'";
		}
		return currentWhereStatement + statement;
	}

	/** Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_BUTTONS.UPDATE_DB.ordinal(), 0,
				getString(R.string.label_menu_update_db)).setIcon(android.R.drawable.ic_menu_rotate);		
		menu.add(0, MENU_BUTTONS.ABOUT.ordinal(), 0,
				getString(R.string.label_menu_about)).setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, MENU_BUTTONS.QUIT.ordinal(), 0,
				getString(R.string.label_menu_quit)).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	/** Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (MENU_BUTTONS.lookUpByOrdinal(item.getItemId())) {
		case ABOUT:
			uB.showAboutDialogue();
			return true;
		case UPDATE_DB :
			if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				Log.d(TAG, "^ SD card not available.");
				uB.showToast(getString(R.string.sd_not_available), Toast.LENGTH_SHORT, Gravity.TOP,0,0);
				return true;
			}

			if (!uB.createDirectories(localDbLocation)){
				return true;
			}

			if (!uB.isOnline()){
				uB.ShowAlert(
						getString(R.string.text_device_offline), 
						getString(R.string.text_device_offline_instructions), 
						getString(R.string.ok));
				return true;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.alert_update_db))
			.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showDialog(UPDATE_DB);
				}

			})
			.setNegativeButton(getString(R.string.no), null).show();
			return true;

		case QUIT:
			this.finish();
			return true;
		}
		return false;
	}

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case UPDATE_DB:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.text_updating_db));
			externalIpThread = new DownloadThread(handler,
					getString(R.string.url_pci_db), 
					localDbLocation,
					getString(R.string.sd_db_name),
					getApplicationContext());
			externalIpThread.start();
			return progressDialog;
		default:
			return null;
		}
	}

	// Define the Handler that receives messages from the thread and update the
	// progress
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			int status = msg.getData().getInt("status");

			switch(status){
			case DownloadThread.RESULT_ERROR:
				uB.showToast(getString(R.string.db_dl_error) + ": " + msg.getData().getString("msg"), Toast.LENGTH_SHORT, Gravity.TOP,0,0);
				break;
			case DownloadThread.RESULT_OK:
			default:
				uB.showToast(getString(R.string.db_dl_ok), Toast.LENGTH_SHORT, Gravity.TOP,0,0);
				break;
			}
			externalIpThread.setState(DownloadThread.STATE_DONE);
			removeDialog(UPDATE_DB);
		}
	};
}