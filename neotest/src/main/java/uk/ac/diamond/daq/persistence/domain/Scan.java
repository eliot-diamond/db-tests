package uk.ac.diamond.daq.persistence.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

@NodeEntity(label = "Scan")
public abstract class Scan extends PersistableItem {

	@Property
	private Camera captureDevice;
	@Property
	private String name;

	private enum Camera {
		BIG_ONE, SMALL_ONE
	}

	public String setCaptureDevice(String camera) {
		switch (camera.toUpperCase()) {
		case "BIG":
		case "BIG_ONE":
			this.captureDevice = Camera.BIG_ONE;
			return "Camera set to the big one";
		case "SMALL":
		case "SMALL_ONE":
			this.captureDevice = Camera.SMALL_ONE;
			return "Camera set to the small one";
		default:
			return "Error, expected BIG or SMALL";
		}
	}

	public String getCaptureDevice() {
		return captureDevice.toString();
	}

	protected Scan(Scan source) {
		super(source);
		this.setCaptureDevice(source.getCaptureDevice());
	}

	public Scan() {
		super();
	}

	public void setName(String theName) {
		this.name = theName;
	}

	public String getName() {
		return this.name;
	}
}
