object = {}

object.onGet = function(self)
  return self.value
end

object.onUpdate = function(self, value)
  self.value = value
  return self
end
