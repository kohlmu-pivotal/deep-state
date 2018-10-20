package metatype.deepstate.kt.core.builder

import metatype.deepstate.kt.core.Event
import metatype.deepstate.kt.core.Transition
import metatype.deepstate.kt.dsl.StateMachineDSL

@StateMachineDSL
class TransitionBuilder : Builder<Transition> {
    lateinit var fromState: String
    lateinit var toState: String
    lateinit var event: String
    lateinit var handler: () -> Unit

    override fun build(builder: Builder<*>): Transition =
            if (builder is StateMachineBuilder) {
                validateLateInitVars(builder)
                val fromState = builder.findStateForName(fromState)
                val toState = builder.findStateForName(toState)
                Transition(fromState, toState, Event(event), handler)

            } else {
                throw IllegalStateException("Transition for $this could not be created")
            }

    private fun validateLateInitVars(builder: StateMachineBuilder) {
        if (!this::fromState.isInitialized) {
            throw IllegalArgumentException("No fromState defined for transition in statemachine ${builder.name}")
        }
        if (!this::toState.isInitialized) {
            throw IllegalArgumentException("No toState defined for transition in statemachine ${builder.name}")
        }
        if (!this::event.isInitialized) {
            throw IllegalArgumentException("No event defined for transition in statemachine ${builder.name}")
        }
        if (!this::handler.isInitialized) {
            handler = {}
        }
    }
}