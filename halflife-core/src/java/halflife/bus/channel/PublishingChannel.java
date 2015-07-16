package halflife.bus.channel;

public interface PublishingChannel<T> {

  public void tell(T item);

}
