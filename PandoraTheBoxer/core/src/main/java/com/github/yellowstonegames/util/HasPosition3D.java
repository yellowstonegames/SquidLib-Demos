package com.github.yellowstonegames.util;

import com.badlogic.gdx.math.Vector3;

/**
 * A simple interface for anything that can have its position requested as a Vector3. This usually means an isometric
 * tile position here, but could be another 3D position in other code. This is used mainly by {@link VoxelCollider}.
 */
public interface HasPosition3D {
    /**
     * Gets the position of this object as a Vector3
     * @return this object's Vector3 position, which should be a direct reference for if code needs to modify it
     */
    Vector3 getPosition();
}
