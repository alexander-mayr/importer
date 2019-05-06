package com.importworker.importworker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import redis.clients.jedis.Jedis;

public class Worker {
	private Channel channel;
	private Jedis jedis;

	public Worker() throws IOException, TimeoutException, InterruptedException {
		this(new ConnectionFactory());
	}

	public Worker(ConnectionFactory factory) throws InterruptedException {
		while(!setup(factory)) {
			System.out.println("Setup failed. Retrying in 2s.");
			Thread.sleep(2000);
		}

		this.setupRedis();
		System.out.println("Setup done.");
	}

	void setupRedis() {
		String redis_host = System.getenv("REDIS_HOST");

		if(redis_host == null) {
			redis_host = "localhost";
		}

		this.jedis = new Jedis(redis_host);		
	}

	boolean setup(ConnectionFactory factory) {
		try {
			String brokerhost = System.getenv("BROKER_HOST");

			if(brokerhost == null) {
				factory.setHost("localhost");
			}
			else {
				factory.setHost(brokerhost);
			}
			
			Connection connection = factory.newConnection();
			this.channel = connection.createChannel();
			this.channel.basicQos(1);
			this.setupQueues(factory);
		} catch (IOException | TimeoutException e) {
			return false;
		}

		return true;
	}

	boolean setupQueues(ConnectionFactory factory) throws IOException, TimeoutException {
		Connection connection;
		connection = factory.newConnection();
		Channel channel = connection.createChannel();
		
		channel.exchangeDeclare("messages", "direct");
		channel.exchangeDeclare("messages-retry", "direct");
	
		Map<String, Object> messages_args = new HashMap<String, Object>();
		messages_args.put("x-dead-letter-exchange", "messages-retry");
		channel.queueDeclare("messages", true, false, false, messages_args);
		channel.queueBind("messages", "messages", "");
	
		Map<String, Object> retry_args = new HashMap<String, Object>();
		retry_args.put("x-dead-letter-exchange", "messages");
		retry_args.put("x-message-ttl", 3000);
		channel.queueDeclare("messages-retry", true, false, false, retry_args);
		channel.queueBind("messages-retry", "messages-retry", "");

		return true;
	}

	public void run() throws IOException, TimeoutException {
	    DeliverCallback callback = (consumerTag, delivery) -> {
	    	String message = new String(delivery.getBody(), "UTF-8");
	    	JSONObject json_object = new JSONObject(message);
	    	Integer retry_count = 0;
	    	Map<String, Object> headers = delivery.getProperties().getHeaders();
	
	    	if(headers != null) {
	    		ArrayList<Object> death = (ArrayList<Object>) headers.get("x-death");
	    		final Map<String, Object> deathHeader = (Map<String, Object>) death.get(0);
	    		retry_count = Integer.parseInt(deathHeader.get("count").toString());
	    	}

	    	String uuid = json_object.get("uuid").toString();
	    	String step = json_object.get("step").toString();
	    	String expected_step;

	    	if(jedis.get(uuid) == null) {
	    		expected_step = "0";
	    	}
	    	else {
	    		expected_step = jedis.get(uuid);
	    	}

	    	if(step.equals(expected_step)) {
	    		System.out.println(message + ": do work");

	    		this.doWork(message);
	    		jedis.set(uuid, Integer.toString(Integer.parseInt(expected_step) + 1));
	    		System.out.println(message + ": done");
	    		this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
	    	}
	    	else if(retry_count >= 10) {
	    		System.out.println(message + ": max retry reached. Stopping");
	    		this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
	    	}
	    	else {
	    		System.out.println(message + ": out of order. Retrying later");
	    		this.channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
	    	}        
	    };

	    this.channel.basicConsume("messages", false, callback, consumerTag -> { });
	}

	// dummy logic
	public boolean doWork(String message) throws IOException {
        System.out.println(message + ": processing ");
        
        try {
        	Thread.sleep(2000);
        }
        catch(java.lang.InterruptedException e) {
        	System.out.println(message + ": interrupted");
        }

        return true;
	}
}
