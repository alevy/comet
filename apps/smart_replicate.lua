onGet = "Hello World!"

onTimer = function(self)
  dht.lookup(dht.key, function(nodes)
    nodes = selectGoodNodes(nodes)
    dht.put(dht.key, self, nodes)
  end)
end