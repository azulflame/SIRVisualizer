/*
 * Created by Todd on 11/13/2020.
 */
package me.toddbensmiller.sirvisual

enum class SIRState
{
    SUSCEPTIBLE,
    INFECTED_TRANSITION,
    INFECTED,
    REMOVED_TRANSITION,
    REMOVED,
    SUSCEPTIBLE_TRANSITION,
    INFECTED_DECAY_ONLY,
    BAD_STATE
}
