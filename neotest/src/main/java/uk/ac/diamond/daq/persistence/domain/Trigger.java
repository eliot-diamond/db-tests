package uk.ac.diamond.daq.persistence.domain;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type = "TRIGGERS")
public class Trigger extends PersistableItem {

	@EndNode
	private Scan scan;
	@StartNode
	private Plan plan;
	@Property("TRIGGERING_CONDITION")
	private String condition;

	private void setScan() {
		this.scan = null;
	}

	@Override
	protected void increment() {
		this.previousVersion = new Trigger(this);
		((Trigger) this.previousVersion).setScan();

	}

	protected Trigger(Trigger source) {
		super(source);
		this.scan = source.scan;
		this.condition = source.condition;
	}

	public Trigger() {
		super();
	}

	public void setScan(Scan theScan) {
		increment();
		this.scan = theScan;
	}

	public Scan getScan() {
		return this.scan;
	}

	public void setCondition(String theCondition) {
		increment();
		this.condition = theCondition;
	}
	
	public String getCondition() {
		return this.condition;
	}

}
