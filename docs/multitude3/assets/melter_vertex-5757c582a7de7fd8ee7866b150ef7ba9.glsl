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
varying vec3 v_coeffs;
varying vec3 v_section;

const vec3 adj = vec3(-1.11, 1.41, 1.61);

// Quilez Basic Noise, from https://www.shadertoy.com/view/3sd3Rs (MIT-license)
vec3 bas(vec3 sc, vec3 x)
{
    // setup
    vec3 i = floor(x);
    vec3 f = fract(x);
    vec3 s = sign(fract(x/2.0)-0.5);

    // use some hash to create a random value k in [0..1] from i
    vec3 k = fract(sc * i + i.yzx);

    // quartic polynomial
    return s*f*(f-1.0)*((16.0*k-4.0)*f*(f-1.0)-1.0);
}

// this is different from other swayRandomized in Northern demos because it uses Quilez basic noise instead of trig
vec3 swayRandomized(vec3 sc, vec3 seed, vec3 value)
{
    return bas(sc, seed.xyz + value.zxy - bas(sc, seed.zxy + value.yzx) + bas(sc, seed.yzx + value.xyz));
}

void main()
{
   v_color = a_color;
   v_color.a = v_color.a * (255.0/254.0);
   v_texCoords = vec2(a_texCoord0.x, a_texCoord0.y);

   v_time = u_time * 0.03125;
   v_coeffs = fract((u_seed + 23.4567) * vec3(0.8191, 0.671, 0.5497)) + 0.5;
   v_section = fract(v_coeffs.zxy - v_coeffs.yzx * 1.618);

   v_s = (swayRandomized(v_section, vec3(34.0, 76.0, 59.0), v_time + adj)) * 0.25;
   v_c = (swayRandomized(v_section, vec3(27.0, 67.0, 45.0), v_time - adj)) * 0.25;

   gl_Position =  u_projTrans * a_position;

}
