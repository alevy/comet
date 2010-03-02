onGet = function(self)
  dht.get()
  if (self.read) then return null end
  self.read = dht.currentTime()
  return self
end

onTimer = function(self)
  if (self.read) then
    dht.get()
    if (self.read + 10000 < dht.currentTime()) then
      dht.deleteSelf()
    end
  end
end
