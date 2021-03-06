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

package org.apache.pig.piggybank.squeal.backend.storm.plans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators.POFRJoin;
import org.apache.pig.impl.io.FileSpec;
import org.apache.pig.impl.plan.DependencyOrderWalker;
import org.apache.pig.impl.plan.VisitorException;

/**
 * The purpose of this class is to find elements of a MapReduce plan that
 * contribute to replicated joins.  For our purposes, they will be rooted
 * in regular load functions and terminate in stores that are FRJoin files.
 * These chains need to be removed and executed before the Storm job.
 * 
 * @author jhl1
 *
 */
public class ReplJoinFileFixer extends SOpPlanVisitor {

	private SOperPlan plan;
	Map<FileSpec, FileSpec> rFileMap = new HashMap<FileSpec, FileSpec>();
	
	public ReplJoinFileFixer(SOperPlan plan) {
		super(plan, new DependencyOrderWalker<StormOper, SOperPlan>(plan));
		this.plan = plan;
		this.rFileMap = plan.getReplFileMap();
	}
		
	class FRJoinFileReplacer extends PhyPlanVisitor {
		
		public FRJoinFileReplacer(PhysicalPlan plan) {
			super(plan, new DependencyOrderWalker<PhysicalOperator, PhysicalPlan>(plan));
		}
		
	    @Override
	    public void visitFRJoin(POFRJoin join) throws VisitorException {
	    	List<FileSpec> newrepl = new ArrayList<FileSpec>();
	    	
	    	// Extract the files.
	    	for (FileSpec f : join.getReplFiles()) {
	    		if (f == null) {
	    			newrepl.add(f);
	    			continue;
	    		}
	    		
	    		// Use the rFileMap to swap things.
	    		newrepl.add(rFileMap.get(f));
	    	}
	    	
	    	join.setReplFiles(newrepl.toArray(join.getReplFiles()));
	    }
	}
	
	public void visitSOp(StormOper sop) throws VisitorException {
		if (sop.plan == null) {
			return;
		}
		
		new FRJoinFileReplacer(sop.plan).visit();
	}
	
	public void convert() {		
		// Start walking.
		try {
			visit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
