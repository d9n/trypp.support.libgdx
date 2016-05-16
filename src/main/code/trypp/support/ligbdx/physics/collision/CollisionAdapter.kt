package trypp.support.ligbdx.physics.collision

import trypp.support.ligbdx.physics.Body

/**
 * Default implementation of the [CollisionHandler] interface.
 */
abstract class CollisionAdapter : CollisionHandler {
    override fun allowPassThru(bodyA: Body, bodyB: Body): Boolean {
        return true
    }

    override fun onCollided(bodyA: Body, bodyB: Body) {
    }

    override fun onOverlapping(bodyA: Body, bodyB: Body) {
    }

    override fun onSeparated(bodyA: Body, bodyB: Body) {
    }
}
