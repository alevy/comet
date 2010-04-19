object = {}

object.heartbeats = {}

function object:onGet()
  local result = self.heartbeats
  --self.heartbeats = {}
  return result
end

function object.handleNodes(self,nodes)
  print("Number of nodes is "..(#nodes))
  local time = dht.sysTime()
  for i,node in ipairs(nodes) do
	local key = node.getIP()..":"..node.getPort().."^"..node.getInstanceId()
  	if not self.heartbeats[key] then
	  print(i, time)
	  self.heartbeats[key] = {{i,time}}
	end
  end
end

function object:onUpdate(other, node)
  if (self.ip == other) then
    local key = node.getIP()..":"..node.getPort().."^"..node.getInstanceId()
    table.insert(self.heartbeats[key], dht.sysTime())
    return self
  end
  return other
end

function object:onStore()
  self.ip = dht.localNode.getIP()
  dht.put(dht.key(), self.ip, 20, self.handleNodes)
  return self
end

