package ops5.workingmemory.data;

import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ops5.workingmemory.node.NodeBeta.Position;

import com.google.common.base.Preconditions;

public final record BetaTuple(ImmutableMap<Position, ReteEntity> betaTuple) {

	public BetaTuple {
		// either Position is allowed to be null/unassigned, but there must be at least
		// one side assigned:
		assert (betaTuple.size() != 0);
	}

	public BetaTuple(EnumMap<Position, ReteEntity> betaTuple) {
		this(Maps.immutableEnumMap(betaTuple));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<Position, ReteEntity> e : this.betaTuple.entrySet()) {
			if (e.getKey() == Position.LEFTBETA) {
				builder.append("L=");
			} else if (e.getKey() == Position.RIGHTALPHA) {
				builder.append("R=");
			} else {
				throw new IllegalStateException();
			}
			builder.append("{" + e.getValue().toString() + "}");
		}
		return builder.toString();
	}
}
