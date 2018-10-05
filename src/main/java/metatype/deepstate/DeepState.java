package metatype.deepstate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import metatype.deepstate.FiniteStateMachine.Action;
import metatype.deepstate.FiniteStateMachine.Event;
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
    
    private Consumer<Exception> uncaughtExceptionHandler;
    private StateFactory<T, U> parent;
    private Consumer<Event<T>> auditor;
    
    private FsmFactory() {
      this(null);
    }
    
    private FsmFactory(StateFactory<T, U> parentState) {
      this.states = new HashMap<>();
      this.transitions = new HashSet<>();
      this.parent = parentState;
    }

    public FsmFactory<T, U> configure(Consumer<FsmFactory<T, U>> factory) {
      Objects.requireNonNull(factory, "factory must not be null");
      factory.accept(this);
      return this;
    }

    public StateFactory<T, U> startingWith(U state) {
      Objects.requireNonNull(state, "state must not be null");
      initialState = state;
      return define(state);
    }

    public StateFactory<T, U> define(U state) {
      Objects.requireNonNull(state, "state must not be null");
      if (states.containsKey(state)) {
        throw new IllegalStateException("Unable to redefine existing state " + state);
      }
      
      StateFactory<T, U> factory = new StateFactory<>(this);
      states.put(state, factory);
      return factory;
    }
    
    public FsmFactory<T, U> catchExceptionsUsing(Consumer<Exception> exceptionHandler) {
      Objects.requireNonNull(exceptionHandler, "exception handler must not be null");
      uncaughtExceptionHandler = exceptionHandler;
      return this;
    }
    
    public FsmFactory<T, U> audit(Consumer<Event<T>> auditor) {
      Objects.requireNonNull(auditor, "auditor must not be null");
      this.auditor = auditor;
      return this;
    }
    
    public TransitionFactory<T, U> transition(T trigger) {
      Objects.requireNonNull(trigger, "trigger must not be null");
      TransitionFactory<T, U> factory = new TransitionFactory<>(this, trigger);
      transitions.add(factory);
      return factory;
    }
    
    public StateFactory<T, U> parent() {
      Objects.requireNonNull(parent, "Parent state is not defined");
      return parent;
    }
    
    public DeepStateFsm<T, U> ready() {
      return create().begin();
    }
    
    private DeepStateFsm<T, U> create() {
      Objects.requireNonNull(initialState, "initial state must not be null");
      
      Map<U, SimpleState<T, U>> realStates = new HashMap<>();
      states.forEach((name, factory) -> {
        realStates.put(name, factory.create(name, uncaughtExceptionHandler));
      });
      
      Set<TriggeredTransition<T, U>> realTransitions = new HashSet<>();
      transitions.forEach((factory) -> {
        SimpleState<T, U> from = realStates.get(factory.from);
        Objects.requireNonNull(from, "Undefined from state " + factory.from + " for transition " + factory.trigger);
        
        SimpleState<T, U> to = realStates.get(factory.to);
        Objects.requireNonNull(to, "Undefined to state " + factory.to + " for transition " + factory.trigger);
        
        if (from == to && factory.guard == null) {
          throw new IllegalStateException("Unguarded self transitions will cause an infinite loop in state " + factory.to);
        }
        
        realTransitions.add(new TriggeredTransition<>(factory.trigger, from, to, factory.guard, factory.action));
      });
      return new DeepStateFsm<>(realStates.get(initialState), realTransitions, uncaughtExceptionHandler, auditor);
    }
  }
  
  public static class StateFactory<T, U> {
    private final FsmFactory<T, U> fsm;
    private Action<U> entryAction;
    private Action<U> exitAction;
    private Map<T, StateAction<T, U>> actions;
    private StateAction<T, U> defaultAction;
    private FsmFactory<T, U> nestedStateMachine;
    
    private StateFactory(FsmFactory<T, U> fsm) {
      this.fsm = fsm;
      actions = new HashMap<>();
    }
    
    public StateFactory<T, U> configure(Consumer<StateFactory<T, U>> factory) {
      factory.accept(this);
      return this;
    }
    
    public StateFactory<T, U> whenEntering(Action<U> entry) {
      this.entryAction = entry;
      return this;
    }
    
    public StateFactory<T, U> whenExiting(Action<U> exit) {
      this.exitAction = exit;
      return this;
    }
    
    public StateFactory<T, U> when(T trigger, StateAction<T, U> action) {
      actions.put(trigger, action);
      return this;
    }
    
    public StateFactory<T, U> whenNothingElseMatches(StateAction<T, U> defaultAction) {
      this.defaultAction = defaultAction;
      return this;
    }
    
    public FsmFactory<T, U> nest() {
      nestedStateMachine = new FsmFactory<>(this);
      return nestedStateMachine;
    }

    public FsmFactory<T, U> and() {
      return fsm;
    }
    
    private SimpleState<T, U> create(U name, Consumer<Exception> uncaughtExceptionHandler) {
      if (nestedStateMachine == null) {
        return new SimpleState<>(name, entryAction, exitAction, actions, defaultAction, uncaughtExceptionHandler);
      }
      return new CompositeState<>(name, entryAction, exitAction, actions, defaultAction, uncaughtExceptionHandler, nestedStateMachine.create());
    }
  }
  
  public static class TransitionFactory<T, U> {
    private final FsmFactory<T, U> fsm;
    private final T trigger;
    
    private U from;
    private U to;
    private Guard<T> guard;
    private TransitionAction<T, U> action;

    private TransitionFactory(FsmFactory<T, U> fsm, T trigger) {
      this.fsm = fsm;
      this.trigger = trigger;
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
      this.guard = guard;
      return this;
    }
    
    public TransitionFactory<T, U> invoke(TransitionAction<T, U> action) {
      this.action = action;
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
