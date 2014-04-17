/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;
import javax.persistence.PersistenceException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.axelor.auth.db.Group;
import com.axelor.common.FileUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.AbstractWidget;
import com.axelor.meta.schema.views.Field;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Selection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

@Singleton
public class ViewLoader extends AbstractLoader {
	
	@Inject
	private ObjectMapper objectMapper;

	@Override
	@Transactional
	protected void doLoad(Module module, boolean update) {
		for (URL file : MetaScanner.findAll(module.getName(), "views", "(.*?)\\.xml")) {
			log.debug("importing: {}", file.getFile());
			try {
				process(file.openStream(), module, update);
			} catch (IOException | JAXBException e) {
				throw Throwables.propagate(e);
			}
		}

		Set<?> unresolved = this.unresolvedKeys();
		if (unresolved.size() > 0) {
			log.error("unresolved items: {}", unresolved);
			throw new PersistenceException("There are some unresolve items, check the log.");
		}

		// generate default views
		importDefault(module);
	}

	private static <T> List<T> getList(List<T> list) {
		if (list == null) {
			return Lists.newArrayList();
		}
		return list;
	}

	private void process(InputStream stream, Module module, boolean update) throws JAXBException {
		final ObjectViews all = XMLViews.unmarshal(stream);

		for (AbstractView view : getList(all.getViews())) {
			importView(view, module, update);
		}

		for (Selection selection : getList(all.getSelections())) {
			importSelection(selection, module, update);
		}

		for (Action action : getList(all.getActions())) {
			importAction(action, module, update);
		}

		for (MenuItem item : getList(all.getMenus())) {
			importMenu(item, module, update);
		}

		for (MenuItem item: getList(all.getActionMenus())) {
			importActionMenu(item, module, update);
		}
	}
	
	private void importView(AbstractView view, Module module, boolean update) {

		String xmlId = view.getId();
		String name = view.getName();
		String type = view.getType();
		String modelName = view.getModel();

		if (StringUtils.isBlank(xmlId)) {
			if (isVisited(view.getClass(), name)) {
				log.error("duplicate view without 'id': {}", name);
				return;
			}
		} else if (isVisited(view.getClass(), xmlId)) {
			return;
		}

		log.debug("Loading view: {}", name);
		
		String xml = XMLViews.toXml(view, true);

		if (type.matches("tree|chart|portal|search")) {
			modelName = null;
		} else if (StringUtils.isBlank(modelName)) {
			throw new IllegalArgumentException("Invalid view, model name missing.");
		}

		if (modelName != null) {
			Class<?> model;
			try {
				model = Class.forName(modelName);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Invalid view, model not found: " + modelName);
			}
			modelName = model.getName();
		}
		
		MetaView entity = MetaView.findByID(xmlId);
		MetaView other = MetaView.findByName(name);
		if (entity == null) {
			entity = MetaView.findByModule(name, module.getName());
		}
		
		if (entity == null) {
			entity = new MetaView(name);
		}
		
		if (other == entity) {
			other = null;
		}

		// set priority higher to existing view
		if (entity.getId() == null && other != null && !Objects.equal(xmlId, other.getXmlId())) {
			entity.setPriority(other.getPriority() + 1);
		}

		if (entity.getId() != null && !update) {
			return;
		}

		entity.setXmlId(xmlId);
		entity.setTitle(view.getDefaultTitle());
		entity.setType(type);
		entity.setModel(modelName);
		entity.setModule(module.getName());
		entity.setXml(xml);

		entity = entity.save();
	}

