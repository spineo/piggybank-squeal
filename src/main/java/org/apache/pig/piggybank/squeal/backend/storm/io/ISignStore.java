package org.apache.pig.piggybank.squeal.backend.storm.io;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface ISignStore {

	public List<String> getUDFs();
	public void setSign(AtomicInteger sign);
	
}