package trypp.support.ligbdx.physics

import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.math.Vector2


/**
 * Utility methods to aid using Box2d physics
 */
class PhysicsUtils(pixelsToMeters: Float = DEFAULT_PIXELS_TO_METERS) {

    companion object {
        /**
         * Default 24 pixels to 1.5 meters, which is a decent amount of vertical space for something
         * generally human sized on a 640x480 screen.
         */
        private val DEFAULT_PIXELS_TO_METERS = 0.0625f
    }

    val PIXELS_TO_METERS = pixelsToMeters
    val METERS_TO_PIXELS = 1 / pixelsToMeters

    fun toPixels(meters: Float): Float {
        return meters * METERS_TO_PIXELS
    }

    fun toMeters(pixels: Float): Float {
        return pixels * PIXELS_TO_METERS
    }

    fun toPixels(meters: Vector2): Vector2 {
        return meters.scl(METERS_TO_PIXELS)
    }

    fun toMeters(pixels: Vector2): Vector2 {
        return pixels.scl(PIXELS_TO_METERS)
    }

    fun newCircle(radiusPixels: Float): CircleShape {
        val circleShape = CircleShape()
        circleShape.radius = radiusPixels * PIXELS_TO_METERS
        return circleShape
    }

    fun newRectangle(halfWidthPixels: Float, halfHeightPixels: Float): PolygonShape {
        val polygonShape = PolygonShape()
        polygonShape.setAsBox(halfWidthPixels * PIXELS_TO_METERS,
                              halfHeightPixels * PIXELS_TO_METERS)
        return polygonShape
    }
}

