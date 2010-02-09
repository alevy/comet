class Array

  def sum
    self.inject(0) {|s,v| s + v}
  end

  def mean
    self.sum * 1.0 / self.size
  end

  def variance2
    mean = self.mean
    n = self.size
    self.inject(0) {|s,x| s + (x - mean)**2} / n
  end

  def stdev
    Math.sqrt(self.variance2)
  end

end
