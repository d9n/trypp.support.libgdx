package trypp.support.ligbdx.physics.collision

import trypp.support.ligbdx.physics.Body
import trypp.support.ligbdx.physics.World

/**
 * Interface for a class that should handle a collision between two bodies.
 *
 * Register your collision handlers via [World.addCollisionHandler].
 */
interface CollisionHandler {
    fun allowPassThru(bodyA: Body, bodyB: Body): Boolean
    fun onCollided(bodyA: Body, bodyB: Body)
    fun onOverlapping(bodyA: Body, bodyB: Body)
    fun onSeparated(bodyA: Body, bodyB: Body)
}