	private void importSelection(Selection selection, Module module, boolean update) {

		String name = selection.getName();
		String xmlId = selection.getXmlId();

		if (StringUtils.isBlank(xmlId)) {
			if (isVisited(Selection.class, name)) {
				log.error("duplicate selection without 'id': {}", name);
				return;
			}
		} else if (isVisited(Selection.class, xmlId)) {
			return;
		}

		log.debug("Loading selection : {}", name);

		MetaSelect entity = MetaSelect.findByID(xmlId);
		MetaSelect other = MetaSelect.findByName(selection.getName());
		if (entity == null) {
			entity = MetaSelect.filter("self.name = ? AND self.module = ?", name, module.getName()).fetchOne();
		}

		if (entity == null) {
			entity = new MetaSelect(selection.getName());
		}
		
		if (other == entity) {
			other = null;
		}

		// set priority higher to existing view
		if (entity.getId() == null && other != null && !Objects.equal(xmlId, other.getXmlId())) {
			entity.setPriority(other.getPriority() + 1);
		}
		
		if (entity.getId() != null && !update) {
			return;
		}

		entity.clearItems();
		entity.setModule(module.getName());

		int sequence = 0;
		for(Selection.Option opt : selection.getOptions()) {
			MetaSelectItem item = new MetaSelectItem();
			item.setValue(opt.getValue());
			item.setTitle(opt.getDefaultTitle());
			item.setOrder(sequence++);
			entity.addItem(item);
			if (opt.getData() == null) {
				continue;
			}

			Map<String, Object> data = Maps.newHashMap();
			for (QName param : opt.getData().keySet()) {
				String paramName = param.getLocalPart();
				if (paramName.startsWith("data-")) {
					data.put(paramName.substring(5), opt.getData().get(param));
				}
			}
			try {
				item.setData(objectMapper.writeValueAsString(data));
			} catch (JsonProcessingException e) {
			}
		}

		entity.save();
	}

	private Set<Group> findGroups(String groups) {
		if (StringUtils.isBlank(groups)) {
			return null;
		}

		Set<Group> all = Sets.newHashSet();
		for(String name : groups.split(",")) {
			Group group = Group.all().filter("self.code = ?1", name).fetchOne();
			if (group == null) {
				log.info("Creating a new user group: {}", name);
				group = new Group();
				group.setCode(name);
				group.setName(name);
				group = group.save();
			}
			all.add(group);
		}

		return all;
	}

	private void importAction(Action action, Module module, boolean update) {

		if (isVisited(Action.class, action.getName())) {
			return;
		}

		log.debug("Loading action : {}", action.getName());

		Class<?> klass = action.getClass();
		Mapper mapper = Mapper.of(klass);

		MetaAction entity = MetaAction.findByName(action.getName());
		if (entity == null) {
			entity = new MetaAction(action.getName());
		}

		if (entity.getId() != null && !update) {
			return;
		}

		entity.setXml(XMLViews.toXml(action,  true));

		String model = (String) mapper.get(action, "model");
		entity.setModel(model);
		entity.setModule(module.getName());

		String type = klass.getSimpleName().replaceAll("([a-z\\d])([A-Z]+)", "$1-$2").toLowerCase();
		entity.setType(type);

		entity = entity.save();

		for (MetaMenu pending : this.resolve(MetaMenu.class, entity.getName())) {
			log.debug("Resolved menu: {}", pending.getName());
			pending.setAction(entity);
			pending.save();
		}
	}

	private void importMenu(MenuItem menuItem, Module module, boolean update) {

		if (isVisited(MenuItem.class, menuItem.getName())) {
			return;
		}

		log.debug("Loading menu : {}", menuItem.getName());

		MetaMenu menu = MetaMenu.findByName(menuItem.getName());
		if (menu == null) {
			menu = new MetaMenu(menuItem.getName());
		}
		
		if (menu.getId() != null && !update) {
			return;
		}

		menu.setPriority(menuItem.getPriority());
		menu.setTitle(menuItem.getDefaultTitle());
		menu.setIcon(menuItem.getIcon());
		menu.setModule(module.getName());
		menu.setTop(menuItem.getTop());
		menu.setLeft(menuItem.getLeft() == null ? true : menuItem.getLeft());
		menu.setMobile(menuItem.getMobile());

		menu.clearGroups();
		menu.setGroups(this.findGroups(menuItem.getGroups()));

		if (!Strings.isNullOrEmpty(menuItem.getParent())) {
			MetaMenu parent = MetaMenu.findByName(menuItem.getParent());
			if (parent == null) {
				log.debug("Unresolved parent : {}", menuItem.getParent());
				this.setUnresolved(MetaMenu.class, menuItem.getParent(), menu);
			} else {
				menu.setParent(parent);
			}
		}

		if (!StringUtils.isBlank(menuItem.getAction())) {
			MetaAction action = MetaAction.findByName(menuItem.getAction());
			if (action == null) {
				log.debug("Unresolved action: {}", menuItem.getAction());
				setUnresolved(MetaMenu.class, menuItem.getAction(), menu);
			} else {
				menu.setAction(action);
			}
		}

		menu = menu.save();

		for (MetaMenu pending : this.resolve(MetaMenu.class, menu.getName())) {
			log.debug("Resolved menu : {}", pending.getName());
			pending.setParent(menu);
			pending.save();
		}
	}

