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
package com.graphhopper.routing.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.Translation;
import com.graphhopper.util.shapes.GHPoint;

/**
 * Implementation of a route with no via points but multiple path lists ('alternatives').
 *
 * @author Peter Karich
 */
public class OneToManyRoutingTemplate extends ViaRoutingTemplate {
	public OneToManyRoutingTemplate(GHRequest ghRequest, GHResponse ghRsp, LocationIndex locationIndex) {
		super(ghRequest, ghRsp, locationIndex);
	}

	@Override
	public List<QueryResult> lookup(List<GHPoint> points, FlagEncoder encoder) {
		return super.lookup(points, encoder);
	}

	@Override
	public List<Path> calcPaths(QueryGraph queryGraph, RoutingAlgorithmFactory algoFactory, AlgorithmOptions algoOpts) {
		long visitedNodesSum = 0L;
		boolean viaTurnPenalty = ghRequest.getHints().getBool(Routing.PASS_THROUGH, false);
		int pointCounts = ghRequest.getPoints().size();
		pathList = new ArrayList<>(pointCounts - 1);
		QueryResult fromQResult = queryResults.get(0);
		StopWatch sw;
		RoutingAlgorithm algo = algoFactory.createAlgo(queryGraph, algoOpts);

		for (int placeIndex = 1; placeIndex < pointCounts; placeIndex++) {
			QueryResult toQResult = queryResults.get(placeIndex);

			sw = new StopWatch().start();
			String debug = ", algoInit:" + sw.stop().getSeconds() + "s";

			sw = new StopWatch().start();
			List<Path> tmpPathList = algo.calcPaths(fromQResult.getClosestNode(), toQResult.getClosestNode());
			debug += ", " + algo.getName() + "-routing:" + sw.stop().getSeconds() + "s";
			if (tmpPathList.isEmpty())
				throw new IllegalStateException("At least one path has to be returned for " + fromQResult + " -> " + toQResult);

			int idx = 0;
			for (Path path : tmpPathList) {
				if (path.getTime() < 0)
					throw new RuntimeException("Time was negative " + path.getTime() + " for index " + idx + ". Please report as bug and include:" + ghRequest);

				pathList.add(path);
				debug += ", " + path.getDebugInfo();
				idx++;
			}

			altResponse.addDebugInfo(debug);

			// reset all direction enforcements in queryGraph to avoid influencing next path
			queryGraph.clearUnfavoredStatus();

			if (algo.getVisitedNodes() >= algoOpts.getMaxVisitedNodes())
				throw new IllegalArgumentException("No path found due to maximum nodes exceeded " + algoOpts.getMaxVisitedNodes());

			visitedNodesSum += algo.getVisitedNodes();
		}

		ghResponse.getHints().put("visited_nodes.sum", visitedNodesSum);
		ghResponse.getHints().put("visited_nodes.average", (float) visitedNodesSum / (pointCounts - 1));

		return pathList;
	}

	@Override
	public boolean isReady(PathMerger pathMerger, Translation tr) {
		if (pathList.isEmpty())
			throw new RuntimeException("Empty paths for alternative route calculation not expected");

		// if alternative route calculation was done then create the responses from single paths        
		PointList wpList = getWaypoints();
		altResponse.setWaypoints(wpList);
		ghResponse.add(altResponse);
		pathMerger.doWork(altResponse, Collections.singletonList(pathList.get(0)), tr);
		for (int index = 1; index < pathList.size(); index++) {
			PathWrapper tmpAltRsp = new PathWrapper();
			tmpAltRsp.setWaypoints(wpList);
			ghResponse.add(tmpAltRsp);
			pathMerger.doWork(tmpAltRsp, Collections.singletonList(pathList.get(index)), tr);
		}
		return true;
	}
}
