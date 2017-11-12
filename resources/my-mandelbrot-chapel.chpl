use DynamicIters;

config const n = 1600,
  maxIter = 150,
  limit = 4.0,
  chunkSize = 1,
  size = 2.0,
  xstart = -1.5,
  ystart = -1.0;

param bitSize = 8;
type elementSize = uint(bitSize);

proc main() {
  const xdim = 0..#divceilpos(n, bitSize);
  var imageSpace : [0..#n, xdim] elementSize;

  // domain means first
  forall (y, xelem) in dynamic(imageSpace.domain, chunkSize) {
    var buff: elementSize; // declare : var something: type(size);

    for off in 0..#bitSize {
      var Zn1, Zn0: complex; // declare complex

      const x = xelem * bitSize + off; // where is x in your logical memory?
      const complexVal = (size * x/n + xstart) + (size * y/n + ystart) * 1i;

      for 1..maxIter{
        if((Zn0.re + Zn0.im) ** 2 - (2 *Zn0.re * Zn0.im) > limit) then
          break;

        Zn1.re = Zn0.re ** 2 - Zn0.im ** 2 + complexVal.re;
        Zn1.im = 2.0 * Zn0.re * Zn0.im + complexVal.im;

        Zn0.re = Zn1.re;
        Zn0.im = Zn1.im;
      }

      buff <<= 1;
      if ((Zn0.re + Zn0.im) ** 2 - (2 *Zn0.re * Zn0.im) <= limit) then
        buff |= 0x1; // draw a black point
    }

    imageSpace[y, xelem] = buff; // store the pixel
  }

  var w = openfd(1).writer(iokind.native, locking=false);

//  w.writef("P4\n");
//  w.writef("%i %i\n", n, n);
  w.write(imageSpace);
}
