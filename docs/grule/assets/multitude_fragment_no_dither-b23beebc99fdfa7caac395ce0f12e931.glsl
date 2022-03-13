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
//vec3 sway(vec3 sc, vec3 x)
//{
//    // setup
//    vec3 i = floor(x);
//    vec3 f = fract(x);
//    vec3 s = sign(fract(x/2.0)-0.5);
//
//    // use some hash to create a random value k in [0..1] from i
//    vec3 k = fract(sc * i + i.yzx);
//
//    // quartic polynomial
//    return s*f*(f-1.0)*((16.0*k-4.0)*f*(f-1.0)-1.0);
//}


vec3 swayTrig(vec3 seed, vec3 value)
{
    return sin(seed.xyz + value.zxy - cos(seed.zxy + value.yzx) + cos(seed.yzx + value.xyz));
}

vec3 bas(vec3 x)
{
    return cross(swayTrig(v_section, x), swayTrig(0.721 - v_section.zxy, x.yzx * 0.5));
//    return sway(v_section, x) * 0.375 + sway(0.618 - v_section, x * 0.5) * 0.3125 + swayTrig(v_section - 0.511, x * 0.75);
}

// this is different from other swayRandomized in Northern demos because it uses cross-product
vec3 swayRandomized(vec3 seed, vec3 value)
{
    return bas(seed.xyz + value.zxy - bas(seed.zxy + value.yzx) + bas(seed.yzx + value.xyz));
}

vec3 cosmic(vec3 c, vec3 con)
{
    return (con
    + swayRandomized(c, con)) * 0.5;
}

void main()
{
    vec2 uv = (gl_FragCoord.xy * 0.3125) + swayRandomized((v_time * 0.2) * v_coeffs.yzx, vec3(gl_FragCoord.yxy * 0.002 + v_time * 0.01)).xy * 42.0;

    vec3 adj = vec3(-1.11, 1.41, 1.61);
    vec3 con = vec3(0.0004375, 0.0005625, 0.0008125) * v_time + v_c * uv.x + v_s * uv.y;

    con = cosmic(v_coeffs, con);
//    con = cosmic(v_coeffs + 1.618, con);
    con = cosmic(0.123 - v_coeffs, con);

    gl_FragColor = vec4(sin(con * 3.1416) * 0.5 + 0.5, 1.0);
//    gl_FragColor = vec4(pow(sin(con * 3.1416) * 0.175 + 0.2, vec3(2.0)), 1.0);

}