package halflife.bus.registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentRegistry<K, V> implements Registry<K, V> {

  private final Map<Object, List<Registration<K, ? extends V>>> lookupMap;
  private final ReentrantLock                         lock;

  public ConcurrentRegistry() {
    this.lookupMap = new ConcurrentHashMap<>();
    this.lock = new ReentrantLock();
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
  public List<Registration<K, ? extends V>> select(K key) {
    List<Registration<K, ? extends V>> lookedUp = lookupMap.get(key);

    return lookedUp;
  }

  @Override
  public void clear() {

  }

  @Override
  public Iterator<Registration<K, ? extends V>> iterator() {
    return null;
  }
}
