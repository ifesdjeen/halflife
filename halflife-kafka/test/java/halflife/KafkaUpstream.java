package halflife;

import halflife.bus.Firehose;
import halflife.bus.integration.Upstream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO: reduce amount of generic args
public class KafkaUpstream<K, K1, V, V1> extends Upstream<K1, V1> {

  private final String topic;
  private final KafkaConsumer<K, V> kafkaConsumer;
  private final Thread workerThread;
  private final Consumer<Exception> onException;

  public KafkaUpstream(Firehose firehose,
                       Properties consumerProperties,
                       String topic,
                       int partition,
                       Function<K, K1> keyMapper,
                       Function<V, V1> valueMapper,
                       Consumer<Exception> onException) {
    super(firehose);
    this.topic = topic;
    this.kafkaConsumer = new KafkaConsumer<K, V>(consumerProperties);
    this.onException = onException;

    kafkaConsumer.subscribe(new TopicPartition(topic, partition));

    this.workerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while(!Thread.currentThread().isInterrupted()) {
          try {
            for (ConsumerRecord<K, V> record : kafkaConsumer.poll(1000).records(topic)) {
              try {
                publish(keyMapper.apply(record.key()),
                        valueMapper.apply(record.value()));
              } catch (Exception e) {
                onException.accept(e);
              }
            }
          } catch (Exception e) {
            onException.accept(e);
          }
        }
      }
    });

    this.workerThread.start();
  }

  public void stop() {
    this.workerThread.interrupt();
  }

}
