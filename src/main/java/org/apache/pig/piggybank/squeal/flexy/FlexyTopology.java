/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pig.piggybank.squeal.flexy;

import java.util.List;

import org.apache.pig.piggybank.squeal.backend.storm.io.ImprovedRichSpoutBatchExecutor;
import org.apache.pig.piggybank.squeal.flexy.model.FStream;
import org.jgrapht.graph.DefaultDirectedGraph;

import storm.trident.util.ErrorEdgeFactory;
import storm.trident.util.IndexedEdge;
import backtype.storm.generated.StormTopology;

public class FlexyTopology {
	DefaultDirectedGraph<FStream, IndexedEdge<FStream>> _graph;
	int index_counter = 0;
	
	public FlexyTopology() {
		_graph = new DefaultDirectedGraph<FStream, IndexedEdge<FStream>>(new ErrorEdgeFactory());
	}

	public StormTopology build() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public FStream merge(List<FStream> intermed) {
		// TODO Auto-generated method stub
		return null;
	}

	public FStream newStream(String name,
			ImprovedRichSpoutBatchExecutor improvedRichSpoutBatchExecutor) {
		
		// Create a new node.
		FStream n = new FStream(name, this, FStream.NodeType.SPOUT);
		n.setSpout(improvedRichSpoutBatchExecutor);
		
		_graph.addVertex(n);
		
		return n;
	}

	public void link(FStream node, FStream n) {
		
		
	}
}