object = {}

function object.onGet(self) return self.heartbeats end

function object.handleNodes(nodes)
  time = dht.sysTime()
  for i,v in ipairs(nodes) do
    key = table.concat({v.getIP(), v.getPort()}, ":")
    if self.heartbeats[key] then
      table.insert(self.heartbeats[key],time)
    else
      self.heartbeats[key] = {time}
    end
  end
end

function object.onStore(self)
  dht.lookup(dht.getKey(), self.handleNodes)
  return self
end

function object.onUpdate(self,caller, value)
  self.heartbeats[caller.getIP()] = value
end

function object.onTimer(self)
  dht.put(dht.getKey(), dht.sysTime(), 20)
end
