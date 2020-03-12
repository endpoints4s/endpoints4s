package assets

import sbt._

object AssetsTasks {

  /**
    * Generates a Scala source file containing the assets digests.
    * The file is generated in a “assets” subdirectory, in the project’s target directory.
    *
    * The generated code contains a singleton object defining a Map of (path, digest).
    *
    * @param baseDirectory Project’s base directory
    * @param targetDirectory Project’s target directory
    * @param generatedObjectName Name of the object to generate
    * @param generatedPackage Name of the package in which the object will be generated
    * @param assetsPath A function that, applied to the base directory, returns the directory that contains the assets
    * @return The generated Scala file.
    */
  def generateDigests(
      baseDirectory: File,
      targetDirectory: File,
      generatedObjectName: String,
      generatedPackage: Option[String],
      assetsPath: File => File = _ / "src" / "main" / "assets"
  ): Seq[File] = {
    val assetsDirectory = assetsPath(baseDirectory)
    val assets = assetsDirectory.allPaths.get
    val digests = assets.collect {
      case asset if asset.isFile =>
        val hash = org.apache.commons.codec.digest.DigestUtils
          .md5Hex(IO.readBytes(asset))
        asset.relativeTo(assetsDirectory).get.getPath -> hash
    }
    val scalaCode = {
      val scalaMap =
        digests
          .map {
            case (name, hash) =>
              s""""${name
                .replace("\\", "/")
                .replace("\"", "\\\"")}" -> "$hash""""
          }
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

  /**
    * Creates a gzipped copy of the assets.
    * The copies are written in the project’s target directory, in a sub-directory “gzipped-assets”
    *
    * @param baseDirectory Base directory of the sbt project
    * @param targetDirectory Target directory of the project
    * @param assetsPath A function that, applied to the base directory, returns the directory containing the assets
    * @return The gzipped assets.
    */
  def gzipAssets(
      baseDirectory: File,
      targetDirectory: File,
      assetsPath: File => File = _ / "src" / "main" / "assets"
  ): Seq[File] = {
    val assetsDirectory = assetsPath(baseDirectory)
    val assets = assetsDirectory.allPaths.get
    assets.collect {
      case asset if asset.isFile =>
        val assetPath = asset.relativeTo(assetsDirectory).get.getPath
        val gzippedAsset = targetDirectory / "gzipped-assets" / s"$assetPath.gz"
        IO.gzip(asset, gzippedAsset)
        gzippedAsset
    }
  }

}
