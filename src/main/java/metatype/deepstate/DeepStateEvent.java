package metatype.deepstate;

import java.util.function.Supplier;

import metatype.deepstate.FiniteStateMachine.Event;

public class DeepStateEvent<T> implements Event<String>, Supplier<T> {
  private final String trigger;
  private final T payload;

  public DeepStateEvent(String trigger) {
    this(trigger, null);
  }
  
  public DeepStateEvent(String trigger, T payload) {
    this.trigger = trigger;
    this.payload = payload;
  }
  
  @Override
  public String getTrigger() {
    return trigger;
  }
  
  @Override
  public String toString() {
    return trigger;
  }

  @Override
  public T get() {
    return payload;
  }
}
