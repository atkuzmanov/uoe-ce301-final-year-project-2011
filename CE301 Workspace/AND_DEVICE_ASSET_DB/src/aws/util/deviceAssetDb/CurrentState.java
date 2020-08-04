package aws.util.deviceAssetDb;

import android.widget.ListAdapter;

public class CurrentState {
	private String inventoryNo = "";
	private String barcode = "";
	
	private String Subsystem = "";
	
	private String Vendor_name = "";
	private String Device_name = "";
	private String Subsystem_name = "";
	
	private ListAdapter Adapter;

	private int mVisibility;
	private String mResults;
	
	public String getInventoryNo() {
		return inventoryNo;
	}

	public String getBarcode() {
		return barcode;
	}

	public String getSubsystem() {
		return Subsystem;
	}

	public String getVendor_name() {
		return Vendor_name;
	}

	public String getDevice_name() {
		return Device_name;
	}

	public String getSubsystem_name() {
		return Subsystem_name;
	}

	public ListAdapter getAdapter() {
		return Adapter;
	}

	public void setInventoryNo(String inventoryNo) {
		this.inventoryNo = inventoryNo;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public void setSubsystem(String subsystem) {
		Subsystem = subsystem;
	}

	public void setVendor_name(String vendorName) {
		Vendor_name = vendorName;
	}

	public void setDevice_name(String deviceName) {
		Device_name = deviceName;
	}

	public void setSubsystem_name(String subsystemName) {
		Subsystem_name = subsystemName;
	}

	public void setAdapter(ListAdapter adapter) {
		Adapter = adapter;
	}
	
	public int getVisibility() {
		return mVisibility;
	}
	public void setVisibility(int Visibility) {
		this.mVisibility = Visibility;
	}	
	
	public String getResults() {
		return mResults;
	}
	public void setResults(String Results) {
		this.mResults = Results;
	}	
	
}
