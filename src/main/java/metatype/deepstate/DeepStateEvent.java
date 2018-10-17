package metatype.deepstate;

import java.util.Optional;

import metatype.deepstate.FiniteStateMachine.Event;

/**
 * An event that contains a trigger and an optional payload
 *
 * @param <T> the trigger type
 * @param <P> the payload type
 */
public class DeepStateEvent<T, P> implements Event<T> {
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

  public Optional<P> getPayload() {
    return Optional.ofNullable(payload);
  }
}
