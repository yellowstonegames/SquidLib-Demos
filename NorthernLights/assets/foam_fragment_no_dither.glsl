#ifdef GL_ES
#define LOWP lowp
precision mediump float;
const float PHI = 1.61803; // phi, the Golden Ratio
const vec2 H2 = vec2(1.32471, 1.75487); // harmonious numbers for 2D
const vec3 H3 = vec3(0.81917, 0.67104, 0.5497); // harmonious numbers for 3D
#else
#define LOWP 
const float PHI = 1.61803398874989484820459; // phi, the Golden Ratio
const vec2 H2 = vec2(1.324717957244746, 1.754877666246693); // harmonious numbers for 2D
const vec3 H3 = vec3(0.8191725134, 0.6710436067, 0.5497004779); // harmonious numbers for 3D
#endif

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

vec3 applyHue(vec3 rgb) {
    float hue = 0.25 * tm;
    vec3 k = vec3(0.57735);
    float c = cos(hue);
    //Rodrigues' rotation formula
    return rgb * c + cross(k, rgb) * sin(hue) + k * dot(k, rgb) * (1.0 - c);
}

// Hash without Sine, https://www.shadertoy.com/view/4djSRW
float hashwithoutsine11(float p)
{
    p = fract(p * .1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hashwithoutsine12(vec2 p)
{
	vec3 p3  = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hashwithoutsine13(vec3 p3)
{
    p3  = fract(p3 * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hashwithoutsine21(float p)
{
	vec3 p3 = fract(vec3(p,p,p) * vec3(.1031, .1030, .0973));
	p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}

vec2 hashwithoutsine22(vec2 p)
{
	vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx+33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}

vec2 hashwithoutsine23(vec3 p3)
{
	p3 = fract(p3 * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx+33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}

vec3 hashwithoutsine31(float p)
{
   vec3 p3 = fract(vec3(p,p,p) * vec3(.1031, .1030, .0973));
   p3 += dot(p3, p3.yzx+33.33);
   return fract((p3.xxy+p3.yzz)*p3.zyx);
}

vec3 hashwithoutsine32(vec2 p)
{
	vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yxz+33.33);
    return fract((p3.xxy+p3.yzz)*p3.zyx);
}

vec3 hashwithoutsine33(vec3 p3)
{
	p3 = fract(p3 * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yxz+33.33);
    return fract((p3.xxy + p3.yxx)*p3.zyx);
}

float hash(float seed, float p) {
    return hashwithoutsine11(hashwithoutsine11(p) + seed);
}

float hash(float seed, vec2 p) {
    return hashwithoutsine12(hashwithoutsine22(p) + seed);
}

vec3 hash(vec3 seed, float p) {
    return hashwithoutsine33(hashwithoutsine31(p) + seed);
}

vec3 hash(vec3 seed, vec3 p) {
    return hashwithoutsine33(hashwithoutsine33(p) + seed);
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

float noise(vec3 x) {
    return noise(42.0, x);
}

vec3 noise(vec3 seed, vec3 x) {
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
    float b = noise(seed + 42.1, p.xzw + a * H3.x);
    float c = noise(seed + 84.2, p.xyw + b * H3.y);
    float d = noise(seed + 126.3, p.xyz + c * H3.z);
    return smoothstep(0.0, 1.0, smoothstep(0.0, 1.0, (a + b + c + d) * 0.25));
}

float foam(vec3 x) { return foam(61.0, x); }


vec3 foam(vec3 seed, vec3 x) {
    vec4 p = vec4(x.x, 
                  dot(x.xy, vec2(-0.3333333333333333, 0.9428090415820634)),
                  dot(x, vec3(-0.3333333333333333, -0.4714045207910317, 0.816496580927726)),
                  dot(x, vec3(-0.3333333333333333, -0.4714045207910317, -0.816496580927726)));
    vec3 a = noise(seed, p.yzw);
    vec3 b = noise(seed + 42.1, p.xzw + a * H3.x);
    vec3 c = noise(seed + 84.2, p.xyw + b * H3.y);
    vec3 d = noise(seed + 126.3, p.xyz + c * H3.z);
    return smoothstep(vec3(0.0), vec3(1.0), smoothstep(vec3(0.0), vec3(1.0), (a + b + c + d) * 0.25));
}

void main() {
  vec3 i = vec3(gl_FragCoord.xy + 1999.0, tm * 0.3125) * 0.0625;
//  gl_FragColor = vec4(applyHue(foam(vec3(42.0, 69.0, 23.0) + seed, i)), v_color.a);
  gl_FragColor.r = foam(42.0 + seed, i);
  gl_FragColor.g = foam(69.0 + seed, i);
  gl_FragColor.b = foam(23.0 + seed, i);
  gl_FragColor.a = v_color.a;
}
