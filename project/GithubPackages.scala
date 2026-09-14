import sbt.*
import sbt.Keys.*

import scala.sys.process.*
import scala.util.Try

object GithubPackages extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  object autoImport {
    val githubOwner = settingKey[String]("The GitHub user or organization that owns the package registry")
    val githubRepository = settingKey[String]("The GitHub repository to publish this project's artifacts to")
    val noPublishSettings = Seq(publish / skip := true)
  }

  import autoImport.*

  def githubToken: String =
    sys.env
      .get("GITHUB_TOKEN")
      .map(_.trim)
      .filter(_.nonEmpty)
      .orElse(Try("git config github.token".!!).toOption.map(_.trim).filter(_.nonEmpty))
      .getOrElse(
        sys.error(
          "GithubPackages: no GitHub token found; set GITHUB_TOKEN or run " +
            "`git config --global github.token <token>`"
        )
      )

  override def projectSettings = Seq(
    // Otherwise the authenticated, owner-wide resolver added below gets
    // baked into the published POM and inherited by consumers, who may
    // have no credentials for it.
    pomIncludeRepository := (_ => false),

    resolvers ++= {
      githubOwner.?.value match {
        case Some(owner) if owner.nonEmpty =>
          Seq("GitHub Package Registry".at(s"https://maven.pkg.github.com/$owner/_"))
        case _ =>
          sLog.value.warn("GithubPackages: `githubOwner` is not set; not adding the GitHub Packages resolver")
          Seq.empty
      }
    },
    credentials ++= {
      githubOwner.?.value match {
        case Some(owner) if owner.nonEmpty =>
          Seq(
            Credentials(
              "GitHub Package Registry",
              "maven.pkg.github.com",
              "_", // username is ignored by GitHub, only the token matters
              githubToken
            )
          )
        case _ =>
          sLog.value.warn("GithubPackages: `githubOwner` is not set; not adding GitHub Packages credentials")
          Seq.empty
      }
    },
    publishTo := {
      def suppressMissingConfigWarning = (publish / skip).value

      val back = for {
        owner <- githubOwner.?.value.filter(_.nonEmpty)
        repo <- githubRepository.?.value.filter(_.nonEmpty)
      } yield {
        if (!publishMavenStyle.value)
          sys.error(
            "GithubPackages: GitHub Packages does not support Ivy-style publication; set publishMavenStyle := true"
          )

        "GitHub Package Registry (publish)".at(s"https://maven.pkg.github.com/$owner/$repo")
      }

      back.orElse {
        if (!suppressMissingConfigWarning)
          sLog.value.warn(
            "GithubPackages: `githubOwner`/`githubRepository` not set; leaving publishTo unchanged"
          )
        publishTo.value
      }
    }
  )

}
