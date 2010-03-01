onStore = function(self)
  self.start = dht.currentTime()
end

onGet = function(self)
  if (self.start + 300000 > dht.currentTime())
    return "myValue"
  end
  return null
end
