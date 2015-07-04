package halflife.bus.registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ConcurrentRegistry<K, V> implements DefaultingRegistry<K, V> {

  private final Map<Object, List<Registration<K, ? extends V>>> lookupMap;
  private final Map<KeyMissMatcher<K>, Supplier<? extends V>>   keyMissMatchers;
  private final ReentrantLock                                   lock;

  public ConcurrentRegistry() {
    this.lookupMap = new ConcurrentHashMap<>();
    this.keyMissMatchers = new ConcurrentHashMap<>();
    this.lock = new ReentrantLock();

  }

  public void addKeyMissMatcher(KeyMissMatcher<K> matcher,
                                Supplier<? extends V> supplier) {
    this.keyMissMatchers.put(matcher, supplier);
  }

  @Override
  public Registration<K, V> register(K obj, V handler) {
    final List<Registration<K, ? extends V>> lookedUpArr = lookupMap.get(obj);

    if (lookedUpArr == null) {
      final List<Registration<K, ? extends V>> emptyArr = new ArrayList<>();
      final SimpleRegistration<K, V> reg = new SimpleRegistration<>(obj,
                                                                    handler,
                                                                    reg1 -> lookupMap.get(obj).remove(handler));
      emptyArr.add(reg);

      lookupMap.put(obj, emptyArr);
      return reg;
    } else {
      final Registration<K, V> reg = new SimpleRegistration<>(obj, handler,
                                                              reg1 -> lookupMap.get(obj).remove(reg1));
      lookedUpArr.add(reg);
      lookupMap.put(obj, lookedUpArr);
      return reg;
    }
  }

  @Override
  public boolean unregister(K key) {
    boolean res = lookupMap.containsKey(key);

    if (res) {
      lookupMap.remove(key);
    }

    return res;
  }

  @Override
  public List<Registration<K, ? extends V>> select(final K key) {
    lookupMap.computeIfAbsent(key, (k_) -> {
      final List<Registration<K, ? extends V>> acc = new ArrayList<>();
      keyMissMatchers.entrySet()
                            .stream()
                            .filter((m) -> m.getKey().test(key))
                            .forEach((matcher) ->  {
                              V handler = matcher.getValue().get();
                              acc.add(new SimpleRegistration<>(key,
                                                               handler,
                                                               reg ->  acc.remove(handler)));

                            });
      return acc;
    });


    return lookupMap.get(key);
  }


  @Override
  public void clear() {
    this.lookupMap.clear();
    this.keyMissMatchers.clear();
  }


  @Override
  public Iterator<Registration<K, V>> iterator() {
    return null;
  }
}
