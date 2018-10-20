package metatype.deepstate.kt.core.builder

import metatype.deepstate.kt.core.State
import metatype.deepstate.kt.core.StateMachine

class StateMachineBuilder : Builder<StateMachine> {
    lateinit var name: String
    lateinit var states: Set<StateBuilder<out State>>
    lateinit var transitions: Set<TransitionBuilder>
    lateinit var initialState: StateBuilder<out State>

    private val statesMap: Map<String, State> by lazy {
        states.map { stateBuilder ->
            val state = stateBuilder.build(this)
            state.name to state
        }.toMap()
    }

    override fun build(builder: Builder<*>): StateMachine {
        validateLateInitVars(this)
        val transitionsMap = transitions.map { transitionBuilder ->
            val transition = transitionBuilder.build(this)
            transition.trigger to transition
        }.toMap()

        val initialState = findStateForName(initialState.name)
        return StateMachine(name, statesMap, transitionsMap, initialState)
    }

    private fun validateLateInitVars(builder: StateMachineBuilder) {
        if (!this::name.isInitialized) {
            throw IllegalArgumentException("State, within statemachine ${builder.name} does not contain valid name")
        }
        if (!this::states.isInitialized) {
            throw IllegalArgumentException("No states defined for statemachine ${builder.name}")
        }
        if (!this::transitions.isInitialized) {
            throw IllegalArgumentException("No transitions defined for statemachine ${builder.name}")
        }
        if (!this::initialState.isInitialized) {
            throw IllegalArgumentException("No initial state defined for statemachine ${builder.name}")
        }
    }

    fun findStateForName(stateName: String): State =
            statesMap[stateName] ?: throw IllegalArgumentException("No state for name $stateName exists")

}