package metatype.deepstate.kt.core

data class TriggerHandler(val fromState: State,
                          val toState: State,
                          val trigger: Event,
                          val handler: () -> Unit) {

    private val _hashCode: Int by lazy { 31 * fromState.hashCode() * toState.hashCode() * trigger.hashCode() }

    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean {
        return if (other is TriggerHandler) {
            fromState === other.fromState && toState === other.toState && trigger == other.trigger
        } else {
            false
        }
    }
}