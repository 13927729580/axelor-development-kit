package com.axelor.wkf.db;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
@Table(name = "WORKFLOW_INSTANCE_COUNTER", uniqueConstraints = { @UniqueConstraint(name = "UNIQUE_WORKFLOW_INSTANCE_COUNTER_INSTANCE_NODE", columnNames = { "instance", "node" }) })
public class InstanceCounter extends AuditableModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Instance instance;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Node node;

	private Integer counter = 0;

	public InstanceCounter() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instance getInstance() {
		return instance;
	}

	public void setInstance(Instance instance) {
		this.instance = instance;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public Integer getCounter() {
		if (counter == null) return 0;
        return counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof InstanceCounter)) return false;
		
		InstanceCounter other = (InstanceCounter) obj;
		if (this.getId() != null && other.getId() != null) {
			return Objects.equal(this.getId(), other.getId());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(this);

		tsh.add("id", this.getId());
		tsh.add("counter", this.getCounter());

		return tsh.omitNullValues().toString();
	}
	
	public static InstanceCounter findByInstanceAndNode(Instance instance, Node node) {
		return InstanceCounter.all()
				.filter("self.instance = :instance AND self.node = :node")
				.bind("instance", instance)
				.bind("node", node)
				.fetchOne();
	}

	/**
	 * Make the entity managed and persistent.
	 * 
	 * @see EntityManager#persist(Object)
	 */
	public InstanceCounter persist() {
		return JPA.persist(this);
	}

	/**
	 * Merge the state of the entity into the current persistence context.
	 * 
	 * @see EntityManager#merge(Object)
	 */
	public InstanceCounter merge() {
		return JPA.merge(this);
	}

	/**
	 * Save the state of the entity.<br>
	 * <br>
	 * It uses either {@link #persist()} or {@link #merge()} and calls
	 * {@link #flush()} to synchronize values with database.
	 * 
	 * @see #persist(Model)
	 * @see #merge(Model)
	 * 
	 */
	public InstanceCounter save() {
		return JPA.save(this);
	}
	
	/**
	 * Remove the entity instance.
	 * 
	 * @see EntityManager#remove(Object)
	 */
	public void remove() {
		JPA.remove(this);
	}
	
	/**
	 * Refresh the state of the instance from the database, overwriting changes
	 * made to the entity, if any.
	 * 
	 * @see EntityManager#refresh(Object)
	 */
	public void refresh() {
		JPA.refresh(this);
	}
	
	/**
	 * Synchronize the persistence context to the underlying database.
	 * 
	 * @see EntityManager#flush()
	 */
	public void flush() {
		JPA.flush();
	}
	
	/**
	 * Find a <code>InstanceCounter</code> by <code>id</code>.
	 *
	 */
	public static InstanceCounter find(Long id) {
		return JPA.find(InstanceCounter.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>InstanceCounter</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<InstanceCounter> all() {
		return JPA.all(InstanceCounter.class);
	}
	
	/**
	 * A shortcut method to <code>InstanceCounter.all().filter(...)</code>
	 *
	 */
	public static Query<InstanceCounter> filter(String filter, Object... params) {
		return JPA.all(InstanceCounter.class).filter(filter, params);
	}
}
