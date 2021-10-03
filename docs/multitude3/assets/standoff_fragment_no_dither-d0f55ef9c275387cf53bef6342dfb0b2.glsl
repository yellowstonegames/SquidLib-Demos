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

// harmonious numbers
const vec3 H3 = vec3(0.8191725134, 0.6710436067, 0.5497004779);
const vec4 H4 = vec4(0.8566748838545029, 0.733891856627126, 0.6287067210378087, 0.5385972572236101);
vec3 norl(vec3 seeds, vec3 p) {
    return fract(16.0 * mix(
                 fract(fract(dot((p + seeds), H3) + seeds.yzx) * fract(dot(H3.zxy - seeds.zxy, p.yzx)))
                 , fract(fract(dot((p + H4.wzy), seeds.yzx) + H4.yzx) * fract(dot(seeds.zxy - H4.yxw, p.yzx)))
                 , normalize(p.zxy)));
}
float nrl(vec3 seeds, vec3 p) {
    return fract(32.0 * dot(
                 fract(dot(p + seeds, H3) + seeds.yzx) * fract(dot(H3.zxy - seeds.zxy, p.yzx)),
                 fract(dot(p + H4.wzy, seeds.yzx) + H4.yzx) * fract(dot(seeds.zxy - H4.yxw, p.yzx))));
}

vec3 swayRandomized(vec3 seed, float value)
{
    vec3 f = vec3(floor(value));
    vec3 start = norl(seed, f);
    vec3 end   = norl(seed, f + 1.0);
    return mix(start, end, smoothstep(0.0, 1.0, value - f));
}
vec3 cosmic(vec3 seed, vec3 con)
{
    con.xyz += swayRandomized(seed.yzx, con.z);
    con.yzx += swayRandomized(seed.zxy, con.x);
    con.zxy += swayRandomized(seed.xyz, con.y);
    return con + 0.125;
}

void main() {
  vec3 s = 31.555 + 19.225 * fract(vec3(seed * 0.61803, seed * 0.75488, seed * 0.56984));

  vec3 alt = vec3(gl_FragCoord.xy + 128.0, tm) * 0.015625;
  vec3 con = (alt.yzx + alt.zxy);

  con.yzx += cosmic(s.zxy, con.xyz);
  con.zxy += cosmic(s.xyz, con.yzx);
  con.xyz += cosmic(s.yzx, con.zxy);

  gl_FragColor.rgb = cos(con * 3.14159265) * 0.5 + 0.5;
  gl_FragColor.a = v_color.a;
}
