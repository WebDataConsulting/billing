package in.webdata.ws

import java.math.BigDecimal;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;

import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import spock.lang.Specification
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.item.ItemPriceDTOEx
import com.sapienter.jbilling.server.item.ItemTypeWS
import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import java.lang.Integer
import java.lang.Object
public class WSTestItemSpec  extends Specification {


	def "testCreate"() {

		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		ItemDTOEx newItem = new ItemDTOEx();

		newItem.setDescription("an item from ws");

		newItem.setPrice(new BigDecimal("29.5"));

		Integer []types = new Integer[1];

		types[0] = new Integer(1);

		newItem.setTypes(types);

		newItem.setPriceManual(new Integer(0));

		System.out.println("Creating item ...");

		Integer ret = api.createItem(newItem);

		expect:

		null		!=		ret;
		System.out.println("Done!");
	}

	def "testPricingRules"() {


		setup:

		System.out.println("Testing Pricing Rules");

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// Tests item pricing for user "gandalf" (id 2)
		PricingField pf = new PricingField("newPrice", new BigDecimal("50.0"));

		def ar = new PricingField[1];

		ar[0]  = pf;

		ItemDTOEx it = api.getItem(new Integer(1), new Integer(2), ar);

		expect:

		new BigDecimal("50.0")		==	 it.getPrice();

		println("Pricing field test passed");


		// Tests access to an item of a different entity
		when:
		//try {
		// Try to get item 4 (should fail because the user is on entity 1 and
		// the item is on entity 2).
		println( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+it);

		it = api.getItem(new Integer(4), new Integer(2), ar);

		then:

		thrown(Exception)
		def i = 2;

		when:

		i != 2

		then:
		println("Security check failed, should not access Item from another entity");

		when:

		i == 2

		then:

		println("Security check passed for item retrieval");

		println("Done!");

	}

	def OrderWS "prepareOrder"() {



		OrderWS newOrder = new OrderWS();
		newOrder.setUserId(new Integer(2));
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(new Integer(1)); // once
		newOrder.setCurrencyId(new Integer(1));

		// now add some lines
		OrderLineWS []lines = new OrderLineWS[2];
		OrderLineWS line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		// this is an item line
		line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setItemId(new Integer(2));
		// take the description from the item
		line.setUseItem(new Boolean(true));
		lines[1] = line;

		newOrder.setOrderLines(lines);
		return newOrder;
	}

	def "testOrderRating"() {


		setup:

		println("Testing Order Rating");
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// Tests item pricing for user "gandalf" (id 2)
		PricingField add = new PricingField("add", new BigDecimal("10.0"));
		PricingField subtract = new PricingField("subtract", new BigDecimal("1.0"));

		System.out.println("Testing pricing fields on order rating 1");
		// rate an order, use "add" pricing field rule (adds 10 to price in all items of order)
		OrderWS newOrder = prepareOrder();

		def		pf	=	new	PricingField[1];

		pf[0]		=	add;

		newOrder.setPricingFields(PricingField.setPricingFieldsValue(pf));
		OrderWS order = api.rateOrder(newOrder);
		OrderLineWS[] l = order.getOrderLines();

		expect:

		null		!=		l;
		true		==		(l.length == 2);
		new BigDecimal("20.00")		==		 l[0].getPriceAsDecimal();
		new BigDecimal("40.00")		==		  l[1].getPriceAsDecimal();


		when:

		println("Testing pricing fields on order rating 2");

		// rate the same order, but using "subtract" (minus 1 to price in all items of order)

		newOrder = prepareOrder();

		def ar = new PricingField[1];

		ar[0]  = subtract;

		newOrder.setPricingFields(PricingField.setPricingFieldsValue(ar));

		order = api.rateOrder(newOrder);

		l = order.getOrderLines();

		then:

		null		!=		l;

		l.length		==		  2;

		new BigDecimal("9.00")		==		  l[0].getPriceAsDecimal();

		new BigDecimal("29.00")		==		  l[1].getPriceAsDecimal();


		when:

		println("Testing double rating with both orders in one shot");
		// rate an order, use "add" pricing field rule (adds 10 to price in all items of order)
		OrderWS newOrder1 = prepareOrder();

		newOrder1.setPricingFields(PricingField.setPricingFieldsValue(pf));

		OrderWS newOrder2 = prepareOrder();

		newOrder2.setPricingFields(PricingField.setPricingFieldsValue(ar));

		def ar2 = new OrderWS[2];

		ar2[0]  = newOrder1;

		ar2[1]  = newOrder2;

		OrderWS []orders = api.rateOrders(ar2);
		l = orders[0].getOrderLines();

		then:

		null		!=		l;
		true		==		(l.length == 2);
		new BigDecimal("20.00")		==		 l[0].getPriceAsDecimal();
		new BigDecimal("40.00")		==		 l[1].getPriceAsDecimal();

		when:

		l = orders[1].getOrderLines();

		then:

		null		!=		l;
		l.length		==		 2;
		new BigDecimal("9.00")		==		 l[0].getPriceAsDecimal();
		new BigDecimal("29.00")		==		 l[1].getPriceAsDecimal();

		System.out.println("Done!");


	}

