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

package org.apache.pig.piggybank.squeal.flexy.model;

import java.io.Serializable;
import java.util.Set;

import org.apache.pig.piggybank.squeal.flexy.FlexyTopology;

import storm.trident.operation.CombinerAggregator;
import storm.trident.operation.Function;
import storm.trident.state.StateFactory;
import storm.trident.util.IndexedEdge;
import storm.trident.util.TridentUtils;
import backtype.storm.topology.IRichSpout;
import backtype.storm.tuple.Fields;

public class FStream implements Serializable {
	
	private String name;
	private transient FlexyTopology parent;
	private NodeType nodeType;
	private IRichSpout spout;
	private int parallelismHint;
	private Fields output_fields;
	private Fields input_fields;
	private Function func;
	private boolean isStage0Agg = false;
	private Fields group_key;
	private CombinerAggregator stage0Agg;
	private CombinerAggregator stage1Agg;
	private CombinerAggregator storeAgg;
	private StateFactory sf;
	private Fields func_project;
	
	public enum NodeType {
		SPOUT, FUNCTION, PROJECTION, SHUFFLE, GROUPBY, MERGE
	}

	public FStream(String name, FlexyTopology parent, NodeType t) {
		this.nodeType = t;

		this.name = name;
		this.parent = parent;
	}

	public FStream(String name, FlexyTopology parent,
			IRichSpout spout) {
		this(name, parent, NodeType.SPOUT);
		this.setSpout(spout);
	}

	public FStream name(String name) {
		this.name = name;
		return this;
	}

	public void parallelismHint(int parallelismHint) {
		this.parallelismHint = parallelismHint;
	}

	public FStream each(Fields input, Function func,
			Fields output) {
		// Create a function node.
		FStream n = new FStream(null, parent, NodeType.FUNCTION);
		
		// Set the function information.
		n.setFunctionInformation(getOutputFields(), input, func, output);
		
		// Link the nodes.
		parent.link(this, n);
		
		return n;
	}

	public FStream project(Fields output) {
		// Create a project node.
		FStream n = new FStream(null, parent, NodeType.PROJECTION);

		// Set the function information.
		n.setProjection(getOutputFields(), output);

		// Link the nodes.
		parent.link(this, n);

		return n;
	}

	public FStream shuffle() {
		// Create a shuffle node.
		FStream n = new FStream(null, parent, NodeType.SHUFFLE);
		
		// Link the nodes.
		parent.link(this, n);

		return n;
	}

	public FStream groupBy(Fields group_key,
			Fields input,
			CombinerAggregator stage1Agg, 
			CombinerAggregator stage2Agg, 
			CombinerAggregator storeAgg, 
			StateFactory sf, 
			Fields output_fields) {
		
		// Create a groupby node.
		FStream n = new FStream(null, parent, NodeType.GROUPBY);

		n.setGroupBySpec(group_key, input, stage1Agg, stage2Agg, storeAgg, sf, output_fields);
		
		// Link the nodes.
		parent.link(this, n);

		return n;
	}
	
	public Fields getAppendOutputFields() {
		switch (nodeType) {
		case FUNCTION:
			return output_fields;
		case GROUPBY:
			if (nodeType == NodeType.GROUPBY && isStage0Agg) {
				return new Fields("stage1_vl");
			}
			return output_fields;
		case MERGE:
		case SHUFFLE:
			// Pull a predecessor.
			return ((FStream[]) 
					parent.getIncomingEdgesOf(this).toArray())[0]
							.getGroupingFields();
		case PROJECTION:
			return output_fields;
		case SPOUT:
			return getOutputFields();
		default:
			throw new RuntimeException("Unknown node type:" + nodeType);
		}
	}
	
	public Fields getOutputFields() {
		switch (nodeType) {
		case FUNCTION:
			return TridentUtils.fieldsConcat(input_fields, output_fields);
		case GROUPBY:
			return TridentUtils.fieldsConcat(group_key, getAppendOutputFields());
		case MERGE:
		case SHUFFLE:
			// Pull a predecessor.
			FStream pred = ((IndexedEdge<FStream>) parent.getIncomingEdgesOf(this).toArray()[0]).source;
			return pred.getOutputFields();
		case PROJECTION:
			return output_fields;
		case SPOUT:
			return TridentUtils.getSingleOutputStreamFields(spout);
		default:
			throw new RuntimeException("Unknown node type:" + nodeType);
		}
	}
	
	public Fields getInputFields() {
		if (nodeType == NodeType.GROUPBY && !isStage0Agg) {
			return new Fields("stage1_vl");
		}
		return input_fields;
	}
	
	private void setSpout(IRichSpout spout) {
		this.spout = spout;
	}

	private void setFunctionInformation(Fields input, Fields project, 
			Function func, Fields output) {
		input_fields = input;
		func_project = project;
		this.func = func;
		output_fields = output;
	}

	private void setProjection(Fields input, Fields output) {
		this.input_fields = input;
		this.output_fields = output;
		
		// XXX: Unnecessary due to projection view code.
		// Ensure that the fields in output are present in the input.
//		for (String field : output.toList()) {
//			if (!input.contains(field)) {
//				throw new RuntimeException("Missing field [" + field + 
//						"] in projecting " + input + " to " + output);
//			}
//		}
	}

	private void setGroupBySpec(
			Fields group_key, 
			Fields input,
			CombinerAggregator stage1Agg,
			CombinerAggregator stage2Agg, CombinerAggregator storeAgg, 
			StateFactory sf, Fields output_fields) {
		this.group_key = group_key;
		this.stage0Agg = stage1Agg;
		this.stage1Agg = stage2Agg;
		this.storeAgg = storeAgg;
		this.sf = sf;
		this.input_fields = input;
		this.output_fields = output_fields;
	}

	public CombinerAggregator getStage0Agg() {
		return stage0Agg;
	}
	
	public CombinerAggregator getStage1Agg() {
		return stage1Agg;
	}
	
	public CombinerAggregator getStorageAgg() {
		return storeAgg;
	}
	
	public StateFactory getStateFactory() {
		return sf;
	}
	
	public NodeType getType() {
		return nodeType;
	}

	public FStream copy() {
		FStream n = new FStream(null, parent, nodeType);
		
		switch (nodeType) {
		case FUNCTION:
			n.setFunctionInformation(input_fields, func_project, func, output_fields);
			break;
		case GROUPBY:
			n.setGroupBySpec(group_key, input_fields, stage0Agg, stage1Agg, storeAgg, sf, output_fields);
			n.setStage0Agg(isStage0Agg);
			break;
		case MERGE:
		case SHUFFLE:
			// This is really a placeholder node...
			break;
		case PROJECTION:
			n.setProjection(input_fields, output_fields);
			break;
		case SPOUT:
			n.setSpout(spout);
			break;
		default:
			throw new RuntimeException("Unknown node type:" + nodeType);
		}
		
		n.parallelismHint(parallelismHint);
		n.name(name);
		
		return n;
	}

	public void setStage0Agg(boolean b) {
		isStage0Agg  = b;
	}
	
	public boolean getIsStage0Agg() {
		return isStage0Agg;
	}

	public Fields getGroupingFields() {
		return this.group_key;
	}

	public String getName() {
		return name;
	}

	public int getParallelism() {
		return parallelismHint;
	}
	
	public String toString() {
		return this.getClass().getCanonicalName() + "@" + Integer.toHexString(hashCode()) + " " + nodeType + " : " + getName();
	}

	public Function getFunc() {
		return func;
	}

	public IRichSpout getSpout() {
		return spout;
	}


}
