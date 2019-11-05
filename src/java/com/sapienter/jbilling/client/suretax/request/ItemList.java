package com.sapienter.jbilling.client.suretax.request;

import java.util.ArrayList;
import java.util.List;

public class ItemList {
	private List<LineItem> itemList;

	public void addItem(LineItem item) {
		if (itemList == null) {
			itemList = new ArrayList<LineItem>();
		}
		itemList.add(item);
	}

	public List<LineItem> getItemList() {
		return itemList;
	}

	public void setItemList(List<LineItem> itemList) {
		this.itemList = itemList;
	}

}
