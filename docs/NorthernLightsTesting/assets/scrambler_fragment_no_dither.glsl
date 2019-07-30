#ifdef GL_ES
#define LOWP lowp
precision highp float;
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
  vec3 s = 5555.555 + 2525.25 * fract(vec3(seed * 0.6180339887498949, seed * 0.7548776662466927, seed * 0.5698402909980532));

  con += cosmic(s.yzx, con.xyz) * 1.6;
  con += cosmic(s.zxy, con.yzx) * 1.6;
  con += cosmic(s.xyz, con.zxy) * 1.6;
  //con += cosmic(s.yzx, con.xyz) * 1.6;
  //con += cosmic(s.zxy, con.yzx) * 1.6;
  //con += cosmic(s.xyz, con.zxy) * 1.25;
//  con += cosmic(s.yzx, con.xyz);
//  con += cosmic(s.zxy, con.yzx);
//  con += cosmic(s.xyz, con.zxy);
//  con += cosmic(s.yzx, con.xyz);
//  con += cosmic(s.zxy, con.yzx);
//  con += cosmic(s.xyz, con.zxy);
    
  gl_FragColor.rgb = cos(con * 3.14159265) * 0.5 + 0.5;
  gl_FragColor.a = v_color.a;
}
