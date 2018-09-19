package metatype.deepstate;

import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import metatype.deepstate.FiniteStateMachine.Event;

public interface FiniteStateMachine<T> extends Consumer<Event<T>> {
  String getName();
  State getCurrentState();
  Deque<State> getCurrentStates();

  interface State {
    String getName();
  }

  interface Transition {
    State getSource();
    State getDestination();
  }  
  
  interface Event<T> {
    T getTrigger();
  }
  
  interface Action extends Consumer<State> {
  }
  
  interface StateAction<T> extends BiConsumer<State, Event<T>> {
  }
  
  interface TransitionAction<T> extends BiConsumer<Transition, Event<T>> {
  }
  
  interface Guard<T> extends Predicate<Event<T>> {
  }
}
