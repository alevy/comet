onGet = function(self)
  dht.get()
  if (self.read) return null end
  self.read = dht.currentTime()
  return self
end
onTimer = function(self)
  if (self.read)
    dht.get()
    if (self.read + 10000 < dht.currentTime())
      dht.deleteSelf()
    end
  end
end

onGet = function(self)
  if(self.read)
    return null
  end
  self.read = true
  dht.get()
  return self
end

onTimer = function(self)
  dht.get()
end