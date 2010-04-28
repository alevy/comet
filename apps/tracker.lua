object = {}

object.seeders = {}

function object:selectPeers(maxnum)
  local result = {}
  local used
  local size = #self.seeders
  if maxnum > size then maxnum = size end
  for i = 1,maxnum do
    result[#result + 1] = self.seeders[math.random(size)]
  end
  return result
end

function object:onUpdate(other)
  return other
end

function object:contains(peer)
  for i,v in ipairs(self.seeders) do
    if v == peer then return i end
  end
  return false
end

function object:onGet(caller, callerId, body)
  if not body then return nil end
  if body.event == "completed" and not self:contains(body.peer) then
    self.seeders[#self.seeders + 1] = body.peer
    return "Thanks!"
  elseif #self.seeders == 0 then return {}
  else
    return self:selectPeers(20)
  end
end


