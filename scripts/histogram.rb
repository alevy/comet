#!/usr/bin/env ruby
require 'distribution'

perc = (ARGV.shift || 50).to_f
inputs = ARGV

result = Hash.new

for file in inputs
  inp = File.new(file, "r")
  hash = Hash.new()
  while inp.gets
    key, value = $_.split(",")
    hash[key.to_i] ||= []
    hash[key.to_i] << value.to_f
  end

  num = File.basename(file).split('.').first.to_i
  result[num] = []
  hash.sort.each do |key,value|
    result[num] << [key,value.percentile(perc)]
  end
end

result.sort.each do |instrs,points|
  points.sort.each do |conc, val|
    puts "#{instrs},#{conc},#{val}"
  end
end