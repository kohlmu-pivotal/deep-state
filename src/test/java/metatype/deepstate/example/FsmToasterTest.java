package metatype.deepstate.example;

import java.time.Duration;

public class FsmToasterTest extends ToasterTest {
  @Override
  public Toaster create(Duration autoIgnite) {
    return new FsmToaster(autoIgnite);
  }
}
