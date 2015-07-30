package halflife;

import halflife.bus.integration.Downstream;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.function.Function;

/**
 * Publishes all the received values to the given kafka topic
 */
public class KafkaDownstream<K, V> implements Downstream<K, V> {

  private final KafkaProducer<K, V>  kafkaProducer;
  private final String               topic;
  private final Function<K, Integer> partitioner;

  public KafkaDownstream(Properties producerProperties,
                         String topic,
                         Function<K, Integer> partitioner) {
    this.kafkaProducer = new KafkaProducer<>(producerProperties);
    this.topic = topic;
    this.partitioner = partitioner;
  }

  @Override
  public void accept(K key, V value) {
    ProducerRecord<K, V> record = new ProducerRecord<>(topic,
                                                       partitioner.apply(key),
                                                       key,
                                                       value);
    kafkaProducer.send(record);
  }
}
