package ops5.workingmemory.data.condition;

import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import ops5.workingmemory.data.Literal;

/**
 * 
 * @param exists           whether the condition tests for existence or
 *                         non-existence
 * @param literal          the class of facts that are tested for the condition
 * @param elementVariable  Either null or name of the ElementVariable
 * @param conditionFilters
 *
 * 
 */
public final record Condition(boolean exists, //
		Literal literal, //
		String elementVariable, //
		ImmutableList<ConditionFilter> conditionFilters //
) {
	public Condition {
		Objects.nonNull(exists);
		Objects.nonNull(literal);
		Objects.nonNull(conditionFilters);
		ImmutableList<ConditionFilter> conditionFiltersBuilder;
		if (elementVariable != null) {
			// for each Condition add a reference to elementVariable
			ImmutableList.Builder<ConditionFilter> tmpFiltersBuilder = new ImmutableList.Builder<>();
			for (ConditionFilter filter : conditionFilters) {
				ConditionFilter tmpFilter = new ConditionFilter(filter.attribute(), filter.predicate(), filter.value(),
						elementVariable);
				tmpFiltersBuilder.add(tmpFilter);
			}
			conditionFiltersBuilder = tmpFiltersBuilder.build();
		} else {
			conditionFiltersBuilder = conditionFilters;
		}
		conditionFilters = ImmutableList.copyOf(conditionFiltersBuilder);
	}

	public Condition(Optional<Boolean> exists, //
			Optional<Literal> literal, //
			Optional<String> elementVariable, //
			Optional<ImmutableList<ConditionFilter>> conditionFilters //
	) {
		this(exists.orElseThrow(), //
				literal.orElseThrow(), //
				elementVariable.orElse(null), //
				ImmutableList.copyOf(conditionFilters.orElseThrow()) //
		);
	}
}
