/**
 * Copyright (C) 2018 NileDB, Inc.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License, version 3,
 *    as published by the Free Software Foundation.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package data;

import java.util.List;

public interface EntityKey {

	void eSetContainer(Entity eContainer);

	Entity eContainer();

	String getName();

	void setName(String value);

	List<EntityAttribute> getAttributes();

	boolean isUnique();

	void setUnique(boolean value);

	boolean isPrimaryKey();

	void setPrimaryKey(boolean value);

	String getDocumentation();

	void setDocumentation(String value);

}
