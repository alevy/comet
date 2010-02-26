time = 300000
value = "myValue"

onStore = function(self)
  self.start = dht.currentTime()
end

onGet = function(self)
  if (self.start + self.time > dht.currentTime())
    return self.value
  end
  return null
end
