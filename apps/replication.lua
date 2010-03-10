onGet = "Hello world!"

onTimer = function(self)
  dht.put(dht.getKey(), self)
end
