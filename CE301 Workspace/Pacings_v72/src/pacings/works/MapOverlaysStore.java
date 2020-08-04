package pacings.works;

import java.util.HashMap;
import java.util.Map;

public class MapOverlaysStore {
	static protected Map<Integer, MapOverlayItemStore> moss = new HashMap<Integer, MapOverlayItemStore>();
	static protected boolean randbool = false;

	public MapOverlaysStore() {
	}

	public static void clearMOSS() {
		moss.clear();
	}
}
