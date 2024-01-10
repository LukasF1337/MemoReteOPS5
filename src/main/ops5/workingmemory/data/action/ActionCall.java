package ops5.workingmemory.data.action;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.checkerframework.common.returnsreceiver.qual.This;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;

import ops5.ModelRete;
import ops5.parser.StringHelpers;
import ops5.workingmemory.data.Fact;
import ops5.workingmemory.data.Literal;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.data.ValueType;
import ops5.workingmemory.node.NodeRoot;
import ops5.workingmemory.node.NodeTermination;

public final class ActionCall implements Action {

	final private Object uniqueNameObject;
	final private NextParameterType uniqueNameType;

	final private ImmutableList<Value<?>> parameterToValues;
	final private ImmutableList<String> parameterToAtomvars;
	final private ImmutableList<FunctionCompute> parameterToComputes;
	final private ImmutableList<ImmutableList<NextParameterType>> parameterToArrayOrder;
	final private ImmutableList<NextParameterType> parameterOrder;

	final private List<Constructor<?>> toBeCalledConstructor;
	final private List<Method> toBeCalledMethod;
	// keep Objects in objectStore (including ECar Objects), available to all
	// ActionCall instances. HashBasedTable<unique name, what class it is, concrete
	// Object instance stored>
	static final private HashBasedTable<String, String, Object> objectStore = HashBasedTable.create();
	// List of allowed classes for correctness and security.
	static final private ImmutableList<String> allowedClasses = ImmutableList.of( //
			"emo.ifs.ecar.ECar", //
			"ops5.workingmemory.data.action.MockClassActionCallTest" // needed for unit tests
	//
	);
	final String classPath; // for example: "emo.ifs.ecar.ECar"
	final private SubAction subAction; // action specified after "=>" or null if none is specified
	// for sub-actions, if needed:
	final private String literalName; // Literal to be created
	final private String elemVar; // Element variable
	final private ImmutableMap<String, Value<?>> attributeToValue; // ^city -> Washington
	final private ImmutableMap<String, String> attributeToAtomvar; // ^city -> <the_city>
	final private ImmutableList<String> attributeToReturn; // ^city; attribute gets assigned return value of toBeCalled
	final private ImmutableMap<String, FunctionCompute> attributeToCompute; // ^value -> (compute <val> + 1)

	private enum NextParameterType {
		VALUE, ATOMVARIABLE, COMPUTE, RETURN, ARRAY
	}

	private enum SubAction {
		MAKE, MODIFY, REMOVE;
	}

	private final static List<Class<?>> numberClasses = Arrays.asList( //
			Integer.class, //
			Long.class, //
			Double.class, //
			Float.class, //
			int.class, //
			long.class, //
			double.class, //
			float.class //
	);

