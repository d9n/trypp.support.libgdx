package trypp.support.ligbdx.physics

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.DynamicBody
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Shape
import com.badlogic.gdx.utils.Array
import trypp.support.collections.ArrayMap
import trypp.support.collections.ArraySet
import trypp.support.ligbdx.physics.collision.Collision
import trypp.support.ligbdx.physics.collision.CollisionCallback
import trypp.support.ligbdx.physics.collision.CollisionData
import trypp.support.ligbdx.physics.collision.CollisionHandler
import trypp.support.ligbdx.physics.collision.CollisionHandlerEntry
import trypp.support.math.BitUtils
import trypp.support.memory.HeapPool
import trypp.support.memory.Pool
import trypp.support.time.Duration
import com.badlogic.gdx.physics.box2d.Body as B2Body
import com.badlogic.gdx.physics.box2d.World as B2World


/**
 * A simple wrapper around a Box2D world. This is the class which will let the user generate
 * [Body] instances, as well as maintains global state.
 */
class World private constructor(gravity: Vector2,
                                expectedBodyCount: Int,
                                stepsPerSec: Int,
                                val collidesWith: IntArray) {
    class Builder(var gravity: Vector2 = Vector2(0f, 0f),
                  var expectedBodyCount: Int = DEFAULT_BODY_COUNT,
                  var stepsPerSec: Int = DEFAULT_STEPS_PER_SEC) {
        private var collidesWith = IntArray(MAX_NUM_CATEGORIES)

        /**
         * Register two category groups as collidable. Unless set, contact between these two collision
         * groups won't trigger a collision.
         */
        fun registerCollidable(categoriesA: Int, categoriesB: Int): Builder {
            for (i in 0 until MAX_NUM_CATEGORIES) {
                val bitMask = 1 shl i
                if (categoriesA and bitMask != 0) {
                    collidesWith[i] = (collidesWith[i] or categoriesB)
                }
                if (categoriesB and bitMask != 0) {
                    collidesWith[i] = (collidesWith[i] or categoriesA)
                }
            }

            return this
        }

        fun build(): World {
            return World(gravity, expectedBodyCount, stepsPerSec, collidesWith)
        }
    }

    companion object {
        private val DEFAULT_BODY_COUNT = 1000
        private val DEFAULT_STEPS_PER_SEC = 60
        private val EXPECTED_COLLISION_HANDLER_COUNT = 30
        private val EXPECTED_NO_CONTACT_BODY_COUNT = 5
        // Recommended values from Box2D manual
        private val VELOCITY_ITERATIONS = 6
        private val POSITION_ITERATIONS = 2

        /**
         * Box2D has a hard limit of 16 collision categories.
         */
        var MAX_NUM_CATEGORIES = 16

        init {
            Box2D.init()
        }
    }

    private inner class CollisionListener : ContactListener {
        override fun beginContact(contact: Contact) {
            if (!contact.isTouching) {
                return
            }

            if (hasCollisionHandlers(contact)) {
                val activeCollision = collisionPool.grabNew()
                activeCollision.fixtureA = contact.fixtureA
                activeCollision.fixtureB = contact.fixtureB
            }
        }

        override fun endContact(contact: Contact) {
            passThruResult.removeIf(contact)
            if (hasCollisionHandlers(contact)) {
                val fixtureA = contact.fixtureA
                val fixtureB = contact.fixtureB
                removeActiveCollision(fixtureA, fixtureB)
            }
        }

        override fun preSolve(contact: Contact, oldManifold: Manifold) {
            if (skippedBodies.contains(contact.fixtureA.body) ||
                skippedBodies.contains(contact.fixtureB.body)) {
                contact.isEnabled = false
                return
            }

            var passThru = passThruResult.getIf(contact)
            if (passThru == null) {
                passThru = allowPassThru(contact)
                passThruResult.put(contact, passThru)
            }

            contact.isEnabled = !passThru
        }

        override fun postSolve(contact: Contact, impulse: ContactImpulse) {
        }
    }

    private val onCollidedDispatcher = object : CollisionCallback {
        override fun run(data: CollisionData) {
            val bodyFirst = bodyMap[data.bodyFirst!!]
            val bodySecond = bodyMap[data.bodySecond!!]

            data.collisionHandler!!.onCollided(bodyFirst, bodySecond)
        }
    }
    private val onOverlappingDispatcher = object : CollisionCallback {
        override fun run(data: CollisionData) {
            val bodyFirst = bodyMap[data.bodyFirst!!]
            val bodySecond = bodyMap[data.bodySecond!!]

            data.collisionHandler!!.onOverlapping(bodyFirst, bodySecond)
        }
    }
    private val onSeparatedDispatcher = object : CollisionCallback {
        override fun run(data: CollisionData) {
            val bodyFirst = bodyMap[data.bodyFirst!!]
            val bodySecond = bodyMap[data.bodySecond!!]

            data.collisionHandler!!.onSeparated(bodyFirst, bodySecond)
        }
    }

    private inner class DebugRender() {
        val renderer = Box2DDebugRenderer()
        val matrix = Matrix4()

        fun render(cameraMatrix: Matrix4, metersToPixels: Float) {
            matrix.set(cameraMatrix).scl(metersToPixels)
            renderer.render(b2World, matrix)
        }
    }

    val bodies: List<Body>
        get() = bodyPool.itemsInUse

    internal val b2World = B2World(gravity, true)

    private val bodyPool = HeapPool.of(Body::class, expectedBodyCount)
    private val bodyMap = ArrayMap<B2Body, Body>(expectedBodyCount)

    private val collisionPool = HeapPool.of(Collision::class, expectedBodyCount / 10)
    // Usually we only need 1 collision data item, but occasionally runCollisionHandlers triggers a
    // callback which calls runCollisionHandlers again recursively, but this should never go very deep.
    private val collisionDataPool = Pool.of(CollisionData::class, 4)

    private val collisionHandlers = Array<CollisionHandlerEntry>(EXPECTED_COLLISION_HANDLER_COUNT)

    /**
     * Body to body contacts that should skip collision logic
     */
    private val passThruResult = ArrayMap<Contact, Boolean>(EXPECTED_COLLISION_HANDLER_COUNT)

    /**
     * Bodies which should always skip collision logic
     */
    private val skippedBodies = ArraySet<B2Body>(EXPECTED_NO_CONTACT_BODY_COUNT)

    private val stepDuration = Duration.ofSeconds(1f / stepsPerSec)
    private val elapsedSoFar = Duration.zero()

    // Reusable shapes for creating bodies
    private var circleShape = CircleShape()
    private var polyShape = PolygonShape()

    private var debugRender: DebugRender? = null

    init {
        b2World.setContactListener(CollisionListener())
    }

    fun getCollidesWith(category: Int): Int {
        val bitIndex = BitUtils.getBitIndex(category)
        assert(bitIndex < MAX_NUM_CATEGORIES, { "Requesting mask for invalid category" })
        return collidesWith[bitIndex]
    }


    fun createBody(category: Int,
                   shape: Shape,
                   type: BodyType = DynamicBody,
                   pos: Vector2? = null): Body {
        BitUtils.requireSingleBit(category)

        val body = bodyPool.grabNew()
        body.set(this, category, getCollidesWith(category), shape, type, pos)
        bodyMap.put(body.b2body!!, body)
        return body
    }

    fun createCircleBody(category: Int,
                         radius: Float,
                         type: BodyType = BodyType.DynamicBody,
                         pos: Vector2? = null): Body {
        circleShape.radius = radius
        return createBody(category, circleShape, type, pos)
    }

    fun createBoxBody(category: Int,
                      w: Float, h: Float,
                      type: BodyType = BodyType.DynamicBody,
                      pos: Vector2? = null): Body {
        polyShape.setAsBox(w / 2f, h / 2f)
        return createBody(category, polyShape, type, pos)
    }

    fun createBoxBody(category: Int,
                      size: Vector2,
                      type: BodyType = BodyType.DynamicBody,
                      pos: Vector2? = null): Body {
        polyShape.setAsBox(size.x, size.y)
        return createBody(category, polyShape, type, pos)
    }

    fun destroyBody(body: Body) {
        val b2Body = body.b2body!! // Always set by createBody
        enableCollisions(body, true) // This forces active collisions to separate
        // setActive puts a body reference in inactiveBodies - remove it!
        skippedBodies.removeIf(b2Body)
        removeActiveCollisions(b2Body)
        bodyMap.remove(b2Body)
        bodyPool.free(body) // Calls b2World.destroyBody internally
    }

    /**
     * Completely disable (or re-enable) collisions for a specific body
     */
    fun enableCollisions(body: Body, enable: Boolean) {
        val b2body = body.b2body!!
        if (enable) {
            if (skippedBodies.removeIf(b2body)) {
                runCollisionHandlers(b2body, onCollidedDispatcher)
            }
        }
        else {
            if (skippedBodies.putIf(b2body)) {
                runCollisionHandlers(b2body, onSeparatedDispatcher)

                val collisions = collisionPool.itemsInUse
                collisions.forEach {
                    if (it.ownsBody(b2body)) {
                        // Reset 'justCollided', in case this collision is reactivated again
                        it.justCollided = true
                    }
                }
            }
        }
    }

    /**
     * Register a [CollisionHandler] with this physics system. Note this either a registered handler will handle
     * a collision OR Box2D will handle it, but not both. The category order that a handler is registered with will
     * be preserved when the handler is called.
     *
     *
     * You can register multiple handlers for the same collision, which is useful if you have a default behavior you
     * want to happen in multiple collision cases.
     */
    fun addCollisionHandler(categoriesA: Int, categoriesB: Int, handler: CollisionHandler) {
        for (i in 0 until MAX_NUM_CATEGORIES) {
            val bitMask = 1 shl i
            if (categoriesA and bitMask != 0 && collidesWith[i] and categoriesB == 0) {
                throw IllegalArgumentException(
                    "Attempting to add handler for categories that don't collide ($categoriesA and $categoriesB). Did you forget to call registerCollidable?")
            }
        }
        collisionHandlers.add(CollisionHandlerEntry(categoriesA, categoriesB, handler))
    }

    fun update(elapsedTime: Duration) {
        elapsedSoFar.add(elapsedTime)
        while (elapsedSoFar.getSeconds() > stepDuration.getSeconds()) {
            b2World.step(stepDuration.getSeconds(), VELOCITY_ITERATIONS, POSITION_ITERATIONS)
            elapsedSoFar.subtract(stepDuration)

            val activeCollisions = collisionPool.itemsInUse
            val numCollisions = activeCollisions.size
            for (i in 0 until numCollisions) {
                val activeCollision = activeCollisions[i]

                val fixtureA = activeCollision.fixtureA
                val fixtureB = activeCollision.fixtureB
                if (skippedBodies.contains(fixtureA!!.body) ||
                    skippedBodies.contains(fixtureB!!.body)) {
                    continue
                }

                runCollisionHandlers(fixtureA, fixtureB,
                                     if (activeCollision.justCollided) onCollidedDispatcher else onOverlappingDispatcher)
                activeCollision.justCollided = false
            }
        }
    }

    private fun removeActiveCollision(fixtureA: Fixture, fixtureB: Fixture) {
        val collisions = collisionPool.itemsInUse
        for (i in 0 until collisions.size) {
            val c = collisions[i]
            if (c.matches(fixtureA, fixtureB)) {
                collisionPool.free(c)
                break
            }
        }
    }

    private fun removeActiveCollisions(body: B2Body) {
        val collisions = collisionPool.itemsInUse
        var numCollisions = collisions.size
        var i = 0
        while (i < numCollisions) {
            val c = collisions[i]
            if (c.ownsBody(body)) {
                collisionPool.free(c)
                i--
                numCollisions--
            }
            i++
        }
    }

    private fun getCollisionHandler(contact: Contact): CollisionHandlerEntry? {
        val fixtureA = contact.fixtureA
        val fixtureB = contact.fixtureB

        for (i in 0 until collisionHandlers.size) {
            val entry = collisionHandlers[i]
            if (entry.matches(fixtureA.filterData.categoryBits.toInt(),
                              fixtureB.filterData.categoryBits.toInt())) {
                return entry
            }
        }

        return null
    }

    private fun hasCollisionHandlers(contact: Contact): Boolean {
        return getCollisionHandler(contact) != null
    }

    private fun allowPassThru(contact: Contact): Boolean {
        val handler = getCollisionHandler(contact) ?: return false

        if (handler.isFirstCategory(contact.fixtureA.filterData.categoryBits.toInt())) {
            val bodyFirst = bodyMap[contact.fixtureA.body]
            val bodySecond = bodyMap[contact.fixtureB.body]
            return handler.collisionHandler.allowPassThru(bodyFirst, bodySecond)
        }
        else {
            val bodyFirst = bodyMap[contact.fixtureB.body]
            val bodySecond = bodyMap[contact.fixtureA.body]
            return handler.collisionHandler.allowPassThru(bodyFirst, bodySecond)

        }
    }

    /**
     * Given two fixtures that are colliding, call any collision handlers that may have been registered to handle it.
     */
    private fun runCollisionHandlers(fixtureA: Fixture, fixtureB: Fixture,
                                     collisionCallback: CollisionCallback) {

        for (i in 0 until collisionHandlers.size) {
            val entry = collisionHandlers[i]
            if (entry.matches(fixtureA.filterData.categoryBits.toInt(),
                              fixtureB.filterData.categoryBits.toInt())) {
                val bodyA = fixtureA.body
                val bodyB = fixtureB.body

                val data = collisionDataPool.grabNew()
                if (entry.isFirstCategory(fixtureA.filterData.categoryBits.toInt())) {
                    data.bodyFirst = bodyA
                    data.bodySecond = bodyB
                }
                else {
                    data.bodyFirst = bodyB
                    data.bodySecond = bodyA
                }
                data.collisionHandler = entry.collisionHandler
                collisionCallback.run(data)
                collisionDataPool.freeCount(1)
            }
        }
    }

    /**
     * Run all active collision handlers that reference the [Body] parameter.
     */
    private fun runCollisionHandlers(body: B2Body, collisionCallback: CollisionCallback) {

        val activeCollisions = collisionPool.itemsInUse
        val numCollisions = activeCollisions.size
        for (i in 0 until numCollisions) {
            val activeCollision = activeCollisions[i]
            if (activeCollision.ownsBody(body)) {
                val fixtureA = activeCollision.fixtureA
                val fixtureB = activeCollision.fixtureB

                // Fixtures are non-null if ownsBody is true
                runCollisionHandlers(fixtureA!!, fixtureB!!, collisionCallback)
            }
        }
    }

    fun dispose() {
        while (!bodies.isEmpty()) {
            destroyBody(bodies[0])
        }
        b2World.dispose()
        circleShape.dispose()
        polyShape.dispose()

        assert(b2World.bodyCount == 0, { "Box2D world wasn't disposed correctly" })
    }

    fun debugRender(cameraMatrix: Matrix4, metersToPixels: Float) {
        if (debugRender == null) {
            debugRender = DebugRender()
        }

        debugRender!!.render(cameraMatrix, metersToPixels)
    }
}