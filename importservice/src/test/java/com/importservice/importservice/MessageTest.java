package com.importservice.importservice;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MessageTest {
	@Before
	public void setup() throws IOException, TimeoutException {
		ConnectionFactory factory = new MockConnectionFactory();
		Connection connection = factory.newConnection();
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
	}

	Message message = new Message("test-uuid", "test-message", 0, new MockConnectionFactory());

   @Test
   public void testQueueup() {
      assertEquals(message.queueUp(), true);
   }

   @Test
   public void testSetupQueues() {
      assertEquals(message.setupQueues(), true);
   }
}