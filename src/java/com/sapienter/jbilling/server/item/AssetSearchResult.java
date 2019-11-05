package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.server.item.AssetWS;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Search results returned by a query taking SearchCriteria.
 * It contains the AssetWS objects returned by the query and the total.
 *
 * @author Gerhard Maree
 * @since 28/06/13
 */
public class AssetSearchResult implements Serializable  {
    /** the total number of objects */
    private int total;
    /** the objects returned by the query */
    private AssetWS[] objects;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public AssetWS[] getObjects() {
        return objects;
    }

    public void setObjects(AssetWS[] objects) {
        this.objects = objects;
    }

    @Override
    public String toString() {
        return "AssetSearchResult{" +
                "total=" + total +
                ", objects=" + (objects == null ? null : Arrays.asList(objects)) +
                '}';
    }
}
