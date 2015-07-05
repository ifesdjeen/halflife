package halflife.bus.registry;

import halflife.bus.concurrent.Atom;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConcurrentRegistry<K, V> implements DefaultingRegistry<K, V> {

  private final Atom<PMap<Object, PVector<Registration<K, ? extends V>>>> lookupMap;
  private final Map<KeyMissMatcher<K>, Function<K, List<? extends V>>>    keyMissMatchers;

  public ConcurrentRegistry() {
    this.lookupMap = new Atom<>(HashTreePMap.empty());
    this.keyMissMatchers = new ConcurrentHashMap<>();
  }

  @Override
  public void addKeyMissMatcher(KeyMissMatcher<K> matcher, Function<K, List<? extends V>> supplier) {
    this.keyMissMatchers.put(matcher, supplier);
  }

  @Override
  public Registration<K, V> register(K obj, V handler) {
    final PVector<Registration<K, ? extends V>> lookedUpArr = lookupMap.deref().get(obj);

    if (lookedUpArr == null) {
      final SimpleRegistration<K, V> reg = new SimpleRegistration<>(obj,
                                                                    handler,
                                                                    // TODO: FIX UNREGISTER
                                                                    reg1 -> lookupMap.deref()
                                                                                     .get(obj)
                                                                                     .remove(handler));
      final PVector<Registration<K, ? extends V>> emptyArr = TreePVector.singleton(reg);

      lookupMap.swap(old -> old.plus(obj, emptyArr));

      return reg;

    } else {
      final Registration<K, V> reg = new SimpleRegistration<>(obj,
                                                              handler,
                                                              // TODO: FIX REMOVES!!
                                                              reg1 -> lookupMap.deref()
                                                                               .get(obj)
                                                                               .remove(reg1));
      lookupMap.swap(old -> old.plus(obj, old.get(obj).plus(reg)));
      return reg;
    }

  }

  @Override
  public boolean unregister(K key) {
    //    boolean res = lookupMap.containsKey(key);
    //
    //    if (res) {
    //      lookupMap.remove(key);
    //    }
    // TODO: FIXME
    return false;
  }

  @Override
  public List<Registration<K, ? extends V>> select(final K key) {
    return lookupMap.swap(old -> {
      if (old.containsKey(key)) {
        return old;
      } else {
        final List<Registration<K, ? extends V>> acc = new ArrayList<>();
        keyMissMatchers.entrySet()
                       .stream()
                       .filter((m) -> m.getKey().test(key))
                       .forEach((matcher) -> {
                         List<? extends V> handlers = matcher.getValue().apply(key);
                         for (V handler: handlers) {
                           acc.add(new SimpleRegistration<>(key,
                                                            handler,
                                                            // TODO: FIX REMOVES!!!
                                                            reg -> acc.remove(handler)));
                         }
                       });
        return old.plus(key, TreePVector.from(acc));
      }
    }).get(key);
  }


  @Override
  public void clear() {
    //    this.lookupMap.clear();
    //    this.keyMissMatchers.clear();
  }


  @Override
  public Iterator<Registration<K, V>> iterator() {
    return null;
  }
}
