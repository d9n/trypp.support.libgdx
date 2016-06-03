package trypp.support.ligbdx.physics

import com.badlogic.gdx.math.Vector2
import com.google.common.truth.Truth.assertThat
import org.testng.Assert
import org.testng.annotations.Test
import trypp.support.time.Duration

class BodyTest {
    val ZERO_CATEGORY = 0
    val MULTI_BIT_CATEGORY = 1 or 2 or 4
    val TEST_CATEGORY = 1 shl 0

    @Test
    fun createBodyHasDefaultValues() {
        val world = World.Builder().build()
        val body = world.createCircleBody(TEST_CATEGORY, 10f)
        assertThat(body.category).isEqualTo(TEST_CATEGORY)
        assertThat(body.mass).isWithin(0f).of(1f)
        assertThat(body.linearVelocity).isEqualTo(Vector2.Zero)
        assertThat(body.position).isEqualTo(Vector2.Zero)
        assertThat(body.linearDamping).isWithin(0f).of(0f)
    }

    @Test
    fun applyForceWorks() {
        val world = World.Builder().build()
        val body = world.createCircleBody(TEST_CATEGORY, 1f)

        // Body mass is 1
        // Push at 3 kg m / s² for 1 second means v is 3
        val elapsed = Duration.zero()
        while (elapsed.getSeconds() < 1f) {
            body.applyForce(3f, 0f)
            world.update(world.stepDuration)
            elapsed.add(world.stepDuration)
        }

        assertThat(body.linearVelocity.x).isWithin(0.1f).of(3f)
    }

    @Test
    fun applyImpulseWorks() {
        val world = World.Builder().build()
        val body = world.createCircleBody(TEST_CATEGORY, 1f)

        body.applyImpluse(3f, 0f)
        world.update(Duration.ofSeconds(1f))

        assertThat(body.linearVelocity.x).isWithin(0.1f).of(3f)
    }

    @Test
    fun setLinearVelocityWorks() {
        val world = World.Builder().build()
        val body = world.createCircleBody(TEST_CATEGORY, 1f)

        body.linearVelocity = Vector2(10f, 0f)
        world.update(Duration.ofSeconds(1f))

        assertThat(body.position.x).isWithin(0.2f).of(10f)
    }

    @Test
    fun setLinearDampingWorks() {
        val world = World.Builder().build()
        val body = world.createCircleBody(TEST_CATEGORY, 1f)

        body.linearVelocity = Vector2(10f, 0f)
        body.linearDamping = 10f
        world.update(Duration.ofSeconds(10f))

        assertThat(body.linearVelocity.isZero(0.1f)).isTrue()
    }

    @Test
    fun createBodyWithZeroCategoryThrowsException() {
        val world = World.Builder().build()
        try {
            world.createCircleBody(ZERO_CATEGORY, 10f)
            Assert.fail("Can't create a body with an invalid category")
        }
        catch (e: IllegalArgumentException) {}
    }

    @Test
    fun createBodyWithMultibitCategoryThrowsException() {
        val world = World.Builder().build()
        try {
            world.createCircleBody(MULTI_BIT_CATEGORY, 10f)
            Assert.fail("Can't create a body with an invalid category")
        }
        catch (e: IllegalArgumentException) {}
    }
}