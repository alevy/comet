object = {}

function object:onGet()
  return self.data
end

function object:onUpdate(other)
  return other
end

function object:onStore()
  dht.get(dht.key(), 20, function(self, vals)
    self.data = vals
  end)
  return self
end
