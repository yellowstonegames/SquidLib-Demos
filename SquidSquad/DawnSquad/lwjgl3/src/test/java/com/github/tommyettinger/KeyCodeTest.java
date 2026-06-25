package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class KeyCodeTest {
    public static void main(String[] args){
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                StringBuilder buffer = new StringBuilder();
                int enumOrdinal = 0;
                int[] glfw2ordinal = new int[GLFW.GLFW_KEY_LAST + 1];
                int[] gdx2ordinal = new int[Input.Keys.MAX_KEYCODE + 1];
                String[] ordinal2Name = new String[512];
                Arrays.fill(glfw2ordinal, -1);
                Arrays.fill(gdx2ordinal, -1);

                for (int i = GLFW.GLFW_KEY_SPACE; i <= GLFW.GLFW_KEY_LAST; i++) {
                    int glfwScancode = GLFW.glfwGetKeyScancode(i);
                    int glfwKeycode = i;
                    int gdxKeycode = ((DefaultLwjgl3Input) Gdx.input).getGdxKeyCode(glfwKeycode);
                    String glfwKeyname = GLFW.glfwGetKeyName(i, GLFW.GLFW_KEY_UNKNOWN);

                    if (glfwScancode == -1) {
                        continue;
                    }

                    glfw2ordinal[glfwKeycode] = enumOrdinal;

                    if (gdxKeycode != Input.Keys.UNKNOWN) {
                        gdx2ordinal[gdxKeycode] = enumOrdinal;
                    }

                    String enumName = "KEY_" + i;

                    ordinal2Name[enumOrdinal] = enumName;
                    enumOrdinal++;

                    if (glfwKeyname != null) {
                        glfwKeyname = "\"" + glfwKeyname + "\"";
                    }

                    buffer.append(enumName + "(" + glfwKeycode + ", " + glfwScancode + ", " + glfwKeyname + ", " + gdxKeycode + "),\n");
                }

                System.out.println(buffer.toString());
                System.out.println("XXX[] GLFW_LOOKUP_TABLE = new XXX[] {" + String.join(", ", Arrays.stream(glfw2ordinal).mapToObj(x -> x < 0 ? "null" : ordinal2Name[x]).toArray(String[]::new)) + "};");
                System.out.println("XXX[] GDX_LOOKUP_TABLE = new XXX[] {" + String.join(", ", Arrays.stream(gdx2ordinal).mapToObj(x -> x < 0 ? "null" : ordinal2Name[x]).toArray(String[]::new)) + "};");

            }
        });
    }
}
