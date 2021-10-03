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

vec3 swayRandomized(vec3 seed, vec3 value)
{
    return sin(seed.xyz + value.zxy - cos(seed.zxy + value.yzx) + cos(seed.yzx + value.xyz));
}

vec3 cosmic(vec3 c, vec3 con)
{
    return (con
    + swayRandomized(c, con)) * 0.5;
}

void main()
{
    vec3 COEFFS = fract((u_seed + 23.4567) * vec3(0.8191, 0.671, 0.5497)) + 0.5;
    vec2 uv = (gl_FragCoord.xy * 0.3125) + swayRandomized(COEFFS.zxy, (u_time * 0.11) * COEFFS.yzx - gl_FragCoord.yxy * 0.01).xy * 42.0;

    vec3 adj = vec3(-1.11, 1.41, 1.61);
    vec3 con = vec3(0.0004375, 0.0005625, 0.0008125) * v_time + v_c * uv.x + v_s * uv.y;

    con = cosmic(COEFFS, con);
    con = cosmic(COEFFS + 1.618, con);

    gl_FragColor = vec4(swayRandomized(COEFFS + 3.0, con * 2.0) * 0.5 + 0.5, 1.0);
}