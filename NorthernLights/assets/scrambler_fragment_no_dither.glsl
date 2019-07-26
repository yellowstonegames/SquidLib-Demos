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
float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = sin((cos(f + seed) * 9.5 + sin(f * 64.987654321) + seed) * 120.413);
    float end   = sin((cos((f+1.0) + seed) * 9.5 + sin((f+1.0) * 64.987654321) + seed) * 120.413);
    return mix(start, end, smoothstep(0.0, 1.0, value - f));
}
vec3 cosmic(vec3 seed, vec3 con)
{
    con.x = swayRandomized(seed.x, con.x);
    con.y = swayRandomized(seed.y, con.y + con.x);//3.14159265;
    con.z = swayRandomized(seed.z, con.z + con.y);//1.5707963267948966;
    return con;
}
void main() {
  vec3 alt = vec3(gl_FragCoord.xy, tm) * 0.0075;
  vec3 con = alt.yzx + alt.zxy;
  vec3 s = vec3(seed * 0.6180339887498949, seed * -0.7548776662466927, seed * 0.5698402909980532);
  vec3 edit = vec3(0.8191725133961645, 0.6710436067037893, 0.5497004779019703);

  con += cosmic(s, con.yzx);
  con += cosmic(s.yzx, con.zxy);
  con += cosmic(s.zxy, con);
  con += cosmic(s * edit, con.yzx);
  con += cosmic(s.yzx * edit, con.zxy);
  con += cosmic(s.zxy * edit, con);

//  vec2 alt = gl_FragCoord.xy * 0.0002;
//  float yt = alt.y - tm;
//  float xt = tm - alt.x;
//  float xy = alt.x + alt.y;
//  vec3 xt3 = vec3(swayRandomized(-16405.31527, xt - 3.11),
//                swayRandomized(77664.8142, 1.41 - xt),
//                swayRandomized(-50993.5190, xt + 2.61));
//  vec3 yt3 = vec3(swayRandomized(-10527.92407, yt - 1.11),
//                swayRandomized(-61557.6687, yt + 2.41),
//                swayRandomized(43527.8990, 3.61 - yt));
//  vec3 xy3 = vec3(swayRandomized(92407.10527, -2.4375 - xy),
//                  swayRandomized(-56687.50993, xy + 1.5625),
//                  swayRandomized(-28142.77664, xy + -3.8125));
//  vec3 con = xy3 * tm * swayRandomized(-11.13 + seed, tm + swayRandomized(17.19 - seed, tm))
//           + yt3 * alt.x * swayRandomized(-23.29 + seed, alt.x + swayRandomized(31.37 - seed, alt.x))
//           + xt3 * alt.y * swayRandomized(-41.47 + seed, alt.y + swayRandomized(53.59 - seed, alt.y));
//  //vec3 con = xy3 * cosmic(123123.456, 1.25 * xt3 + 0.85 * yt3) + yt3 * cosmic(123456.123, 0.75 * xt3 + 1.35 * xy3) + xt3 * cosmic(456123.123, 1.15 * yt3 + 0.65 * xy3);
//  con.x = cosmic(seed, con);
//  con.y = cosmic(seed + 321234.567, con + 1.2345);
//  con.z = cosmic(seed - 654321.123, con + 2.3456);
    
  gl_FragColor.rgb = cos(con * 3.14159265) * 0.5 + 0.5;
  gl_FragColor.a = v_color.a;
}
