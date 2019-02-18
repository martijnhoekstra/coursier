package coursier

import java.io.File
import java.lang.{Boolean => JBoolean}

import coursier.cache.Cache
import coursier.core.{Classifier, Type}
import coursier.error.CoursierError
import coursier.params.ResolutionParams
import coursier.util.{Schedulable, Task}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object Fetch {

  def fetchIO[F[_]](
    dependencies: Seq[Dependency],
    repositories: Seq[Repository] = Resolve.defaultRepositories,
    resolutionParams: ResolutionParams = ResolutionParams(),
    cache: Cache[F] = Cache.default,
    classifiers: Set[Classifier] = Set(),
    mainArtifacts: JBoolean = null,
    artifactTypes: Set[Type] = core.Resolution.defaultTypes,
    beforeResolutionLogging: () => Unit = () => (),
    afterResolutionLogging: Boolean => Unit = _ => (),
    beforeFetchLogging: () => Unit = () => (),
    afterFetchLogging: Boolean => Unit = _ => ()
  )(implicit S: Schedulable[F] = Task.schedulable): F[(Resolution, Seq[(Artifact, File)])] = {

    val resolutionIO = Resolve.resolveIO[F](
      dependencies,
      repositories,
      resolutionParams,
      cache,
      beforeResolutionLogging,
      afterResolutionLogging
    )

    S.bind(resolutionIO) { resolution =>
      val fetchIO_ = Artifacts.artifactsIO(
        resolution,
        classifiers,
        mainArtifacts,
        artifactTypes,
        cache,
        beforeFetchLogging,
        afterFetchLogging
      )

      S.map(fetchIO_) { artifacts =>
        (resolution, artifacts)
      }
    }
  }

  def fetchFuture(
    dependencies: Seq[Dependency],
    repositories: Seq[Repository] = Resolve.defaultRepositories,
    resolutionParams: ResolutionParams = ResolutionParams(),
    cache: Cache[Task] = Cache.default,
    classifiers: Set[Classifier] = Set(),
    mainArtifacts: JBoolean = null,
    artifactTypes: Set[Type] = core.Resolution.defaultTypes,
    beforeResolutionLogging: () => Unit = () => (),
    afterResolutionLogging: Boolean => Unit = _ => (),
    beforeFetchLogging: () => Unit = () => (),
    afterFetchLogging: Boolean => Unit = _ => ()
  )(implicit ec: ExecutionContext = cache.ec): Future[(Resolution, Seq[(Artifact, File)])] = {

    val task = fetchIO(
      dependencies,
      repositories,
      resolutionParams,
      cache,
      classifiers,
      mainArtifacts,
      artifactTypes,
      beforeResolutionLogging,
      afterResolutionLogging,
      beforeFetchLogging,
      afterFetchLogging
    )

    task.future()
  }

  def fetchEither(
    dependencies: Seq[Dependency],
    repositories: Seq[Repository] = Resolve.defaultRepositories,
    resolutionParams: ResolutionParams = ResolutionParams(),
    cache: Cache[Task] = Cache.default,
    classifiers: Set[Classifier] = Set(),
    mainArtifacts: JBoolean = null,
    artifactTypes: Set[Type] = core.Resolution.defaultTypes,
    beforeResolutionLogging: () => Unit = () => (),
    afterResolutionLogging: Boolean => Unit = _ => (),
    beforeFetchLogging: () => Unit = () => (),
    afterFetchLogging: Boolean => Unit = _ => ()
  )(implicit ec: ExecutionContext = cache.ec): Either[CoursierError, (Resolution, Seq[(Artifact, File)])] = {

    val task = fetchIO(
      dependencies,
      repositories,
      resolutionParams,
      cache,
      classifiers,
      mainArtifacts,
      artifactTypes,
      beforeResolutionLogging,
      afterResolutionLogging,
      beforeFetchLogging,
      afterFetchLogging
    )

    val f = task
      .map(Right(_))
      .handle { case ex: CoursierError => Left(ex) }
      .future()

    Await.result(f, Duration.Inf)
  }

  def fetch(
    dependencies: Seq[Dependency],
    repositories: Seq[Repository] = Resolve.defaultRepositories,
    resolutionParams: ResolutionParams = ResolutionParams(),
    cache: Cache[Task] = Cache.default,
    classifiers: Set[Classifier] = Set(),
    mainArtifacts: JBoolean = null,
    artifactTypes: Set[Type] = core.Resolution.defaultTypes,
    beforeResolutionLogging: () => Unit = () => (),
    afterResolutionLogging: Boolean => Unit = _ => (),
    beforeFetchLogging: () => Unit = () => (),
    afterFetchLogging: Boolean => Unit = _ => ()
  )(implicit ec: ExecutionContext = cache.ec): (Resolution, Seq[(Artifact, File)]) = {

    val task = fetchIO(
      dependencies,
      repositories,
      resolutionParams,
      cache,
      classifiers,
      mainArtifacts,
      artifactTypes,
      beforeResolutionLogging,
      afterResolutionLogging,
      beforeFetchLogging,
      afterFetchLogging
    )

    val f = task.future()

    Await.result(f, Duration.Inf)
  }

}
