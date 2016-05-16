# trypp.support.libgdx.physics

Box2D is a flexible API, but in most cases, I haven't needed that flexibility
in my own games. Instead, I just want to create circles, rectangles, and other
shapes which move in an arcadey way and handle collisions correctly.

Initially I tried providing my own thin system around Box2D, but every time I
put the system down and came back to it a couple months later, I always had to
relearn so much.

Instead, I've decided to take an approach where I have my own API which looks
like Box2D but has a much simpler API. Collision is easier to support (just
register some collision handlers with the world) and body creation is as
simple as providing a shape now, with no more mucking around with fixtures or
body definitions.