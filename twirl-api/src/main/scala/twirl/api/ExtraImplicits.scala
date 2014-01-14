package twirl.api

// compatibility implicits for Scala 2.9
object ExtraImplicits {
  class WithRuntimeClass[T](manifest: ClassManifest[T]) {
    def runtimeClass: java.lang.Class[_] = manifest.erasure
  }
  implicit def addRuntimeClass[T](manifest: ClassManifest[T]): WithRuntimeClass[T] =
    new WithRuntimeClass(manifest)
}
