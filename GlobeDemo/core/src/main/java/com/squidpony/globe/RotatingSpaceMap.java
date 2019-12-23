package com.squidpony.globe;

import squidpony.ArrayTools;
import squidpony.annotation.Beta;
import squidpony.squidgrid.mapping.WorldMapGenerator;
import squidpony.squidmath.FastNoise;
import squidpony.squidmath.Noise;
import squidpony.squidmath.NumberTools;

import java.util.Arrays;

/**
 * A concrete implementation of {@link WorldMapGenerator} that imitates an infinite-distance perspective view of a
 * world, showing only one hemisphere, that should be as wide as it is tall (its outline is a circle). It should
 * look as a world would when viewed from space, and implements rotation differently to allow the planet to be
 * rotated without recalculating all the data, though it cannot zoom. Note that calling
 * {@link #setCenterLongitude(double)} does a lot more work than in other classes, but less than fully calling
 * {@link #generate()} in those classes, since it doesn't remake the map data at a slightly different rotation and
 * instead keeps a single map in use the whole time, using sections of it. This uses an
 * <a href="https://en.wikipedia.org/wiki/Orthographic_projection_in_cartography">Orthographic projection</a> with
 * the latitude always at the equator; the internal map is stored as a {@link SphereMap}, which uses a
 * <a href="https://en.wikipedia.org/wiki/Cylindrical_equal-area_projection#Discussion">cylindrical equal-area
 * projection</a>, specifically the Smyth equal-surface projection.
 * <br>
 * <a href="https://i.imgur.com/WNa5nQ1.gifv">Example view of a planet rotating</a>.
 * <a href="https://i.imgur.com/NV5IMd6.gifv">Another example</a>.
 */
@Beta
public class RotatingSpaceMap extends WorldMapGenerator {
    public final double[][] xPositions,
            yPositions,
            zPositions;
    protected final int[] edges;
    public final SphereMap storedMap;
    /**
     * Constructs a concrete WorldMapGenerator for a map that can be used to view a spherical world from space,
     * showing only one hemisphere at a time.
     * Always makes a 100x100 map.
     * Uses FastNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
     * If you were using {@link com.squidpony.globe.RotatingSpaceMap#RotatingSpaceMap(long, int, int, Noise.Noise3D, double)}, then this would be the
     * same as passing the parameters {@code 0x1337BABE1337D00DL, 100, 100, DEFAULT_NOISE, 1.0}.
     */
    public RotatingSpaceMap() {
        this(0x1337BABE1337D00DL, 100, 100, DEFAULT_NOISE, 1.0);
    }

    /**
     * Constructs a concrete WorldMapGenerator for a map that can be used to view a spherical world from space,
     * showing only one hemisphere at a time.
     * Takes only the width/height of the map. The initial seed is set to the same large long
     * every time, and it's likely that you would set the seed when you call {@link #generate(long)}. The width and
     * height of the map cannot be changed after the fact, but you can zoom in.
     * Uses FastNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
     *
     * @param mapWidth  the width of the map(s) to generate; cannot be changed later
     * @param mapHeight the height of the map(s) to generate; cannot be changed later
     */
    public RotatingSpaceMap(int mapWidth, int mapHeight) {
        this(0x1337BABE1337D00DL, mapWidth, mapHeight, DEFAULT_NOISE, 1.0);
    }

    /**
     * Constructs a concrete WorldMapGenerator for a map that can be used to view a spherical world from space,
     * showing only one hemisphere at a time.
     * Takes an initial seed and the width/height of the map. The {@code initialSeed}
     * parameter may or may not be used, since you can specify the seed to use when you call {@link #generate(long)}.
     * The width and height of the map cannot be changed after the fact, but you can zoom in.
     * Uses FastNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
     *
     * @param initialSeed the seed for the GWTRNG this uses; this may also be set per-call to generate
     * @param mapWidth    the width of the map(s) to generate; cannot be changed later
     * @param mapHeight   the height of the map(s) to generate; cannot be changed later
     */
    public RotatingSpaceMap(long initialSeed, int mapWidth, int mapHeight) {
        this(initialSeed, mapWidth, mapHeight, DEFAULT_NOISE, 1.0);
    }

    /**
     * Constructs a concrete WorldMapGenerator for a map that can be used to view a spherical world from space,
     * showing only one hemisphere at a time.
     * Takes an initial seed and the width/height of the map. The {@code initialSeed}
     * parameter may or may not be used, since you can specify the seed to use when you call {@link #generate(long)}.
     * The width and height of the map cannot be changed after the fact, but you can zoom in.
     * Uses FastNoise as its noise generator, with the given octave multiplier affecting detail.
     *
     * @param initialSeed the seed for the GWTRNG this uses; this may also be set per-call to generate
     * @param mapWidth    the width of the map(s) to generate; cannot be changed later
     * @param mapHeight   the height of the map(s) to generate; cannot be changed later
     * @param octaveMultiplier used to adjust the level of detail, with 0.5 at the bare-minimum detail and 1.0 normal
     */
    public RotatingSpaceMap(long initialSeed, int mapWidth, int mapHeight, double octaveMultiplier) {
        this(initialSeed, mapWidth, mapHeight, DEFAULT_NOISE, octaveMultiplier);
    }

