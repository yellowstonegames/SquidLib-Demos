package com.github.yellowstonegames.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Allows checking a group of {@code T} entities, where an entity is any type that can have a Vector3 position retrieved
 * from it via the {@link HasPosition3D} interface, for collisions with another entity or a member of its own group.
 * This doesn't currently allow getting a minimum translation vector to undo the collision. Since this only is meant to
 * check one moving entity at a time against all other entities, you can potentially refuse the movement that would
 * result in the collision. You might do this by tracking the previous position and reverting to it if any collision
 * occurs.
 * <br>
 * This is only meant for relatively small entity counts (under about 50). Larger amounts of entities will take longer
 * to process.
 *
 * @param <T> Any type that you can get a Vector3 position from; must implement HasPosition3D, often {@link gdx.liftoff.game.Mover}
 */
public class VoxelCollider<T extends HasPosition3D> {

    /**
     * The Array of all HasPosition3D entities that can enter a collision.
     */
    public Array<T> entities;

    /**
     * Temporary Vector3 to avoid allocating these all the time,
     */
    private final Vector3 tempVector3 = new Vector3();

    /**
     * Stores the most recent Array of colliding entities, which can be empty if there are no overlaps.
     */
    public final Array<T> colliding = new Array<>();

    /**
     * Creates a VoxelCollider with an empty entities Array.
     */
    public VoxelCollider() {
        entities = new Array<>(16);
    }

    /**
     * Creates a VoxelCollider with a copy of the given Array of entities.
     * @param colliders an Array of entities that will be copied
     */
    public VoxelCollider(Array<T> colliders){
        entities = new Array<>(colliders);
    }

    /**
     * Gets the Array of T entities that collide with the given Vector3 position.
     * If {@code collder} is a T in {@link #entities} (by identity), then the returned Array will never contain
     * that T entity.
     * <br>
     * This is a simple check between collider and every entity to see if their positions are close enough to collide.
     * This runs in O(n) time, where n is the number of entities. This could be sped up using an
     * {@link com.badlogic.gdx.math.Octree}, potentially, if it ever matters.
     *
     * @param collider an entity to check for collision; will never be considered self-colliding, and may be in entities
     * @return a reused Array (which will change on the next call to this method) of all T entities that overlap with {@code collider}, not including itself.
     */
    public Array<T> collisionsWith(T collider) {
        Vector3 voxelPosition = collider.getPosition();
        // The reused colliding Array must be cleared to avoid the last calculation remaining here.
        colliding.clear();
        float x = voxelPosition.x, y = voxelPosition.y, z = voxelPosition.z;
        for (T e : entities) {
            // If e is reference-identical to collider, and we added it to colliding, we would be including
            // the collider in its own collisions. We don't do that!
            if(e != collider
                && Math.abs(x - e.getPosition().x) < 1f
                && Math.abs(y - e.getPosition().y) < 1f
                && Math.abs(z - e.getPosition().z) < 1f) {
                // The distance between x coordinates is close enough for them to be overlapping
                colliding.add(e);
            }
        }
        return colliding;
    }
}
