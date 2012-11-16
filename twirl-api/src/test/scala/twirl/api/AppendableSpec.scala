package twirl.api

import org.specs2.mutable.Specification


object AppendableSpec extends Specification {

  "A Seq[Html]" should {
    "support the `joinWith` pimp" in {
      val seq = Seq("foo", "bar", "baz").map(Html.apply)
      seq.joinWith(Html(", ")).mkString === "foo, bar, baz"
    }
  }

}
