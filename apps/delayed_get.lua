pending = {}

onGet = function(self, callerNode, callbackKey)
  if(self.value)
    return self.value
  end
  pending[callerNode] = callbackKey
  return null
end

onUpdate = function(self, value)
  self.value = value
  for callerNode,key in pairs(self.pending) do
    dht.put(key, value, {callerNode})
  end
  self.pending = {}
end
