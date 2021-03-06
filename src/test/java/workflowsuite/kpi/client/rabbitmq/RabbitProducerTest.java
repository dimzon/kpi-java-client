package workflowsuite.kpi.client.rabbitmq;

import org.junit.Assert;
import workflowsuite.kpi.client.CheckpointMessage;
import workflowsuite.kpi.client.MessageProducer;
import workflowsuite.kpi.client.serviceregistry.ServiceRegistryClient;

import javax.net.SocketFactory;
import java.net.URI;
import java.time.Instant;

public class RabbitProducerTest {
    //@Test
    public void trySendMessage() throws Exception {
        ServiceRegistryClient serviceRegistryClient = new ServiceRegistryClient(new URI("http://msk-dev-foris:9130/"),
                org.slf4j.LoggerFactory.getILoggerFactory());
        MessageProducer rabbitProducer = new CheckpointRabbitProducer(
                new RabbitConfigurationProvider(serviceRegistryClient, ServiceRegistryClient.DEFAULT_REFRESH_TIME,
                        RabbitConfigurationProvider.KPI_GENERAL_QUEUE_CONTRACT),
                org.slf4j.LoggerFactory.getILoggerFactory(), SocketFactory.getDefault());
        CheckpointMessage msg = new CheckpointMessage();
        msg.setSessionId("02be60e389d24425bb6e6254fbfe1cae");
        msg.setClientEventTime(Instant.now());
        msg.setSynchronizedEventTime(Instant.now());
        msg.setCheckpointCode("TestCheckpoint");
        msg.setUnreachable(true);
        boolean send = rabbitProducer.trySendMessage(msg);
        Assert.assertTrue(send);
    }
}
