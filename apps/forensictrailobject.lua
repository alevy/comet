object = {}
object.replicaIps = {}
object.hostIps = {}
object.accessorIps = {}

object.onGet = function(self, callerIp)
  table.insert(self.accessorIps, callerIp)
  return self
end

object.onStore = function(self, caller)
  table.insert(self.accessorIps, caller.getIP())
  table.insert(self.hostIps, dht.localNode.getIP())
  return self
end

object.onTimer = function(self)
  dht.put(dht.getKey(), self, 20, function(nodes)
    for i,v in ipairs(nodes) do table.insert(self.replicaIps, v.getIp()) end
  end)
end

