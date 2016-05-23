# Node Graph

Simple GUI for creating directed and undirected node graphs.

## Prerequisities

Needed [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

## Getting Started

### Basic input
```
A
B-C
C>D
```
'A' is a single node, 'B' is conected to 'C' (no direction) and 'C' is connected to 'D' (with direction).

### Formating edges
```
A w=0.5
B-C h=0.3 b=0.7
C>D w=0.8 o=0.2 f=0.2
```
w: edge width.
h: edge hue.
b: edge brightness.
o: edge opacity.
f: edge fuzziness.

All values go from 0.0 to 1.0. 

## Built With

* JavaFX

## Authors

* **Elio Ventocilla**