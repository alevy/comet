ForensicTrailObject = {
  replica_ips = {}
  accessor_ips = {}
}

function ForensicTrailObject:get(caller_ip)
  table.insert(accessor_ips, caller_ip)
end

function ForensicTrailObject:add(node_ip, caller_ip)
  table.insert(accessor_ips, node_ip)
  table.insert(replica_ips, node_ip)
end