	def "testGetAllItems"() {


		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		println("Getting all items");

		ItemDTOEx[] items =  api.getAllItems();

		Arrays.sort(items, new Comparator<ItemDTOEx>() {

					public int compare(ItemDTOEx i1, ItemDTOEx i2) {
						return i1.getId().compareTo(i2.getId());
					}
				});

		expect:


		null		!=		 items;

		64		==		 items.length;

		"Lemonade - 1 per day monthly pass"		==		 items[0].getDescription();

		new BigDecimal("10")		==		items[0].getPrice();

		new BigDecimal("10")		==		(getCurrencyPrice(items[0].getPrices(), 1).getPrice());

		new Integer(1)		==		items[0].getId();

		"DP-1"		==		items[0].getNumber();

		new Integer(1)		==		 items[0].getTypes()[0];

		"Lemonade - all you can drink monthly"		==		items[1].getDescription();
		new BigDecimal("20") 		==		items[1].getPrice();
		new BigDecimal("20")		==		(getCurrencyPrice(items[1].getPrices(), 1).getPrice());
		new Integer(2)		==		items[1].getId();
		"DP-2"		==		items[1].getNumber();
		new Integer(1)		==		items[1].getTypes()[0];

		"Coffee - one per day - Monthly"		==		items[2].getDescription();
		new BigDecimal("15")		==		items[2].getPrice();

		new BigDecimal("15")		==		(getCurrencyPrice(items[2].getPrices(), 1).getPrice());

		new Integer(3)		==		items[2].getId();
		"DP-3"		==		items[2].getNumber();
		new Integer(1)		==		items[2].getTypes()[0];

		"10% Elf discount."		==		 items[3].getDescription();
		new BigDecimal("-10.00") 		==		items[3].getPercentage();
		new Integer(14)		==		 items[3].getId();
		"J-01"		==		 items[3].getNumber();
		new Integer(12) 		==		items[3].getTypes()[0];

		"Cancel fee" 		==		items[4].getDescription();
		new BigDecimal("5") 		==		items[4].getPrice();
		new Integer(24) 		==		items[4].getId();
		"F-1" 		==		items[4].getNumber();
		new Integer(22) 		==		items[4].getTypes()[0];

		// item at index 5 tested in testCurrencyConvert() below

		// this is alwyas the last item
		int lastItem = items.length - 1;
		"an item from ws" 		==		items[lastItem].getDescription();
		new BigDecimal("29.5") 		==		items[lastItem].getPrice();
		new BigDecimal("29.5")		==		 (getCurrencyPrice(items[lastItem].getPrices(), 1).getPrice());
		new Integer(1) 		==		items[lastItem].getTypes()[0];

		System.out.println("Done!");

	}

	def "testUpdateItem"() {


		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		System.out.println("Getting item");

		def ar = new PricingField[0];

		when:
		ItemDTOEx item = api.getItem(new Integer(1), new Integer(2), ar);
		String description = item.getDescription();
		Integer prMan = item.getPriceManual();
		String number = item.getNumber();
		BigDecimal price = item.getPrice();
		BigDecimal perc = item.getPercentage();
		String promo = item.getPromoCode();

		println("Changing properties");
		item.setDescription("Another description");
		item.setPriceManual(new Integer(1));
		item.setNumber("NMR-01");
		item.setPrice(new BigDecimal("1.00"));

		println("Updating item");
		api.updateItem(item);

		ItemDTOEx itemChanged = api.getItem(new Integer(1), new Integer(2), ar );

		then:

		itemChanged.getDescription()		==		 "Another description";
		itemChanged.getPriceManual()		==		  new Integer(1);
		itemChanged.getNumber()		==		  "NMR-01";
		itemChanged.getPrice()		==		  price;
		itemChanged.getPercentage()		==		  perc;
		itemChanged.getPromoCode()		==		  promo;
		println("Done!");

		println("Restoring initial item state.");
		item.setDescription(description);
		item.setPriceManual(prMan);
		item.setNumber(number);
		api.updateItem(item);
		println("Done!");
	}

