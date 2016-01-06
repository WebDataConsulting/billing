
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

package com.sapienter.jbilling.server.metafields.db.value;


import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import org.hibernate.annotations.CollectionOfElements;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("list")
public class ListMetaFieldValue extends MetaFieldValue<List<String>> {

    private List<String> value = new ArrayList<String>();

    public ListMetaFieldValue () {
    }

    public ListMetaFieldValue (MetaField name) {
        super(name);
    }

    @CollectionOfElements(targetElement = String.class)
    @JoinTable(name = "list_meta_field_values",
               joinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Column(name = "list_value")
    public List<String> getValue () {
        return value;
    }

    public void setValue (List<String> values) {
        this.value = values;
    }

    @Override
    @Transient
    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }
}