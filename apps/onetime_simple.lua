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
