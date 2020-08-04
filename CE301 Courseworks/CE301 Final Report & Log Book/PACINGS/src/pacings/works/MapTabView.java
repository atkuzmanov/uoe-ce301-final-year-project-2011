package pacings.works;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import pacings.works.MyLocation.LocationResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

enum MENU_BUTTONS {
	SHOWSTART, CLEARMAP;
	public static MENU_BUTTONS lookUpByOrdinal(int i) {
		return MENU_BUTTONS.values()[i];
	}
}

public class MapTabView extends MapActivity {
	private final String TAG = this.getClass().getName();
	private OverlayItem overlayitem;
	private MyItemizedOverlay itemizedoverlay;
	private MyItemizedOverlay itemizedoverlayLocation;
	private List<Overlay> mapOverlays;
	private int lat;
	private int longt;
	private Location locateme;
	private MapView mapView;
	private Drawable drawable;
	private MyLocation myLocation;
	private Handler timeHandler;
	private long myStartTime;
	private int lat_overlay;
	private int longt_overlay;
	private GeoPoint newPoint;
	private GeoPoint myLocationPoint;
	private int init_map_anim_lat;
	private int init_map_anim_longt;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.maptabview);
		Log.d(TAG, "[[[ MapTabView ONCREATE! ");
		// Check if there are saved current location coordinates
		if (MapStateStore.myx != 0 && MapStateStore.myy != 0) {
			lat = MapStateStore.myx;
			longt = MapStateStore.myy;
		} else {
			lat = 0;
			longt = 0;
		}
		lat_overlay = 0;
		longt_overlay = 0;
		newPoint = null;
		//
		init_map_anim_lat = (int) (51.876111 * 1E6);
		init_map_anim_longt = (int) (0.945483 * 1E6);
		// Set up MapView
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(true);
		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.marker_default);
		itemizedoverlay = new MyItemizedOverlay(drawable, this);
		itemizedoverlayLocation = new MyItemizedOverlay(this.getResources()
				.getDrawable(R.drawable.user_loc), this);
		// Set up current location
		myLocation = new MyLocation();
		// Start listening for current location
		locationClick();
		// Set up refresh current location timers
		timeHandler = new Handler();
		myStartTime = 0L;
		myStartTime = System.currentTimeMillis();
		timeHandler.removeCallbacks(mUpdateTimeTask);
		timeHandler.postDelayed(mUpdateTimeTask, 10000);
		// Restore map overlays if saved items exist
		RestoreMapState();
		// Initially get current location
		updateFreshLocation();
		// Refresh map
		mapView.invalidate();
	}

	private void RestoreMapState() {
		Iterator<Integer> it = MapOverlaysStore.moss.keySet().iterator();
		while (it.hasNext()) {
			MapOverlayItemStore mtemp = MapOverlaysStore.moss.get(it.next());
			int lat = mtemp.xlocation;
			int longt = mtemp.ylocation;
			String title = mtemp.locationname;
			String info = mtemp.infolocation;
			String type = mtemp.spinnerselection;
			addOverlayItemOnMap(lat, longt, title, info, type);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void locationClick() {
		myLocation.getLocation(this, locationResult);
	}

	private LocationResult locationResult = new LocationResult() {
		@Override
		public void gotLocation(final Location location) {
			locateme = location;
			lat = (int) (locateme.getLatitude() * 1E6);
			longt = (int) (locateme.getLongitude() * 1E6);
			MapStateStore.myx = lat;
			MapStateStore.myy = longt;
			Log.d(TAG, "[[[ locationResult coor: " + lat + " " + longt);
		}
	};

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			final long start = myStartTime;
			long millis = SystemClock.uptimeMillis() - start;
			int seconds = (int) (millis / 1000);
			int minutes = seconds / 60;
			seconds = seconds % 60;
			// Update current location and refresh map
			updateFreshLocation();
			mapView.invalidate();
			//
			timeHandler.postAtTime(this, start
					+ (((minutes * 60) + seconds + 1) * 10000));
			timeHandler.postDelayed(mUpdateTimeTask, 10000);
		}
	};

	private void updateFreshLocation() {
		if (lat > 0 && longt > 0) {
			myLocationPoint = new GeoPoint(lat, longt);
			overlayitem = new OverlayItem(myLocationPoint, "You are here!", "");
			Log.d(TAG, "[[[ GeoPointC after: " + lat + " " + longt);
			if (!itemizedoverlayLocation.contains(overlayitem)) {
				itemizedoverlayLocation.clear();
				mapOverlays.remove(itemizedoverlayLocation);
				//
				itemizedoverlayLocation.addOverlay(overlayitem);
				mapOverlays.add(itemizedoverlayLocation);
				mapView.invalidate();
			}
		}
	}

	private void addOverlayItemOnMap(int lat, int longt, String title,
			String snip, String type) {
		changeOverlayItemType(type);
		newPoint = new GeoPoint(lat, longt);
		overlayitem = new OverlayItem(newPoint, title, snip);
		Log.d(TAG, "[[[ addLocationOverlayItem: " + lat_overlay + " "
				+ longt_overlay);
		if (!itemizedoverlay.contains(overlayitem)) {
			itemizedoverlay.addOverlay(overlayitem);
			mapOverlays.add(itemizedoverlay);
			mapView.invalidate();
		}
	}

	private void changeOverlayItemType(String type) {
		char sel = type.charAt(0);
		switch (sel) {
		case 'R':
			drawable = this.getResources().getDrawable(R.drawable.room);
			itemizedoverlay = new MyItemizedOverlay(drawable, this);
			break;
		case 'B':
			drawable = this.getResources().getDrawable(R.drawable.building);
			itemizedoverlay = new MyItemizedOverlay(drawable, this);
			break;
		case 'E':
			drawable = this.getResources().getDrawable(R.drawable.entrance);
			itemizedoverlay = new MyItemizedOverlay(drawable, this);
			break;
		case 'S':
			drawable = this.getResources().getDrawable(R.drawable.square);
			itemizedoverlay = new MyItemizedOverlay(drawable, this);
			break;
		case 'C':
			drawable = this.getResources().getDrawable(R.drawable.uni_campus);
			itemizedoverlay = new MyItemizedOverlay(drawable, this);
			break;
		case 'I':
			drawable = this.getResources()
					.getDrawable(R.drawable.start_marker);
			itemizedoverlay = new MyItemizedOverlay(drawable, this);
			break;
		default:
			// Do nothing
			break;
		}
	}

	/** Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_BUTTONS.SHOWSTART.ordinal(), 0,
				getString(R.string.lable_menu_showstart)).setIcon(
				android.R.drawable.ic_menu_compass);
		menu.add(0, MENU_BUTTONS.CLEARMAP.ordinal(), 0,
				getString(R.string.lable_menu_clearmap)).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	/** Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (MENU_BUTTONS.lookUpByOrdinal(item.getItemId())) {
		case SHOWSTART:
			getStartLocation();
			MapOverlaysStore.randbool = true;
			mapView.getController().animateTo(
					new GeoPoint(init_map_anim_lat, init_map_anim_longt));
			mapView.getController().setZoom(17);
			return true;
		case CLEARMAP:
			MapOverlaysStore.moss.clear();
			MapStateStore.mapclear = true;
			mapView.getOverlays().clear();
			mapOverlays.clear();
			mapView.invalidate();
			return true;
		}
		return false;
	}

	private void getStartLocation() {
		try {

			String localDbLocation = Environment.getExternalStorageDirectory()
					+ getString(R.string.sd_db_location);
			String localDbFullPath = localDbLocation
					+ getString(R.string.sd_db_name);

			SQLiteDatabase db = SQLiteDatabase.openDatabase(localDbFullPath,
					null, SQLiteDatabase.OPEN_READONLY);

			Cursor cur = db.rawQuery(getStartLocationQuery(), null);
			cur.moveToFirst();
			Map<String, Object> map = new HashMap<String, Object>();
			while (cur.isAfterLast() == false) {
				for (int i = 0; i < cur.getColumnCount(); i++) {
					map.put(cur.getColumnName(i), setTextIfBlank(cur.getString(
							i).trim().replace("''", "'"), 1, '-'));
				}
				cur.moveToNext();
			}
			cur.close();
			db.close();

			int loc_lat = (int) (Double.parseDouble((String) map
					.get("room_latit")) * 1E6);
			int loc_longt = (int) (Double.parseDouble((String) map
					.get("room_longtit")) * 1E6);

			if (!MapOverlaysStore.moss.containsKey(Integer
					.parseInt((String) map.get("roomid")))) {
				addOverlayItemOnMap(loc_lat, loc_longt, (String) map
						.get("roomname"), (String) map.get("roominfo"),
						"InfoStart");
			}
			MapOverlayItemStore mois2 = new MapOverlayItemStore(loc_lat,
					loc_longt, (String) map.get("roomname"), (String) map
							.get("roominfo"), "InfoStart");
			MapOverlaysStore.moss.put(Integer.parseInt((String) map
					.get("roomid")), mois2);
		} catch (Exception e) {
			e.printStackTrace();
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

	private String getStartLocationQuery() {
		String statement = "";
		String sSelect = "";
		String sFrom = "";
		String sWhere = "";
		String sOrder = "";
		String startLocation = "information";
		sSelect = "SELECT r.roomid, r.roomname, r.roominfo, r.room_latit, r.room_longtit";
		sFrom = " FROM tblroom r";
		sWhere = " WHERE r.roomname like '"
				+ startLocation.trim().replace("'", "''")
				+ "%' OR r.roomaltname like '"
				+ startLocation.trim().replace("'", "''") + "%'";
		sOrder = "";
		statement = sSelect + sFrom + sWhere + sOrder;
		Log.e(TAG, "[[[ statement: " + statement);
		return statement;
	}
}
