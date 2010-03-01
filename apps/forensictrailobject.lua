replicaIps = {}
hostIps = {}
accessorIps = {}
value = "myValue"

onGet = function(self, callerIp)
  table.insert(self.accessorIps, caller_ip)
  return self.value
end

onStore = function(self, callerIp)
  table.insert(accessorIps, callerIp)
  table.insert(hostIps, dht.localNode.getIP())
  return self
end

onTimer = function(self)
  dht.put(dht.getKey(), self, 20, function(nodes)
    for i,v in ipairs(nodes) do table.insert(replicaIps, v.getIp()) end
  end)
end

