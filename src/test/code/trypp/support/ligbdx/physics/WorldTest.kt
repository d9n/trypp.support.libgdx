package trypp.support.ligbdx.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.google.common.truth.Truth.assertThat
import org.testng.annotations.Test
import trypp.support.ligbdx.physics.collision.CollisionAdapter
import trypp.support.time.Duration
import java.util.*

class WorldTest {

    val BALL = 1 shl 0
    val SENSOR_1DAMAGE = 1 shl 1
    val SENSOR_5DAMAGE = 1 shl 2
    val PLATFORM = 1 shl 3

    @Test fun testCollisionBetweenTwoBodies() {
        val world = World.Builder().registerCollidable(BALL, BALL).build()

        var collided = false
        world.addCollisionHandler(BALL, BALL, object : CollisionAdapter() {
            override fun onCollided(bodyA: Body, bodyB: Body) {
                collided = true
            }
        })

        val firstBody = world.createCircleBody(BALL, 5.0f, pos = Vector2(-10f, 0f))
        world.createCircleBody(BALL, 5.0f, pos = Vector2(10f, 0f))

        firstBody.setLinearVelocity(20f, 0f)

        assertThat(collided).isFalse()
        world.update(Duration.ofSeconds(1f))
        assertThat(collided).isTrue()

        world.dispose()
    }

    @Test fun testBodiesCollideIfNoHandlersRegistered() {
        val world = World.Builder().registerCollidable(BALL, BALL).build()

        val ball1 = world.createCircleBody(BALL, 5.0f, pos = Vector2(-10f, -3f))
        val ball2 = world.createCircleBody(BALL, 5.0f, pos = Vector2(10f, 3f))

        ball1.setLinearVelocity(20f, 0f)
        ball2.setLinearVelocity(-20f, 0f)

        assertThat(ball1.position.y).isWithin(0f).of(-3f)
        world.update(Duration.ofSeconds(1f))
        assertThat(ball1.position.y).isLessThan(-4f)

        world.dispose()
    }

    @Test fun testDisableCollisionForBody() {
        val world = World.Builder().registerCollidable(BALL, BALL).build()

        var collided = false
        world.addCollisionHandler(BALL, BALL, object : CollisionAdapter() {
            override fun onCollided(bodyA: Body, bodyB: Body) {
                collided = true
            }
        })

        val firstBody = world.createCircleBody(BALL, 5.0f, pos = Vector2(-10f, 0f))
        val secondBody = world.createCircleBody(BALL, 5.0f, pos = Vector2(10f, 0f))

        firstBody.setLinearVelocity(20f, 0f)

        world.enableCollisions(secondBody, false)

        world.update(Duration.ofSeconds(1f))
        assertThat(collided).isFalse()

        world.dispose()
    }


    @Test fun passThruCheckOnlyCalledOnce() {
        val world = World.Builder().registerCollidable(BALL, BALL).build()
        var count = 0
        world.addCollisionHandler(BALL, BALL, object : CollisionAdapter() {
            override fun allowPassThru(bodyA: Body, bodyB: Body): Boolean {
                count++
                return true
            }

            override fun onCollided(bodyA: Body, bodyB: Body) {
            }
        })

        val ball = world.createCircleBody(BALL, 10f, pos = Vector2(-10f, 0f))
        ball.setLinearVelocity(20f, 0f)
        world.createCircleBody(BALL, 10f, pos = Vector2(10f, 0f))

        world.update(Duration.ofSeconds(2f))
        assertThat(count).isEqualTo(1)

        world.dispose()
    }

    @Test fun testCollisionWithSensorsWorks() {
        val world = World.Builder().
            registerCollidable(BALL, SENSOR_1DAMAGE or SENSOR_5DAMAGE).build()

        var hp = 10
        world.addCollisionHandler(BALL, SENSOR_1DAMAGE, object : CollisionAdapter() {
            override fun allowPassThru(bodyA: Body, bodyB: Body): Boolean {
                return true
            }

            override fun onCollided(bodyA: Body, bodyB: Body) {
                hp -= 1
            }
        })

        world.addCollisionHandler(BALL, SENSOR_5DAMAGE, object : CollisionAdapter() {
            override fun allowPassThru(bodyA: Body, bodyB: Body): Boolean {
                return true
            }

            override fun onCollided(bodyA: Body, bodyB: Body) {
                hp -= 5
            }
        })

        val ball = world.createCircleBody(BALL, 10f, pos = Vector2(-20f, 0f))
        ball.setLinearVelocity(20f, 0f)

        world.createCircleBody(SENSOR_1DAMAGE, 10f, pos = Vector2(0f, 0f))
        world.createBoxBody(SENSOR_5DAMAGE, 10f, 10f, pos = Vector2(20f, 0f))

        assertThat(hp).isEqualTo(10)

        world.update(Duration.ofSeconds(1f))
        assertThat(hp).isEqualTo(9) // Came in contact with sensor1

        world.update(Duration.ofSeconds(1f))
        assertThat(hp).isEqualTo(4) // Came in contact with sensor5

        world.dispose()

        assertThat(ball.b2body).isNull()
    }

    @Test fun directionalCollisionWorks() {
        val world = World.Builder(gravity = Vector2(0f, -10f)).
            registerCollidable(BALL, PLATFORM).build()

        val passThruHistory = ArrayList<Boolean>()
        world.addCollisionHandler(BALL, PLATFORM, object : CollisionAdapter() {
            override fun allowPassThru(bodyA: Body, bodyB: Body): Boolean {
                val canPassThru = bodyA.position.y < bodyB.position.y
                passThruHistory.add(canPassThru)
                return canPassThru
            }
        })

        // Throw ball up, which should go through the platform and then land on it on
        // its downward arc
        val ball = world.createCircleBody(BALL, 10f, pos = Vector2(0f, -10f))
        val platform = world.createBoxBody(PLATFORM, Vector2(100f, 1f), type = BodyType.StaticBody)

        ball.setLinearVelocity(0f, 40f)

        assertThat(passThruHistory.size).isEqualTo(0)
        assertThat(ball.position.y).isLessThan(platform.position.y)

        world.update(Duration.ofSeconds(10f))

        assertThat(passThruHistory.size).isEqualTo(2)
        assertThat(passThruHistory).containsExactly(true, false)

        assertThat(ball.position.y).isGreaterThan(platform.position.y)

        world.dispose()
    }
}


