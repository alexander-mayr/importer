package com.importservice.importservice;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;
import com.rabbitmq.client.Channel;

public class Message {
	private String uuid;
	private String message;
	private Integer step;
	private ConnectionFactory factory;

	public Message(String uuid, String message, Integer step) {
		this(uuid, message, step, new ConnectionFactory());
	}

	public Message(String uuid, String message, Integer step, ConnectionFactory factory) {
		this.uuid = uuid;
		this.message = message;
		this.step = step;
		this.factory = factory;

		String brokerhost = System.getenv("BROKER_HOST");

		if(brokerhost == null) {
			factory.setHost("localhost");
		}
		else {
			factory.setHost(brokerhost);
		}

		this.factory.setHost(brokerhost);
	}

	public String getQueuePayload() {
		Map<String, String> dictionary = new HashMap<String, String>();
		dictionary.put("uuid", this.uuid);
		dictionary.put("step", this.step.toString());
		dictionary.put("message", this.message);

		return new JSONObject(dictionary).toString();
	}

	public String getMessage() {
		return this.message;
	}

	public boolean queueUp()  {
		if(this.setupQueues()) {
			try {
				Connection connection = this.factory.newConnection();
		    	Channel channel = connection.createChannel();
				channel.basicPublish("messages", "", null, this.getQueuePayload().getBytes());
			}
			catch(java.util.concurrent.TimeoutException e) {
				return false;
			}
			catch(java.io.IOException e) {
				return false;
			}

			return true;			
		}
		else {
			return false;
		}

	}

	boolean setupQueues() {
		Connection connection;
		try {
			connection = this.factory.newConnection();
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
		} catch (IOException | TimeoutException e) {
			return false;
		}

		return true;
	}
}

