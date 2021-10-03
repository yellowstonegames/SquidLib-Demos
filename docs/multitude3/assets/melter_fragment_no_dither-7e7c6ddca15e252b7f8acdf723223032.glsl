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
varying vec3 v_coeffs;
varying vec3 v_section;

uniform sampler2D u_texture;

uniform float u_seed;
uniform float u_time;

// Quilez Basic Noise, from https://www.shadertoy.com/view/3sd3Rs (MIT-license)
vec3 bas(vec3 x)
{
    // setup
    vec3 i = floor(x);
    vec3 f = fract(x);
    vec3 s = sign(fract(x/2.0)-0.5);

    // use some hash to create a random value k in [0..1] from i
    vec3 k = fract(v_section * i + i.yzx);

    // quartic polynomial
    return s*f*(f-1.0)*((16.0*k-4.0)*f*(f-1.0)-1.0);
}

// this is different from other swayRandomized in Northern demos because it uses Quilez basic noise instead of trig
vec3 swayRandomized(vec3 seed, vec3 value)
{
    return bas(seed.xyz + value.zxy - bas(seed.zxy + value.yzx) + bas(seed.yzx + value.xyz));
}

vec3 swayTrig(vec3 seed, vec3 value)
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
    vec2 uv = (gl_FragCoord.xy * 0.1) + swayTrig(v_coeffs.zxy, (v_time * 0.25) * v_coeffs.yzx - gl_FragCoord.yxy * 0.004).xy * 42.0;

    vec3 adj = vec3(-1.11, 1.41, 1.61);
    vec3 con = vec3(0.0004375, 0.0005625, 0.0008125) * v_time + v_c * uv.x + v_s * uv.y;

    con = cosmic(v_coeffs, con);
    con = cosmic(v_coeffs + 1.618, con);

    gl_FragColor = vec4(sin(con * 3.1416) * 0.5 + 0.5, 1.0);
}