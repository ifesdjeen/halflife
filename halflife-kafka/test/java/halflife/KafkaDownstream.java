package halflife;

import halflife.bus.integration.Downstream;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.function.Function;

/**
 * Publishes all the received values to the given kafka topic
 */
public class KafkaDownstream<K, K1, V, V1> implements Downstream<K, V> {

  private final KafkaProducer<K1, V1> kafkaProducer;
  private final String                topic;
  private final Function<K, K1>       keyMapper;
  private final Function<V, V1>       valueMapper;
  private final Function<K1, Integer> partitioner;

  public KafkaDownstream(Properties producerProperties,
                         String topic,
                         Function<K, K1> keyMapper,
                         Function<V, V1> valueMapper,
                         Function<K1, Integer> partitioner) {
    this.kafkaProducer = new KafkaProducer<>(producerProperties);
    this.topic = topic;
    this.keyMapper = keyMapper;
    this.valueMapper = valueMapper;
    this.partitioner = partitioner;
  }

  @Override
  public void accept(K key, V value) {
    K1 transformedKey = keyMapper.apply(key);
    ProducerRecord<K1, V1> record = new ProducerRecord<>(topic,
                                                         partitioner.apply(transformedKey),
                                                         transformedKey,
                                                         valueMapper.apply(value));
    kafkaProducer.send(record);
  }
}
