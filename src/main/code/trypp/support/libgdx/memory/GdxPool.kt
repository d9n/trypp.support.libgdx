package trypp.support.libgdx.memory

import com.badlogic.gdx.math.Vector2
import trypp.support.memory.Pool

/**
 * Helper methods for creating pools around common libgdx objects
 */
class GdxPool {
    companion object {
        fun ofVec2(capactiy: Int = Pool.DEFAULT_CAPACITY): Pool<Vector2> {
            return Pool({ Vector2() }, { it.setZero() }, capactiy)
        }
    }
}