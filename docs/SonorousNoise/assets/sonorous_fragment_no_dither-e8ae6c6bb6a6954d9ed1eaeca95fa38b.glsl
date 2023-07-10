#ifdef GL_ES
#define LOWP lowp
precision highp float;
#else
#define LOWP 
#endif

const float divisions = 5.0;
const float PHI = 0.61803398874989484820459; // phi, the Golden Ratio
const vec2 H2 = vec2(1.324717957244746, 1.754877666246693); // harmonious numbers for 2D
const vec3 H3 = vec3(0.8191725134, 0.6710436067, 0.5497004779); // harmonious numbers for 3D
const vec4 H4 = vec4(0.8566748838545, 0.7338918566271, 0.6287067210378, 0.5385972572236); // harmonious numbers for 4D

// Based on a Shadertoy: https://www.shadertoy.com/view/4dS3Wd
// By Morgan McGuire @morgan3d, http://graphicscodex.com
// Reuse permitted under the BSD license.

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform float u_seed;
uniform float u_time;
uniform vec2 u_resolution;

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
    float b = noise(seed + 23.1, vec4(x.x, p.yzw) + a * H4.x);
    float c = noise(seed + 46.2, vec4(x.x, p.xzw) + b * H4.y);
    float d = noise(seed + 69.3, vec4(x.x, p.xyw) + c * H4.z);
    float e = noise(seed + 92.4, vec4(x.x, p.xyz) + d * H4.w);
    return smoothstep(0.0, 1.0, smoothstep(0.0, 1.0, (a + b + c + d + e) * 0.2));
}

float ridged(float seed, vec4 x) {
  float f = foam(seed, x);
  return 1. - abs(1. - 2. * f);
}
// Credit to Andrey-Postelzhuk,
// https://forum.unity.com/threads/hue-saturation-brightness-contrast-shader.260649/
vec3 rodriguesHue(vec3 rgb, float hue)
{
    vec3 k = vec3(0.57735);
    float c = cos(hue);
    //Rodrigues' rotation formula
    return rgb * c + cross(k, rgb) * sin(hue) + k * dot(k, rgb) * (1.0 - c);
}

void main() {
  if(texture2D(u_texture, v_texCoords).a <= 0.) discard;
  vec2 center = (gl_FragCoord.xy - u_resolution * 0.5) / 400.;
  float c = u_time;
  float theta = atan(center.y, center.x) * divisions;
  float len = length(center);
  vec2 shrunk = vec2((len + 0.125 * sin(c - len)) / divisions, 32.0);
  float adj = (len * 16. - c) * (1.0 / (256.0));
  vec2 pos = vec2(theta, adj);
  vec4 i = vec4(sin(pos) * shrunk, cos(pos) * shrunk) * 16.;
  gl_FragColor.r = ridged(4.3 + u_seed, i);
  gl_FragColor.gb = vec2(0.0);
  gl_FragColor.rgb = rodriguesHue(gl_FragColor.rgb, c * 0.4 - (len * 3.0 - sin(gl_FragColor.r * 7.) * 2.0));
//  gl_FragColor.g = foam(61.6 + u_seed, i);//i.ywxz);
//  gl_FragColor.b = foam(257.9 + u_seed, i);//i.zxwy);
  gl_FragColor.a = v_color.a;
}
