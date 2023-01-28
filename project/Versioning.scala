import coursierapi.{Module, Versions}
import sbt.librarymanagement.CrossVersion
import sbtrelease.Version
import sbtversionpolicy.Compatibility

object Versioning {

  private val versions = Versions.create()

  /**
    * @param module             Module name (without “cross-version suffix”), e.g. "algebra"
    * @param crossVersion       CrossVersion of the module
    * @param scalaBinaryVersion Scala binary version
    * @param scalaFullVersion   Scala full version
    * @return The latest stable release of the module.
    */
  def lastVersion(module: String, crossVersion: CrossVersion, scalaBinaryVersion: String, scalaFullVersion: String): String = {
    val artifactName =
      CrossVersion(crossVersion, scalaFullVersion, scalaBinaryVersion)
        .getOrElse(sys.error(s"Unable to compute the artifact name of the module ${module}"))
        .apply(module)
    val version =
      versions
        .withModule(Module.of("org.endpoints4s", artifactName))
        .versions()
        .getMergedListings
        .getRelease
    if (version.isEmpty) "0.0.0" // fallback in case the module has not been published yet
    else version
  }

  /**
    * @return The version of the given module such that it complies with the
    *         intended compatibility guarantees.
    *
    * By default, it adds the suffix "+n" to the latest release version of the module.
    * However, in “release mode” (if the environment variable `RELEASE` is set to `true`)
    * it computes the release version by bumping the appropriate version numbers
    * according to the intended compatibility guarantees.
    *
    * It is possible to add a qualifier to the computed version by setting the environment
    * variable `VERSION_QUALIFIER`. For instance `VERSION_QUALIFIER="-RC1"`
    */
  def computeVersion(module: String, crossVersion: CrossVersion, scalaBinaryVersion: String, scalaFullVersion: String, compatibility: Compatibility): String = {
    val rawVersion = lastVersion(module, crossVersion, scalaBinaryVersion, scalaFullVersion)
    val last = Version(rawVersion).getOrElse(sys.error(s"Unable to parse the version of module ${module}: ${rawVersion}"))
    if (sys.env.get("RELEASE").contains("true")) {
      val qualifier = sys.env.getOrElse("VERSION_QUALIFIER", "")
      val bumpedVersion =
        compatibility match {
          case Compatibility.None => last.bumpMajor
          case Compatibility.BinaryCompatible => last.bumpMinor
          case Compatibility.BinaryAndSourceCompatible => last.bumpBugfix
        }
      bumpedVersion.string + qualifier
    } else {
      last.string + "+n"
    }
  }

}