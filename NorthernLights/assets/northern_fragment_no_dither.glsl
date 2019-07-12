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
  float yt = gl_FragCoord.y * 0.0025 - tm;
  float xt = tm - gl_FragCoord.x * 0.0025;
  float xy = (gl_FragCoord.x + gl_FragCoord.y) * 0.0025;
  vec3 s = vec3(swayRandomized(-16405.31527, xt - 1.11),
                swayRandomized(77664.8142, 1.41 - xt),
                swayRandomized(-50993.5190, xt + 2.61)) * 0.00625;
  vec3 c = vec3(swayRandomized(-10527.92407, yt - 1.11),
                swayRandomized(-61557.6687, yt + 1.41),
                swayRandomized(43527.8990, 2.61 - yt)) * 0.00625;
  vec3 con = vec3(swayRandomized(92407.10527, -2.4375 - xy),
                  swayRandomized(-56687.50993, xy + 1.5625),
                  swayRandomized(-28142.77664, xy + -3.8125)) * tm + c * gl_FragCoord.x + s * gl_FragCoord.y;
  con.x = cosmic(seed, con);
  con.y = cosmic(seed + 123.456, con);
  con.z = cosmic(seed - 456.123, con);
    
  gl_FragColor.rgb = cos(con * 3.14159265) * 0.5 + 0.5;
  gl_FragColor.a = v_color.a;

}