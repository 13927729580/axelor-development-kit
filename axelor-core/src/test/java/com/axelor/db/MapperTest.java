package com.axelor.db;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.axelor.BaseTest;
import com.axelor.db.mapper.Mapper;
import com.axelor.test.db.Contact;
import com.axelor.test.db.TypeCheck;

public class MapperTest extends BaseTest {

	Mapper mapper = Mapper.of(Contact.class);

	@Test
	public void testGet() {
	
		Contact contact = Contact.all().fetchOne();
		
		String firstName = contact.getFirstName();
		String lastName = contact.getLastName();
		Long id = contact.getId();
		
		Assert.assertEquals(firstName, mapper.get(contact, "firstName"));
		Assert.assertEquals(lastName, mapper.get(contact, "lastName"));
		Assert.assertEquals(id, mapper.get(contact, "id"));
	}
	
	@Test
	public void testSet() {
	
		Contact contact = Contact.all().fetchOne();
		
		contact.setFirstName("Some");
		contact.setLastName("Name");
		
		Assert.assertEquals("Some", mapper.get(contact, "firstName"));
		Assert.assertEquals("Name", mapper.get(contact, "lastName"));
	}
	
	@Test
	public void testBean() {
		Map<String, Object> values = getDemoData();
		Contact contact = JPA.edit(Contact.class, values);
		
		Assert.assertEquals("Some", contact.getFirstName());
		Assert.assertEquals("Name", contact.getLastName());
		Assert.assertNotSame("Mr. My Name", contact.getFullName());
		Assert.assertEquals("Mr. Some Name", contact.getFullName());
		
		Assert.assertNotNull(contact);
		Assert.assertNotNull(contact.getId());
		Assert.assertNotNull(contact.getDateOfBirth());
		
		LocalDate date = contact.getDateOfBirth();
		Assert.assertEquals(1975, date.getYear());
		Assert.assertEquals(3, date.getMonthOfYear());
		Assert.assertEquals(23, date.getDayOfMonth());
		
		Assert.assertNotNull(contact.getTitle());
		Assert.assertEquals("Mr.", contact.getTitle().getName());
		
		Assert.assertNotNull(contact.getGroups());
		Assert.assertEquals(1, contact.getGroups().size());
		Assert.assertEquals("Business", contact.getGroups().get(0).getTitle());
	}
	
	private Map<String, Object> getDemoData() {
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("id", 1L);
		values.put("firstName", "Some");
		values.put("lastName", "Name");
		values.put("fullName", "My Name");  // test readonly
		values.put("dateOfBirth", "1975-02-27");
		
		Map<String, Object> title = new HashMap<String, Object>();
		title.put("code", "mr");
		title.put("name", "Mr.");
		values.put("title", title);
		
		Set<Map<String, Object>> groups = new HashSet<Map<String, Object>>();
		Map<String, Object> family = new HashMap<String, Object>();
		family.put("code", "family");
		family.put("title", "Family");
		groups.add(family);
		values.put("groups", groups);
		
		return values;
	}
	
	@Test
	public void testTypes() {
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("boolValue", true);
		values.put("intValue", 121);
		values.put("longValue", 199L);
		values.put("doubleValue", 23.12);
		values.put("decimalValue1", "233.232344");
		values.put("dateTime1", "2011-01-11");
		values.put("localDate1", "1111-11-11");
		
		values.put("boolValue2", null);
		values.put("intValue2", null);
		values.put("longValue2", null);
		values.put("doubleValue2", null);
		values.put("decimalValue2", null);
		values.put("dateTime2", null);
		values.put("localDate2", null);
		
		TypeCheck bean = JPA.edit(TypeCheck.class, values);
		
		Assert.assertEquals(true, bean.getBoolValue());
		Assert.assertTrue(121 == bean.getIntValue());
		Assert.assertTrue(199L == bean.getLongValue());
		Assert.assertTrue(23.12 == bean.getDoubleValue());
		
		Assert.assertTrue(false == bean.isBoolValue2());
		Assert.assertTrue(0 == bean.getIntValue2());
		Assert.assertTrue(0L == bean.getLongValue2());
		Assert.assertTrue(0.0 == bean.getDoubleValue2());
		
		Assert.assertEquals("233.23", bean.getDecimalValue1().toString());
		Assert.assertTrue(bean.getDateTime1().getYear() == 2011);
		Assert.assertTrue(bean.getLocalDate1().getYear() == 1111);
	}
}
