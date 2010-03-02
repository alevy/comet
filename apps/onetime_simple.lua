onGet = function(self)
  if(self.read) then
    return null
  end
  self.read = true
  dht.get()
  return self
end

onTimer = function(self)
  dht.get()
end
