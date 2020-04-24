#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

// This Shadertoy ( https://www.shadertoy.com/view/wssBz8 ) shows "Foam Noise" by Tommy Ettinger.
// It's just value noise that's rotated and domain warps the next result.

// Based on a Shadertoy: https://www.shadertoy.com/view/4dS3Wd
// By Morgan McGuire @morgan3d, http://graphicscodex.com
// Reuse permitted under the BSD license.

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
//uniform sampler2D u_palette;

uniform float seed;
uniform float tm;
//uniform vec3 s;
//uniform vec3 c;

const vec3 hm = vec3(.8191725133961645, .6710436067037893, .5497004779019703);

    //// old, slow
//    return fract(dot(sin(seed + hm + p), (seed + hm * p)));

float hash(float seed, float p) {
    //// thanks mgsx! https://www.mgsx.net/2015/07/21/006-011-fm-3D-WIP.html
    return fract(sin((p+seed) * 12.9898) * 43758.5453 + (p+seed) * 2.6180339887498949);
}

float hash(float seed, vec2 p) {
    //return fract(length(hm * p.xyx + seed * p.yxy));
    //// faster way, seems better-distributed than the 1D noise, thanks again mgsx
    return fract(sin(seed + dot(p, vec2(12.9898,78.233))) * 43758.5453 + seed * 2.6180339887498949);
}

float noise(float seed, float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash(seed, i), hash(seed, i + 1.0), u);
}

float noise(float x) {
    return noise(42.0, x);
}

float noise(float seed, vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);

	// Four corners in 2D of a tile
	float a = hash(seed, i);
    float b = hash(seed, i + vec2(1.0, 0.0));
    float c = hash(seed, i + vec2(0.0, 1.0));
    float d = hash(seed, i + vec2(1.0, 1.0));
    
    vec2 u = f * f * (3.0 - 2.0 * f);
	return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float noise(vec2 x) {
    return noise(42.0, x);
}


float noise(float seed, vec3 x) {
    const vec3 step = vec3(110.0, 241.0, 171.0);

    vec3 i = floor(x);
    vec3 f = fract(x);
 
    // For performance, compute the base input to a 1D hash from the integer part of the argument and the 
    // incremental change to the 1D based on the 3D -> 1D wrapping
    float n = dot(i, step);

    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix( hash(seed, n                              ), hash(seed, n + dot(step, vec3(1., 0., 0.))), u.x),
                   mix( hash(seed, n + dot(step, vec3(0., 1., 0.))), hash(seed, n + dot(step, vec3(1., 1., 0.))), u.x), u.y),
               mix(mix( hash(seed, n + dot(step, vec3(0., 0., 1.))), hash(seed, n + dot(step, vec3(1., 0., 1.))), u.x),
                   mix( hash(seed, n + dot(step, vec3(0., 1., 1.))), hash(seed, n + dot(step, vec3(1., 1., 1.))), u.x), u.y), u.z);
}

float noise(vec3 x) {
    return noise(42.0, x);
}

float foam(float seed, float x) {
    return noise(seed, x);
}

float foam(float x) { return foam(61.0, x); }

float foam(float seed, vec2 x) {
    vec3 p = vec3(x.x, dot(x, vec2(-0.5, 0.8660254037844386)), dot(x, vec2(-0.5, -0.8660254037844386)));
    float a = noise(seed, p.yz);
    float b = noise(seed + 42.1, p.xz + a);
    float c = noise(seed + 84.2, p.xy + b);
    return smoothstep(0.0, 1.0, (a + b + c) * (1.0 / 3.0));
}

float foam(vec2 x) { return foam(61.0, x); }

float foam(float seed, vec3 x) {
    vec4 p = vec4(x.x, 
                  dot(x.xy, vec2(-0.3333333333333333, 0.9428090415820634)),
                  dot(x, vec3(-0.3333333333333333, -0.4714045207910317, 0.816496580927726)),
                  dot(x, vec3(-0.3333333333333333, -0.4714045207910317, -0.816496580927726)));
    float a = noise(seed, p.yzw);
    float b = noise(seed + 42.1, p.xzw + a);
    float c = noise(seed + 84.2, p.xyw + b);
    float d = noise(seed + 126.3, p.xyz + c);
    return smoothstep(0.0, 1.0, smoothstep(0.0, 1.0, (a + b + c + d) * 0.25));
    
}
float foam(vec3 x) { return foam(61.0, x); }
void main() {
  vec3 i = vec3(gl_FragCoord.xy + 1999.0, tm * 0.3125) * 0.0625;
  gl_FragColor.r = foam(420.0 + seed, i);
  gl_FragColor.g = foam(69.0 + seed, i);
  gl_FragColor.b = foam(666.0 + seed, i);
  gl_FragColor.a = v_color.a;
}
