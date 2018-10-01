package metatype.deepstate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import metatype.deepstate.FiniteStateMachine.Action;
import metatype.deepstate.FiniteStateMachine.Guard;
import metatype.deepstate.FiniteStateMachine.StateAction;
import metatype.deepstate.FiniteStateMachine.TransitionAction;
import metatype.deepstate.core.CompositeState;
import metatype.deepstate.core.DeepStateFsm;
import metatype.deepstate.core.SimpleState;
import metatype.deepstate.core.TriggeredTransition;

public class DeepState {
  private DeepState() { }
  
  public static <T, U> FsmFactory<T, U> model() {
    return new FsmFactory<>();
  }
  
  public static class FsmFactory<T, U> {
    private U initialState;
    private Map<U, StateFactory<T, U>> states;
    private Set<TransitionFactory<T, U>> transitions;
    
    private Optional<Consumer<Exception>> uncaughtExceptionHandler;
    private Optional<StateFactory<T, U>> parent;
    
    private FsmFactory() {
      this(Optional.empty());
    }
    
    private FsmFactory(Optional<StateFactory<T, U>> parentState) {
      states = new HashMap<>();
      transitions = new HashSet<>();
      uncaughtExceptionHandler = Optional.empty();
      this.parent = parentState;
    }

    public FsmFactory<T, U> configure(Consumer<FsmFactory<T, U>> factory) {
      factory.accept(this);
      return this;
    }

    public StateFactory<T, U> startingWith(U state) {
      initialState = state;
      return define(state);
    }

    public StateFactory<T, U> define(U state) {
      if (states.containsKey(state)) {
        throw new IllegalStateException("Unable to redefine existing state " + state);
      }
      
      StateFactory<T, U> factory = new StateFactory<>(this);
      states.put(state, factory);
      return factory;
    }
    
    public FsmFactory<T, U> catchExceptionsUsing(Consumer<Exception> exceptionHandler) {
      uncaughtExceptionHandler = Optional.of(exceptionHandler);
      return this;
    }
    
    public TransitionFactory<T, U> transition(T trigger) {
      TransitionFactory<T, U> factory = new TransitionFactory<>(this, trigger);
      transitions.add(factory);
      return factory;
    }
    
    public StateFactory<T, U> parent() {
      return parent.orElseThrow(() -> new IllegalStateException("Parent state is not defined"));
    }
    
    public DeepStateFsm<T, U> ready() {
      return create().begin();
    }
    
    private DeepStateFsm<T, U> create() {
      if (initialState == null) {
        throw new IllegalStateException("Initial state is not defined");
      }
      
      Map<U, SimpleState<T, U>> realStates = new HashMap<>();
      states.forEach((name, factory) -> {
        realStates.put(name, factory.create(name, uncaughtExceptionHandler));
      });
      
      Set<TriggeredTransition<T, U>> realTransitions = new HashSet<>();
      transitions.forEach((factory) -> {
        SimpleState<T, U> from = realStates.get(factory.from);
        if (from == null) {
          throw new IllegalStateException("Undefined from state " + factory.from + " for transition " + factory.trigger);
        }
        
        SimpleState<T, U> to = realStates.get(factory.to);
        if (to == null) {
          throw new IllegalStateException("Undefined to state " + factory.to + " for transition " + factory.trigger);
        }
        
        if (from == to && !factory.guard.isPresent()) {
          throw new IllegalStateException("Unguarded self transitions will cause an infinite loop in state " + factory.to);
        }
        
        realTransitions.add(new TriggeredTransition<>(factory.trigger, from, to, factory.guard, factory.action));
      });
      return new DeepStateFsm<>(realStates.get(initialState), realTransitions, uncaughtExceptionHandler);
    }
  }
  
  public static class StateFactory<T, U> {
    private final FsmFactory<T, U> fsm;
    private Optional<Action<U>> entryAction;
    private Optional<Action<U>> exitAction;
    private Map<T, StateAction<T, U>> actions;
    private Optional<StateAction<T, U>> defaultAction;
    private Optional<FsmFactory<T, U>> nestedStateMachine;
    
    private StateFactory(FsmFactory<T, U> fsm) {
      this.fsm = fsm;
      this.entryAction = Optional.empty();
      this.exitAction = Optional.empty();
      this.defaultAction = Optional.empty();
      actions = new HashMap<>();
      nestedStateMachine = Optional.empty();
    }
    
    public StateFactory<T, U> configure(Consumer<StateFactory<T, U>> factory) {
      factory.accept(this);
      return this;
    }
    
    public StateFactory<T, U> whenEntering(Action<U> entry) {
      this.entryAction = Optional.of(entry);
      return this;
    }
    
    public StateFactory<T, U> whenExiting(Action<U> exit) {
      this.exitAction = Optional.of(exit);
      return this;
    }
    
    public StateFactory<T, U> when(T trigger, StateAction<T, U> action) {
      actions.put(trigger, action);
      return this;
    }
    
    public StateFactory<T, U> whenNothingElseMatches(StateAction<T, U> defaultAction) {
      this.defaultAction = Optional.of(defaultAction);
      return this;
    }
    
    public FsmFactory<T, U> nest() {
      nestedStateMachine = Optional.of(new FsmFactory<>(Optional.of(this)));
      return nestedStateMachine.get();
    }

    public FsmFactory<T, U> and() {
      return fsm;
    }
    
    private SimpleState<T, U> create(U name, Optional<Consumer<Exception>> uncaughtExceptionHandler) {
      if (nestedStateMachine.isPresent()) {
        return new CompositeState<>(name, entryAction, exitAction, actions, defaultAction, uncaughtExceptionHandler, nestedStateMachine.get().create());
      }
      return new SimpleState<>(name, entryAction, exitAction, actions, defaultAction, uncaughtExceptionHandler);
    }
  }
  
  public static class TransitionFactory<T, U> {
    private final FsmFactory<T, U> fsm;
    private final T trigger;
    
    private U from;
    private U to;
    private Optional<Guard<T>> guard;
    private Optional<TransitionAction<T, U>> action;

    private TransitionFactory(FsmFactory<T, U> fsm, T trigger) {
      this.fsm = fsm;
      this.trigger = trigger;
      guard = Optional.empty();
      action = Optional.empty();
    }
    
    public TransitionFactory<T, U> configure(Consumer<TransitionFactory<T, U>> factory) {
      factory.accept(this);
      return this;
    }

    public TransitionFactory<T, U> from(U state) {
      this.from = state;
      return this;
    }
    
    public TransitionFactory<T, U> to(U state) {
      this.to = state;
      return this;
    }

    public TransitionFactory<T, U> guardedBy(Guard<T> guard) {
      this.guard = Optional.of(guard);
      return this;
    }
    
    public TransitionFactory<T, U> invoke(TransitionAction<T, U> action) {
      this.action = Optional.of(action);
      return this;
    }
    
    public FsmFactory<T, U> and() {
      if (from == null || to == null) {
        throw new IllegalStateException("Transition endpoints must be set");
      }
      return fsm;
    }
  }
}
