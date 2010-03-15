object = {}

object.foo = function(baz)
  object.baz = baz
  baz(4)
end

object.bar = function()
  local i = 10
  object.foo(function(j) i = j end)
  return i
end
