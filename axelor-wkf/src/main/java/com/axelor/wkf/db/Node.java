package com.axelor.wkf.db;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.Widget;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.MetaAction;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity 
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@Table(name = "WORKFLOW_NODE")
public class Node extends AuditableModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@NotNull
	@Index(name = "WORKFLOW_NODE_NAME_IDX")
	private String name;

	@Widget(selection = "node.type.selection")
	@NotNull
	private String type = "";

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private MetaAction action;

	@Widget(readonly = true)
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "nextNode", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequence")
	private List<Transition> startTransitions;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "startNode", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequence")
	private List<Transition> endTransitions;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.type.TextType")
	private String description;

	private String ref;

	public Node() {
	}
	
	public Node(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public MetaAction getAction() {
		return action;
	}

	public void setAction(MetaAction action) {
		this.action = action;
	}

	public List<Transition> getStartTransitions() {
		return startTransitions;
	}

	public void setStartTransitions(List<Transition> startTransitions) {
		this.startTransitions = startTransitions;
	}
	
	/**
	 * Add the given {@link #Transition} item to the {@code startTransitions}.
	 *
	 * <p>
	 * It sets {@code item.nextNode = this} to ensure the proper relationship.
	 * </p>
	 */
	public void addStartTransition(Transition item) {
		if (startTransitions == null) {
			startTransitions = new ArrayList<Transition>();
		}
		startTransitions.add(item);
		item.setNextNode(this);
	}

	/**
	 * Remove the given {@link #Transition} item from the {@code startTransitions}.
	 *
	 */	
	public void removeStartTransition(Transition item) {
		if (startTransitions == null) {
			return;
		}
		startTransitions.remove(item);
	}
	
	/**
	 * Clear the {@code startTransitions} collection.
	 *
	 * <p>
	 * It calls the {@code this.flush()} method to avoid unexpected errors
	 * if any of the item in the collection is changed.
	 * </p>
	 */	
	public void clearStartTransitions() {
		if (startTransitions != null) {
			startTransitions.clear();
			this.flush();
		}
	}

	public List<Transition> getEndTransitions() {
		return endTransitions;
	}

	public void setEndTransitions(List<Transition> endTransitions) {
		this.endTransitions = endTransitions;
	}
	
	/**
	 * Add the given {@link #Transition} item to the {@code endTransitions}.
	 *
	 * <p>
	 * It sets {@code item.startNode = this} to ensure the proper relationship.
	 * </p>
	 */
	public void addEndTransition(Transition item) {
		if (endTransitions == null) {
			endTransitions = new ArrayList<Transition>();
		}
		endTransitions.add(item);
		item.setStartNode(this);
	}

	/**
	 * Remove the given {@link #Transition} item from the {@code endTransitions}.
	 *
	 */	
	public void removeEndTransition(Transition item) {
		if (endTransitions == null) {
			return;
		}
		endTransitions.remove(item);
	}
	
	/**
	 * Clear the {@code endTransitions} collection.
	 *
	 * <p>
	 * It calls the {@code this.flush()} method to avoid unexpected errors
	 * if any of the item in the collection is changed.
	 * </p>
	 */	
	public void clearEndTransitions() {
		if (endTransitions != null) {
			endTransitions.clear();
			this.flush();
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof Node)) return false;
		
		Node other = (Node) obj;
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
		tsh.add("name", this.getName());
		tsh.add("type", this.getType());
		tsh.add("ref", this.getRef());

		return tsh.omitNullValues().toString();
	}
	
	public static Node findByName(String name) {
		return Node.all()
				.filter("self.name = :name")
				.bind("name", name)
				.fetchOne();
	}

	/**
	 * Make the entity managed and persistent.
	 * 
	 * @see EntityManager#persist(Object)
	 */
	public Node persist() {
		return JPA.persist(this);
	}

	/**
	 * Merge the state of the entity into the current persistence context.
	 * 
	 * @see EntityManager#merge(Object)
	 */
	public Node merge() {
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
	public Node save() {
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
	 * Find a <code>Node</code> by <code>id</code>.
	 *
	 */
	public static Node find(Long id) {
		return JPA.find(Node.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>Node</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<Node> all() {
		return JPA.all(Node.class);
	}
	
	/**
	 * A shortcut method to <code>Node.all().filter(...)</code>
	 *
	 */
	public static Query<Node> filter(String filter, Object... params) {
		return JPA.all(Node.class).filter(filter, params);
	}
	
	public Object execute( ActionHandler actionHandler, Object...params ) { return true; };
	
}
