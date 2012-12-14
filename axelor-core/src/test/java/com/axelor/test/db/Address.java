package com.axelor.test.db;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.axelor.db.JPA;
import com.axelor.db.JpaModel;
import com.axelor.db.Query;

@Entity
@Table(name = "CONTACT_ADDRESS")
public class Address extends JpaModel {

	@NotNull
	private String street;

	private String area;

	@NotNull
	private String city;

	@NotNull
	private String zip;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Country country;

	@NotNull
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Contact contact;

	public Address() {
	}

	public Address(String street, String area, String city) {
		this.street = street;
		this.area = area;
		this.city = city;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}
	
	public static Query<Address> all() {
		return JPA.all(Address.class);
	}

}
