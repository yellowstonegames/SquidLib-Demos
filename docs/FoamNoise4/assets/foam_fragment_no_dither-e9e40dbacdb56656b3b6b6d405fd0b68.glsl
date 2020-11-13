#ifdef GL_ES
#define LOWP lowp
precision highp float;
//const float PHI = 1.61803; // phi, the Golden Ratio
//const vec2 H2 = vec2(1.32471, 1.75487); // harmonious numbers for 2D
//const vec3 H3 = vec3(0.81917, 0.67104, 0.54970); // harmonious numbers for 3D
#else
#define LOWP 
#endif

const float PHI = 1.61803398874989484820459; // phi, the Golden Ratio
const vec2 H2 = vec2(1.324717957244746, 1.754877666246693); // harmonious numbers for 2D
const vec3 H3 = vec3(0.8191725134, 0.6710436067, 0.5497004779); // harmonious numbers for 3D

// This Shadertoy ( https://www.shadertoy.com/view/wssBz8 ) shows "Foam Noise" by Tommy Ettinger.
// It's just value noise that's rotated and domain warps the next result.

// Based on a Shadertoy: https://www.shadertoy.com/view/4dS3Wd
// By Morgan McGuire @morgan3d, http://graphicscodex.com
// Reuse permitted under the BSD license.

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform float seed;
uniform float tm;

float hash(float seed, float p) {
    return fract(fract((p - seed) * PHI + seed) * (PHI - p) - seed);
}

// Interleaved Gradient Noise,
//  - Jimenez, Next Generation Post Processing in Call of Duty: Advanced Warfare
//    Advances in Real-time Rendering, SIGGRAPH 2014
// slightly tweaked so the input, instead of v.xy, is vec2(dot(v, H2), dot(v.yxy * v.yxx, H3))
// H2 and H3 are harmonious number vectors.
//float hash(float seed, float p)
//{
//    vec3 magic = vec3(0.06711f, 0.00583f, 52.982f);
//    vec2 v = vec2(seed, p);
//    return fract(magic.z * fract(dot(vec2(dot(v, H2), dot(v.yxy * v.yxx, H3)), magic.xy)));
//}

//float hash(float seed, vec2 p) {
//    return fract((dot((p + seed), H2) + seed) * (0.5 + fract(dot(H2.yx - seed, p))));
//}
//
//vec3 hash(vec3 seed, float p) {
//    return fract(fract((p - seed) * PHI + seed) * (PHI - p) - seed);
//}
//
//vec3 hash(vec3 seed, vec3 p) {
//    return fract((dot((p + seed), H3) + seed) * (0.5 + fract(dot(H3.zxy - seed, p.yzx))));
//}

//float noise(float seed, float x) {
//    float i = floor(x);
//    float f = fract(x);
//    float u = f * f * (3.0 - 2.0 * f);
//    return mix(hash(seed, i), hash(seed, i + 1.0), u);
//}
//
//float noise(float x) {
//    return noise(42.0, x);
//}
//
//float noise(float seed, vec2 x) {
//    vec2 i = floor(x);
//    vec2 f = fract(x);
//
//	// Four corners in 2D of a tile
//	float a = hash(seed, i);
//    float b = hash(seed, i + vec2(1.0, 0.0));
//    float c = hash(seed, i + vec2(0.0, 1.0));
//    float d = hash(seed, i + vec2(1.0, 1.0));
//
//    vec2 u = f * f * (3.0 - 2.0 * f);
//	return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
//}
//
//float noise(vec2 x) {
//    return noise(42.0, x);
//}

float noise(float seed, vec3 x) {
    const vec3 step = vec3(59.0, 43.0, 37.0); //vec3(110.0, 241.0, 171.0);

    vec3 i = floor(x);
    vec3 f = fract(x);

    float n = dot(i, step);

    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix( hash(seed, n                              ), hash(seed, n + dot(step, vec3(1., 0., 0.))), u.x),
                   mix( hash(seed, n + dot(step, vec3(0., 1., 0.))), hash(seed, n + dot(step, vec3(1., 1., 0.))), u.x), u.y),
               mix(mix( hash(seed, n + dot(step, vec3(0., 0., 1.))), hash(seed, n + dot(step, vec3(1., 0., 1.))), u.x),
                   mix( hash(seed, n + dot(step, vec3(0., 1., 1.))), hash(seed, n + dot(step, vec3(1., 1., 1.))), u.x), u.y), u.z);
}

