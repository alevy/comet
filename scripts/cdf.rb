lines = File.readlines(ARGV.shift)

col = ARGV.first ? ARGV.first.to_i : 0

counts = Hash.new(0)

for line in lines do
  counts[line.split(',')[col].to_f] += 1
end

arr = Array.new
counts.keys.sort.each do |key|
  arr << [key, counts[key].to_f]
end

for i in 1..(arr.size - 1) do
  arr[i] = [arr[i].first, arr[i].last + arr[i - 1].last]
end

arr = arr.map {|v| [v.first, v.last / lines.size]}

puts arr.map {|v| v.join(",")}.join("\n")