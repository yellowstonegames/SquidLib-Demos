#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

uniform float seed;
uniform float tm;
float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = sin((cos(f + seed) * 12.973 + seed) * 31.413);
    float end   = sin((cos(f + 1.0 + seed) * 12.973 + seed) * 31.413);
    return mix(start, end, smoothstep(0.0, 1.0, value - f)) + 1.1;
}
vec3 cosmic(vec3 seed, vec3 con)
{
    con.x = swayRandomized(seed.x, con.x + con.z);
    con.y = swayRandomized(seed.y, con.y + con.x);
    con.z = swayRandomized(seed.z, con.z + con.y);
    return con;
}

void main() {
  vec3 alt = vec3(gl_FragCoord.xy / 180.0, tm / 160.0);
  vec3 con = alt.yzx + alt.zxy;
  vec3 s = 31.555 + 19.225 * fract(vec3(seed * 0.61803, seed * 0.75488, seed * 0.56984));

  con += cosmic(s.yzx, con.xyz);
  con += cosmic(s.zxy, con.yzx);
  con += cosmic(s.xyz, con.zxy);
  //con += cosmic(s.yzx, con.xyz);
  //con += cosmic(s.zxy, con.yzx);
  //con += cosmic(s.xyz, con.zxy);
//  con += cosmic(s.yzx, con.xyz);
//  con += cosmic(s.zxy, con.yzx);
//  con += cosmic(s.xyz, con.zxy);
//  con += cosmic(s.yzx, con.xyz);
//  con += cosmic(s.zxy, con.yzx);
//  con += cosmic(s.xyz, con.zxy);
    
  gl_FragColor.b = cos(con.b * 3.14159) * 0.125 + 0.875;
  gl_FragColor.r = gl_FragColor.b * 0.2 + sin(con.r * 3.14159) * 0.0625;
  gl_FragColor.g = gl_FragColor.b * 0.5 + sin(con.g * 3.14159) * 0.125;
  gl_FragColor.a = v_color.a;
}
