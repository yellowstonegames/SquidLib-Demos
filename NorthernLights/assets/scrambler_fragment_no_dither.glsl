#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform float seed;
uniform float tm;
const float PHI = 1.618034;

vec3 applyHue(vec3 rgb) {
    float hue = 0.25 * tm;
    vec3 k = vec3(0.57735);
    float c = cos(hue);
    //Rodrigues' rotation formula
    return rgb * c + cross(k, rgb) * sin(hue) + k * dot(k, rgb) * (1.0 - c);
}

float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = fract(fract((f - seed) * PHI + seed) * (PHI - f) - seed);
    float end   = fract(fract((f + 1.0 - seed) * PHI + seed) * (PHI - f - 1.0) - seed);
    
    // a little slower; use with different multiplier/addend on cosmic
    //float start = sin((cos(f + seed) * 12.973 + seed) * 31.413);
    //float end   = sin((cos(f + 1.0 + seed) * 12.973 + seed) * 31.413);

    return mix(start, end, smoothstep(0.0, 1.0, value - f));
}
vec3 cosmic(vec3 seed, vec3 con)
{
    con.x = swayRandomized(seed.x, con.x + con.z);
    con.y = swayRandomized(seed.y, con.y + con.x);
    con.z = swayRandomized(seed.z, con.z + con.y);
    return con * 2.2 + 0.25;
}

void main() {
  vec3 s = 31.555 + 19.225 * fract(vec3(seed * 0.61803, seed * 0.75488, seed * 0.56984));

  vec3 alt = vec3(gl_FragCoord.xy * 0.03125, tm * 0.03125);
  vec3 con = (alt.yzx + alt.zxy);

  con += cosmic(s.yzx, con.xyz);
  con += cosmic(s.zxy, con.yzx);
  con += cosmic(s.xyz, con.zxy);

  gl_FragColor.rgb = applyHue(cos(con * 3.14159) * 0.5 + 0.5);
  gl_FragColor.a = v_color.a;
}