	def "testCurrencyConvert"() {

		setup:
		JbillingAPI api = JbillingAPIFactory.getAPI();

		def ar = new PricingField[0];

		ItemDTOEx item = api.getItem(new Integer(240), new Integer(2), ar );

		ItemPriceDTOEx priceUSD = getCurrencyPrice(item.getPrices(), 1);
		expect:
		1		==		 item.getCurrencyId().intValue();
		new BigDecimal("10.0")		==		 item.getPrice();
		2		==		  item.getPrices().size();


		priceUSD.getCurrencyId().intValue()		==		 1;

		priceUSD.getPrice()		==		 null;

		when:

		ItemPriceDTOEx priceAUD = getCurrencyPrice(item.getPrices(), 11);

		then:

		priceAUD.getCurrencyId().intValue()		==		 11;

		priceAUD.getPrice()		==		 new BigDecimal("15.0");
	}

	def ItemPriceDTOEx getCurrencyPrice(List prices, int currencyId) {
		Iterator iter = prices.iterator();
		while (iter.hasNext()) {
			ItemPriceDTOEx itemPrice = (ItemPriceDTOEx) iter.next();
			if (itemPrice.getCurrencyId().intValue() == currencyId) {
				return itemPrice;
			}
		}
		return null;
	}

	def "testGetAllItemCategories"()  {

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();

		ItemTypeWS[] types = api.getAllItemCategories();

		then:

		10		==		 types.length;

		12		==		 types[0].getId().intValue();
		"Drink passes"		== types[0].getDescription();
	}

	def "testCreateItemCategory"()  {

		setup:

		String description = "Ice creams (WS test)";

		System.out.println("Getting API...");
		JbillingAPI api = JbillingAPIFactory.getAPI();

		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription(description);
		itemType.setOrderLineTypeId(1);

		System.out.println("Creating item category '" + description + "'...");
		Integer itemTypeId = api.createItemCategory(itemType);

		expect:

		null	!=		itemTypeId;

		when:

		System.out.println("Done.");

		System.out.println("Getting all item categories...");
		ItemTypeWS[] types = api.getAllItemCategories();

		boolean addedFound = false;
		for(int i = 0; i < types.length; ++i) {
			if(description.equals(types[i].getDescription())) {
				System.out.println("Test category was found. Creation was completed successfully.");
				addedFound = true;
				break;
			}
		}
		then:

		true		==		 addedFound;
		System.out.println("Test completed!");
	}

	def		"testUpdateItemCategory"() {

		setup:

		Integer categoryId;
		String originalDescription;
		String description = "Drink passes (WS test)";

		println("Getting API...");
		JbillingAPI api = JbillingAPIFactory.getAPI();

		println("Getting all item categories...");
		ItemTypeWS[] types = api.getAllItemCategories();

		println("Changing description...");
		categoryId = types[0].getId();
		originalDescription = types[0].getDescription();
		types[0].setDescription(description);
		api.updateItemCategory(types[0]);

		println("Getting all item categories...");
		types = api.getAllItemCategories();
		println("Verifying description has changed...");
		for(int i = 0; i < types.length; ++i) {

			when:
			categoryId.equals(types[i].getId())		==		true

			then:
			description		==		types[i].getDescription();

			System.out.println("Restoring description...");
			types[i].setDescription(originalDescription);
			api.updateItemCategory(types[i]);
			break;
		}


		println("Test completed!");
	}

	def "testGetItemsByCategory"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		final Integer DRINK_ITEM_CATEGORY_ID = 2;

		ItemDTOEx[] items = api.getItemByCategory(DRINK_ITEM_CATEGORY_ID);

		then:

		1		==		 items.length;
		4		==		 items[0].getId().intValue();
		"Poison Ivy juice (cold)"		==		 items[0].getDescription();
	}

	public static void assertEquals(BigDecimal expected, BigDecimal actual) {
		assertEquals(null, expected, actual);
	}

	public static void assertEquals(String message, BigDecimal expected, BigDecimal actual) {
		assertEquals(message,
				(Object) (expected == null ? null : expected.setScale(2, RoundingMode.HALF_UP)),
				(Object) (actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP)));
	}
}
