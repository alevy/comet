replica_ips = {}
accessor_ips = {}
value = "myValue"

onGet = function(self, caller_ip)
  table.insert(self.accessor_ips, caller_ip)
  return self.value
end

onStore = function(self, callerIp)
  table.insert(accessor_ips, callerIp)
  table.insert(replica_ips, dht.nodeIP)
  return self
end
