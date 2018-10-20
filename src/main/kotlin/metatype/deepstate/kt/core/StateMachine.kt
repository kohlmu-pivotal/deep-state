package metatype.deepstate.kt.core


class StateMachine(private val name: String, private val states: Map<String, State>,
                   private val transitions: Map<Event, Transition>, private val initialState: State) {
    private var currentState: State = initialState
        set(value) {
            if (currentState === value) {
                return
            }

            currentState.onEnter.invoke()
            field = value
            currentState.onExit.invoke()
        }

    init {
        currentState = initialState
        initialState.onEnter.invoke()
    }

//    fun addTriggerHandler(triggerHandler: TriggerHandler) {
//        transitions.put(triggerHandler.event, triggerHandler)?.run {
//            throw IllegalArgumentException("TriggerHandler ${triggerHandler.state} -> ${triggerHandler.event} is already added")
//        }
//    }

    fun processEvent(event: Event) {
        transitions[event]?.run {
            if (this.fromState === currentState) {
                this.handler.invoke()
            }
        } ?: throw IllegalStateException("No event exists for ${event.name}")
    }
}