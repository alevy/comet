SECONDS = 1000
MINUTES = 60 * SECONDS
HOURS = MINUTES * 60

require 'progressbar'

def delays(arr, result = [])
  arr.each_cons(2) {|a,b| result << (b[1] - a[1]) / MINUTES.to_f }
  return result
end

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
last_start =  false#1270438748401

result = []
pbar = ProgressBar.new("Analysing", hash.size)
hash.each do |neighbor, arr|
  if arr.first.first <= distance
    arr.sort! {|a,b| a[1] <=> b[1]}
    result = delays(arr, result)
  end
  pbar.inc
end

puts result.sort.join("\n")