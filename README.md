# pachax

For this webapp you can find all the routes in handler.clj

fun paths at the moment include

    localhost:3000/cider

    localhost:3000/xblurbsample

    localhost:3000/blurbuploadtest

this project is under active development
and has three phases.  this is the first phase

in the eventual, it is meant to act as a conduit for practical human knowledge, understanding, insight, uplifting talk, research, and generally beautiful and streamlined idea representation and discourse.

# arrangement of directories
 the src/pachax directory contains two main source files, as of this writing global.clj and upload.clj  which play with their html counterparts and enlive.

the static html pages are located in the directory "resources" and the CSS file is located in resources/public .. all pretty standard lein project layouts.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2015 humanity
