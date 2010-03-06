object = {}

function object.onGet(self) return self.heartbeats end

function object.onUpdate(self,caller, value)
  self.heartbeats[caller.getIP()] = value
end

function object.onTimer(self)
  dht.put(dht.getKey(), dht.currentTime, 20)
end
