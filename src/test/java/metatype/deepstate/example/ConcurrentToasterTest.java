package metatype.deepstate.example;

import java.time.Duration;

public class ConcurrentToasterTest extends ToasterTest {
  @Override
  public Toaster create(Duration autoIgnite) {
    return new ConcurrentToaster(autoIgnite);
  }
}
