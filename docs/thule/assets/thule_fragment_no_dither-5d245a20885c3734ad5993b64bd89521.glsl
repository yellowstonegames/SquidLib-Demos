#ifdef GL_ES
#define LOWP lowp
precision highp float;
#else
#define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying float v_time;
varying vec3 v_s;
varying vec3 v_c;
uniform sampler2D u_texture;

uniform float u_seed;
uniform float u_time;

// what's different here is mostly how swayRandomized() incorporates the x, y, and z of seed and value for each component.
vec3 swayRandomized(vec3 seed, vec3 value)
{
    return sin(seed.xyz + value.zxy - cos(seed.zxy + value.yzx - cos(seed.yzx + value.xyz) * 1.3) * 1.6);
}

// this function, if given steadily-increasing values in con, may return exponentially-rapidly-changing results.
// even though it should always return a vec3 with components between -1 and 1, we use it carefully.
vec3 cosmic(vec3 c, vec3 con)
{
    return (con
    + swayRandomized(c, con.yzx)
    + swayRandomized(c + 1.1, con.zxy)
    + swayRandomized(c + 2.2, con.xyz)) * 0.25;
}

void main()
{
    vec3 COEFFS = fract((u_seed + 23.4567) * vec3(0.8191725133961645, 0.6710436067037893, 0.5497004779019703)) + 0.5;
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = gl_FragCoord.xy * 0.0625 + swayRandomized(COEFFS.zxy, (u_time * 0.1875) * COEFFS.yzx).xy * 32.0;

    vec3 con = vec3(0.0004375, 0.0005625, 0.0008125) * v_time + v_c * uv.x + v_s * uv.y;

    con = cosmic(COEFFS, con);
    con = cosmic(COEFFS + 1.618, con + COEFFS);

    gl_FragColor = vec4(swayRandomized(COEFFS + 3.0, con * 3.14159265) * 0.5 + 0.5, 1.0);
}