/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper GmbH licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing;

import com.carrotsearch.hppc.IntObjectMap;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.util.Parameters;

/**
 * A simple dijkstra tuned to perform one to many queries more efficient than Dijkstra. Old data
 * structures are cached between requests and potentially reused.
 * <p>
 *
 * @author Peter Karich
 */
public class RealDijkstraOneToMany extends Dijkstra {
	protected IntObjectMap<SPTEntry> nodesMap;

	public RealDijkstraOneToMany(Graph graph, Weighting weighting, TraversalMode tMode) {
		super(graph, weighting, tMode);
	}

	@Override
	protected void initCollections(int size) {
		super.initCollections(size);
		nodesMap = new GHIntObjectHashMap<SPTEntry>(size);
	}

	@Override
	public Path calcPath(int from, int to) {
		this.to = to;
		currEdge = nodesMap.get(to);

		if (currEdge == null) {
			return super.calcPath(from, to);
		}

		return extractPath();
	}

	@Override
	public String getName() {
		return Parameters.Algorithms.REAL_DIJKSTRA_ONE_TO_MANY;
	}

	@Override
	protected boolean isMaxVisitedNodesExceeded() {
		nodesMap.put(currEdge.adjNode, currEdge);
		return super.isMaxVisitedNodesExceeded();
	}
}