	private void importActionMenu(MenuItem menuItem, Module module, boolean update) {

		if (isVisited(MenuItem.class, menuItem.getName())) {
			return;
		}

		log.debug("Loading action menu : {}", menuItem.getName());

		MetaActionMenu menu = MetaActionMenu.findByName(menuItem.getName());
		if (menu == null) {
			menu = new MetaActionMenu(menuItem.getName());
		}

		if (menu.getId() != null && !update) {
			return;
		}

		menu.setTitle(menuItem.getDefaultTitle());
		menu.setModule(module.getName());
		menu.setCategory(menuItem.getCategory());

		if (!StringUtils.isBlank(menuItem.getParent())) {
			MetaActionMenu parent = MetaActionMenu.findByName(menuItem.getParent());
			if (parent == null) {
				log.debug("Unresolved parent : {}", menuItem.getParent());
				this.setUnresolved(MetaActionMenu.class, menuItem.getParent(), menu);
			} else {
				menu.setParent(parent);
			}
		}

		if (!Strings.isNullOrEmpty(menuItem.getAction())) {
			MetaAction action = MetaAction.findByName(menuItem.getAction());
			if (action == null) {
				log.debug("Unresolved action: {}", menuItem.getAction());
				this.setUnresolved(MetaActionMenu.class, menuItem.getAction(), menu);
			} else {
				menu.setAction(action);
			}
		}

		menu = menu.save();

		for (MetaActionMenu pending : this.resolve(MetaActionMenu.class, menu.getName())) {
			log.debug("Resolved action menu : {}", pending.getName());
			pending.setParent(menu);
			pending.save();
		}
	}

	private static final File outputDir = FileUtils.getFile(System.getProperty("java.io.tmpdir"), "axelor", "generated");

	private void importDefault(Module module) {
		for (Class<?> klass: JPA.models()) {
			if (module.hasEntity(klass) && MetaView.all().filter("self.model = ?1", klass.getName()).count() == 0) {
				File out = FileUtils.getFile(outputDir, "views", klass.getSimpleName() + ".xml");
				String xml = createDefaultViews(module, klass);
				try {
					log.debug("Creating default views: {}", out);
					Files.createParentDirs(out);
					Files.write(xml, out, Charsets.UTF_8);
				} catch (IOException e) {
					log.error("Unable to create: {}", out);
				}
			}
		}
	}

	@SuppressWarnings("all")
	private String createDefaultViews(Module module, final Class<?> klass) {

		final FormView formView = new FormView();
		final GridView gridView = new GridView();

		String name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, klass.getSimpleName());
		String title = klass.getSimpleName();

		formView.setName(name + "-form");
		gridView.setName(name + "-grid");

		formView.setModel(klass.getName());
		gridView.setModel(klass.getName());

		formView.setTitle(title);
		gridView.setTitle(title);

		List<AbstractWidget> formItems = Lists.newArrayList();
		List<AbstractWidget> gridItems = Lists.newArrayList();

		Mapper mapper = Mapper.of(klass);
		List<String> fields = Lists.reverse(fieldNames(klass));

		for(String n : fields) {

			Property p = mapper.getProperty(n);

			if (p == null || p.isPrimary() || p.isVersion())
				continue;

			Field field = new Field();
			field.setName(p.getName());

			if (p.isCollection()) {
				field.setColSpan(4);
				field.setShowTitle(false);
			} else {
				gridItems.add(field);
			}
			formItems.add(field);
		}

		formView.setItems(formItems);
		gridView.setItems(gridItems);


		importView(formView, module, false);
		importView(gridView, module, false);

		return XMLViews.toXml(ImmutableList.of(gridView, formView), false);
	}

	// Fields names are not in ordered but some JVM implementation can.
	private List<String> fieldNames(Class<?> klass) {
		List<String> result = new ArrayList<String>();
		for(java.lang.reflect.Field field : klass.getDeclaredFields()) {
			if (!field.getName().matches("id|version|selected|created(By|On)|updated(By|On)")) {
				result.add(field.getName());
			}
		}
		if (klass.getSuperclass() != Object.class) {
			result.addAll(fieldNames(klass.getSuperclass()));
		}
		return Lists.reverse(result);
	}
}
