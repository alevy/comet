object = {}
object.replicaIps = {}

object.onGet = function(self)
  return self.replicaIps
end

object.onTimer = function(self)
  dht.lookup(dht.key(), function(nodes)
    for i,v in pairs(nodes) do table.insert(self.replicaIps, v.getIP()) end
  end)
end