    /**
     * Constructs a concrete WorldMapGenerator for a map that can be used to view a spherical world from space,
     * showing only one hemisphere at a time.
     * Takes an initial seed and the width/height of the map. The {@code initialSeed}
     * parameter may or may not be used, since you can specify the seed to use when you call {@link #generate(long)}.
     * The width and height of the map cannot be changed after the fact, but you can zoom in.
     * Uses the given noise generator, with 1.0 as the octave multiplier affecting detail.
     *
     * @param initialSeed the seed for the GWTRNG this uses; this may also be set per-call to generate
     * @param mapWidth    the width of the map(s) to generate; cannot be changed later
     * @param mapHeight   the height of the map(s) to generate; cannot be changed later
     * @param noiseGenerator an instance of a noise generator capable of 3D noise, usually {@link FastNoise}
     */
    public RotatingSpaceMap(long initialSeed, int mapWidth, int mapHeight, Noise.Noise3D noiseGenerator) {
        this(initialSeed, mapWidth, mapHeight, noiseGenerator, 1.0);
    }

    /**
     * Constructs a concrete WorldMapGenerator for a map that can be used to view a spherical world from space,
     * showing only one hemisphere at a time.
     * Takes an initial seed, the width/height of the map, and parameters for noise
     * generation (a {@link Noise.Noise3D} implementation, which is usually {@link FastNoise#instance}, and a
     * multiplier on how many octaves of noise to use, with 1.0 being normal (high) detail and higher multipliers
     * producing even more detailed noise when zoomed-in). The {@code initialSeed} parameter may or may not be used,
     * since you can specify the seed to use when you call {@link #generate(long)}. The width and height of the map
     * cannot be changed after the fact, but you can zoom in. FastNoise will be the fastest 3D generator to use for
     * {@code noiseGenerator}, and the seed it's constructed with doesn't matter because this will change the
     * seed several times at different scales of noise (it's fine to use the static {@link FastNoise#instance}
     * because it has no changing state between runs of the program). The {@code octaveMultiplier} parameter should
     * probably be no lower than 0.5, but can be arbitrarily high if you're willing to spend much more time on
     * generating detail only noticeable at very high zoom; normally 1.0 is fine and may even be too high for maps
     * that don't require zooming.
     * @param initialSeed the seed for the GWTRNG this uses; this may also be set per-call to generate
     * @param mapWidth the width of the map(s) to generate; cannot be changed later
     * @param mapHeight the height of the map(s) to generate; cannot be changed later
     * @param noiseGenerator an instance of a noise generator capable of 3D noise, usually {@link FastNoise}
     * @param octaveMultiplier used to adjust the level of detail, with 0.5 at the bare-minimum detail and 1.0 normal
     */
    public RotatingSpaceMap(long initialSeed, int mapWidth, int mapHeight, Noise.Noise3D noiseGenerator, double octaveMultiplier) {
        super(initialSeed, mapWidth, mapHeight);
        xPositions = new double[mapWidth][mapHeight];
        yPositions = new double[mapWidth][mapHeight];
        zPositions = new double[mapWidth][mapHeight];
        edges = new int[height << 1];
        storedMap = new SphereMap(initialSeed, mapWidth << 1, mapHeight, noiseGenerator, octaveMultiplier);
    }

    /**
     * Copies the RotatingSpaceMap {@code other} to construct a new one that is exactly the same. References will only
     * be shared to Noise classes.
     * @param other a RotatingSpaceMap to copy
     */
    public RotatingSpaceMap(com.squidpony.globe.RotatingSpaceMap other)
    {
        super(other);
        xPositions = ArrayTools.copy(other.xPositions);
        yPositions = ArrayTools.copy(other.yPositions);
        zPositions = ArrayTools.copy(other.zPositions);
        edges = Arrays.copyOf(other.edges, other.edges.length);
        storedMap = new SphereMap(other.storedMap);
    }


    @Override
    public int wrapX(int x, int y) {
        y = Math.max(0, Math.min(y, height - 1));
        return Math.max(edges[y << 1], Math.min(x, edges[y << 1 | 1]));
    }

    @Override
    public int wrapY(final int x, final int y)  {
        return Math.max(0, Math.min(y, height - 1));
    }

