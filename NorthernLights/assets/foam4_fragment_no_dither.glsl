#ifdef GL_ES
#define LOWP lowp
precision highp float;
#else
#define LOWP 
#endif

const float PHI = 0.61803398874989484820459; // phi, the Golden Ratio
const vec2 H2 = vec2(1.324717957244746, 1.754877666246693); // harmonious numbers for 2D
const vec3 H3 = vec3(0.8191725134, 0.6710436067, 0.5497004779); // harmonious numbers for 3D
const vec4 H4 = vec4(0.8566748838545, 0.7338918566271, 0.6287067210378, 0.5385972572236); // harmonious numbers for 4D

// This Shadertoy ( https://www.shadertoy.com/view/wssBz8 ) shows "Foam Noise" by Tommy Ettinger.
// It's just value noise that's rotated and domain warps the next result.

// Based on a Shadertoy: https://www.shadertoy.com/view/4dS3Wd
// By Morgan McGuire @morgan3d, http://graphicscodex.com
// Reuse permitted under the BSD license.

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform float u_seed;
uniform float u_time;

float hash(float seed, float p) {
    return fract(fract((p - seed) * PHI + seed) * (PHI - p) - seed);
}

float noise(float seed, vec4 x) {
    const vec4 step = vec4(59.0, 43.0, 37.0, 53.0);

    vec4 i = floor(x);
    vec4 f = fract(x);

    float n = dot(i, step);

    vec4 u = f * f * (3.0 - 2.0 * f);
    return
       mix(mix(mix(mix( hash(seed, n                                  ), hash(seed, n + dot(step, vec4(1., 0., 0., 0.))), u.x),
                   mix( hash(seed, n + dot(step, vec4(0., 1., 0., 0.))), hash(seed, n + dot(step, vec4(1., 1., 0., 0.))), u.x), u.y),
               mix(mix( hash(seed, n + dot(step, vec4(0., 0., 1., 0.))), hash(seed, n + dot(step, vec4(1., 0., 1., 0.))), u.x),
                   mix( hash(seed, n + dot(step, vec4(0., 1., 1., 0.))), hash(seed, n + dot(step, vec4(1., 1., 1., 0.))), u.x), u.y), u.z),
           mix(mix(mix( hash(seed, n + dot(step, vec4(0., 0., 0., 1.))), hash(seed, n + dot(step, vec4(1., 0., 0., 1.))), u.x),
                   mix( hash(seed, n + dot(step, vec4(0., 1., 0., 1.))), hash(seed, n + dot(step, vec4(1., 1., 0., 1.))), u.x), u.y),
               mix(mix( hash(seed, n + dot(step, vec4(0., 0., 1., 1.))), hash(seed, n + dot(step, vec4(1., 0., 1., 1.))), u.x),
                   mix( hash(seed, n + dot(step, vec4(0., 1., 1., 1.))), hash(seed, n + dot(step, vec4(1., 1., 1., 1.))), u.x), u.y), u.z), u.w);
}

float foam(float seed, vec4 x) {
    vec4 p = vec4(
                  dot(x.xy,  vec2(-0.25, 0.9682458365518543)),
                  dot(x.xyz, vec3(-0.25, -0.3227486121839514,  0.91287092917527690)),
                  dot(x,     vec4(-0.25, -0.3227486121839514, -0.45643546458763834,  0.7905694150420948)),
                  dot(x,     vec4(-0.25, -0.3227486121839514, -0.45643546458763834, -0.7905694150420948)));

    float a = noise(seed, p.xyzw);
    float b = noise(seed + 42.1, vec4(x.x, p.yzw) + a * H4.x);
    float c = noise(seed + 84.2, vec4(x.x, p.xzw) + b * H4.y);
    float d = noise(seed + 126.3, vec4(x.x, p.xyw) + c * H4.z);
    float e = noise(seed + 168.4, vec4(x.x, p.xyz) + d * H4.w);
    return smoothstep(0.0, 1.0, smoothstep(0.0, 1.0, (a + b + c + d + e) * 0.2));
}

void main() {
  vec2 pos = gl_FragCoord.xy * 0.03125 + u_time * 0.625;
  vec4 i = vec4(sin(pos), cos(pos));
  gl_FragColor.r = foam(4.0 + u_seed, i);
  gl_FragColor.g = foam(61.0 + u_seed, i);
  gl_FragColor.b = foam(257.0 + u_seed, i);
  gl_FragColor.a = v_color.a;
}