	/**
	 * 
	 * @param str "(call uniqueNameObject procedureName ^parameterName1 parameter1
	 *            ^parameterName2 * arrayValue1 arrayValue2 ^parameterName3
	 *            parameter3 ... => null, ActionMake or ActionModify)"
	 *            uniqueNameObject and each parameter can be an atom variable to
	 *            enable dynamic programming.
	 */
	public ActionCall(String str, Map<String, Literal> literals, Set<String> atomVariables,
			HashMap<String, Literal> elemvarToLiteral) throws Exception {
		super();
		final ImmutableList.Builder<Value<?>> parameterToValuesBuilder = ImmutableList.builder();
		final ImmutableList.Builder<String> parameterToAtomvarsBuilder = ImmutableList.builder();
		final ImmutableList.Builder<FunctionCompute> parameterToComputesBuilder = ImmutableList.builder();
		final ImmutableList.Builder<ImmutableList<NextParameterType>> parameterToArrayOrderBuilder = ImmutableList
				.builder();
		final ImmutableList.Builder<NextParameterType> parameterOrderBuilder = ImmutableList.builder();

		String[] parts = str.split("\\=>");
		if (!(parts.length == 1 || parts.length == 2)) {
			// 1 part or 2 part where the second part is action make or modify
			throw new IllegalArgumentException("Cant parse:" + str + "\n" + parts.toString());
		}

		// parse the call part:
		final String[] parameters = parts[0].split("\\^");
		final String fullName = parameters[0].trim();
		final String[] fullNameParts = fullName.split(" ", 3);
		if (!(fullNameParts.length == 3 && fullNameParts[0].toLowerCase().equals("call"))) {
			throw new IllegalArgumentException(
					"Form a string like: (call uniqueObjectName procedureName ^parameterName1 parameter1 " + //
							"^parameterName2 parameter2 ... => ...)\n" + //
							"not like:\n" + fullName);
		}
		final String nameOfUnique = fullNameParts[1];
		if (nameOfUnique.charAt(0) == '<' && nameOfUnique.charAt(nameOfUnique.length() - 1) == '>') {
			uniqueNameObject = nameOfUnique;
			uniqueNameType = NextParameterType.ATOMVARIABLE;
		} else if (nameOfUnique.charAt(0) == '(' && nameOfUnique.charAt(nameOfUnique.length() - 1) == ')') {
			uniqueNameObject = new FunctionCompute(nameOfUnique, atomVariables);
			uniqueNameType = NextParameterType.COMPUTE;
		} else {
			uniqueNameObject = new Value.Builder().setValue(Optional.of(nameOfUnique)).build();
			uniqueNameType = NextParameterType.VALUE;
		}

		int ind = fullNameParts[2].lastIndexOf('.');
		classPath = fullNameParts[2].substring(0, ind);
		final String procedureName = fullNameParts[2].substring(ind + 1);

		boolean found = false;
		for (String classString : ActionCall.allowedClasses) {
			if (classString.equals(classPath)) {
				found = true;
				break;
			}
		}
		if (!found) {
			throw new IllegalArgumentException("\nclassPath " + classPath + " not part of the allowed Classes List.\n" //
					+ "If you want to access more classes modify ops5.workingmemory.data.action.allowedClasses.\n" + //
					"Currently allowed classes:\n" + ActionCall.allowedClasses.toString());
		}
		final Class<?> clazz = Class.forName(classPath);
		if (procedureName.equals("new")) {
			// constructor new was called
			final Constructor<?>[] constructors = clazz.getConstructors();
			toBeCalledConstructor = prefilterConstructors( //
					constructors, Arrays.copyOfRange(parameters, 1, parameters.length));
			toBeCalledMethod = null;
		} else {
			// a method was called
			Method[] methods = clazz.getMethods();
			toBeCalledMethod = prefilterMethods( //
					methods, Arrays.copyOfRange(parameters, 1, parameters.length), procedureName);
			toBeCalledConstructor = null;
		}

		// parse each parameter of the method/constructor toBeCalled and store it in
		// variables parameterTo*:
		for (String paramPackString : Arrays.copyOfRange(parameters, 1, parameters.length)) {
			String[] parameterStr = paramPackString.split(" ", 2);
			if (parameterStr.length != 2) {
				throw new IllegalArgumentException("Cant parse parameter: " + paramPackString);
			}
			// Parameter name comparison works only with special compiler options which can
			// not be expected to be used all the time, therefore skip it

			// classify parameter
			String valueString = parameterStr[1].trim();
			final NextParameterType currentType;
			if (valueString.startsWith("*")) {
				String[] valueStringParts = valueString.split(" ");
				final ImmutableList.Builder<NextParameterType> arrayParameterOrderBuilder = ImmutableList.builder();
				for (int indx = 1; indx < valueStringParts.length; indx++) {
					final String valueStrr = valueStringParts[indx].trim();
					if (valueStrr.charAt(0) == '<' && valueStrr.charAt(valueStrr.length() - 1) == '>') {
						parameterToAtomvarsBuilder.add(valueStrr);
						arrayParameterOrderBuilder.add(NextParameterType.ATOMVARIABLE);
					} else if (valueStrr.charAt(0) == '(' && valueStrr.charAt(valueStrr.length() - 1) == ')') {
						parameterToComputesBuilder.add(new FunctionCompute(valueStrr, atomVariables));
						arrayParameterOrderBuilder.add(NextParameterType.COMPUTE);
					} else {
						final Value<?> newValue = new Value.Builder().setValue(Optional.of(valueStrr)).build();
						parameterToValuesBuilder.add(newValue);
						arrayParameterOrderBuilder.add(NextParameterType.VALUE);
					}
				}
				parameterToArrayOrderBuilder.add(arrayParameterOrderBuilder.build());
				currentType = NextParameterType.ARRAY;
			} else if (valueString.charAt(0) == '<' && valueString.charAt(valueString.length() - 1) == '>') {
				parameterToAtomvarsBuilder.add(valueString);
				currentType = NextParameterType.ATOMVARIABLE;
			} else if (valueString.charAt(0) == '(' && valueString.charAt(valueString.length() - 1) == ')') {
				parameterToComputesBuilder.add(new FunctionCompute(valueString, atomVariables));
				currentType = NextParameterType.COMPUTE;
			} else {
				final Value<?> newValue = new Value.Builder().setValue(Optional.of(valueString)).build();
				parameterToValuesBuilder.add(newValue);
				currentType = NextParameterType.VALUE;
			}
			parameterOrderBuilder.add(currentType);
		}

		this.parameterToValues = parameterToValuesBuilder.build();
		this.parameterToAtomvars = parameterToAtomvarsBuilder.build();
		this.parameterToComputes = parameterToComputesBuilder.build();
		this.parameterToArrayOrder = parameterToArrayOrderBuilder.build();
		this.parameterOrder = parameterOrderBuilder.build();

		// ===========================================
		// parts[1] is the second part after "=>"
		if (parts.length == 2) {
			parts[1] = parts[1].trim();
			String[] attributes = parts[1].split("\\^");
			String[] prefix = attributes[0].split(" ");
			if (parts[1].toLowerCase().startsWith("make")) {
				// subaction make
				subAction = SubAction.MAKE;
				if (prefix.length == 2) { // if "make robot"
					literalName = prefix[1];
					if (literals.get(literalName) == null) {
						throw new Exception("Can't find literal \"" + literalName + "\"\n" + str);
					}
				} else {
					throw new Exception("Can't parse:\n" + prefix + "\nas part of of:\n" + str);
				}
				this.elemVar = null;
			} else if (parts[1].toLowerCase().startsWith("modify")) {
				// subaction modify
				subAction = SubAction.MODIFY;
				if (prefix.length == 2) { // if "modify <robot>"
					this.elemVar = prefix[1];
					if (!elemvarToLiteral.containsKey(elemVar)) {
						throw new IllegalArgumentException("elemVar undefined: " + elemVar
								+ "\nNote that reference by index is not allowed in this OPS5 implementation.");
					}
					this.literalName = elemvarToLiteral.get(this.elemVar).name();
					if (literals.get(this.literalName) == null) {
						throw new Exception("Can't find literal \"" + this.literalName + "\"\n" + str);
					}
				} else {
					throw new IllegalArgumentException("Can't parse:\n" + prefix + "\nas part of of:\n" + str);
				}
			} else if (parts[1].toLowerCase().startsWith("remove")) {
				// subaction remove
				subAction = SubAction.REMOVE;
				if (prefix.length == 2) { // if "remove <robot>"
					elemVar = prefix[1];
					if (!elemvarToLiteral.containsKey(elemVar)) {
						throw new IllegalArgumentException("elemVar undefined: " + elemVar
								+ "\nNote that reference by index is not allowed in this OPS5 implementation.");
					}
					this.literalName = elemvarToLiteral.get(elemVar).name();
					if (literals.get(this.literalName) == null) {
						throw new Exception("Can't find literal \"" + this.literalName + "\"\n" + str);
					}
				} else {
					throw new IllegalArgumentException("Can't parse:\n" + prefix + "\nas part of of:\n" + str);
				}
			} else {
				throw new IllegalStateException("Cant parse:\n" + parts[1]);
			}

			ImmutableMap.Builder<String, Value<?>> attributeToValueBuilder = new ImmutableMap.Builder<>();
			ImmutableMap.Builder<String, String> attributeToAtomvarBuilder = new ImmutableMap.Builder<>();
			ImmutableList.Builder<String> attributeToReturn = new ImmutableList.Builder<>();
			ImmutableMap.Builder<String, FunctionCompute> attributeToComputeBuilder = new ImmutableMap.Builder<>();
			// get attributes of fact:
			for (int i1 = 1; i1 < attributes.length; i1++) {
				if (!(this.subAction.equals(SubAction.MAKE) || this.subAction.equals(SubAction.MODIFY))) {
					throw new IllegalStateException();
				}
				String[] tmp = attributes[i1].split(" ", 2);
				String namee = tmp[0]; // could I check if this attribute name actually is part of the literal?
				String valueString = tmp[1].trim();
				if (valueString.equals("<?>")) {
					// use value returned by the function. Can be one value or Array of values.
					attributeToReturn.add(namee);
				} else if (valueString.charAt(0) == '<' && valueString.charAt(valueString.length() - 1) == '>') {
					attributeToAtomvarBuilder.put(namee, valueString);
				} else if (valueString.charAt(0) == '(' && valueString.charAt(valueString.length() - 1) == ')') {
					attributeToComputeBuilder.put(namee, new FunctionCompute(valueString, atomVariables));
				} else {
					Value<?> newValue = new Value.Builder().setValue(Optional.of(valueString)).build();
					attributeToValueBuilder.put(namee, newValue);
				}
			}
			this.attributeToValue = attributeToValueBuilder.build();
			this.attributeToAtomvar = attributeToAtomvarBuilder.build();
			this.attributeToReturn = attributeToReturn.build();
			this.attributeToCompute = attributeToComputeBuilder.build();
		} else {
			subAction = null;
			// init with null, no make, modify or remove declared by user program
			this.literalName = null;
			this.elemVar = null;
			this.attributeToValue = null;
			this.attributeToAtomvar = null;
			this.attributeToReturn = null;
			this.attributeToCompute = null;
		}
	}

