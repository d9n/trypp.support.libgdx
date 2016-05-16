package trypp.support.ligbdx.physics.collision

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Fixture
import trypp.support.memory.Poolable

internal class Collision : Poolable {
    var fixtureA: Fixture? = null
    var fixtureB: Fixture? = null
    var justCollided = true

    override fun reset() {
        fixtureA = null
        fixtureB = null
        justCollided = true
    }

    fun matches(fixtureC: Fixture, fixtureD: Fixture): Boolean {
        return fixtureA === fixtureC && fixtureB === fixtureD ||
            fixtureA === fixtureD && fixtureB === fixtureC
    }

    fun ownsBody(body: Body): Boolean {
        return fixtureA?.body === body || fixtureB?.body === body
    }
}