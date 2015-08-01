package halflife.bus.registry;

import halflife.bus.KeyedConsumer;
import halflife.bus.concurrent.Atom;
import halflife.bus.key.Key;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;
import reactor.fn.tuple.Tuple;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcurrentRegistry<K extends Key> implements DefaultingRegistry<K> {

  private final Atom<PMap<K, PVector<Registration<K>>>>                    lookupMap;
  private final Map<KeyMissMatcher<K>, Function<K, Map<K, KeyedConsumer>>> keyMissMatchers;

  public ConcurrentRegistry() {
    this.lookupMap = new Atom<>(HashTreePMap.empty());
    this.keyMissMatchers = new ConcurrentHashMap<>();
  }

  @Override
  public void addKeyMissMatcher(KeyMissMatcher<K> matcher, Function<K, Map<K, KeyedConsumer>> supplier) {
    this.keyMissMatchers.put(matcher, supplier);
  }

  @Override
  public <V extends KeyedConsumer> Registration<K> register(K obj, V handler) {
    final PVector<Registration<K>> lookedUpArr = lookupMap.deref().get(obj);

    if (lookedUpArr == null) {
      final SimpleRegistration<K, V> reg = new SimpleRegistration<>(obj,
                                                                    handler,
                                                                    // TODO: FIX UNREGISTER
                                                                    reg1 -> lookupMap.deref()
                                                                                     .get(obj)
                                                                                     .remove(handler));
      final PVector<Registration<K>> emptyArr = TreePVector.singleton(reg);

      lookupMap.swap(old -> old.plus(obj, emptyArr));

      return reg;

    } else {
      final Registration<K> reg = new SimpleRegistration<>(obj,
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
    return lookupMap.swapReturnOther((map) -> {
      PMap<K, PVector<Registration<K>>> newv = map.minus(key);

      return Tuple.of(newv,
                      map.containsKey(key));
    });
  }

  @Override
  public boolean unregister(Predicate<K> pred) {
    return lookupMap.swapReturnOther((map) -> {
      List<K> unsubscribeKys = map.keySet()
                                  .stream()
                                  .filter(pred)
                                  .collect(Collectors.toList());

      PMap<K, PVector<Registration<K>>> newv = map.minusAll(unsubscribeKys);

      return Tuple.of(newv,
                      !unsubscribeKys.isEmpty());
    });
  }

  @Override
  public List<Registration<K>> select(final K key) {
    return
      lookupMap.swap(old -> {
        if (old.containsKey(key)) {
          return old;
        } else {
          return keyMissMatchers.entrySet()
                                .stream()
                                .filter((m) -> {
                                  return m.getKey().test(key);
                                })
                                .map(
                                  (Map.Entry<KeyMissMatcher<K>, Function<K, Map<K, KeyedConsumer>>> m) -> {
                                    return m.getValue();
                                  })
                                .flatMap((Function<K, Map<K, KeyedConsumer>> m) -> {
                                  return m.apply(key).entrySet().stream();
                                })
                                .reduce(old,
                                        (PMap<K, PVector<Registration<K>>> acc,
                                         Map.Entry<K, KeyedConsumer> entry) -> {
                                          Registration<K> reg = new SimpleRegistration<K, KeyedConsumer>(
                                            entry.getKey(),
                                            entry.getValue(),
                                            // TODO: Fix removes!
                                            null);

                                          if (acc.containsKey(entry.getKey())) {
                                            return acc.plus(entry.getKey(),
                                                            acc.get(entry.getValue()).plus(reg));
                                          } else {
                                            return acc.plus(entry.getKey(),
                                                            TreePVector.singleton(reg));
                                          }
                                        },
                                        PMap::plusAll);


        }
      }).getOrDefault(key, TreePVector.empty());

  }


  @Override
  public void clear() {
    // TODO: FIXME
    //    this.lookupMap.clear();
    //    this.keyMissMatchers.clear();
  }


  @Override
  public Iterator<Registration<K>> iterator() {
    return this.stream().iterator();
  }

  public Stream<Registration<K>> stream() {
    return this.lookupMap.deref().values().stream().flatMap(i -> i.stream());


  }
}
