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
//uniform vec3 s;
//uniform vec3 c;
const float b_adj = 31.0 / 32.0;
const float rb_adj = 32.0 / 1023.0;
float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = sin((cos(f * seed) + sin(f * 1024.)) * (420.413));
    float end   = sin((cos((f+1.) * seed) + sin((f+1.) * 1024.)) * (420.413));
    return mix(start, end, smoothstep(0., 1., value - f));
}
float cosmic(float seed, vec3 con)
{
    float sum = swayRandomized(seed, con.z + con.x) + 2.0;
    return sum + 1.75f * swayRandomized(-seed, sum * 0.5698402909980532 + 0.7548776662466927 * (con.x - con.y - con.z));
    //sum = sum + swayRandomized(seed, con.x + con.y + sum);
    //sum = sum + swayRandomized(seed, con.y + con.z + sum);
    //return sum * 0.3333333333;
}
void main() {
  float yt = gl_FragCoord.y * 0.00375 - tm;
  float xt = tm + gl_FragCoord.x * 0.00375;
  float xy = (gl_FragCoord.x - gl_FragCoord.y) * 0.00125;
  vec3 s = vec3(swayRandomized(-16405.31527, xt - 1.11),
                swayRandomized(77664.8142, 1.41 - xt),
                swayRandomized(-50993.5190, xt + 2.61)) * 0.005;
  vec3 c = vec3(swayRandomized(-10527.92407, yt - 1.11),
                swayRandomized(-61557.6687, yt + 1.41),
                swayRandomized(43527.8990, 2.61 - yt)) * 0.005;
  vec3 con = vec3(swayRandomized(92407.10527, -2.4375 - xy),
                  swayRandomized(-56687.50993, xy + 1.5625),
                  swayRandomized(-28142.77664, xy + -3.8125)) * tm + c * gl_FragCoord.x + s * gl_FragCoord.y;
  con.x = cosmic(seed, con);
  con.y = cosmic(seed + 12.3456, con);
  con.z = cosmic(seed - 45.6123, con);
    
  vec3 tgt = sin(con * 3.14159265) * 0.5 + 0.5;
  vec4 used = texture2D(u_palette, vec2((tgt.b * b_adj + floor(tgt.r * 31.999)) * rb_adj, 1.0 - tgt.g));
  float len = length(tgt) + 1.0;
  float adj = fract(52.9829189 * fract(0.06711056 * gl_FragCoord.x + 0.00583715 * gl_FragCoord.y)) * len - len * 0.5;
  //float adj = sin(dot(gl_FragCoord.xy, vec2(4.743036261279236, 3.580412143837574))) * 1.1 + 0.05;
  tgt = clamp(tgt + (tgt - used.rgb) * adj, 0.0, 1.0);
  gl_FragColor.rgb = v_color.rgb * texture2D(u_palette, vec2((tgt.b * b_adj + floor(tgt.r * 31.999)) * rb_adj, 1.0 - tgt.g)).rgb;
  gl_FragColor.a = v_color.a;

}