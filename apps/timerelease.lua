object = {}
object.value = "myValue"

object.onStore = function(self)
  self.start = dht.sysTime()
  return self
end

object.onGet = function(self)
  if (self.start + 60000 < dht.sysTime()) then
    return self.value
  end
  return null
end
