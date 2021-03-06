# dataflow viz

This repo contains a small Clojure script to visualize timely / differential dataflows using Graphviz.

## Usage

### 0. Make sure [Graphviz](https://www.graphviz.org) is installed.

E.g. run `brew install graphviz` on OSX or see [Graphviz' Installation Notes](https://www.graphviz.org/download/).

### 1. Dump timely `Operates` & `Channels` to `*.edn` format

On a Stream of timely `LogEvent`s, run something like this:

```rust
// Stream: &Stream<S, (Duration, usize, TimelyEvent)>

let operates = stream
    .filter(|(_, worker, _)| *worker == 0)
    .flat_map(|(t, _worker, x)| if let Operates(event) = x {Some(event)} else {None})
    .inspect_batch(|_t, xs| {
        use std::io::prelude::*;
        use std::fs::OpenOptions;

        let mut file = OpenOptions::new()
            .append(true)
            .create(true)
            .open("ops.edn")
            .unwrap();

        for x in xs {
            writeln!(&mut file, "{{ :id {} :addr {:?} :name {} }}", x.id, x.addr, x.name).unwrap();
        }
    });

let channels = stream
    .filter(|(_, worker, _)| *worker == 0)
    .flat_map(|(t, _worker, x)| if let Channels(event) = x {Some(event)} else {None})
    .inspect_batch(|_t, xs| {
        use std::io::prelude::*;
        use std::fs::OpenOptions;

        let mut file = OpenOptions::new()
            .append(true)
            .create(true)
            .open("chs.edn")
            .unwrap();

        for x in xs {
            let mut src_addr = x.scope_addr.clone();
            let mut target_addr = x.scope_addr.clone();
            if x.source.0 != 0 {
                src_addr.push(x.source.0);
            }
            if x.target.0 != 0 {
                target_addr.push(x.target.0);
            }
            writeln!(&mut file, "{{ :id {} :src {:?} :target {:?} :scope {:?} }}", x.id, src_addr, target_addr, x.scope_addr).unwrap();
        }
    });
```

This repository contains an exemplary `ops.edn` and `chs.edn` so that you can see what the output should look like.

### 2. Create a graph using this Clojure project

Once you've moved `ops.edn` and `chs.edn` (or whatever you've called them) to the project root, simply run the `-main` with `clj -m viz.core <ops-file> <chs-file>` to create a graph representation.

For the provided `ops.edn` and `chs.edn`, it looks like this:

<p align="center">
  <img src="https://github.com/li1/dataflow-viz/raw/master/exemplary-graph.png" width="250">
</p>


**Note:** Currently I don't respect `Channels` ports, so you'll have to take the visualization with a grain of salt.

### 3. (Optional) Create uberjar

Simply run `clj -A:uberjar`. Make sure you have some `ops.edn` & `chs.edn` available so the build doesn't fail. Also check out [cambada](https://github.com/luchiniatwork/cambada) in case you're interested in building e.g. a Graal Native Image.

## Additional Notes

For the exemplary viz, I've used the following dataflow (stolen right from the [Differential mdbook](https://timelydataflow.github.io/differential-dataflow/)):

```rust
        // define a new computation.
        let probe = worker.dataflow(|scope| {
            // create a new collection from our input.
            let manages = input.to_collection(scope);

            manages
                .iterate(|transitive| {

                    let manages = manages.enter(&transitive.scope());

                    transitive
                        .map(|(mk, m1)| (m1, mk))
                        .join(&manages)
                        .map(|(m1, (mk, p))| (mk, p))
                        .concat(&manages)
                        .distinct()
                })
                .probe()
        });
```
