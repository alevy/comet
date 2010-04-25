object = {}

object.seeders = {}

function object.selectVivaldiClosePeers(self, coords, maxnum)
  local result = {}
  local maxv = math.pow(coords[1] - self.seeders[0][1]) + math.pow(coords[2] - self.seeders[0][2])
  for i = 1,maxnum do
    local minv = -1
    local nth = 0
    for k,v in self.seeders do
      local d = math.pow(coords[1] - v[1]) + math.pow(coords[2] - v[2])
      if d > maxv and (d < minv or minv < 0) then
        nth = k
        minv = d
      end
    end
    result[#result] = coords[nth]
    maxv = minv
  end
  return result
end

function object.onGet(self, caller, callerId, body)
  if not body then return "Must include body"
  if body.event == "completed" then
    self.seeders[body.peer] = body.vivaldi
  else
    return self:selectVivaldiClosePeers(body.vivaldi, 20)
  end
end

