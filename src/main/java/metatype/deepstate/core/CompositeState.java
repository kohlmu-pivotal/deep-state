package metatype.deepstate.core;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import metatype.deepstate.FiniteStateMachine;

public class CompositeState<T> extends SimpleState<T> implements FiniteStateMachine<T> {
  private final DeepStateFsm<T> nested;

  public CompositeState(String name, Optional<Action> entry, Optional<Action> exit,
      Map<T, StateAction<T>> actions, Optional<StateAction<T>> defaultAction, Optional<Consumer<Exception>> uncaughtExceptionHandler, DeepStateFsm<T> nested) {
    super(name, entry, exit, actions, defaultAction, uncaughtExceptionHandler);
    this.nested = nested;
  }

  @Override
  public void accept(Event<T> event) {
    super.accept(event);
    nested.accept(event);
  }

  @Override
  public State getCurrentState() {
    return nested.getCurrentState();
  }
  
  @Override
  public Deque<State> getCurrentStates() {
    return nested.getCurrentStates();
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
