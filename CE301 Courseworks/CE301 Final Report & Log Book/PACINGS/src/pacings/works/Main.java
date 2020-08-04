package pacings.works;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

import com.google.android.maps.MapView;

public class Main extends TabActivity {
	final String TAG = this.getClass().getName();
	private TabHost tabHost;
	private MapView myMapView;
	private ListView myList;
	private EditText fldSearchLocation;
	private Spinner spinner;
	private Button btnSearch;
	private Button btnBack;
	private Button btnShowHide;
	private String localDbLocation;
	private String localDbFullPath;
	private String spinner_selected;
	private SQLiteDatabase db;
	private TabSpec tabSpec;
	private int visibilitySwitch;
	private int visibilityMenu;
	private Intent mapintent;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "[[[ onCreate");
		super.onCreate(savedInstanceState);
		// Loading maintabs.xml layout view
		setContentView(R.layout.maintabs);
		// Initialising variables
		spinner_selected = "";
		visibilitySwitch = View.VISIBLE;
		// List initialisation
		final Object data = getLastNonConfigurationInstance();
		if (data == null) {
			Log.d(TAG, "[[[ Creating myList!");
			myList = (ListView) findViewById(R.id.result_list);
		}
		// Setting up Tabs
		tabHost = getTabHost();
		tabSpec = tabHost.newTabSpec("tab_location").setIndicator("Map",
				getResources().getDrawable(R.drawable.ruffled_map_s))
				.setContent(R.id.main_scrollview_location_info);
		//
		tabHost.addTab(tabHost.newTabSpec("tab_device").setIndicator("Info",
				getResources().getDrawable(R.drawable.info_s)).setContent(
				R.id.main_ll_get_info));
		//
		tabHost.addTab(tabSpec);
		//
		mapintent = new Intent(this, MapTabView.class)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		tabSpec.setContent(mapintent);
		// Set up MapView
		if (myMapView == null) {
			myMapView = (MapView) findViewById(R.id.mapview);
		}
		// Set up Selection Spinner
		spinner = (Spinner) findViewById(R.id.locations_types_spinner);
		ArrayAdapter<CharSequence> arrayadapter = ArrayAdapter
				.createFromResource(this, R.array.locations_types_array,
						android.R.layout.simple_spinner_item);
		arrayadapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(arrayadapter);
		spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
		// Set up Text field
		fldSearchLocation = (EditText) findViewById(R.id.editRoomName);
		// Set up DB location paths to SD Card Memory
		localDbLocation = Environment.getExternalStorageDirectory()
				+ getString(R.string.sd_db_location);
		localDbFullPath = localDbLocation + getString(R.string.sd_db_name);
		// Set up Search Button + Listener
		btnSearch = (Button) findViewById(R.id.btnSearch);
		btnSearch.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (myList != null && myList.getVisibility() == View.GONE)
					showHideListItem();
				if (spinner_selected.equals("None")) {
					// showToast("Please select location type.",
					// Toast.LENGTH_SHORT, Gravity.TOP, 0, 0);
					showCustomToast("Please select location type.",
							Toast.LENGTH_LONG, Gravity.TOP, 0, 0);
				} else {
					populateList(spinner_selected);
				}
			}
		});
		// Set up List + Listener
		myList = (ListView) findViewById(R.id.result_list);
		myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				MenuStore.listonclickid = id;
				listOnClick(v, pos, id);
				MapStateStore.mapclear = false;
			}
		});
		// Set up Back Button + Listener
		btnBack = (Button) findViewById(R.id.btnBack);
		btnBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showHideListItem();
			}
		});
		// Set up Show/Hide Menu Button + Listener
		visibilityMenu = View.VISIBLE;
		btnShowHide = (Button) findViewById(R.id.btnShowHide);
		btnShowHide.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (visibilityMenu == View.VISIBLE && !MenuStore.showhidemenu) {
					visibilityMenu = View.GONE;
				} else {
					visibilityMenu = View.VISIBLE;
				}
				setVisibilityMenu(visibilityMenu);
			}
		});
		// Tab listener
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				if (tabId == "tab_device") {
					MapOverlaysStore.randbool = false;
				} else {
					MapOverlaysStore.randbool = true;
				}
			}
		});
		// Restore layout
		Restoration(savedInstanceState);
		// Uncomment/Comment the line below to lock/unclock the screen
		// orientation
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	private void Restoration(Object savedInstanceState) {
		if (savedInstanceState != null) {
			if (MenuStore.spinnerselection != -1) {
				spinner.setSelection(MenuStore.spinnerselection);
				this.fldSearchLocation.setText(MenuStore.searchbar);
				populateList(spinner.getSelectedItem().toString());
				spinner_selected = spinner.getSelectedItem().toString();
			}
			if (MenuStore.listitemselected) {
				visibilitySwitch = View.GONE;
			} else {
				visibilitySwitch = View.VISIBLE;
			}
			if (MenuStore.listonclickid != (long) -1) {
				listItemClicked((ListView) findViewById(R.id.result_list),
						MenuStore.itemPosition, MenuStore.listonclickid);
				showHideListItem();
			}
			if (MenuStore.showhidemenu) {
				setVisibilityMenu(View.GONE);
			} else {
				setVisibilityMenu(View.VISIBLE);
			}
		}
	}

	private void setVisibilityMenu(int visibilityState) {
		TableRow row1 = (TableRow) findViewById(R.id.row1);
		TableRow row2 = (TableRow) findViewById(R.id.row2);
		TableRow row3 = (TableRow) findViewById(R.id.row3);
		TableRow row4 = (TableRow) findViewById(R.id.row4);
		row1.setVisibility(visibilityState);
		row2.setVisibility(visibilityState);
		row3.setVisibility(visibilityState);
		row4.setVisibility(visibilityState);
		if (visibilityState == TableLayout.VISIBLE) {
			btnShowHide.setText(getString(R.string.label_hide));
			MenuStore.showhidemenu = false;
		} else {
			btnShowHide.setText(getString(R.string.label_show));
			MenuStore.showhidemenu = true;
		}
	}

	// @SuppressWarnings("unchecked")
	private void listOnClick(View v, int pos, long id) {
		MapStateStore.mapclear = false;
		MenuStore.listItemsIds.clear();
		listItemClicked(v, pos, id);
	}

	@SuppressWarnings("unchecked")
	public void listItemClicked(View v, int pos, long id) {
		try {
			HashMap<String, Object> selection = (HashMap<String, Object>) myList
					.getItemAtPosition(pos);
			MenuStore.itemPosition = pos;
			MenuStore.listonclickid = id;
			String itemid = "";
			char sel = spinner_selected.charAt(0);
			switch (sel) {
			case 'R':
				itemid = selection.get("roomid").toString();
				showDetails(getSQLStatement(spinner_selected, itemid.trim()));
				break;
			case 'B':
				itemid = selection.get("buildingid").toString();
				showDetails(getSQLStatement(spinner_selected, itemid.trim()));
				break;
			case 'E':
				itemid = selection.get("entranceid").toString();
				showDetails(getSQLStatement(spinner_selected, itemid.trim()));
				break;
			case 'S':
				itemid = selection.get("squareid").toString();
				showDetails(getSQLStatement(spinner_selected, itemid.trim()));
				break;
			case 'C':
				itemid = selection.get("campusid").toString();
				showDetails(getSQLStatement(spinner_selected, itemid.trim()));
				break;
			default:
				itemid = "";
				break;
			}
			MenuStore.listItemsIds.add(id);
			// Uncomment android-toast for debugging
			// showToast("itemid: " + itemid, Toast.LENGTH_SHORT, Gravity.TOP,
			// 0,
			// 0);
			showHideListItem();
		} catch (Exception e) {
			Log.e(TAG, "[[[ listOnClick error: " + e.getMessage());
		}
	}

	public void showHideListItem() {
		if (visibilitySwitch == View.VISIBLE) {
			MenuStore.listitemselected = true;
			btnBack.setVisibility(View.VISIBLE);
			myList.setVisibility(View.GONE);
			btnSearch.setVisibility(View.GONE);
			fldSearchLocation.setVisibility(View.GONE);
			findViewById(R.id.label_select).setVisibility(View.GONE);
			findViewById(R.id.label_search).setVisibility(View.GONE);
			spinner.setVisibility(View.GONE);
			findViewById(R.id.layoutdetailsTable).setVisibility(View.VISIBLE);
			findViewById(R.id.btnShowHide).setVisibility(View.GONE);
			visibilitySwitch = View.GONE;
		} else {
			MenuStore.listitemselected = false;
			btnBack.setVisibility(View.GONE);
			myList.setVisibility(View.VISIBLE);
			btnSearch.setVisibility(View.VISIBLE);
			fldSearchLocation.setVisibility(View.VISIBLE);
			findViewById(R.id.label_select).setVisibility(View.VISIBLE);
			findViewById(R.id.label_search).setVisibility(View.VISIBLE);
			spinner.setVisibility(View.VISIBLE);
			findViewById(R.id.layoutdetailsTable).setVisibility(View.GONE);
			findViewById(R.id.btnShowHide).setVisibility(View.VISIBLE);
			visibilitySwitch = View.VISIBLE;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		MenuStore.spinnerselection = spinner.getSelectedItemPosition();
		MenuStore.searchbar = this.fldSearchLocation.getText().toString();
		super.onSaveInstanceState(savedInstanceState);
	}

	public void showDetails(String sqlstatement) {
		try {
			openDBConnection();
			Cursor cur = db.rawQuery(sqlstatement, null);
			cur.moveToFirst();
			TextView[] tvarr = new TextView[9];
			tvarr[0] = (TextView) findViewById(R.id.location_id_dtbl);
			tvarr[1] = (TextView) findViewById(R.id.location_name_dtbl);
			tvarr[2] = (TextView) findViewById(R.id.location_altname_dtbl);
			tvarr[3] = (TextView) findViewById(R.id.location_info_dtbl);
			tvarr[4] = (TextView) findViewById(R.id.location_notes_dtbl);
			tvarr[5] = (TextView) findViewById(R.id.location_avtype_dtbl);
			tvarr[6] = (TextView) findViewById(R.id.location_disabledaccess_dtbl);
			tvarr[7] = (TextView) findViewById(R.id.location_latit_dtbl);
			tvarr[8] = (TextView) findViewById(R.id.location_longtit_dtbl);
			// Display "n/a" when information not available
			for (int j = 0; j < tvarr.length; j++) {
				tvarr[j].setText("n/a");
			}
			while (cur.isAfterLast() == false) {
				for (int i = 0; i < cur.getColumnCount(); i++) {
					tvarr[i].setText(setTextIfBlank(cur.getString(i).trim()
							.replace("''", "'"), 1, '-'));
				}
				cur.moveToNext();
			}
			cur.close();
			closeDBConnection();
			//
			if (!MenuStore.listItemsIds.contains(MenuStore.listonclickid)
					&& !MapStateStore.mapclear)
				addLocationOverlayToMap(tvarr[0].getText().toString().trim(),
						tvarr[7].getText().toString().trim(), tvarr[8]
								.getText().toString().trim(), tvarr[3]
								.getText().toString().trim(), tvarr[1]
								.getText().toString().trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addLocationOverlayToMap(String id, String sloc_latit,
			String sloc_longt, String info, String name) {
		try {

			int loc_lat = (int) (Double.parseDouble(sloc_latit) * 1E6);
			int loc_longt = (int) (Double.parseDouble(sloc_longt) * 1E6);
			if (!MapOverlaysStore.randbool) {
				MapOverlayItemStore mois = new MapOverlayItemStore(loc_lat,
						loc_longt, info, name, spinner.getItemAtPosition(
								MenuStore.spinnerselection).toString());
				MapOverlaysStore.moss.put(Integer.parseInt(id), mois);
			}
		} catch (Exception e) {
			Log.e(TAG, "[[[ addLocationOverlaySD error: " + e.getMessage());
		}
	}

	private void populateList(String spinner_selected) {
		if (myList == null) {
			Log.d(TAG, "[[[ mylist is null!");
			myList = (ListView) findViewById(R.id.result_list);
		}
		if (myList.getAdapter() != null) {
			myList.setAdapter(null);
		}
		try {
			List<Map<String, Object>> drInfo = new ArrayList<Map<String, Object>>();
			openDBConnection();
			Cursor cur = db.rawQuery(getSQLStatement(spinner_selected, "-1"),
					null);
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {
				Map<String, Object> map = new HashMap<String, Object>();
				for (int i = 0; i < cur.getColumnCount(); i++) {
					map.put(cur.getColumnName(i), setTextIfBlank(cur.getString(
							i).trim().replace("''", "'"), 1, '-'));
				}
				drInfo.add(map);
				cur.moveToNext();
			}
			cur.close();
			closeDBConnection();
			//
			myList
					.setAdapter(new SimpleAdapter(this, drInfo,
							R.layout.listitem, cur.getColumnNames(), new int[] {
									R.id.room_id, R.id.room_name,
									R.id.building_name }));
		} catch (Exception e) {
			Log.e(TAG, "[[[ populateList error: " + e.getMessage());
		}
	}

	private String getSQLStatement(String spinner_selected, String itemid) {
		String statement = "";
		char sel = spinner_selected.charAt(0);
		switch (sel) {
		case 'R':
			statement = getRooms(itemid);
			break;
		case 'B':
			statement = getBuildings(itemid);
			break;
		case 'E':
			statement = getEntrances(itemid);
			break;
		case 'S':
			statement = getSquares(itemid);
			break;
		case 'C':
			statement = getCampuses(itemid);
			break;
		default:
			statement = "";
			break;
		}
		return statement;
	}

	public String getRooms(String itemid) {
		String statement = "";
		String sSelect = "";
		String sFrom = "";
		String sWhere = "";
		String sOrder = "";
		if (!itemid.equals("-1")) {
			sSelect = "SELECT r.roomid, r.roomname, r.roomaltname, r.roominfo, r.roomnotes, r.roomavtype, r.roomdisabledaccess, r.room_latit, r.room_longtit";
			sFrom = " FROM tblroom r";
			sWhere = " WHERE r.roomid = '" + itemid + "' ";
			sOrder = ";";// " ORDER BY r.roomname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
			Log.e(TAG, "[[[ ROOM ID INFO: " + statement);
		} else if (fldSearchLocation.getText().toString().trim().equals("")) {
			sSelect = "SELECT r.roomid, r.roomname, r.roomaltname";
			sFrom = " FROM tblroom r";
			sWhere = "";
			sOrder = " ORDER BY r.roomname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		} else {
			sSelect = "SELECT r.roomid, r.roomname, r.roomaltname";
			sFrom = " FROM tblroom r";
			sWhere = " WHERE r.roomname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''")
					+ "%' OR r.roomaltname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''") + "%'";
			sOrder = " ORDER BY r.roomname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		}
		return statement;
	}

	public String getBuildings(String itemid) {
		String statement = "";
		String sSelect = "";
		String sFrom = "";
		String sWhere = "";
		String sOrder = "";
		if (!itemid.equals("-1")) {
			sSelect = "SELECT b.buildingid, b.buildingname, b.buildingaltname, b.buildinginfo, b.buildingnotes, b.buildingavtype, b.buildingdisabledaccess, b.building_latit, b.building_longtit";
			sFrom = " FROM tblbuilding b";
			sWhere = " WHERE b.buildingid = '" + itemid + "' ";
			sOrder = ";";// " ORDER BY r.roomname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
			Log.e(TAG, "[[[ ROOM ID INFO: " + statement);
		} else if (fldSearchLocation.getText().toString().trim().equals("")) {
			sSelect = "SELECT b.buildingid, b.buildingname, b.buildingaltname";
			sFrom = " FROM tblbuilding b";
			sWhere = "";
			sOrder = " ORDER BY b.buildingname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		} else {
			sSelect = "SELECT b.buildingid, b.buildingname, b.buildingaltname";
			sFrom = " FROM tblbuilding b";
			sWhere = " WHERE b.buildingname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''")
					+ "%' OR b.buildingaltname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''") + "%'";
			sOrder = " ORDER BY b.buildingname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		}
		return statement;
	}

	public String getEntrances(String itemid) {
		String statement = "";
		String sSelect = "";
		String sFrom = "";
		String sWhere = "";
		String sOrder = "";
		if (!itemid.equals("-1")) {
			sSelect = "SELECT e.entranceid, e.entrancename, e.entrancealtname, e.entranceinfo, e.entrancenotes, e.entranceavtype, e.entrancedisabledaccess, e.entrance_latit, e.entrance_longtit";
			sFrom = " FROM tblentrance e";
			sWhere = " WHERE e.entranceid = '" + itemid + "' ";
			sOrder = ";";// " ORDER BY r.roomname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
			Log.e(TAG, "[[[ ROOM ID INFO: " + statement);
		} else if (fldSearchLocation.getText().toString().trim().equals("")) {
			sSelect = "SELECT e.entranceid, e.entrancename, e.entrancealtname";
			sFrom = " FROM tblentrance e";
			sWhere = "";
			sOrder = " ORDER BY e.entrancename ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		} else {
			sSelect = "SELECT e.entranceid, e.entrancename, e.entrancealtname";
			sFrom = " FROM tblentrance e";
			sWhere = " WHERE e.entrancename like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''")
					+ "%' OR e.entrancealtname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''") + "%'";
			sOrder = " ORDER BY e.entrancename ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		}
		return statement;
	}

	public String getSquares(String itemid) {
		String statement = "";
		String sSelect = "";
		String sFrom = "";
		String sWhere = "";
		String sOrder = "";
		if (!itemid.equals("-1")) {
			sSelect = "SELECT s.squareid, s.squarename, s.squarealtname, s.squareinfo, s.squarenotes, s.squareavtype, s.squaredisabledaccess, s.square_latit, s.square_longtit";
			sFrom = " FROM tblsquare s";
			sWhere = " WHERE s.squareid = '" + itemid + "' ";
			sOrder = ";";// " ORDER BY r.roomname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
			Log.e(TAG, "[[[ ROOM ID INFO: " + statement);
		} else if (fldSearchLocation.getText().toString().trim().equals("")) {
			sSelect = "SELECT s.squareid, s.squarename, s.squarealtname";
			sFrom = " FROM tblsquare s";
			sWhere = "";
			sOrder = " ORDER BY s.squarename ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		} else {
			sSelect = "SELECT s.squareid, s.squarename, s.squarealtname";
			sFrom = " FROM tblsquare s";
			sWhere = " WHERE s.squarename like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''")
					+ "%' OR s.squarealtname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''") + "%'";
			sOrder = " ORDER BY s.squarename ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		}
		return statement;
	}

	public String getCampuses(String itemid) {
		String statement = "";
		String sSelect = "";
		String sFrom = "";
		String sWhere = "";
		String sOrder = "";
		if (!itemid.equals("-1")) {
			sSelect = "SELECT c.campusid, c.campusname, c.campusaltname, c.campusinfo, c.campusnotes, c.campusavtype, c.campusdisabledaccess, c.campus_latit, c.campus_longtit";
			sFrom = " FROM tblcampus c";
			sWhere = " WHERE c.campusid = '" + itemid + "' ";
			sOrder = ";";// " ORDER BY r.roomname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
			Log.e(TAG, "[[[ ROOM ID INFO: " + statement);
		} else if (fldSearchLocation.getText().toString().trim().equals("")) {
			sSelect = "SELECT c.campusid, c.campusname, c.campusaltname";
			sFrom = " FROM tblcampus c";
			sWhere = "";
			sOrder = " ORDER BY c.campusname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		} else {
			sSelect = "SELECT c.campusid, c.campusname, c.campusaltname";
			sFrom = " FROM tblcampus c";
			sWhere = " WHERE c.campusname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''")
					+ "%' OR c.campusaltname like '"
					+ fldSearchLocation.getText().toString().trim().replace(
							"'", "''") + "%'";
			sOrder = " ORDER BY c.campusname ASC";
			statement = sSelect + sFrom + sWhere + sOrder;
		}
		return statement;
	}

	public String fixString(String s) {
		return s.trim().replace("'", "''");
	}

	private void openDBConnection() {
		try {
			db = SQLiteDatabase.openDatabase(localDbFullPath, null,
					SQLiteDatabase.OPEN_READONLY);
			Log.e(TAG, "[[[ DB OPENED.");
		} catch (Exception e) {
			Log.e(TAG, "[[[ Error opening DB: " + e.getMessage());
		}
	}

	private void closeDBConnection() {
		try {
			db.close();
			Log.e(TAG, "[[[ DB CLOSED.");
		} catch (Exception e) {
			Log.e(TAG, "[[[ Error closing DB: " + e.getMessage());
		}
	}

	private String setTextIfBlank(String str, int count, char ch) {
		if (str.equals("")) {
			char[] chars = new char[count];
			while (count > 0)
				chars[--count] = ch;
			return String.valueOf(chars);
		} else {
			return str;
		}
	}

	public class MyOnItemSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			// Uncomment android-toast for debugging
			// Toast.makeText(parent.getContext(),
			// "Selected " + parent.getItemAtPosition(pos).toString(),
			// Toast.LENGTH_LONG).show();
			spinner_selected = parent.getItemAtPosition(pos).toString();
			MenuStore.spinnerselection = pos;
			populateList(parent.getItemAtPosition(pos).toString());
		}

		@SuppressWarnings("unchecked")
		public void onNothingSelected(AdapterView parent) {
			// Do nothing.
		}
	}

	public void showToast(String message, int duration, int location,
			int x_offset, int y_offset) {
		Toast toast = Toast.makeText(this, message, duration);
		toast.setGravity(location, x_offset, y_offset);
		toast.show();
	}

	public void showCustomToast(String message, int duration, int location,
			int x_offset, int y_offset) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.toast_layout,
				(ViewGroup) findViewById(R.id.toast_layout_root));

		ImageView image = (ImageView) layout.findViewById(R.id.image);
		image.setImageResource(R.drawable.marker_android);
		TextView text = (TextView) layout.findViewById(R.id.text);
		text.setText(message);

		Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_VERTICAL, x_offset, y_offset);
		toast.setDuration(duration);
		toast.setView(layout);
		toast.show();
	}

	/** Called when the application is minimised */
	@Override
	protected void onPause() {
		super.onPause();
		Log.e(TAG, "[[[ onPAUSE");
	}

	/** Called when the application resumes */
	@Override
	protected void onResume() {
		super.onResume();
		Log.e(TAG, "[[[ onRESUME ");
	}
}