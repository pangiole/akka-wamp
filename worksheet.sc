
def fact(n: Int) = (1 /: (1 to n))(_ * _)

// fact :: (Integral n) => n -> n

def f(n: Int): Int =
  if (n == 1) n
  else n * f(n -1)