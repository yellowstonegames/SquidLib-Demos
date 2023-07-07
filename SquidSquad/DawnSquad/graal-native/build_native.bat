C:\d\jvm\graal17-new\bin\native-image.cmd ^
-H:IncludeResources="(dawnlike/.*)|(.*\.dll)" ^
--no-fallback ^
 -H:ReflectionConfigurationFiles=reflect-config.json ^
-jar ../lwjgl3/build/libs/DawnSquad-0.0.1.jar