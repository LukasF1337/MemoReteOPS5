package ops5.workingmemory.data.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockClassActionCallTest {

	public MockClassActionCallTest() {
		System.out.print("MockClassActionCall()");
	}

	public MockClassActionCallTest(String str) {
		assert (str.equals("test"));
	}

	public MockClassActionCallTest(Integer integr) {
		assert (integr.equals(5));
	}

	public MockClassActionCallTest(Double doublenum) {
		assert (doublenum.equals(7.0));
	}

	public MockClassActionCallTest(String[] list) {
		String[] list2 = new String[] { "test1", "test2" };
		assert (Arrays.equals(list, list2));
	}

	public MockClassActionCallTest(Integer[] list) {
		assert (Arrays.equals(list, new Integer[] { 1, 2 }));
	}

	public MockClassActionCallTest(Integer[] list, Integer integr) {
		assert (Arrays.equals(list, new Integer[] { 1, 2 }) && integr == 7);
	}
	
	public MockClassActionCallTest(Integer[] list, Double doubl) {
		assert (Arrays.equals(list, new Integer[] { 1, 2 }) && doubl == 8.0);
	}

	public MockClassActionCallTest(Double[] list) {
		assert (Arrays.equals(list, new Double[] { 1.0, 2.0 }));
	}

	public String test() {
		return "success";
	}

	public String test2() {
		return "success2";
	}

	public String test(String[] list) {
		assert (Arrays.equals(list, new String[] { "test1", "test2" }));
		return "success";
	}

	public String test(Integer[] list) {
		assert (Arrays.equals(list, new Integer[] { 1, 2 }));
		return "success";
	}

	public String[] test2(String[] list) {
		assert (Arrays.equals(list, new String[] { "test1", "test2" }));
		return list;
	}

}
