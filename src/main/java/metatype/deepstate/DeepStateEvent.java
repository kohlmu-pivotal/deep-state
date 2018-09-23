package metatype.deepstate;

import java.util.function.Supplier;

import metatype.deepstate.FiniteStateMachine.Event;

public class DeepStateEvent<T, P> implements Event<T>, Supplier<P> {
  private final T trigger;
  private final P payload;

  public DeepStateEvent(T trigger) {
    this(trigger, null);
  }
  
  public DeepStateEvent(T trigger, P payload) {
    this.trigger = trigger;
    this.payload = payload;
  }
  
  @Override
  public T getTrigger() {
    return trigger;
  }
  
  @Override
  public String toString() {
    return trigger.toString();
  }

  @Override
  public P get() {
    return payload;
  }
}
