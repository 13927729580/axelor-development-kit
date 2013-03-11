package com.axelor.web.db;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import com.axelor.db.JPA;
import com.axelor.db.JpaModel;
import com.axelor.db.NameColumn;
import com.axelor.db.Query;
import com.axelor.db.VirtualColumn;
import com.axelor.db.Widget;

@Entity
public class Contact extends JpaModel {

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Title title;

	@NotNull
	private String firstName;

	@NotNull
	private String lastName;

	@Widget(title = "Full Name", search = { "firstName", "lastName" })
	@NameColumn
	@VirtualColumn
	@Access(AccessType.PROPERTY)
	private String fullName;

	@NotNull
	private String email;

	private String phone;

	@Widget(title = "Address List")
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Address> addresses;
	
	public Contact() {
	}

	public Contact(String firstName, String lastName, String email, String phone) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phone = phone;
	}
	
	public Title getTitle() {
		return title;
	}

	public void setTitle(Title title) {
		this.title = title;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getFullName() {
		fullName = computeFullName();
		return fullName;
	}

	protected String computeFullName() {
		if (title == null) return firstName + " " + lastName;
		return title.getName() + " " + firstName + " " + lastName;
	}
	
	public static Query<Contact> all() {
		return JPA.all(Contact.class);
	}
	
	public Contact save() {
		return JPA.save(this);
	}
}
