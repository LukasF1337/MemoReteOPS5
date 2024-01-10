package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpecialBiMultiMapTest {
	private SpecialBiMultiMap<Integer, Integer> bimultimap;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		bimultimap = new SpecialBiMultiMap<>();
		bimultimap.put(1, 2);
		bimultimap.put(2, 3);
		bimultimap.put(3, 4);
		bimultimap.put(2, 2);
		bimultimap.put(4, 3);
		bimultimap.put(1, 1);
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void getTest_00() {
		Set<Integer> res;
		res = bimultimap.getValues(1);
		assertEquals(res, new HashSet<>(Arrays.asList(1, 2)));
		res = bimultimap.getValues(2);
		assertEquals(res, new HashSet<>(Arrays.asList(2, 3)));
		res = bimultimap.getValues(3);
		assertEquals(res, new HashSet<>(Arrays.asList(4)));
		res = bimultimap.getValues(4);
		assertEquals(res, new HashSet<>(Arrays.asList(3)));

		res = bimultimap.getKeys(1);
		assertEquals(res, new HashSet<>(Arrays.asList(1)));
		res = bimultimap.getKeys(2);
		assertEquals(res, new HashSet<>(Arrays.asList(1, 2)));
		res = bimultimap.getKeys(3);
		assertEquals(res, new HashSet<>(Arrays.asList(2, 4)));
		res = bimultimap.getKeys(4);
		assertEquals(res, new HashSet<>(Arrays.asList(3)));
	}

	@Test
	void removeKeyTest_00() {
		Collection<Integer> res;
		res = bimultimap.removeKey(1);
		assertEquals(res, new HashSet<>(Arrays.asList(1)));
	}

	@Test
	void removeKeyTest_01() {
		Collection<Integer> res;
		res = bimultimap.removeKey(2);
		assertEquals(res, new HashSet<>(Arrays.asList()));
	}

	@Test
	void removeKeyTest_02() {
		Collection<Integer> res;
		res = bimultimap.removeKey(3);
		assertEquals(res, new HashSet<>(Arrays.asList(4)));
	}

	@Test
	void removeKeyTest_03() {
		Collection<Integer> res;
		res = bimultimap.removeKey(4);
		assertEquals(res, new HashSet<>(Arrays.asList()));
	}

	@Test
	void removeValueTest_00() {
		Collection<Integer> res;
		res = bimultimap.removeValue(1);
		assertEquals(res, new HashSet<>(Arrays.asList()));
	}

	@Test
	void removeValueTest_01() {
		Collection<Integer> res;
		res = bimultimap.removeValue(2);
		assertEquals(res, new HashSet<>(Arrays.asList()));
	}

	@Test
	void removeValueTest_02() {
		Collection<Integer> res;
		res = bimultimap.removeValue(3);
		assertEquals(res, new HashSet<>(Arrays.asList(4)));
	}

	@Test
	void removeValueTest_03() {
		Collection<Integer> res;
		res = bimultimap.removeValue(4);
		assertEquals(res, new HashSet<>(Arrays.asList(3)));
	}
}
