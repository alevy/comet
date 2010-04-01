object = {}

object.heartbeats = {}

function object.onGet(self) return self.heartbeats end

function object.handleNodes(self, nodes)
  local time = dht.sysTime()
  for i,v in ipairs(nodes) do
    local key = v.getIP()
    if self.heartbeats[key] then
      table.insert(self.heartbeats[key],time)
    else
      self.heartbeats[key] = {time}
    end
  end
end

function object.onStore(self)
  dht.lookup(dht.key(), self.handleNodes)
  return self
end

function object.onTimer(self)
  dht.lookup(dht.key(), self.handleNodes)
end
