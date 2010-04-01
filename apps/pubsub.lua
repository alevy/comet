subscribers = {},

onGet = function(self, callerNode, callbackKey, payload)
  if payload == "getval" then
    return self.value
  end
  if #self.subscribers < 10 then
    table.insert(self.subscribers, {callerNode, callbackKey}
    return null
  else
    return self.subscribers
  end
end

onUpdate = function(self, value)
  self.value = value
  for i,v in ipairs(self.subscribers) do
    dht.put(v[1], value, {v[2]})
  end
  self.subscribers = {}
end