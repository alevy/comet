OneTimeObject = {grace_period: 10}

function OneTimeObject:on_get()
  if (self.read)
    post_actions.abort()
  end
  self.read = true
end
