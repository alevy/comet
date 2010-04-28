object = {}

object.seeders = {}

function object:selectVivaldiClosePeers(coords, maxnum)
  local result = {}
  local maxv = 0
  for i = 1,maxnum do
    local minv = math.huge
    local nth = nil
    for k,v in pairs(self.seeders) do
      local d = 0
      for j,c in ipairs(coords) do d = d + math.pow(c - (v[j] or math.huge), 2) end
      if d > maxv and (d < minv) then
        nth = k
        minv = d
      end
    end
    if nth then
      result[nth] = self.seeders[nth]
      maxv = minv
    else
      break
    end
  end
  return result
end

function object:onUpdate(other)
  return other
end

function object:onGet(caller, callerId, body)
  if not body then return nil end
  if body.event == "completed" then
    self.seeders[body.peer] = body.vivaldi
  end
  if #self.seeders == 1 then return {}
  else
    return self:selectVivaldiClosePeers(body.vivaldi, 20)
  end
end

