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

const float PHI = 1.618034; // phi, the Golden Ratio

float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = fract(fract((f - seed) * PHI + seed - f * 6.98765) * (PHI + f));
    float end   = fract(fract((f+1.0 - seed) * PHI + seed - (f + 1.0) * 6.98765) * (PHI + f+1.0));
    return mix(start, end, smoothstep(0., 1., value - f));
}
float cosmic(float seed, vec4 con)
{
    float sum = swayRandomized(seed, con.x - con.y + con.z - con.w) * 0.6;
    return (sum + 0.4 * swayRandomized(-seed, sum * 0.5698402909980532 + 0.7548776662466927 * (con.y - con.x + con.w - con.z - sum)));
}
void main() {
  vec2 circle = vec2(cos(tm * 3.14159265), sin(tm * 3.14159265)) * 10.0;
//  vec4 xyzw = vec4(gl_FragCoord.xy, circle);
  vec2 distort = acos(1.25 * (v_texCoords - 0.5)) * pow(PHI, -2.75 + distance(v_texCoords, vec2(0.5, 0.5))) * 100.0;
  vec4 xyzw = vec4(distort.x + cos(distort.y * 0.0107 + circle.x * 0.15),
                   (distort.y + 10.0) * sin(distort.x * 0.0131 + circle.x * 0.25),
                   (distort.y - 10.0) * cos(circle.y * 0.2) + distort.x * 0.017,
                   (distort.x - 15.0) * sin(circle.y * 0.1) + distort.y * 0.019
                   );
  vec4 alt = xyzw * 0.009 - xyzw.ywxz * 0.005 + xyzw.zxwy * 0.003;
  
  float xx = (alt.y * PHI + alt.z - alt.w) * 0.5 * (swayRandomized(123.456 + seed, alt.x * 0.2123) + 1.5);
  float yy = (alt.z * PHI + alt.w - alt.x) * 0.5 * (swayRandomized(seed, alt.y * 0.2123) + 1.5);
  float zz = (alt.w * PHI + alt.x - alt.y) * 0.5 * (swayRandomized(789.123 - seed, alt.z * 0.2123) + 1.5);
  float ww = (alt.x * PHI + alt.y - alt.z) * 0.5 * (swayRandomized(5.432 - seed, alt.w * 0.2123) + 1.5);
  vec4 alpha = vec4(swayRandomized(-164.3152, xx - 3.11),
                swayRandomized(776.8142, 1.41 - xx),
                swayRandomized(376.1472, -2.13 - xx),
                swayRandomized(-509.5190, xx + 2.61)) - 0.5;
  vec4 beta = vec4(swayRandomized(-105.9240, -1.11 - yy),
                swayRandomized(-615.6687, yy - 2.41),
                swayRandomized(215.6687, yy + 3.08),
                swayRandomized(435.8990, 3.61 - yy)) - 0.5;
  vec4 gamma = vec4(swayRandomized(205.9240, zz + 2.58),
                swayRandomized(195.3755, zz - 1.93),
                swayRandomized(-111.8771, 3.28 - zz),
                swayRandomized(435.8990, -1.42 - zz)) - 0.5;
  vec4 con = -swayRandomized(-seed, (xyzw.z + xyzw.w) * -0.03)
             + ((length(alpha) + length(beta) + length(gamma) + PHI)) * (vec4(
                  swayRandomized(924.1052, -2.4375 - ww),
                  swayRandomized(-566.5093, ww + 1.5625),
                  swayRandomized(613.9617, 2.742 - ww),
                  swayRandomized(-281.77664, ww + -3.8125))
                   * swayRandomized(11111.1 + seed, alt.z)
                   + alpha * swayRandomized(11.1111 + seed, alt.w)
                   + beta * swayRandomized(111.111 + seed, alt.x)
                   + gamma * swayRandomized(1111.11 + seed, alt.y));
  con.x = cosmic(seed, con * 3.0);
  con.y = cosmic(seed + 123.456, con * 3.0 + PHI);
  con.z = cosmic(seed - 456.123, (con + PHI) * 3.0);
  con.w = cosmic(seed + 789.789, (con * PHI) * 3.0);
      
  gl_FragColor.rgb = mix(con.zxy, sin(con.ywx * 3.14159265), cos(con.wzy * 7.0 + con.zxw * 5.0) + 0.5);
  gl_FragColor.a = 1.0;// acos(distance(v_texCoords, vec2(0.5, 0.5)) * 2.5) * 0.6366197723675814;
}
