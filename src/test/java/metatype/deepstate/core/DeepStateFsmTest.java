package metatype.deepstate.core;

import static metatype.deepstate.DeepState.model;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    FiniteStateMachine<String> fsm = DeepState.<String>model("test")
        .startingWith("Initial")
        .and().ready();
    
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Initial");
  }

  @Test
  public void testOnInitialEntry() {
    Action entry = mock(Action.class);
    
    model("test")
        .startingWith("initial")
        .whenEntering(entry)
        .and().ready();

    verify(entry, times(1)).accept(any());
  }
  
  @Test
  public void testStateAction() {
    StateAction<String> action = mock(StateAction.class);

    FiniteStateMachine<String> fsm = DeepState.<String>model("test")
        .startingWith("initial")
        .when("test", action)
        .and().ready();
    
    fsm.accept(new TestEvent("test"));
    verify(action, times(1)).accept(any(), any());
  }

  @Test
  public void testStateActionIsNotFired() {
    StateAction<String> action = mock(StateAction.class);

    FiniteStateMachine<String> fsm = DeepState.<String>model("test")
        .startingWith("initial")
        .when("test", action)
        .and().ready();
    
    fsm.accept(new TestEvent("test2"));
    verify(action, times(0)).accept(any(), any());
  }
  
  @Test
  public void testTransition() {
    Action entry = mock(Action.class);
    Action exit = mock(Action.class);
    TransitionAction<String> guard = mock(TransitionAction.class);
    
    FiniteStateMachine<String> fsm = DeepState.<String>model("test")
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
    Action entry = mock(Action.class);
    Action exit = mock(Action.class);
    TransitionAction<String> guard = mock(TransitionAction.class);

    FiniteStateMachine<String> fsm = DeepState.<String>model("test")
        .startingWith("Initial")
        .whenExiting(exit)
        .and().define("Next")
        .whenEntering(entry)
        .and().transition("go").from("Initial").to("Next")
        .invoke(guard)
        .guard((event) -> (Boolean) ((TestEvent) event).get())
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
    FiniteStateMachine<String> fsm = DeepState.<String>model("test")
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
    Action entry = mock(Action.class);
    Action exit = mock(Action.class);

    FiniteStateMachine<String> fsm = DeepState.<String>model("test")
        .startingWith("Initial")
        .whenEntering(entry)
        .whenExiting(exit)
        .and().transition("goto self").from("Initial").to("Initial")
        .and().ready();

    fsm.accept(new TestEvent("goto self"));
    
    verify(entry, times(2)).accept(any());
    verify(exit, times(1)).accept(any());
    assertThat(fsm.getCurrentState().getName()).isEqualTo("Initial");
  }

  @Test
  public void testEventThatCreatesEvent() {
    AtomicReference<FiniteStateMachine<String>> fsm = new AtomicReference<>();

    fsm.set(DeepState.<String>model("test")
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
    assertThatThrownBy(() -> { DeepState.<String>model("test")
        .define("Next")
        .and().ready();
    }).isInstanceOf(IllegalStateException.class);
  }
  
  @Test
  public void testBadToTransition() {
    assertThatThrownBy(() -> { DeepState.<String>model("test")
        .startingWith("here")
        .and().transition("whatever").from("here").to("eternity")
        .and().ready();
    }).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testBadFromTransition() {
    assertThatThrownBy(() -> { DeepState.<String>model("test")
        .startingWith("eternity")
        .and().transition("whatever").from("here").to("eternity")
        .and().ready();
    }).isInstanceOf(IllegalStateException.class);
  }
}
