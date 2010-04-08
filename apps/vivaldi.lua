object = {}

object.seeders = {}

function object.selectVivaldiClosePeers(self, coords)
  local result = {}
  table.sort(self.seeders, function (a,b)
    local ad = math.pow(coords[1] - a[1]) + math.pow(coords[2] - b[2])
    local bd = math.pow(coords[1] - a[1]) + math.pow(coords[2] - b[2])
    return ad > bd
  end)
  for peer,vivaldi in pairs(self.seeders) do
    
  end
end

function object.onGet(self, caller, callerId, body)
  if body.event == "completed" then
    self.seeders[body.peer] = body.vivaldi
  else
    return self:selectVivaldiClosePeers(body.vivaldi)
  end
end
