object = {}

object.heartbeats = {}

function object:onGet()
  local result = self.heartbeats
  --self.heartbeats = {}
  return result
end

function object.handleNodes(self,nodes)
  self.heartbeats = dht.localNode.getIP()
end

function object:onUpdate(other, sender)
  return other
end

function object:onStore()
  --self.ip = dht.localNode.getIP()
  dht.lookup(dht.key(), function(self, nodes) print(#self); self.heartbeats = "hello world"; end)
  return self
end
