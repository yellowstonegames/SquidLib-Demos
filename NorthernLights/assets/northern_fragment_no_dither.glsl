#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
//uniform sampler2D u_palette;

uniform float seed;
uniform float tm;
//uniform vec3 s;
//uniform vec3 c;

const float PHI = 1.618034; // phi, the Golden Ratio

float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = fract(fract((f - seed) * PHI + seed - f * 6.98765) * (PHI + f));
    float end   = fract(fract((f+1.0 - seed) * PHI + seed - (f + 1.0) * 6.98765) * (PHI + f+1.0));
//    float start = sin((cos(f + seed) * 9.5 + sin(f * 64.987654321) + seed) * (120.413));
//    float end   = sin((cos((f+1.) + seed) * 9.5 + sin((f+1.) * 64.987654321) + seed) * (120.413));
    return mix(start, end, smoothstep(0., 1., value - f));
}
float cosmic(float seed, vec3 con)
{
    float sum = swayRandomized(seed, con.x + con.y + con.z) * 3.0;
    return -2.5 + (sum + 2.0 * swayRandomized(-seed, sum * 0.5698402909980532 + 0.7548776662466927 * (con.x + con.y + con.z - sum)));
}
void main() {
//  vec3 xyz = vec3(gl_FragCoord.xy, tm);
  vec2 distort = acos(1.5 * (v_texCoords - 0.5)) * pow(PHI, -2.75 + distance(v_texCoords, vec2(0.5, 0.5))) * 300.0;
  vec3 xyz = vec3(distort.x, (distort.y + 10.0) * sin(tm * (3.14159265 * 0.02)) * 0.3, (distort.y + 10.0) * cos(tm * (3.14159265 * 0.02)) * 0.3);
  vec3 alt = xyz * 0.009 - xyz.yzx * 0.005 + xyz.zxy * 0.003;
  
  float yt = (alt.y * PHI + alt.z - alt.x) * 0.5 * (swayRandomized(123.456 + seed, alt.x * 0.2123) + 1.5);
  float xt = (alt.z * PHI + alt.x - alt.y) * 0.5 * (swayRandomized(seed, alt.y * 0.2123) + 1.5);
  float xy = (alt.x * PHI + alt.y - alt.z) * 0.5 * (swayRandomized(789.123 - seed, alt.z * 0.2123) + 1.5);
  vec3 s = vec3(swayRandomized(-164.31527, xt - 3.11),
                swayRandomized(776.8142, 1.41 - xt),
                swayRandomized(-509.5190, xt + 2.61)) - 0.5;
  vec3 c = vec3(swayRandomized(-105.92407, yt - 1.11),
                swayRandomized(-615.6687, yt + 2.41),
                swayRandomized(435.8990, 3.61 - yt)) - 0.5;
  vec3 con = -swayRandomized(-seed, xyz.z * -0.04)
             + ((length(s) + length(c) + PHI)) * (vec3(
                  swayRandomized(924.10527, -2.4375 - xy),
                  swayRandomized(-566.50993, xy + 1.5625),
                  swayRandomized(-281.77664, xy + -3.8125))
                   * swayRandomized(1111.11 + seed, alt.z) + c * swayRandomized(11.1111 + seed, alt.x) + s * swayRandomized(111.111 + seed, alt.y));
  con.x = cosmic(seed, con);
  con.y = cosmic(seed + 123.456, con + PHI);
  con.z = cosmic(seed - 456.123, con - PHI);
      
  gl_FragColor.rgb = sin(con * 3.14159265) * 0.5 + 0.5;
  gl_FragColor.a = acos(distance(v_texCoords, vec2(0.5, 0.5)) * 2.5) * 0.6366197723675814;
}
