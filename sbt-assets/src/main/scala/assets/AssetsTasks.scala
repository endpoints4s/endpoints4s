package assets

import sbt._

object AssetsTasks {

  def generateDigests(
    baseDirectory: File,
    targetDirectory: File,
    generatedObjectName: String,
    generatedPackage: Option[String],
    assetsPath: File => File = _ / "src" / "main" / "assets"
  ): Seq[File] = {
      val assetsDirectory = assetsPath(baseDirectory)
      val assets = assetsDirectory.***.get
      val digests = assets.collect {
        case asset if asset.isFile =>
          val hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(IO.readBytes(asset))
          asset.relativeTo(assetsDirectory).get.getPath -> hash
      }
      val scalaCode = {
        val scalaMap =
          digests
            .map { case (name, hash) => s""""${name.replace("\\", "/").replace("\"", "\\\"")}" -> "$hash"""" }
            .mkString("Map(", ", ", ")")
        s"""
           |${generatedPackage.fold("")(name => s"package $name")}
           |
           |object $generatedObjectName {
           |  val digests: Map[String, String] = $scalaMap
           |}
            """.stripMargin
      }
      val scalaFile = targetDirectory / "assets" / s"$generatedObjectName.scala"
      IO.write(scalaFile, scalaCode)
      Seq(scalaFile)
    }

}