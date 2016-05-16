package trypp.support.ligbdx.physics.collision

import com.badlogic.gdx.physics.box2d.Body
import trypp.support.memory.Poolable

internal class CollisionData : Poolable {
    // Order matters with callbacks! Users expect one body to appear first and another
    // to appear second (depending on how they registered their callback)
    var bodyFirst: Body? = null
    var bodySecond: Body? = null
    var collisionHandler: CollisionHandler? = null

    override fun reset() {
        bodyFirst = null
        bodySecond = null
        collisionHandler = null
    }
}