package halflife;


import halflife.bus.Firehose;
import halflife.bus.integration.StreamTuple;
import halflife.bus.integration.Upstream;
import halflife.bus.key.Key;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * See: https://cwiki.apache.org/confluence/display/KAFKA/Consumer+Group+Example
 */
public class KafkaUpstream<K extends Key, V> extends Upstream<K, V> {

  private final ExecutorService executor;

  public KafkaUpstream(Firehose firehose,
                       Properties consumerProperties,
                       String topic,
                       Decoder<K> keyDecoder,
                       Decoder<V> valueDecoder) {
    super(firehose);

    this.executor = Executors.newFixedThreadPool(1);
    ConsumerConnector consumer = Consumer.createJavaConsumerConnector(
      new ConsumerConfig(consumerProperties));

    Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    topicCountMap.put(topic, 1);
    Map<String, List<KafkaStream<K, V>>> consumerMap = consumer.createMessageStreams(topicCountMap,
                                                                                     keyDecoder,
                                                                                     valueDecoder);
    List<KafkaStream<K, V>> streams = consumerMap.get(topic);

    for (final KafkaStream stream : streams) {
      executor.execute(() -> {
        ConsumerIterator<K, V> it = stream.iterator();
        while (it.hasNext() && !Thread.currentThread().isInterrupted()) {
          MessageAndMetadata<K, V> msg = it.next();
          publish(new StreamTuple<K, V>(msg.key(), msg.message()));
        }
      });
    }
  }

  public void stop() {
    this.executor.shutdown();
  }

}
