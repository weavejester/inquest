# Inquest

Inquest is a library for non-invasive logging and monitoring in
Clojure.

Logging or monitoring code is typically added inline to an
application's source code. Inquest takes the view that logging should
be more akin to an external debugger.

Rather than adding logging code inline, you instead tell Inquest which
vars you wish to monitor, and then pass it a reporter function that is
called whenever the var is used.


## Installation

To install, add the following to your project `:dependencies`:

    [inquest "0.1.0-SNAPSHOT"]


## Basic Usage

Let's say you have some functions you wish to monitor:

```clojure
(defn foo [x] (+ x 1))
(defn bar [x] (- x 1))
```

The easiest way to monitor these functions is to create an *inquest*:

```clojure
(require '[inquest.core :as inq])

(def stop-inquest (inq/inquest [#'foo #'bar] prn))
```

In this case the inquest is monitoring both `foo` and `bar`, and the
reporter it uses is just `prn`.

If we run one of the monitored functions, we can see the reports being
generated:

```clojure
user=> (foo 1)
{:time 296453512642817, :thread 51, :state :enter, :var #'user/foo, :args (1)}
{:time 296453512979429, :thread 51, :state :exit, :var #'user/foo, :return 2}
2
```

Any exceptions that occur are also reported:

```clojure
user=> (foo "1")
{:time 296509361190517, :thread 52, :state :enter, :var #'user/foo, :args ("1")}
{:time 296509361928724, :thread 52, :state :throw, :var #'user/foo, :exception #error {...}}
```

When we want to stop the inquest, we call the function it returned
earlier:

```clojure
user=> (stop-inquest)
```


### Advanced Usage

Often you'll want a more sophisticated reporter than `prn`. One
solution is to define a multimethod:

```clojure
(defmulti console-reporter
  (juxt :state :var))

(defmethod console-reporter :default [_]
  ; do nothing
  )

(defmethod console-reporter [:enter #'foo] [{[x] :args}]
  (println "Running foo with" x))

(defmethod console-reporter [:enter #'bar] [{[x] :args}]
  (println "Running bar with" x))
```

We can then use this multimethod to generate an inquest:

```clojure
(def stop-inquest
  (let [vars (map second (keys (methods console-reporter)))]
    (inq/inquest vars console-reporter)))
```

Note that rather than declare the vars we want twice, we can use the
`methods` function to ask the multimethod which vars it can handle. In
order for this to work, the inquest must be started after the
multimethod is fully defined.


## Caveats

Inquest uses the `alter-var-root` function to add monitoring to
vars. This function doesn't currently work with protocol methods, so
neither does Inquest.


## License

Copyright © 2015 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
