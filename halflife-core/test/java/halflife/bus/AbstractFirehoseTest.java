package halflife.bus;

import halflife.bus.key.Key;
import org.junit.After;
import org.junit.Before;

public class AbstractFirehoseTest {

  protected Firehose<Key> firehose;

  @Before
  public void setup() {
    this.firehose = new Firehose<>();
  }

  @After
  public void teardown() {
  }

}


