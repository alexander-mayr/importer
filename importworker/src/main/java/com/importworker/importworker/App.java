package com.importworker.importworker;

import com.rabbitmq.client.ConnectionFactory;

public class App {
    public static void main(String[] argv) throws Exception {
        Worker worker = new Worker(new ConnectionFactory());
        worker.run();
    }
}
