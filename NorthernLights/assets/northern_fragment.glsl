#version 140
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform int seed;
uniform int tm;

float swayRandomized (int seed, float value) {
  int fl = int(floor(value));
  const float magic = 4.656612874161595E-10;//1.1920930376163766E-7;//0.00000000046566125955216364 * 2.0;
  int shift = seed;
  shift += fl * 0x6C8D;
  float start = ((shift ^ (shift << 11 | (shift >> 21 & 0x7FF))) * (shift | 0xA529)) * magic;
  shift += 0x6C8D;
  float end = ((shift ^ (shift << 11 | (shift >> 21 & 0x7FF))) * (shift | 0xA529)) * magic;
  value -= fl;
  value *= value * (3.0 - 2.0 * value);
  return (1.0 - value) * start + value * end;
}

float swayTight (float value) {
  return sin((value + 0.25) * 3.14159265358979323846) * 0.5 + 0.5;
}

float cosmic (float c0, float c1, float c2) {
  float sum = swayRandomized(seed, c2 + c0);
  sum += swayRandomized(seed, sum + c0 + c1);
  sum += swayRandomized(seed, sum + c1 + c2);
  return sum * 0.33333333333 + 0.5;
}

void main() {
  float magic = 0.00018310546875;
  float ftm = tm * magic;
  float s0 = swayRandomized(0x9E3779B9, ftm - 1.11) * 0.025;
  float c0 = swayRandomized(0xC13FA9A9, ftm - 1.11) * 0.025;
  float s1 = swayRandomized(0xD1B54A32, ftm + 1.41) * 0.025;
  float c1 = swayRandomized(0xDB4F0B91, ftm + 1.41) * 0.025;
  float s2 = swayRandomized(0xE19B01AA, ftm + 2.61) * 0.025;
  float c2 = swayRandomized(0xE60E2B72, ftm + 2.61) * 0.025;
  float conn0, conn1, conn2;
  
  float x = gl_FragCoord.x;
  float y = gl_FragCoord.y;
  conn0 = tm * (0.0004375) + x * c0 - y * s0;
  conn1 = tm * (0.0005625) - x * c1 + y * s1;
  conn2 = tm * (0.0008125) + x * c2 + y * s2;

  conn0 = cosmic(conn0, conn1, conn2);
  conn1 = cosmic(conn0, conn1, conn2);
  conn2 = cosmic(conn0, conn1, conn2);

  float r = swayTight(conn0);
  float g = swayTight(conn1);
  float b = swayTight(conn2);

  gl_FragColor = vec4(r, g, b, 1.0);
}