    @Override
    public void setCenterLongitude(double centerLongitude) {
        super.setCenterLongitude(centerLongitude);
        int ax, ay;
        double
                ps, pc,
                qs, qc,
                h, yPos, xPos, iyPos, ixPos,
                i_uw = usedWidth / (double)width,
                i_uh = usedHeight / (double)height,
                lon, lat, rho,
                i_pi = 1.0 / Math.PI,
                rx = width * 0.5, irx = i_uw / rx,
                ry = height * 0.5, iry = i_uh / ry;

        yPos = startY - ry;
        iyPos = yPos / ry;
        for (int y = 0; y < height; y++, yPos += i_uh, iyPos += iry) {
            boolean inSpace = true;
            xPos = startX - rx;
            ixPos = xPos / rx;
            lat = NumberTools.asin(iyPos);
            for (int x = 0; x < width; x++, xPos += i_uw, ixPos += irx) {
                rho = (ixPos * ixPos + iyPos * iyPos);
                if(rho > 1.0) {
                    heightCodeData[x][y] = 1000;
                    inSpace = true;
                    continue;
                }
                if(inSpace)
                {
                    inSpace = false;
                    edges[y << 1] = x;
                }
                edges[y << 1 | 1] = x;
//                    th = NumberTools.asin(rho); // c
                lon = removeExcess((centerLongitude + (NumberTools.atan2(ixPos, NumberTools.cos(NumberTools.asin(Math.sqrt(rho)))))) * 0.5);
//                    lon = removeExcess((centerLongitude + (NumberTools.atan2(ixPos * rho, rho * NumberTools.cos(th)))) * 0.5);
//                lon = removeExcess(centerLongitude + NumberTools.asin(ixPos));
                
                qs = lat * 0.6366197723675814;
                qc = qs + 1.0;
                int sf = (qs >= 0.0 ? (int) qs : (int) qs - 1) & -2;
                int cf = (qc >= 0.0 ? (int) qc : (int) qc - 1) & -2;
                qs -= sf;
                qc -= cf;
                qs *= 2.0 - qs;
                qc *= 2.0 - qc;
                qs = qs * (-0.775 - 0.225 * qs) * ((sf & 2) - 1);
                qc = qc * (-0.775 - 0.225 * qc) * ((cf & 2) - 1);


                ps = lon * 0.6366197723675814;
                pc = ps + 1.0;
                sf = (ps >= 0.0 ? (int) ps : (int) ps - 1) & -2;
                cf = (pc >= 0.0 ? (int) pc : (int) pc - 1) & -2;
                ps -= sf;
                pc -= cf;
                ps *= 2.0 - ps;
                pc *= 2.0 - pc;
                ps = ps * (-0.775 - 0.225 * ps) * ((sf & 2) - 1);
                pc = pc * (-0.775 - 0.225 * pc) * ((cf & 2) - 1);

                ax = (int)((lon * i_pi + 1.0) * width);
                ay = (int)((qs + 1.0) * ry);
                
//                    // Hammer projection, not an inverse projection like we usually use
//                    z = 1.0 / Math.sqrt(1 + qc * NumberTools.cos(lon * 0.5));
//                    ax = (int)((qc * NumberTools.sin(lon * 0.5) * z + 1.0) * width);
//                    ay = (int)((qs * z + 1.0) * height * 0.5);

                if(ax >= storedMap.width || ax < 0 || ay >= storedMap.height || ay < 0)
                {
                    heightCodeData[x][y] = 1000;
                    continue;
                }
                if(storedMap.heightCodeData[ax][ay] >= 1000) // for the seam we get when looping around
                {
                    ay = storedMap.wrapY(ax, ay);
                    ax = storedMap.wrapX(ax, ay);
                }

                xPositions[x][y] = pc * qc;
                yPositions[x][y] = ps * qc;
                zPositions[x][y] = qs;

                heightData[x][y] = h = storedMap.heightData[ax][ay];
                heightCodeData[x][y] = codeHeight(h);
                heatData[x][y] = storedMap.heatData[ax][ay];
                moistureData[x][y] = storedMap.moistureData[ax][ay];

                minHeightActual = Math.min(minHeightActual, h);
                maxHeightActual = Math.max(maxHeightActual, h);
            }
            minHeightActual = Math.min(minHeightActual, minHeight);
            maxHeightActual = Math.max(maxHeightActual, maxHeight);
        }

    }

    protected void regenerate(int startX, int startY, int usedWidth, int usedHeight,
                              double landMod, double heatMod, int stateA, int stateB)
    {
        if(cacheA != stateA || cacheB != stateB)// || landMod != storedMap.landModifier || coolMod != storedMap.coolingModifier)
        {
            
            storedMap.generate(landMod, heatMod, (long) stateB << 32 | (stateA & 0xFFFFFFFFL));
            minHeightActual = Double.POSITIVE_INFINITY;
            maxHeightActual = Double.NEGATIVE_INFINITY;

            minHeight = storedMap.minHeight;
            maxHeight = storedMap.maxHeight;

            minHeat = storedMap.minHeat;
            maxHeat = storedMap.maxHeat;

            minWet = storedMap.minWet;
            maxWet = storedMap.maxWet;

            cacheA = stateA;
            cacheB = stateB;
        }
        setCenterLongitude(centerLongitude);
        landData.refill(heightCodeData, 4, 999);
    }
}