	private Method getMatchingMethod(Object[] paramterValues) throws Exception {
		Method matchingMethod = null;
		for (Method method : this.toBeCalledMethod) {
			if (invokableMatches(paramterValues, method.getParameters())) {
				matchingMethod = method;
				break;
			}
		}
		if (matchingMethod == null) {
			throw new IllegalStateException("Method not found: " + paramterValues.toString() + "\nFrom available"
					+ this.toBeCalledMethod.toString());
		}
		return matchingMethod;
	}

	private Constructor<?> getMatchingConstructor(Object[] paramterValues) throws Exception {
		Constructor<?> matchingConstructor = null;
		for (Constructor<?> constr : this.toBeCalledConstructor) {
			if (invokableMatches(paramterValues, constr.getParameters())) {
				matchingConstructor = constr;
				break;
			}
		}
		if (matchingConstructor == null) {
			throw new IllegalStateException("Constructor not found: " + paramterValues.toString() + "\nFrom available"
					+ this.toBeCalledConstructor.toString());
		}
		return matchingConstructor;
	}

	/**
	 * test whether given parameters match given parameters of constructor or method
	 * for invocation and cast Primitive Types to Objects and vice versa if needed.
	 * 
	 */
	private boolean invokableMatches(Object[] paramterValues, java.lang.reflect.Parameter[] parameters) {
		boolean res = true;
		int paramIndex = -1;
		int parameterToArrayOrderIndex = -1;
		for (NextParameterType orderElement : this.parameterOrder) {
			paramIndex++;
			switch (orderElement) {
			case ARRAY:
				parameterToArrayOrderIndex++;
				ImmutableList<NextParameterType> arrayOrder = //
						this.parameterToArrayOrder.get(parameterToArrayOrderIndex);
				Object[] array = (Object[]) paramterValues[paramIndex];
				int arrayIndex = -1;
				for (NextParameterType arrayOrderElement : arrayOrder) {
					arrayIndex++;
					switch (arrayOrderElement) {
					case ATOMVARIABLE:
					case COMPUTE:
					case VALUE:
						if (parameters.length <= paramIndex) {
							res = false;
							break;
						}
						if (array[arrayIndex].getClass()
								.equals(castPrimitiveToClass(parameters[paramIndex].getType().componentType()))) {
							// same class compatible: String.class==String.class
						} else {
							res = false;
						}
						break;
					case ARRAY:
					case RETURN:
					default:
						throw new IllegalStateException();
					}
					if (res == false) {
						break;
					}
				}
				break;
			case ATOMVARIABLE:
			case COMPUTE:
			case VALUE:
				if (parameters.length <= paramIndex) {
					res = false;
					break;
				}
				if (paramterValues[paramIndex].getClass()
						.equals(castPrimitiveToClass(parameters[paramIndex].getType()))) {
					// same class compatible: String.class==String.class
				} else {
					res = false;
				}
				break;
			case RETURN:
			default:
				throw new IllegalStateException();
			}
			if (res == false) {
				break;
			}
		}
		if (parameters.length != paramIndex + 1) {
			// must have reached the end of parameter list, otherwise no match:
			res = false;
		}
		return res;
	}

