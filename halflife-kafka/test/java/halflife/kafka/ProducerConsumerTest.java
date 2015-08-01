package halflife.kafka;

import halflife.KafkaDownstream;
import halflife.bus.Firehose;
import halflife.bus.key.Key;
import halflife.bus.registry.ConcurrentRegistry;
import halflife.bus.registry.DefaultingRegistry;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.Environment;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class ProducerConsumerTest {

  protected Firehose<Key>           firehose;
  protected Environment             environment;
  protected DefaultingRegistry<Key> consumerRegistry;
  protected Consumer<Throwable>     dispatchErrorHandler;

  @Before
  public void setup() {
    this.environment = new Environment();
    this.consumerRegistry = new ConcurrentRegistry<>();
    this.dispatchErrorHandler = throwable -> {
      System.out.println(throwable.getMessage());
      throwable.printStackTrace();
    };
    this.firehose = new Firehose<>(environment.getDispatcher("sync"),
                                   consumerRegistry,
                                   null,
                                   null);
  }

  @After
  public void teardown() {
    this.environment.shutdown();
  }

  @Test
  public void simpleUpstreamDownstreamTest() {

    Properties producerProperties = new Properties();
    producerProperties.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "testProducer");
    producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "workstation:9092");
    producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                   "org.apache.kafka.common.serialization.StringSerializer");
    producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                   "org.apache.kafka.common.serialization.StringSerializer");
    KafkaDownstream<Key, String> kafkaDownstream = new KafkaDownstream<>(producerProperties,
                                                                         "test_topic",
                                                                         (i) -> 0);

    //    this.firehose.on(Key.wrap("kafkaDownstream"),
    //                     kafkaDownstream);


  }

  @Test
  public void simpleProducerConsumerTest() throws ExecutionException, InterruptedException {
    //    String topic = UUID.randomUUID().toString();
    //
    //    Properties producerProperties = new Properties();
    //    producerProperties.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "testProducer");
    //    producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "workstation:9092");
    //    producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
    //                                   "org.apache.kafka.common.serialization.StringSerializer");
    //    producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
    //                                   "org.apache.kafka.common.serialization.StringSerializer");
    //
    //    KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);
    //    System.out.println(1);
    //    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    //    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    //    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    //    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    //    System.out.println(2);
    //
    //    Properties consumerProperties = new Properties();
    //    consumerProperties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, "testConsumer");
    //    consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "workstation:9092");
    //    consumerProperties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    //    consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    //    consumerProperties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    //    consumerProperties.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "1000");
    //
    //    KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerProperties);
    //    consumer.subscribe(new TopicPartition(topic, 0));
    //
    //
    //    for (ConsumerRecord<String, String> record : consumer.poll(1000).records(topic)) {
    //      System.out.println(record.key());
    //      System.out.println(record.value());
    //    }

  }
}


