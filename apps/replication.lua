onGet = "Hello world!"

onTimer = function(self)
  dht.put(dht.key, self, 20)
end