	private static Class<?> castPrimitiveToClass(Class<?> clazz) {
		if (clazz.equals(int.class) || clazz.equals(long.class)) {
			return Integer.class;
		} else if (clazz.equals(float.class) || clazz.equals(double.class)) {
			return Double.class;
		}
		return clazz;
	}

	/**
	 * Prefiltering of methods that could match
	 * 
	 */
	private static ArrayList<Method> prefilterMethods(Method[] methods, String[] parameters, String methodName)
			throws Exception {
		ArrayList<Method> methodList = new ArrayList<>(Arrays.asList(methods));
		methodList.removeIf(method -> !method.getName().equals(methodName));
		ArrayList<Method> methodListTmp = new ArrayList<>();
		for (Method method : methodList) {
			if (!prefilterBasedOnParameters(method.getParameters(), parameters)) {
				methodListTmp.add(method);
			}
		}
		methodList = methodListTmp;
		if (methodList.size() == 0) {
			throw new IllegalArgumentException("Cant find matching constructor.\n" + methodName + "\n"
					+ methods.toString() + "\n" + parameters.toString());
		}
		return methodList;
	}

	/**
	 * Prefiltering of constructors that could match
	 * 
	 */
	private static ArrayList<Constructor<?>> prefilterConstructors(Constructor<?>[] constructors, String[] parameters)
			throws Exception {
		ArrayList<Constructor<?>> constructorsList = new ArrayList<>(Arrays.asList(constructors));
		ArrayList<Constructor<?>> constructorsListTmp = new ArrayList<>();
		for (Constructor<?> construc : constructorsList) {
			if (!prefilterBasedOnParameters(construc.getParameters(), parameters)) {
				constructorsListTmp.add(construc);
			}
		}
		constructorsList = constructorsListTmp;
		if (constructorsList.size() == 0) {
			throw new IllegalArgumentException("Cant find matching constructor.\n" + constructors + "\n" + parameters);
		}
		return constructorsList;
	}

