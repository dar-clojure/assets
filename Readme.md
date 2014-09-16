#assets

Assets management framework inspired by [component](http://tjholowaychuk.tumblr.com/post/27984551477/components).
Helps to organize and build css, scripts, etc within Clojure projects.

Any dir on classpath can be turned into assets component by placing `assets.edn` there.
For example:

```clojure
{:dependencies ["com/twitter/bootstrap"]
 :css ["foo.css", "bar.css"]
 :files ["background.png"]
 :page {:main-ns foo.bar.main
        :body "body.html"}}
```

That's enough for building a fully functional web page, ready to be served from anywhere.

##Spec

###assets.edn

####:dependencies

A list of dependencies. For example, if some dependency contains
css files, those will be included before its own css.

####:files

A list of files to be served as a static resources, e.g
images, fonts, etc.

```clojure
{:files
 ["bg.png" ; path relative to assets.edn location
  "/com/twitter/bootstrap/icons.svg" ; absolute class-path
 ]}
```

####:css

A list of css files.

```clojure
{:css ["foo.css", "/com/company/bar.css"]} ; same path resolution rules as for :files
```

All urls containing in the file will be resolved relative to its location
and appropriately rewrited, however, corresponding resource will not be copied
automatically to the public dir and still should be listed as a file of some component.
Urls starting with `//` are treated as absolute class-paths.

####:page

A map of options that describe an app entry point, i.e. specify the given component as a main.

#####:main-ns

Main namespace. Must contain `-main` (`_main`) function, which should be marked as exported.

```clojure
(defn ^:export -main [])
```

All required `.cljs` files will be compiled and appropriately included.

Google Closure style JavaScript files are supported.

To use foreign JavaScript
library just compile it to a single file, prepend `goog.provide('ns.for.lib')`
statement, place it to the corresponding class-path location, then use it as a normal clojure lib.

Main call will be placed at the end of the `body` tag. Typically, there is no need
to listen for `DOMContentLoaded` event.

#####:body

Either html file or a valid hiccup markup to include in `body` tag.

#####:head

Either html file or a valid hiccup markup to include in `head` tag.

#####:title

Page title.

##Usage

Currently, via [assets-lein](https://github.com/dar-clojure/assets-lein) plugin.

##License

Copyright © 2014 Eldar Gabdullin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
