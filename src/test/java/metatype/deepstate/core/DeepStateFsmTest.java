package metatype.deepstate.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import metatype.deepstate.DeepState;
import metatype.deepstate.FiniteStateMachine;
import metatype.deepstate.FiniteStateMachine.Action;
import metatype.deepstate.FiniteStateMachine.Event;
import metatype.deepstate.FiniteStateMachine.StateAction;
import metatype.deepstate.FiniteStateMachine.TransitionAction;

public class DeepStateFsmTest {
  public class TestEvent implements Event<String> {
    private final String trigger;
    private final boolean data;
    
    public TestEvent(String trigger, boolean data) {
      this.trigger = trigger;
      this.data = data;
    }
    
    public TestEvent(String trigger) {
      this(trigger, false);
    }

    @Override
    public String getTrigger() {
      return trigger;
    }

    public boolean get() {
      return data;
    }
  }
  
  @Test
  public void testInitialState() {
    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("Initial")
        .and().ready();
    
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Initial");
  }

  @Test
  public void testOnInitialEntry() {
    Action<String> entry = mock(Action.class);
    
    DeepState.<String, String>model()
        .startingWith("initial")
        .whenEntering(entry)
        .and().ready();

    verify(entry, times(1)).accept(any());
  }
  
  @Test
  public void testStateAction() {
    StateAction<String, String> action = mock(StateAction.class);

    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("initial")
        .when("test", action)
        .and().ready();
    
    fsm.accept(new TestEvent("test"));
    verify(action, times(1)).accept(any(), any());
  }

  @Test
  public void testStateActionIsNotFired() {
    StateAction<String, String> action = mock(StateAction.class);

    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("initial")
        .when("test", action)
        .and().ready();
    
    fsm.accept(new TestEvent("test2"));
    verify(action, times(0)).accept(any(), any());
  }
  
  @Test
  public void testDefaultAction() {
    StateAction<String, String> action = mock(StateAction.class);

    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("initial")
        .whenNothingElseMatches(action)
        .and().ready();
    
    fsm.accept(new TestEvent("test"));
    verify(action, times(1)).accept(any(), any());
  }

  @Test
  public void testTransition() {
    Action<String> entry = mock(Action.class);
    Action<String> exit = mock(Action.class);
    TransitionAction<String, String> guard = mock(TransitionAction.class);
    
    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("Initial")
        .whenExiting(exit)
        .and().define("Next")
        .whenEntering(entry)
        .and().transition("go").from("Initial").to("Next")
        .invoke(guard)
        .and().ready();

    fsm.accept(new TestEvent("go"));

    verify(entry, times(1)).accept(any());
    verify(exit, times(1)).accept(any());
    verify(guard, times(1)).accept(any(), any());
    
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Next");
  }

  @Test
  public void testTransitionWithGuard() {
    Action<String> entry = mock(Action.class);
    Action<String> exit = mock(Action.class);
    TransitionAction<String, String> guard = mock(TransitionAction.class);

    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("Initial")
        .whenExiting(exit)
        .and().define("Next")
        .whenEntering(entry)
        .and().transition("go").from("Initial").to("Next")
        .invoke(guard)
        .guardedBy((event) -> (Boolean) ((TestEvent) event).get())
        .and().ready();

    
    fsm.accept(new TestEvent("go", false));
    
    verify(entry, times(0)).accept(any());
    verify(exit, times(0)).accept(any());
    verify(guard, times(0)).accept(any(), any());
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Initial");

    fsm.accept(new TestEvent("go", true));
    verify(entry, times(1)).accept(any());
    verify(exit, times(1)).accept(any());
    verify(guard, times(1)).accept(any(), any());
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Next");
  }
  
  @Test
  public void testMultipleTransitions() {
    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("Initial")
        .and().define("Next")
        .and().define("Other")
        .and().transition("goto next").from("Initial").to("Next")
        .and().transition("goto other").from("Initial").to("Other")
        .and().ready();
    
    fsm.accept(new TestEvent("goto next"));
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Next");
  }
  
  @Test
  public void testSelfTransition() {
    AtomicBoolean guard = new AtomicBoolean(true);
    
    Action<String> entry = mock(Action.class);
    Action<String> exit = mock(Action.class);

    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("Initial")
        .whenEntering(entry)
        .whenExiting(exit)
        .and().transition("goto self").from("Initial").to("Initial")
        .guardedBy((event) -> guard.get())
        .invoke((transition, event) -> guard.set(false))
        .and().ready();

    fsm.accept(new TestEvent("goto self"));
    
    verify(entry, times(2)).accept(any());
    verify(exit, times(1)).accept(any());
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Initial");
  }

