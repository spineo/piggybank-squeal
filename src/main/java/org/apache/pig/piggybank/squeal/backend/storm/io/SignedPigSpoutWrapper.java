package org.apache.pig.piggybank.squeal.backend.storm.io;

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.mapreduce.Job;
import org.apache.pig.ResourceSchema;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.io.NullableTuple;
import org.apache.pig.impl.util.StorageUtil;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.tuple.TridentTuple;
import backtype.storm.tuple.Values;

public class SignedPigSpoutWrapper extends SpoutWrapper {
	
	public SignedPigSpoutWrapper(String spoutClass) {
		super(spoutClass, null);
	}
	
	public SignedPigSpoutWrapper(String spoutClass, String jsonArgs) {
		super(spoutClass, jsonArgs, null);
	}
	
	public SignedPigSpoutWrapper(String spoutClass, String jsonArgs, String parallelismHint) {
		super(spoutClass, jsonArgs, parallelismHint);
	}

	@Override
	public ResourceSchema getSchema(String location, Job job)
			throws IOException {
		return null;
	}
	
	public Class<? extends BaseFunction> getTupleConverter() {
		return MakePigTuples.class;
	}
	
	static public class MakePigTuples extends BaseFunction {
		private TupleFactory tf;
		
		@Override
		public void prepare(java.util.Map conf, TridentOperationContext context) {
			 tf = TupleFactory.getInstance();
		}
			
		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			byte[] buf;
			try {
				buf = DataType.toBytes(tuple.get(0));
			} catch (ExecException e) {
				throw new RuntimeException(e);
			}
			
			Tuple t = StorageUtil.bytesToTuple(buf, 0, buf.length, (byte) '\t');;
			Integer sign;
//			System.err.println("SignedPigSpout.execute: " + t);
			List<Object> lst = t.getAll();
			DataByteArray dba = (DataByteArray) lst.remove(lst.size() - 1);
			sign = Integer.parseInt(dba.toString().replace("\n", ""));
			t = tf.newTupleNoCopy(lst);
			
			collector.emit(new Values(null, new NullableTuple(t), sign));
		}

	}
}