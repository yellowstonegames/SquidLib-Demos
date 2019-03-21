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
uniform vec3 s;
uniform vec3 c;
float swayRandomized(float seed, float value)
{
    float f = floor(value);
    float start = sin((cos(f * seed) + sin(f * 1024.)) * 345. + seed);
    float end   = sin((cos((f+1.) * seed) + sin((f+1.) * 1024.)) * 345. + seed);
    return mix(start, end, smoothstep(0., 1., value - f));
}

float cosmic(float seed, vec3 con)
{
    float sum = swayRandomized(seed, con.z + con.x);
    sum = sum + swayRandomized(seed, con.x + con.y + sum);
    sum = sum + swayRandomized(seed, con.y + con.z + sum);
    return sum * 0.3333333333;
}

void main() {
  //vec3 s = vec3(swayRandomized(-16405.31527, tm - 1.11),
  //              swayRandomized(-77664.8142, tm + 1.41),
  //              swayRandomized(-50993.5190, tm + 2.61)) * 5.;
  //vec3 c = vec3(swayRandomized(-10527.92407, tm - 1.11),
  //              swayRandomized(-61557.6687, tm + 1.41),
  //              swayRandomized(-43527.8990, tm + 2.61)) * 5.;
  vec3 con = vec3(0.0004375, 0.0005625, 0.0008125) * tm  + c * v_texCoords.x + s * v_texCoords.y;
  con.x = cosmic(seed, con);
  con.y = cosmic(seed + 12.3456, con);
  con.z = cosmic(seed - 45.6123, con);
    
  vec3 tgt = sin(con * 3.14159265) * 0.5 + 0.5;
  vec4 used = texture2D(u_palette, vec2((tgt.b + floor(tgt.r * 31.99999)) * 0.03125, 1.0 - tgt.g));
  float adj = sin(dot(gl_FragCoord.xy, vec2(4.743036261279236, 3.580412143837574))) * 1.1 + 0.05;
  tgt = clamp(tgt + (used.rgb - tgt) * adj, 0.0, 1.0);
  gl_FragColor.rgb = v_color.rgb * texture2D(u_palette, vec2((tgt.b + floor(tgt.r * 31.99999)) * 0.03125, 1.0 - tgt.g)).rgb; //(tgt.b + floor(tgt.r * 32.0)) * 0.03125, tgt.g
  gl_FragColor.a = v_color.a;

}