  @Test
  public void testEventThatCreatesEvent() {
    AtomicReference<FiniteStateMachine<String, String>> fsm = new AtomicReference<>();

    fsm.set(DeepState.<String, String>model()
        .startingWith("Initial")
        .and().define("Next")
        .and().define("Last")
        .and().transition("goto next").from("Initial").to("Next")
        .invoke((transition, event) -> fsm.get().accept(new TestEvent("goto last")))
        .and().transition("goto last").from("Next").to("Last")
        .and().ready());

    fsm.get().accept(new TestEvent("goto next", false));
    assertThat(fsm.get().getCurrentState().getName()).isEqualTo("Last");
  }
  
  @Test
  public void testNoInitialState() {
    assertThatThrownBy(() -> { DeepState.<String, String>model()
        .define("Next")
        .and().ready();
    }).isInstanceOf(IllegalStateException.class);
  }
  
  @Test
  public void testBadToTransition() {
    assertThatThrownBy(() -> { DeepState.<String, String>model()
        .startingWith("here")
        .and().transition("whatever").from("here").to("eternity")
        .and().ready();
    }).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testBadFromTransition() {
    assertThatThrownBy(() -> { DeepState.<String, String>model()
        .startingWith("eternity")
        .and().transition("whatever").from("here").to("eternity")
        .and().ready();
    }).isInstanceOf(IllegalStateException.class);
  }
  
  @Test
  public void testNestedCurrentState() {
    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("Initial")
        .nest().startingWith("Initial.Inside")
        .and().parent()
        .and().ready();
    
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Initial");
    assertThat(fsm.getCurrentStates().getFirst().getName()).isEqualTo("Initial");
    assertThat(fsm.getCurrentStates().getLast().getName()).isEqualTo("Initial.Inside");
    assertThat(fsm.getCurrentStates().size()).isEqualTo(2);
    assertThat(fsm.getCurrentStates().getLast().getName()).isEqualTo("Initial.Inside");
  }
  
  @Test
  public void testNestedEvent() {
    StateAction<String, String> action = mock(StateAction.class);
    StateAction<String, String> innerAction = mock(StateAction.class);
    
    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("initial")
        .when("test", action)
        .nest().startingWith("inner")
        .when("test", innerAction)
        .and().parent()
        .and().ready();
    
    fsm.accept(new TestEvent("test"));
    verify(action, times(1)).accept(any(), any());
    verify(innerAction, times(1)).accept(any(), any());
  }
  
  @Test
  public void testNestedTransition() {
    StateAction<String, String> action = mock(StateAction.class);
    TransitionAction<String, String> transitionAction = mock(TransitionAction.class);

    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("initial")
        .when("test", action)
        .nest().startingWith("first")
        .and().define("second")
        .and().transition("test").from("first").to("second")
        .invoke(transitionAction)
        .and().parent()
        .and().ready();
    
    fsm.accept(new TestEvent("test"));
    verify(action, times(1)).accept(any(), any());
    verify(transitionAction, times(1)).accept(any(), any());
    assertThat(fsm.getCurrentStates().getLast().getName()).isEqualTo("second");
  }
  
  @Test
  public void testNestedTransitionOfParentState() {
    Action<String> action = mock(Action.class);
    FiniteStateMachine<String, String> fsm = DeepState.<String, String>model()
        .startingWith("initial")
        .nest().startingWith("first")
        .and().define("second")
        .whenExiting(action)
        .and().transition("test").from("first").to("second")
        .and().parent()
        .and().define("last")
        .and().transition("test").from("initial").to("last")
        .and().ready();
    
    fsm.accept(new TestEvent("test"));
    verify(action, times(1)).accept(any());
    assertThat(fsm.getCurrentState().getName()).isEqualTo("last");
  }
  
  @Test
  public void testReplay() {
    StateAction<String, String> action1 = mock(StateAction.class);
    StateAction<String, String> action2 = mock(StateAction.class);
    List<Event<String>> capturedEvents = new ArrayList<>();
    
    DeepStateFsm<String, String> fsm = DeepState.<String, String>model()
        .audit(capturedEvents::add)
        .startingWith("A")
        .and().define("B")
        .when("B.action1", action1)
        .and().define("C")
        .when("C.action2", action2)
        .and().transition("goto B").from("A").to("B")
        .and().transition("goto C").from("B").to("C")
        .and().ready();
    
    List<Event<String>> events = Arrays.asList(
        new TestEvent("goto B"),
        new TestEvent("B.action1"),
        new TestEvent("goto C"),
        new TestEvent("C.action2"));
    
    events.forEach(fsm::accept);
    
    assertThat(fsm.getCurrentState().getName()).isEqualTo("C");
    verify(action1, times(1)).accept(any(), any());
    verify(action2, times(1)).accept(any(), any());
    assertThat(capturedEvents).isEqualTo(events);
    
    fsm.begin();
    new ArrayList<>(capturedEvents).forEach(fsm::accept);
    assertThat(fsm.getCurrentState().getName()).isEqualTo("C");
  }
}
