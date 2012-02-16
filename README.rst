#######
 Twirl
#######

*Twirl* is the `Play Framework`_ `Scala template engine`_, repackaged for stand-alone use.
This project provides an `SBT`_ plugin that lets you use *Twirl* in your Scala_ applications without any additional
dependencies on the `Play Framework`_.

The `Scala template engine`_ (which we dubbed *Twirl*, see below) provided by Play 2.0 enables type-safe templating that
integrates seamlessly into your Scala applications. Templates are text files containing a mix of "markup" and Scala code.
At compile time the Twirl compiler translates them into actual Scala source files, which are then picked up by the Scala
compiler and compiled together with the rest of your application sources into regular .class files.
On a type level each template is just a function from a number of (strongly typed) input values to a result object.

The *Twirl* SBT plugin smoothly integrates templating support into your Scala builds. It supports triggered
compilation (via SBTs ``~`` operator) as well as hot reloading via `sbt-revolver`_.

**Note:** The large majority of the code within this project, i.e. the twirl-api, the twirl-compiler and significant
portions of the SBT plugin are nothing but verbatim copies of the respective code pieces from the
`Play Framework Repository`_. All credits and copyrights for these belong to Play framework team.


Installation
============

sbt-twirl requires SBT 0.11.1 or 0.11.2. Add the following dependency to your ``project/*.sbt`` file
(e.g. ``project/plugins.sbt``)::

    resolvers += "spray repo" at "http://repo.spray.cc"

    addSbtPlugin("cc.spray" % "sbt-twirl" % "0.5.1")

and this to your ``build.sbt``::

    seq(Twirl.settings: _*)

If you use SBTs full-configuration you need to::

    import twirl.sbt.TwirlPlugin._

and then add the ``Twirl.settings`` to all (sub-)projects containing *Twirl* templates.


Usage
=====

*Twirl* template files are expected to live in the ``twirl`` subdirectory of your source directory, e.g. ``src/main/twirl``.
They have to be named ``{template-name}.scala.{ext}``, whereby ``ext`` can be either ``html``, ``xml`` or ``txt``
(more extensions might be supported in the future).

Example::

    {project-directory}/src/main/twirl/index.scala.html


Template Syntax
---------------

The internal template syntax is documented `here`__. Currently the Twirl engine does not differ from the original
Play Scala Template Engine in any way.


__ `Scala template engine`_


Template Rendering
------------------

To your Scala code a template is an object providing the following methods::

    def apply(: A, b: B, ...): R

    def render(a: A, b: B, ...): R = apply(a, b, ...)

    def f: (A, B) => R = (a, b) => apply(a, b)

whereby ``A`` and ``B`` are the parameter types you have specified in the first line of your template.
``R`` is the result type, which is either ``twirl.api.Html``, ``twirl.api.Txt`` or ``twirl.api.Xml``, depending on
the extension of your template. All result types are defined in `this file`_, please check it out to understand what
interface the respective result type offers.


.. _`this file`: https://github.com/spray/twirl/blob/master/twirl-api/src/main/scala/twirl/api/Formats.scala


From your Scala code you "render" a template by simply calling one of its methods, e.g., the template defined in
``{project-directory}/src/main/twirl/index.scala.html`` would could be called with::

    html.index.render("Bob", 42)

and the template ``{project-directory}/src/main/twirl/org/example/hello.scala.txt`` could be called with::

    org.example.txt.hello("Fred", "Astair")


Package Names
-------------

As you already might have guessed from the last example above sub directories underneath the ``../twirl`` template
directory are turned into package names for the created template objects. So, if you place a template in the
``{project-directory}/src/main/twirl/org/example/`` directory it will have the package ``org.example.html``,
``org.example.txt`` or ``org.example.xml``, depending on the file extension.


Configuration
=============

sbt-twirl currently offers the following customization options:

1. ``twirlImports = SettingKey[Seq[String]]``: lets you specify one or more imports that are to be made available to the
   Scala source in your template files.

   For example::

       twirlImports := Seq("org.example.util._", "com.mycompany.DbTools")

   will be turned into the following import statements::

       import org.example.util._
       import com.mycompany.DbTools

   In addition the strings in ``twirlImports`` can contain a ``%format%`` placeholder, which is replaced with the template
   file extension. This way you can target imports for specific template types.

2. ``twirlSourceCharset = SettingKey[Charset]``: lets you specify the `java.nio.charset.Charset` to use when reading
   twirl template sources and writing their corresponding ``.scala`` files. The default value is the ``UTF-8`` charset.


Example
=======

The ``/example`` directory of this project contains a tiny, stand-alone SBT 0.11.2 example project that you can look
at or use as the basis for your own endeavors.


Why "Twirl" ?
=============

As a replacement for the rather unwieldy name "Play Framework Scala template engine" we were looking for something
shorter with a bit of "punch" and liked *Twirl* as a reference to the template languages "magic" character ``@``,
which is sometimes also called "twirl".


Known Issues
============

Scala compilation errors in templates will be shown twice. Once as the verbatim error message as generated by the
compiler for the Scala source file created by the *Twirl* compiler and once mapped to the actual location in the
template source file. Suppressing the first message probably requires a fix in SBT.


License
=======

Just like the `Play Framework`_ `Scala template engine`_ *Twirl* is licensed under the `Apache License 2.0`_.


Credits
=======

All credits are to go to the Play developers who devised the template language and provided its implementation!


Patch Policy
============

Feedback and contributions to the project, no matter what kind, are always very welcome. However, patches can only be
accepted from their original author. Along with any patches, please state that the patch is your original work and that
you license the work to the sbt-revolver project under the project’s open source license.


.. _`Play Framework`: http://www.playframework.org/
.. _`Scala`: http://www.scala-lang.org/
.. _`Scala template engine`: https://github.com/playframework/Play20/wiki/ScalaTemplates
.. _`SBT`: https://github.com/harrah/xsbt/wiki
.. _`sbt-revolver`: https://github.com/spray/sbt-revolver
.. _`Play Framework Repository`: https://github.com/playframework/Play20
.. _`Apache License 2.0`: http://www.apache.org/licenses/LICENSE-2.0
