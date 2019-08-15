package uk.ac.diamond.daq.persistence.domain;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class ScanMap extends PersistableItem {

	@Relationship("MAPS")
	private HashMap<String, Scan> scans;

	public boolean addScan(String key, Scan value) {
		if (this.scans.containsKey(key) || this.scans.containsValue(value)) {
			return false;
		}
		increment();
		this.scans.put(key, value);
		return true;
	}

	public boolean updateScan(String key, Scan value) {
		this.increment();
		if (!this.scans.containsKey(key)) {
			return false;
		}
		increment();
		this.scans.remove(key);
		this.scans.put(key, value);
		return true;
	}

	public Map<String, Scan> getMap() {
		return this.scans;
	}
	
	private void setScans() {
		for (String name : this.scans.keySet()) {
			this.updateScan(name, null);
		}
	}

	@Override
	protected void increment() {
		this.previousVersion = new ScanMap(this);		
	}

	protected ScanMap(ScanMap source) {
		super(source);
		this.scans = null;
	}
	
	public ScanMap() {
		super();
	}

}
