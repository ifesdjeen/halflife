package halflife.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ProducderConsumerTest {

  @Before
  public void setup() {

  }

  @After
  public void teardown() {

  }

  @Test
  public void simpleProducerConsumerTest() throws ExecutionException, InterruptedException {
    String topic = UUID.randomUUID().toString();

    Properties producerProperties = new Properties();
    producerProperties.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "testProducer");
    producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
    producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                   "org.apache.kafka.common.serialization.StringSerializer");
    producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                   "org.apache.kafka.common.serialization.StringSerializer");

    KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);
    System.out.println(1);
    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    System.out.println(producer.send(new ProducerRecord<String, String>(topic, 0, "key", "value")).get());
    System.out.println(2);

    Properties consumerProperties = new Properties();
    consumerProperties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, "testConsumer");
    consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                   "org.apache.kafka.common.serialization.StringDeserializer");
    consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                   "org.apache.kafka.common.serialization.StringDeserializer");
    consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
    consumerProperties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProperties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    consumerProperties.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "1000");

    KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerProperties);
    consumer.subscribe(new TopicPartition(topic, 0));


    for (ConsumerRecord<String, String> record : consumer.poll(1000).records(topic)) {
      System.out.println(record.key());
      System.out.println(record.value());
    }
  }
}


