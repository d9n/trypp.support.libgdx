package trypp.support.ligbdx.physics.collision

internal class CollisionHandlerEntry(var categoriesFirst: Int,
                                    var categoriesSecond: Int,
                                    var collisionHandler: CollisionHandler) {
    fun matches(categoryA: Int, categoryB: Int): Boolean {
        return (categoriesFirst and categoryA != 0 && categoriesSecond and categoryB != 0) ||
            (categoriesFirst and categoryB != 0 && categoriesSecond and categoryA != 0)
    }

    fun isFirstCategory(categoryBitsA: Int): Boolean {
        return categoriesFirst == categoryBitsA
    }
}