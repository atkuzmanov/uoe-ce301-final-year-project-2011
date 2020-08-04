package pacings.works;

public class MapOverlayItemStore {
	protected int xlocation;
	protected int ylocation;
	protected String infolocation;
	protected String locationname;
	protected String spinnerselection;

	public MapOverlayItemStore() {
		xlocation = -1;
		ylocation = -1;
		infolocation = "";
		locationname = "";
		spinnerselection = "";
	}

	public MapOverlayItemStore(int x, int y, String info, String name,
			String sselect) {
		this.xlocation = x;
		this.ylocation = y;
		this.infolocation = info;
		this.locationname = name;
		this.spinnerselection = sselect;
	}
}
