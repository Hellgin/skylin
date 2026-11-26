package com.distantmelody.framework.skylin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * An AM's root views keep the order they were created in and are found by name, before and after the AM initializes.
 * Views created after that are not root views of the AM.
 */
class AMViewsTest
{
	static class Orders extends View
	{
		Orders(AM am)
		{
			super(am);
			setColumnNames("no");
			setColumnTypes(String.class);
		}
	}

	static class Customers extends Orders
	{
		Customers(AM am)
		{
			super(am);
		}
	}

	@Test
	void rootViewsKeepTheirOrderAndAreFoundByName()
	{
		AM am = new AM();
		Orders orders = new Orders(am);
		Customers customers = new Customers(am);
		assertSame(customers, am.getView("Customers"), "found before the AM initializes");
		am.setDataSource("none");

		assertEquals(List.of(orders, customers), am.views, "creation order, not sorted by name");
		assertSame(orders, am.getView("Orders"));
		assertSame(customers, am.getViewObject("Customers"));
		assertThrows(SkylinException.class, () -> am.getView("Missing"));

		boolean[] found = {false};
		assertSame(orders, am.handleVirtualMethodCall("Orders", new Object[0], found), "$am->Orders in PHP");
		assertTrue(found[0]);
		found[0] = false;
		assertNull(am.handleVirtualMethodCall("Orders", new Object[] {1}, found), "a call with arguments is not a view");
		assertFalse(found[0]);
		assertNull(am.handleVirtualMethodCall("Missing", new Object[0], found));
		assertFalse(found[0]);
	}

	@Test
	void nextNameNamesTheNextView()
	{
		AM am = new AM();
		am.nextName = "OpenOrders";
		Orders open = new Orders(am);
		Orders closed = new Orders(am);
		am.setDataSource("none");
		assertSame(open, am.getView("OpenOrders"));
		assertSame(closed, am.getView("Orders"));
	}

	@Test
	void aSecondViewWithTheSameNameStaysARootViewButIsNotFoundByName()
	{
		AM am = new AM();
		Orders first = new Orders(am);
		Orders second = new Orders(am);
		am.setDataSource("none");
		assertEquals(List.of(first, second), am.views);
		assertSame(first, am.getView("Orders"));
	}

	@Test
	void aViewCreatedAfterInitIsNotARootView()
	{
		AM am = new AM();
		Orders declared = new Orders(am);
		am.setDataSource("none");
		new Customers(am);
		new Orders(am);
		assertEquals(List.of(declared), am.views);
		assertSame(declared, am.getView("Orders"));
		assertThrows(SkylinException.class, () -> am.getView("Customers"));
	}
}
