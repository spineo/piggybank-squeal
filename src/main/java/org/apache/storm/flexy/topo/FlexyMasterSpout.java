package org.apache.storm.flexy.topo;

import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class FlexyMasterSpout extends BaseRichSpout {
	private static final Log log = LogFactory.getLog(FlexyMasterSpout.class);

	private SpoutOutputCollector collector;
	int cur_state = 0;
	Integer cur_batch = 0;
	boolean last_failed = false;
	Random r;

	@Override
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		this.collector = collector;
		r = new Random();
	}

	@Override
	public void nextTuple() {
		if (cur_state == 0) {
			// Start a new batch.
			collector.emit("start", new Values(cur_batch, last_failed), r.nextInt());
			last_failed = false;
			cur_batch ++;
			cur_state ++;
		} else if (cur_state == 1) {
			// Waiting for propagation of batch..
		} else if (cur_state == 2) {
			// Batch completed, begin commit.
			collector.emit("commit", new Values(cur_batch), r.nextInt());
			cur_state ++;
		} else if (cur_state == 3) {
			// Waiting for commit complete.
		} else if (cur_state == 4) {
			// Commit failed, roll back.
			collector.emit("commit", new Values(-cur_batch), r.nextInt());
		} else {
			// Do nothing.
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("start", new Fields("batchid", "last_failed"));
		declarer.declareStream("commit", new Fields("batchid"));
	}

	@Override
    public void ack(Object msgId) {
		// Successful complete drops us back to go.
		if (cur_state > 3) {
			cur_state = 0;
		}
		cur_state ++;
    }

    @Override
    public void fail(Object msgId) {
    	// We need to switch to a failed state.
    	cur_state = 4;
    	last_failed = true;
    }
}
