#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_palette;

uniform float seed;
uniform float tm;
const float b_adj = 31.0 / 32.0;
const float rb_adj = 32.0 / 1023.0;
float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = sin((cos(f + seed) * 64.987654321 + seed) * 120.413);
    float end   = sin((cos((f+1.0) + seed) * 64.987654321 + seed) * 120.413);
    return mix(start, end, smoothstep(0.0, 1.0, value - f));
}
vec3 cosmic(vec3 seed, vec3 con)
{
    con.x = swayRandomized(seed.x, con.x + con.z);
    con.y = swayRandomized(seed.y, con.y + con.x);
    con.z = swayRandomized(seed.z, con.z + con.y);
    return con;
}

void main() {
  vec3 alt = vec3(gl_FragCoord.xy, tm) * 0.0075;
  vec3 con = alt.yzx + alt.zxy;
  vec3 s = 10101.101 + 2525.25 * fract(vec3(seed * 0.6180339887498949, seed * 0.7548776662466927, seed * 0.5698402909980532));

  con += cosmic(s.yzx, con.xyz) * 1.7;
  con += cosmic(s.zxy, con.yzx) * 1.7;
  con += cosmic(s.xyz, con.zxy) * 1.7;
  //con += cosmic(s.yzx, con.xyz) * 1.6;
  //con += cosmic(s.zxy, con.yzx) * 1.6;
  //con += cosmic(s.xyz, con.zxy) * 1.25;
//  con += cosmic(s.yzx, con.xyz);
//  con += cosmic(s.zxy, con.yzx);
//  con += cosmic(s.xyz, con.zxy);
//  con += cosmic(s.yzx, con.xyz);
//  con += cosmic(s.zxy, con.yzx);
//  con += cosmic(s.xyz, con.zxy);
    
  vec3 tgt = cos(con * 3.14159265) * 0.5 + 0.5;
  
  vec4 used = texture2D(u_palette, vec2((tgt.b * b_adj + floor(tgt.r * 31.999)) * rb_adj, 1.0 - tgt.g));
  float len = length(tgt) + 1.0;
  float adj = fract(52.9829189 * fract(0.06711056 * gl_FragCoord.x + 0.00583715 * gl_FragCoord.y)) * len - len * 0.5;
  tgt = clamp(tgt + (tgt - used.rgb) * adj, 0.0, 1.0);
  gl_FragColor.rgb = v_color.rgb * texture2D(u_palette, vec2((tgt.b * b_adj + floor(tgt.r * 31.999)) * rb_adj, 1.0 - tgt.g)).rgb;
  gl_FragColor.a = v_color.a;
}
