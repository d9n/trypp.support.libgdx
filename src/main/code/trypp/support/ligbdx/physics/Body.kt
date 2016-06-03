package trypp.support.ligbdx.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.Shape
import trypp.support.memory.Poolable
import com.badlogic.gdx.physics.box2d.Body as B2Body

/**
 * Create a body
 */
class Body : Poolable {
    var world: World? = null
        private set

    var category = 0
        private set

    val mass: Float
        get() = b2body!!.mass

    internal var b2body: B2Body? = null
        private set

    override fun reset() {
        world!!.b2World.destroyBody(b2body)

        world = null
        b2body = null
    }

    companion object {
        private val bodyDef = BodyDef()
        private val fixtureDef = FixtureDef()
    }

    var linearVelocity: Vector2
        get() = b2body!!.linearVelocity
        set(value) {
            b2body!!.linearVelocity = value
        }

    /**
     * Damping which reduces the linear velocity each step. 0 means no damping, 1+ should start to
     * reduce velocity more quickly. This value may require hand tuning to find what feels best.
     */
    var linearDamping: Float
        get() = b2body!!.linearDamping
        set(value) {
            b2body!!.linearDamping = value
        }

    val position: Vector2
        get() = b2body!!.worldCenter

    fun set(world: World,
            category: Int,
            collidesWith: Int,
            shape: Shape,
            type: BodyType,
            pos: Vector2?) {
        this.world = world
        this.category = category

        bodyDef.type = type
        if (pos != null) {
            bodyDef.position.set(pos)
        }
        else {
            bodyDef.position.setZero()
        }
        fixtureDef.shape = shape
        fixtureDef.friction = 0f
        fixtureDef.filter.categoryBits = category.toShort()
        fixtureDef.filter.maskBits = collidesWith.toShort()

        b2body = world.b2World.createBody(bodyDef)
        b2body!!.createFixture(fixtureDef)
    }

    fun setLinearVelocity(vX: Float, vY: Float) {
        b2body!!.setLinearVelocity(vX, vY)
    }

    fun applyForce(vX: Float, vY: Float) {
        b2body!!.applyForceToCenter(vX, vY, true);
    }

    fun applyImpluse(vX: Float, vY: Float) {
        val worldCenter = b2body!!.worldCenter
        b2body!!.applyLinearImpulse(vX, vY, worldCenter.x, worldCenter.y, true)
    }

    override fun toString(): String {
        return "Body { pos: $position, vel: $linearVelocity }"
    }
}