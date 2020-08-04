package aws.apps.deviceAssetDb;

import java.util.Date;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import aws.apps.deviceAssetDb.MapTabView;
import aws.util.deviceAssetDb.UsefulBits;
import aws.apps.deviceAssetDb.MENU_BUTTONS;

public class AssetInfo extends TabActivity  {
	final String TAG =  this.getClass().getName();

	final int GET_EXTERNAL_IP = 0;
	private TableLayout tableCellInfo;
	private TableLayout tableLocationInfo;
	private Bundle extras;
	private UsefulBits useful;
	private TabHost tabHost;
	
	private MapView myMapView;
	private String TimeDate;
	private Button btnStart;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "^ Intent Started");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.assetinfo);
		useful = new UsefulBits(this);

		extras = getIntent().getExtras();
		Log.d(TAG, "^ Selection name: " + extras.getString("item_id"));
		
		tableCellInfo = (TableLayout) findViewById(R.id.main_table_cell_info);
		tableLocationInfo = (TableLayout) findViewById(R.id.main_table_location_info);

		tabHost = getTabHost();

		//Build the mapview
		TabSpec tabSpec = tabHost.newTabSpec("tab_location").setIndicator("Map", 
				getResources().getDrawable(R.drawable.map)).setContent(R.id.main_scrollview_location_info);

		Context ctx = this.getApplicationContext();			
		Intent i = new Intent(ctx, MapTabView.class);
		tabSpec.setContent(i);

		tabHost.addTab(tabHost.newTabSpec("tab_device").setIndicator("Cell", 
				getResources().getDrawable(R.drawable.sim)).setContent(R.id.main_scrollview_cell_info));
		tabHost.addTab(tabSpec);

		tabHost.setCurrentTab(0);
		formatTabs(tabHost.getTabWidget());
		
		if (myMapView==null) {	
				myMapView = (MapView) findViewById(R.id.mapview); 
			}
		
		
//		GeoPoint initGeoPoint = new GeoPoint(
//				   (int)(gi.getNetLocation().getLatitude()*1000000),
//				   (int)(gi.getNetLocation().getLongitude()*1000000));
//				   CenterLocation(initGeoPoint, myMapView);
				   
		
		// Populate the table.
		populateTable();
	}



	/** Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_BUTTONS.EXPORT.ordinal(), 0,
				getString(R.string.label_menu_export)).setIcon(android.R.drawable.ic_menu_upload);	
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
			new UsefulBits(this).showAboutDialogue();
			return true;
		case QUIT:
			this.finish();
			return true;
		case EXPORT:
			Intent myIntent = new Intent();
			String export_text = "";

			export_text += getString(R.string.label_export_cell_info) + "\n";
			export_text += tableToString(tableCellInfo);
			export_text += tableToString(tableLocationInfo);

			myIntent.putExtra("info", export_text);
			myIntent.putExtra("time", TimeDate);
			
			myIntent.setClassName(getPackageName(), getPackageName() + ".ExportActivity");
			startActivity(myIntent);
			return true;
		}
		return false;
	}


	/** Convenience function combining clearInfo and getInfo */
	public void refreshInfo() {
		clearInfo();
		populateTable();
	}

	/** Clears the table and field contents */
	public void clearInfo() {
		tableCellInfo.removeAllViews();
		tableLocationInfo.removeAllViews();
	}

	/** Retrieves and displays info */
	private void populateTable() {
		TimeDate = useful.formatDateTime("yyyy-MM-dd-HHmmssZ", new Date());
		LayoutParams lp = new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

//		addCellInfo(tableCellInfo, pi, lp);
	}

	private void formatTabs(TabWidget tw){
		for (int i = 0; i < tw.getChildCount(); i++) {

			tw.getChildAt(i).getLayoutParams().height = 80;

			final TextView tv = (TextView) tw.getChildAt(i).findViewById(android.R.id.title);        
			tv.setTextColor(this.getResources().getColorStateList(R.drawable.text_tab_indicator));
			tv.setTextSize(12);
		} 
	}

	private String tableToString(TableLayout t) {
		String res = "";

		for (int i=0; i <= t.getChildCount()-1; i++){
			TableRow row = (TableRow) t.getChildAt(i);

			for (int j=0; j <= row.getChildCount()-1; j++){
				View v = row.getChildAt(j);

				try {
					if(v.getClass() == Class.forName("android.widget.TextView")){
						TextView tmp = (TextView) v;
						res += tmp.getText();

						if(j==0){res += " ";}
					} else if(v.getClass() == Class.forName("android.widget.EditText")){
						EditText tmp = (EditText) v;
						res += tmp.getText().toString();
					} else {
						//do nothing
					}
				} catch (Exception e) {
					res = e.toString();
					Log.e(TAG, "^ tableToString: " + res);
				}
			}
			res +="\n";
		}
		return res;
	}

	 private void CenterLocation(GeoPoint centerGeoPoint, MapView mv)
	 {
		 if(mv != null){
			 mv.getController().animateTo(centerGeoPoint);
		 } else {
			 Log.e(TAG, "^ MapView is null");
		 }
	 };
	 
	/** Called when the application is minimised */
	@Override
	protected void onPause()
	{
		super.onPause();
	}

	/** Called when the application resumes */
	@Override
	protected void onResume()
	{
		super.onResume();
	}
}
