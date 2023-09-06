C:\d\jvm\graal17\bin\native-image.cmd ^
-H:IncludeResources="(dawnlike/.*)|(.*\.dll)|(com/badlogic/gdx/utils/.*\.(png|fnt))" ^
-march=compatibility ^
-jar ../lwjgl3/build/libs/DawnSquad.jar