//float noise(vec3 x) {
//    return noise(42.0, x);
//}
//
//vec3 noise(vec3 seed, vec3 x) {
//    const vec3 step = vec3(59.0, 43.0, 37.0); //vec3(110.0, 241.0, 171.0);
//
//    vec3 i = floor(x);
//    vec3 f = fract(x);
//
//    float n = dot(i, step);
//
//    vec3 u = f * f * (3.0 - 2.0 * f);
//    return mix(mix(mix( hash(seed, n                              ), hash(seed, n + dot(step, vec3(1., 0., 0.))), u.x),
//                   mix( hash(seed, n + dot(step, vec3(0., 1., 0.))), hash(seed, n + dot(step, vec3(1., 1., 0.))), u.x), u.y),
//               mix(mix( hash(seed, n + dot(step, vec3(0., 0., 1.))), hash(seed, n + dot(step, vec3(1., 0., 1.))), u.x),
//                   mix( hash(seed, n + dot(step, vec3(0., 1., 1.))), hash(seed, n + dot(step, vec3(1., 1., 1.))), u.x), u.y), u.z);
//}

//float foam(float seed, float x) {
//    return noise(seed, x);
//}
//
//float foam(float x) { return foam(61.0, x); }
//
//float foam(float seed, vec2 x) {
//    vec3 p = vec3(x.x, dot(x, vec2(-0.5, 0.866)), dot(x, vec2(-0.5, -0.866)));
//    float a = noise(seed, p.yz);
//    float b = noise(seed + 42.1, p.xz + a);
//    float c = noise(seed + 84.2, p.xy + b);
//    return smoothstep(0.0, 1.0, (a + b + c) * (1.0 / 3.0));
//}
//
//float foam(vec2 x) { return foam(61.0, x); }

float foam(float seed, vec3 x) {
    vec4 p = vec4(x.x, 
                  dot(x.xy, vec2(-0.333, 0.942)),
                  dot(x, vec3(-0.333, -0.471,  0.816)),
                  dot(x, vec3(-0.333, -0.471, -0.816)));
    float a = noise(seed, p.yzw);
    float b = noise(seed + 42.1, p.xzw + a * H3.x);
    float c = noise(seed + 84.2, p.xyw + b * H3.y);
    float d = noise(seed + 126.3, p.xyz + c * H3.z);
    return smoothstep(0.0, 1.0, smoothstep(0.0, 1.0, (a + b + c + d) * 0.25));
}

//float foam(vec3 x) { return foam(61.0, x); }
//
//vec3 foam(vec3 seed, vec3 x) {
//    vec4 p = vec4(x.x,
//                  dot(x.xy, vec2(-0.333, 0.942)),
//                  dot(x, vec3(-0.333, -0.471,  0.816)),
//                  dot(x, vec3(-0.333, -0.471, -0.816)));
//    vec3 a = noise(seed, p.yzw);
//    vec3 b = noise(seed + 42.1, p.xzw + a * H3.x);
//    vec3 c = noise(seed + 84.2, p.xyw + b * H3.y);
//    vec3 d = noise(seed + 126.3, p.xyz + c * H3.z);
//    return smoothstep(vec3(0.0), vec3(1.0), smoothstep(vec3(0.0), vec3(1.0), (a + b + c + d) * 0.25));
//}

void main() {
  vec3 i = vec3(gl_FragCoord.xy + 99.0, tm * 0.3125) * 0.0625;
  gl_FragColor.r = foam(4.0 + seed, i);
  gl_FragColor.g = foam(61.0 + seed, i);
  gl_FragColor.b = foam(257.0 + seed, i);
  gl_FragColor.a = v_color.a;
}