	private static boolean prefilterBasedOnParameters(java.lang.reflect.Parameter[] parameters, String[] parametersStr)
			throws Exception {
		Boolean remove = false;
		int parameterIndex = 0;
		for (String parameterStr : parametersStr) {
			final int copyParameterIndex = parameterIndex;
			parameterIndex++;
			if (parameters.length <= copyParameterIndex) {
				remove = true;
				break;
			}
			String[] tmp = parameterStr.split(" ", 2);
			/// per parameter check for compatible type:
			String valueString = tmp[1].trim();
			if (valueString.startsWith("* ")) {
				// array "* <atomvar1> 5 135.2 robo3 <atomvar15> ..."
				remove |= // varargs are arrays and vice versa
						!(parameters[copyParameterIndex].getType().isArray());
				if (remove)
					break;
				// FIXME correct regex for skipping over |...|? \Q|\E
				String[] valueStringParts = valueString.split(" ");
				Class<?> arrayType = parameters[copyParameterIndex].getType().getComponentType();
				for (int index = 1; index < valueStringParts.length; index++) {
					// index start with 1, skips the "*"
					String curStr = valueStringParts[index];
					if (curStr.charAt(0) == '<' && curStr.charAt(curStr.length() - 1) == '>') {
						// NOOP now, later type test for each executeAction();
					} else if (curStr.charAt(0) == '(' && curStr.charAt(curStr.length() - 1) == ')') {
						// must be number:
						remove |= !numberClasses.contains(arrayType);
					} else {
						// must be same class:
						Value<?> newValue = new Value.Builder().setValue(Optional.of(curStr)).build();
						final List<Class<?>> classesAllowedForParameter = switch (newValue.attributeType()) {
						case INTEGER -> numberClasses;
						case DOUBLE -> numberClasses;
						case STRING -> Arrays.asList(String[].class, String.class);
						default -> throw new IllegalStateException();
						};
						remove |= !classesAllowedForParameter.contains(arrayType);
					}
				}
			} else if (valueString.charAt(0) == '<' && valueString.charAt(valueString.length() - 1) == '>') {
				// NOOP now, later type test for each executeAction();
			} else if (valueString.charAt(0) == '(' && valueString.charAt(valueString.length() - 1) == ')') {
				// must be number:
				remove |= !numberClasses.contains(parameters[copyParameterIndex].getType());
			} else {
				// must be same class:
				Value<?> newValue = new Value.Builder().setValue(Optional.of(valueString)).build();
				final List<Class<?>> classesAllowedForParameter = switch (newValue.attributeType()) {
				case INTEGER -> numberClasses;
				case DOUBLE -> numberClasses;
				case STRING -> Arrays.asList(String.class);
				default -> throw new IllegalStateException();
				};
				remove |= !classesAllowedForParameter.contains(parameters[copyParameterIndex].getType());
			}
			if (remove) {
				break;
			}
		}
		return remove;
	}

