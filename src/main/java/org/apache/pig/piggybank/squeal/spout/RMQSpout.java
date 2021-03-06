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

package org.apache.pig.piggybank.squeal.spout;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class RMQSpout extends BaseRichSpout {
	ConnectionFactory factory;
	Connection connection;
	Channel channel;
	private QueueingConsumer consumer;
	private String rabbitURI;
	private String exchangeName;
	private SpoutOutputCollector collector;
	private String _queueName;
	private Random r;
	private Map<Integer, Long> out_id = new HashMap<Integer, Long>();
	int flush_counter = 0;
	int QOS = 250;
		
	public RMQSpout(String rabbitURI, String exchangeName) {
		this(rabbitURI, exchangeName, null);
	}
	public RMQSpout(String rabbitURI, String exchangeName, String queueName) {
		this(rabbitURI, exchangeName, queueName, null);
	}
	
	public RMQSpout(String rabbitURI, String exchangeName, String queueName, String queueSize) {
		this.rabbitURI = rabbitURI;
		this.exchangeName = exchangeName;
		this._queueName = queueName;
		if (queueSize != null) {
			QOS = Integer.parseInt(queueSize);
		}
	}
	
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		this.collector = collector;
		if (factory == null) {
			connect();
		}
		r = new Random();
	}

	public void connect() {
		try {
			factory = new ConnectionFactory();
			factory.setUri(rabbitURI);

			connection = factory.newConnection();
			channel = connection.createChannel();

			channel.exchangeDeclare(exchangeName, "fanout", true);
			if (QOS > 0) {
				channel.basicQos(QOS);				
			}
			
			// TODO: Setup message-ttl so we don't gum up the works if things get bad.
//			Map<String, Object> args = new HashMap<String, Object>();
//			args.put("x-message-ttl", 60000);

			String queueName;
			if (_queueName != null) {
				queueName = _queueName;
				channel.queueDeclare(queueName, true, false, false, null);
			} else {
				queueName = channel.queueDeclare().getQueue();				
			}
			
			channel.queueBind(queueName, exchangeName, "");

			consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, false, consumer);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to setup reader", e);
		}
	}
	
	public void activate() {
		if (factory == null) {
			connect();
		}
	}

	public void disconnect() {
		if (factory != null) {
			try {
				consumer = null;
				channel.close();
				channel = null;
				connection.close();
				connection = null;
				factory = null;
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
	public void deactivate() {
		disconnect();
	}
	
	public void nextTuple() {
		try {
			Delivery d = consumer.nextDelivery(0);
			if (d != null) {
				long tag = d.getEnvelope().getDeliveryTag();
				int an_id = r.nextInt();
				out_id.put(an_id, tag);
				collector.emit(new Values(d.getBody()), an_id);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void ack(Object msgId) {
		try {
			Integer an_id = (Integer) msgId;
			channel.basicAck(out_id.remove(an_id), false);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void fail(Object msgId) {
		try {
			Integer an_id = (Integer) msgId;
			channel.basicNack(out_id.remove(an_id), false, false);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("msg"));
	}
	
	public static void main(String args[]) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: " + RMQSpout.class.getCanonicalName() + " <rmquri> <exchange> [queuename] ");
			return;
		}
		
		RMQSpout rmq;
		if (args.length == 2) {
			rmq = new RMQSpout(args[0], args[1]);
		} else {
			rmq = new RMQSpout(args[0], args[3]);
		}
		rmq.connect();
		
		while (true) {
			Delivery d = rmq.consumer.nextDelivery();
			System.out.println(new String(d.getBody()));
			rmq.channel.basicAck(d.getEnvelope().getDeliveryTag(), false);
		}
		
	}
}
