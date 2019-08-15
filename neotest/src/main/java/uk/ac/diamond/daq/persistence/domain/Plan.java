package uk.ac.diamond.daq.persistence.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Plan extends PersistableItem {

	@Relationship("CONSISTS_OF")
	private ArrayList<Trigger> triggers = new ArrayList<>();

	private Plan(Plan source, boolean New) {
		super(source);
		if (New) {
			this.triggers = source.triggers;
			this.ID = 0;
		}
		this.triggers = source.triggers;
	}

	public boolean addTrigger(Trigger thisOne) {
		if (this.triggers.contains(thisOne)) {
			return false;
		}
		increment();
		this.triggers.add(thisOne);
		return true;
	}

	public List<Trigger> getTriggers() {
		return this.triggers;
	}

	private void setTriggers() {
		this.triggers = null;
	}

	public Trigger getTrigger(int index) {
		return this.triggers.get(index);
	}

	@Override
	protected void increment() {
		this.previousVersion = new Plan(this, false);
	}

	public Plan() {
		super();
	}

}
