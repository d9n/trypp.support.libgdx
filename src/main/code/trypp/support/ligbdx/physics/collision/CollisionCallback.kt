package trypp.support.ligbdx.physics.collision

internal interface CollisionCallback {
    fun run(data: CollisionData)
}