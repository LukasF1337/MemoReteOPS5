package ops5;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;

import ops5.workingmemory.data.*;
import ops5.workingmemory.node.Node;

/**
 * ordering of rules firing
 */
public enum Strategy {
	LEX, MEA;

	// using ComparisonChain of guava:
	// Order production rules firing first according to LEX or MEA. If there are
	// multiple possible continue on to next step. Each stop filters the productions
	// rules possible for next rule firing. This filtering happens only among rules
	// that have currently valid matches.

	public static Comparator<ReteEntityWrapper> makeComparator(Strategy strategy) {
		switch (strategy) {
		case LEX:
			// LEX:
			// 1. discard production rules instantiations that have fired once already at
			// any time and are still part of the conflict set.
			// 2. choose production rules that have been most recently instantiated first
			// according to time stamp of the youngest contributing Rete Entitys
			// 3. choose the more specific ones, depending on more beta nodes than others
			// 4. choose the one most specific according to the number of preceding alpha
			// nodes added to the number of preceding beta nodes.
			// 5. if there are still multiple, choose the first one and fire
			return new Comparator<ReteEntityWrapper>() {
				@Override
				public int compare(ReteEntityWrapper o1, ReteEntityWrapper o2) {
					return ComparisonChain.start() // step 1 is done by deleting entries in this list
							.compare(o1.reteEntity().creationTime(), o2.reteEntity().creationTime()) // 2
							.compare(o1.nodeTerm().nodeBetaSpecificityScore, o2.nodeTerm().nodeBetaSpecificityScore) // 3
							.compare(o1.nodeTerm().nodeBetaSpecificityScore + o1.nodeTerm().nodeAlphaSpecificityScore, // 4
									o2.nodeTerm().nodeBetaSpecificityScore + o2.nodeTerm().nodeAlphaSpecificityScore) //
							.compare(o1, o2) // 5 differentiate unequal Objects
							.result();
				}
			};
		case MEA:
			// MEA:
			// 1. discard production rules instantiations that have fired once already at
			// any time and are still part of the conflict set.
			// 2. production rules that have been most recently instantiated first according
			// to time stamp of the MOST LEFT contributing Alpha Node
			// 3. choose production rules that have been most recently instantiated first
			// according to time stamp of the youngest contributing Rete Entitys
			// 4. choose the more specific ones, depending on more beta nodes than others
			// 5. choose the one most specific according to the number of preceding alpha
			// nodes added to the number of preceding beta nodes.
			// 6. if there are still multiple, choose the first one and fire
			return new Comparator<ReteEntityWrapper>() {
				@Override
				public int compare(ReteEntityWrapper o1, ReteEntityWrapper o2) {
					return ComparisonChain.start() // step 1 is done by deleting entries in this list
							.compare(o1.reteEntity().creationTimeMostLeft(), o2.reteEntity().creationTimeMostLeft()) // 2
							.compare(o1.reteEntity().creationTime(), o2.reteEntity().creationTime()) // 3
							.compare(o1.nodeTerm().nodeBetaSpecificityScore, o2.nodeTerm().nodeBetaSpecificityScore) // 4
							.compare(o1.nodeTerm().nodeBetaSpecificityScore + o1.nodeTerm().nodeAlphaSpecificityScore, // 5
									o2.nodeTerm().nodeBetaSpecificityScore + o2.nodeTerm().nodeAlphaSpecificityScore) //
							.compare(o1, o2) // 6 differentiate unequal Objects
							.result();
				}
			};
		default:
			throw new IllegalStateException();
		}
	}
}
