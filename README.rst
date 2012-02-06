Splay Templates
===============

This project is a remix of `Play Scala Templates`_. We extracted the template compiler and
corresponding sbt-plugin methods to provide an isolated version of templates which
can be used without play.

Play's Scala Templates is a novel templating engine which uses an idiomatic approach to
enable type-safe templating with seamless Scala code support. Each template is just a function
from input values to the formatted output text.

Installation
------------

All you have to do is to add an sbt-plugin to your project. Add the following dependency to your
`project/plugins.sbt` file:

::

  addSbtPlugin("cc.spray" % "templates" % "0.5.0")

Add the following to your `build.sbt`:

::

  seq(Template.settings: _*)

If you use SBTs full-configuration you need to

::

  import templates.sbt.TemplatePlugin._

and then add the ``Template.settings`` to the (sub-)project in which to use templates.


Usage
-----

Templates are expected in the ``template`` subdirectory of your source directory, e.g.
``src/main/templates``.


Template Documentation
----------------------

See the `template syntax`__ documentation at the Play website.

__ `Play Scala Templates`_


Known Issues
------------

- Scala compilation errors in templates will be shown twice. Once as the verbatim error message
  falling out of the compiler and once properly reported for the template file.
  Probably needs a fix on the sbt side.

License
-------

Templates is licensed under the `Apache License 2.0`_.


Credits
-------

All the credits go to the play developers who came up with the templating scheme and created
the template compiler.

.. _`Play Scala Templates`: https://github.com/playframework/Play20/wiki/ScalaTemplates
.. _`Apache License 2.0`: http://www.apache.org/licenses/LICENSE-2.0
