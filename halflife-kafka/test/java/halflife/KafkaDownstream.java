package halflife;

import halflife.bus.integration.Downstream;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class KafkaDownstream<K, V> implements Downstream<K, V> {

  private final KafkaProducer<String, String> kafkaProducer;

  public KafkaDownstream(Properties producerProperties) {
    kafkaProducer = new KafkaProducer<>(producerProperties);
  }

  @Override
  public void accept(K key, V value) {

  }
}
