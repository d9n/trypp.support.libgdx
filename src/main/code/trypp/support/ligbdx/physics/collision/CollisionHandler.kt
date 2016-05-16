package trypp.support.ligbdx.physics.collision

import trypp.support.ligbdx.physics.Body

/**
 * Interface for a class that should handle a collision between two bodies.
 *
 * Register your collision handlers via [PhysicsSystem.addCollisionHandler].
 */
interface CollisionHandler {
    fun allowPassThru(bodyA: Body, bodyB: Body): Boolean
    fun onCollided(bodyA: Body, bodyB: Body)
    fun onOverlapping(bodyA: Body, bodyB: Body)
    fun onSeparated(bodyA: Body, bodyB: Body)
}
