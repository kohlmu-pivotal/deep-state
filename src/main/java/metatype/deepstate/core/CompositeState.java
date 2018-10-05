package metatype.deepstate.core;

import java.util.Deque;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import metatype.deepstate.FiniteStateMachine;

public class CompositeState<T, U> extends SimpleState<T, U> implements FiniteStateMachine<T, U> {
  private final DeepStateFsm<T, U> nested;

  public CompositeState(U name, Action<U> entry, Action<U> exit,
      Map<T, StateAction<T, U>> actions, StateAction<T, U> defaultAction, Consumer<Exception> uncaughtExceptionHandler, DeepStateFsm<T, U> nested) {
    super(name, entry, exit, actions, defaultAction, uncaughtExceptionHandler);
    this.nested = nested;
  }

  @Override
  public void accept(Event<T> event) {
    super.accept(event);
    nested.accept(event);
  }

  @Override
  public State<U> getCurrentState() {
    return nested.getCurrentState();
  }
  
  @Override
  public Deque<State<U>> getCurrentStates() {
    return nested.getCurrentStates();
  }
  
  @Override
  public <R> R read(Supplier<R> value) {
    return nested.read(value);
  }
  
  @Override
  public void enter() {
    super.enter();
    nested.begin();
  }
  
  @Override
  public void exit() {
    nested.end();
    super.exit();
  }
}
