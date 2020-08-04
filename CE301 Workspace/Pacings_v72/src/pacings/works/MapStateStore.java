package pacings.works;

import java.util.ArrayList;

public class MapStateStore {

	static protected ArrayList<Integer> xlocations = new ArrayList<Integer>();
	static protected ArrayList<Integer> ylocations = new ArrayList<Integer>();
	static protected ArrayList<String> infolocations = new ArrayList<String>();
	static protected ArrayList<String> locationnames = new ArrayList<String>();
	static protected ArrayList<String> spinnerselections = new ArrayList<String>();
	static protected int myx = 0;
	static protected int myy = 0;
	static protected boolean mapclear = false;
	static protected boolean onpause = false;

	public MapStateStore() {
	}

	public static void addLocationStore(int x, int y, String info, String name,
			String sselect) {
		if (!(locationnames.contains((String) name))) {
			xlocations.add(x);
			ylocations.add(y);
			infolocations.add(info);
			locationnames.add(name);
			spinnerselections.add(sselect);
		}
	}

	public static void clear() {
		xlocations.clear();
		ylocations.clear();
		infolocations.clear();
		locationnames.clear();
		spinnerselections.clear();
		myx = 0;
		myy = 0;
		mapclear = true;
	}
}
