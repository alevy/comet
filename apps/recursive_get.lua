object = {}

function object.onGet(self, caller, callbackId)
  if self.value then return self.value end
  dht.get(dht.getKey(1), 1, function(values)
    put(callbackId, values[0], {caller})
  end)
  return nil
end
