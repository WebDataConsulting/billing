/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */
package com.sapienter.jbilling.server.pluggableTask.admin;

public class ParameterDescription {
	public enum Type { STR, INT, FLOAT, DATE, BOOLEAN }

    private final String name;
	private final boolean required;
	private final Type type;
	private boolean isPassword;
	
	
	public ParameterDescription(String name, boolean required, Type type, boolean isPassword) {
		super();
		this.name = name;
		this.required = required;
		this.type = type;
		this.isPassword = isPassword;
	}
	public ParameterDescription(String name, boolean required, Type type) {
		super();
		this.name = name;
		this.required = required;
		this.type = type;
	}
	
	public boolean getIsPassword() {
		return isPassword;
	}
	
	public String getName() {
		return name;
	}
	public boolean isRequired() {
		return required;
	}
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return "ParameterDescription [name=" + name + ", required=" + required
				+ ", type=" + type + "]";
	}
}
