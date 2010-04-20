object = {}

object.hb = {}

function object:onGet()
  local result = self.hb
  self.hb = {}
  return result
end

function object:doo(n, t, i)
  local k = n.getIP()..":"..n.getPort().."^"..n.getInstanceId()
  if not self.hb[k] then
    self.hb[k] = {{i,t}}
  else
    table.insert(self.hb[k], {i, t})
  end
end

function object:handle(ns)
  local t = dht.sysTime()
  for i,n in ipairs(ns) do
	self:doo(n, t, i)
  end
end

function object:onUpdate(o, n)
  print(o)
  if (self.ip == o) then
    self:doo(n, dht.sysTime(), 0)
    return self
  end
  return o
end

function object:onStore()
  self.ip = dht.localNode.getIP()
  dht.put(dht.key(), self.ip, 20, self.handle)
  return self
end

object.onTimer = object.onStore