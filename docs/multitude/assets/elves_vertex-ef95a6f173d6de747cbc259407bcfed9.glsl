attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
uniform mat4 u_projTrans;
uniform float u_seed;
uniform float u_time;
varying vec4 v_color;
varying vec2 v_texCoords;
varying float v_time;
varying vec3 v_s;
varying vec3 v_c;

const vec3 adj = vec3(-1.11, 1.41, 1.61);

vec3 swayRandomized(vec3 seed, vec3 value)
{
    return sin(seed.xyz + value.zxy - cos(seed.zxy + value.yzx) + cos(seed.yzx + value.xyz));
}

void main()
{
   v_color = a_color;
   v_color.a = v_color.a * (255.0/254.0);
   v_texCoords = vec2(a_texCoord0.x, a_texCoord0.y);

   v_time = u_time * 0.0625;
   v_s = (swayRandomized(vec3(34.0, 76.0, 59.0), v_time + adj)) * 0.25;
   v_c = (swayRandomized(vec3(27.0, 67.0, 45.0), v_time - adj)) * 0.25;

   gl_Position =  u_projTrans * a_position;

}
