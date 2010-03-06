object = {}

object.pending = {}

object.onGet = function(self, callerNode, callbackKey)
  if(self.value) then
    return self.value
  end
  pending[callerNode] = callbackKey
  return null
end

object.onUpdate = function(self, callerNode, value)
  self.value = value
  for callerNode,key in pairs(self.pending) do
    dht.put(key, value, {callerNode})
  end
  self.pending = {}
end
