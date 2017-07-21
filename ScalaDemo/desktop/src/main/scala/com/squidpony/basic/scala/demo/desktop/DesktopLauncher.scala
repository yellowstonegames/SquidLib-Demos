package com.squidpony.basic.scala.demo.desktop

import com.badlogic.gdx.Files.FileType
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.squidpony.basic.scala.demo.MainApplication

/** Launches the desktop (LWJGL) application. */
object DesktopLauncher {
  def main(args: Array[String]): Unit = createApplication

  private def createApplication = new LwjglApplication(new MainApplication, getDefaultConfiguration)

  private def getDefaultConfiguration = {
    val config = new LwjglApplicationConfiguration
    config.title = "BasicScalaDemo"
    config.allowSoftwareMode = true
    config.width = MainApplication.gridWidth * MainApplication.cellWidth
    config.height = (MainApplication.gridHeight + MainApplication.bonusHeight) * MainApplication.cellHeight
    for (size <- Array[Int](128, 64, 32, 16)) {
      config.addIcon("Tentacle-" + size + ".png", FileType.Internal)
    }
    config
  }
}