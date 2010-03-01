onGet = "Hello World!"

onTimer = function(self)
  dht.lookup(dht.getKey(), function(nodes)
    nodes = self.selectGoodNodes(nodes)
    dht.put(dht.getKey(), self, nodes)
  end)
end