	@Override
	public Boolean executeAction(ReteEntity reteEntity, NodeTermination from, BigInteger time) throws Exception {
		// execute call part:
		final Value<?> resolvedUniqueNameObject;
		switch (this.uniqueNameType) {
		case ATOMVARIABLE:
			String atomVar = (String) this.uniqueNameObject;
			resolvedUniqueNameObject = reteEntity.getValue(atomVar);
			break;
		case COMPUTE:
			FunctionCompute comp = (FunctionCompute) this.uniqueNameObject;
			resolvedUniqueNameObject = comp.executeAndGetValue(reteEntity).orElse(null);
			break;
		case VALUE:
			resolvedUniqueNameObject = (Value<?>) this.uniqueNameObject;
			break;
		default:
			throw new IllegalStateException();
		}

		// retrieve callParameters
		ArrayList<Object> callParameters = new ArrayList<>();
		final EnumMap<NextParameterType, Integer> nextParamIndex = new EnumMap<>(NextParameterType.class);
		for (NextParameterType nextType : NextParameterType.values()) {
			nextParamIndex.put(nextType, -1);
		}
		for (NextParameterType orderElem : this.parameterOrder) {
			final Object callParameter;
			nextParamIndex.put(orderElem, nextParamIndex.get(orderElem) + 1);
			switch (orderElem) {
			case ATOMVARIABLE:
				String atomVar = this.parameterToAtomvars.get(nextParamIndex.get(orderElem));
				callParameter = reteEntity.getValue(atomVar).attributeValue();
				callParameters.add(callParameter);
				break;
			case COMPUTE:
				FunctionCompute comp = this.parameterToComputes.get(nextParamIndex.get(orderElem));
				callParameter = comp.executeAndGetValue(reteEntity).orElse(null).attributeValue();
				callParameters.add(callParameter);
				break;
			case VALUE:
				callParameter = this.parameterToValues.get(nextParamIndex.get(orderElem)).attributeValue();
				callParameters.add(callParameter);
				break;
			case ARRAY:
				ImmutableList<NextParameterType> arrayOrder = this.parameterToArrayOrder
						.get(nextParamIndex.get(orderElem));
				Object[] arrayElements = new Object[] {};
				int arrayIdx = -1;
				for (NextParameterType nex : arrayOrder) {
					arrayIdx++;
					nextParamIndex.put(nex, nextParamIndex.get(nex) + 1);
					Value<?> value;
					switch (nex) {
					case ATOMVARIABLE:
						String atomVarrr = this.parameterToAtomvars.get(nextParamIndex.get(nex));
						value = reteEntity.getValue(atomVarrr);
						break;
					case COMPUTE:
						FunctionCompute comput = this.parameterToComputes.get(nextParamIndex.get(nex));
						value = comput.executeAndGetValue(reteEntity).orElse(null);
						break;
					case VALUE:
						value = this.parameterToValues.get(nextParamIndex.get(nex));
						break;
					case ARRAY:
					case RETURN:
					default:
						throw new IllegalStateException();
					}
					if (arrayElements.getClass().componentType().equals(Object.class)) {
						// define type and size of whole array:
						switch (value.attributeType()) {
						case DOUBLE:
							arrayElements = new Double[arrayOrder.size()];
							break;
						case INTEGER:
							arrayElements = new Integer[arrayOrder.size()];
							break;
						case STRING:
							arrayElements = new String[arrayOrder.size()];
							break;
						case NIL:
						case ATOMVARIABLE:
						default:
							throw new IllegalArgumentException(value.toString());
						}
					}
					if (arrayElements.getClass().componentType().equals(Double.class)) {
						if (value.attributeValue() instanceof Double doub) {
							((Double[]) arrayElements)[arrayIdx] = doub;
						} else {
							throw new IllegalArgumentException("Array must be same type of each element" + //
									arrayElements.toString() + "\nElement: " + value.attributeValue());
						}
					} else if (arrayElements.getClass().componentType().equals(Integer.class)) {
						if (value.attributeValue() instanceof Integer integ) {
							((Integer[]) arrayElements)[arrayIdx] = integ;
						} else {
							throw new IllegalArgumentException("Array must be same type of each element" + //
									arrayElements.toString() + "\nElement: " + value.attributeValue());
						}
					} else if (arrayElements.getClass().componentType().equals(String.class)) {
						if (value.attributeValue() instanceof String stri) {
							((String[]) arrayElements)[arrayIdx] = stri;
						} else {
							throw new IllegalArgumentException("Array must be same type of each element" + //
									arrayElements.toString() + "\nElement: " + value.attributeValue());
						}
					}
				}
//				if (arrayElements == null) {
//					callParameter = (new ArrayList<Object>()).toArray();
//				} else if (arrayElements.getClass().componentType().equals(Double.class)) {
//					callParameter = ((ArrayList<Double>) arrayElements).toArray();
//				} else if (arrayElements.getClass().componentType().equals(Integer.class)) {
//					callParameter = ((ArrayList<Integer>) arrayElements).toArray();
//				} else if (arrayElements.getClass().componentType().equals(String.class)) {
//					callParameter = ((ArrayList<String>) arrayElements).toArray();
//				} else {
//					throw new IllegalStateException();
//				}
				assert (arrayElements.length == arrayIdx + 1);
				callParameter = arrayElements;
				callParameters.add(callParameter);
				break;
			case RETURN:
			default:
				throw new IllegalStateException();
			}
		}
		// invoke call:
		Object[] callParametersArray = callParameters.toArray();
		Object returned;
		if (this.toBeCalledConstructor != null) {
			assert (this.toBeCalledMethod == null);
			final Constructor<?> constructor = this.getMatchingConstructor(callParametersArray);
			returned = constructor.newInstance(callParametersArray);
			ActionCall.objectStore.put(resolvedUniqueNameObject.toString(), classPath, returned);
		} else if (this.toBeCalledMethod != null) {
			assert (this.toBeCalledConstructor == null);
			final Method methodd = this.getMatchingMethod(callParametersArray);
			final Object instance = ActionCall.objectStore.get(resolvedUniqueNameObject.toString(), classPath);
			returned = methodd.invoke(instance, callParametersArray);
			// returned can be Object or primitive Array.
		} else {
			throw new IllegalStateException();
		}
		if (this.subAction == null) {
			// no action, only the call
			// NOOP
		} else {
			ModelRete modelRete = from.modelRete.get();
			if (modelRete == null) {
				throw new IllegalStateException("Outrageous");
			}
			NodeRoot nodeRoot = modelRete.getRootNode(literalName);
			if (this.subAction.equals(SubAction.MAKE) || this.subAction.equals(SubAction.MODIFY)) {
				ImmutableMap.Builder<String, Value<?>> builder = ImmutableMap.builder();
				if (this.subAction.equals(SubAction.MODIFY)) {
					Fact fact = reteEntity.getElemvarFact(elemVar);
					builder.putAll(fact.attributeValues());
				}
				// MAKE or "make" part of MODIFY
				// for each <?> returned which is potentially an array:
				for (String attribute : this.attributeToReturn) {
					// convert primitive Types to Objects
					if (returned instanceof double[] d) {
						Double[] newDoubleArray = new Double[d.length];
						int itr = 0;
						for (double val : d) {
							newDoubleArray[itr] = val;
							itr++;
						}
						returned = newDoubleArray;
					} else if (returned instanceof float[] d) {
						Double[] newDoubleArray = new Double[d.length];
						int itr = 0;
						for (float val : d) {
							newDoubleArray[itr] = (double) val;
							itr++;
						}
						returned = newDoubleArray;
					} else if (returned instanceof int[] in) {
						Integer[] newIntegerArray = new Integer[in.length];
						int itr = 0;
						for (int val : in) {
							newIntegerArray[itr] = val;
							itr++;
						}
						returned = newIntegerArray;
					} else if (returned instanceof long[] in) {
						Integer[] newIntegerArray = new Integer[in.length];
						int itr = 0;
						for (long val : in) {
							newIntegerArray[itr] = (int) val;
							itr++;
						}
						returned = newIntegerArray;
					}
					// build fact from returned values:
					switch (returned) {
					case Object[] l:
						Literal lit = nodeRoot.literal;
						Iterator<String> attributeIterator = lit.attributeNames().iterator();
						String currentAttribute = null;
						while (attributeIterator.hasNext()) {
							currentAttribute = attributeIterator.next();
							if (currentAttribute.equals(attribute)) {
								break;
							}
						}
						for (Object listelem : l) {
							switch (listelem) {
							case String str:
								builder.put(currentAttribute, new Value(ValueType.STRING, listelem));
								break;
							case Double doub:
								builder.put(currentAttribute, new Value(ValueType.DOUBLE, listelem));
								break;
							case Integer integ:
								builder.put(currentAttribute, new Value(ValueType.INTEGER, listelem));
								break;
							default:
								throw new IllegalStateException();
							}
							if (attributeIterator.hasNext()) {
								currentAttribute = attributeIterator.next();
							} else {
								break;
							}
						}
						break;
					case String str:
						builder.put(attribute, new Value(ValueType.STRING, returned));
						break;
					case Double doub:
						builder.put(attribute, new Value(ValueType.DOUBLE, returned));
						break;
					case Integer integ:
						builder.put(attribute, new Value(ValueType.INTEGER, returned));
						break;
					default:
						throw new IllegalStateException(
								"Return value of type: " + returned.getClass() + " not supported");
					}
				}
				builder.putAll(attributeToValue);
				for (ImmutableMap.Entry<String, String> e : attributeToAtomvar.entrySet()) {
					String attribute = e.getKey();
					Value<?> value = reteEntity.getValue(e.getValue());
					builder.put(attribute, value);
				}
				for (ImmutableMap.Entry<String, FunctionCompute> e : attributeToCompute.entrySet()) {
					String attribute = e.getKey();
					FunctionCompute comp = e.getValue();
					Value<?> value = comp.executeAndGetValue(reteEntity).orElse(null);
					if (value != null) {
						builder.put(attribute, value);
					}
				}

				Fact newFact = new Fact(builder.buildKeepingLast());
				nodeRoot.addFact(newFact, time); // add new or modified fact
			}
			if (this.subAction.equals(SubAction.REMOVE) || this.subAction.equals(SubAction.MODIFY)) {
				// REMOVE or "remove" part of MODIFY
				Fact fact = reteEntity.getElemvarFact(elemVar);
				nodeRoot.removeFact(fact); // remove old fact
			}
		}

		return true; // no halt called, return true;
	}

	@Override
	public String getElemVar() {
		return this.elemVar;
	}
}
