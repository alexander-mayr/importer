package com.importworker.importworker;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;

public class WorkerTest {
   @Test
   public void testdoWork() throws IOException, TimeoutException {
	   Worker worker = new Worker(new MockConnectionFactory());
	   assertEquals(worker.setupQueues(new MockConnectionFactory()), true);
   }
}
