/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.auth;

import java.util.Set;

import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity.AccessType;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;

/**
 * This class is responsible to resolve permissions.
 * 
 */
final class AuthResolver {

	/**
	 * Check whether the given {@link Permission} confirms the requested access
	 * type.
	 * 
	 * @param permission
	 *            the permission instance to check
	 * @param accessType
	 *            the required access type
	 * @return true if can confirm false otherwise
	 */
	private boolean hasAccess(Permission permission, AccessType accessType) {
		switch(accessType) {
		case READ:
			return permission.getCanRead() == Boolean.TRUE;
		case WRITE:
			return permission.getCanWrite() == Boolean.TRUE;
		case CREATE:
			return permission.getCanCreate() == Boolean.TRUE;
		case REMOVE:
			return permission.getCanRemove() == Boolean.TRUE;
		case EXPORT:
			return permission.getCanExport() == Boolean.TRUE;
		default:
			return false;
		}
	}

	/**
	 * Filter the given set of permissions for the given object with the
	 * required access type. <br>
	 * <br>
	 * It first tried to find exact match for the given object else it tries to
	 * find wild card (by package name).
	 * 
	 * @param permissions
	 *            set of permissions to filter
	 * @param object
	 *            object name for which to check the permission
	 * @param type
	 *            the requested access type
	 * @return filtered set of {@link Permission}
	 */
	private Set<Permission> filterPermissions(final Set<Permission> permissions, final String object, final AccessType type) {
		
		if (permissions == null || permissions.isEmpty()) {
			return Sets.newLinkedHashSet();
		}

		final Set<Permission> all = Sets.newLinkedHashSet();
		
		for (final Permission permission : permissions) {
			if (Objects.equal(object, permission.getObject()) && (type == null || hasAccess(permission, type))) {
				all.add(permission);
			}
		}
		if (!all.isEmpty()) {
			return all;
		}
		
		final String pkg = object.substring(0, object.lastIndexOf('.')) + ".*";
		
		for (final Permission permission : permissions) {
			if (Objects.equal(pkg, permission.getObject()) && (type == null || hasAccess(permission, type))) {
				all.add(permission);
			}
		}
		
		return all;
	}
	
	/**
	 * Get the set of {@link Permission} for the given type on the object. <br>
	 * <br>
	 * The permission resolution is done in following way: <br>
	 * <br>
	 * Check the permissions directly assigned to the user, else check
	 * permissions assigned to the user's roles, else check the permissions
	 * assigned directly to the user group, else check the permissions assigned
	 * to the group's roles.
	 * 
	 * @param user
	 *            the user to authorize
	 * @param object
	 *            the object name (class or package name)
	 * @param type
	 *            access type to check
	 * @return {@link Set} of {@link Permission}
	 */
	public Set<Permission> resolve(final User user, final String object, final AccessType type) {
		
		// first find user permissions
		Set<Permission> all = filterPermissions(user.getPermissions(), object, type);

		// else user role permissions
		if (all.isEmpty() && user.getRoles() != null) {
			for (final Role role : user.getRoles()) {
				all.addAll(filterPermissions(role.getPermissions(), object, type));
			}
		}

		// else group permissions
		if (all.isEmpty() && user.getGroup() != null) {
			all.addAll(filterPermissions(user.getGroup().getPermissions(), object, type));
		}

		// else group role permissions
		if (all.isEmpty() && user.getGroup() != null && user.getGroup().getRoles() != null) {
			for (final Role role : user.getGroup().getRoles()) {
				all.addAll(filterPermissions(role.getPermissions(), object, type));
			}
		}
		
		return all;
	}
}
