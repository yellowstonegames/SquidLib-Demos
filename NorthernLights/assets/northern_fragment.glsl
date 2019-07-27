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
    float start = sin((cos(f + seed) * 9.5 + sin(f * 64.987654321) + seed) * (120.413));
    float end   = sin((cos((f+1.) + seed) * 9.5 + sin((f+1.) * 64.987654321) + seed) * (120.413));
    return mix(start, end, smoothstep(0., 1., value - f));
}
float cosmic(float seed, vec3 con)
{
    float sum = swayRandomized(seed, con.x + con.y + con.z) * 1.5;
    return sum + swayRandomized(-seed, sum * 0.5698402909980532 + 0.7548776662466927 * (con.x - con.y - con.z));
}
void main() {
  vec3 alt = vec3(gl_FragCoord.xy, tm) * 0.00325;
  float yt = alt.y + alt.z;
  float xt = alt.z + alt.x;
  float xy = alt.x + alt.y;
  vec3 s = vec3(swayRandomized(-16405.31527, xt - 3.11),
                swayRandomized(77664.8142, 1.41 - xt),
                swayRandomized(-50993.5190, xt + 2.61));
  vec3 c = vec3(swayRandomized(-10527.92407, yt - 1.11),
                swayRandomized(-61557.6687, yt + 2.41),
                swayRandomized(43527.8990, 3.61 - yt));
  vec3 con = (1.251234567 * (length(s) + length(c))) * (vec3(swayRandomized(92407.10527, -2.4375 - xy),
                  swayRandomized(-56687.50993, xy + 1.5625),
                  swayRandomized(-28142.77664, xy + -3.8125))
                   * swayRandomized(111111.111 + seed, alt.z) + c * swayRandomized(11111.1111 + seed, alt.x) + s * swayRandomized(111.111111 + seed, alt.y));
                   //* (sin(xt) + cos(yt)) + c * (sin(xy) + cos(xt)) + s * (sin(yt) + cos(xy)));
                   //* swayRandomized(111111.111 + seed, xt + yt) + c * swayRandomized(11111.1111 + seed, xy + xt) + s * swayRandomized(111.111111 + seed, yt + xy));
                   //* sin(tm) + c * sin(alt.x) + s * sin(alt.y));
  con.x = cosmic(seed, con);
  con.y = cosmic(seed + 123.456, con);
  con.z = cosmic(seed - 456.123, con);
      
  vec3 tgt = cos(con * 3.14159265) * 0.5 + 0.5;
  
  vec4 used = texture2D(u_palette, vec2((tgt.b * b_adj + floor(tgt.r * 31.999)) * rb_adj, 1.0 - tgt.g));
  float len = length(tgt) + 1.0;
  float adj = fract(52.9829189 * fract(0.06711056 * gl_FragCoord.x + 0.00583715 * gl_FragCoord.y)) * len - len * 0.5;
  tgt = clamp(tgt + (tgt - used.rgb) * adj, 0.0, 1.0);
  gl_FragColor.rgb = v_color.rgb * texture2D(u_palette, vec2((tgt.b * b_adj + floor(tgt.r * 31.999)) * rb_adj, 1.0 - tgt.g)).rgb;
  gl_FragColor.a = v_color.a;
}