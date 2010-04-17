SECONDS = 1000
MINUTES = 60 * SECONDS
HOURS = MINUTES * 60

require 'progressbar'

def delays(arr)
  delays = []
  arr.each_cons(2) {|a,b| delays << (b[1] - a[1]) / MINUTES.to_f }
  return delays
end

ttl = ARGV.map {|f| File.readlines(f).size }.inject {|i,v| i + v }

pbar = ProgressBar.new("Reading", ttl)
hash = Hash.new([])

last_time = 0
first_time = 999999999999999
  
for file in ARGV do 
  lines = File.readlines(file)
  lines.each do |line|
    neighbor, port, lifetimes = line.chomp.split(':')
    lifetimes = lifetimes.split(',').map { |s| s.split('-').map {|i| first_time = [first_time, i.to_i].min; last_time = [last_time, i.to_i].max; i.to_i} }
    hash[file + neighbor + ":" + port] += lifetimes unless not hash[file + neighbor + ":" + port]
    pbar.inc 
  end
end

distance = 5
last_start =  last_time - 8 * HOURS
first_start = first_time + 8 * HOURS

result = []
pbar = ProgressBar.new("Analysing", hash.size)
hash.each do |neighbor, arr|
  if arr.first.first <= distance and arr.first.last <= last_start and arr.first.last >= first_start
    arr.sort! {|a,b| a[1] <=> b[1]}
    result << (arr.last[1] - arr.first[1]) / HOURS.to_f
    pbar.inc
  end
end

puts result.sort.join("\n")
#puts hash.values.sort {|a,b| a[0] <=> b[0]}.map {|a| a.join(",")}.join("\n")