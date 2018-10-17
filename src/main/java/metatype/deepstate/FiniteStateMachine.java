package metatype.deepstate;

import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A representation of a UML state machine.
 *
 * @param <T> the type of the event trigger
 * @param <U> the type of the state name
 */
public interface FiniteStateMachine<T, U> {
  /**
   * Returns the current state.
   * @return the current state
   */
  State<U> getCurrentState();
  
  /**
   * Returns an ordered queue containing the current state and any nested, active states.
   * The front of the queue holds the top-level state and the end of the queue holds the
   * most deeply nested state.
   * 
   * @return the current states
   */
  Deque<State<U>> getCurrentStates();

  /**
   * Obtains the supplied value in a thread-safe way.  This allows reads to be coordinated
   * with updates from the state machine.
   * 
   * @param value the value to get
   * @return the value
   */
  <R> R read(Supplier<R> value);
  
  /**
   * Updates the state machine by applying the supplied event, invoking any actions and transitions
   * as needed according to the state machine definition.
   * 
   * @param event the event to apply
   */
  void accept(Event<T> event);
  
  /**
   * A logical representation of the allowed conditions within a system or component.  Must
   * be uniquely identified.
   * 
   * @param <U> the type of the state identity
   */
  interface State<U> {
    /**
     * Returns an object representing the identity of the state.
     * @return the identity
     */
    U getIdentity();
  }

  /**
   * Links states together.
   *
   * @param <U> the type of the state name
   */
  interface Transition<U> {
    /**
     * The source state of the transition.
     * @return the source
     */
    State<U> getSource();
    
    /**
     * The destination state of the transition.
     * @return the destination
     */
    State<U> getDestination();
  }  
  
  /**
   * External signals that may transitions and action invocations within the state machine.  Events
   * contain a uniquely defined trigger that allows the state machine to intepret and respond to the
   * event.
   *
   * @param <T> the type of the event trigger
   */
  interface Event<T> {
    /**
     * Returns the trigger for this event.
     * @return the trigger
     */
    T getTrigger();
  }
  
  /**
   * An action that is invoked when entering or leaving a state.
   *
   * @param <U> the type of the state name
   */
  interface Action<U> extends Consumer<State<U>> {
  }
  
  /**
   * An action that can be invoked with a state.
   *
   * @param <T> the type of the trigger
   * @param <U> the type of the state name
   */
  interface StateAction<T, U> extends BiConsumer<State<U>, Event<T>> {
  }
  
  /**
   * An action that can be invoked during a transition.
   *
   * @param <T> the type of the trigger
   * @param <U> the type of the state name
   */
  interface TransitionAction<T, U> extends BiConsumer<Transition<U>, Event<T>> {
  }
  
  /**
   * A condition that can be applied to a transition.
   *
   * @param <T> the type of the trigger
   */
  interface Guard<T> extends Predicate<Event<T>> {
  }
}
