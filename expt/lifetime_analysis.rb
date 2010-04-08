SECONDS = 1000
MINUTES = 60 * SECONDS
HOURS = MINUTES * 60

require 'progressbar'

ttl = ARGV.map {|f| File.readlines(f).size }.inject {|i,v| i + v }

pbar = ProgressBar.new("Reading", ttl)
hash = Hash.new([])

for file in ARGV do 
  lines = File.readlines(file)
  lines.each do |line|
    neighbor, port, lifetimes = line.chomp.split(':')
    lifetimes = lifetimes.split(',').map { |s| s.split('-').map {|i| i.to_i} }
    hash[file + neighbor + ":" + port] += lifetimes unless not hash[file + neighbor + ":" + port]
    pbar.inc 
  end
end

distance = 5
response_gap = 20 * MINUTES
last_start =  1270438748401

result = []
pbar = ProgressBar.new("Analysing", hash.size)
hash.each do |neighbor, arr|
  arr.sort! {|a,b| a[1] <=> b[1]}
  if arr.first.first > distance
    hash.delete(neighbor)
  else
    first = arr.first.last
    stop = first > last_start if last_start
    prev = first
    for cur in arr do
      if cur.last - prev > response_gap
        result << (prev - first) / HOURS.to_f unless stop
        first = cur.last
        stop = first > last_start if last_start
      end
      prev = cur.last
    end
    result << (prev - first) / HOURS.to_f unless prev == first or stop
    #max_delay = 0
    #hash[neighbor] = [(arr.last[1] - arr.first[1]) / 3600000.0, max_delay / 600000.0]
    #hash.delete(neighbor) if max_delay > 1200000

#    sum = 0.0
#    arr.each_cons(2) {|a,b| sum += b[1] - a[1] }
#    result << sum / arr.size
  end
  pbar.inc
end

puts result.sort.join("\n")
#puts hash.values.sort {|a,b| a[0] <=> b[0]}.map {|a| a.join(",")}.join("\n")