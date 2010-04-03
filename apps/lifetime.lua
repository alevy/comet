object = {}

object.heartbeats = {}

function object.onGet(self)
  local result = self.heartbeats
  self.heartbeats = {}
  return result
end

function object.handleNodes(self, nodes)
  local time = dht.sysTime()
  for i,v in ipairs(nodes) do
    local key = v.getIP()..":"..v.getPort()
    if self.heartbeats[key] then
      table.insert(self.heartbeats[key],{i, time})
    else
      self.heartbeats[key] = {{i, time}}
    end
  end
end

function object.onUpdate(self, other)
  return other
end

function object.onTimer(self)
  dht.lookup(dht.key(), self.handleNodes)
end
